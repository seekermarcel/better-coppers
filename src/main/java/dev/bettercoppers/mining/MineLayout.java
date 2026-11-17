package dev.bettercoppers.mining;

import dev.bettercoppers.BetterCoppers;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

/**
 * Turns a Mine Section into the flat, ordered list of steps a golem works through; each step says all the golem needs
 * to know about it. The steps are rebuilt from the section's kind and Open End whenever they are needed.
 * <p>
 * Cells are addressed from the section's Open End: {@code forward} along its facing, {@code side} to the right,
 * {@code up} above the parent's floor. A slice is everything at one {@code forward} distance; slice -1 is the parent's
 * last slice, where the golem stands to dig slice 0.
 */
public final class MineLayout {
	public static final Identifier STRAIGHT = BetterCoppers.id("straight");
	public static final Identifier STAIRS_DOWN = BetterCoppers.id("stairs_down");

	public enum Action {
		/** Dig out the block. */
		DIG,
		/** Make sure the block is solid to walk on. */
		FLOOR,
		FENCE,
		BEAM,
		RAIL,
		TORCH
	}

	/**
	 * {@code stand} is where the golem works from: the floor of the slice just behind, which is already dug out and
	 * connected to the entrance. Walking to the target block itself could lead the golem to the far side of a wall.
	 * {@code facing} is only used by torches: the direction the torch points, away from the block it hangs on.
	 * {@code backOff} is the way to step back from {@code stand} to lay a missing floor under it. {@code needs} is what
	 * the golem must carry before it digs here. {@code resumeAt} is the step to carry on from when its section is cut
	 * short: its rails.
	 */
	public record Step(Action action, BlockPos pos, BlockPos stand, Direction facing, Direction backOff, Needs needs, int resumeAt) {
		/** Work outside the list (an ore, a fallen block, a floor to stand on): it needs nothing and gives up only itself. */
		public static Step extra(Action action, BlockPos pos, BlockPos stand) {
			return new Step(action, pos, stand, Direction.UP, Direction.UP, Needs.NONE, -1);
		}

		private Step resumingAt(int index) {
			return new Step(this.action, this.pos, this.stand, this.facing, this.backOff, this.needs, index);
		}
	}

	/** Supplies to carry before digging; rails are optional and never needed. Digs that are checked together share one instance. */
	public record Needs(int fences, int planks) {
		public static final Needs NONE = new Needs(0, 0);
		private static final Needs SUPPORT = new Needs(4, 3);
	}

	private static final int STRAIGHT_LENGTH = 8;
	private static final int SUPPORT_SPACING = 4;
	/** Clear blocks on each step: a player needs this much to jump up one. */
	private static final int STAIR_HEIGHT = 4;
	private static final int STAIR_STEPS = 4;

	private final BlockPos start;
	private final Direction forward;
	private final Direction right;
	private final List<Step> steps = new ArrayList<>();
	private BlockPos stand;
	private Direction backOff;
	private Needs needs = Needs.NONE;

	private MineLayout(Mines.OpenEnd start) {
		this.start = start.pos();
		this.forward = start.facing();
		this.right = this.forward.getClockWise();
		this.stand = this.start;
		this.backOff = this.forward.getOpposite();
	}

	/** The steps of a section of {@code kind} growing from {@code start}. */
	public static List<Step> build(Identifier kind, Mines.OpenEnd start) {
		MineLayout layout = new MineLayout(start);
		if (kind.equals(STAIRS_DOWN)) {
			return layout.stairsDown();
		}
		if (kind.equals(STRAIGHT)) {
			return layout.straight();
		}
		throw new IllegalArgumentException("unknown Mine Section kind " + kind);
	}

	/**
	 * Like the vanilla abandoned ones: 3 wide, 3 high, plank floor where there is none, and supports of two fence posts
	 * carrying a plank beam at slices 2 and 6, with a torch hung on the first beam. Rails come last.
	 */
	private List<Step> straight() {
		for (int i = 0; i < STRAIGHT_LENGTH; i++) {
			this.standBehind(i, 0);
			this.needs = isSupportSlice(i) ? Needs.SUPPORT : Needs.NONE;
			this.digSlice(i, 0, 3);
			if (isSupportSlice(i)) {
				for (int side : new int[] {-1, 1}) {
					this.add(Action.FENCE, this.cell(i, side, 0));
					this.add(Action.FENCE, this.cell(i, side, 1));
				}
				for (int side = -1; side <= 1; side++) {
					this.add(Action.BEAM, this.cell(i, side, 2));
				}
			}
			// Hangs on the far side of the previous beam, so it needs this slice dug out first.
			if (i % (SUPPORT_SPACING * 2) == 3) {
				this.add(Action.TORCH, this.cell(i, 0, 2), this.forward);
			}
		}
		return this.withRails(STRAIGHT_LENGTH, i -> 0);
	}

	/**
	 * Four steps down, one per slice and 4 high so a player can walk back up, then a 3 high landing 4 lower, so the next
	 * section starts at a normal slice. No supports. Each step is dug from the step above it, top block first.
	 */
	private List<Step> stairsDown() {
		this.backOff = Direction.UP;
		for (int i = 0; i <= STAIR_STEPS; i++) {
			this.standBehind(i, stairFloor(i - 1));
			this.digSlice(i, stairFloor(i), i < STAIR_STEPS ? STAIR_HEIGHT : 3);
			if (i == 2) {
				this.add(Action.TORCH, this.cell(i, -1, stairFloor(i) + 1), this.right);
			}
		}
		return this.withRails(STAIR_STEPS + 1, MineLayout::stairFloor);
	}

	/** Feet height of stair slice {@code i}: the parent's floor behind it, one down per step, then the landing. */
	private static int stairFloor(int i) {
		return -Math.clamp(i + 1, 0, STAIR_STEPS);
	}

	/**
	 * Mobs cannot step onto rails (that is what keeps them inside vanilla mineshafts), so the rails go down last, from the
	 * far end back, each laid from the slice behind it. A section cut short still gets them in the part already dug.
	 */
	private List<Step> withRails(int slices, IntUnaryOperator floor) {
		int railsStart = this.steps.size();
		this.steps.replaceAll(step -> step.resumingAt(railsStart));
		this.needs = Needs.NONE;
		for (int i = slices - 1; i >= 0; i--) {
			this.standBehind(i, floor.applyAsInt(i - 1));
			this.add(Action.RAIL, this.cell(i, 0, floor.applyAsInt(i)));
		}
		int end = this.steps.size();
		this.steps.subList(railsStart, end).replaceAll(step -> step.resumingAt(end));
		return this.steps;
	}

	private static boolean isSupportSlice(int slice) {
		return slice % SUPPORT_SPACING == 2;
	}

	private BlockPos cell(int forward, int side, int up) {
		return this.start.relative(this.forward, forward).relative(this.right, side).above(up);
	}

	/** Work on slice {@code i} from the middle of slice {@code i - 1}, standing at height {@code up}. */
	private void standBehind(int i, int up) {
		this.stand = this.cell(i - 1, 0, up);
	}

	private void add(Action action, BlockPos pos) {
		this.add(action, pos, Direction.UP);
	}

	private void add(Action action, BlockPos pos, Direction facing) {
		this.steps.add(new Step(action, pos, this.stand, facing, this.backOff, action == Action.DIG ? this.needs : Needs.NONE, 0));
	}

	/** Opens a slice {@code height} high from {@code floor} up and makes sure the floor under it is solid. */
	private void digSlice(int i, int floor, int height) {
		// Middle column first, then the sides: side blocks are then seen from the open middle instead of past the
		// fence posts beside the golem. Each column top down, like a player.
		for (int side : new int[] {0, -1, 1}) {
			for (int up = floor + height - 1; up >= floor; up--) {
				this.add(Action.DIG, this.cell(i, side, up));
			}
		}
		for (int side = -1; side <= 1; side++) {
			this.add(Action.FLOOR, this.cell(i, side, floor - 1));
		}
	}
}

package dev.bettercoppers.mining;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Turns a plan into the flat, ordered list of steps a golem works through; each step says all the golem needs to know
 * about it. Each Mine Pattern has one generator method, chosen in {@link #build}.
 * <p>
 * Cells are addressed relative to the entrance: {@code forward} along the heading, {@code side} to the right,
 * {@code up} above the tunnel floor. A slice is everything at one {@code forward} distance.
 */
public final class MineLayout {
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
	 * the golem must carry before it digs here. {@code resumeAt} is the step to carry on from when its Mine Section is
	 * given up.
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

	/** Supplies to carry before digging. Digs that are checked together share one instance. */
	public record Needs(int rails, int fences, int planks) {
		public static final Needs NONE = new Needs(0, 0, 0);
	}

	private static final int SUPPORT_SPACING = 4;

	private final BlockPos entrance;
	private final Direction forward;
	private final Direction right;
	private final List<Step> steps = new ArrayList<>();
	private BlockPos stand;
	private Needs needs = Needs.NONE;
	private int resumeAt;

	private MineLayout(BlockPos entrance, Direction forward) {
		this.entrance = entrance;
		this.forward = forward;
		this.right = forward.getClockWise();
		this.stand = entrance;
	}

	public static List<Step> build(MinePlan plan, int minY) {
		MineLayout layout = new MineLayout(plan.entrance().orElseThrow(), plan.heading().orElseThrow());
		return switch (plan.pattern()) {
			case MINESHAFT -> layout.mineshaft(plan.length());
		};
	}

	/**
	 * Like the vanilla abandoned ones: 3 wide, 3 high, plank floor where there is none, and every few blocks a support
	 * of two fence posts carrying a plank beam, with a torch hung on every other beam. Rails come last.
	 */
	private List<Step> mineshaft(int length) {
		for (int i = 0; i < length; i++) {
			this.standBehind(i);
			this.slice(i);
		}
		// A hazard gives up the rest of the tunnel, and the rails then still go down in the part already dug.
		int railsStart = this.steps.size();
		this.steps.replaceAll(step -> step.resumingAt(railsStart));
		this.resumeAt = railsStart + length;
		// Mobs cannot step onto rails (that is what keeps them inside vanilla mineshafts), so the rails go down last,
		// from the far end back to the entrance, each laid from the slice behind it.
		for (int i = length - 1; i >= 0; i--) {
			this.standBehind(i);
			this.add(Action.RAIL, this.cell(i, 0, 0));
		}
		return this.steps;
	}

	/** Slices that get two fence posts and a plank beam. */
	private static boolean isSupportSlice(int slice) {
		return slice % SUPPORT_SPACING == 2;
	}

	private BlockPos cell(int forward, int side, int up) {
		return this.entrance.relative(this.forward, forward).relative(this.right, side).above(up);
	}

	/** Work on slice {@code i} from the middle of slice {@code i - 1}. */
	private void standBehind(int i) {
		this.stand = this.cell(i - 1, 0, 0);
	}

	private void add(Action action, BlockPos pos) {
		this.add(action, pos, Direction.UP);
	}

	private void add(Action action, BlockPos pos, Direction facing) {
		this.steps.add(new Step(action, pos, this.stand, facing, this.forward.getOpposite(), action == Action.DIG ? this.needs : Needs.NONE, this.resumeAt));
	}

	private void slice(int i) {
		// A rail for this slice and every slice before it (they are laid at the end), fences and planks for a support.
		this.needs = isSupportSlice(i) ? new Needs(i + 1, 4, 3) : new Needs(i + 1, 0, 0);
		// Middle column first, then the sides: side blocks are then seen from the open middle instead of past the
		// fence posts beside the golem. Each column top down, like a player.
		for (int side : new int[] {0, -1, 1}) {
			for (int up = 2; up >= 0; up--) {
				this.add(Action.DIG, this.cell(i, side, up));
			}
		}
		for (int side = -1; side <= 1; side++) {
			this.add(Action.FLOOR, this.cell(i, side, -1));
		}
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
}

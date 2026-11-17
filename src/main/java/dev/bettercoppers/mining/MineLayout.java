package dev.bettercoppers.mining;

import dev.bettercoppers.BetterCoppers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jspecify.annotations.Nullable;

/**
 * Turns a Mine Section into the flat, ordered list of steps a golem works through; each step says all the golem needs
 * to know about it. The steps are rebuilt from the section's kind and Open End whenever they are needed.
 * <p>
 * Cells are addressed from the section's Open End: {@code forward} along its facing, {@code side} to the right,
 * {@code up} above the parent's floor. A slice is everything at one {@code forward} distance; slice -1 is the parent's
 * last slice, where the golem stands to dig slice 0. A mirrored section swaps right and left.
 */
public final class MineLayout {
	public static final Identifier STRAIGHT = BetterCoppers.id("straight");
	public static final Identifier CORNER = BetterCoppers.id("corner");
	public static final Identifier CROSSING = BetterCoppers.id("crossing");
	public static final Identifier SMALL_HALL = BetterCoppers.id("small_hall");
	public static final Identifier STAIRS_DOWN = BetterCoppers.id("stairs_down");
	public static final Identifier STAIRS_UP = BetterCoppers.id("stairs_up");
	public static final Identifier DEAD_END = BetterCoppers.id("dead_end");
	/** Every built-in Section Kind. */
	public static final List<Identifier> KINDS = List.of(STRAIGHT, CORNER, CROSSING, SMALL_HALL, STAIRS_DOWN, STAIRS_UP, DEAD_END);
	/** The fences across a Closed End, in the parent's last slice. Never rolled. */
	public static final Identifier BARRIER = BetterCoppers.id("barrier");

	public enum Action {
		/** Dig out the block. */
		DIG,
		/** Make sure the block is solid to walk on. */
		FLOOR,
		FENCE,
		BEAM,
		RAIL,
		TORCH,
		/** A Barrier's sign, if the golem carries one. */
		SIGN
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
	/** Dig order of a slice's columns: middle first, then outwards, left before right. */
	private static final int[] NARROW = {0, -1, 1};
	private static final int[] WIDE = {0, -1, 1, -2, 2};

	private final BlockPos start;
	private final Direction forward;
	private final Direction right;
	private final List<Step> steps = new ArrayList<>();
	private final List<Mines.OpenEnd> openEnds = new ArrayList<>();
	/** The columns of each slice the golem can stand in: dug out, with no fence or pillar in them. */
	private final Map<Integer, Set<Integer>> free = new HashMap<>();
	private BlockPos stand;
	private int standUp;
	private Direction backOff;
	private Needs needs = Needs.NONE;

	private MineLayout(Mines.OpenEnd start, boolean mirrored) {
		this.start = start.pos();
		this.forward = start.facing();
		this.right = mirrored ? this.forward.getCounterClockWise() : this.forward.getClockWise();
		this.stand = this.start;
		this.backOff = this.forward.getOpposite();
		// Only the middle of the parent's last slice: at the entrance its sides are the frame's posts.
		this.free.put(-1, Set.of(0));
	}

	/** The layout of a section of {@code kind} growing from {@code start}. */
	public static MineLayout of(Identifier kind, Mines.OpenEnd start, boolean mirrored) {
		if (!kind.getNamespace().equals(BetterCoppers.MOD_ID)) {
			throw new IllegalArgumentException("unknown Mine Section kind " + kind);
		}
		MineLayout layout = new MineLayout(start, mirrored);
		switch (kind.getPath()) {
			case "straight" -> layout.straight();
			case "corner" -> layout.corner();
			case "crossing" -> layout.crossing();
			case "small_hall" -> layout.smallHall();
			case "stairs_down" -> layout.stairsDown();
			case "stairs_up" -> layout.stairsUp();
			case "dead_end" -> layout.deadEnd();
			case "barrier" -> layout.barrier();
			default -> throw new IllegalArgumentException("unknown Mine Section kind " + kind);
		}
		return layout;
	}

	/** The steps of a section of {@code kind} growing from {@code start}. */
	public static List<Step> build(Identifier kind, Mines.OpenEnd start, boolean mirrored) {
		return of(kind, start, mirrored).steps;
	}

	public static List<Step> build(Mines.Section section) {
		return build(section.kind(), section.start(), section.mirrored());
	}

	public List<Step> steps() {
		return this.steps;
	}

	/** Where the next sections can start, in the order they were laid out. */
	public List<Mines.OpenEnd> openEnds() {
		return this.openEnds;
	}

	/** The footprint and its floor layer: every block a step works on. */
	public BoundingBox box() {
		return BoundingBox.encapsulatingPositions(this.steps.stream().map(Step::pos).toList()).orElseThrow();
	}

	/**
	 * Like the vanilla abandoned ones: 3 wide, 3 high, plank floor where there is none, and supports of two fence posts
	 * carrying a plank beam at slices 2 and 6, with a torch hung on the first beam. Rails come last.
	 */
	private void straight() {
		for (int i = 0; i < STRAIGHT_LENGTH; i++) {
			this.needs = isSupportSlice(i) ? Needs.SUPPORT : Needs.NONE;
			this.digSlice(i, NARROW);
			if (isSupportSlice(i)) {
				this.support(i, 1);
			}
			// Hangs on the far side of the previous beam, so it needs this slice dug out first.
			if (i % (SUPPORT_SPACING * 2) == 3) {
				this.place(Action.TORCH, i, 0, 2, this.forward);
			}
		}
		this.openEnd(STRAIGHT_LENGTH, 0, 0, this.forward);
		this.withRails(STRAIGHT_LENGTH, i -> 0);
	}

	/** Three slices with the far wall left standing, open to the right. No support: it would block the turn. */
	private void corner() {
		for (int i = 0; i < 3; i++) {
			this.digSlice(i, NARROW);
		}
		this.openEnd(1, 2, 0, this.right);
		// The rails curve by themselves.
		this.withRails(List.of(new int[] {1, 1}, new int[] {1, 0}, new int[] {0, 0}), i -> 0);
	}

	/** Five by five, a plus, with a plank pillar at each inner corner. Open ahead, left and right. */
	private void crossing() {
		int[][] slices = {NARROW, WIDE, WIDE, WIDE, NARROW};
		for (int i = 0; i < slices.length; i++) {
			this.needs = i == 1 || i == 3 ? new Needs(0, 6) : Needs.NONE;
			this.digSlice(i, slices[i]);
			// A slice's pillars go up once the next slice is open: before, they would cut its outer columns off.
			if (i == 2 || i == 4) {
				for (int side : new int[] {-1, 1}) {
					for (int up = 0; up < 3; up++) {
						this.place(Action.BEAM, i - 1, side, up, Direction.UP);
					}
				}
			}
			if (i == 2) {
				// Hangs on the right pillar.
				this.place(Action.TORCH, 1, 0, 2, this.right.getOpposite());
			}
		}
		this.openEnd(5, 0, 0, this.forward);
		this.openEnd(2, -3, 0, this.right.getOpposite());
		this.openEnd(2, 3, 0, this.right);
		this.withRails(5, i -> 0);
	}

	/** Five wide and seven long, with wide supports at slices 1 and 5. The side openings fall between them. */
	private void smallHall() {
		for (int i = 0; i < 7; i++) {
			boolean support = i == 1 || i == 5;
			this.needs = support ? new Needs(4, 5) : Needs.NONE;
			this.digSlice(i, WIDE);
			if (support) {
				this.support(i, 2);
			}
			if (i == 2) {
				this.place(Action.TORCH, i, 0, 2, this.forward);
			}
			if (i == 5) {
				// Hangs on this beam, from the slice before it.
				this.place(Action.TORCH, 4, 0, 2, this.forward.getOpposite());
			}
		}
		this.openEnd(7, 0, 0, this.forward);
		this.openEnd(3, -3, 0, this.right.getOpposite());
		this.openEnd(3, 3, 0, this.right);
		this.withRails(7, i -> 0);
	}

	/** Four slices, one support and a torch on the face. No Open End: it ends a branch. */
	private void deadEnd() {
		for (int i = 0; i < 4; i++) {
			this.needs = i == 1 ? Needs.SUPPORT : Needs.NONE;
			this.digSlice(i, NARROW);
			if (i == 1) {
				this.support(i, 1);
			}
		}
		this.place(Action.TORCH, 3, 0, 1, this.forward.getOpposite());
		this.withRails(3, i -> 0);
	}

	/**
	 * Three fence posts across the parent's last slice at feet height, the middle one last, with a sign on it facing the
	 * tunnel. At the entrance the frame's posts stand where the outer two go. Built from the slice behind.
	 */
	private void barrier() {
		this.stand = this.cell(-2, 0, 0);
		for (int side : new int[] {-1, 1, 0}) {
			this.add(Action.FENCE, this.cell(-1, side, 0));
		}
		this.add(Action.SIGN, this.cell(-1, 0, 1), this.forward.getOpposite());
	}

	/** The middle fence of a Barrier closing {@code end}: where a player closes or opens it. */
	public static BlockPos middleFence(Mines.OpenEnd end) {
		return end.pos().relative(end.facing().getOpposite());
	}

	/**
	 * Four steps down, one per slice and 4 high so a player can walk back up, then a 3 high landing 4 lower, so the next
	 * section starts at a normal slice. No supports. Each step is dug from the step above it, top block first.
	 */
	private void stairsDown() {
		this.backOff = Direction.UP;
		for (int i = 0; i <= STAIR_STEPS; i++) {
			this.standUp = stairFloor(i - 1);
			this.digSlice(i, stairFloor(i), i < STAIR_STEPS ? STAIR_HEIGHT : 3, NARROW);
			if (i == 2) {
				this.place(Action.TORCH, i, -1, stairFloor(i) + 1, this.right);
			}
		}
		this.openEnd(STAIR_STEPS + 1, 0, stairFloor(STAIR_STEPS), this.forward);
		this.withRails(STAIR_STEPS + 1, MineLayout::stairFloor);
	}

	/** Feet height of stair slice {@code i}: the parent's floor behind it, one down per step, then the landing. */
	private static int stairFloor(int i) {
		return -Math.clamp(i + 1, 0, STAIR_STEPS);
	}

	/**
	 * A flat slice, four steps up and a 3 high landing 4 higher. From the step behind the golem reaches only the two lower
	 * blocks of a step, so each step is dug in two passes: those two from behind, then, standing on the new step, its
	 * third block and the fourth (head room) block of the step behind.
	 */
	private void stairsUp() {
		this.backOff = Direction.UP;
		int slices = STAIR_STEPS + 2;
		int[] dugTo = new int[slices];
		for (int i = 0; i < slices; i++) {
			int floor = stairUpFloor(i);
			int top = floor + (i < STAIR_STEPS ? STAIR_HEIGHT : 3) - 1;
			this.standUp = stairUpFloor(i - 1);
			dugTo[i] = Math.min(top, this.standUp + 2);
			this.digSlice(i, floor, dugTo[i] - floor + 1, NARROW);
			// Standing on the new step: the rest it reaches of this one and of the one behind.
			this.stand = this.cell(i, 0, floor);
			for (int slice = i; slice >= Math.max(0, i - 1); slice--) {
				int sliceTop = stairUpFloor(slice) + (slice < STAIR_STEPS ? STAIR_HEIGHT : 3) - 1;
				int to = Math.min(sliceTop, floor + 2);
				for (int side : NARROW) {
					for (int up = to; up > dugTo[slice]; up--) {
						this.add(Action.DIG, this.cell(slice, side, up));
					}
				}
				dugTo[slice] = Math.max(dugTo[slice], to);
			}
			if (i == 3) {
				this.place(Action.TORCH, i, -1, floor + 1, this.right);
			}
		}
		this.openEnd(slices, 0, stairUpFloor(slices - 1), this.forward);
		this.withRails(slices, MineLayout::stairUpFloor);
	}

	/** Feet height of stair-up slice {@code i}: flat first, one up per step, then the landing. */
	private static int stairUpFloor(int i) {
		return Math.clamp(i, 0, STAIR_STEPS);
	}

	/**
	 * Mobs cannot step onto rails (that is what keeps them inside vanilla mineshafts), so the rails go down last, from the
	 * far end back, each laid from the slice behind it. A section cut short still gets them in the part already dug.
	 */
	private void withRails(int slices, IntUnaryOperator floor) {
		List<int[]> rails = new ArrayList<>();
		for (int i = slices - 1; i >= 0; i--) {
			rails.add(new int[] {i, 0});
		}
		this.withRails(rails, floor);
	}

	/** Lays a rail on each {@code {forward, side}} cell, in this order, at the slice's {@code floor}. */
	private void withRails(List<int[]> rails, IntUnaryOperator floor) {
		int railsStart = this.steps.size();
		this.steps.replaceAll(step -> step.resumingAt(railsStart));
		this.needs = Needs.NONE;
		for (int[] rail : rails) {
			this.standUp = floor.applyAsInt(rail[0] - 1);
			this.place(Action.RAIL, rail[0], rail[1], floor.applyAsInt(rail[0]), Direction.UP);
		}
		int end = this.steps.size();
		this.steps.subList(railsStart, end).replaceAll(step -> step.resumingAt(end));
	}

	private static boolean isSupportSlice(int slice) {
		return slice % SUPPORT_SPACING == 2;
	}

	private BlockPos cell(int forward, int side, int up) {
		return this.start.relative(this.forward, forward).relative(this.right, side).above(up);
	}

	/**
	 * Where to work on column {@code side} of slice {@code i} from: the free column of slice {@code i - 1} next to it
	 * that is nearest the middle, at height {@link #standUp}. If the slice behind is too narrow, a column of this slice
	 * already dug beside it.
	 */
	private BlockPos standFor(int i, int side) {
		Integer behind = nearestFree(i - 1, side);
		if (behind != null) {
			return this.cell(i - 1, behind, this.standUp);
		}
		Integer beside = nearestFree(i, side);
		if (beside == null || beside == side) {
			throw new IllegalStateException("nowhere to stand for slice " + i + " column " + side);
		}
		return this.cell(i, beside, this.standUp);
	}

	private @Nullable Integer nearestFree(int slice, int side) {
		return this.free.getOrDefault(slice, Set.of()).stream()
			.filter(column -> Math.abs(column - side) <= 1)
			.min((a, b) -> Math.abs(a) - Math.abs(b))
			.orElse(null);
	}

	/** Builds (or digs) one cell, standing beside it in the slice behind. */
	private void place(Action action, int i, int side, int up, Direction facing) {
		this.stand = this.standFor(i, side);
		this.add(action, this.cell(i, side, up), facing);
		// A post or pillar at feet height takes the column; a beam overhead leaves it free.
		if ((action == Action.FENCE || action == Action.BEAM) && up == 0) {
			this.free.get(i).remove(side);
		}
	}

	/** Two fence posts at {@code -width} and {@code width} carrying a plank beam across the top. */
	private void support(int i, int width) {
		for (int side : new int[] {-width, width}) {
			this.place(Action.FENCE, i, side, 0, Direction.UP);
			this.place(Action.FENCE, i, side, 1, Direction.UP);
		}
		for (int side = -width; side <= width; side++) {
			this.place(Action.BEAM, i, side, 2, Direction.UP);
		}
	}

	private void openEnd(int forward, int side, int up, Direction facing) {
		this.openEnds.add(new Mines.OpenEnd(this.cell(forward, side, up), facing));
	}

	private void add(Action action, BlockPos pos) {
		this.add(action, pos, Direction.UP);
	}

	private void add(Action action, BlockPos pos, Direction facing) {
		this.steps.add(new Step(action, pos, this.stand, facing, this.backOff, action == Action.DIG ? this.needs : Needs.NONE, 0));
	}

	/** A level slice: 3 high on the parent's floor. */
	private void digSlice(int i, int[] sides) {
		this.standUp = 0;
		this.digSlice(i, 0, 3, sides);
	}

	/**
	 * Opens the {@code sides} columns of a slice {@code height} high from {@code floor} up and makes sure the floor under
	 * them is solid, standing in the slice behind at {@link #standUp}.
	 */
	private void digSlice(int i, int floor, int height, int[] sides) {
		// Middle column first, then the sides: side blocks are then seen from the open middle instead of past the
		// fence posts beside the golem. Each column top down, like a player.
		Set<Integer> columns = new HashSet<>();
		this.free.put(i, columns);
		for (int side : sides) {
			this.stand = this.standFor(i, side);
			for (int up = floor + height - 1; up >= floor; up--) {
				this.add(Action.DIG, this.cell(i, side, up));
			}
			columns.add(side);
		}
		int[] across = sides.clone();
		Arrays.sort(across);
		for (int side : across) {
			this.stand = this.standFor(i, side);
			this.add(Action.FLOOR, this.cell(i, side, floor - 1));
		}
		this.stand = this.standFor(i, 0);
	}
}

package dev.bettercoppers.mining;

import dev.bettercoppers.BetterCoppers;
import dev.bettercoppers.api.MineSectionTemplate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
	}

	/** Clear blocks on each step: a player needs this much to jump up one. */
	private static final int STAIR_HEIGHT = 4;
	private static final int STAIR_STEPS = 4;
	/** Dig order of a slice's columns: middle first, then outwards, left before right. */
	private static final int[] NARROW = {0, -1, 1};

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
		MineLayout layout = new MineLayout(start, mirrored);
		MineSections.Kind registered = MineSections.get(kind);
		if (kind.equals(BARRIER)) {
			layout.barrier();
		} else if (kind.equals(STAIRS_DOWN)) {
			layout.stairsDown();
		} else if (kind.equals(STAIRS_UP)) {
			layout.stairsUp();
		} else if (registered != null && registered.template() != null) {
			layout.level(registered.template());
		} else {
			throw new IllegalArgumentException("unknown Mine Section kind " + kind);
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
	 * A level section from its template, slice by slice: dig it, then put up its supports, the pillars of the slice
	 * behind (before, they would cut its outer columns off) and the torches that now have something to hang on. Rails
	 * come last, from the far end back.
	 */
	private void level(MineSectionTemplate template) {
		Set<MineSectionTemplate.Cell> columns = template.columns();
		int last = columns.stream().mapToInt(MineSectionTemplate.Cell::f).max().orElseThrow();
		List<Torch> torches = template.torches().stream().map(torch -> this.hang(template, torch)).toList();
		for (int i = 0; i <= last; i++) {
			int slice = i;
			int fences = 0;
			int planks = 0;
			for (MineSectionTemplate.Support support : template.supports()) {
				if (support.f() == i) {
					fences += 4;
					planks += support.toSide() - support.fromSide() + 1;
				}
			}
			planks += 3 * (int) template.pillars().stream().filter(pillar -> pillar.f() == slice).count();
			this.needs = fences + planks > 0 ? new Needs(fences, planks) : Needs.NONE;
			this.digSlice(i, columns.stream().filter(cell -> cell.f() == slice).mapToInt(MineSectionTemplate.Cell::side).boxed()
				.sorted(Comparator.comparingInt((Integer side) -> Math.abs(side)).thenComparingInt(side -> side)).mapToInt(side -> side).toArray());
			for (MineSectionTemplate.Support support : template.supports()) {
				if (support.f() == i) {
					this.support(i, support.fromSide(), support.toSide());
				}
			}
			for (MineSectionTemplate.Cell pillar : template.pillars()) {
				if (pillarSlice(template, pillar) == i) {
					for (int up = 0; up < 3; up++) {
						this.place(Action.BEAM, pillar.f(), pillar.side(), up, Direction.UP);
					}
				}
			}
			for (Torch torch : torches) {
				if (torch.slice() == i) {
					this.place(Action.TORCH, torch.cell().f(), torch.cell().side(), torch.up(), torch.facing());
				}
			}
		}
		for (MineSectionTemplate.OpenEnd end : template.openEnds()) {
			this.openEnd(end.f(), end.side(), 0, switch (end.turn()) {
				case AHEAD -> this.forward;
				case LEFT -> this.right.getOpposite();
				case RIGHT -> this.right;
			});
		}
		this.withRails(template.rails().stream().sorted(Comparator.comparingInt(rail -> -(rail.f() + Math.abs(rail.side()))))
			.map(rail -> new int[] {rail.f(), rail.side()}).toList(), i -> 0);
	}

	/** A torch at {@code up} in {@code cell}, pointing {@code facing}, put up in {@code slice}, once what it hangs on is there. */
	private record Torch(MineSectionTemplate.Cell cell, int up, Direction facing, int slice) {
	}

	/**
	 * Where a torch hangs: at head height on a beam or pillar beside it, else one lower on the rock beside it, looking
	 * behind, ahead, right and left in that order.
	 */
	private Torch hang(MineSectionTemplate template, MineSectionTemplate.Cell torch) {
		int f = torch.f();
		int side = torch.side();
		List<MineSectionTemplate.Cell> around = List.of(new MineSectionTemplate.Cell(f - 1, side), new MineSectionTemplate.Cell(f + 1, side),
			new MineSectionTemplate.Cell(f, side + 1), new MineSectionTemplate.Cell(f, side - 1));
		List<Direction> away = List.of(this.forward, this.forward.getOpposite(), this.right.getOpposite(), this.right);
		for (int i = 0; i < around.size(); i++) {
			MineSectionTemplate.Cell next = around.get(i);
			if (template.pillars().contains(next)) {
				return new Torch(torch, 2, away.get(i), Math.max(f, pillarSlice(template, next)));
			}
			for (MineSectionTemplate.Support support : template.supports()) {
				if (support.f() == next.f() && next.side() >= support.fromSide() && next.side() <= support.toSide()) {
					return new Torch(torch, 2, away.get(i), Math.max(f, support.f()));
				}
			}
		}
		for (int i = 0; i < around.size(); i++) {
			MineSectionTemplate.Cell next = around.get(i);
			if (next.f() >= 0 && !template.columns().contains(next)) {
				return new Torch(torch, 1, away.get(i), f);
			}
		}
		throw new IllegalArgumentException("torch " + torch + " has nothing to hang on");
	}

	/** The slice a pillar goes up in: once the slice after it is dug, if there is one. */
	private static int pillarSlice(MineSectionTemplate template, MineSectionTemplate.Cell pillar) {
		return template.columns().stream().anyMatch(cell -> cell.f() == pillar.f() + 1) ? pillar.f() + 1 : pillar.f();
	}

	/** Lays {@code template} out once, so a block out of reach or a torch with nothing to hang on fails at init. */
	public static void check(MineSectionTemplate template) {
		try {
			new MineLayout(new Mines.OpenEnd(BlockPos.ZERO, Direction.NORTH), false).level(template);
		} catch (IllegalStateException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
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

	/** Two fence posts at {@code from} and {@code to} carrying a plank beam across the top. */
	private void support(int i, int from, int to) {
		for (int side : new int[] {from, to}) {
			this.place(Action.FENCE, i, side, 0, Direction.UP);
			this.place(Action.FENCE, i, side, 1, Direction.UP);
		}
		for (int side = from; side <= to; side++) {
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

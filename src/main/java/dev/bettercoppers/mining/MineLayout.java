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
	private static final int STAIR_TORCH_SPACING = 8;
	/** Clear blocks on each step: a player needs this much to jump up one. */
	private static final int STAIR_HEIGHT = 4;
	/** How high vanilla's bedrock floor reaches; a golem cannot dig it. */
	private static final int BEDROCK_LAYERS = 5;
	private static final int BRANCH_SPACING = 3;
	private static final int STRIP_TORCH_SPACING = 9;
	/** Marks a Strip Mine's corridor steps until the index past the last step is known. */
	private static final int CORRIDOR = -1;
	private static final int PILLAR_SPACING = 4;
	/** Stands in for the end of the list until a hall's last step is known. */
	private static final int REST_OF_HALL = -2;
	private static final int LEVEL_SPACING = 6;
	/** Depth of each of the two flights between levels; together they drop one {@link #LEVEL_SPACING}. */
	private static final int FLIGHT_DEPTH = LEVEL_SPACING / 2;
	/** Stands in for the end of the list on the stairs between levels. */
	private static final int LOWER_LEVELS = -3;

	private BlockPos entrance;
	private final Direction forward;
	private final Direction right;
	private final List<Step> steps = new ArrayList<>();
	private BlockPos stand;
	private Direction backOff;
	private Needs needs = Needs.NONE;
	private int resumeAt;

	private MineLayout(BlockPos entrance, Direction forward) {
		this.entrance = entrance;
		this.forward = forward;
		this.right = forward.getClockWise();
		this.stand = entrance;
		this.backOff = forward.getOpposite();
	}

	public static List<Step> build(MinePlan plan, int minY) {
		MineLayout layout = new MineLayout(plan.entrance().orElseThrow(), plan.heading().orElseThrow());
		return switch (plan.pattern()) {
			case MINESHAFT -> layout.mineshaft(plan.length());
			case STAIRCASE -> layout.staircase(plan.length(), minY);
			case STRIP_MINE -> layout.stripMine(plan.length(), plan.setting());
			case ROOM_AND_PILLAR -> layout.roomAndPillar(plan);
			case MULTILEVEL_MINE -> layout.multilevelMine(plan, minY);
		};
	}

	/**
	 * A 3x3 corridor dug a stretch at a time, each stretch followed by its 1 wide, 2 high branches to both sides. Torches
	 * are the only supply. A branch is on the last slice of its stretch, so a hazard anywhere in a stretch also cuts off
	 * that stretch's branches, and giving up the corridor gives up the rest of the mine.
	 */
	private List<Step> stripMine(int plannedLength, int plannedBranchLength) {
		// A plan edited by command can ask for anything.
		int length = Math.clamp(plannedLength, MinePlan.MIN_LENGTH, MinePattern.STRIP_MINE.maxLength());
		MinePattern.Setting setting = MinePattern.STRIP_MINE.setting();
		int branchLength = Math.clamp(plannedBranchLength, setting.min(), setting.max());
		this.resumeAt = CORRIDOR;
		for (int i = 0; i < length; i++) {
			this.standBehind(i);
			this.digSlice(i);
			// On a slice without branches, so there is rock to hang on.
			if (i % STRIP_TORCH_SPACING == 3) {
				this.add(Action.TORCH, this.cell(i, 1, 1), this.right.getOpposite());
			}
			if (i % BRANCH_SPACING == BRANCH_SPACING - 1) {
				this.branch(i, 1, branchLength);
				this.branch(i, -1, branchLength);
			}
		}
		int end = this.steps.size();
		this.steps.replaceAll(step -> step.resumeAt() == CORRIDOR ? step.resumingAt(end) : step);
		return this.steps;
	}

	/** One side's branch at {@code slice}, heading away from the corridor; {@code towards} is 1 for the right, -1 for the left. */
	private void branch(int slice, int towards, int length) {
		int start = this.steps.size();
		this.backOff = towards > 0 ? this.right.getOpposite() : this.right;
		for (int i = 0; i < length; i++) {
			int side = towards * (2 + i);
			this.stand = this.cell(slice, side - towards, 0);
			this.add(Action.DIG, this.cell(slice, side, 1));
			this.add(Action.DIG, this.cell(slice, side, 0));
			this.add(Action.FLOOR, this.cell(slice, side, -1));
			if (i % STRIP_TORCH_SPACING == 3) {
				this.add(Action.TORCH, this.cell(slice, side, 1), this.forward);
			}
		}
		this.backOff = this.forward.getOpposite();
		int next = this.steps.size();
		this.steps.subList(start, next).replaceAll(step -> step.resumingAt(next));
	}

	/**
	 * Like the vanilla abandoned ones: 3 wide, 3 high, plank floor where there is none, and every few blocks a support
	 * of two fence posts carrying a plank beam, with a torch hung on every other beam. Rails come last.
	 */
	private List<Step> mineshaft(int length) {
		int first = this.steps.size();
		for (int i = 0; i < length; i++) {
			this.standBehind(i);
			this.slice(i);
		}
		// A hazard gives up the rest of the tunnel, and the rails then still go down in the part already dug.
		int railsStart = this.steps.size();
		this.steps.subList(first, railsStart).replaceAll(step -> step.resumingAt(railsStart));
		this.resumeAt = railsStart + length;
		// Mobs cannot step onto rails (that is what keeps them inside vanilla mineshafts), so the rails go down last,
		// from the far end back to the entrance, each laid from the slice behind it.
		for (int i = length - 1; i >= 0; i--) {
			this.standBehind(i);
			this.add(Action.RAIL, this.cell(i, 0, 0));
		}
		return this.steps;
	}

	/**
	 * Mineshafts stacked straight down from the entrance, each finished, rails included, before the next. Levels that would
	 * reach the bedrock are dropped. A hazard in a tunnel gives up the rest of that level only, because the stairs start
	 * behind its first slice; a hazard on the stairs gives up everything below them.
	 */
	private List<Step> multilevelMine(MinePlan plan, int minY) {
		// A plan edited by command can ask for anything.
		int length = Math.clamp(plan.length(), MinePlan.MIN_LENGTH, plan.pattern().maxLength());
		MinePattern.Setting setting = plan.pattern().setting();
		int planned = Math.clamp(plan.setting(), setting.min(), setting.max());
		int levels = Math.min(planned, Math.floorDiv(this.entrance.getY() - minY - BEDROCK_LAYERS, LEVEL_SPACING) + 1);
		for (int level = 0; level < levels; level++) {
			if (level > 0) {
				this.stairsToNextLevel();
			}
			this.mineshaft(length);
		}
		int end = this.steps.size();
		this.steps.replaceAll(step -> step.resumeAt() == LOWER_LEVELS ? step.resumingAt(end) : step);
		return this.steps;
	}

	/**
	 * Two flights, there and back, in the two rows behind the first slice, so they need no tunnel to be dug and leave its
	 * floor whole. The flights are a block out of step, with a flat corner between them: the pathfinder cuts corners
	 * upwards without seeing a wall at head height, and side by side they would trap a golem coming up. Moves the
	 * entrance down to the next level, whose first slice is then worked from where they end.
	 */
	private void stairsToNextLevel() {
		this.resumeAt = LOWER_LEVELS;
		this.stairs(this.cell(-1, 0, 0), this.right, FLIGHT_DEPTH);
		BlockPos corner = this.cell(-1, FLIGHT_DEPTH + 1, -FLIGHT_DEPTH);
		BlockPos landing = this.cell(-2, FLIGHT_DEPTH + 1, -FLIGHT_DEPTH);
		this.carveStep(this.cell(-1, FLIGHT_DEPTH, -FLIGHT_DEPTH), corner);
		this.carveStep(corner, landing);
		this.add(Action.TORCH, landing.above(), this.right.getOpposite());
		this.stairs(landing, this.right.getOpposite(), FLIGHT_DEPTH);
		this.entrance = this.entrance.below(LEVEL_SPACING);
		this.carveStep(this.cell(-2, 1, 0), this.cell(-2, 0, 0));
		this.carveStep(this.cell(-2, 0, 0), this.cell(-1, 0, 0));
		this.backOff = this.forward.getOpposite();
	}

	/** Stops above the bedrock; a hazard gives up the rest of the stairs, and there is nothing after them. */
	private List<Step> staircase(int length, int minY) {
		int depth = Math.min(Math.min(length, MinePattern.STAIRCASE.maxLength()), this.entrance.getY() - minY - BEDROCK_LAYERS);
		this.stairs(this.cell(-1, 0, 0), this.forward, depth);
		int end = this.steps.size();
		this.steps.replaceAll(step -> step.resumingAt(end));
		return this.steps;
	}

	/**
	 * A carved stairway, one block down per block of {@code run}, from the open block {@code start} (where the golem
	 * stands for the first step) until it is {@code depth} blocks lower. One wide and {@link #STAIR_HEIGHT} high on each step,
	 * and nothing to carry but floor patches, torches optional. Any generator can append these.
	 */
	private void stairs(BlockPos start, Direction run, int depth) {
		this.needs = Needs.NONE;
		for (int i = 1; i <= depth; i++) {
			BlockPos step = start.relative(run, i).below(i);
			this.carveStep(start.relative(run, i - 1).below(i - 1), step);
			if (i % STAIR_TORCH_SPACING == STAIR_TORCH_SPACING / 2) {
				this.add(Action.TORCH, step.above(), run.getCounterClockWise());
			}
		}
		this.backOff = this.forward.getOpposite();
	}

	/** Opens one step of stairs from {@code stand}, top block first. */
	private void carveStep(BlockPos stand, BlockPos step) {
		this.stand = stand;
		// One step back is up as well as back, and straight back is the rock under the step behind.
		this.backOff = Direction.UP;
		for (int up = STAIR_HEIGHT - 1; up >= 0; up--) {
			this.add(Action.DIG, step.above(up));
		}
		this.add(Action.FLOOR, step.below());
	}

	/**
	 * A 3-high hall from the entrance on, row by row, each row from its centre out to one side and then the other, with
	 * 1x1 rock pillars left standing. Each half-row is its own Mine Section; the centre column carries the whole hall.
	 */
	private List<Step> roomAndPillar(MinePlan plan) {
		// A plan edited by command can hold any setting or length, so build only sizes the pattern knows.
		MinePattern.Setting width = plan.pattern().setting();
		int length = Math.clamp(plan.length(), MinePlan.MIN_LENGTH, plan.pattern().maxLength());
		int halfWidth = (plan.setting() >= width.max() ? width.max() : width.min()) / 2;
		this.resumeAt = REST_OF_HALL;
		for (int row = 0; row < length; row++) {
			this.standBehind(row);
			this.column(row, 0);
			for (int direction : new int[] {1, -1}) {
				this.halfRow(row, direction, halfWidth);
			}
		}
		int end = this.steps.size();
		this.steps.replaceAll(step -> step.resumeAt() == REST_OF_HALL ? step.resumingAt(end) : step);
		return this.steps;
	}

	/** The golem stands in the part of the row just dug, or in the row behind where a pillar is in the way. */
	private void halfRow(int row, int direction, int halfWidth) {
		int first = this.steps.size();
		for (int distance = 1; distance <= halfWidth; distance++) {
			int side = distance * direction;
			this.stand = isPillar(row, side - direction) ? this.cell(row - 1, side, 0) : this.cell(row, side - direction, 0);
			if (!isPillar(row, side)) {
				this.column(row, side);
			} else if (row % (PILLAR_SPACING * 2) == 2) {
				this.add(Action.TORCH, this.cell(row - 1, side, 1), this.forward.getOpposite());
			}
		}
		int next = this.steps.size();
		this.steps.subList(first, next).replaceAll(step -> step.resumingAt(next));
	}

	private static boolean isPillar(int row, int side) {
		return row % PILLAR_SPACING == 2 && (Math.abs(side) == 2 || Math.abs(side) == 6);
	}

	/** One column of a hall, top down, and the floor under it. */
	private void column(int row, int side) {
		for (int up = 2; up >= 0; up--) {
			this.add(Action.DIG, this.cell(row, side, up));
		}
		this.add(Action.FLOOR, this.cell(row, side, -1));
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
		this.steps.add(new Step(action, pos, this.stand, facing, this.backOff, action == Action.DIG ? this.needs : Needs.NONE, this.resumeAt));
	}

	private void slice(int i) {
		// A rail for this slice and every slice before it (they are laid at the end), fences and planks for a support.
		this.needs = isSupportSlice(i) ? new Needs(i + 1, 4, 3) : new Needs(i + 1, 0, 0);
		this.digSlice(i);
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

	/** Opens a 3x3 slice and makes sure its floor is solid. */
	private void digSlice(int i) {
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
	}
}

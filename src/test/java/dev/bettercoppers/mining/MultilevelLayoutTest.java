package dev.bettercoppers.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Heading north from y 0: the tunnels are z 0 and further north, and x grows to the golem's right. */
class MultilevelLayoutTest {
	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	private static List<MineLayout.Step> mine(int length, int levels, int entranceY, int minY) {
		return MineLayout.build(MinePlan.blank(MinePattern.MULTILEVEL_MINE, length, levels).marked(new BlockPos(0, entranceY, 0), Direction.NORTH), minY);
	}

	private static List<MineLayout.Step> mineshaftAt(int length, int y) {
		return MineLayout.build(MinePlan.blank(MinePattern.MINESHAFT, length).marked(new BlockPos(0, y, 0), Direction.NORTH), -64);
	}

	private record Work(MineLayout.Action action, BlockPos pos) {
	}

	private static List<Work> work(List<MineLayout.Step> steps) {
		return steps.stream().map(step -> new Work(step.action(), step.pos())).toList();
	}

	/** The work inside the tunnels, in order; the stairs are all south of z 0. */
	private static List<Work> tunnelWork(List<MineLayout.Step> steps, int y) {
		return work(steps).stream().filter(work -> work.pos().getZ() <= 0 && work.pos().getY() <= y + 2 && work.pos().getY() >= y - 1).toList();
	}

	@Test
	void eachLevelIsTheSameMineshaftSixBlocksBelowTheLast() {
		List<MineLayout.Step> steps = mine(8, 3, 0, -64);
		for (int level = 0; level < 3; level++) {
			assertEquals(work(mineshaftAt(8, -6 * level)), tunnelWork(steps, -6 * level), "level " + level);
		}
	}

	private static final BlockPos TOP = new BlockPos(0, 0, 1);
	private static final BlockPos SECOND_LEVEL_DOOR = new BlockPos(0, -6, 1);

	private static Set<BlockPos> dug(List<MineLayout.Step> steps) {
		Set<BlockPos> dug = new HashSet<>();
		steps.stream().filter(step -> step.action() == MineLayout.Action.DIG).forEach(step -> dug.add(step.pos()));
		return dug;
	}

	/** Every place a golem can stand on when walking the dug blocks from the entrance, a block up or down at a time. */
	private static Set<BlockPos> walkable(Set<BlockPos> dug) {
		Set<BlockPos> open = new HashSet<>(dug);
		open.add(TOP);
		open.add(TOP.above());
		Set<BlockPos> reached = new HashSet<>(Set.of(TOP));
		ArrayDeque<BlockPos> queue = new ArrayDeque<>(reached);
		while (!queue.isEmpty()) {
			BlockPos from = queue.poll();
			for (Direction direction : Direction.Plane.HORIZONTAL) {
				for (int rise = -1; rise <= 1; rise++) {
					BlockPos to = from.relative(direction).above(rise);
					if (open.contains(to) && open.contains(to.above()) && !open.contains(to.below()) && reached.add(to)) {
						queue.add(to);
					}
				}
			}
		}
		return reached;
	}

	@Test
	void theStairsJoinEachLevelToTheNextWalkingOnly() {
		Set<BlockPos> reached = walkable(dug(mine(8, 3, 0, -64)));
		assertTrue(reached.contains(SECOND_LEVEL_DOOR), "second level not reached");
		assertTrue(reached.contains(SECOND_LEVEL_DOOR.below(6)), "third level not reached");
		assertTrue(reached.contains(new BlockPos(0, -12, -7)), "far end of the third level not reached");
	}

	/**
	 * The pathfinder cuts a corner upwards without looking at the blocks beside it at the higher level, and a golem then
	 * presses into that wall for ever. So wherever one place stands diagonally above another, both corners must be open.
	 */
	@Test
	void noRisingDiagonalBetweenStairsHasAWallAtItsCorner() {
		Set<BlockPos> open = dug(mine(8, 3, 0, -64));
		open.add(TOP);
		open.add(TOP.above());
		Set<BlockPos> places = walkable(open);
		for (BlockPos from : places) {
			for (Direction first : List.of(Direction.NORTH, Direction.SOUTH)) {
				for (Direction second : List.of(Direction.EAST, Direction.WEST)) {
					BlockPos high = from.relative(first).relative(second).above();
					if (places.contains(high)) {
						assertTrue(open.contains(from.relative(first).above()) && open.contains(from.relative(second).above()), "wall at the corner from " + from + " up to " + high);
					}
				}
			}
		}
	}

	@Test
	void everyStepStandsSomewhereAlreadyOpen() {
		Set<BlockPos> open = new HashSet<>(Set.of(TOP));
		for (MineLayout.Step step : mine(8, 3, 0, -64)) {
			assertTrue(open.contains(step.stand()), "stands in rock: " + step);
			if (step.action() == MineLayout.Action.DIG) {
				open.add(step.pos());
			}
		}
	}

	@Test
	void theRockBetweenTheTunnelsStaysUntouched() {
		for (MineLayout.Step step : mine(8, 3, 0, -64)) {
			int y = step.pos().getY();
			if (step.pos().getZ() <= 0 && step.action() == MineLayout.Action.DIG) {
				assertTrue(Math.floorMod(y, 6) <= 2, "dug between the tunnels: " + step);
			}
		}
	}

	@Test
	void theStairsCarryNoRailsAndNeedNothing() {
		for (MineLayout.Step step : mine(8, 2, 0, -64)) {
			if (step.pos().getZ() > 0) {
				assertTrue(step.action() != MineLayout.Action.RAIL && step.action() != MineLayout.Action.FENCE && step.action() != MineLayout.Action.BEAM, "on the stairs: " + step);
				assertEquals(MineLayout.Needs.NONE, step.needs(), "step: " + step);
			}
		}
	}

	@Test
	void theLandingTorchHangsOnSolidRock() {
		List<MineLayout.Step> steps = mine(8, 2, 0, -64);
		Set<BlockPos> dug = dug(steps);
		List<MineLayout.Step> torches = steps.stream().filter(step -> step.action() == MineLayout.Action.TORCH && step.pos().getZ() > 0).toList();
		assertEquals(1, torches.size());
		MineLayout.Step torch = torches.getFirst();
		assertTrue(dug.contains(torch.pos()), "torch in rock: " + torch);
		assertTrue(!dug.contains(torch.pos().relative(torch.facing().getOpposite())), "nothing to hang on: " + torch);
	}

	private static int indexOfDig(List<MineLayout.Step> steps, BlockPos pos) {
		for (int i = 0; i < steps.size(); i++) {
			if (steps.get(i).action() == MineLayout.Action.DIG && steps.get(i).pos().equals(pos)) {
				return i;
			}
		}
		throw new AssertionError("never dug: " + pos);
	}

	private static int firstRailOf(List<MineLayout.Step> steps, int y) {
		for (int i = 0; i < steps.size(); i++) {
			if (steps.get(i).action() == MineLayout.Action.RAIL && steps.get(i).pos().getY() == y) {
				return i;
			}
		}
		throw new AssertionError("no rails at y " + y);
	}

	@Test
	void aLevelIsFinishedRailsIncludedBeforeTheStairsBelowIt() {
		List<MineLayout.Step> steps = mine(8, 2, 0, -64);
		int firstStairDig = indexOfDig(steps, new BlockPos(1, 2, 1));
		assertEquals(firstRailOf(steps, 0) + 8, firstStairDig, "the stairs start right after the last rail");
		assertTrue(firstStairDig < indexOfDig(steps, new BlockPos(0, -4, 0)), "the next level comes after the stairs");
	}

	@Test
	void aTunnelHazardGivesUpOnlyThatLevelsTunnelAndKeepsItsRails() {
		List<MineLayout.Step> steps = mine(8, 2, 0, -64);
		int topStep = indexOfDig(steps, new BlockPos(0, 2, -3));
		assertEquals(firstRailOf(steps, 0), steps.get(topStep).resumeAt());
		int lowerStep = indexOfDig(steps, new BlockPos(0, -4, -3));
		assertEquals(firstRailOf(steps, -6), steps.get(lowerStep).resumeAt());
	}

	@Test
	void aLevelsRailsMoveOnToTheStairsBelowIt() {
		List<MineLayout.Step> steps = mine(8, 2, 0, -64);
		assertEquals(indexOfDig(steps, new BlockPos(1, 2, 1)), steps.get(firstRailOf(steps, 0)).resumeAt());
	}

	@Test
	void aHazardOnTheStairsGivesUpEverythingBelow() {
		List<MineLayout.Step> steps = mine(8, 3, 0, -64);
		for (MineLayout.Step step : steps) {
			if (step.pos().getZ() > 0) {
				assertEquals(steps.size(), step.resumeAt(), "step: " + step);
			}
		}
	}

	@Test
	void restockingStaysPerLevel() {
		List<MineLayout.Step> steps = mine(16, 2, 0, -64);
		assertEquals(1, steps.get(indexOfDig(steps, new BlockPos(0, -4, 0))).needs().rails());
		assertEquals(16, steps.stream().mapToInt(step -> step.needs().rails()).max().orElseThrow());
	}

	private static int levelsDug(int entranceY, int minY) {
		return (int) dug(mine(8, 8, entranceY, minY)).stream().filter(pos -> pos.getX() == 0 && pos.getZ() == -7).map(BlockPos::getY).distinct().count() / 3;
	}

	/** World bottom -64, so a floor may stand no lower than -59. */
	@Test
	void levelsThatWouldReachTheBedrockAreDropped() {
		assertEquals(2, levelsDug(-53, -64), "-53 and -59 fit");
		assertEquals(1, levelsDug(-54, -64), "-60 does not");
		assertEquals(1, levelsDug(-59, -64), "the entrance level itself just fits");
		assertEquals(0, levelsDug(-60, -64), "not even the first level fits");
	}

	@Test
	void aDroppedLevelLeavesNoStairsToIt() {
		assertTrue(mine(8, 8, -54, -64).stream().noneMatch(step -> step.pos().getZ() > 0), "stairs to nowhere");
	}

	@Test
	void aPlanEditedOutOfRangeStaysWithinTheLimits() {
		assertEquals(dug(mine(8, 2, 300, -64)), dug(mine(-3, -100, 300, -64)));
		assertEquals(dug(mine(128, 8, 300, -64)), dug(mine(100000, 100000, 300, -64)));
	}
}

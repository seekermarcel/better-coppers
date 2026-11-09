package dev.bettercoppers.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MineLayoutTest {
	/** Dig, floor, support and torch steps of an 8 long mineshaft; its rails start here. */
	private static final int TUNNEL_STEPS_OF_8 = 111;

	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	private static List<MineLayout.Step> north(int length) {
		return MineLayout.build(MinePlan.blank(MinePattern.MINESHAFT, length).marked(BlockPos.ZERO, Direction.NORTH), 0);
	}

	/** Heading north from the origin, a block's slice is how far north it is. */
	private static int slice(MineLayout.Step step) {
		return -step.pos().getZ();
	}

	private static List<MineLayout.Step> stairs(int length, int entranceY, int minY) {
		return MineLayout.build(MinePlan.blank(MinePattern.STAIRCASE, length).marked(new BlockPos(0, entranceY, 0), Direction.NORTH), minY);
	}

	private static List<BlockPos> positions(List<MineLayout.Step> steps, MineLayout.Action action) {
		return steps.stream().filter(step -> step.action() == action).map(MineLayout.Step::pos).toList();
	}

	@Test
	void stairsGoOneDownPerBlockWithTwoClearBlocksOnEachStep() {
		List<MineLayout.Step> steps = stairs(8, 0, -64);
		List<BlockPos> digs = positions(steps, MineLayout.Action.DIG);
		assertEquals(16, digs.size());
		assertEquals(List.of(new BlockPos(0, 0, 0), new BlockPos(0, -1, 0), new BlockPos(0, -1, -1), new BlockPos(0, -2, -1)), digs.subList(0, 4));
		assertEquals(List.of(new BlockPos(0, -7, -7), new BlockPos(0, -8, -7)), digs.subList(14, 16));
		assertEquals(new BlockPos(0, -9, -7), positions(steps, MineLayout.Action.FLOOR).getLast());
		assertEquals(new BlockPos(0, -7, -6), steps.stream().filter(step -> step.pos().equals(new BlockPos(0, -8, -7))).findFirst().orElseThrow().stand());
		assertEquals(new BlockPos(0, 0, 1), steps.getFirst().stand());
	}

	@Test
	void stairsCarryNoSuppliesAndNoRails() {
		List<MineLayout.Step> steps = stairs(8, 0, -64);
		steps.forEach(step -> assertEquals(MineLayout.Needs.NONE, step.needs(), "step: " + step));
		assertEquals(Set.of(MineLayout.Action.DIG, MineLayout.Action.FLOOR, MineLayout.Action.TORCH), steps.stream().map(MineLayout.Step::action).collect(Collectors.toSet()));
	}

	@Test
	void stairsHangATorchOnTheWallEveryEightSteps() {
		List<MineLayout.Step> torches = stairs(16, 0, -64).stream().filter(step -> step.action() == MineLayout.Action.TORCH).toList();
		assertEquals(List.of(new BlockPos(0, -3, -3), new BlockPos(0, -11, -11)), torches.stream().map(MineLayout.Step::pos).toList());
		assertEquals(Direction.WEST, torches.getFirst().facing(), "hangs on the east wall");
	}

	@Test
	void stairsStopAboveTheBedrockAtTheBottomOfTheWorld() {
		// Entrance 12 above the bottom: 5 layers of bedrock leave 7 steps, the lowest standing at -59.
		List<BlockPos> digs = positions(stairs(32, -52, -64), MineLayout.Action.DIG);
		assertEquals(14, digs.size());
		assertEquals(-59, digs.getLast().getY());
	}

	@Test
	void stairsAtTheBottomOfTheWorldHaveNoSteps() {
		assertEquals(List.of(), stairs(32, -64, -64));
		assertEquals(List.of(), stairs(32, -100, -64));
	}

	@Test
	void aPlanEditedOutOfRangeStaysWithinTheStaircaseLimits() {
		assertEquals(2 * MinePlan.MAX_LENGTH, positions(stairs(100000, 300, -64), MineLayout.Action.DIG).size());
		assertEquals(List.of(), stairs(-5, 0, -64));
		assertEquals(List.of(), stairs(0, 0, -64));
	}

	@Test
	void givingUpTheStairsEndsThePlan() {
		List<MineLayout.Step> steps = stairs(8, 0, -64);
		steps.forEach(step -> assertEquals(steps.size(), step.resumeAt(), "step: " + step));
	}

	/** A step back along the stairs is up as well as back; straight back is the rock under the step behind. */
	@Test
	void stairsBackOffStraightUp() {
		stairs(8, 0, -64).forEach(step -> assertEquals(Direction.UP, step.backOff(), "step: " + step));
	}

	@Test
	void layoutFollowsPlan() {
		List<MineLayout.Step> steps = north(16);
		assertEquals(MineLayout.Action.DIG, steps.getFirst().action());
		assertEquals(BlockPos.ZERO.above(2), steps.getFirst().pos());
		for (MineLayout.Step step : steps) {
			assertTrue(slice(step) >= 0 && slice(step) < 16, "outside the plan: " + step.pos());
		}
	}

	@Test
	void digsCarryWhatTheirSliceNeeds() {
		List<MineLayout.Step> steps = north(16);
		for (MineLayout.Step dig : steps) {
			if (dig.action() != MineLayout.Action.DIG) {
				assertEquals(MineLayout.Needs.NONE, dig.needs(), "only digs need supplies: " + dig);
				continue;
			}
			assertEquals(slice(dig) + 1, dig.needs().rails());
			assertEquals(steps.stream().filter(step -> step.action() == MineLayout.Action.FENCE && slice(step) == slice(dig)).count(), dig.needs().fences());
			assertEquals(steps.stream().filter(step -> step.action() == MineLayout.Action.BEAM && slice(step) == slice(dig)).count(), dig.needs().planks());
		}
		assertEquals(new MineLayout.Needs(3, 4, 3), steps.stream().filter(step -> step.action() == MineLayout.Action.DIG && slice(step) == 2).findFirst().orElseThrow().needs());
	}

	@Test
	void digsOfOneSliceShareTheirNeeds() {
		List<MineLayout.Step> digs = north(8).stream().filter(step -> step.action() == MineLayout.Action.DIG && slice(step) == 3).toList();
		digs.forEach(dig -> assertSame(digs.getFirst().needs(), dig.needs()));
	}

	@Test
	void givingUpTheTunnelResumesAtTheRails() {
		List<MineLayout.Step> steps = north(8);
		assertEquals(MineLayout.Action.RAIL, steps.get(TUNNEL_STEPS_OF_8).action());
		assertTrue(steps.get(TUNNEL_STEPS_OF_8 - 1).action() != MineLayout.Action.RAIL, "the rails start one step later");
		steps.subList(0, TUNNEL_STEPS_OF_8).forEach(step -> assertEquals(TUNNEL_STEPS_OF_8, step.resumeAt(), "tunnel step: " + step));
	}

	@Test
	void markingAgainForgetsWhereItGaveUp() {
		MinePlan plan = MinePlan.blank(MinePattern.MINESHAFT, 8).marked(BlockPos.ZERO, Direction.NORTH).givenUpAt(BlockPos.ZERO.above(), 50);
		assertEquals(BlockPos.ZERO.above(), plan.withProgress(60).givenUp().orElseThrow());
		assertTrue(plan.marked(BlockPos.ZERO, Direction.NORTH).givenUp().isEmpty());
	}

	@Test
	void stepsBackOffTowardsTheEntrance() {
		north(8).forEach(step -> assertEquals(Direction.SOUTH, step.backOff(), "step: " + step));
	}

	private static List<MineLayout.Step> stripMine(int length, int branchLength) {
		return MineLayout.build(MinePlan.blank(MinePattern.STRIP_MINE, length, branchLength).marked(BlockPos.ZERO, Direction.NORTH), 0);
	}

	private static Set<BlockPos> digs(List<MineLayout.Step> steps) {
		return steps.stream().filter(step -> step.action() == MineLayout.Action.DIG).map(MineLayout.Step::pos).collect(Collectors.toSet());
	}

	@Test
	void stripMineIsACorridorWithTwoHighBranchesEveryThreeBlocks() {
		Set<BlockPos> expected = new HashSet<>();
		for (int slice = 0; slice < 8; slice++) {
			for (int side = -1; side <= 1; side++) {
				for (int up = 0; up < 3; up++) {
					expected.add(new BlockPos(side, up, -slice));
				}
			}
		}
		for (int slice : new int[] {2, 5}) {
			for (int cell = 2; cell < 6; cell++) {
				for (int up = 0; up < 2; up++) {
					expected.add(new BlockPos(cell, up, -slice));
					expected.add(new BlockPos(-cell, up, -slice));
				}
			}
		}
		assertEquals(expected, digs(stripMine(8, 4)));
	}

	@Test
	void stripMineDigsAStretchThenItsBranches() {
		List<BlockPos> digs = stripMine(8, 4).stream().filter(step -> step.action() == MineLayout.Action.DIG).map(MineLayout.Step::pos).toList();
		int lastOfStretch = digs.lastIndexOf(new BlockPos(1, 0, -2));
		int branch = digs.indexOf(new BlockPos(2, 1, -2));
		int nextStretch = digs.indexOf(new BlockPos(0, 2, -3));
		assertTrue(lastOfStretch < branch && branch < nextStretch, "order: " + lastOfStretch + ", " + branch + ", " + nextStretch);
	}

	@Test
	void stripMineNeedsNoRailsOrSupports() {
		for (MineLayout.Step step : stripMine(16, 8)) {
			assertTrue(EnumSet.of(MineLayout.Action.DIG, MineLayout.Action.FLOOR, MineLayout.Action.TORCH).contains(step.action()), "step: " + step);
			assertEquals(MineLayout.Needs.NONE, step.needs(), "step: " + step);
		}
	}

	@Test
	void stripMineBranchGivesUpOnlyItself() {
		List<MineLayout.Step> steps = stripMine(8, 4);
		MineLayout.Step inRight = steps.stream().filter(step -> step.pos().equals(new BlockPos(3, 1, -2))).findFirst().orElseThrow();
		MineLayout.Step firstOfLeft = steps.stream().filter(step -> step.pos().equals(new BlockPos(-2, 1, -2))).findFirst().orElseThrow();
		assertSame(firstOfLeft, steps.get(inRight.resumeAt()));
		MineLayout.Step inLeft = steps.stream().filter(step -> step.pos().equals(new BlockPos(-5, 0, -2))).findFirst().orElseThrow();
		assertEquals(new BlockPos(0, 2, -3), steps.get(inLeft.resumeAt()).pos(), "the next stretch of corridor");
	}

	@Test
	void stripMineCorridorGivesUpTheRestOfTheMine() {
		List<MineLayout.Step> steps = stripMine(8, 4);
		for (MineLayout.Step step : steps) {
			if (Math.abs(step.pos().getX()) <= 1) {
				assertEquals(steps.size(), step.resumeAt(), "corridor step: " + step);
			}
		}
	}

	@Test
	void stripMineBranchBacksOffTowardsTheCorridor() {
		List<MineLayout.Step> steps = stripMine(8, 4);
		steps.stream().filter(step -> step.pos().getX() > 1).forEach(step -> assertEquals(Direction.WEST, step.backOff(), "right: " + step));
		steps.stream().filter(step -> step.pos().getX() < -1).forEach(step -> assertEquals(Direction.EAST, step.backOff(), "left: " + step));
		steps.stream().filter(step -> Math.abs(step.pos().getX()) <= 1).forEach(step -> assertEquals(Direction.SOUTH, step.backOff(), "corridor: " + step));
	}

	@Test
	void stripMineHangsItsTorchesOnSolidWalls() {
		Set<BlockPos> dug = digs(stripMine(32, 16));
		List<MineLayout.Step> torches = stripMine(32, 16).stream().filter(step -> step.action() == MineLayout.Action.TORCH).toList();
		assertTrue(torches.stream().anyMatch(torch -> Math.abs(torch.pos().getX()) > 1), "branches get torches");
		assertTrue(torches.stream().anyMatch(torch -> Math.abs(torch.pos().getX()) <= 1), "the corridor gets torches");
		for (MineLayout.Step torch : torches) {
			assertTrue(dug.contains(torch.pos()), "torch in rock: " + torch);
			assertTrue(!dug.contains(torch.pos().relative(torch.facing().getOpposite())), "nothing to hang on: " + torch);
		}
	}

	@Test
	void stripMineStaysInsideItsRangeWhateverThePlanSays() {
		assertEquals(digs(stripMine(8, 4)), digs(stripMine(-3, -100)));
		assertEquals(digs(stripMine(128, 32)), digs(stripMine(100000, 100000)));
	}
}

package dev.bettercoppers.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}

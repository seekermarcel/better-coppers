package dev.bettercoppers.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
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

package dev.bettercoppers.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MineLayoutTest {
	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void layoutFollowsPlan() {
		MinePlan plan = MinePlan.blank(MinePattern.MINESHAFT, 16).marked(BlockPos.ZERO, Direction.NORTH);
		List<MineLayout.Step> steps = MineLayout.build(plan, 0);
		assertEquals(MineLayout.Action.DIG, steps.getFirst().action());
		assertEquals(BlockPos.ZERO.above(2), steps.getFirst().pos());
		for (MineLayout.Step step : steps) {
			assertTrue(MineLayout.sliceOf(plan, step.pos()) >= 0 && MineLayout.sliceOf(plan, step.pos()) < 16, "outside the plan: " + step.pos());
		}
	}

	@Test
	void sliceNeedsMatchTheSteps() {
		MinePlan plan = MinePlan.blank(MinePattern.MINESHAFT, 16).marked(BlockPos.ZERO, Direction.NORTH);
		List<MineLayout.Step> steps = MineLayout.build(plan, 0);
		for (int slice = 0; slice < 16; slice++) {
			int current = slice;
			MineLayout.SliceNeeds needs = MineLayout.needs(slice);
			assertEquals(slice + 1, needs.rails());
			assertEquals(steps.stream().filter(step -> step.action() == MineLayout.Action.FENCE && MineLayout.sliceOf(plan, step.pos()) == current).count(), needs.fences());
			assertEquals(steps.stream().filter(step -> step.action() == MineLayout.Action.BEAM && MineLayout.sliceOf(plan, step.pos()) == current).count(), needs.planks());
		}
		assertEquals(new MineLayout.SliceNeeds(3, 4, 3), MineLayout.needs(2));
		assertEquals(new MineLayout.SliceNeeds(1, 0, 0), MineLayout.needs(0));
	}
}

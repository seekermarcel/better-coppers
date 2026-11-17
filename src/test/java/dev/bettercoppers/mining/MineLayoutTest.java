package dev.bettercoppers.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.bettercoppers.golem.Golems;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Sections grown north from an Open End at the origin: forward is -z, right is +x, the parent's last slice is z = 1. */
class MineLayoutTest {
	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	private static List<MineLayout.Step> north(net.minecraft.resources.Identifier kind) {
		return MineLayout.build(kind, new Mines.OpenEnd(BlockPos.ZERO, Direction.NORTH));
	}

	private static List<BlockPos> positions(List<MineLayout.Step> steps, MineLayout.Action action) {
		return steps.stream().filter(step -> step.action() == action).map(MineLayout.Step::pos).toList();
	}

	private static void assertAllInReach(List<MineLayout.Step> steps) {
		for (MineLayout.Step step : steps) {
			var golem = EntityTypes.COPPER_GOLEM.getDimensions().makeBoundingBox(Vec3.atBottomCenterOf(step.stand()));
			assertTrue(Golems.isInReach(golem, step.pos()), "out of reach: " + step);
		}
	}

	@Test
	void straightIsEightSlicesWithTwoSupportsAndATorch() {
		List<MineLayout.Step> steps = north(MineLayout.STRAIGHT);
		assertEquals(72, positions(steps, MineLayout.Action.DIG).size());
		assertEquals(24, positions(steps, MineLayout.Action.FLOOR).size());
		assertEquals(List.of(
			new BlockPos(-1, 0, -2), new BlockPos(-1, 1, -2), new BlockPos(1, 0, -2), new BlockPos(1, 1, -2),
			new BlockPos(-1, 0, -6), new BlockPos(-1, 1, -6), new BlockPos(1, 0, -6), new BlockPos(1, 1, -6)
		), positions(steps, MineLayout.Action.FENCE));
		assertEquals(6, positions(steps, MineLayout.Action.BEAM).size());
		MineLayout.Step torch = steps.stream().filter(step -> step.action() == MineLayout.Action.TORCH).findFirst().orElseThrow();
		assertEquals(new BlockPos(0, 2, -3), torch.pos());
		assertEquals(Direction.NORTH, torch.facing());
		assertEquals(new BlockPos(0, 0, 1), steps.getFirst().stand(), "starts from the parent's last slice");
	}

	@Test
	void straightLaysItsRailsLastFromTheFarEndBack() {
		List<MineLayout.Step> steps = north(MineLayout.STRAIGHT);
		List<BlockPos> rails = positions(steps, MineLayout.Action.RAIL);
		assertEquals(8, rails.size());
		assertEquals(new BlockPos(0, 0, -7), rails.getFirst());
		assertEquals(BlockPos.ZERO, rails.getLast());
		assertEquals(MineLayout.Action.RAIL, steps.getLast().action());
	}

	@Test
	void railsAreNeverNeededToDig() {
		for (MineLayout.Step step : north(MineLayout.STRAIGHT)) {
			if (step.action() == MineLayout.Action.DIG && step.pos().getZ() == -2) {
				assertEquals(new MineLayout.Needs(4, 3), step.needs(), "a support slice needs its fences and planks");
			}
		}
		north(MineLayout.STAIRS_DOWN).forEach(step -> assertEquals(MineLayout.Needs.NONE, step.needs(), "step: " + step));
	}

	@Test
	void stairsDownAreFourStepsAndALandingFourLower() {
		List<MineLayout.Step> steps = north(MineLayout.STAIRS_DOWN);
		assertEquals(57, positions(steps, MineLayout.Action.DIG).size());
		// Each step is dug from the one above it, top block first.
		assertEquals(List.of(new BlockPos(0, 2, 0), new BlockPos(0, 1, 0), new BlockPos(0, 0, 0), new BlockPos(0, -1, 0)), positions(steps, MineLayout.Action.DIG).subList(0, 4));
		List<BlockPos> floors = positions(steps, MineLayout.Action.FLOOR);
		assertEquals(new BlockPos(-1, -2, 0), floors.getFirst());
		assertEquals(new BlockPos(1, -5, -4), floors.getLast());
		assertEquals(List.of(new BlockPos(0, -4, -4), new BlockPos(0, -4, -3), new BlockPos(0, -3, -2), new BlockPos(0, -2, -1), new BlockPos(0, -1, 0)),
			positions(steps, MineLayout.Action.RAIL));
		MineLayout.Step torch = steps.stream().filter(step -> step.action() == MineLayout.Action.TORCH).findFirst().orElseThrow();
		assertEquals(new BlockPos(-1, -2, -2), torch.pos());
		assertEquals(Direction.EAST, torch.facing(), "hangs on the left wall");
		assertTrue(positions(steps, MineLayout.Action.DIG).stream().noneMatch(pos -> pos.getZ() == -4 && pos.getY() > -2), "the landing is 3 high");
	}

	@Test
	void everyStepIsInReachOfWhereTheGolemStands() {
		assertAllInReach(north(MineLayout.STRAIGHT));
		assertAllInReach(north(MineLayout.STAIRS_DOWN));
	}

	@Test
	void turnsWithTheOpenEnd() {
		List<MineLayout.Step> steps = MineLayout.build(MineLayout.STRAIGHT, new Mines.OpenEnd(new BlockPos(10, 5, 10), Direction.EAST));
		assertEquals(new BlockPos(17, 5, 10), positions(steps, MineLayout.Action.RAIL).getFirst());
		assertEquals(new BlockPos(9, 5, 10), steps.getFirst().stand());
	}
}

package dev.bettercoppers.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.bettercoppers.golem.Golems;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
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

	private static List<MineLayout.Step> north(Identifier kind) {
		return MineLayout.build(kind, new Mines.OpenEnd(BlockPos.ZERO, Direction.NORTH), false);
	}

	private static List<Mines.OpenEnd> ends(Identifier kind, boolean mirrored) {
		return MineLayout.of(kind, new Mines.OpenEnd(BlockPos.ZERO, Direction.NORTH), mirrored).openEnds();
	}

	private static Mines.OpenEnd end(int x, int y, int z, Direction facing) {
		return new Mines.OpenEnd(new BlockPos(x, y, z), facing);
	}

	private static MineLayout.Step torch(List<MineLayout.Step> steps) {
		return steps.stream().filter(step -> step.action() == MineLayout.Action.TORCH).findFirst().orElseThrow();
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
		for (Identifier kind : MineLayout.KINDS) {
			assertAllInReach(north(kind));
		}
		assertAllInReach(MineLayout.build(MineLayout.CORNER, new Mines.OpenEnd(BlockPos.ZERO, Direction.NORTH), true));
	}

	@Test
	void noStepStandsInRockOrInASupport() {
		for (Identifier kind : MineLayout.KINDS) {
			List<MineLayout.Step> steps = north(kind);
			List<BlockPos> dug = positions(steps, MineLayout.Action.DIG);
			java.util.Set<BlockPos> built = new java.util.HashSet<>();
			for (MineLayout.Step step : steps) {
				// The parent's last slice (z = 1) is already dug.
				assertTrue(step.stand().getZ() == 1 || dug.contains(step.stand()), kind + ": stands in rock " + step);
				assertTrue(!built.contains(step.stand()), kind + ": stands in a support " + step);
				if (step.action() == MineLayout.Action.FENCE || step.action() == MineLayout.Action.BEAM) {
					built.add(step.pos());
				}
			}
		}
	}

	@Test
	void eachKindLeavesItsOpenEnds() {
		assertEquals(List.of(end(0, 0, -8, Direction.NORTH)), ends(MineLayout.STRAIGHT, false));
		assertEquals(List.of(end(2, 0, -1, Direction.EAST)), ends(MineLayout.CORNER, false));
		assertEquals(List.of(end(-2, 0, -1, Direction.WEST)), ends(MineLayout.CORNER, true));
		assertEquals(List.of(end(0, 0, -5, Direction.NORTH), end(-3, 0, -2, Direction.WEST), end(3, 0, -2, Direction.EAST)), ends(MineLayout.CROSSING, false));
		assertEquals(List.of(end(0, 0, -7, Direction.NORTH), end(-3, 0, -3, Direction.WEST), end(3, 0, -3, Direction.EAST)), ends(MineLayout.SMALL_HALL, false));
		assertEquals(List.of(end(0, -4, -5, Direction.NORTH)), ends(MineLayout.STAIRS_DOWN, false));
		assertEquals(List.of(end(0, 4, -6, Direction.NORTH)), ends(MineLayout.STAIRS_UP, false));
		assertEquals(List.of(), ends(MineLayout.DEAD_END, false));
	}

	@Test
	void theBoxIsTheFootprintAndItsFloor() {
		var box = MineLayout.of(MineLayout.STRAIGHT, new Mines.OpenEnd(BlockPos.ZERO, Direction.NORTH), false).box();
		assertEquals(new net.minecraft.world.level.levelgen.structure.BoundingBox(-1, -1, -7, 1, 2, 0), box);
	}

	@Test
	void cornerTurnsRightOrMirroredLeftAndCurvesItsRails() {
		List<MineLayout.Step> right = north(MineLayout.CORNER);
		assertEquals(27, positions(right, MineLayout.Action.DIG).size());
		assertEquals(List.of(new BlockPos(1, 0, -1), new BlockPos(0, 0, -1), BlockPos.ZERO), positions(right, MineLayout.Action.RAIL));
		assertTrue(positions(right, MineLayout.Action.FENCE).isEmpty(), "no support in a corner");
		List<MineLayout.Step> left = MineLayout.build(MineLayout.CORNER, new Mines.OpenEnd(BlockPos.ZERO, Direction.NORTH), true);
		assertEquals(List.of(new BlockPos(-1, 0, -1), new BlockPos(0, 0, -1), BlockPos.ZERO), positions(left, MineLayout.Action.RAIL));
	}

	@Test
	void crossingHasPlankPillarsAtItsInnerCorners() {
		List<MineLayout.Step> steps = north(MineLayout.CROSSING);
		assertEquals(63, positions(steps, MineLayout.Action.DIG).size());
		assertEquals(21, positions(steps, MineLayout.Action.FLOOR).size());
		List<BlockPos> planks = positions(steps, MineLayout.Action.BEAM);
		assertEquals(12, planks.size());
		for (BlockPos corner : List.of(new BlockPos(-1, 0, -1), new BlockPos(1, 0, -1), new BlockPos(-1, 0, -3), new BlockPos(1, 0, -3))) {
			for (int up = 0; up < 3; up++) {
				assertTrue(planks.contains(corner.above(up)), "no pillar at " + corner.above(up));
			}
		}
		assertEquals(new BlockPos(0, 2, -1), torch(steps).pos());
		assertEquals(Direction.WEST, torch(steps).facing(), "hangs on the right pillar");
		assertEquals(5, positions(steps, MineLayout.Action.RAIL).size());
		assertTrue(steps.stream().filter(step -> step.action() == MineLayout.Action.DIG && step.pos().getZ() == -1).allMatch(step -> step.needs().planks() == 6), "a pillar slice needs its planks");
	}

	@Test
	void smallHallHasTwoWideSupportsAndTwoTorches() {
		List<MineLayout.Step> steps = north(MineLayout.SMALL_HALL);
		assertEquals(105, positions(steps, MineLayout.Action.DIG).size());
		assertEquals(8, positions(steps, MineLayout.Action.FENCE).size());
		assertTrue(positions(steps, MineLayout.Action.FENCE).stream().allMatch(pos -> Math.abs(pos.getX()) == 2 && (pos.getZ() == -1 || pos.getZ() == -5)));
		assertEquals(10, positions(steps, MineLayout.Action.BEAM).size());
		List<MineLayout.Step> torches = steps.stream().filter(step -> step.action() == MineLayout.Action.TORCH).toList();
		assertEquals(List.of(new BlockPos(0, 2, -2), new BlockPos(0, 2, -4)), torches.stream().map(MineLayout.Step::pos).toList());
		assertEquals(List.of(Direction.NORTH, Direction.SOUTH), torches.stream().map(MineLayout.Step::facing).toList());
		// The second torch hangs on the far beam, so it comes after that beam.
		assertTrue(steps.indexOf(torches.getLast()) > steps.indexOf(steps.stream().filter(step -> step.pos().equals(new BlockPos(0, 2, -5)) && step.action() == MineLayout.Action.BEAM).findFirst().orElseThrow()));
		assertEquals(7, positions(steps, MineLayout.Action.RAIL).size());
	}

	@Test
	void stairsUpAreAFlatSliceFourStepsAndALandingFourHigher() {
		List<MineLayout.Step> steps = north(MineLayout.STAIRS_UP);
		List<BlockPos> dug = positions(steps, MineLayout.Action.DIG);
		assertEquals(66, dug.size());
		int[] feet = {0, 1, 2, 3, 4, 4};
		for (int f = 0; f < feet.length; f++) {
			for (int up = 0; up < (f < 4 ? 4 : 3); up++) {
				assertTrue(dug.contains(new BlockPos(0, feet[f] + up, -f)), "not dug: f" + f + " y" + (feet[f] + up));
			}
		}
		assertEquals(List.of(new BlockPos(0, 4, -5), new BlockPos(0, 4, -4), new BlockPos(0, 3, -3), new BlockPos(0, 2, -2), new BlockPos(0, 1, -1), BlockPos.ZERO),
			positions(steps, MineLayout.Action.RAIL));
		assertEquals(new BlockPos(-1, 4, -3), torch(steps).pos());
		assertEquals(Direction.EAST, torch(steps).facing());
		assertTrue(dug.stream().noneMatch(pos -> pos.getZ() > 0), "nothing dug in the parent");
	}

	@Test
	void deadEndHasOneSupportAndATorchOnItsFace() {
		List<MineLayout.Step> steps = north(MineLayout.DEAD_END);
		assertEquals(36, positions(steps, MineLayout.Action.DIG).size());
		assertEquals(4, positions(steps, MineLayout.Action.FENCE).size());
		assertEquals(3, positions(steps, MineLayout.Action.BEAM).size());
		assertEquals(new BlockPos(0, 1, -3), torch(steps).pos());
		assertEquals(Direction.SOUTH, torch(steps).facing());
		assertEquals(3, positions(steps, MineLayout.Action.RAIL).size());
	}

	@Test
	void turnsWithTheOpenEnd() {
		List<MineLayout.Step> steps = MineLayout.build(MineLayout.STRAIGHT, new Mines.OpenEnd(new BlockPos(10, 5, 10), Direction.EAST), false);
		assertEquals(new BlockPos(17, 5, 10), positions(steps, MineLayout.Action.RAIL).getFirst());
		assertEquals(new BlockPos(9, 5, 10), steps.getFirst().stand());
	}
}

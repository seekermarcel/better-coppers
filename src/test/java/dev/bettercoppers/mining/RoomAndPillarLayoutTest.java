package dev.bettercoppers.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Heading north from the origin, a block's row is how far north it is and its side how far east. */
class RoomAndPillarLayoutTest {
	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	private static List<MineLayout.Step> hall(int length, int width) {
		return MineLayout.build(MinePlan.blank(MinePattern.ROOM_AND_PILLAR, length, width).marked(BlockPos.ZERO, Direction.NORTH), 0);
	}

	private static BlockPos at(int row, int side, int up) {
		return new BlockPos(side, up, -row);
	}

	private static long count(List<MineLayout.Step> steps, MineLayout.Action action) {
		return steps.stream().filter(step -> step.action() == action).count();
	}

	private static boolean digs(List<MineLayout.Step> steps, BlockPos pos) {
		return steps.stream().anyMatch(step -> step.action() == MineLayout.Action.DIG && step.pos().equals(pos));
	}

	@Test
	void aSmallHallLeavesItsFourPillarsStanding() {
		List<MineLayout.Step> steps = hall(8, 9);
		// 8 rows of 9 columns, 3 high, less the 4 pillars in rows 2 and 6.
		assertEquals(204, count(steps, MineLayout.Action.DIG));
		for (int row : new int[] {2, 6}) {
			for (int side : new int[] {-2, 2}) {
				for (int up = 0; up < 3; up++) {
					assertFalse(digs(steps, at(row, side, up)), "dug the pillar at row " + row + ", side " + side);
				}
			}
		}
	}

	@Test
	void theLargestHallsHaveTheTorchesOfTheDesign() {
		// Rows 2, 6, ... 62 hold pillars, every other one of them torches.
		assertEquals(3072, count(hall(64, 17), MineLayout.Action.DIG));
		assertEquals(32, count(hall(64, 17), MineLayout.Action.TORCH));
		assertEquals(1632, count(hall(64, 9), MineLayout.Action.DIG));
		assertEquals(16, count(hall(64, 9), MineLayout.Action.TORCH));
	}

	@Test
	void aWideHallHasFourPillarsInARow() {
		List<MineLayout.Step> steps = hall(8, 17);
		for (int side : new int[] {-6, -2, 2, 6}) {
			assertFalse(digs(steps, at(2, side, 1)), "dug the pillar at side " + side);
		}
	}

	private static MineLayout.Step dig(List<MineLayout.Step> steps, int row, int side) {
		return steps.stream().filter(step -> step.action() == MineLayout.Action.DIG && step.pos().equals(at(row, side, 0))).findFirst().orElseThrow();
	}

	@Test
	void aRowGoesFromTheCentreToOneSideThenTheOther() {
		List<BlockPos> digs = hall(8, 9).stream().filter(step -> step.action() == MineLayout.Action.DIG && step.pos().getY() == 2).map(MineLayout.Step::pos).toList();
		assertEquals(List.of(at(0, 0, 2), at(0, 1, 2), at(0, 2, 2), at(0, 3, 2), at(0, 4, 2), at(0, -1, 2), at(0, -2, 2), at(0, -3, 2), at(0, -4, 2), at(1, 0, 2)), digs.subList(0, 10));
	}

	@Test
	void theGolemStandsInThePartOfTheRowJustDug() {
		List<MineLayout.Step> steps = hall(8, 9);
		assertEquals(at(-1, 0, 0), dig(steps, 0, 0).stand(), "the centre is dug from the row behind");
		assertEquals(at(3, 0, 0), dig(steps, 3, 1).stand());
		assertEquals(at(3, 3, 0), dig(steps, 3, 4).stand());
		assertEquals(at(3, -3, 0), dig(steps, 3, -4).stand());
	}

	@Test
	void behindAPillarTheGolemStandsInTheRowBehind() {
		List<MineLayout.Step> steps = hall(8, 17);
		assertEquals(at(1, 3, 0), dig(steps, 2, 3).stand());
		assertEquals(at(1, -7, 0), dig(steps, 2, -7).stand());
		assertEquals(at(2, 4, 0), dig(steps, 2, 5).stand(), "not behind a pillar any more");
	}

	@Test
	void torchesHangOnTheEntranceSideOfEveryOtherPillarRow() {
		List<MineLayout.Step> torches = hall(16, 9).stream().filter(step -> step.action() == MineLayout.Action.TORCH).toList();
		assertEquals(List.of(at(1, 2, 1), at(1, -2, 1), at(9, 2, 1), at(9, -2, 1)), torches.stream().map(MineLayout.Step::pos).toList());
		torches.forEach(torch -> assertEquals(Direction.SOUTH, torch.facing(), "points away from its pillar"));
		assertEquals(at(2, 1, 0), torches.getFirst().stand(), "beside the pillar, in the part just dug");
	}

	@Test
	void everyColumnLaysItsFloorBeforeTheNextColumnStandsOnIt() {
		List<MineLayout.Step> steps = hall(8, 9);
		assertEquals(68, count(steps, MineLayout.Action.FLOOR));
		assertEquals(new MineLayout.Step(MineLayout.Action.FLOOR, at(0, 1, -1), at(0, 0, 0), Direction.UP, Direction.SOUTH, MineLayout.Needs.NONE, steps.get(7).resumeAt()), steps.get(7));
	}

	private static int indexOfDig(List<MineLayout.Step> steps, int row, int side) {
		return steps.indexOf(dig(steps, row, side));
	}

	@Test
	void aHazardInAHalfRowGivesUpOnlyTheOuterPartOfIt() {
		List<MineLayout.Step> steps = hall(8, 9);
		int otherHalf = indexOfDig(steps, 3, -1) - 2;
		int nextRow = indexOfDig(steps, 4, 0) - 2;
		assertEquals(otherHalf, dig(steps, 3, 1).resumeAt());
		assertEquals(otherHalf, dig(steps, 3, 4).resumeAt());
		assertEquals(nextRow, dig(steps, 3, -1).resumeAt());
		assertEquals(nextRow, dig(steps, 3, -4).resumeAt());
		assertEquals(MineLayout.Action.DIG, steps.get(otherHalf).action());
		assertEquals(at(3, -1, 2), steps.get(otherHalf).pos());
		assertEquals(at(4, 0, 2), steps.get(nextRow).pos());
	}

	@Test
	void aHazardInTheCentreColumnGivesUpTheRestOfTheHall() {
		List<MineLayout.Step> steps = hall(8, 9);
		assertEquals(steps.size(), dig(steps, 0, 0).resumeAt());
		assertEquals(steps.size(), dig(steps, 5, 0).resumeAt());
	}

	@Test
	void aPlanEditedOutOfRangeStillMakesAHallOfTheDesign() {
		assertEquals(hall(64, 9), hall(128, 9), "longer than the pattern allows");
		assertEquals(hall(8, 9), hall(8, 0), "no setting at all");
		assertEquals(hall(8, 9), hall(8, 13), "between the two widths");
		assertEquals(hall(8, 17), hall(8, 1000), "wider than the pattern allows");
		assertEquals(hall(8, 9), MineLayout.build(new MinePlan(MinePattern.ROOM_AND_PILLAR, -5, 9, java.util.Optional.of(BlockPos.ZERO), java.util.Optional.of(Direction.NORTH), 0, java.util.Optional.empty()), 0), "shorter than any plan");
	}

	@Test
	void aHallNeedsNoSupplies() {
		hall(8, 9).forEach(step -> assertEquals(MineLayout.Needs.NONE, step.needs()));
	}
}

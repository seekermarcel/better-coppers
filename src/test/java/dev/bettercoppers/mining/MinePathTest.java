package dev.bettercoppers.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

class MinePathTest {
	@Test
	void points_in_a_line_merge_into_one_leg() {
		List<BlockPos> points = List.of(new BlockPos(0, 0, 0), new BlockPos(0, 0, 1), new BlockPos(0, 0, 9), new BlockPos(0, 0, 17));
		assertEquals(List.of(new BlockPos(0, 0, 0), new BlockPos(0, 0, 17)), MinePath.legs(points));
	}

	@Test
	void a_turn_breaks_the_leg() {
		List<BlockPos> points = List.of(new BlockPos(0, 0, 0), new BlockPos(0, 0, 8), new BlockPos(8, 0, 8), new BlockPos(16, 0, 8));
		assertEquals(List.of(new BlockPos(0, 0, 0), new BlockPos(0, 0, 8), new BlockPos(16, 0, 8)), MinePath.legs(points));
	}

	@Test
	void no_leg_is_longer_than_32_blocks() {
		List<BlockPos> points = List.of(new BlockPos(0, 0, 0), new BlockPos(0, 0, 8), new BlockPos(0, 0, 16), new BlockPos(0, 0, 24),
			new BlockPos(0, 0, 32), new BlockPos(0, 0, 40));
		assertEquals(List.of(new BlockPos(0, 0, 0), new BlockPos(0, 0, 32), new BlockPos(0, 0, 40)), MinePath.legs(points));
	}

	@Test
	void the_path_runs_from_the_opening_along_the_parents_to_the_target() {
		Mines mines = new Mines();
		BlockPos beam = new BlockPos(0, 70, 0);
		mines.register(beam, Direction.Axis.X);
		Mines.Mine mine = mines.start(mines.find(beam, Direction.Axis.X).orElseThrow(), Direction.SOUTH, MineLayout.STRAIGHT);
		mine = mines.grow(mine, mine.ends().getFirst(), MineLayout.CORNER, false);
		Mines.OpenEnd target = mine.ends().getFirst();
		BlockPos corner = mine.sections().get(1).start().pos();
		assertEquals(List.of(beam.below(2), corner, target.pos()), MinePath.to(mine, target));
	}
}

package dev.bettercoppers.mining;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

class MineLimitsTest {
	private static final BlockPos BEAM = new BlockPos(0, 12, 0);
	/** In front of the opening, two below the beam, heading north. */
	private static final Mines.OpenEnd END = new Mines.OpenEnd(new BlockPos(0, 10, -1), Direction.NORTH);

	@Test
	void a_straight_reaches_8_blocks_from_the_beam() {
		assertTrue(new MineLimits(8, -64, 32).allows(BEAM, MineLayout.build(MineLayout.STRAIGHT, END)));
		assertFalse(new MineLimits(7, -64, 32).allows(BEAM, MineLayout.build(MineLayout.STRAIGHT, END)));
	}

	/** The landing is dug from y 6 up; the floor laid under it at y 5 is not dug, so it doesn't count. */
	@Test
	void stairs_down_dig_to_4_below_the_open_end() {
		assertTrue(new MineLimits(64, 6, 32).allows(BEAM, MineLayout.build(MineLayout.STAIRS_DOWN, END)));
		assertFalse(new MineLimits(64, 7, 32).allows(BEAM, MineLayout.build(MineLayout.STAIRS_DOWN, END)));
	}
}

package dev.bettercoppers.courier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class TrailTest {
	/** Walks east one block at a time, so a crumb lands every {@link Trail#SPACING} blocks. */
	private static Trail walked(int blocks) {
		Trail trail = new Trail();
		for (int x = 0; x <= blocks; x++) {
			trail.record(new BlockPos(x, 0, 0));
		}
		return trail;
	}

	@Test
	void eachEscapeInARowGoesFurtherBack() {
		Trail trail = walked(200);
		// Crumbs at 0, 4, ..., 200; only the last 24 are kept, back to x = 108.
		assertEquals(new BlockPos(172, 0, 0), trail.backtrack(1));
		assertEquals(new BlockPos(108, 0, 0), trail.backtrack(2));
		assertNull(trail.backtrack(3));
	}

	@Test
	void aShortTrailGoesBackToItsStart() {
		assertEquals(new BlockPos(0, 0, 0), walked(10).backtrack(1));
		assertNull(new Trail().backtrack(1));
	}
}

package dev.bettercoppers.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The blocked check on a fake world of stone. The section is a Straight heading north from (0, 10, 0): it digs
 * z 0..-7, x -1..1, y 10..12, and its shell is one block around that, the parent's last slice (z 1) left out.
 */
class BlockedTest {
	private static final BlockPos BEAM = new BlockPos(0, 12, 2);
	private static final Mines.OpenEnd END = new Mines.OpenEnd(new BlockPos(0, 10, 0), Direction.NORTH);
	private static final MineLimits LIMITS = new MineLimits(64, -59, 32);
	private static final Predicate<BoundingBox> FREE = box -> false;

	@BeforeAll
	static void bootstrap() {
		net.minecraft.SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	private static final class Fake implements Blocked.Site {
		final Map<BlockPos, BlockState> blocks = new HashMap<>();
		final Set<BlockPos> sky = new HashSet<>();
		final Set<BlockPos> structure = new HashSet<>();
		final Set<BlockPos> unloaded = new HashSet<>();

		@Override
		public @Nullable BlockState state(BlockPos pos) {
			return this.unloaded.contains(pos) ? null : this.blocks.getOrDefault(pos, Blocks.STONE.defaultBlockState());
		}

		@Override
		public boolean seesSky(BlockPos pos) {
			return this.sky.contains(pos);
		}

		@Override
		public boolean inStructure(BlockPos pos) {
			return this.structure.contains(pos);
		}

		@Override
		public boolean diggable(BlockState state) {
			return !state.is(Blocks.OBSIDIAN);
		}

		Fake set(BlockState state, BlockPos... cells) {
			for (BlockPos cell : cells) {
				this.blocks.put(cell, state);
			}
			return this;
		}
	}

	private static Mines.Section straight(int parent) {
		return Mines.Section.of(MineLayout.STRAIGHT, END, false, parent);
	}

	private static @Nullable BlockedReason check(Fake site) {
		return Blocked.check(site, straight(0), LIMITS, BEAM, FREE);
	}

	private static BlockPos[] cells(int count, int x, int y) {
		BlockPos[] cells = new BlockPos[count];
		for (int i = 0; i < count; i++) {
			cells[i] = new BlockPos(x, y, -i);
		}
		return cells;
	}

	@Test
	void solid_rock_fits() {
		assertNull(check(new Fake()));
	}

	@Test
	void any_lava_blocks() {
		assertEquals(BlockedReason.LAVA, check(new Fake().set(Blocks.LAVA.defaultBlockState(), new BlockPos(2, 11, -4))), "lava in the wall");
	}

	@Test
	void more_than_4_water_cells_block() {
		BlockState water = Blocks.WATER.defaultBlockState();
		assertNull(check(new Fake().set(water, cells(4, 2, 11))), "4 water cells are plugged");
		assertEquals(BlockedReason.WATER, check(new Fake().set(water, cells(5, 2, 11))));
	}

	@Test
	void more_than_4_open_shell_cells_are_a_cave() {
		BlockState air = Blocks.AIR.defaultBlockState();
		assertNull(check(new Fake().set(air, cells(4, -2, 11))));
		assertEquals(BlockedReason.CAVE, check(new Fake().set(air, cells(5, -2, 11))));
		Fake parent = new Fake();
		for (int x = -2; x <= 2; x++) {
			parent.set(air, new BlockPos(x, 10, 1), new BlockPos(x, 11, 1), new BlockPos(x, 12, 1));
		}
		assertNull(check(parent), "the parent's last slice is dug out and doesn't count");
	}

	@Test
	void a_ceiling_cell_under_the_sky_is_the_surface() {
		Fake site = new Fake();
		site.sky.add(new BlockPos(0, 13, -6));
		assertEquals(BlockedReason.SURFACE, check(site));
		assertNull(Blocked.check(site, straight(-1), LIMITS, BEAM, FREE), "the first section may start at the surface");
	}

	@Test
	void open_shell_cells_under_the_sky_are_no_cave_for_the_first_section() {
		Fake site = new Fake();
		for (int x = -1; x <= 1; x++) {
			for (int z = 0; z >= -7; z--) {
				BlockPos above = new BlockPos(x, 13, z);
				site.set(Blocks.AIR.defaultBlockState(), above);
				site.sky.add(above);
			}
		}
		assertNull(Blocked.check(site, straight(-1), LIMITS, BEAM, FREE));
		assertEquals(BlockedReason.SURFACE, check(site), "but not for a later one");
	}

	@Test
	void an_overlap_is_another_tunnel() {
		assertEquals(BlockedReason.ANOTHER_TUNNEL, Blocked.check(new Fake(), straight(0), LIMITS, BEAM, box -> true));
	}

	@Test
	void a_cell_in_a_structure_piece_blocks() {
		Fake site = new Fake();
		site.structure.add(new BlockPos(1, 12, -3));
		assertEquals(BlockedReason.STRUCTURE, check(site));
	}

	@Test
	void undiggable_rock_is_hard_rock() {
		assertEquals(BlockedReason.HARD_ROCK, check(new Fake().set(Blocks.OBSIDIAN.defaultBlockState(), new BlockPos(0, 11, -2))));
		assertNull(check(new Fake().set(Blocks.OBSIDIAN.defaultBlockState(), new BlockPos(0, 9, -2))), "the floor under it is not dug");
	}

	@Test
	void the_mine_range_and_the_depth_floor_block() {
		assertEquals(BlockedReason.TOO_FAR, Blocked.check(new Fake(), straight(0), new MineLimits(8, -59, 32), BEAM, FREE));
		assertEquals(BlockedReason.TOO_DEEP, Blocked.check(new Fake(), straight(0), new MineLimits(64, 11, 32), BEAM, FREE));
	}

	@Test
	void unloaded_cells_are_no_verdict() {
		Fake site = new Fake();
		assertTrue(Blocked.loaded(site, straight(0)));
		site.unloaded.add(new BlockPos(0, 13, -7));
		assertFalse(Blocked.loaded(site, straight(0)));
	}
}

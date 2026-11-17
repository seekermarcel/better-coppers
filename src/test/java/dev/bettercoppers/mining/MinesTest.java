package dev.bettercoppers.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

class MinesTest {
	private static Tag save(Mines mines) {
		return Mines.CODEC.encodeStart(NbtOps.INSTANCE, mines).getOrThrow();
	}

	private static Mines load(Tag tag) {
		return Mines.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
	}

	@Test
	void mines_survive_a_save_and_load() {
		Mines mines = new Mines();
		mines.register(new BlockPos(10, 64, -3), Direction.Axis.X);
		mines.register(new BlockPos(-7, 12, 40), Direction.Axis.Z);
		Mines loaded = load(save(mines));
		assertEquals(List.of(
			new Mines.Mine(new BlockPos(10, 64, -3), Direction.Axis.X, Optional.empty(), List.of(), List.of()),
			new Mines.Mine(new BlockPos(-7, 12, 40), Direction.Axis.Z, Optional.empty(), List.of(), List.of())), loaded.all());
	}

	@Test
	void sections_and_their_progress_survive_a_save_and_load() {
		Mines mines = new Mines();
		BlockPos beam = new BlockPos(3, 70, 3);
		mines.register(beam, Direction.Axis.X);
		Mines.Mine mine = mines.start(mines.find(beam, Direction.Axis.X).orElseThrow(), Direction.SOUTH, MineLayout.STAIRS_DOWN);
		mines.setProgress(mine, 0, 12, false);
		Mines.Mine loaded = load(save(mines)).find(beam, Direction.Axis.X).orElseThrow();
		assertEquals(Optional.of(Direction.SOUTH), loaded.facing());
		Mines.OpenEnd start = new Mines.OpenEnd(new BlockPos(3, 68, 4), Direction.SOUTH);
		assertEquals(List.of(new Mines.Section(MineLayout.STAIRS_DOWN, start, false, -1, MineLayout.of(MineLayout.STAIRS_DOWN, start, false).box(), 12, false)), loaded.sections());
		assertEquals(List.of(new Mines.OpenEnd(new BlockPos(3, 64, 9), Direction.SOUTH)), loaded.ends(), "the stairs' Open End");
	}

	@Test
	void a_mine_is_finished_with_no_open_ends_and_every_section_done() {
		Mines.OpenEnd start = new Mines.OpenEnd(BlockPos.ZERO, Direction.EAST);
		Mines.Section deadEnd = new Mines.Section(MineLayout.DEAD_END, start, false, -1, MineLayout.of(MineLayout.DEAD_END, start, false).box(), 0, false);
		Mines.OpenEnd end = new Mines.OpenEnd(new BlockPos(9, 0, 0), Direction.EAST);
		assertFalse(new Mines.Mine(BlockPos.ZERO, Direction.Axis.Z, Optional.empty(), List.of(), List.of()).isFinished(), "nothing dug yet");
		assertFalse(new Mines.Mine(BlockPos.ZERO, Direction.Axis.Z, Optional.empty(), List.of(deadEnd), List.of()).isFinished(), "a section is not done");
		assertFalse(new Mines.Mine(BlockPos.ZERO, Direction.Axis.Z, Optional.empty(), List.of(deadEnd.at(36, true)), List.of(end)).isFinished(), "an Open End is left");
		assertTrue(new Mines.Mine(BlockPos.ZERO, Direction.Axis.Z, Optional.empty(), List.of(deadEnd.at(36, true)), List.of()).isFinished());
	}

	@Test
	void an_open_end_rolls_the_same_section_every_time_and_knows_its_parent() {
		Mines mines = new Mines();
		mines.register(BlockPos.ZERO, Direction.Axis.Z);
		Mines.Mine mine = mines.start(mines.find(BlockPos.ZERO, Direction.Axis.Z).orElseThrow(), Direction.EAST, MineLayout.STRAIGHT);
		Mines.OpenEnd end = mine.ends().getFirst();
		assertEquals(0, mine.parentOf(end));
		Mines.Section rolled = mines.roll(42L, new MineLimits(64, -59, 32), mine, end);
		assertEquals(end, rolled.start());
		assertEquals(0, rolled.parent());
		assertEquals(rolled, mines.roll(42L, new MineLimits(64, -59, 32), mine, end));
	}

	@Test
	void a_rolled_section_that_overlaps_a_tunnel_is_dropped() {
		Mines mines = new Mines();
		mines.register(BlockPos.ZERO, Direction.Axis.Z);
		Mines.Mine mine = mines.start(mines.find(BlockPos.ZERO, Direction.Axis.Z).orElseThrow(), Direction.EAST, MineLayout.STRAIGHT);
		// An Open End facing back into the Straight: every kind overlaps it.
		Mines.OpenEnd back = new Mines.OpenEnd(new BlockPos(4, -2, 0), Direction.WEST);
		assertEquals(null, mines.roll(42L, new MineLimits(64, -59, 32), mine, back));
	}

	@Test
	void saved_data_names_its_format() {
		assertEquals(1, ((CompoundTag) save(new Mines())).getIntOr("format", 0));
	}

	@Test
	void registering_the_same_frame_twice_keeps_one_mine() {
		Mines mines = new Mines();
		assertTrue(mines.register(BlockPos.ZERO, Direction.Axis.X));
		assertFalse(mines.register(BlockPos.ZERO, Direction.Axis.X));
		assertTrue(mines.register(BlockPos.ZERO, Direction.Axis.Z), "the other axis is another mine");
		assertEquals(2, mines.all().size());
	}

	@Test
	void a_newer_format_is_left_alone() {
		CompoundTag newer = new CompoundTag();
		newer.putInt("format", 99);
		newer.putString("mines", "something only a later version understands");
		Mines loaded = load(newer);
		assertTrue(loaded.all().isEmpty());
		assertFalse(loaded.register(BlockPos.ZERO, Direction.Axis.X), "nothing is registered into data it can't read");
		assertFalse(loaded.isDirty());
		assertEquals(newer, save(loaded), "saving writes back what was read");
	}
}

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
			new Mines.Mine(new BlockPos(10, 64, -3), Direction.Axis.X, Optional.empty(), List.of()),
			new Mines.Mine(new BlockPos(-7, 12, 40), Direction.Axis.Z, Optional.empty(), List.of())), loaded.all());
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
		assertEquals(List.of(new Mines.Section(MineLayout.STAIRS_DOWN, new Mines.OpenEnd(new BlockPos(3, 68, 4), Direction.SOUTH), false, -1, 12, false)), loaded.sections());
	}

	@Test
	void a_mine_is_finished_once_its_first_section_is_done() {
		Mines mines = new Mines();
		mines.register(BlockPos.ZERO, Direction.Axis.Z);
		Mines.Mine mine = mines.find(BlockPos.ZERO, Direction.Axis.Z).orElseThrow();
		assertFalse(mine.isFinished(), "nothing dug yet");
		mine = mines.start(mine, Direction.EAST, MineLayout.STRAIGHT);
		assertFalse(mine.isFinished());
		mines.setProgress(mine, 0, 100, true);
		assertTrue(mines.find(BlockPos.ZERO, Direction.Axis.Z).orElseThrow().isFinished());
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

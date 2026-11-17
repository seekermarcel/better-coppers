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
			new Mines.Mine(new BlockPos(10, 64, -3), Direction.Axis.X, Optional.empty()),
			new Mines.Mine(new BlockPos(-7, 12, 40), Direction.Axis.Z, Optional.empty())), loaded.all());
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

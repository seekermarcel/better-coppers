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
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MinesTest {
	/** Solid stone everywhere, all loaded. */
	private static final Blocked.Site ROCK = new Blocked.Site() {
		@Override
		public BlockState state(BlockPos pos) {
			return Blocks.STONE.defaultBlockState();
		}

		@Override
		public boolean seesSky(BlockPos pos) {
			return false;
		}

		@Override
		public boolean inStructure(BlockPos pos) {
			return false;
		}

		@Override
		public boolean diggable(BlockState state) {
			return true;
		}
	};

	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

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
		assertEquals(List.of(new Mines.Section(MineLayout.STAIRS_DOWN, start, false, -1, MineLayout.of(MineLayout.STAIRS_DOWN, start, false).box(), 12, false, Optional.empty())), loaded.sections());
		assertEquals(List.of(new Mines.OpenEnd(new BlockPos(3, 64, 9), Direction.SOUTH)), loaded.ends(), "the stairs' Open End");
	}

	@Test
	void a_mine_is_finished_with_no_open_ends_and_every_section_done() {
		Mines.OpenEnd start = new Mines.OpenEnd(BlockPos.ZERO, Direction.EAST);
		Mines.Section deadEnd = new Mines.Section(MineLayout.DEAD_END, start, false, -1, MineLayout.of(MineLayout.DEAD_END, start, false).box(), 0, false, Optional.empty());
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
		Mines.Section rolled = mines.roll(42L, new MineLimits(64, -59, 32), ROCK, mine, end);
		assertEquals(end, rolled.start());
		assertEquals(0, rolled.parent());
		assertEquals(rolled, mines.roll(42L, new MineLimits(64, -59, 32), ROCK, mine, end));
	}

	@Test
	void an_open_end_where_every_try_overlaps_a_tunnel_gets_a_barrier() {
		Mines mines = new Mines();
		mines.register(BlockPos.ZERO, Direction.Axis.Z);
		Mines.Mine mine = mines.start(mines.find(BlockPos.ZERO, Direction.Axis.Z).orElseThrow(), Direction.EAST, MineLayout.STRAIGHT);
		// An Open End facing back into the Straight: every kind overlaps it.
		Mines.OpenEnd back = new Mines.OpenEnd(new BlockPos(4, -2, 0), Direction.WEST);
		Mines.Section barrier = mines.roll(42L, new MineLimits(64, -59, 32), ROCK, mine, back);
		assertEquals(MineLayout.BARRIER, barrier.kind());
		assertEquals(Optional.of(BlockedReason.ANOTHER_TUNNEL), barrier.reason());
		assertEquals(back, barrier.start());
	}

	private static Mines.Mine straightMine(Mines mines) {
		mines.register(BlockPos.ZERO, Direction.Axis.Z);
		return mines.start(mines.find(BlockPos.ZERO, Direction.Axis.Z).orElseThrow(), Direction.EAST, MineLayout.STRAIGHT);
	}

	@Test
	void a_players_fence_closes_an_open_end_and_breaking_it_opens_the_end_again() {
		Mines mines = new Mines();
		Mines.OpenEnd end = straightMine(mines).ends().getFirst();
		assertFalse(mines.close(end.pos()), "the fence goes in the slice behind the end");
		assertTrue(mines.close(MineLayout.middleFence(end)));
		Mines.Mine closed = mines.find(BlockPos.ZERO, Direction.Axis.Z).orElseThrow();
		assertEquals(List.of(), closed.ends());
		Mines.Section barrier = closed.sections().get(1);
		assertEquals(Optional.of(BlockedReason.PLAYER), barrier.reason());
		assertTrue(barrier.done());
		Mines.Mine loaded = load(save(mines)).find(BlockPos.ZERO, Direction.Axis.Z).orElseThrow();
		assertEquals(barrier, loaded.sections().get(1), "the reason is saved");
		assertTrue(mines.reopen(MineLayout.middleFence(end)));
		Mines.Mine open = mines.find(BlockPos.ZERO, Direction.Axis.Z).orElseThrow();
		assertEquals(List.of(end), open.ends());
		assertEquals(1, open.sections().size(), "the Barrier is gone");
	}

	@Test
	void a_section_cut_short_loses_its_ends_and_reopening_its_barrier_digs_it_on() {
		Mines mines = new Mines();
		Mines.Mine mine = straightMine(mines);
		// The middle top block of the fourth slice, dug standing in the third.
		MineLayout.Step step = MineLayout.build(mine.sections().getFirst()).stream()
			.filter(s -> s.pos().equals(new BlockPos(4, 0, 0))).findFirst().orElseThrow();
		mines.setProgress(mine, 0, 30, false);
		mines.cutShort(mine, 0, step, BlockedReason.LAVA);
		Mines.Mine cut = mines.find(BlockPos.ZERO, Direction.Axis.Z).orElseThrow();
		assertEquals(List.of(), cut.ends());
		Mines.Section barrier = cut.sections().get(1);
		assertEquals(new Mines.OpenEnd(new BlockPos(4, -2, 0), Direction.EAST), barrier.start(), "posts in the third slice, at its floor");
		assertEquals(0, barrier.parent());
		assertTrue(mines.reopen(new BlockPos(3, -2, 0)));
		Mines.Mine open = mines.find(BlockPos.ZERO, Direction.Axis.Z).orElseThrow();
		assertEquals(mine.ends(), open.ends());
		assertEquals(List.of(mine.sections().getFirst()), open.sections(), "the Straight is dug on from the start");
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

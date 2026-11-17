package dev.bettercoppers.test;

import dev.bettercoppers.home.HomeLinking;
import dev.bettercoppers.home.Homes;
import dev.bettercoppers.job.Load;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import dev.bettercoppers.mining.MineLayout;
import dev.bettercoppers.mining.MinerData;
import dev.bettercoppers.mining.Mines;
import dev.bettercoppers.registry.ModAttachments;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Describes the miner's current behaviour: finding its Mine Entrance, digging the first Mine Section, supplies, ore
 * and fluids. The usual scene is a stone block with a Mine Entrance in front of it, the mine heading north.
 */
public class MinerGameTests {
	private static final Block COPPER_CHEST = Blocks.COPPER_CHEST.weathering().unaffected();
	private static final BlockPos HOME = new BlockPos(1, 1, 7);
	/** The middle beam of the scene's Mine Entrance; its opening is (4, 1..2, 6). */
	private static final BlockPos BEAM = new BlockPos(4, 3, 6);
	/** Beside the shaft (its east wall is x = 5), in the second slice. */
	private static final BlockPos WALL = new BlockPos(6, 2, 4);
	/** In the opening of the entrance. */
	private static final BlockPos START = new BlockPos(4, 1, 6);
	/** A slice is 9 digs and 3 floors, plus supports; every step counts as one progress. */
	private static final int TWO_SLICES = 24;

	private record Scene(CopperGolem golem, Container chest, BlockPos beam) {
	}

	/** Stone block, daytime, a Mine Entrance in front of it, a miner with home and pickaxe, and a home chest with supplies or empty. */
	private static Scene scene(GameTestHelper h, boolean supplies, BlockPos start) {
		ServerLevel level = h.getLevel();
		level.clockManager().setTotalTicks(level.dimensionType().defaultClock().orElseThrow(), 1000L);
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
		for (int x = 2; x <= 7; x++) {
			for (int y = 1; y <= 4; y++) {
				for (int z = 0; z <= 5; z++) {
					h.setBlock(new BlockPos(x, y, z), Blocks.STONE);
				}
			}
		}
		BlockPos beam = GameTests.mineEntrance(h, BEAM);
		Container chest = GameTests.chest(h, HOME, COPPER_CHEST);
		if (supplies) {
			chest.setItem(0, new ItemStack(Items.RAIL, 16));
			chest.setItem(1, new ItemStack(Items.OAK_PLANKS, 32));
			chest.setItem(2, new ItemStack(Items.OAK_FENCE, 16));
			chest.setItem(3, new ItemStack(Items.TORCH, 16));
		}
		return new Scene(miner(h, start, HOME), chest, beam);
	}

	/** Linked first, so the pickaxe makes it take the nearest mine right away. */
	private static CopperGolem miner(GameTestHelper h, BlockPos start, BlockPos home) {
		CopperGolem golem = GameTests.golem(h, start);
		GameTests.energize(golem);
		Homes.set(golem, h.absolutePos(home));
		give(h, golem, new ItemStack(Items.COPPER_PICKAXE));
		return golem;
	}

	private static void give(GameTestHelper h, CopperGolem golem, ItemStack stack) {
		Player player = h.makeMockPlayer(GameType.SURVIVAL);
		player.setShiftKeyDown(true);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		UseEntityCallback.EVENT.invoker().interact(player, h.getLevel(), InteractionHand.MAIN_HAND, golem, null);
	}

	private static void forceChunks(GameTestHelper h, BlockPos far) {
		BlockPos near = h.absolutePos(BlockPos.ZERO);
		BlockPos end = h.absolutePos(far);
		for (int cx = Math.min(near.getX(), end.getX()) >> 4; cx <= Math.max(near.getX(), end.getX()) >> 4; cx++) {
			for (int cz = Math.min(near.getZ(), end.getZ()) >> 4; cz <= Math.max(near.getZ(), end.getZ()) >> 4; cz++) {
				h.getLevel().setChunkForced(cx, cz, true);
			}
		}
	}

	private static Mines.Mine mine(GameTestHelper h, BlockPos beam) {
		return Mines.get(h.getLevel()).find(beam, Direction.Axis.X).orElseThrow();
	}

	private static Mines.Section firstSection(GameTestHelper h, BlockPos beam) {
		return mine(h, beam).sections().getFirst();
	}

	private static List<MineLayout.Step> steps(GameTestHelper h, BlockPos beam) {
		Mines.Section section = firstSection(h, beam);
		return MineLayout.build(section.kind(), section.start());
	}

	/** Turns the first two slices to glass, which drops nothing, so the miner gets no filler from digging them. */
	private static void glassSlices(GameTestHelper h) {
		for (int x = 3; x <= 5; x++) {
			for (int y = 1; y <= 3; y++) {
				for (int z = 4; z <= 5; z++) {
					h.setBlock(new BlockPos(x, y, z), Blocks.GLASS);
				}
			}
		}
	}

	/** Hill on the north side of the frame, open ground south. */
	@GameTest
	public void a_mine_runs_into_the_hill_and_starts_with_a_straight(GameTestHelper h) {
		Scene s = scene(h, false, new BlockPos(7, 1, 7));
		Mines.Mine mine = mine(h, s.beam());
		h.assertValueEqual(mine.facing(), Optional.of(Direction.NORTH), "facing");
		h.assertValueEqual(firstSection(h, s.beam()).kind(), MineLayout.STRAIGHT, "first section");
		h.assertValueEqual(firstSection(h, s.beam()).start(), new Mines.OpenEnd(h.absolutePos(new BlockPos(4, 1, 5)), Direction.NORTH), "Open End");
		h.assertValueEqual(MinerData.get(s.golem()).mine(), s.beam(), "the miner's mine");
		h.assertTrue(hasKey(HomeLinking.status(s.golem()), "status.bettercoppers.mine"), "no mine in the status line");
		h.succeed();
	}

	/** Open ground on both sides: the mine runs away from the home chest, and goes down stairs first. */
	@GameTest
	public void on_flat_ground_a_mine_runs_away_from_home_and_starts_with_stairs_down(GameTestHelper h) {
		JobGameTests.scene(h);
		BlockPos beam = GameTests.mineEntrance(h, new BlockPos(4, 3, 3));
		GameTests.chest(h, new BlockPos(4, 1, 7), COPPER_CHEST);
		CopperGolem golem = miner(h, new BlockPos(4, 1, 5), new BlockPos(4, 1, 7));
		h.assertValueEqual(mine(h, beam).facing(), Optional.of(Direction.NORTH), "facing");
		h.assertValueEqual(firstSection(h, beam).kind(), MineLayout.STAIRS_DOWN, "first section");
		h.assertValueEqual(MinerData.get(golem).mine(), beam, "the miner's mine");
		h.succeed();
	}

	/**
	 * A whole Straight into a stone hill, sealed from daylight, from an entrance at z = 10: supports at z = 7 and 3, a
	 * torch, rails last, and then the mine is finished and the load goes home. It is longer than the test area, and
	 * entities freeze outside the chunks a test keeps loaded, so those are forced.
	 */
	@GameTest(maxTicks = 9600, padding = 48)
	public void miner_digs_a_straight_to_done(GameTestHelper h) {
		ServerLevel level = h.getLevel();
		level.clockManager().setTotalTicks(level.dimensionType().defaultClock().orElseThrow(), 1000L);
		forceChunks(h, new BlockPos(8, 0, 13));
		fill(h, new BlockPos(0, 0, 0), new BlockPos(8, 0, 13), Blocks.STONE);
		fill(h, new BlockPos(1, 1, 0), new BlockPos(7, 5, 9), Blocks.STONE);
		BlockPos beam = GameTests.mineEntrance(h, new BlockPos(4, 3, 10));
		BlockPos home = new BlockPos(1, 1, 12);
		Container chest = GameTests.chest(h, home, COPPER_CHEST);
		chest.setItem(0, new ItemStack(Items.RAIL, 16));
		chest.setItem(1, new ItemStack(Items.OAK_PLANKS, 32));
		chest.setItem(2, new ItemStack(Items.OAK_FENCE, 16));
		chest.setItem(3, new ItemStack(Items.TORCH, 16));
		CopperGolem golem = miner(h, new BlockPos(4, 1, 11), home);
		h.assertValueEqual(MinerData.get(golem).mine(), beam, "the miner's mine");
		h.assertValueEqual(firstSection(h, beam).kind(), MineLayout.STRAIGHT, "first section");
		h.succeedWhen(() -> {
			h.assertTrue(firstSection(h, beam).done(), "section not done, progress " + firstSection(h, beam).progress());
			for (int z = 2; z <= 9; z++) {
				h.assertTrue(h.getBlockState(new BlockPos(4, 1, z)).is(Blocks.RAIL), "no rail at z " + z);
				h.assertTrue(h.getBlockState(new BlockPos(4, 2, z)).isAir(), "not dug at z " + z);
			}
			for (int z : new int[] {7, 3}) {
				for (int x : new int[] {3, 5}) {
					h.assertTrue(h.getBlockState(new BlockPos(x, 1, z)).is(Blocks.OAK_FENCE) && h.getBlockState(new BlockPos(x, 2, z)).is(Blocks.OAK_FENCE), "no support post at " + x + ", " + z);
				}
				h.assertTrue(h.getBlockState(new BlockPos(4, 3, z)).is(Blocks.OAK_PLANKS), "no beam at z " + z);
			}
			h.assertTrue(h.getBlockState(new BlockPos(4, 3, 6)).is(Blocks.WALL_TORCH), "no torch");
			h.assertTrue(h.getBlockState(new BlockPos(4, 1, 1)).is(Blocks.STONE), "dug past the section");
			h.assertTrue(mine(h, beam).isFinished(), "mine not finished");
			h.assertTrue(MinerData.get(golem).mine() == null, "still holds a finished mine");
			h.assertTrue(chest.countItem(Items.COBBLESTONE) > 0, "the load is not home");
		});
	}

	/** Stairs Down from an entrance on flat stone: 4 steps, each 4 high, and a landing 4 lower. Fits inside the test area's barrier walls. */
	@GameTest(maxTicks = 9600, padding = 48)
	public void miner_digs_stairs_down_to_done(GameTestHelper h) {
		ServerLevel level = h.getLevel();
		level.clockManager().setTotalTicks(level.dimensionType().defaultClock().orElseThrow(), 1000L);
		fill(h, new BlockPos(0, 0, 0), new BlockPos(7, 4, 7), Blocks.STONE);
		BlockPos beam = GameTests.mineEntrance(h, new BlockPos(4, 7, 6));
		BlockPos home = new BlockPos(1, 5, 7);
		Container chest = GameTests.chest(h, home, COPPER_CHEST);
		CopperGolem golem = miner(h, new BlockPos(4, 5, 7), home);
		h.assertValueEqual(MinerData.get(golem).mine(), beam, "the miner's mine");
		h.assertValueEqual(firstSection(h, beam).kind(), MineLayout.STAIRS_DOWN, "first section");
		h.succeedWhen(() -> {
			h.assertValueEqual(firstSection(h, beam).kind(), MineLayout.STAIRS_DOWN, "first section of " + mine(h, beam) + ", golem's mine " + MinerData.get(golem).mine());
			h.assertTrue(firstSection(h, beam).done(), "section not done, progress " + firstSection(h, beam).progress());
			// Feet height of each slice from z = 5 north, then the landing at z = 1.
			int[] feet = {4, 3, 2, 1, 1};
			for (int i = 0; i < feet.length; i++) {
				BlockPos step = new BlockPos(4, feet[i], 5 - i);
				for (int up = 0; up < (i < 4 ? 4 : 3); up++) {
					h.assertTrue(h.getBlockState(step.above(up)).isAir(), "not dug " + up + " above step " + i);
				}
				h.assertTrue(h.getBlockState(step.below()).isSolid(), "no floor under step " + i);
			}
			h.assertTrue(h.getBlockState(new BlockPos(4, 4, 0)).is(Blocks.STONE), "dug past the landing");
			h.assertTrue(MinerData.get(golem).mine() == null, "still holds a finished mine");
			h.assertTrue(chest.countItem(Items.COBBLESTONE) > 0, "the load is not home");
		});
	}

	/** A broken frame closes the mine: the miner forgets it and goes home. It may then take a neighbouring test's mine. */
	@GameTest(maxTicks = 1200)
	public void breaking_the_frame_closes_the_mine(GameTestHelper h) {
		Scene s = scene(h, true, START);
		h.setBlock(BEAM.east(), Blocks.AIR);
		h.succeedWhen(() -> {
			h.assertTrue(!s.beam().equals(MinerData.get(s.golem()).mine()), "still holds the broken mine");
			h.assertTrue(h.getBlockState(new BlockPos(4, 3, 5)).is(Blocks.STONE), "dug in a closed mine");
		});
	}
	static boolean hasKey(Component component, String key) {
		return component.getContents() instanceof TranslatableContents contents && contents.getKey().equals(key)
			|| component.getSiblings().stream().anyMatch(sibling -> hasKey(sibling, key));
	}

	private static int progress(GameTestHelper h, Scene s) {
		return firstSection(h, s.beam()).progress();
	}

	@GameTest(maxTicks = 600)
	public void miner_without_supplies_waits_at_home(GameTestHelper h) {
		// Starts 6 blocks from home, so a golem that never moves fails the "at home" check.
		Scene s = scene(h, false, new BlockPos(7, 1, 7));
		// The first support is in the third slice (z = 3): its fences have to come from somewhere.
		List<BlockPos> supportSlice = BlockPos.betweenClosedStream(new BlockPos(3, 1, 3), new BlockPos(5, 3, 3)).map(BlockPos::immutable).toList();
		// The idle miner strolls around a bit, so check that it came to the chest at some point, not where it ends up.
		int[] closest = {Integer.MAX_VALUE};
		for (int t = 5; t < 500; t += 5) {
			h.runAfterDelay(t, () -> closest[0] = Math.min(closest[0], s.golem().blockPosition().distManhattan(h.absolutePos(HOME))));
		}
		h.runAfterDelay(500, () -> {
			h.assertTrue(closest[0] <= 3, "never came to the chest, closest distance " + closest[0]);
			for (BlockPos pos : supportSlice) {
				h.assertTrue(!h.getBlockState(pos).isAir(), "dug a support slice without supplies: " + pos);
			}
			h.succeed();
		});
	}

	@GameTest(maxTicks = 1200)
	public void chest_broken_keeps_load_and_resumes(GameTestHelper h) {
		Scene s = scene(h, false, START);
		// A full pouch sends it home; the chest goes while it is on the way.
		Load.of(s.golem()).stash(new ItemStack(Items.COAL, 64));
		h.startSequence()
			.thenExecuteAfter(5, () -> h.setBlock(HOME, Blocks.AIR))
			.thenExecuteAfter(100, () -> {
				h.assertValueEqual(Load.of(s.golem()).count(stack -> stack.is(Items.COAL)), 64, "coal carried");
				h.assertValueEqual(GameTests.dropped(h, Items.COAL), 0, "coal dropped");
				h.setBlock(HOME, COPPER_CHEST);
			})
			.thenWaitUntil(() -> {
				Container chest = (Container) h.getBlockEntity(HOME, BlockEntity.class);
				h.assertValueEqual(chest.countItem(Items.COAL), 64, "coal in the new chest");
			})
			.thenSucceed();
	}

	@GameTest(maxTicks = 1800)
	public void chest_broken_stops_digging_and_resumes(GameTestHelper h) {
		Scene s = scene(h, true, START);
		// It carries all it needs, so only the missing chest can stop it.
		Load load = Load.of(s.golem());
		load.addSupplies(new ItemStack(Items.RAIL, 16));
		load.addSupplies(new ItemStack(Items.OAK_PLANKS, 32));
		load.addSupplies(new ItemStack(Items.OAK_FENCE, 16));
		h.setBlock(HOME, Blocks.AIR);
		h.startSequence()
			.thenExecuteAfter(300, () -> {
				h.assertValueEqual(progress(h, s), 0, "plan progress without a chest");
				h.setBlock(HOME, COPPER_CHEST);
			})
			.thenWaitUntil(() -> h.assertTrue(progress(h, s) > 0, "did not resume"))
			.thenSucceed();
	}

	/** Positive control for the test above: the same scene with supplies in the chest does make progress. */
	@GameTest(maxTicks = 1200)
	public void miner_with_supplies_makes_progress(GameTestHelper h) {
		Scene s = scene(h, true, new BlockPos(7, 1, 7));
		h.succeedWhen(() -> h.assertTrue(progress(h, s) > 0, "no progress yet"));
	}

	private static void fill(GameTestHelper h, BlockPos from, BlockPos to, Block block) {
		BlockPos.betweenClosed(from, to).forEach(pos -> h.setBlock(pos, block));
	}

	@GameTest(maxTicks = 3600)
	public void backed_up_miner_stops_digging_until_there_is_room(GameTestHelper h) {
		Scene s = scene(h, true, START);
		// Stacks big enough that restocking leaves no slot empty.
		for (int slot = 0; slot < 4; slot++) {
			s.chest().getItem(slot).setCount(64);
		}
		// Room for 30 of the 64 coal it carries: the visit leaves it holding 34.
		s.chest().setItem(4, new ItemStack(Items.COAL, 34));
		for (int slot = 5; slot < s.chest().getContainerSize(); slot++) {
			s.chest().setItem(slot, new ItemStack(Items.DIRT, 64));
		}
		Load.of(s.golem()).stash(new ItemStack(Items.COAL, 64));
		h.startSequence()
			.thenExecuteAfter(600, () -> {
				h.assertValueEqual(Load.of(s.golem()).count(stack -> stack.is(Items.COAL)), 34, "coal still carried");
				h.assertValueEqual(progress(h, s), 0, "plan progress");
				s.chest().setItem(5, ItemStack.EMPTY);
			})
			.thenWaitUntil(() -> {
				h.assertValueEqual(s.chest().countItem(Items.COAL), 98, "coal in the chest");
				h.assertTrue(progress(h, s) > 0, "plan did not carry on");
			})
			.thenSucceed();
	}

	@GameTest(maxTicks = 3600)
	public void miner_digs_ore_vein_and_carries_on(GameTestHelper h) {
		Scene s = scene(h, true, START);
		// A nearly full pouch, so the trip home does not wait on how fast it digs: 64 items fill it, and it returns on its own.
		Load.of(s.golem()).stash(new ItemStack(Items.COBBLESTONE, 56));
		h.setBlock(WALL, Blocks.COAL_ORE);
		h.succeedWhen(() -> {
			h.assertTrue(h.getBlockState(WALL).isAir(), "ore not dug yet");
			h.assertTrue(s.chest().countItem(Items.COAL) >= 1, "no coal in the chest yet");
			h.assertTrue(progress(h, s) > TWO_SLICES, "plan did not carry on: " + progress(h, s));
		});
	}

	/** Padded, like the one below: without it, these two made the miner and repair tests next to them fail now and then. */
	@GameTest(maxTicks = 3600, padding = 48)
	public void miner_finds_diamonds_and_hurries_home(GameTestHelper h) {
		Scene s = scene(h, true, START);
		h.setBlock(WALL, Blocks.DIAMOND_ORE);
		h.startSequence()
			.thenWaitUntil(() -> h.assertTrue(Boolean.TRUE.equals(s.golem().getAttached(ModAttachments.EXCITED)), "not excited yet"))
			.thenWaitUntil(() -> h.assertTrue(s.chest().countItem(Items.DIAMOND) >= 1, "no diamond in the chest yet"))
			.thenExecute(() -> h.assertTrue(h.getBlockState(WALL).isAir(), "diamond ore still in the wall"))
			.thenSucceed();
	}

	@GameTest(maxTicks = 3600, padding = 48)
	public void miner_digs_gold_redstone_and_emerald_ore(GameTestHelper h) {
		Scene s = scene(h, true, START);
		h.setBlock(WALL.below(), Blocks.GOLD_ORE);
		h.setBlock(WALL, Blocks.REDSTONE_ORE);
		h.setBlock(WALL.above(), Blocks.EMERALD_ORE);
		h.succeedWhen(() -> {
			h.assertTrue(h.getBlockState(WALL.below()).isAir(), "gold ore not dug yet");
			h.assertTrue(h.getBlockState(WALL).isAir(), "redstone ore not dug yet");
			h.assertTrue(h.getBlockState(WALL.above()).isAir(), "emerald ore not dug yet");
			for (var drop : List.of(Items.RAW_GOLD, Items.REDSTONE, Items.EMERALD)) {
				int found = s.chest().countItem(drop) + Load.of(s.golem()).count(stack -> stack.is(drop));
				h.assertTrue(found >= 1, "no " + drop + " carried or stored");
			}
		});
	}

	@GameTest(maxTicks = 3600)
	public void miner_seals_fluid_with_filler(GameTestHelper h) {
		Scene s = scene(h, true, START);
		h.setBlock(WALL, Blocks.WATER);
		h.succeedWhen(() -> {
			h.assertTrue(h.getBlockState(WALL).is(Blocks.COBBLESTONE) || h.getBlockState(WALL).is(Blocks.STONE), "water not plugged yet");
			h.assertTrue(progress(h, s) > TWO_SLICES, "plan did not carry on: " + progress(h, s));
		});
	}

	@GameTest(maxTicks = 3600)
	public void miner_without_filler_cuts_the_section_short_and_still_lays_its_rails(GameTestHelper h) {
		Scene s = scene(h, true, START);
		glassSlices(h);
		// Above the first block of the third slice, shut in by stone.
		h.setBlock(new BlockPos(4, 4, 3), Blocks.WATER);
		h.succeedWhen(() -> {
			h.assertTrue(firstSection(h, s.beam()).done(), "section not done");
			h.assertTrue(h.getBlockState(new BlockPos(4, 1, 5)).is(Blocks.RAIL), "no rail in the first slice");
			h.assertTrue(h.getBlockState(new BlockPos(4, 1, 4)).is(Blocks.RAIL), "no rail in the second slice");
			h.assertTrue(h.getBlockState(new BlockPos(4, 3, 3)).is(Blocks.STONE), "dug on at the water");
		});
	}

	@GameTest(maxTicks = 3600)
	public void miner_without_filler_skips_only_a_chased_ore_at_a_fluid(GameTestHelper h) {
		Scene s = scene(h, true, START);
		glassSlices(h);
		h.setBlock(WALL, Blocks.COAL_ORE);
		// Behind the ore, out of reach of the shaft. The stone keeps the water from running out of the scene.
		h.setBlock(WALL.east(), Blocks.WATER);
		h.setBlock(WALL.east(2), Blocks.STONE);
		h.succeedWhen(() -> {
			// A section cut short would never open the third slice.
			h.assertTrue(h.getBlockState(new BlockPos(4, 3, 3)).isAir(), "third slice not opened: progress " + progress(h, s));
			h.assertTrue(h.getBlockState(WALL).is(Blocks.COAL_ORE), "dug the ore at the water");
		});
	}
}

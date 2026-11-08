package dev.bettercoppers.test;

import dev.bettercoppers.home.Homes;
import dev.bettercoppers.job.Load;
import java.util.List;
import dev.bettercoppers.mining.MineLayout;
import dev.bettercoppers.mining.MinePattern;
import dev.bettercoppers.mining.MinePlan;
import dev.bettercoppers.mining.MinerData;
import dev.bettercoppers.registry.ModAttachments;
import dev.bettercoppers.registry.ModComponents;
import dev.bettercoppers.registry.ModItems;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Describes the miner's current behaviour: supplies, ore and fluids. The scene is a stone block with a shaft heading north. */
public class MinerGameTests {
	private static final Block COPPER_CHEST = Blocks.COPPER_CHEST.weathering().unaffected();
	private static final BlockPos HOME = new BlockPos(1, 1, 7);
	private static final BlockPos ENTRANCE = new BlockPos(4, 1, 5);
	/** Beside the shaft (its east wall is x = 5), in the second slice. */
	private static final BlockPos WALL = new BlockPos(6, 2, 4);
	private static final BlockPos START = new BlockPos(4, 1, 6);
	/** A slice is 9 digs and 3 floors, plus supports; every step counts as one progress. */
	private static final int TWO_SLICES = 24;

	private record Scene(CopperGolem golem, Container chest, MinePlan plan) {
	}

	/** Stone block, daytime, a miner with plan and pickaxe, and a home chest with supplies or empty. */
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
		Container chest = GameTests.chest(h, HOME, COPPER_CHEST);
		if (supplies) {
			chest.setItem(0, new ItemStack(Items.RAIL, 16));
			chest.setItem(1, new ItemStack(Items.OAK_PLANKS, 32));
			chest.setItem(2, new ItemStack(Items.OAK_FENCE, 16));
			chest.setItem(3, new ItemStack(Items.TORCH, 16));
		}
		CopperGolem golem = GameTests.golem(h, start);
		GameTests.energize(golem);
		MinePlan plan = MinePlan.blank(MinePattern.MINESHAFT, 8).marked(h.absolutePos(ENTRANCE), Direction.NORTH);
		ItemStack planStack = new ItemStack(ModItems.MINESHAFT_PLAN);
		planStack.set(ModComponents.MINE_PLAN, plan);
		give(h, golem, new ItemStack(Items.COPPER_PICKAXE));
		give(h, golem, planStack);
		Homes.set(golem, h.absolutePos(HOME));
		return new Scene(golem, chest, plan);
	}

	private static void give(GameTestHelper h, CopperGolem golem, ItemStack stack) {
		Player player = h.makeMockPlayer(GameType.SURVIVAL);
		player.setShiftKeyDown(true);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		UseEntityCallback.EVENT.invoker().interact(player, h.getLevel(), InteractionHand.MAIN_HAND, golem, null);
	}

	private static int progress(CopperGolem golem) {
		return MinerData.get(golem).plan().progress();
	}

	@GameTest(maxTicks = 600)
	public void miner_without_supplies_waits_at_home(GameTestHelper h) {
		// Starts 6 blocks from home, so a golem that never moves fails the "at home" check.
		Scene s = scene(h, false, new BlockPos(7, 1, 7));
		var firstSlice = MineLayout.build(s.plan(), h.getLevel().getMinY()).stream()
			.filter(step -> step.action() == MineLayout.Action.DIG && MineLayout.sliceOf(s.plan(), step.pos()) == 0)
			.toList();
		// The idle miner strolls around a bit, so check that it came to the chest at some point, not where it ends up.
		int[] closest = {Integer.MAX_VALUE};
		for (int t = 5; t < 500; t += 5) {
			h.runAfterDelay(t, () -> closest[0] = Math.min(closest[0], s.golem().blockPosition().distManhattan(h.absolutePos(HOME))));
		}
		h.runAfterDelay(500, () -> {
			h.assertTrue(closest[0] <= 3, "never came to the chest, closest distance " + closest[0]);
			for (var step : firstSlice) {
				h.assertTrue(!h.getLevel().getBlockState(step.pos()).isAir(), "dug without supplies: " + step.pos());
			}
			h.assertTrue(progress(s.golem()) == 0, "plan advanced without supplies");
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
				h.assertValueEqual(progress(s.golem()), 0, "plan progress without a chest");
				h.setBlock(HOME, COPPER_CHEST);
			})
			.thenWaitUntil(() -> h.assertTrue(progress(s.golem()) > 0, "did not resume"))
			.thenSucceed();
	}

	/** Positive control for the test above: the same scene with supplies in the chest does make progress. */
	@GameTest(maxTicks = 1200)
	public void miner_with_supplies_makes_progress(GameTestHelper h) {
		Scene s = scene(h, true, new BlockPos(7, 1, 7));
		h.succeedWhen(() -> h.assertTrue(progress(s.golem()) > 0, "no progress yet"));
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
			h.assertTrue(progress(s.golem()) > TWO_SLICES, "plan did not carry on: " + progress(s.golem()));
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
			h.assertTrue(progress(s.golem()) > TWO_SLICES, "plan did not carry on: " + progress(s.golem()));
		});
	}
}

package dev.bettercoppers.test;

import dev.bettercoppers.courier.CourierRoute;
import dev.bettercoppers.courier.Couriers;
import dev.bettercoppers.entity.CopperChestMinecart;
import dev.bettercoppers.home.Homes;
import dev.bettercoppers.job.Job;
import dev.bettercoppers.job.Load;
import dev.bettercoppers.job.Tools;
import dev.bettercoppers.job.WorkerData;
import dev.bettercoppers.mining.MineLayout;
import dev.bettercoppers.mining.MinePattern;
import dev.bettercoppers.mining.MinePlan;
import dev.bettercoppers.registry.ModAttachments;
import dev.bettercoppers.registry.ModComponents;
import dev.bettercoppers.registry.ModEntities;
import dev.bettercoppers.registry.ModItems;
import dev.bettercoppers.upkeep.Upkeep;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;

/** One game test per job: each builds a small scene, hands the golem its tool the way a player would, and waits. */
public class JobGameTests {
	private static final Block COPPER_CHEST = Blocks.COPPER_CHEST.weathering().unaffected();

	/** Stone floor under the test area and daytime, so job golems are allowed to work. */
	private static void scene(GameTestHelper h) {
		ServerLevel level = h.getLevel();
		level.clockManager().setTotalTicks(level.dimensionType().defaultClock().orElseThrow(), 1000L);
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
	}

	/** A player right-clicks the golem with this item, through the registered interaction callbacks. */
	private static void give(GameTestHelper h, CopperGolem golem, ItemStack stack) {
		Player player = h.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		UseEntityCallback.EVENT.invoker().interact(player, h.getLevel(), InteractionHand.MAIN_HAND, golem, null);
	}

	private static int count(Container container, Item item) {
		return container.countItem(item);
	}

	@GameTest(maxTicks = 1200)
	public void sorter_with_bundle_delivers_to_matching_chest(GameTestHelper h) {
		scene(h);
		BlockPos home = new BlockPos(2, 1, 4);
		GameTests.chest(h, home, COPPER_CHEST).setItem(0, new ItemStack(Items.COBBLESTONE));
		Container match = GameTests.chest(h, new BlockPos(6, 1, 4), Blocks.CHEST);
		match.setItem(0, new ItemStack(Items.COBBLESTONE));
		GameTests.chest(h, new BlockPos(4, 1, 6), Blocks.CHEST);
		CopperGolem golem = GameTests.golem(h, new BlockPos(4, 1, 4));
		GameTests.energize(golem);
		give(h, golem, new ItemStack(Items.BUNDLE));
		Homes.set(golem, h.absolutePos(home));
		h.succeedWhen(() -> h.assertTrue(count(match, Items.COBBLESTONE) == 2, "cobblestone not delivered yet"));
	}

	@GameTest(maxTicks = 2400)
	public void farmer_harvests_and_replants(GameTestHelper h) {
		scene(h);
		BlockPos home = new BlockPos(1, 1, 1);
		Container chest = GameTests.chest(h, home, COPPER_CHEST);
		BlockPos[] crops = {new BlockPos(3, 1, 3), new BlockPos(4, 1, 3), new BlockPos(5, 1, 3)};
		for (BlockPos crop : crops) {
			h.setBlock(crop.below(), Blocks.FARMLAND);
			h.setBlock(crop, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
		}
		CopperGolem golem = GameTests.golem(h, new BlockPos(2, 1, 2));
		GameTests.energize(golem);
		give(h, golem, new ItemStack(Items.COPPER_HOE));
		Homes.set(golem, h.absolutePos(home));
		h.succeedWhen(() -> {
			h.assertTrue(count(chest, Items.WHEAT) >= 1, "no wheat delivered yet");
			for (BlockPos crop : crops) {
				h.assertBlockPresent(Blocks.WHEAT, crop);
			}
		});
	}

	@GameTest(maxTicks = 2400)
	public void miner_digs_first_slice(GameTestHelper h) {
		scene(h);
		// Stone block in front of the golem; the tunnel heads north from the entrance.
		for (int x = 2; x <= 6; x++) {
			for (int y = 1; y <= 4; y++) {
				for (int z = 0; z <= 5; z++) {
					h.setBlock(new BlockPos(x, y, z), Blocks.STONE);
				}
			}
		}
		BlockPos home = new BlockPos(1, 1, 7);
		Container supplies = GameTests.chest(h, home, COPPER_CHEST);
		supplies.setItem(0, new ItemStack(Items.RAIL, 16));
		supplies.setItem(1, new ItemStack(Items.OAK_PLANKS, 32));
		supplies.setItem(2, new ItemStack(Items.OAK_FENCE, 16));
		supplies.setItem(3, new ItemStack(Items.TORCH, 16));
		CopperGolem golem = GameTests.golem(h, new BlockPos(4, 1, 6));
		GameTests.energize(golem);
		MinePlan plan = MinePlan.blank(MinePattern.MINESHAFT, 16).marked(h.absolutePos(new BlockPos(4, 1, 5)), Direction.NORTH);
		ItemStack planStack = new ItemStack(ModItems.MINESHAFT_PLAN);
		planStack.set(ModComponents.MINE_PLAN, plan);
		give(h, golem, new ItemStack(Items.COPPER_PICKAXE));
		give(h, golem, planStack);
		Homes.set(golem, h.absolutePos(home));
		var firstSlice = MineLayout.build(plan, h.getLevel().getMinY()).stream()
			.filter(step -> step.action() == MineLayout.Action.DIG && MineLayout.sliceOf(plan, step.pos()) == 0)
			.toList();
		h.succeedWhen(() -> {
			for (var step : firstSlice) {
				h.assertTrue(h.getLevel().getBlockState(step.pos()).isAir(), "not dug yet: " + step.pos());
			}
		});
	}

	@GameTest(maxTicks = 1200)
	public void courier_moves_items(GameTestHelper h) {
		scene(h);
		BlockPos from = new BlockPos(1, 1, 4);
		BlockPos to = new BlockPos(7, 1, 4);
		GameTests.chest(h, from, COPPER_CHEST).setItem(0, new ItemStack(Items.IRON_INGOT, 8));
		Container target = GameTests.chest(h, to, COPPER_CHEST);
		CopperGolem golem = GameTests.golem(h, new BlockPos(4, 1, 4));
		GameTests.energize(golem);
		Couriers.setRoute(golem, new CourierRoute(h.absolutePos(from), h.absolutePos(to)));
		h.succeedWhen(() -> h.assertTrue(count(target, Items.IRON_INGOT) == 8, "ingots not delivered yet"));
	}

	@GameTest
	public void job_item_assigns_job(GameTestHelper h) {
		scene(h);
		CopperGolem golem = GameTests.golem(h, new BlockPos(4, 1, 4));
		give(h, golem, new ItemStack(Items.COPPER_AXE));
		h.assertValueEqual(WorkerData.job(golem), Job.FORESTER, "job");
		give(h, golem, new ItemStack(Items.COPPER_HOE));
		h.assertValueEqual(WorkerData.job(golem), Job.FARMER, "job");
		h.succeed();
	}

	@GameTest
	public void non_copper_tool_is_refused(GameTestHelper h) {
		scene(h);
		CopperGolem golem = GameTests.golem(h, new BlockPos(4, 1, 4));
		give(h, golem, new ItemStack(Items.IRON_HOE));
		h.assertTrue(golem.getMainHandItem().isEmpty(), "golem took an iron hoe");
		h.assertTrue(!Upkeep.hasJob(golem), "golem got a job from an iron hoe");
		h.succeed();
	}

	@GameTest(maxTicks = 1200)
	public void broken_tool_keeps_job_and_is_refetched(GameTestHelper h) {
		scene(h);
		BlockPos home = new BlockPos(1, 1, 1);
		Container chest = GameTests.chest(h, home, COPPER_CHEST);
		chest.setItem(0, new ItemStack(Items.COPPER_HOE));
		CopperGolem golem = GameTests.golem(h, new BlockPos(4, 1, 4));
		GameTests.energize(golem);
		ItemStack worn = new ItemStack(Items.COPPER_HOE);
		worn.setDamageValue(worn.getMaxDamage() - 1);
		give(h, golem, worn);
		Homes.set(golem, h.absolutePos(home));
		Tools.wear(golem, golem.getMainHandItem());
		h.assertTrue(golem.getMainHandItem().isEmpty(), "hoe did not break");
		h.assertValueEqual(WorkerData.job(golem), Job.FARMER, "job after the tool broke");
		h.succeedWhen(() -> {
			h.assertTrue(golem.getMainHandItem().is(Items.COPPER_HOE), "no new hoe yet");
			h.assertTrue(count(chest, Items.COPPER_HOE) == 0, "hoe still in the chest");
		});
	}

	/** A cart the golem tows, with the given items already in it. */
	private static CopperChestMinecart cart(GameTestHelper h, CopperGolem golem, BlockPos rel, ItemStack... contents) {
		CopperChestMinecart cart = h.spawn(ModEntities.COPPER_CHEST_MINECART, rel);
		for (int slot = 0; slot < contents.length; slot++) {
			cart.setItem(slot, contents[slot]);
		}
		cart.chainTo(golem);
		return cart;
	}

	@GameTest(maxTicks = 1200)
	public void lost_tool_with_loaded_cart_goes_home_then_refetches(GameTestHelper h) {
		scene(h);
		BlockPos home = new BlockPos(1, 1, 1);
		Container chest = GameTests.chest(h, home, COPPER_CHEST);
		chest.setItem(0, new ItemStack(Items.COPPER_HOE));
		CopperGolem golem = GameTests.golem(h, new BlockPos(4, 1, 4));
		GameTests.energize(golem);
		ItemStack worn = new ItemStack(Items.COPPER_HOE);
		worn.setDamageValue(worn.getMaxDamage() - 1);
		give(h, golem, worn);
		Homes.set(golem, h.absolutePos(home));
		CopperChestMinecart cart = cart(h, golem, new BlockPos(5, 1, 4), new ItemStack(Items.WHEAT, 8));
		Tools.wear(golem, golem.getMainHandItem());
		h.assertTrue(golem.getMainHandItem().isEmpty(), "hoe did not break");
		h.succeedWhen(() -> {
			h.assertTrue(count(chest, Items.WHEAT) == 8, "cart not unloaded yet");
			h.assertTrue(cart.isEmpty(), "cart still loaded");
			h.assertTrue(golem.getMainHandItem().is(Items.COPPER_HOE), "no new hoe yet");
		});
	}

	@GameTest(maxTicks = 1200)
	public void forester_finds_sapling_in_cart(GameTestHelper h) {
		scene(h);
		BlockPos home = new BlockPos(1, 1, 1);
		GameTests.chest(h, home, COPPER_CHEST);
		BlockPos stump = new BlockPos(4, 1, 2);
		h.setBlock(stump.below(), Blocks.DIRT);
		CopperGolem golem = GameTests.golem(h, new BlockPos(4, 1, 4));
		GameTests.energize(golem);
		give(h, golem, new ItemStack(Items.COPPER_AXE));
		WorkerData.getOrCreate(golem).marks().add(h.absolutePos(stump));
		Homes.set(golem, h.absolutePos(home));
		CopperChestMinecart cart = cart(h, golem, new BlockPos(5, 1, 4), new ItemStack(Items.OAK_SAPLING));
		h.succeedWhen(() -> {
			h.assertBlockPresent(Blocks.OAK_SAPLING, stump);
			h.assertTrue(cart.isEmpty(), "sapling still in the cart");
		});
	}

	@GameTest
	public void golem_death_drops_load_cart_keeps_items(GameTestHelper h) {
		scene(h);
		CopperGolem golem = GameTests.golem(h, new BlockPos(4, 1, 4));
		give(h, golem, new ItemStack(Items.COPPER_HOE));
		Load load = Load.of(golem);
		h.assertTrue(load.isEmpty() && !golem.hasAttached(ModAttachments.LOAD), "reading added an attachment");
		load.stash(new ItemStack(Items.WHEAT, 8));
		load.addSupplies(new ItemStack(Items.WHEAT_SEEDS, 4));
		h.assertTrue(golem.hasAttached(ModAttachments.LOAD), "writing added no attachment");
		CopperChestMinecart cart = cart(h, golem, new BlockPos(5, 1, 4), new ItemStack(Items.CARROT, 5));
		golem.kill(h.getLevel());
		h.succeedWhen(() -> {
			h.assertValueEqual(GameTests.dropped(h, Items.WHEAT), 8, "dropped pouch");
			h.assertValueEqual(GameTests.dropped(h, Items.WHEAT_SEEDS), 4, "dropped supplies");
			h.assertValueEqual(GameTests.dropped(h, Items.CARROT), 0, "dropped from the cart");
			h.assertValueEqual(count(cart, Items.CARROT), 5, "left in the cart");
		});
	}

	@GameTest(maxTicks = 1200)
	public void full_chest_keeps_load_until_there_is_room(GameTestHelper h) {
		scene(h);
		BlockPos home = new BlockPos(1, 1, 1);
		Container chest = GameTests.chest(h, home, COPPER_CHEST);
		for (int slot = 0; slot < chest.getContainerSize(); slot++) {
			chest.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
		}
		CopperGolem golem = GameTests.golem(h, new BlockPos(2, 1, 2));
		GameTests.energize(golem);
		give(h, golem, new ItemStack(Items.COPPER_HOE));
		Homes.set(golem, h.absolutePos(home));
		Load.of(golem).stash(new ItemStack(Items.WHEAT, 64));
		h.startSequence()
			.thenExecuteAfter(200, () -> {
				h.assertValueEqual(Load.of(golem).count(stack -> stack.is(Items.WHEAT)), 64, "wheat carried");
				h.assertValueEqual(GameTests.dropped(h, Items.WHEAT), 0, "wheat dropped");
				chest.setItem(0, ItemStack.EMPTY);
			})
			.thenWaitUntil(() -> h.assertValueEqual(count(chest, Items.WHEAT), 64, "wheat in the chest"))
			.thenSucceed();
	}

	@GameTest(maxTicks = 2400)
	public void sorter_prefers_chest_with_item_frame(GameTestHelper h) {
		scene(h);
		BlockPos home = new BlockPos(2, 1, 4);
		GameTests.chest(h, home, COPPER_CHEST).setItem(0, new ItemStack(Items.COBBLESTONE, 4));
		// A chest that already holds cobblestone would win, if not for the frame on the other one.
		Container matching = GameTests.chest(h, new BlockPos(6, 1, 1), Blocks.CHEST);
		matching.setItem(0, new ItemStack(Items.COBBLESTONE));
		BlockPos framedPos = new BlockPos(6, 1, 6);
		Container framed = GameTests.chest(h, framedPos, Blocks.CHEST);
		ItemFrame frame = new ItemFrame(h.getLevel(), h.absolutePos(framedPos.west()), Direction.WEST);
		frame.setItem(new ItemStack(Items.COBBLESTONE));
		h.getLevel().addFreshEntity(frame);
		CopperGolem golem = GameTests.golem(h, new BlockPos(4, 1, 4));
		GameTests.energize(golem);
		give(h, golem, new ItemStack(Items.BUNDLE));
		Homes.set(golem, h.absolutePos(home));
		h.succeedWhen(() -> {
			h.assertTrue(count(framed, Items.COBBLESTONE) == 4, "not delivered to the framed chest yet");
			h.assertTrue(count(matching, Items.COBBLESTONE) == 1, "delivered to the unframed chest");
		});
	}
}

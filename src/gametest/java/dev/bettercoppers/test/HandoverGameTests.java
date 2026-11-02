package dev.bettercoppers.test;

import dev.bettercoppers.api.BetterCoppersApi;
import dev.bettercoppers.courier.CourierRoute;
import dev.bettercoppers.courier.Couriers;
import dev.bettercoppers.entity.CopperChestMinecart;
import dev.bettercoppers.golem.Sorter;
import dev.bettercoppers.home.Homes;
import dev.bettercoppers.golem.Errands;
import dev.bettercoppers.job.Job;
import dev.bettercoppers.job.JobHandover;
import dev.bettercoppers.job.Jobs;
import dev.bettercoppers.job.Jobs.JobKind;
import dev.bettercoppers.job.Load;
import dev.bettercoppers.job.Tools;
import dev.bettercoppers.job.WorkerData;
import dev.bettercoppers.mining.MinePattern;
import dev.bettercoppers.mining.MinePlan;
import dev.bettercoppers.mining.MinerData;
import dev.bettercoppers.registry.ModAttachments;
import dev.bettercoppers.registry.ModComponents;
import dev.bettercoppers.registry.ModEntities;
import dev.bettercoppers.registry.ModItems;
import dev.bettercoppers.trim.GolemTrims;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Starting and ending jobs, and which clicks do what. Clicks go through the callbacks and then vanilla, as in the game. */
public class HandoverGameTests {
	private static final BlockPos GOLEM = new BlockPos(4, 1, 4);
	private static final int NEW_PRESS = 6;

	private static Player player(GameTestHelper h) {
		Player player = h.makeMockPlayer(GameType.SURVIVAL);
		player.setPos(h.absoluteVec(new Vec3(4.5, 1.0, 6.5)));
		return player;
	}

	/** A right-click: the main hand, and the off hand too if that did nothing, each through the callbacks and then vanilla. */
	private static void click(GameTestHelper h, Player player, CopperGolem golem, boolean sneak, ItemStack held) {
		player.setShiftKeyDown(sneak);
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		for (InteractionHand hand : InteractionHand.values()) {
			InteractionResult result = UseEntityCallback.EVENT.invoker().interact(player, h.getLevel(), hand, golem, null);
			if (result == InteractionResult.PASS) {
				result = player.interactOn(golem, hand, Vec3.ZERO);
			}
			if (result.consumesAction()) {
				return;
			}
		}
	}

	private static void give(GameTestHelper h, Player player, CopperGolem golem, Item item) {
		click(h, player, golem, true, new ItemStack(item));
	}

	/** Two presses of sneak + empty hand, far enough apart not to count as one; then the checks, then success. */
	private static void endJob(GameTestHelper h, Player player, CopperGolem golem, Runnable checks) {
		click(h, player, golem, true, ItemStack.EMPTY);
		h.startSequence()
			.thenExecuteAfter(NEW_PRESS, () -> {
				click(h, player, golem, true, ItemStack.EMPTY);
				checks.run();
			})
			.thenSucceed();
	}

	private static void plainClick(GameTestHelper h, Player player, CopperGolem golem) {
		click(h, player, golem, false, ItemStack.EMPTY);
	}

	private static void assertHas(GameTestHelper h, Player player, Item item, int count) {
		h.assertValueEqual(player.getInventory().countItem(item), count, "player's " + item);
	}

	/** A farmer with a hoe, 8 wheat in its pouch and 4 seeds in its supplies. */
	private static CopperGolem loadedFarmer(GameTestHelper h, Player player) {
		CopperGolem golem = GameTests.golem(h, GOLEM);
		give(h, player, golem, Items.COPPER_HOE);
		Load.of(golem).stash(new ItemStack(Items.WHEAT, 8));
		Load.of(golem).addSupplies(new ItemStack(Items.WHEAT_SEEDS, 4));
		return golem;
	}

	private static void assertFarmerGaveEverything(GameTestHelper h, Player player, CopperGolem golem) {
		assertHas(h, player, Items.COPPER_HOE, 1);
		assertHas(h, player, Items.WHEAT, 8);
		assertHas(h, player, Items.WHEAT_SEEDS, 4);
		h.assertFalse(golem.hasAttached(ModAttachments.WORKER), "worker data left");
		h.assertFalse(golem.hasAttached(ModAttachments.LOAD), "load left");
	}

	/** How many of this item lie within a block of the player, who stands well away from the golem. */
	private static int droppedAt(GameTestHelper h, Player player, Item item) {
		return h.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(1.0), entity -> entity.getItem().is(item)).stream()
			.mapToInt(entity -> entity.getItem().getCount()).sum();
	}

	private static CourierRoute route(GameTestHelper h) {
		return new CourierRoute(h.absolutePos(new BlockPos(1, 1, 1)), h.absolutePos(new BlockPos(6, 1, 6)));
	}

	/** The plan in the player's inventory, or null. */
	private static @Nullable MinePlan planInInventory(Player player) {
		for (ItemStack stack : player.getInventory()) {
			if (stack.is(ModItems.MINESHAFT_PLAN)) {
				return stack.get(ModComponents.MINE_PLAN);
			}
		}
		return null;
	}

	private static ItemStack markedPlan(GameTestHelper h) {
		ItemStack plan = new ItemStack(ModItems.MINESHAFT_PLAN);
		plan.set(ModComponents.MINE_PLAN, MinePlan.blank(MinePattern.MINESHAFT, 8).marked(h.absolutePos(new BlockPos(4, 1, 1)), Direction.NORTH));
		return plan;
	}

	private static InteractionResult clickBlock(GameTestHelper h, Player player, BlockPos rel) {
		BlockPos pos = h.absolutePos(rel);
		player.setShiftKeyDown(false);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		return UseBlockCallback.EVENT.invoker().interact(player, h.getLevel(), InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
	}

	// --- ending a job ---

	@GameTest(maxTicks = 1200)
	public void after_taking_a_hoe_golem_stays_jobless_with_a_hoe_in_its_chest(GameTestHelper h) {
		JobGameTests.scene(h);
		BlockPos home = new BlockPos(2, 1, 4);
		GameTests.chest(h, home, Blocks.COPPER_CHEST.weathering().unaffected()).setItem(0, new ItemStack(Items.COPPER_HOE));
		Container target = GameTests.chest(h, new BlockPos(6, 1, 4), Blocks.CHEST);
		target.setItem(0, new ItemStack(Items.COPPER_HOE));
		Player player = player(h);
		CopperGolem golem = GameTests.golem(h, GOLEM);
		give(h, player, golem, Items.COPPER_HOE);
		Homes.set(golem, h.absolutePos(home));
		click(h, player, golem, true, ItemStack.EMPTY);
		h.startSequence()
			.thenExecuteAfter(NEW_PRESS, () -> {
				click(h, player, golem, true, ItemStack.EMPTY);
				assertHas(h, player, Items.COPPER_HOE, 1);
				h.onEachTick(() -> h.assertFalse(Jobs.has(golem), "the hoe gave it its job back"));
			})
			// Vanilla transport carried the spare hoe over.
			.thenWaitUntil(() -> h.assertValueEqual(target.countItem(Items.COPPER_HOE), 2, "hoes in the chest"))
			.thenSucceed();
	}

	@GameTest
	public void ending_returns_everything_and_a_loaded_cart_stays_loaded(GameTestHelper h) {
		JobGameTests.scene(h);
		Player player = player(h);
		CopperGolem golem = loadedFarmer(h, player);
		BlockPos home = h.absolutePos(new BlockPos(1, 1, 1));
		Homes.set(golem, home);
		CopperChestMinecart cart = JobGameTests.cart(h, golem, new BlockPos(5, 1, 4), new ItemStack(Items.CARROT, 5));
		endJob(h, player, golem, () -> {
			assertFarmerGaveEverything(h, player, golem);
			h.assertFalse(Jobs.has(golem), "still has a job");
			h.assertTrue(golem.getMainHandItem().isEmpty(), "still holds something");
			h.assertFalse(cart.isChainedTo(golem), "cart still chained");
			h.assertTrue(cart.isAlive(), "cart gone");
			h.assertValueEqual(cart.countItem(Items.CARROT), 5, "carrots in the cart");
			h.assertValueEqual(Homes.get(golem), home, "home");
		});
	}

	@GameTest
	public void job_ends_with_full_inventory_items_land_at_player(GameTestHelper h) {
		JobGameTests.scene(h);
		Player player = player(h);
		CopperGolem golem = loadedFarmer(h, player);
		for (int slot = 1; slot < 36; slot++) {
			player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
		}
		endJob(h, player, golem, () -> {
			// The empty hand's slot takes the hoe.
			assertHas(h, player, Items.COPPER_HOE, 1);
			h.assertValueEqual(droppedAt(h, player, Items.WHEAT), 8, "wheat at the player's feet");
			h.assertValueEqual(droppedAt(h, player, Items.WHEAT_SEEDS), 4, "seeds at the player's feet");
		});
	}

	@GameTest
	public void job_of_a_golem_with_a_lost_tool_ends_and_its_load_comes_back(GameTestHelper h) {
		JobGameTests.scene(h);
		Player player = player(h);
		CopperGolem golem = GameTests.golem(h, GOLEM);
		ItemStack worn = new ItemStack(Items.COPPER_HOE);
		worn.setDamageValue(worn.getMaxDamage() - 1);
		click(h, player, golem, true, worn);
		Load.of(golem).addSupplies(new ItemStack(Items.WHEAT_SEEDS, 4));
		Tools.wear(golem, golem.getMainHandItem());
		h.assertValueEqual(WorkerData.job(golem), Job.FARMER, "job after the tool broke");
		endJob(h, player, golem, () -> {
			h.assertFalse(Jobs.has(golem), "still has a job");
			h.assertFalse(golem.hasAttached(ModAttachments.LOST_TOOL), "still looks for its tool");
			assertHas(h, player, Items.WHEAT_SEEDS, 4);
		});
	}

	@GameTest
	public void ending_works_for_a_sorter(GameTestHelper h) {
		JobGameTests.scene(h);
		Player player = player(h);
		CopperGolem sorter = GameTests.golem(h, GOLEM);
		give(h, player, sorter, Items.BUNDLE);
		endJob(h, player, sorter, () -> {
			h.assertFalse(Jobs.has(sorter), "sorter still has a job");
			assertHas(h, player, Items.BUNDLE, 1);
		});
	}

	@GameTest
	public void ending_works_for_a_courier(GameTestHelper h) {
		JobGameTests.scene(h);
		Player player = player(h);
		CopperGolem courier = GameTests.golem(h, GOLEM);
		Couriers.setRoute(courier, route(h));
		courier.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_INGOT, 3));
		endJob(h, player, courier, () -> {
			h.assertFalse(Jobs.has(courier), "courier still has a job");
			assertHas(h, player, Items.IRON_INGOT, 3);
		});
	}

	@GameTest(maxTicks = 200)
	public void second_click_only_ends_the_job_within_five_seconds(GameTestHelper h) {
		JobGameTests.scene(h);
		Player player = player(h);
		CopperGolem golem = GameTests.golem(h, GOLEM);
		give(h, player, golem, Items.COPPER_HOE);
		click(h, player, golem, true, ItemStack.EMPTY);
		h.startSequence()
			.thenExecuteAfter(101, () -> {
				click(h, player, golem, true, ItemStack.EMPTY);
				h.assertTrue(Jobs.has(golem), "ended after the window");
			})
			.thenExecuteAfter(NEW_PRESS, () -> {
				click(h, player, golem, true, ItemStack.EMPTY);
				h.assertFalse(Jobs.has(golem), "not ended by a click in the window");
			})
			.thenSucceed();
	}

	/** The button held down repeats the click every 4 ticks: one press, however long. */
	@GameTest(maxTicks = 100)
	public void held_button_is_one_press_and_a_new_press_ends_the_job(GameTestHelper h) {
		JobGameTests.scene(h);
		Player player = player(h);
		CopperGolem golem = GameTests.golem(h, GOLEM);
		give(h, player, golem, Items.COPPER_HOE);
		Runnable sneakClick = () -> click(h, player, golem, true, ItemStack.EMPTY);
		sneakClick.run();
		h.startSequence()
			.thenExecuteAfter(4, () -> {
				sneakClick.run();
				h.assertTrue(Jobs.has(golem), "ended at t+4");
			})
			.thenExecuteAfter(4, () -> {
				sneakClick.run();
				h.assertTrue(Jobs.has(golem), "ended at t+8");
			})
			.thenExecuteAfter(7, () -> {
				sneakClick.run();
				h.assertFalse(Jobs.has(golem), "not ended at t+15");
			})
			.thenSucceed();
	}

	/** The hoe comes back into the selected hand, so a held button repeats with the hoe in it. */
	@GameTest(maxTicks = 100)
	public void rest_of_the_ending_press_does_not_give_the_job_back(GameTestHelper h) {
		JobGameTests.scene(h);
		Player player = player(h);
		CopperGolem golem = GameTests.golem(h, GOLEM);
		give(h, player, golem, Items.COPPER_HOE);
		Runnable repeat = () -> {
			click(h, player, golem, true, player.getMainHandItem());
			h.assertFalse(Jobs.has(golem), "the repeat gave the job back");
			h.assertTrue(player.getMainHandItem().is(Items.COPPER_HOE), "the repeat took the hoe");
		};
		click(h, player, golem, true, ItemStack.EMPTY);
		h.startSequence()
			.thenExecuteAfter(NEW_PRESS, () -> {
				click(h, player, golem, true, ItemStack.EMPTY);
				h.assertFalse(Jobs.has(golem), "not ended");
				h.assertTrue(player.getMainHandItem().is(Items.COPPER_HOE), "the hoe is not in the selected hand");
			})
			.thenExecuteAfter(4, repeat)
			// More than a gap after the end, so only the refreshing keeps this one out.
			.thenExecuteAfter(4, repeat)
			.thenExecuteAfter(12, () -> {
				click(h, player, golem, true, player.getMainHandItem());
				h.assertTrue(Jobs.has(golem), "a new press gave no job");
			})
			.thenSucceed();
	}

	@GameTest
	public void ending_press_does_not_stop_clicks_on_another_golem(GameTestHelper h) {
		JobGameTests.scene(h);
		Player player = player(h);
		CopperGolem golem = GameTests.golem(h, GOLEM);
		CopperGolem other = GameTests.golem(h, GOLEM);
		give(h, player, golem, Items.COPPER_HOE);
		endJob(h, player, golem, () -> {
			click(h, player, other, true, player.getMainHandItem());
			h.assertTrue(Jobs.has(other), "the other golem refused the hoe");
		});
	}

	/** The button held from giving the hoe: it also repeats on the now empty hand. */
	@GameTest(maxTicks = 100)
	public void held_button_after_giving_a_tool_does_not_end_the_job(GameTestHelper h) {
		JobGameTests.scene(h);
		Player player = player(h);
		CopperGolem golem = GameTests.golem(h, GOLEM);
		give(h, player, golem, Items.COPPER_HOE);
		h.startSequence()
			.thenExecuteAfter(4, () -> click(h, player, golem, true, ItemStack.EMPTY))
			.thenExecuteAfter(4, () -> {
				click(h, player, golem, true, ItemStack.EMPTY);
				h.assertTrue(Jobs.has(golem), "ended while the button was held");
			})
			.thenSucceed();
	}

	@GameTest(maxTicks = 100)
	public void first_click_must_have_said_the_second_ends_the_job(GameTestHelper h) {
		JobGameTests.scene(h);
		Player player = player(h);
		CopperGolem golem = GameTests.golem(h, GOLEM);
		click(h, player, golem, true, ItemStack.EMPTY);
		h.startSequence()
			.thenExecuteAfter(NEW_PRESS, () -> give(h, player, golem, Items.COPPER_HOE))
			.thenExecuteAfter(NEW_PRESS, () -> {
				click(h, player, golem, true, ItemStack.EMPTY);
				h.assertTrue(Jobs.has(golem), "first click on a jobless golem counted");
			})
			.thenSucceed();
	}

	@GameTest(maxTicks = 100)
	public void giving_a_job_closes_the_window_to_end_one(GameTestHelper h) {
		JobGameTests.scene(h);
		Player player = player(h);
		CopperGolem golem = GameTests.golem(h, GOLEM);
		give(h, player, golem, Items.COPPER_HOE);
		click(h, player, golem, true, ItemStack.EMPTY);
		h.startSequence()
			.thenExecuteAfter(NEW_PRESS, () -> give(h, player, golem, Items.COPPER_PICKAXE))
			.thenExecuteAfter(NEW_PRESS, () -> {
				click(h, player, golem, true, ItemStack.EMPTY);
				h.assertValueEqual(Jobs.of(golem), JobKind.MINER, "the new job");
			})
			.thenSucceed();
	}

	@GameTest
	public void ending_a_miners_job_returns_its_plan_with_progress(GameTestHelper h) {
		JobGameTests.scene(h);
		Player player = player(h);
		CopperGolem golem = GameTests.golem(h, GOLEM);
		give(h, player, golem, Items.COPPER_PICKAXE);
		click(h, player, golem, true, markedPlan(h));
		MinerData data = MinerData.get(golem);
		data.setPlan(data.plan().withProgress(5));
		golem.setAttached(ModAttachments.EXCITED, true);
		endJob(h, player, golem, () -> {
			h.assertValueEqual(planInInventory(player).progress(), 5, "plan's progress");
			assertHas(h, player, Items.COPPER_PICKAXE, 1);
			h.assertFalse(golem.hasAttached(ModAttachments.MINER), "miner data left");
			h.assertFalse(golem.hasAttached(ModAttachments.EXCITED), "still excited");
		});
	}

	@GameTest
	public void a_new_plan_returns_the_old_one(GameTestHelper h) {
		JobGameTests.scene(h);
		Player player = player(h);
		CopperGolem golem = GameTests.golem(h, GOLEM);
		give(h, player, golem, Items.COPPER_PICKAXE);
		click(h, player, golem, true, markedPlan(h));
		MinerData data = MinerData.get(golem);
		data.setPlan(data.plan().withProgress(5));
		click(h, player, golem, true, markedPlan(h));
		h.assertValueEqual(data.plan().progress(), 0, "the new plan");
		h.assertValueEqual(planInInventory(player).progress(), 5, "the old plan's progress");
		h.succeed();
	}

	@GameTest
	public void miner_with_a_lost_pickaxe_ends_and_its_plan_comes_back(GameTestHelper h) {
		JobGameTests.scene(h);
		Player player = player(h);
		CopperGolem golem = GameTests.golem(h, GOLEM);
		ItemStack worn = new ItemStack(Items.COPPER_PICKAXE);
		worn.setDamageValue(worn.getMaxDamage() - 1);
		click(h, player, golem, true, worn);
		click(h, player, golem, true, markedPlan(h));
		Tools.wear(golem, golem.getMainHandItem());
		h.assertValueEqual(Jobs.of(golem), JobKind.MINER, "job after the pickaxe broke");
		endJob(h, player, golem, () -> {
			h.assertFalse(Jobs.has(golem), "still has a job");
			h.assertFalse(golem.hasAttached(ModAttachments.LOST_TOOL), "still looks for its pickaxe");
			h.assertTrue(planInInventory(player) != null, "plan not back");
		});
	}

	@GameTest
	public void ending_returns_repair_iron(GameTestHelper h) {
		JobGameTests.scene(h);
		Player player = player(h);
		CopperGolem golem = GameTests.golem(h, GOLEM);
		Couriers.setRoute(golem, route(h));
		golem.setAttached(ModAttachments.CARRIED_IRON, 2);
		endJob(h, player, golem, () -> {
			assertHas(h, player, Items.IRON_INGOT, 2);
			h.assertFalse(golem.hasAttached(ModAttachments.CARRIED_IRON), "iron left on the golem");
		});
	}

	/** With nowhere to belong the errand stops at once, or it would go on fetching iron from the chest nearby. */
	@GameTest(maxTicks = 400)
	public void ex_courier_drops_its_repair_errand(GameTestHelper h) {
		JobGameTests.scene(h);
		GameTests.chest(h, new BlockPos(1, 1, 4), Blocks.CHEST).setItem(0, new ItemStack(Items.IRON_INGOT, 64));
		IronGolem patient = h.spawn(EntityTypes.IRON_GOLEM, new BlockPos(6, 1, 6));
		patient.setHealth(20.0F);
		Player player = player(h);
		CopperGolem golem = GameTests.golem(h, GOLEM);
		Couriers.setRoute(golem, route(h));
		h.startSequence()
			.thenWaitUntil(() -> h.assertTrue(Errands.isBusy(golem), "no errand yet"))
			.thenExecute(() -> click(h, player, golem, true, ItemStack.EMPTY))
			.thenExecuteAfter(NEW_PRESS, () -> click(h, player, golem, true, ItemStack.EMPTY))
			.thenExecuteAfter(3, () -> {
				h.assertFalse(Couriers.isCourier(golem), "still a courier");
				h.assertFalse(Errands.isBusy(golem), "still on its errand");
			})
			.thenSucceed();
	}

	@GameTest
	public void ending_a_job_cancels_a_pending_route(GameTestHelper h) {
		JobGameTests.scene(h);
		CopperGolem golem = GameTests.golem(h, GOLEM);
		golem.discard();
		Couriers.assign(h.getLevel(), golem.getUUID(), route(h));
		JobHandover.clear(player(h), golem);
		Couriers.onEntityLoad(golem, h.getLevel());
		h.assertFalse(Couriers.isCourier(golem), "got the route back");
		h.succeed();
	}

	// --- changing a job ---

	@GameTest
	public void farmer_to_miner_returns_the_load_and_keeps_the_cart(GameTestHelper h) {
		JobGameTests.scene(h);
		Player player = player(h);
		CopperGolem golem = loadedFarmer(h, player);
		CopperChestMinecart cart = JobGameTests.cart(h, golem, new BlockPos(5, 1, 4), new ItemStack(Items.CARROT, 5));
		give(h, player, golem, Items.COPPER_PICKAXE);
		assertFarmerGaveEverything(h, player, golem);
		h.assertValueEqual(Jobs.of(golem), JobKind.MINER, "job");
		h.assertTrue(cart.isChainedTo(golem), "cart unchained");
		h.assertValueEqual(cart.countItem(Items.CARROT), 5, "carrots in the cart");
		h.succeed();
	}

	@GameTest
	public void sorter_to_farmer_returns_bundle_and_cargo(GameTestHelper h) {
		JobGameTests.scene(h);
		Player player = player(h);
		CopperGolem golem = GameTests.golem(h, GOLEM);
		give(h, player, golem, Items.BUNDLE);
		golem.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.COBBLESTONE, 3));
		give(h, player, golem, Items.COPPER_HOE);
		assertHas(h, player, Items.BUNDLE, 1);
		assertHas(h, player, Items.COBBLESTONE, 3);
		h.assertValueEqual(Jobs.of(golem), JobKind.WORKER, "job");
		h.assertTrue(golem.getOffhandItem().isEmpty(), "still holds the bundle");
		h.succeed();
	}

	@GameTest
	public void courier_to_farmer_clears_the_route_and_returns_cargo(GameTestHelper h) {
		JobGameTests.scene(h);
		Player player = player(h);
		CopperGolem golem = GameTests.golem(h, GOLEM);
		Couriers.setRoute(golem, route(h));
		golem.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_INGOT, 3));
		give(h, player, golem, Items.COPPER_HOE);
		assertHas(h, player, Items.IRON_INGOT, 3);
		h.assertFalse(Couriers.isCourier(golem), "still on its route");
		h.assertValueEqual(WorkerData.job(golem), Job.FARMER, "job");
		h.succeed();
	}

	@GameTest
	public void farmer_to_sorter_returns_the_load(GameTestHelper h) {
		JobGameTests.scene(h);
		Player player = player(h);
		CopperGolem golem = loadedFarmer(h, player);
		give(h, player, golem, Items.BUNDLE);
		assertFarmerGaveEverything(h, player, golem);
		h.assertValueEqual(Jobs.of(golem), JobKind.SORTER, "job");
		h.succeed();
	}

	@GameTest
	public void same_job_with_a_new_tool_keeps_the_load(GameTestHelper h) {
		JobGameTests.scene(h);
		Player player = player(h);
		CopperGolem golem = loadedFarmer(h, player);
		give(h, player, golem, Items.COPPER_HOE);
		assertHas(h, player, Items.COPPER_HOE, 1);
		assertHas(h, player, Items.WHEAT_SEEDS, 0);
		h.assertValueEqual(Load.of(golem).suppliesCount(stack -> stack.is(Items.WHEAT_SEEDS)), 4, "seeds kept");
		h.assertValueEqual(Load.of(golem).count(stack -> stack.is(Items.WHEAT)), 8, "wheat kept");
		h.assertTrue(golem.getMainHandItem().is(Items.COPPER_HOE), "no hoe in hand");
		h.succeed();
	}

	@GameTest
	public void new_pickaxe_keeps_the_plan(GameTestHelper h) {
		JobGameTests.scene(h);
		Player player = player(h);
		CopperGolem golem = GameTests.golem(h, GOLEM);
		give(h, player, golem, Items.COPPER_PICKAXE);
		click(h, player, golem, true, markedPlan(h));
		give(h, player, golem, Items.COPPER_PICKAXE);
		h.assertTrue(MinerData.get(golem).plan() != null, "plan gone");
		assertHas(h, player, ModItems.MINESHAFT_PLAN, 0);
		h.succeed();
	}

	@GameTest
	public void the_api_tool_ends_any_other_job(GameTestHelper h) {
		JobGameTests.scene(h);
		Player player = player(h);
		CopperGolem courier = GameTests.golem(h, GOLEM);
		Couriers.setRoute(courier, route(h));
		BetterCoppersApi.giveTool(player, courier, new ItemStack(Items.STICK));
		h.assertFalse(Couriers.isCourier(courier), "courier kept its route");
		h.assertTrue(courier.getMainHandItem().is(Items.STICK), "no tool in hand");
		CopperGolem worker = loadedFarmer(h, player);
		BetterCoppersApi.giveTool(player, worker, new ItemStack(Items.STICK));
		assertFarmerGaveEverything(h, player, worker);
		JobGameTests.ADDON_WORKPLACES.put(worker.getUUID(), null);
		give(h, player, worker, Items.COPPER_HOE);
		h.assertFalse(JobGameTests.ADDON_WORKPLACES.containsKey(worker.getUUID()), "add-on job not told it ended");
		h.assertValueEqual(Jobs.of(worker), JobKind.WORKER, "job");
		h.succeed();
	}

	// --- refusals ---

	@GameTest
	public void route_is_refused_at_the_first_chest_for_a_golem_with_a_job(GameTestHelper h) {
		JobGameTests.scene(h);
		BlockPos first = new BlockPos(1, 1, 1);
		BlockPos second = new BlockPos(6, 1, 6);
		h.setBlock(first, Blocks.CHEST);
		h.setBlock(second, Blocks.CHEST);
		Player player = player(h);
		CopperGolem sorter = GameTests.golem(h, GOLEM);
		give(h, player, sorter, Items.BUNDLE);
		CopperGolem addOn = GameTests.golem(h, GOLEM);
		JobGameTests.ADDON_WORKPLACES.put(addOn.getUUID(), null);
		for (CopperGolem golem : new CopperGolem[] {sorter, addOn}) {
			click(h, player, golem, true, ItemStack.EMPTY);
			h.assertValueEqual(clickBlock(h, player, first), InteractionResult.FAIL, "first chest");
			h.assertValueEqual(clickBlock(h, player, second), InteractionResult.PASS, "second chest opens as usual");
			h.assertFalse(Couriers.isCourier(golem), "got a route");
		}
		JobGameTests.ADDON_WORKPLACES.remove(addOn.getUUID());
		h.succeed();
	}

	@GameTest
	public void route_is_refused_if_the_golem_got_a_job_on_the_way(GameTestHelper h) {
		JobGameTests.scene(h);
		BlockPos first = new BlockPos(1, 1, 1);
		BlockPos second = new BlockPos(6, 1, 6);
		h.setBlock(first, Blocks.CHEST);
		h.setBlock(second, Blocks.CHEST);
		Player player = player(h);
		CopperGolem golem = GameTests.golem(h, GOLEM);
		click(h, player, golem, true, ItemStack.EMPTY);
		h.assertTrue(clickBlock(h, player, first).consumesAction(), "jobless golem refused at the first chest");
		give(h, player, golem, Items.COPPER_HOE);
		h.assertValueEqual(clickBlock(h, player, second), InteractionResult.FAIL, "second chest");
		h.assertFalse(Couriers.isCourier(golem), "worker became a courier");
		h.succeed();
	}

	@GameTest
	public void jobless_golem_still_takes_a_route(GameTestHelper h) {
		JobGameTests.scene(h);
		BlockPos first = new BlockPos(1, 1, 1);
		BlockPos second = new BlockPos(6, 1, 6);
		h.setBlock(first, Blocks.CHEST);
		h.setBlock(second, Blocks.CHEST);
		Player player = player(h);
		CopperGolem golem = GameTests.golem(h, GOLEM);
		click(h, player, golem, true, ItemStack.EMPTY);
		clickBlock(h, player, first);
		clickBlock(h, player, second);
		h.assertTrue(Couriers.isCourier(golem), "no route");
		h.succeed();
	}

	@GameTest
	public void plan_is_refused_without_a_pickaxe_in_hand(GameTestHelper h) {
		JobGameTests.scene(h);
		Player player = player(h);
		CopperGolem golem = GameTests.golem(h, GOLEM);
		ItemStack plan = markedPlan(h);
		click(h, player, golem, true, plan);
		h.assertValueEqual(plan.getCount(), 1, "plan kept by the player");
		h.assertFalse(golem.hasAttached(ModAttachments.MINER), "miner data from a refused plan");
		// One it picked up itself does not count either.
		golem.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.COPPER_PICKAXE));
		click(h, player, golem, true, plan);
		h.assertValueEqual(plan.getCount(), 1, "plan kept by the player");
		h.assertFalse(Jobs.has(golem), "got a job from a refused plan");
		h.succeed();
	}

	// --- clicks ---

	@GameTest
	public void plain_click_on_jobless_golem_is_vanilla(GameTestHelper h) {
		JobGameTests.scene(h);
		CopperGolem golem = GameTests.golem(h, GOLEM);
		golem.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.COBBLESTONE, 3));
		Homes.set(golem, h.absolutePos(new BlockPos(1, 1, 1)));
		plainClick(h, player(h), golem);
		h.assertTrue(golem.getMainHandItem().isEmpty(), "still holds its cargo");
		h.assertValueEqual(GameTests.dropped(h, Items.COBBLESTONE), 3, "thrown cargo");
		h.succeed();
	}

	@GameTest
	public void plain_click_on_job_golem_changes_nothing(GameTestHelper h) {
		JobGameTests.scene(h);
		Player player = player(h);
		CopperGolem farmer = loadedFarmer(h, player);
		plainClick(h, player, farmer);
		h.assertTrue(farmer.getMainHandItem().is(Items.COPPER_HOE), "lost its hoe");
		h.assertValueEqual(Load.of(farmer).count(stack -> stack.is(Items.WHEAT)), 8, "wheat");
		CopperGolem sorter = GameTests.golem(h, GOLEM);
		give(h, player, sorter, Items.BUNDLE);
		sorter.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.COBBLESTONE, 3));
		plainClick(h, player, sorter);
		h.assertValueEqual(sorter.getMainHandItem().getCount(), 3, "sorter's cargo");
		h.assertTrue(Sorter.isSorter(sorter), "lost its bundle");
		CopperGolem courier = GameTests.golem(h, GOLEM);
		Couriers.setRoute(courier, route(h));
		courier.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_INGOT, 3));
		plainClick(h, player, courier);
		h.assertValueEqual(courier.getMainHandItem().getCount(), 3, "courier's cargo");
		h.assertValueEqual(GameTests.dropped(h, Items.COBBLESTONE) + GameTests.dropped(h, Items.IRON_INGOT), 0, "thrown items");
		// A real connection gets the golem's hand back.
		plainClick(h, h.makeMockServerPlayerInLevel(), farmer);
		h.assertTrue(farmer.getMainHandItem().is(Items.COPPER_HOE), "lost its hoe to a server player");
		h.succeed();
	}

	/** The main hand's item does nothing, so vanilla goes on to the empty off hand, which would throw. */
	@GameTest
	public void plain_click_with_any_item_never_throws_a_job_golems_tool(GameTestHelper h) {
		JobGameTests.scene(h);
		Player player = player(h);
		CopperGolem golem = GameTests.golem(h, GOLEM);
		give(h, player, golem, Items.COPPER_HOE);
		click(h, player, golem, false, new ItemStack(Items.DIRT));
		h.assertTrue(golem.getMainHandItem().is(Items.COPPER_HOE), "plain dirt threw the hoe");
		click(h, player, golem, true, new ItemStack(Items.DIRT));
		h.assertTrue(golem.getMainHandItem().is(Items.COPPER_HOE), "sneak dirt threw the hoe");
		click(h, player, golem, false, new ItemStack(Items.COPPER_PICKAXE));
		h.assertTrue(golem.getMainHandItem().is(Items.COPPER_HOE), "plain pickaxe changed the job");
		h.succeed();
	}

	@GameTest
	public void plain_click_with_a_job_item_gives_nothing(GameTestHelper h) {
		JobGameTests.scene(h);
		Player player = player(h);
		CopperGolem golem = GameTests.golem(h, GOLEM);
		for (Item item : new Item[] {Items.COPPER_HOE, Items.COPPER_PICKAXE, Items.BUNDLE, Items.IRON_BLOCK, Items.IRON_HOE}) {
			ItemStack stack = new ItemStack(item);
			click(h, player, golem, false, stack);
			h.assertValueEqual(stack.getCount(), 1, "player kept " + item);
		}
		h.assertFalse(Jobs.has(golem), "got a job");
		h.succeed();
	}

	@GameTest
	public void plain_axe_still_scrapes_job_golem(GameTestHelper h) {
		JobGameTests.scene(h);
		Player player = player(h);
		CopperGolem golem = GameTests.golem(h, GOLEM);
		give(h, player, golem, Items.COPPER_HOE);
		golem.setWeatherState(WeatheringCopper.WeatherState.EXPOSED);
		click(h, player, golem, false, new ItemStack(Items.IRON_AXE));
		h.assertValueEqual(golem.getWeatherState(), WeatheringCopper.WeatherState.UNAFFECTED, "rust");
		h.assertTrue(golem.getMainHandItem().is(Items.COPPER_HOE), "lost its hoe");
		// On a clean golem nothing happens, and it is not told off.
		click(h, player, golem, false, new ItemStack(Items.IRON_AXE));
		h.assertTrue(golem.getMainHandItem().is(Items.COPPER_HOE), "lost its hoe");
		// Sneaking does not change it: the axe scrapes and is not given.
		golem.setWeatherState(WeatheringCopper.WeatherState.EXPOSED);
		click(h, player, golem, true, new ItemStack(Items.COPPER_AXE));
		h.assertValueEqual(golem.getWeatherState(), WeatheringCopper.WeatherState.UNAFFECTED, "rust");
		h.assertValueEqual(WorkerData.job(golem), Job.FARMER, "job");
		h.succeed();
	}

	@GameTest
	public void shears_take_a_trim_off_only_with_a_sneak_and_after_the_flower(GameTestHelper h) {
		JobGameTests.scene(h);
		Player player = player(h);
		CopperGolem golem = GameTests.golem(h, GOLEM);
		give(h, player, golem, Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE);
		give(h, player, golem, Items.IRON_INGOT);
		h.assertTrue(GolemTrims.trim(golem) != null, "no trim");
		click(h, player, golem, false, new ItemStack(Items.SHEARS));
		h.assertTrue(GolemTrims.trim(golem) != null, "plain shears took the trim off");
		golem.setItemSlot(CopperGolem.EQUIPMENT_SLOT_ANTENNA, new ItemStack(Items.POPPY));
		click(h, player, golem, true, new ItemStack(Items.SHEARS));
		h.assertTrue(golem.getItemBySlot(CopperGolem.EQUIPMENT_SLOT_ANTENNA).isEmpty(), "the flower stayed");
		h.assertTrue(GolemTrims.trim(golem) != null, "the trim came off before the flower");
		click(h, player, golem, true, new ItemStack(Items.SHEARS));
		h.assertTrue(GolemTrims.trim(golem) == null, "sneak shears left the trim on");
		assertHas(h, player, Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE, 1);
		h.succeed();
	}

	@GameTest
	public void chained_cart_chains_without_taking_the_tool(GameTestHelper h) {
		JobGameTests.scene(h);
		Player player = player(h);
		CopperGolem golem = GameTests.golem(h, GOLEM);
		give(h, player, golem, Items.COPPER_HOE);
		CopperChestMinecart cart = h.spawn(ModEntities.COPPER_CHEST_MINECART, new BlockPos(5, 1, 5));
		cart.chainTo(player);
		plainClick(h, player, golem);
		h.assertTrue(cart.isChainedTo(golem), "cart not handed over");
		h.assertTrue(golem.getMainHandItem().is(Items.COPPER_HOE), "lost its hoe");
		h.succeed();
	}
}

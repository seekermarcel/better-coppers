package dev.bettercoppers.test;

import dev.bettercoppers.api.BetterCoppersApi;
import dev.bettercoppers.api.GolemJob;
import dev.bettercoppers.courier.CourierRoute;
import dev.bettercoppers.courier.Couriers;
import dev.bettercoppers.entity.CopperChestMinecart;
import dev.bettercoppers.golem.Golems;
import dev.bettercoppers.golem.HomeStroll;
import dev.bettercoppers.golem.SortItemsBehavior;
import dev.bettercoppers.home.Homes;
import dev.bettercoppers.job.CraftBehavior;
import dev.bettercoppers.job.FarmBehavior;
import dev.bettercoppers.job.FishBehavior;
import dev.bettercoppers.job.ForestBehavior;
import dev.bettercoppers.job.Job;
import dev.bettercoppers.job.Jobs;
import dev.bettercoppers.job.Jobs.JobKind;
import dev.bettercoppers.job.KeeperBehavior;
import dev.bettercoppers.job.Load;
import dev.bettercoppers.job.ToolFetchBehavior;
import dev.bettercoppers.job.Tools;
import dev.bettercoppers.job.WorkerData;
import dev.bettercoppers.mining.MineBehavior;
import dev.bettercoppers.mining.MineLayout;
import dev.bettercoppers.mining.MinePattern;
import dev.bettercoppers.mining.MinePlan;
import dev.bettercoppers.mining.MinerData;
import dev.bettercoppers.registry.ModAttachments;
import dev.bettercoppers.registry.ModComponents;
import dev.bettercoppers.registry.ModEntities;
import dev.bettercoppers.registry.ModItems;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.jspecify.annotations.Nullable;

/** One game test per job: each builds a small scene, hands the golem its tool the way a player would, and waits. */
public class JobGameTests {
	private static final Block COPPER_CHEST = Blocks.COPPER_CHEST.weathering().unaffected();

	/** Stone floor under the test area and daytime, so job golems are allowed to work. */
	static void scene(GameTestHelper h) {
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
		player.setShiftKeyDown(true);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		UseEntityCallback.EVENT.invoker().interact(player, h.getLevel(), InteractionHand.MAIN_HAND, golem, null);
	}

	private static int count(Container container, Item item) {
		return container.countItem(item);
	}

	/** Stands in for an add-on's job, for the golems put in this map. */
	static final Map<UUID, BlockPos> ADDON_WORKPLACES = new HashMap<>();

	static {
		BetterCoppersApi.registerJob(new GolemJob() {
			@Override
			public boolean has(CopperGolem golem) {
				return ADDON_WORKPLACES.containsKey(golem.getUUID());
			}

			@Override
			public Component name() {
				return Component.literal("test job");
			}

			@Override
			public @Nullable BlockPos workplace(CopperGolem golem) {
				return ADDON_WORKPLACES.get(golem.getUUID());
			}

			@Override
			public void onEnded(CopperGolem golem) {
				ADDON_WORKPLACES.remove(golem.getUUID());
			}
		});
	}

	private static void hold(CopperGolem golem, Item item) {
		golem.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(item));
	}

	private static void loseTool(CopperGolem golem, Item item) {
		golem.setAttached(ModAttachments.LOST_TOOL, item);
	}

	private static void pouch(CopperGolem golem) {
		Load.of(golem).stash(new ItemStack(Items.COBBLESTONE));
	}

	private record OrderRow(String name, JobKind expected, Consumer<CopperGolem> setup) {
	}

	@GameTest
	public void job_order_table(GameTestHelper h) {
		scene(h);
		ItemStack bundle = new ItemStack(Items.BUNDLE);
		CourierRoute route = new CourierRoute(h.absolutePos(new BlockPos(1, 1, 1)), h.absolutePos(new BlockPos(6, 1, 6)));
		BlockPos home = h.absolutePos(new BlockPos(2, 1, 2));
		List<OrderRow> rows = List.of(
			new OrderRow("untouched", JobKind.NONE, g -> {
			}),
			new OrderRow("only a home", JobKind.NONE, g -> Homes.set(g, home)),
			new OrderRow("only a home, holding a hoe", JobKind.NONE, g -> {
				Homes.set(g, home);
				hold(g, Items.COPPER_HOE);
			}),
			new OrderRow("holding a pickaxe it picked up", JobKind.NONE, g -> hold(g, Items.COPPER_PICKAXE)),
			new OrderRow("load but no data", JobKind.NONE, JobGameTests::pouch),
			new OrderRow("miner data, holding a hoe", JobKind.NONE, g -> {
				MinerData.getOrCreate(g);
				hold(g, Items.COPPER_HOE);
			}),
			new OrderRow("worker data, nothing else", JobKind.NONE, WorkerData::getOrCreate),
			new OrderRow("worker data and only supplies", JobKind.NONE, g -> {
				WorkerData.getOrCreate(g);
				Load.of(g).addSupplies(new ItemStack(Items.WHEAT_SEEDS));
			}),
			new OrderRow("farmer", JobKind.WORKER, g -> give(h, g, new ItemStack(Items.COPPER_HOE))),
			new OrderRow("miner", JobKind.MINER, g -> give(h, g, new ItemStack(Items.COPPER_PICKAXE))),
			new OrderRow("farmer with a lost tool", JobKind.WORKER, g -> {
				WorkerData.getOrCreate(g);
				loseTool(g, Items.COPPER_HOE);
			}),
			new OrderRow("miner with a lost tool", JobKind.MINER, g -> {
				MinerData.getOrCreate(g);
				loseTool(g, Items.COPPER_PICKAXE);
			}),
			new OrderRow("miner handed a hoe", JobKind.WORKER, g -> {
				give(h, g, new ItemStack(Items.COPPER_PICKAXE));
				give(h, g, new ItemStack(Items.COPPER_HOE));
			}),
			new OrderRow("farmer handed a pickaxe", JobKind.MINER, g -> {
				give(h, g, new ItemStack(Items.COPPER_HOE));
				give(h, g, new ItemStack(Items.COPPER_PICKAXE));
			}),
			new OrderRow("stale data and a load is no job", JobKind.NONE, g -> {
				WorkerData.getOrCreate(g);
				MinerData.getOrCreate(g);
				pouch(g);
			}),
			new OrderRow("sorter", JobKind.SORTER, g -> give(h, g, bundle.copy())),
			new OrderRow("sorter carrying a hoe it picked up", JobKind.SORTER, g -> {
				give(h, g, bundle.copy());
				hold(g, Items.COPPER_HOE);
			}),
			new OrderRow("sorter with stale data and a load", JobKind.SORTER, g -> {
				give(h, g, bundle.copy());
				WorkerData.getOrCreate(g);
				pouch(g);
			}),
			new OrderRow("sorter handed a hoe", JobKind.WORKER, g -> {
				give(h, g, bundle.copy());
				give(h, g, new ItemStack(Items.COPPER_HOE));
			}),
			new OrderRow("courier", JobKind.COURIER, g -> Couriers.setRoute(g, route)),
			new OrderRow("courier carrying a hoe, stale data", JobKind.COURIER, g -> {
				give(h, g, new ItemStack(Items.COPPER_HOE));
				Couriers.setRoute(g, route);
			}),
			new OrderRow("courier with stale data and a load", JobKind.COURIER, g -> {
				WorkerData.getOrCreate(g);
				pouch(g);
				Couriers.setRoute(g, route);
			}),
			new OrderRow("add-on job", JobKind.ADDON, g -> ADDON_WORKPLACES.put(g.getUUID(), null)),
			new OrderRow("add-on job beats a tool", JobKind.ADDON, g -> {
				give(h, g, new ItemStack(Items.COPPER_HOE));
				ADDON_WORKPLACES.put(g.getUUID(), null);
			}),
			new OrderRow("route beats add-on job", JobKind.COURIER, g -> {
				ADDON_WORKPLACES.put(g.getUUID(), null);
				Couriers.setRoute(g, route);
			})
		);
		for (OrderRow row : rows) {
			CopperGolem golem = GameTests.golem(h, new BlockPos(4, 1, 4));
			row.setup().accept(golem);
			h.assertValueEqual(Jobs.of(golem), row.expected(), row.name());
			h.assertValueEqual(Jobs.has(golem), row.expected() != JobKind.NONE, row.name() + ": has");
			boolean paying = row.expected() == JobKind.MINER || row.expected() == JobKind.WORKER || row.expected() == JobKind.ADDON;
			h.assertValueEqual(Jobs.isPaying(golem), paying, row.name() + ": paying");
			ADDON_WORKPLACES.remove(golem.getUUID());
			golem.discard();
		}
		h.succeed();
	}

	@GameTest
	public void place_is_workplace_then_home(GameTestHelper h) {
		scene(h);
		BlockPos home = h.absolutePos(new BlockPos(2, 1, 2));
		BlockPos workplace = h.absolutePos(new BlockPos(6, 1, 6));
		CopperGolem golem = GameTests.golem(h, new BlockPos(4, 1, 4));
		h.assertTrue(Jobs.place(golem) == null, "untouched golem has a place");
		Homes.set(golem, home);
		h.assertValueEqual(Jobs.place(golem), home, "home");
		ADDON_WORKPLACES.put(golem.getUUID(), workplace);
		h.assertValueEqual(Jobs.place(golem), workplace, "workplace");
		ADDON_WORKPLACES.remove(golem.getUUID());
		h.assertValueEqual(Jobs.place(golem), home, "home again when the job ends");
		Couriers.setRoute(golem, new CourierRoute(home, workplace));
		h.assertTrue(Jobs.place(golem) == null, "a route did not clear the home");
		// A home set on a courier is its place too, and only the courier check in HomeStroll keeps it on its route.
		BlockPos far = h.absolutePos(new BlockPos(30, 1, 30));
		Homes.set(golem, far);
		h.assertValueEqual(Jobs.place(golem), far, "courier with a home");
		golem.getBrain().setMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS, 400);
		h.assertFalse(new HomeStroll().tryStart(h.getLevel(), golem, 0L), "walked a courier back from its route");
		Couriers.setRoute(golem, null);
		h.assertTrue(new HomeStroll().tryStart(h.getLevel(), golem, 0L), "did not walk back a golem far from home");
		h.succeed();
	}

	private record StartRow(String name, Set<String> expected, Consumer<CopperGolem> setup) {
	}

	/** Who starts for a golem that may seem to have two jobs. */
	@GameTest
	public void exactly_one_behaviour_starts(GameTestHelper h) {
		scene(h);
		BlockPos home = new BlockPos(2, 1, 2);
		GameTests.chest(h, home, COPPER_CHEST);
		Map<String, Supplier<? extends Behavior<CopperGolem>>> behaviours = Map.of(
			"mine", MineBehavior::new,
			"forest", ForestBehavior::new,
			"fish", FishBehavior::new,
			"keep", KeeperBehavior::new,
			"craft", CraftBehavior::new,
			"farm", FarmBehavior::new,
			"sort", SortItemsBehavior::new,
			"fetch", ToolFetchBehavior::new
		);
		CourierRoute route = new CourierRoute(h.absolutePos(new BlockPos(1, 1, 1)), h.absolutePos(new BlockPos(6, 1, 6)));
		List<StartRow> rows = List.of(
			new StartRow("stale data and a load", Set.of(), g -> {
				WorkerData.getOrCreate(g);
				MinerData.getOrCreate(g);
				pouch(g);
			}),
			new StartRow("broken hoe, both datas, load", Set.of("farm"), g -> {
				WorkerData.getOrCreate(g);
				MinerData.getOrCreate(g);
				loseTool(g, Items.COPPER_HOE);
				pouch(g);
			}),
			new StartRow("broken pickaxe, both datas, load", Set.of("mine"), g -> {
				WorkerData.getOrCreate(g);
				MinerData.getOrCreate(g);
				loseTool(g, Items.COPPER_PICKAXE);
				pouch(g);
			}),
			new StartRow("hoe, stale miner data, load", Set.of("farm"), g -> {
				give(h, g, new ItemStack(Items.COPPER_HOE));
				MinerData.getOrCreate(g);
				pouch(g);
			}),
			new StartRow("pickaxe, stale worker data, load", Set.of("mine"), g -> {
				give(h, g, new ItemStack(Items.COPPER_PICKAXE));
				WorkerData.getOrCreate(g);
				pouch(g);
			}),
			new StartRow("broken hoe, no load, goes for a new one", Set.of("fetch"), g -> {
				WorkerData.getOrCreate(g);
				loseTool(g, Items.COPPER_HOE);
			}),
			new StartRow("sorter", Set.of("sort"), g -> give(h, g, new ItemStack(Items.BUNDLE))),
			new StartRow("sorter with stale data and a load", Set.of("sort"), g -> {
				give(h, g, new ItemStack(Items.BUNDLE));
				WorkerData.getOrCreate(g);
				pouch(g);
			}),
			new StartRow("untouched, holding a hoe", Set.of(), g -> hold(g, Items.COPPER_HOE)),
			new StartRow("courier carrying a hoe, stale data and a load", Set.of(), g -> {
				give(h, g, new ItemStack(Items.COPPER_HOE));
				MinerData.getOrCreate(g);
				pouch(g);
				Couriers.setRoute(g, route);
				Homes.set(g, h.absolutePos(home));
			})
		);
		for (StartRow row : rows) {
			Set<String> started = new TreeSet<>();
			behaviours.forEach((name, behaviour) -> {
				CopperGolem golem = GameTests.golem(h, new BlockPos(4, 1, 4));
				GameTests.energize(golem);
				Homes.set(golem, h.absolutePos(home));
				row.setup().accept(golem);
				// A new golem starts on a random cooldown, and it would refuse every start.
				golem.getBrain().eraseMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS);
				if (behaviour.get().tryStart(h.getLevel(), golem, 0L)) {
					started.add(name);
				}
				golem.discard();
			});
			h.assertValueEqual(started, new TreeSet<>(row.expected()), row.name());
		}
		h.succeed();
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

	@GameTest(maxTicks = 400)
	public void tool_fetch_without_a_spare_tool_ends_idle(GameTestHelper h) {
		scene(h);
		BlockPos home = new BlockPos(2, 1, 4);
		GameTests.chest(h, home, COPPER_CHEST);
		CopperGolem golem = GameTests.golem(h, new BlockPos(3, 1, 4));
		GameTests.energize(golem);
		WorkerData.getOrCreate(golem);
		loseTool(golem, Items.COPPER_HOE);
		Homes.set(golem, h.absolutePos(home));
		golem.getBrain().eraseMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS);
		ToolFetchBehavior fetch = new ToolFetchBehavior();
		h.assertTrue(fetch.tryStart(h.getLevel(), golem, 0L), "fetch did not start");
		h.assertTrue(golem.getState() == net.minecraft.world.entity.animal.golem.CopperGolemState.GETTING_NO_ITEM, "no head shake");
		fetch.tickOrStop(h.getLevel(), golem, 1L);
		h.assertTrue(golem.getState() == net.minecraft.world.entity.animal.golem.CopperGolemState.GETTING_NO_ITEM, "no head shake after a tick");
		for (long t = 2L; t < 30L; t++) {
			fetch.tickOrStop(h.getLevel(), golem, t);
		}
		h.assertTrue(golem.getState() == net.minecraft.world.entity.animal.golem.CopperGolemState.IDLE, "still shaking its head");
		h.succeed();
	}

	@GameTest(maxTicks = 1200)
	public void stale_data_does_not_stop_a_sorter(GameTestHelper h) {
		scene(h);
		BlockPos home = new BlockPos(2, 1, 4);
		GameTests.chest(h, home, COPPER_CHEST).setItem(0, new ItemStack(Items.COBBLESTONE));
		Container match = GameTests.chest(h, new BlockPos(6, 1, 4), Blocks.CHEST);
		match.setItem(0, new ItemStack(Items.COBBLESTONE));
		CopperGolem golem = GameTests.golem(h, new BlockPos(4, 1, 4));
		GameTests.energize(golem);
		give(h, golem, new ItemStack(Items.BUNDLE));
		WorkerData.getOrCreate(golem);
		MinerData.getOrCreate(golem);
		pouch(golem);
		Homes.set(golem, h.absolutePos(home));
		h.succeedWhen(() -> h.assertTrue(count(match, Items.COBBLESTONE) == 2, "cobblestone not delivered yet"));
	}

	@GameTest(maxTicks = 1200)
	public void stale_data_does_not_stop_a_courier(GameTestHelper h) {
		scene(h);
		BlockPos from = new BlockPos(1, 1, 4);
		BlockPos to = new BlockPos(7, 1, 4);
		GameTests.chest(h, from, COPPER_CHEST).setItem(0, new ItemStack(Items.IRON_INGOT, 8));
		Container target = GameTests.chest(h, to, COPPER_CHEST);
		CopperGolem golem = GameTests.golem(h, new BlockPos(4, 1, 4));
		GameTests.energize(golem);
		WorkerData.getOrCreate(golem);
		MinerData.getOrCreate(golem);
		pouch(golem);
		Couriers.setRoute(golem, new CourierRoute(h.absolutePos(from), h.absolutePos(to)));
		h.succeedWhen(() -> h.assertTrue(count(target, Items.IRON_INGOT) == 8, "ingots not delivered yet"));
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
		h.assertTrue(!Jobs.has(golem), "golem got a job from an iron hoe");
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
	static CopperChestMinecart cart(GameTestHelper h, CopperGolem golem, BlockPos rel, ItemStack... contents) {
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
		CopperChestMinecart cart = cart(h, golem, new BlockPos(5, 1, 4), new ItemStack(Items.OAK_SAPLING, 2));
		// One left in the cart proves it planted from there: a detour home would have emptied the cart first.
		h.succeedWhen(() -> {
			h.assertBlockPresent(Blocks.OAK_SAPLING, stump);
			h.assertValueEqual(count(cart, Items.OAK_SAPLING), 1, "saplings left in the cart");
		});
	}

	@GameTest
	public void golem_death_drops_load_cart_keeps_items(GameTestHelper h) {
		scene(h);
		CopperGolem golem = GameTests.golem(h, new BlockPos(4, 1, 4));
		give(h, golem, new ItemStack(Items.COPPER_HOE));
		Load load = Load.of(golem);
		h.assertTrue(load.hasNothingToBringHome() && !golem.hasAttached(ModAttachments.LOAD), "reading added an attachment");
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

	/** A sorter and a worker share a home chest that holds the worker's stock and some cobblestone; only the cobblestone may leave. */
	private static void stockStays(GameTestHelper h, BlockPos sorterHome, BlockPos workerHome, ItemStack worker, Item stock) {
		Container destination = GameTests.chest(h, new BlockPos(6, 1, 4), Blocks.CHEST);
		CopperGolem sorter = GameTests.golem(h, new BlockPos(4, 1, 4));
		GameTests.energize(sorter);
		give(h, sorter, new ItemStack(Items.BUNDLE));
		Homes.set(sorter, h.absolutePos(sorterHome));
		CopperGolem other = GameTests.golem(h, new BlockPos(3, 1, 3));
		GameTests.energize(other);
		give(h, other, worker);
		Homes.set(other, h.absolutePos(workerHome));
		h.succeedWhen(() -> {
			h.assertTrue(count(destination, Items.COBBLESTONE) == 4, "cobblestone not delivered yet");
			h.assertTrue(count(destination, stock) == 0, "working stock was sorted away");
		});
	}

	@GameTest(maxTicks = 2400)
	public void seeds_protected_across_double_chest(GameTestHelper h) {
		scene(h);
		// The sorter is linked to one half, the farmer to the other.
		h.getLevel().setBlock(h.absolutePos(new BlockPos(1, 1, 1)), COPPER_CHEST.defaultBlockState().setValue(ChestBlock.TYPE, ChestType.LEFT), 3);
		h.getLevel().setBlock(h.absolutePos(new BlockPos(2, 1, 1)), COPPER_CHEST.defaultBlockState().setValue(ChestBlock.TYPE, ChestType.RIGHT), 3);
		Container chest = Homes.containerAt(h.getLevel(), h.absolutePos(new BlockPos(1, 1, 1)));
		chest.setItem(0, new ItemStack(Items.WHEAT_SEEDS, Golems.SEED_RESERVE));
		chest.setItem(1, new ItemStack(Items.COBBLESTONE, 4));
		stockStays(h, new BlockPos(1, 1, 1), new BlockPos(2, 1, 1), new ItemStack(Items.COPPER_HOE), Items.WHEAT_SEEDS);
	}

	@GameTest(maxTicks = 2400)
	public void forester_keeps_saplings_with_sorter(GameTestHelper h) {
		scene(h);
		BlockPos home = new BlockPos(1, 1, 1);
		Container chest = GameTests.chest(h, home, COPPER_CHEST);
		chest.setItem(0, new ItemStack(Items.OAK_SAPLING, ForestBehavior.SAPLING_RESERVE));
		chest.setItem(1, new ItemStack(Items.COBBLESTONE, 4));
		stockStays(h, home, home, new ItemStack(Items.COPPER_AXE), Items.OAK_SAPLING);
	}

	@GameTest(maxTicks = 2400)
	public void spare_tool_stays_with_sorter(GameTestHelper h) {
		scene(h);
		BlockPos home = new BlockPos(1, 1, 1);
		Container chest = GameTests.chest(h, home, COPPER_CHEST);
		chest.setItem(0, new ItemStack(Items.COPPER_HOE));
		chest.setItem(1, new ItemStack(Items.COBBLESTONE, 4));
		stockStays(h, home, home, new ItemStack(Items.COPPER_HOE), Items.COPPER_HOE);
	}

	@GameTest(maxTicks = 2400)
	public void keeper_keeps_food_without_animals(GameTestHelper h) {
		scene(h);
		BlockPos home = new BlockPos(1, 1, 1);
		Container chest = GameTests.chest(h, home, COPPER_CHEST);
		chest.setItem(0, new ItemStack(Items.WHEAT, Golems.SEED_RESERVE));
		chest.setItem(1, new ItemStack(Items.COBBLESTONE, 4));
		stockStays(h, home, home, new ItemStack(Items.BRUSH), Items.WHEAT);
	}

	/** A sorter takes only the seeds above the reserve, from a stack of this size, in hand and cart alike. */
	private static void reserveHolds(GameTestHelper h, int seeds, boolean withCart) {
		scene(h);
		BlockPos home = new BlockPos(1, 1, 1);
		Container chest = GameTests.chest(h, home, COPPER_CHEST);
		chest.setItem(0, new ItemStack(Items.WHEAT_SEEDS, seeds));
		Container destination = GameTests.chest(h, new BlockPos(6, 1, 4), Blocks.CHEST);
		CopperGolem sorter = GameTests.golem(h, new BlockPos(4, 1, 4));
		GameTests.energize(sorter);
		give(h, sorter, new ItemStack(Items.BUNDLE));
		Homes.set(sorter, h.absolutePos(home));
		if (withCart) {
			cart(h, sorter, new BlockPos(5, 1, 3));
		}
		CopperGolem farmer = GameTests.golem(h, new BlockPos(3, 1, 3));
		give(h, farmer, new ItemStack(Items.COPPER_HOE));
		Homes.set(farmer, h.absolutePos(home));
		h.succeedWhen(() -> {
			h.assertTrue(count(destination, Items.WHEAT_SEEDS) == seeds - Golems.SEED_RESERVE, "surplus not delivered yet");
			h.assertTrue(count(chest, Items.WHEAT_SEEDS) == Golems.SEED_RESERVE, "the reserve is not what is left");
		});
	}

	@GameTest(maxTicks = 2400)
	public void sorter_takes_only_seeds_above_reserve(GameTestHelper h) {
		reserveHolds(h, Golems.SEED_RESERVE + 4, false);
	}

	@GameTest(maxTicks = 2400)
	public void sorter_with_cart_takes_only_seeds_above_reserve(GameTestHelper h) {
		reserveHolds(h, 64, true);
	}
}

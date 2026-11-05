package dev.bettercoppers.upkeep;

import dev.bettercoppers.api.BetterCoppersApi;
import dev.bettercoppers.block.ChargingPadBlock;
import dev.bettercoppers.courier.CourierRoute;
import dev.bettercoppers.courier.Couriers;
import dev.bettercoppers.golem.Errands;
import dev.bettercoppers.home.Homes;
import dev.bettercoppers.job.JobHandover;
import dev.bettercoppers.job.Jobs;
import dev.bettercoppers.job.WorkerData;
import dev.bettercoppers.mining.MineLayout;
import dev.bettercoppers.mining.MinePattern;
import dev.bettercoppers.mining.MinePlan;
import dev.bettercoppers.registry.ModBlocks;
import dev.bettercoppers.registry.ModComponents;
import dev.bettercoppers.registry.ModItems;
import dev.bettercoppers.test.GameTests;
import dev.bettercoppers.test.JobGameTests;
import java.util.List;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/**
 * Charging, checked against what the handbook promises. In this package so it can drive the behaviour itself.
 * <p>
 * Golems look for a pad 16 blocks around, so these tests keep that far from each other. Tests that change the time or
 * charge every golem in the level run in test environments of their own, because environments run one after another.
 */
public class ChargeGameTests {
	private static final int PADDING = 48;
	private static final int NIGHT = 18000;
	private static final int DAY = 1000;
	private static final String ALONE_NIGHT = "bettercoppers-gametest:alone_night";
	private static final String ALONE_SLEEP = "bettercoppers-gametest:alone_sleep";

	private static final BlockPos PAD = new BlockPos(2, 0, 2);
	private static final BlockPos ON_PAD = PAD.above();

	/** A golem with a paying job, the way a player makes one, standing at {@code pos}. */
	private static CopperGolem farmer(GameTestHelper h, BlockPos pos, int energy) {
		CopperGolem golem = h.spawn(EntityTypes.COPPER_GOLEM, pos);
		BetterCoppersApi.giveTool(h.makeMockPlayer(GameType.SURVIVAL), golem, new ItemStack(Items.COPPER_HOE));
		WorkerData.getOrCreate(golem);
		Upkeep.get(golem).setEnergy(energy);
		return golem;
	}

	/** Two observers watching each other: a clock, pulsing about every 6 ticks into the block behind {@code at}. */
	private static void clock(GameTestHelper h, BlockPos at, Direction away) {
		h.setBlock(at, Blocks.OBSERVER.defaultBlockState().setValue(ObserverBlock.FACING, away));
		h.setBlock(at.relative(away), Blocks.OBSERVER.defaultBlockState().setValue(ObserverBlock.FACING, away.getOpposite()));
	}

	private static void give(GameTestHelper h, CopperGolem golem, ItemStack stack) {
		Player player = h.makeMockPlayer(GameType.SURVIVAL);
		player.setShiftKeyDown(true);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		UseEntityCallback.EVENT.invoker().interact(player, h.getLevel(), InteractionHand.MAIN_HAND, golem, null);
	}

	/** A miner in front of a stone block with a marked plan, home (1, 1, 7), its first dig out of reach of (4, 1, 7). */
	private static MinePlan miner(GameTestHelper h, CopperGolem golem) {
		for (int x = 2; x <= 6; x++) {
			for (int y = 1; y <= 4; y++) {
				for (int z = 0; z <= 5; z++) {
					h.setBlock(new BlockPos(x, y, z), Blocks.STONE);
				}
			}
		}
		BlockPos home = new BlockPos(1, 1, 7);
		Container supplies = GameTests.chest(h, home, Blocks.COPPER_CHEST.weathering().unaffected());
		supplies.setItem(0, new ItemStack(Items.RAIL, 16));
		supplies.setItem(1, new ItemStack(Items.OAK_PLANKS, 32));
		supplies.setItem(2, new ItemStack(Items.OAK_FENCE, 16));
		supplies.setItem(3, new ItemStack(Items.TORCH, 16));
		MinePlan plan = MinePlan.blank(MinePattern.MINESHAFT, 16).marked(h.absolutePos(new BlockPos(4, 1, 5)), Direction.NORTH);
		ItemStack planStack = new ItemStack(ModItems.MINESHAFT_PLAN);
		planStack.set(ModComponents.MINE_PLAN, plan);
		give(h, golem, new ItemStack(Items.COPPER_PICKAXE));
		give(h, golem, planStack);
		Homes.set(golem, h.absolutePos(home));
		return plan;
	}

	private static void assertDugFirstBlock(GameTestHelper h, MinePlan plan) {
		var first = MineLayout.build(plan, h.getLevel().getMinY()).stream().filter(step -> step.action() == MineLayout.Action.DIG).findFirst().orElseThrow();
		h.assertTrue(h.getLevel().getBlockState(first.pos()).isAir(), "has not dug yet");
	}

	private static void assertAt(GameTestHelper h, CopperGolem golem, BlockPos pos, String message) {
		h.assertTrue(golem.blockPosition().equals(h.absolutePos(pos)), message + ", off by " + golem.blockPosition().subtract(h.absolutePos(pos)));
	}

	private static int energy(CopperGolem golem) {
		return Upkeep.get(golem).energy();
	}

	/** A job ended on the pad: nothing there can charge the golem any more, so the behaviour must let go. */
	@GameTest(maxTicks = 100)
	public void ending_the_job_on_a_pad_frees_the_golem(GameTestHelper h) {
		h.setBlock(new BlockPos(2, 0, 2), ModBlocks.CHARGING_PAD);
		CopperGolem golem = h.spawn(EntityTypes.COPPER_GOLEM, new BlockPos(2, 1, 2));
		Player player = h.makeMockPlayer(GameType.SURVIVAL);
		BetterCoppersApi.giveTool(player, golem, new ItemStack(Items.COPPER_HOE));
		WorkerData.getOrCreate(golem);
		Upkeep.get(golem).setEnergy(0);
		ChargeBehavior charge = new ChargeBehavior();
		long now = h.getLevel().getGameTime();
		h.assertTrue(charge.tryStart(h.getLevel(), golem, now), "did not start charging");
		charge.tickOrStop(h.getLevel(), golem, now);
		h.assertTrue(charge.getStatus() == Behavior.Status.RUNNING, "stopped while working");
		JobHandover.clear(player, golem);
		Upkeep.get(golem).setEnergy(10);
		charge.tickOrStop(h.getLevel(), golem, now + 1);
		h.assertTrue(charge.getStatus() == Behavior.Status.STOPPED, "still held on the pad");
		h.succeed();
	}

	/** Handbook: "A steady signal does nothing", "every pulse gives a little energy". */
	@GameTest(maxTicks = 200, padding = PADDING)
	public void only_the_rising_edge_of_a_signal_charges(GameTestHelper h) {
		JobGameTests.scene(h);
		h.setBlock(PAD, ModBlocks.CHARGING_PAD);
		CopperGolem golem = farmer(h, ON_PAD, 0);
		BlockPos power = PAD.west();
		h.startSequence()
			.thenExecute(() -> h.setBlock(power, Blocks.REDSTONE_BLOCK))
			.thenExecute(() -> h.assertValueEqual(energy(golem), ChargingPadBlock.ENERGY_PER_PULSE, "energy after a pulse"))
			.thenExecuteAfter(40, () -> h.assertValueEqual(energy(golem), ChargingPadBlock.ENERGY_PER_PULSE, "energy after held power"))
			.thenExecute(() -> h.setBlock(power, Blocks.AIR))
			.thenExecuteAfter(10, () -> h.assertValueEqual(energy(golem), ChargingPadBlock.ENERGY_PER_PULSE, "energy after the signal ended"))
			.thenExecute(() -> h.setBlock(power, Blocks.REDSTONE_BLOCK))
			.thenExecute(() -> h.assertValueEqual(energy(golem), 2 * ChargingPadBlock.ENERGY_PER_PULSE, "energy after a second pulse"))
			// Off and on again during the cooldown: only counts once.
			.thenExecute(() -> h.setBlock(power, Blocks.AIR))
			.thenExecute(() -> h.setBlock(power, Blocks.REDSTONE_BLOCK))
			.thenExecute(() -> h.assertValueEqual(energy(golem), 2 * ChargingPadBlock.ENERGY_PER_PULSE, "energy after a pulse in the cooldown"))
			.thenSucceed();
	}

	/** Handbook: "up to full in about 25 seconds", on the fastest clock: one pulse going around two repeaters, one every 4 ticks. */
	@GameTest(maxTicks = 1200, padding = PADDING)
	public void the_fastest_clock_fills_an_empty_golem_in_about_25_seconds(GameTestHelper h) {
		JobGameTests.scene(h);
		BlockPos pad = new BlockPos(2, 1, 2);
		h.setBlock(pad, ModBlocks.CHARGING_PAD);
		CopperGolem golem = farmer(h, pad.above(), 0);
		List<BlockPos> wires = List.of(new BlockPos(2, 1, 3), new BlockPos(4, 1, 3), new BlockPos(4, 1, 2));
		for (BlockPos wire : wires) {
			h.setBlock(wire, Blocks.REDSTONE_WIRE);
		}
		h.setBlock(new BlockPos(3, 1, 3), Blocks.REPEATER.defaultBlockState().setValue(RepeaterBlock.FACING, Direction.WEST));
		h.setBlock(new BlockPos(3, 1, 2), Blocks.REPEATER.defaultBlockState().setValue(RepeaterBlock.FACING, Direction.EAST));
		for (BlockPos wire : wires) {
			h.setBlock(wire, Block.updateFromNeighbourShapes(Blocks.REDSTONE_WIRE.defaultBlockState(), h.getLevel(), h.absolutePos(wire)));
		}
		// The one pulse that goes around.
		h.setBlock(new BlockPos(3, 1, 2), Blocks.REPEATER.defaultBlockState().setValue(RepeaterBlock.FACING, Direction.EAST).setValue(RepeaterBlock.POWERED, true));
		h.onEachTick(() -> {
			if (energy(golem) == GolemUpkeep.MAX_ENERGY) {
				h.assertTrue(h.getTick() >= 450 && h.getTick() <= 560, "full after " + h.getTick() + " ticks");
				h.succeed();
			}
		});
	}

	/** Handbook: "At night, or when it runs empty". By day it goes only when empty, and a full golem has no reason to. */
	@GameTest(maxTicks = 100, padding = PADDING)
	public void by_day_a_golem_goes_charging_only_when_empty(GameTestHelper h) {
		JobGameTests.scene(h);
		h.setBlock(PAD, ModBlocks.CHARGING_PAD);
		CopperGolem golem = farmer(h, new BlockPos(5, 1, 5), GolemUpkeep.MAX_ENERGY);
		long now = h.getLevel().getGameTime();
		h.assertFalse(new ChargeBehavior().tryStart(h.getLevel(), golem, now), "full golem went charging");
		Upkeep.get(golem).setEnergy(1);
		h.assertFalse(new ChargeBehavior().tryStart(h.getLevel(), golem, now), "golem with energy left went charging");
		Upkeep.get(golem).setEnergy(0);
		h.assertTrue(new ChargeBehavior().tryStart(h.getLevel(), golem, now), "empty golem did not go charging");
		h.succeed();
	}

	/**
	 * A farmer runs out mid-job. It walks to the pad, stays until full although there is work and it has a spark of energy
	 * after the first pulse, and then goes back to the crops it left.
	 */
	@GameTest(maxTicks = 2400, padding = PADDING)
	public void a_golem_that_ran_empty_stays_until_full_and_then_resumes(GameTestHelper h) {
		JobGameTests.scene(h);
		BlockPos home = new BlockPos(1, 1, 7);
		Container chest = GameTests.chest(h, home, Blocks.COPPER_CHEST.weathering().unaffected());
		h.setBlock(PAD, ModBlocks.CHARGING_PAD);
		clock(h, PAD.west(), Direction.WEST);
		BlockPos[] crops = {new BlockPos(5, 1, 5), new BlockPos(6, 1, 5), new BlockPos(7, 1, 5)};
		for (BlockPos crop : crops) {
			h.setBlock(crop.below(), Blocks.FARMLAND);
			h.setBlock(crop, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
		}
		CopperGolem golem = farmer(h, new BlockPos(4, 1, 6), 1);
		Homes.set(golem, h.absolutePos(home));
		boolean[] state = new boolean[2];
		h.onEachTick(() -> {
			int energy = energy(golem);
			state[0] |= energy == 0;
			if (energy == GolemUpkeep.MAX_ENERGY && state[0]) {
				state[1] = true;
			}
			if (state[0] && !state[1] && energy > 0) {
				assertAt(h, golem, ON_PAD, "left the pad at energy " + energy);
			}
		});
		h.succeedWhen(() -> {
			h.assertTrue(state[1], "never charged to full");
			int mature = 0;
			for (BlockPos crop : crops) {
				if (h.getBlockState(crop).getValue(CropBlock.AGE) == 7) {
					mature++;
				}
			}
			h.assertValueEqual(mature, 0, "crops left");
			h.assertTrue(chest.countItem(Items.WHEAT) >= 1, "no wheat delivered");
		});
	}

	/** It went to bed with energy left on a pad without a clock. The morning must not hold it there. */
	@GameTest(maxTicks = 1800, padding = PADDING, environment = ALONE_NIGHT)
	public void morning_ends_a_night_on_a_pad_without_a_clock(GameTestHelper h) {
		JobGameTests.scene(h);
		ServerLevel level = h.getLevel();
		var clock = level.dimensionType().defaultClock().orElseThrow();
		level.clockManager().setTotalTicks(clock, NIGHT);
		BlockPos pad = new BlockPos(4, 0, 7);
		h.setBlock(pad, ModBlocks.CHARGING_PAD);
		CopperGolem golem = h.spawn(EntityTypes.COPPER_GOLEM, new BlockPos(2, 1, 6));
		MinePlan plan = miner(h, golem);
		Upkeep.get(golem).setEnergy(900);
		h.startSequence()
			.thenWaitUntil(() -> assertAt(h, golem, pad.above(), "not in bed"))
			.thenExecute(() -> level.clockManager().setTotalTicks(clock, DAY))
			.thenWaitUntil(() -> assertDugFirstBlock(h, plan))
			.thenSucceed();
	}

	/** The clock broke while it was charging. With energy to spare it goes back to work instead of waiting for ever. */
	@GameTest(maxTicks = 1800, padding = PADDING)
	public void a_stopped_clock_sends_a_golem_with_energy_back_to_work(GameTestHelper h) {
		JobGameTests.scene(h);
		BlockPos pad = new BlockPos(4, 0, 7);
		h.setBlock(pad, ModBlocks.CHARGING_PAD);
		CopperGolem golem = h.spawn(EntityTypes.COPPER_GOLEM, pad.above());
		MinePlan plan = miner(h, golem);
		Upkeep.get(golem).setEnergy(0);
		BlockPos power = pad.east();
		h.startSequence()
			.thenExecuteAfter(5, () -> h.setBlock(power, Blocks.REDSTONE_BLOCK))
			.thenExecute(() -> h.assertValueEqual(energy(golem), ChargingPadBlock.ENERGY_PER_PULSE, "energy after a pulse"))
			.thenWaitUntil(() -> assertDugFirstBlock(h, plan))
			.thenSucceed();
	}

	/** Without any energy it has nothing to go back to work with, so a dead pad holds it, and it does nothing else. */
	@GameTest(maxTicks = 400, padding = PADDING)
	public void an_empty_golem_waits_on_a_pad_without_a_clock(GameTestHelper h) {
		JobGameTests.scene(h);
		BlockPos home = new BlockPos(1, 1, 7);
		GameTests.chest(h, home, Blocks.COPPER_CHEST.weathering().unaffected());
		h.setBlock(PAD, ModBlocks.CHARGING_PAD);
		BlockPos crop = new BlockPos(5, 1, 5);
		h.setBlock(crop.below(), Blocks.FARMLAND);
		h.setBlock(crop, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
		CopperGolem golem = farmer(h, ON_PAD, 0);
		Homes.set(golem, h.absolutePos(home));
		h.onEachTick(() -> {
			assertAt(h, golem, ON_PAD, "left the pad");
			h.assertValueEqual(h.getBlockState(crop).getValue(CropBlock.AGE), 7, "crop age");
		});
		h.runAfterDelay(300, h::succeed);
	}

	/** Two golems, one pad on a clock: the second gets its turn once the first is full and back at work. */
	@GameTest(maxTicks = 2400, padding = PADDING)
	public void two_empty_golems_take_turns_on_one_pad(GameTestHelper h) {
		JobGameTests.scene(h);
		BlockPos home = new BlockPos(1, 1, 7);
		GameTests.chest(h, home, Blocks.COPPER_CHEST.weathering().unaffected());
		h.setBlock(PAD, ModBlocks.CHARGING_PAD);
		clock(h, PAD.east(), Direction.EAST);
		CopperGolem one = farmer(h, new BlockPos(0, 1, 2), 0);
		CopperGolem two = farmer(h, new BlockPos(0, 1, 4), 0);
		Homes.set(one, h.absolutePos(home));
		Homes.set(two, h.absolutePos(home));
		h.succeedWhen(() -> {
			h.assertValueEqual(energy(one), GolemUpkeep.MAX_ENERGY, "energy of one");
			h.assertValueEqual(energy(two), GolemUpkeep.MAX_ENERGY, "energy of two");
		});
	}

	/** The pad is broken under an empty golem: it finds the next one. */
	@GameTest(maxTicks = 900, padding = PADDING)
	public void an_empty_golem_moves_on_when_its_pad_is_removed(GameTestHelper h) {
		JobGameTests.scene(h);
		h.setBlock(PAD, ModBlocks.CHARGING_PAD);
		CopperGolem golem = farmer(h, ON_PAD, 0);
		BlockPos other = new BlockPos(6, 0, 6);
		h.startSequence()
			.thenExecuteAfter(20, () -> {
				h.setBlock(PAD, Blocks.STONE);
				h.setBlock(other, ModBlocks.CHARGING_PAD);
			})
			.thenWaitUntil(() -> assertAt(h, golem, other.above(), "not on the other pad"))
			.thenSucceed();
	}

	/** The pad is broken under a golem that has energy again, and there is no other: nothing holds it any more. */
	@GameTest(maxTicks = 100, padding = PADDING)
	public void a_removed_pad_lets_go_of_a_golem_with_energy(GameTestHelper h) {
		JobGameTests.scene(h);
		h.setBlock(PAD, ModBlocks.CHARGING_PAD);
		CopperGolem golem = farmer(h, ON_PAD, 0);
		ChargeBehavior charge = new ChargeBehavior();
		long now = h.getLevel().getGameTime();
		h.assertTrue(charge.tryStart(h.getLevel(), golem, now), "did not start charging");
		charge.tickOrStop(h.getLevel(), golem, now);
		Upkeep.get(golem).setEnergy(ChargingPadBlock.ENERGY_PER_PULSE);
		charge.tickOrStop(h.getLevel(), golem, now + 1);
		h.assertTrue(charge.getStatus() == Behavior.Status.RUNNING, "stopped before it was full");
		h.setBlock(PAD, Blocks.STONE);
		for (int tick = 2; tick < 5; tick++) {
			charge.tickOrStop(h.getLevel(), golem, now + tick);
		}
		h.assertTrue(charge.getStatus() == Behavior.Status.STOPPED, "still held without a pad");
		h.succeed();
	}

	/** A mine is long: a golem that runs empty far from home still gets to the pad near its home. */
	@GameTest(maxTicks = 1200, padding = PADDING)
	public void a_golem_far_from_home_walks_back_to_its_pad(GameTestHelper h) {
		JobGameTests.scene(h);
		// A stone road through the barrier wall of the test area.
		for (int x = 0; x <= 40; x++) {
			for (int z = 1; z <= 3; z++) {
				h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
				h.setBlock(new BlockPos(x, 1, z), Blocks.AIR);
				h.setBlock(new BlockPos(x, 2, z), Blocks.AIR);
			}
			// Only the test area itself is kept ticking.
			ChunkPos chunk = ChunkPos.containing(h.absolutePos(new BlockPos(x, 0, 2)));
			h.getLevel().setChunkForced(chunk.x(), chunk.z(), true);
		}
		BlockPos home = new BlockPos(1, 1, 7);
		GameTests.chest(h, home, Blocks.COPPER_CHEST.weathering().unaffected());
		h.setBlock(PAD, ModBlocks.CHARGING_PAD);
		CopperGolem golem = farmer(h, new BlockPos(38, 1, 2), 0);
		Homes.set(golem, h.absolutePos(home));
		h.succeedWhen(() -> assertAt(h, golem, ON_PAD, "not at the pad yet"));
	}

	/** Sorters, couriers and golems without a job cost nothing, so a pad neither charges nor holds them, nor does a night's sleep. */
	@GameTest(maxTicks = 100, padding = PADDING, environment = ALONE_SLEEP)
	public void golems_without_a_paying_job_are_left_alone(GameTestHelper h) {
		JobGameTests.scene(h);
		h.setBlock(PAD, ModBlocks.CHARGING_PAD);
		BlockPos route = new BlockPos(6, 1, 6);
		CopperGolem jobless = h.spawn(EntityTypes.COPPER_GOLEM, ON_PAD);
		CopperGolem sorter = h.spawn(EntityTypes.COPPER_GOLEM, ON_PAD);
		give(h, sorter, new ItemStack(Items.BUNDLE));
		CopperGolem courier = h.spawn(EntityTypes.COPPER_GOLEM, ON_PAD);
		Couriers.setRoute(courier, new CourierRoute(h.absolutePos(route), h.absolutePos(route.north())));
		h.assertTrue(Jobs.of(jobless) == Jobs.JobKind.NONE, "not jobless");
		h.assertTrue(Jobs.of(sorter) == Jobs.JobKind.SORTER, "not a sorter");
		h.assertTrue(Jobs.of(courier) == Jobs.JobKind.COURIER, "not a courier");
		List<CopperGolem> golems = List.of(jobless, sorter, courier);
		long now = h.getLevel().getGameTime();
		for (CopperGolem golem : golems) {
			Upkeep.get(golem).setEnergy(0);
			h.assertFalse(new ChargeBehavior().tryStart(h.getLevel(), golem, now), Jobs.of(golem) + " went charging");
		}
		h.setBlock(PAD.west(), Blocks.REDSTONE_BLOCK);
		ChargeBehavior.rechargeOvernight(h.getLevel());
		for (CopperGolem golem : golems) {
			h.assertValueEqual(energy(golem), 0, Jobs.of(golem) + " energy");
		}
		h.succeed();
	}

	/** Handbook: "every working golem with a free pad near its home wakes up fully charged, clock or not." */
	@GameTest(maxTicks = 100, padding = PADDING, environment = ALONE_SLEEP)
	public void sleeping_charges_golems_with_a_free_pad_near_home(GameTestHelper h) {
		JobGameTests.scene(h);
		BlockPos home = new BlockPos(1, 1, 1);
		CopperGolem golem = farmer(h, new BlockPos(1, 1, 3), 0);
		Homes.set(golem, h.absolutePos(home));
		record Case(String name, BlockPos pad, boolean charges) {
		}
		List<Case> cases = List.of(
			new Case("16 blocks away", home.offset(16, -1, 0), true),
			new Case("17 blocks away", home.offset(17, -1, 0), false),
			new Case("6 blocks up", home.offset(4, 6, 0), true),
			new Case("7 blocks up", home.offset(4, 7, 0), false)
		);
		ChargeBehavior.rechargeOvernight(h.getLevel());
		h.assertValueEqual(energy(golem), 0, "energy without a pad");
		for (Case test : cases) {
			Upkeep.get(golem).setEnergy(0);
			h.setBlock(test.pad(), ModBlocks.CHARGING_PAD);
			// The test area has a barrier ceiling, which would count as a roof.
			h.setBlock(test.pad().above(), Blocks.AIR);
			h.setBlock(test.pad().above(2), Blocks.AIR);
			ChargeBehavior.rechargeOvernight(h.getLevel());
			h.assertValueEqual(energy(golem), test.charges() ? GolemUpkeep.MAX_ENERGY : 0, test.name());
			h.setBlock(test.pad(), Blocks.AIR);
		}

		// A pad with a roof is no place to stand; a pad somebody stands on is not free; one pad charges one golem.
		BlockPos pad = new BlockPos(4, 0, 1);
		h.setBlock(pad, ModBlocks.CHARGING_PAD);
		Upkeep.get(golem).setEnergy(0);
		h.setBlock(pad.above(2), Blocks.STONE);
		ChargeBehavior.rechargeOvernight(h.getLevel());
		h.assertValueEqual(energy(golem), 0, "energy with a roofed pad");
		h.setBlock(pad.above(2), Blocks.AIR);
		CopperGolem onPad = h.spawn(EntityTypes.COPPER_GOLEM, pad.above());
		ChargeBehavior.rechargeOvernight(h.getLevel());
		h.assertValueEqual(energy(golem), 0, "energy with a taken pad");
		onPad.discard();
		CopperGolem other = farmer(h, new BlockPos(1, 1, 4), 0);
		Homes.set(other, h.absolutePos(home));
		ChargeBehavior.rechargeOvernight(h.getLevel());
		h.assertValueEqual(energy(golem) + energy(other), GolemUpkeep.MAX_ENERGY, "energy of two golems on one pad");
		h.succeed();
	}

	/** Energy, wear and being halfway through a recharge survive saving and loading. */
	@GameTest(maxTicks = 100, padding = PADDING)
	public void energy_survives_a_save(GameTestHelper h) {
		ServerLevel level = h.getLevel();
		CopperGolem golem = farmer(h, ON_PAD, 0);
		Upkeep.get(golem).setEnergy(123);
		Upkeep.get(golem).setWear(45);
		TagValueOutput saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		golem.saveWithoutId(saved);
		CopperGolem loaded = EntityTypes.COPPER_GOLEM.create(level, EntitySpawnReason.LOAD);
		loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved.buildResult()));
		h.assertValueEqual(energy(loaded), 123, "energy");
		h.assertValueEqual(Upkeep.get(loaded).wear(), 45, "wear");
		h.assertTrue(Upkeep.get(loaded).recharging(), "forgot it was recharging");
		h.succeed();
	}

	/** Panic and errands come first, and the golem can start charging again afterwards. */
	@GameTest(maxTicks = 100, padding = PADDING)
	public void panic_and_errands_interrupt_charging(GameTestHelper h) {
		JobGameTests.scene(h);
		h.setBlock(PAD, ModBlocks.CHARGING_PAD);
		CopperGolem golem = farmer(h, ON_PAD, 0);
		ServerLevel level = h.getLevel();
		long now = level.getGameTime();

		ChargeBehavior charge = new ChargeBehavior();
		h.assertTrue(charge.tryStart(level, golem, now), "did not start charging");
		golem.getBrain().setMemory(MemoryModuleType.IS_PANICKING, true);
		charge.tickOrStop(level, golem, now + 1);
		h.assertTrue(charge.getStatus() == Behavior.Status.STOPPED, "charged through a panic");
		h.assertFalse(charge.tryStart(level, golem, now + 2), "started charging in a panic");
		golem.getBrain().eraseMemory(MemoryModuleType.IS_PANICKING);

		h.assertTrue(charge.tryStart(level, golem, now + 3), "did not start charging after the panic");
		Errands.setBusy(golem, true);
		charge.tickOrStop(level, golem, now + 4);
		h.assertTrue(charge.getStatus() == Behavior.Status.STOPPED, "charged through an errand");
		h.assertFalse(charge.tryStart(level, golem, now + 5), "started charging on an errand");
		Errands.setBusy(golem, false);
		h.assertTrue(charge.tryStart(level, golem, now + 6), "did not start charging after the errand");
		h.succeed();
	}
}

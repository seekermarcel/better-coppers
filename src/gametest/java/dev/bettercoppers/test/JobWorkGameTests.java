package dev.bettercoppers.test;

import dev.bettercoppers.api.BetterCoppersApi;
import dev.bettercoppers.api.GolemUpgrade;
import dev.bettercoppers.entity.GolemFloat;
import dev.bettercoppers.home.Homes;
import dev.bettercoppers.job.Jobs;
import dev.bettercoppers.job.Load;
import dev.bettercoppers.job.WorkerData;
import dev.bettercoppers.registry.ModAttachments;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;

/** The real work of the forester, fisher, animal keeper, beekeeper and crafter, from the job item to what lands in the home chest. */
public class JobWorkGameTests {
	private static final Block COPPER_CHEST = Blocks.COPPER_CHEST.weathering().unaffected();
	/** Far enough that no golem of another test reaches this test's chests, trees or animals. */
	private static final int PADDING = 48;
	/** Golems that fish ten times as fast: a bite after 2 to 5 seconds instead of 20 to 50, the rest of the job unchanged. */
	private static final Set<UUID> HASTY = new HashSet<>();

	static {
		BetterCoppersApi.registerUpgrade(new GolemUpgrade() {
			@Override
			public float speed(CopperGolem golem) {
				return HASTY.contains(golem.getUUID()) ? 10.0F : 1.0F;
			}
		});
	}

	/** A player sneak + right-clicks the golem with its job item, then links it to the chest at {@code home}. */
	private static CopperGolem worker(GameTestHelper h, BlockPos rel, Item jobItem, BlockPos home) {
		CopperGolem golem = GameTests.golem(h, rel);
		GameTests.energize(golem);
		Player player = h.makeMockPlayer(GameType.SURVIVAL);
		player.setShiftKeyDown(true);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(jobItem));
		UseEntityCallback.EVENT.invoker().interact(player, h.getLevel(), InteractionHand.MAIN_HAND, golem, null);
		Homes.set(golem, h.absolutePos(home));
		return golem;
	}

	@GameTest(maxTicks = 2400, padding = PADDING)
	public void forester_fells_a_tree_and_replants(GameTestHelper h) {
		JobGameTests.scene(h);
		BlockPos home = new BlockPos(1, 1, 1);
		Container chest = GameTests.chest(h, home, COPPER_CHEST);
		chest.setItem(0, new ItemStack(Items.OAK_SAPLING, 8));
		BlockPos stump = new BlockPos(5, 1, 5);
		h.setBlock(stump.below(), Blocks.DIRT);
		for (int y = 0; y < 4; y++) {
			h.setBlock(stump.above(y), Blocks.OAK_LOG);
		}
		h.setBlock(stump.above(4), Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE, 1));
		// A column a player built: logs on soil, but no leaves. It stands closer to the golem than the tree does.
		BlockPos placed = new BlockPos(1, 1, 5);
		h.setBlock(placed.below(), Blocks.DIRT);
		h.setBlock(placed, Blocks.OAK_LOG);
		h.setBlock(placed.above(), Blocks.OAK_LOG);
		h.spawnItem(Items.APPLE, new BlockPos(3, 1, 2));
		worker(h, new BlockPos(2, 1, 2), Items.COPPER_AXE, home);
		// The sapling goes in only after a trip home, where the logs and apple end up and the saplings come from.
		h.succeedWhen(() -> {
			h.assertBlockPresent(Blocks.OAK_SAPLING, stump);
			h.assertValueEqual(chest.countItem(Items.OAK_LOG), 4, "logs in the chest");
			h.assertTrue(chest.countItem(Items.APPLE) >= 1, "no apple in the chest");
			h.assertBlockPresent(Blocks.OAK_LOG, placed);
			h.assertBlockPresent(Blocks.OAK_LOG, placed.above());
		});
	}

	@GameTest(maxTicks = 2400, padding = PADDING)
	public void forester_replants_all_four_stumps_of_a_2x2_tree(GameTestHelper h) {
		JobGameTests.scene(h);
		BlockPos home = new BlockPos(1, 1, 1);
		Container chest = GameTests.chest(h, home, COPPER_CHEST);
		chest.setItem(0, new ItemStack(Items.DARK_OAK_SAPLING, 8));
		List<BlockPos> stumps = List.of(new BlockPos(5, 1, 5), new BlockPos(6, 1, 5), new BlockPos(5, 1, 6), new BlockPos(6, 1, 6));
		for (BlockPos stump : stumps) {
			h.setBlock(stump.below(), Blocks.DIRT);
			for (int y = 0; y < 3; y++) {
				h.setBlock(stump.above(y), Blocks.DARK_OAK_LOG);
			}
		}
		h.setBlock(new BlockPos(5, 4, 5), Blocks.DARK_OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE, 1));
		worker(h, new BlockPos(2, 1, 2), Items.COPPER_AXE, home);
		h.succeedWhen(() -> stumps.forEach(stump -> h.assertBlockPresent(Blocks.DARK_OAK_SAPLING, stump)));
	}

	@GameTest(maxTicks = 2400, padding = PADDING)
	public void forester_replants_the_species_it_felled(GameTestHelper h) {
		JobGameTests.scene(h);
		BlockPos home = new BlockPos(1, 1, 1);
		GameTests.chest(h, home, COPPER_CHEST);
		BlockPos stump = new BlockPos(5, 1, 5);
		h.setBlock(stump.below(), Blocks.DIRT);
		for (int y = 0; y < 3; y++) {
			h.setBlock(stump.above(y), Blocks.BIRCH_LOG);
		}
		h.setBlock(stump.above(3), Blocks.BIRCH_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE, 1));
		CopperGolem golem = worker(h, new BlockPos(2, 1, 2), Items.COPPER_AXE, home);
		// Oak comes first in its supplies: the old code planted whichever sapling it found first.
		Load load = Load.of(golem);
		load.addSupplies(new ItemStack(Items.OAK_SAPLING, 8));
		load.addSupplies(new ItemStack(Items.BIRCH_SAPLING, 8));
		h.succeedWhen(() -> h.assertBlockPresent(Blocks.BIRCH_SAPLING, stump));
	}

	@GameTest(maxTicks = 2400, padding = PADDING)
	public void forester_fetches_the_felled_species_instead_of_planting_another(GameTestHelper h) {
		JobGameTests.scene(h);
		BlockPos home = new BlockPos(1, 1, 1);
		Container chest = GameTests.chest(h, home, COPPER_CHEST);
		chest.setItem(0, new ItemStack(Items.BIRCH_SAPLING, 8));
		BlockPos stump = new BlockPos(5, 1, 5);
		h.setBlock(stump.below(), Blocks.DIRT);
		for (int y = 0; y < 3; y++) {
			h.setBlock(stump.above(y), Blocks.BIRCH_LOG);
		}
		h.setBlock(stump.above(3), Blocks.BIRCH_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE, 1));
		CopperGolem golem = worker(h, new BlockPos(2, 1, 2), Items.COPPER_AXE, home);
		// It carries only oak, and a birch waits in the chest: an oak on that stump can never turn into a birch.
		Load.of(golem).addSupplies(new ItemStack(Items.OAK_SAPLING, 8));
		h.succeedWhen(() -> h.assertBlockPresent(Blocks.BIRCH_SAPLING, stump));
	}

	@GameTest
	public void forester_remembers_stump_species_across_saves(GameTestHelper h) {
		BlockPos stump = new BlockPos(1, 2, 3);
		WorkerData data = new WorkerData();
		data.marks().add(stump);
		data.stumpSaplings().put(stump, Items.BIRCH_SAPLING);
		WorkerData loaded = WorkerData.CODEC.parse(NbtOps.INSTANCE, WorkerData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow()).getOrThrow();
		h.assertValueEqual(loaded.marks(), List.of(stump), "marks");
		h.assertValueEqual(loaded.stumpSaplings().get(stump), Items.BIRCH_SAPLING, "sapling");
		// Saved before stumps remembered their species: the mark survives and takes any sapling.
		WorkerData old = WorkerData.CODEC.parse(NbtOps.INSTANCE, new CompoundTag()).getOrThrow();
		h.assertTrue(old.stumpSaplings().isEmpty(), "old save has species");
		h.succeed();
	}

	@GameTest(maxTicks = 1200, padding = PADDING)
	public void fisher_casts_and_brings_its_catch_home(GameTestHelper h) {
		JobGameTests.scene(h);
		// A pond in a raised stone platform, so that the water cannot run off.
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				boolean pond = x >= 3 && x <= 6 && z >= 3 && z <= 6;
				h.setBlock(new BlockPos(x, 1, z), pond ? Blocks.WATER : Blocks.STONE);
			}
		}
		BlockPos home = new BlockPos(1, 2, 4);
		Container chest = GameTests.chest(h, home, COPPER_CHEST);
		// Already on the shore, so the speed-up only shortens the wait and not the walk.
		CopperGolem golem = worker(h, new BlockPos(2, 2, 4), Items.FISHING_ROD, home);
		HASTY.add(golem.getUUID());
		// One catch fills the pouch (a fisher goes home at 8), so the catch goes on to the chest.
		Load.of(golem).stash(new ItemStack(Items.COBBLESTONE, 7));
		boolean[] castOnWater = {false};
		boolean[] biting = {false};
		h.onEachTick(() -> {
			for (GolemFloat golemFloat : h.getLevel().getEntitiesOfClass(GolemFloat.class, h.getBounds(), golemFloat -> golemFloat.owner() == golem)) {
				castOnWater[0] |= h.getLevel().getFluidState(golemFloat.blockPosition()).is(FluidTags.WATER);
				biting[0] |= golemFloat.isBiting();
			}
		});
		h.succeedWhen(() -> {
			h.assertValueEqual(chest.countItem(Items.COBBLESTONE), 7, "pouch in the chest");
			int caught = 0;
			for (int slot = 0; slot < chest.getContainerSize(); slot++) {
				ItemStack stack = chest.getItem(slot);
				if (!stack.isEmpty() && !stack.is(Items.COBBLESTONE)) {
					caught += stack.getCount();
					// Only these are treasure alone: fishing rods also come as junk.
					h.assertFalse(List.of(Items.NAME_TAG, Items.SADDLE, Items.BOW, Items.BOOK, Items.NAUTILUS_SHELL).contains(stack.getItem()), "landed treasure");
				}
			}
			h.assertTrue(caught >= 1, "no catch in the chest");
			h.assertTrue(castOnWater[0], "no float on the water");
			h.assertTrue(biting[0], "the float never dipped");
		});
	}

	@GameTest(maxTicks = 2400, padding = PADDING)
	public void keeper_breeds_a_pair_with_food_from_the_chest(GameTestHelper h) {
		JobGameTests.scene(h);
		// A pen, so the animals stay in the test area.
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				if (x == 0 || x == 7 || z == 0 || z == 7) {
					h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
					h.setBlock(new BlockPos(x, 2, z), Blocks.STONE);
				}
			}
		}
		BlockPos home = new BlockPos(1, 1, 1);
		Container chest = GameTests.chest(h, home, COPPER_CHEST);
		chest.setItem(0, new ItemStack(Items.WHEAT, 4));
		h.spawn(EntityTypes.COW, new BlockPos(4, 1, 4));
		h.spawn(EntityTypes.COW, new BlockPos(5, 1, 5));
		CopperGolem golem = worker(h, new BlockPos(2, 1, 2), Items.BRUSH, home);
		// All the wheat left the chest, two went into the cows, and the golem holds on to the rest.
		h.succeedWhen(() -> {
			boolean baby = h.getLevel().getEntitiesOfClass(Animal.class, h.getBounds(), Animal::isBaby).size() == 1;
			h.assertTrue(baby, "no calf yet");
			h.assertValueEqual(chest.countItem(Items.WHEAT), 0, "wheat left in the chest");
			h.assertValueEqual(Load.of(golem).suppliesCount(stack -> stack.is(Items.WHEAT)), 2, "wheat left in the supplies");
		});
	}

	private static void fullHive(GameTestHelper h, BlockPos pos) {
		h.setBlock(pos, Blocks.BEEHIVE.defaultBlockState().setValue(BeehiveBlock.HONEY_LEVEL, BeehiveBlock.MAX_HONEY_LEVELS));
	}

	private static int honey(GameTestHelper h, BlockPos pos) {
		return h.getBlockState(pos).getValue(BeehiveBlock.HONEY_LEVEL);
	}

	@GameTest(maxTicks = 2400, padding = PADDING)
	public void beekeeper_bottles_then_shears_smoked_hives_only(GameTestHelper h) {
		JobGameTests.scene(h);
		BlockPos home = new BlockPos(1, 1, 1);
		Container chest = GameTests.chest(h, home, COPPER_CHEST);
		chest.setItem(0, new ItemStack(Items.GLASS_BOTTLE));
		BlockPos first = new BlockPos(5, 2, 3);
		BlockPos second = new BlockPos(3, 2, 5);
		BlockPos unsmoked = new BlockPos(6, 1, 6);
		h.setBlock(first.below(), Blocks.CAMPFIRE);
		h.setBlock(second.below(), Blocks.CAMPFIRE);
		fullHive(h, first);
		fullHive(h, second);
		fullHive(h, unsmoked);
		worker(h, new BlockPos(2, 1, 2), Items.SHEARS, home);
		// One bottle for one hive, comb from the other, and the hive without smoke stays full.
		h.succeedWhen(() -> {
			h.assertValueEqual(chest.countItem(Items.HONEY_BOTTLE), 1, "honey bottles in the chest");
			h.assertValueEqual(chest.countItem(Items.HONEYCOMB), 3, "honeycomb in the chest");
			h.assertValueEqual(honey(h, first) + honey(h, second), 0, "honey left in the smoked hives");
			h.assertValueEqual(honey(h, unsmoked), BeehiveBlock.MAX_HONEY_LEVELS, "honey in the hive without smoke");
		});
	}

	/** Sneak + shears takes the flower off, then the trim, and only then gives the job. */
	@GameTest
	public void shears_give_the_job_only_after_flower_and_trim(GameTestHelper h) {
		JobGameTests.scene(h);
		CopperGolem golem = GameTests.golem(h, new BlockPos(2, 1, 2));
		golem.setItemSlot(CopperGolem.EQUIPMENT_SLOT_ANTENNA, new ItemStack(Items.POPPY));
		golem.setAttached(ModAttachments.TRIM_TEMPLATE, new ItemStack(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE));
		Player player = h.makeMockPlayer(GameType.SURVIVAL);
		player.setShiftKeyDown(true);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
		Runnable click = () -> UseEntityCallback.EVENT.invoker().interact(player, h.getLevel(), InteractionHand.MAIN_HAND, golem, null);
		click.run();
		h.assertFalse(golem.getMainHandItem().is(Items.SHEARS), "job given past a flower");
		h.assertTrue(golem.getAttached(ModAttachments.TRIM_TEMPLATE) != null, "trim taken off past a flower");
		golem.setItemSlot(CopperGolem.EQUIPMENT_SLOT_ANTENNA, ItemStack.EMPTY); // What vanilla's shearing does after the callback passes.
		click.run();
		h.assertFalse(golem.getMainHandItem().is(Items.SHEARS), "job given past a trim");
		h.assertTrue(golem.getAttached(ModAttachments.TRIM_TEMPLATE) == null, "trim still on");
		click.run();
		h.assertTrue(golem.getMainHandItem().is(Items.SHEARS) && Jobs.of(golem) == Jobs.JobKind.WORKER, "no beekeeper");
		h.succeed();
	}

	@GameTest(maxTicks = 1200, padding = PADDING)
	public void crafter_crafts_from_the_chest_up_to_a_stack(GameTestHelper h) {
		JobGameTests.scene(h);
		BlockPos home = new BlockPos(1, 1, 1);
		Container chest = GameTests.chest(h, home, COPPER_CHEST);
		chest.setItem(0, new ItemStack(Items.STICK, 60));
		chest.setItem(1, new ItemStack(Items.OAK_PLANKS, 5));
		ItemFrame frame = new ItemFrame(h.getLevel(), h.absolutePos(home.west()), Direction.WEST);
		frame.setItem(new ItemStack(Items.STICK));
		h.getLevel().addFreshEntity(frame);
		worker(h, new BlockPos(2, 1, 2), Items.CRAFTING_TABLE, home);
		// One batch fills the stack. Then it stops, though the chest still holds enough planks for another.
		Runnable oneBatch = () -> {
			h.assertValueEqual(chest.countItem(Items.STICK), 64, "sticks in the chest");
			h.assertValueEqual(chest.countItem(Items.OAK_PLANKS), 3, "planks in the chest");
		};
		h.startSequence()
			.thenWaitUntil(oneBatch)
			.thenExecuteAfter(200, oneBatch)
			.thenSucceed();
	}

	@GameTest(maxTicks = 2400, padding = PADDING)
	public void backed_up_crafter_delivers_then_stops_at_a_stack_until_it_is_taken(GameTestHelper h) {
		JobGameTests.scene(h);
		BlockPos home = new BlockPos(1, 1, 1);
		Container chest = GameTests.chest(h, home, COPPER_CHEST);
		for (int slot = 0; slot < chest.getContainerSize(); slot++) {
			chest.setItem(slot, new ItemStack(Items.OAK_LOG, 64));
		}
		ItemFrame frame = new ItemFrame(h.getLevel(), h.absolutePos(home.west()), Direction.WEST);
		frame.setItem(new ItemStack(Items.OAK_PLANKS));
		h.getLevel().addFreshEntity(frame);
		CopperGolem golem = worker(h, new BlockPos(2, 1, 2), Items.CRAFTING_TABLE, home);
		Load.of(golem).stash(new ItemStack(Items.OAK_PLANKS, 64));
		int full = 27 * 64;
		h.startSequence()
			.thenExecuteAfter(400, () -> {
				h.assertValueEqual(Load.of(golem).count(stack -> stack.is(Items.OAK_PLANKS)), 64, "planks while backed up");
				h.assertValueEqual(chest.countItem(Items.OAK_LOG), full, "logs while backed up");
				chest.setItem(0, ItemStack.EMPTY);
			})
			.thenWaitUntil(() -> h.assertValueEqual(chest.countItem(Items.OAK_PLANKS), 64, "planks delivered"))
			// A stack of the result is in the chest: its normal stop, so taking more logs changes nothing.
			.thenExecuteAfter(400, () -> {
				h.assertValueEqual(chest.countItem(Items.OAK_LOG), full - 64, "logs at a full stack of planks");
				chest.setItem(1, ItemStack.EMPTY);
			})
			.thenExecuteAfter(400, () -> {
				h.assertValueEqual(chest.countItem(Items.OAK_LOG), full - 128, "logs after taking more logs");
				chest.setItem(0, ItemStack.EMPTY);
			})
			.thenWaitUntil(() -> h.assertTrue(chest.countItem(Items.OAK_PLANKS) >= 4, "did not craft again"))
			.thenSucceed();
	}
}

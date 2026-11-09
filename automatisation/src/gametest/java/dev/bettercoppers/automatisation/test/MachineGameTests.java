package dev.bettercoppers.automatisation.test;

import dev.bettercoppers.api.BetterCoppersApi;
import dev.bettercoppers.automatisation.arm.GrabberArmBlockEntity;
import dev.bettercoppers.automatisation.assembler.GolemAssemblerBlockEntity;
import dev.bettercoppers.automatisation.belt.BeltDriveBlockEntity;
import dev.bettercoppers.automatisation.belt.ConveyorBeltBlock;
import dev.bettercoppers.automatisation.chute.ChuteBlockEntity;
import dev.bettercoppers.automatisation.funnel.FunnelBlock;
import dev.bettercoppers.automatisation.machine.MachineBlockEntity;
import dev.bettercoppers.automatisation.press.PressBlockEntity;
import dev.bettercoppers.automatisation.registry.AutoAttachments;
import dev.bettercoppers.automatisation.registry.AutoBlocks;
import dev.bettercoppers.automatisation.registry.AutoItems;
import dev.bettercoppers.automatisation.saw.SawBlockEntity;
import dev.bettercoppers.automatisation.upgrade.GolemUpgrades;
import dev.bettercoppers.automatisation.upgrade.Upgrades;
import java.util.List;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** What the machines make. Most tests turn the crank themselves; two have an operator do it. */
public class MachineGameTests {
	private static final BlockPos DRIVE = new BlockPos(2, 1, 5);
	private static final BlockPos PRESS = new BlockPos(3, 2, 4);
	private static final BlockPos TARGET = new BlockPos(7, 1, 4);

	private static void floor(GameTestHelper h) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
	}

	/** Chest, funnel, belts under a press, funnel, chest, running east. The chest at the start holds {@code load}. */
	private static void line(GameTestHelper h, ItemStack load) {
		floor(h);
		h.setBlock(new BlockPos(0, 1, 4), Blocks.CHEST);
		h.setBlock(new BlockPos(1, 1, 4), AutoBlocks.FUNNEL.defaultBlockState().setValue(FunnelBlock.FACING, Direction.EAST).setValue(FunnelBlock.PUSHING, true));
		for (int x = 2; x <= 5; x++) {
			h.setBlock(new BlockPos(x, 1, 4), AutoBlocks.CONVEYOR_BELT.defaultBlockState().setValue(ConveyorBeltBlock.FACING, Direction.EAST));
		}
		h.setBlock(DRIVE, AutoBlocks.BELT_DRIVE);
		h.setBlock(PRESS, AutoBlocks.PRESS);
		h.setBlock(new BlockPos(6, 1, 4), AutoBlocks.FUNNEL.defaultBlockState().setValue(FunnelBlock.FACING, Direction.WEST));
		h.setBlock(TARGET, Blocks.CHEST);
		container(h, new BlockPos(0, 1, 4)).setItem(0, load);
	}

	private static Container container(GameTestHelper h, BlockPos rel) {
		return (Container) h.getBlockEntity(rel, BlockEntity.class);
	}

	private static void crank(GameTestHelper h, BlockPos rel) {
		if (h.getLevel().getBlockEntity(h.absolutePos(rel)) instanceof MachineBlockEntity machine) {
			machine.operate(h.getLevel(), false);
		}
	}

	/** Cranks and runs the machine this many times inside one game tick, so the count is exact. */
	private static void runTicks(GameTestHelper h, BlockPos rel, MachineBlockEntity machine, boolean geared, int ticks) {
		BlockPos pos = h.absolutePos(rel);
		for (int i = 0; i < ticks; i++) {
			machine.operate(h.getLevel(), geared);
			MachineBlockEntity.serverTick(h.getLevel(), pos, h.getLevel().getBlockState(pos), machine);
		}
	}

	private static GolemAssemblerBlockEntity assemblerWithParts(GameTestHelper h, BlockPos rel) {
		h.setBlock(rel, AutoBlocks.GOLEM_ASSEMBLER);
		GolemAssemblerBlockEntity machine = h.getBlockEntity(rel, GolemAssemblerBlockEntity.class);
		int slot = 0;
		for (var part : GolemAssemblerBlockEntity.RECIPE.entrySet()) {
			machine.setItem(slot++, new ItemStack(part.getKey(), part.getValue()));
		}
		return machine;
	}
	private static void drop(GameTestHelper h, BlockPos rel, ItemStack stack) {
		Vec3 at = h.absoluteVec(Vec3.atCenterOf(rel));
		ItemEntity item = new ItemEntity(h.getLevel(), at.x, at.y, at.z, stack);
		item.setDeltaMovement(Vec3.ZERO);
		h.getLevel().addFreshEntity(item);
	}

	private static int lyingAt(GameTestHelper h, BlockPos rel, Item item) {
		List<ItemEntity> items = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(rel)), found -> found.getItem().is(item));
		return items.stream().mapToInt(found -> found.getItem().getCount()).sum();
	}

	/** An operator with a wrench, linked to the machine, in broad daylight. */
	private static CopperGolem operator(GameTestHelper h, BlockPos machine) {
		h.getLevel().clockManager().setTotalTicks(h.getLevel().dimensionType().defaultClock().orElseThrow(), 1000L);
		Player player = h.makeMockPlayer(GameType.SURVIVAL);
		CopperGolem golem = h.spawn(EntityTypes.COPPER_GOLEM, new BlockPos(6, 1, 6));
		player.setShiftKeyDown(true);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AutoItems.COPPER_WRENCH));
		UseEntityCallback.EVENT.invoker().interact(player, h.getLevel(), InteractionHand.MAIN_HAND, golem, null);
		golem.setAttached(AutoAttachments.MACHINE, h.absolutePos(machine));
		h.assertTrue(BetterCoppersApi.jobOf(golem) != null, "the wrench gave no job");
		return golem;
	}

	@GameTest
	public void geared_crank_does_a_third_more_work(GameTestHelper h) {
		floor(h);
		BlockPos rel = new BlockPos(3, 1, 3);
		GolemAssemblerBlockEntity machine = assemblerWithParts(h, rel);
		runTicks(h, rel, machine, false, 100);
		h.assertValueEqual(machine.progress, 100, "plain crank");
		runTicks(h, rel, machine, true, 100);
		h.assertTrue(machine.progress >= 229 && machine.progress <= 231, "geared crank reached " + machine.progress);
		h.succeed();
	}

	/** The client counts along for the animation, so it has to be told. */
	@GameTest
	public void client_learns_the_machine_is_geared(GameTestHelper h) {
		floor(h);
		BlockPos rel = new BlockPos(3, 1, 3);
		GolemAssemblerBlockEntity machine = assemblerWithParts(h, rel);
		runTicks(h, rel, machine, true, 1);
		h.assertTrue(machine.getUpdateTag(h.getLevel().registryAccess()).getBooleanOr("geared", false), "geared not sent to the client");
		h.succeed();
	}

	/** Belts run at one speed, so gearwork must not wear the drive out faster for nothing. */
	@GameTest
	public void gearwork_does_not_wear_a_belt_drive_faster(GameTestHelper h) {
		floor(h);
		h.setBlock(DRIVE, AutoBlocks.BELT_DRIVE);
		BeltDriveBlockEntity machine = h.getBlockEntity(DRIVE, BeltDriveBlockEntity.class);
		h.onEachTick(() -> {
			// It wears on every twentieth game tick, and all of these runs happen inside one.
			if (h.getLevel().getGameTime() % 20 == 0) {
				runTicks(h, DRIVE, machine, true, 20);
				h.assertValueEqual(machine.wearPercent(), 1, "wear after 20 runs");
				h.succeed();
			}
		});
	}

	@GameTest(maxTicks = 400)
	public void operator_with_gearwork_runs_its_machine_faster(GameTestHelper h) {
		floor(h);
		BlockPos rel = new BlockPos(3, 1, 3);
		GolemAssemblerBlockEntity machine = assemblerWithParts(h, rel);
		CopperGolem golem = operator(h, rel);
		Upgrades.set(golem, GolemUpgrades.NONE.withGearwork(Upgrades.GEARWORK));
		int[] start = new int[1];
		h.startSequence()
			.thenWaitUntil(() -> h.assertTrue(machine.isRunning(), "not cranked yet"))
			.thenExecute(() -> start[0] = machine.progress)
			.thenExecuteAfter(100, () -> h.assertTrue(machine.progress - start[0] >= 115, "got " + (machine.progress - start[0]) + " ticks of work in 100"))
			.thenSucceed();
	}

	/** The whole point of the add-on: a golem cranks, and sheets come out. */
	@GameTest(maxTicks = 600)
	public void operator_presses_ingots_into_sheets(GameTestHelper h) {
		floor(h);
		BlockPos press = new BlockPos(3, 2, 3);
		h.setBlock(press, AutoBlocks.PRESS);
		drop(h, press.below(), new ItemStack(Items.IRON_INGOT, 8));
		operator(h, press);
		h.succeedWhen(() -> {
			h.assertValueEqual(lyingAt(h, press.below(), AutoItems.IRON_SHEET), 8, "sheets under the press");
			h.assertTrue(h.getBlockEntity(press, PressBlockEntity.class).wearPercent() > 0, "the press did not wear");
		});
	}

	/** The handbook's promise: a jammed machine stands still until its operator fetches a part from a chest nearby. */
	@GameTest(maxTicks = 800)
	public void operator_repairs_a_jammed_machine_with_a_part_from_a_chest(GameTestHelper h) {
		floor(h);
		BlockPos press = new BlockPos(3, 2, 3);
		h.setBlock(press, AutoBlocks.PRESS);
		PressBlockEntity machine = h.getBlockEntity(press, PressBlockEntity.class);
		CompoundTag worn = new CompoundTag();
		worn.putInt("wear", 64);
		machine.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), worn));
		h.assertTrue(machine.isBroken(), "the press is not jammed");
		BlockPos supply = new BlockPos(1, 1, 1);
		h.setBlock(supply, Blocks.CHEST);
		container(h, supply).setItem(0, new ItemStack(AutoItems.IRON_SHEET));
		operator(h, press);
		h.succeedWhen(() -> {
			h.assertFalse(machine.isBroken(), "still jammed");
			h.assertTrue(container(h, supply).isEmpty(), "the part stayed in the chest");
		});
	}

	@GameTest(maxTicks = 400)
	public void factory_line_presses_a_chest_of_ingots_into_a_chest_of_sheets(GameTestHelper h) {
		line(h, new ItemStack(Items.IRON_INGOT, 8));
		h.onEachTick(() -> {
			crank(h, DRIVE);
			crank(h, PRESS);
		});
		h.succeedWhen(() -> {
			h.assertValueEqual(container(h, TARGET).countItem(AutoItems.IRON_SHEET), 8, "sheets in the chest");
			h.assertValueEqual(container(h, TARGET).countItem(Items.IRON_INGOT), 0, "ingots that skipped the press");
		});
	}

	/** A press holds what passes under it. Once the press is gone, the belt takes the item on. */
	@GameTest(maxTicks = 400)
	public void belt_takes_an_item_on_after_its_press_is_broken(GameTestHelper h) {
		line(h, new ItemStack(Items.IRON_INGOT, 8));
		h.onEachTick(() -> crank(h, DRIVE));
		h.startSequence()
			.thenWaitUntil(() -> h.assertValueEqual(lyingAt(h, PRESS.below(), Items.IRON_INGOT), 8, "ingots under the press"))
			.thenExecuteAfter(20, () -> {
				h.assertValueEqual(lyingAt(h, PRESS.below(), Items.IRON_INGOT), 8, "the press let the ingots go");
				h.setBlock(PRESS, Blocks.AIR);
			})
			.thenWaitUntil(() -> h.assertValueEqual(container(h, TARGET).countItem(Items.IRON_INGOT), 8, "ingots in the chest"))
			.thenSucceed();
	}

	/** The README promises half again as many as crafting: 6 per log. */
	@GameTest(maxTicks = 200)
	public void saw_cuts_logs_into_planks(GameTestHelper h) {
		floor(h);
		BlockPos saw = new BlockPos(3, 2, 3);
		h.setBlock(saw, AutoBlocks.SAW);
		h.assertTrue(h.getBlockEntity(saw, SawBlockEntity.class).filter().isEmpty(), "a new saw has a filter");
		drop(h, saw.below(), new ItemStack(Items.OAK_LOG, 2));
		h.onEachTick(() -> crank(h, saw));
		h.succeedWhen(() -> h.assertValueEqual(lyingAt(h, saw.below(), Items.OAK_PLANKS), 12, "planks under the saw"));
	}

	@GameTest(maxTicks = 200)
	public void chute_passes_items_down_into_a_chest(GameTestHelper h) {
		floor(h);
		BlockPos chest = new BlockPos(3, 1, 3);
		h.setBlock(chest, Blocks.CHEST);
		h.setBlock(chest.above(), AutoBlocks.CHUTE);
		h.setBlock(chest.above(2), AutoBlocks.CHUTE);
		h.getBlockEntity(chest.above(2), ChuteBlockEntity.class).setItem(0, new ItemStack(Items.COBBLESTONE, 5));
		h.succeedWhen(() -> h.assertValueEqual(container(h, chest).countItem(Items.COBBLESTONE), 5, "cobblestone in the chest"));
	}

	@GameTest(maxTicks = 300)
	public void arm_swings_items_from_its_input_to_its_output(GameTestHelper h) {
		floor(h);
		BlockPos arm = new BlockPos(3, 1, 3);
		BlockPos input = new BlockPos(1, 1, 3);
		BlockPos output = new BlockPos(5, 1, 3);
		h.setBlock(arm, AutoBlocks.GRABBER_ARM);
		h.setBlock(input, Blocks.CHEST);
		h.setBlock(output, Blocks.CHEST);
		container(h, input).setItem(0, new ItemStack(Items.COBBLESTONE, 20));
		GrabberArmBlockEntity machine = h.getBlockEntity(arm, GrabberArmBlockEntity.class);
		machine.toggle(h.absolutePos(input));
		machine.toggle(h.absolutePos(output));
		machine.toggle(h.absolutePos(output));
		h.onEachTick(() -> crank(h, arm));
		h.succeedWhen(() -> h.assertValueEqual(container(h, output).countItem(Items.COBBLESTONE), 20, "cobblestone in the output"));
	}

	/** A golem from the assembler is an ordinary jobless golem. */
	@GameTest(maxTicks = 500)
	public void assembler_uses_up_its_parts_and_builds_a_jobless_golem(GameTestHelper h) {
		floor(h);
		BlockPos assembler = new BlockPos(3, 1, 3);
		h.setBlock(assembler, AutoBlocks.GOLEM_ASSEMBLER);
		GolemAssemblerBlockEntity machine = h.getBlockEntity(assembler, GolemAssemblerBlockEntity.class);
		int slot = 0;
		for (var part : GolemAssemblerBlockEntity.RECIPE.entrySet()) {
			machine.setItem(slot++, new ItemStack(part.getKey(), part.getValue()));
		}
		h.onEachTick(() -> crank(h, assembler));
		h.succeedWhen(() -> {
			List<CopperGolem> golems = h.getLevel().getEntitiesOfClass(CopperGolem.class, new AABB(h.absolutePos(assembler)).inflate(8.0));
			h.assertValueEqual(golems.size(), 1, "golems built");
			h.assertTrue(BetterCoppersApi.jobOf(golems.getFirst()) == null, "the new golem has a job");
			h.assertTrue(machine.isEmpty(), "parts left in the assembler");
		});
	}

	@GameTest
	public void belt_drive_finds_a_whole_line_from_any_belt(GameTestHelper h) {
		line(h, ItemStack.EMPTY);
		h.assertValueEqual(BeltDriveBlockEntity.connectedBelts(h.getLevel(), h.absolutePos(DRIVE)).size(), 4, "belts in the line");
		h.succeed();
	}
}

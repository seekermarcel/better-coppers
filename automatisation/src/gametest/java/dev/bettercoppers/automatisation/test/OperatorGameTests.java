package dev.bettercoppers.automatisation.test;

import dev.bettercoppers.api.BetterCoppersApi;
import dev.bettercoppers.automatisation.golem.Operators;
import dev.bettercoppers.automatisation.registry.AutoAttachments;
import dev.bettercoppers.automatisation.registry.AutoItems;
import dev.bettercoppers.automatisation.upgrade.Upgrades;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Only a player can make a golem an operator, and ending the job leaves nothing behind. */
public class OperatorGameTests {
	private static Container chest(GameTestHelper h, BlockPos rel, net.minecraft.world.level.block.Block block) {
		h.setBlock(rel, block);
		return (Container) h.getBlockEntity(rel, BlockEntity.class);
	}

	private static void floor(GameTestHelper h) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
	}

	private static void click(GameTestHelper h, Player player, CopperGolem golem, boolean sneak, ItemStack held) {
		player.setShiftKeyDown(sneak);
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		UseEntityCallback.EVENT.invoker().interact(player, h.getLevel(), InteractionHand.MAIN_HAND, golem, null);
	}

	private static void give(GameTestHelper h, Player player, CopperGolem golem, Item item) {
		click(h, player, golem, true, new ItemStack(item));
	}

	@GameTest(maxTicks = 1200)
	public void untouched_golem_with_wrench_stays_jobless(GameTestHelper h) {
		floor(h);
		chest(h, new BlockPos(2, 1, 4), Blocks.COPPER_CHEST.weathering().unaffected()).setItem(0, new ItemStack(AutoItems.COPPER_WRENCH));
		Container target = chest(h, new BlockPos(6, 1, 4), Blocks.CHEST);
		target.setItem(0, new ItemStack(AutoItems.COPPER_WRENCH));
		CopperGolem golem = h.spawn(EntityTypes.COPPER_GOLEM, new BlockPos(4, 1, 4));
		h.onEachTick(() -> h.assertTrue(BetterCoppersApi.jobOf(golem) == null, "untouched golem became an operator"));
		h.succeedWhen(() -> h.assertTrue(target.countItem(AutoItems.COPPER_WRENCH) == 2, "wrench not sorted yet"));
	}

	@GameTest(maxTicks = 1200)
	public void ex_operator_stays_jobless_after_picking_up_a_wrench(GameTestHelper h) {
		floor(h);
		chest(h, new BlockPos(2, 1, 4), Blocks.COPPER_CHEST.weathering().unaffected()).setItem(0, new ItemStack(AutoItems.COPPER_WRENCH));
		Container target = chest(h, new BlockPos(6, 1, 4), Blocks.CHEST);
		target.setItem(0, new ItemStack(AutoItems.COPPER_WRENCH));
		Player player = h.makeMockPlayer(GameType.SURVIVAL);
		CopperGolem golem = h.spawn(EntityTypes.COPPER_GOLEM, new BlockPos(4, 1, 4));
		give(h, player, golem, AutoItems.COPPER_WRENCH);
		h.assertTrue(Operators.isOperator(golem), "the wrench gave no job");
		golem.setAttached(AutoAttachments.MACHINE, h.absolutePos(new BlockPos(1, 1, 1)));
		click(h, player, golem, true, ItemStack.EMPTY);
		click(h, player, golem, true, ItemStack.EMPTY);
		h.assertTrue(player.getInventory().countItem(AutoItems.COPPER_WRENCH) == 1, "wrench not back");
		h.assertFalse(golem.hasAttached(AutoAttachments.OPERATOR), "operator marker left");
		h.assertFalse(golem.hasAttached(AutoAttachments.MACHINE), "machine link left");
		h.onEachTick(() -> h.assertTrue(BetterCoppersApi.jobOf(golem) == null, "ex-operator got the job back"));
		h.succeedWhen(() -> h.assertTrue(target.countItem(AutoItems.COPPER_WRENCH) == 2, "wrench not sorted yet"));
	}

	@GameTest
	public void worker_handed_a_wrench_returns_its_load(GameTestHelper h) {
		floor(h);
		Player player = h.makeMockPlayer(GameType.SURVIVAL);
		CopperGolem golem = h.spawn(EntityTypes.COPPER_GOLEM, new BlockPos(4, 1, 4));
		give(h, player, golem, Items.COPPER_HOE);
		click(h, player, golem, true, new ItemStack(Items.WHEAT_SEEDS, 4));
		give(h, player, golem, AutoItems.COPPER_WRENCH);
		h.assertTrue(Operators.isOperator(golem), "no operator");
		h.assertTrue(player.getInventory().countItem(Items.COPPER_HOE) == 1, "hoe not back");
		h.assertTrue(player.getInventory().countItem(Items.WHEAT_SEEDS) == 4, "supplies not back");
		// And the other way: the farmer's hoe ends the operator's job.
		give(h, player, golem, Items.COPPER_HOE);
		h.assertTrue(BetterCoppersApi.jobOf(golem) == null, "still an operator");
		h.assertFalse(golem.hasAttached(AutoAttachments.OPERATOR), "operator marker left");
		h.assertTrue(player.getInventory().countItem(AutoItems.COPPER_WRENCH) == 1, "wrench not back");
		h.succeed();
	}

	@GameTest
	public void wrench_and_upgrades_need_a_sneak(GameTestHelper h) {
		floor(h);
		Player player = h.makeMockPlayer(GameType.SURVIVAL);
		CopperGolem golem = h.spawn(EntityTypes.COPPER_GOLEM, new BlockPos(4, 1, 4));
		click(h, player, golem, false, new ItemStack(AutoItems.COPPER_WRENCH));
		h.assertFalse(Operators.isOperator(golem), "plain click gave a wrench");
		click(h, player, golem, false, new ItemStack(AutoItems.IRON_PLATING));
		h.assertTrue(Upgrades.get(golem).plating() == 0, "plain click fitted plating");
		give(h, player, golem, AutoItems.IRON_PLATING);
		h.assertTrue(Upgrades.get(golem).plating() > 0, "sneak click fitted no plating");
		h.succeed();
	}
}

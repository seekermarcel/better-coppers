package dev.bettercoppers.job;

import dev.bettercoppers.api.BetterCoppersApi;
import dev.bettercoppers.api.GolemJob;
import dev.bettercoppers.courier.Couriers;
import dev.bettercoppers.entity.CopperChestMinecart;
import dev.bettercoppers.golem.CartChaining;
import dev.bettercoppers.golem.Inventories;
import dev.bettercoppers.home.HomeLinking;
import dev.bettercoppers.mining.MinerData;
import dev.bettercoppers.mining.MinerInteractions;
import dev.bettercoppers.registry.ModAttachments;
import dev.bettercoppers.repair.RepairBehavior;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Starting and ending jobs. The player gets back everything the golem carries for its job, or at their feet what does not
 * fit, and the job's data goes, so nothing can bring an old job back. Home, eyes, trim, energy and rust stay. A broken
 * tool is not a job end: {@link Tools}.
 */
public final class JobHandover {
	private JobHandover() {
	}

	/** Puts one of {@code held} in the golem's hand, ending any other job. The same job with a new tool only swaps tools. The cart stays chained. */
	public static void giveTool(Player player, CopperGolem golem, ItemStack held) {
		ItemStack tool = held.copyWithCount(1);
		HomeLinking.markHandover(player, golem);
		// First, so what comes back can use the slot.
		held.consume(1, player);
		if (isSameJob(golem, tool)) {
			giveBack(player, golem, EquipmentSlot.MAINHAND);
			HomeLinking.closeEndJobWindow(golem);
		} else {
			clear(player, golem);
		}
		golem.setItemSlot(EquipmentSlot.MAINHAND, tool);
		golem.setGuaranteedDrop(EquipmentSlot.MAINHAND);
		Tools.clearLost(golem);
		golem.playSound(SoundEvents.COPPER_GOLEM_ITEM_GET);
	}

	/** Ends the job and leaves the golem with empty hands. The cart stays chained; a sorter's new bundle goes in after this. */
	public static void clear(Player player, CopperGolem golem) {
		HomeLinking.markHandover(player, golem);
		giveBack(player, golem, EquipmentSlot.MAINHAND);
		giveBack(player, golem, EquipmentSlot.OFFHAND);
		Load.of(golem).handOver(stack -> Inventories.giveOrDrop(player, stack));
		RepairBehavior.giveCarried(player, golem);
		MinerData miner = golem.getAttached(ModAttachments.MINER);
		if (miner != null && miner.plan() != null) {
			MinerInteractions.returnPlan(player, golem, miner);
		}
		golem.removeAttached(ModAttachments.MINER);
		golem.removeAttached(ModAttachments.WORKER);
		golem.removeAttached(ModAttachments.LOST_TOOL);
		golem.removeAttached(ModAttachments.EXCITED);
		Couriers.setRoute(golem, null);
		HomeLinking.closeEndJobWindow(golem);
		for (GolemJob job : BetterCoppersApi.jobs()) {
			job.onEnded(golem);
		}
	}

	/** Ends the job and unchains the cart, which stays where it is and keeps its items. */
	public static void end(Player player, CopperGolem golem) {
		clear(player, golem);
		CopperChestMinecart cart = CartChaining.findCart(golem);
		if (cart != null) {
			cart.unchain(true);
		}
	}

	private static boolean isSameJob(CopperGolem golem, ItemStack tool) {
		return switch (Jobs.of(golem)) {
			case WORKER -> WorkerData.job(golem) == Job.forTool(tool);
			case MINER -> tool.is(Items.COPPER_PICKAXE);
			default -> false;
		};
	}

	private static void giveBack(Player player, CopperGolem golem, EquipmentSlot slot) {
		ItemStack stack = golem.getItemBySlot(slot);
		if (!stack.isEmpty()) {
			golem.setItemSlot(slot, ItemStack.EMPTY);
			Inventories.giveOrDrop(player, stack);
		}
	}
}

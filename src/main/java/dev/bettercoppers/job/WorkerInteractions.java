package dev.bettercoppers.job;

import dev.bettercoppers.guide.Guide;
import dev.bettercoppers.home.Homes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jspecify.annotations.Nullable;

/**
 * Sneak + right-click hands a copper golem its job tool: a hoe (farmer), an axe (forester), a fishing rod (fisher), a
 * brush (animal keeper) or a crafting table (crafter). An axe only becomes a tool for a clean golem; on a rusty or waxed
 * one it scrapes, as in vanilla. Seeds handed to a farmer go into its supplies.
 */
public final class WorkerInteractions {
	private WorkerInteractions() {
	}

	public static InteractionResult onUseEntity(Player player, Level level, InteractionHand hand, Entity entity, @Nullable EntityHitResult hitResult) {
		if (!(entity instanceof CopperGolem golem) || player.isSpectator() || hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) {
			return InteractionResult.PASS;
		}
		ItemStack held = player.getItemInHand(hand);
		if (FarmBehavior.isSeed(held) && WorkerData.job(golem) == Job.FARMER) {
			if (!level.isClientSide()) {
				Load.of(golem).addSupplies(held);
				held.consume(held.getCount(), player);
				golem.playSound(SoundEvents.COPPER_GOLEM_ITEM_GET);
			}
			return InteractionResult.SUCCESS;
		}
		Job job = Job.forTool(held);
		if (job == null || Tools.scrapes(golem, held)) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			JobHandover.giveTool(player, golem, held);
			WorkerData.getOrCreate(golem);
			Component jobName = Component.translatable("job.bettercoppers." + job.id());
			Guide.job(player, job.id());
			player.sendOverlayMessage(
				Homes.get(golem) == null
					? Component.translatable("message.bettercoppers.worker_no_chest", jobName)
					: Component.translatable("message.bettercoppers.worker_ready", jobName)
			);
		}
		return InteractionResult.SUCCESS;
	}
}

package dev.bettercoppers.mining;

import dev.bettercoppers.guide.Guide;
import dev.bettercoppers.home.Homes;
import dev.bettercoppers.job.JobHandover;
import dev.bettercoppers.registry.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jspecify.annotations.Nullable;

/**
 * Turning a copper golem into a miner: sneak + right-click it with a copper pickaxe. With a home it then looks for the
 * nearest open Mine Entrance; without one, or without an entrance, the player is told what is missing.
 */
public final class MinerInteractions {
	private MinerInteractions() {
	}

	public static InteractionResult onUseEntity(Player player, Level level, InteractionHand hand, Entity entity, @Nullable EntityHitResult hitResult) {
		if (!(entity instanceof CopperGolem golem) || player.isSpectator() || hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) {
			return InteractionResult.PASS;
		}
		ItemStack held = player.getItemInHand(hand);
		if (!held.is(Items.COPPER_PICKAXE)) {
			return InteractionResult.PASS;
		}
		if (level instanceof ServerLevel serverLevel) {
			JobHandover.giveTool(player, golem, held);
			MinerData.getOrCreate(golem);
			Guide.job(player, "miner");
			if (Homes.get(golem) == null) {
				player.sendOverlayMessage(Component.translatable("message.bettercoppers.miner_needs_home", MineLimits.of(serverLevel).entranceSearchRadius()));
			} else if (Mines.of(serverLevel, golem, true) == null) {
				player.sendOverlayMessage(noEntrance(serverLevel));
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** One message for the hand-over hint and the status line. */
	public static Component noEntrance(ServerLevel level) {
		return Component.translatable("status.bettercoppers.no_mine_entrance", MineLimits.of(level).entranceSearchRadius());
	}

	/** Forgets the mine with the golem, and frees its claim. */
	public static void dropAll(CopperGolem golem) {
		Mines.release(golem);
		golem.removeAttached(ModAttachments.MINER);
	}
}

package dev.bettercoppers.golem;

import dev.bettercoppers.guide.Guide;
import dev.bettercoppers.home.Homes;
import dev.bettercoppers.job.JobHandover;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jspecify.annotations.Nullable;

/**
 * A golem only sorts once it is given a bundle with sneak + right-click (kept in its off hand, since the main hand carries
 * the items). Without one it sorts like vanilla.
 */
public final class Sorter {
	private Sorter() {
	}

	public static boolean isSorter(CopperGolem golem) {
		return golem.getOffhandItem().is(ItemTags.BUNDLES);
	}

	public static InteractionResult onUseEntity(Player player, Level level, InteractionHand hand, Entity entity, @Nullable EntityHitResult hitResult) {
		if (!(entity instanceof CopperGolem golem) || player.isSpectator() || hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) {
			return InteractionResult.PASS;
		}
		ItemStack held = player.getItemInHand(hand);
		if (held.is(ItemTags.BUNDLES) && !isSorter(golem)) {
			if (!level.isClientSide()) {
				ItemStack bundle = held.copyWithCount(1);
				// First, so what comes back can use the slot.
				held.consume(1, player);
				JobHandover.clear(player, golem);
				golem.setItemSlot(EquipmentSlot.OFFHAND, bundle);
				golem.setGuaranteedDrop(EquipmentSlot.OFFHAND);
				golem.playSound(SoundEvents.BUNDLE_INSERT);
				Guide.job(player, "sorter");
				player.sendOverlayMessage(
					Component.translatable(Homes.get(golem) == null ? "message.bettercoppers.sorter_needs_home" : "message.bettercoppers.sorter_ready")
				);
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}
}

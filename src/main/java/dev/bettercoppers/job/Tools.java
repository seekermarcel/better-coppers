package dev.bettercoppers.job;

import dev.bettercoppers.registry.ModAttachments;
import dev.bettercoppers.runningcosts.RunningCosts;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.phys.EntityHitResult;
import org.jspecify.annotations.Nullable;

/**
 * Job tools. Golems only work with copper tools (and the fishing rod, shears, brush and crafting table, which have no copper
 * version). When a tool breaks, the golem remembers which one it was: it keeps its job and fetches a new one from its
 * home chest or a container nearby ({@link ToolFetchBehavior}).
 */
public final class Tools {
	private Tools() {
	}

	/** A pickaxe, axe or hoe that is not made of copper: golems refuse those. */
	public static boolean isNonCopperTool(ItemStack stack) {
		return (stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.AXES) || stack.is(ItemTags.HOES))
			&& !stack.is(Items.COPPER_PICKAXE) && !stack.is(Items.COPPER_AXE) && !stack.is(Items.COPPER_HOE);
	}

	/** Uses the tool in the golem's main hand once. If it breaks, the golem remembers it for {@link ToolFetchBehavior}. */
	public static void wear(CopperGolem golem, ItemStack tool) {
		Item item = tool.getItem();
		tool.hurtAndBreak(1, golem, EquipmentSlot.MAINHAND);
		if (golem.getMainHandItem().isEmpty()) {
			golem.setAttached(ModAttachments.LOST_TOOL, item);
		}
	}

	/** The tool that broke, while the golem has not found a new one. */
	public static @Nullable Item lostTool(CopperGolem golem) {
		return golem.getMainHandItem().isEmpty() ? golem.getAttached(ModAttachments.LOST_TOOL) : null;
	}

	public static boolean hasLost(CopperGolem golem, Item item) {
		return lostTool(golem) == item;
	}

	/** A new tool is in the golem's hand. */
	public static void clearLost(CopperGolem golem) {
		golem.removeAttached(ModAttachments.LOST_TOOL);
	}

	/** Any axe scrapes a rusty or waxed golem, as in vanilla, so it cannot be handed over. */
	public static boolean scrapes(CopperGolem golem, ItemStack stack) {
		return stack.is(ItemTags.AXES) && (golem.getWeatherState() != WeatheringCopper.WeatherState.UNAFFECTED || RunningCosts.isWaxed(golem));
	}

	/** Refuses to take non-copper tools, with a message. */
	public static InteractionResult onUseEntity(Player player, Level level, InteractionHand hand, Entity entity, @Nullable EntityHitResult hitResult) {
		if (!(entity instanceof CopperGolem golem) || player.isSpectator() || hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) {
			return InteractionResult.PASS;
		}
		ItemStack held = player.getItemInHand(hand);
		if (!isNonCopperTool(held) || scrapes(golem, held)) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			player.sendOverlayMessage(Component.translatable("message.bettercoppers.copper_tools_only"));
			golem.playSound(SoundEvents.COPPER_GOLEM_ITEM_NO_GET);
		}
		return InteractionResult.SUCCESS;
	}
}

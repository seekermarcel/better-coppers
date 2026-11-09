package dev.bettercoppers.automatisation.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** Gold Gearwork, which says who it is for. Fitting it is in {@code Upgrades}. */
public class GoldGearworkItem extends Item {
	public GoldGearworkItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		builder.accept(Component.translatable("tooltip.bettercoppers_automatisation.gold_gearwork").withStyle(ChatFormatting.GRAY));
	}
}

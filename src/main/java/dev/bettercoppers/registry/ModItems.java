package dev.bettercoppers.registry;

import dev.bettercoppers.BetterCoppers;
import dev.bettercoppers.api.BetterCoppersApi;
import dev.bettercoppers.guide.GolemHandbookItem;
import dev.bettercoppers.item.CopperChestMinecartItem;
import dev.bettercoppers.item.MineshaftPlanItem;
import dev.bettercoppers.mining.MinePattern;
import dev.bettercoppers.mining.MinePlan;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;

public final class ModItems {
	public static final Item COPPER_CHEST_MINECART = register(
		"copper_chest_minecart", p -> new CopperChestMinecartItem(ModEntities.COPPER_CHEST_MINECART, p), new Item.Properties().stacksTo(1)
	);
	public static final Item MINESHAFT_PLAN = register("mineshaft_plan", MineshaftPlanItem::new, new Item.Properties().stacksTo(1));
	public static final Item GOLEM_HANDBOOK = register("golem_handbook", GolemHandbookItem::new, new Item.Properties().stacksTo(1));

	private ModItems() {
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BetterCoppers.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	public static void init() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, BetterCoppersApi.CREATIVE_TAB, FabricCreativeModeTab.builder()
			.title(Component.translatable("itemGroup.bettercoppers.main"))
			.icon(() -> new ItemStack(GOLEM_HANDBOOK))
			.displayItems((parameters, output) -> {
				output.accept(GOLEM_HANDBOOK);
				output.accept(ModBlocks.SURVEY_TABLE);
				output.accept(blankPlan());
				output.accept(ModBlocks.CHARGING_PAD);
				output.accept(COPPER_CHEST_MINECART);
			})
			.build());
	}

	/** A plan as the Survey Table makes it at its default settings, so it works straight from the tab. */
	private static ItemStack blankPlan() {
		ItemStack plan = new ItemStack(MINESHAFT_PLAN);
		plan.set(ModComponents.MINE_PLAN, MinePlan.blank(MinePattern.MINESHAFT, MinePlan.DEFAULT_LENGTH));
		return plan;
	}
}

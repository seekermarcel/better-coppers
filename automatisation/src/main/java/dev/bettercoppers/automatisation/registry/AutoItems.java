package dev.bettercoppers.automatisation.registry;

import dev.bettercoppers.api.BetterCoppersApi;
import dev.bettercoppers.automatisation.Automatisation;
import dev.bettercoppers.automatisation.item.WrenchItem;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public final class AutoItems {
	public static final Item COPPER_WRENCH = register("copper_wrench", WrenchItem::new, new Item.Properties().stacksTo(1));
	public static final Item COPPER_SHEET = register("copper_sheet", Item::new, new Item.Properties());
	public static final Item IRON_SHEET = register("iron_sheet", Item::new, new Item.Properties());
	public static final Item GOLD_SHEET = register("gold_sheet", Item::new, new Item.Properties());
	public static final Item COPPER_GEAR = register("copper_gear", Item::new, new Item.Properties());
	public static final Item GOLD_GEAR = register("gold_gear", Item::new, new Item.Properties());
	public static final Item IRON_PLATING = register("iron_plating", Item::new, new Item.Properties().stacksTo(16));
	public static final Item COPPER_COATING = register("copper_coating", Item::new, new Item.Properties().stacksTo(16));
	public static final Item GOLD_GEARWORK = register("gold_gearwork", Item::new, new Item.Properties().stacksTo(16));
	public static final Item COPPER_SATCHEL = register("copper_satchel", Item::new, new Item.Properties().stacksTo(1));
	public static final Item CART_EXTENSION = register("cart_extension", Item::new, new Item.Properties().stacksTo(16));

	private AutoItems() {
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Automatisation.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	/** Everything goes into the Better Coppers tab; the key works whichever mod initialises first. */
	public static void init() {
		CreativeModeTabEvents.modifyOutputEvent(BetterCoppersApi.CREATIVE_TAB).register(output -> {
			output.accept(COPPER_WRENCH);
			output.accept(AutoBlocks.CONVEYOR_BELT);
			output.accept(AutoBlocks.BELT_DRIVE);
			output.accept(AutoBlocks.FUNNEL);
			output.accept(AutoBlocks.CHUTE);
			output.accept(AutoBlocks.PRESS);
			output.accept(AutoBlocks.SAW);
			output.accept(AutoBlocks.GRABBER_ARM);
			output.accept(AutoBlocks.GOLEM_ASSEMBLER);
			output.accept(AutoBlocks.CHARGING_STATION);
			output.accept(COPPER_SHEET);
			output.accept(IRON_SHEET);
			output.accept(GOLD_SHEET);
			output.accept(COPPER_GEAR);
			output.accept(GOLD_GEAR);
			output.accept(IRON_PLATING);
			output.accept(COPPER_COATING);
			output.accept(GOLD_GEARWORK);
			output.accept(COPPER_SATCHEL);
			output.accept(CART_EXTENSION);
		});
	}
}

package dev.bettercoppers.test;

import dev.bettercoppers.BetterCoppers;
import dev.bettercoppers.api.BetterCoppersApi;
import dev.bettercoppers.mining.MinePlan;
import dev.bettercoppers.registry.ModComponents;
import dev.bettercoppers.registry.ModItems;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** The Better Coppers creative tab. */
public class CreativeTabGameTests {
	@GameTest
	public void tab_holds_every_item_of_the_mod(GameTestHelper h) {
		CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(BetterCoppersApi.CREATIVE_TAB);
		CreativeModeTabs.tryRebuildTabContents(h.getLevel().enabledFeatures(), true, h.getLevel().registryAccess());
		Set<Item> inTab = new HashSet<>();
		for (ItemStack stack : tab.getDisplayItems()) {
			inTab.add(stack.getItem());
			if (stack.is(ModItems.MINESHAFT_PLAN)) {
				MinePlan plan = stack.get(ModComponents.MINE_PLAN);
				h.assertTrue(plan != null && !plan.isMarked(), "plan in the tab is not a blank plan");
			}
		}
		for (Item item : BuiltInRegistries.ITEM) {
			if (BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(BetterCoppers.MOD_ID)) {
				h.assertTrue(inTab.contains(item), "missing from the tab: " + BuiltInRegistries.ITEM.getKey(item));
			}
		}
		h.succeed();
	}
}

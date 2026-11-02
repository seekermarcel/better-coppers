package dev.bettercoppers.automatisation.test;

import dev.bettercoppers.api.BetterCoppersApi;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** The add-on's items join the base mod's tab, whichever mod initialised first. */
public class CreativeTabGameTests {
	@GameTest
	public void tab_holds_every_item_of_both_mods(GameTestHelper h) {
		CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(BetterCoppersApi.CREATIVE_TAB);
		CreativeModeTabs.tryRebuildTabContents(h.getLevel().enabledFeatures(), true, h.getLevel().registryAccess());
		Set<Item> inTab = new HashSet<>();
		for (ItemStack stack : tab.getDisplayItems()) {
			inTab.add(stack.getItem());
		}
		int found = 0;
		for (Item item : BuiltInRegistries.ITEM) {
			String namespace = BuiltInRegistries.ITEM.getKey(item).getNamespace();
			if (namespace.equals("bettercoppers") || namespace.equals("bettercoppers_automatisation")) {
				found++;
				h.assertTrue(inTab.contains(item), "missing from the tab: " + BuiltInRegistries.ITEM.getKey(item));
			}
		}
		h.assertTrue(found > 20, "too few items found: " + found);
		h.succeed();
	}
}

package dev.bettercoppers.golem;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ItemFamiliesTest {
	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		// Item stacks need their default components, which vanilla binds only when a world loads.
		BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createWorldLookup()).forEach(DataComponentInitializers.PendingComponents::apply);
	}

	@Test
	void related() {
		assertTrue(ItemFamilies.related(new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_BLOCK)));
		assertFalse(ItemFamilies.related(new ItemStack(Items.IRON_INGOT), new ItemStack(Items.OAK_LOG)));
	}

	@Test
	void unstackablesOfOneFrameGroupAreRelated() {
		assertTrue(ItemFamilies.related(new ItemStack(Items.IRON_HELMET), new ItemStack(Items.DIAMOND_BOOTS)));
		assertTrue(ItemFamilies.related(new ItemStack(Items.POTION), new ItemStack(Items.LINGERING_POTION)));
		assertFalse(ItemFamilies.related(new ItemStack(Items.IRON_HELMET), new ItemStack(Items.ENCHANTED_BOOK)));
	}
}

package dev.bettercoppers.golem;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.crafting.RecipeType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FrameFilterTest {
	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createWorldLookup()).forEach(DataComponentInitializers.PendingComponents::apply);
	}

	/** Only a blast furnace cooks anything here, and only raw iron. */
	private static FrameFilter filter(ItemStack... shown) {
		return new FrameFilter(List.of(shown), (type, item) -> type == RecipeType.BLASTING && item.is(Items.RAW_IRON));
	}

	private static ItemStack bundleOf(Item... items) {
		ItemStack bundle = new ItemStack(Items.BUNDLE);
		bundle.set(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY.copyWithContents(Stream.of(items).map(ItemStack::new)));
		return bundle;
	}

	private static boolean allows(FrameFilter filter, Item item) {
		return filter.allows(new ItemStack(item));
	}

	@Test
	void plainFrameMatchesTheExactItem() {
		FrameFilter coal = filter(new ItemStack(Items.COAL));
		assertTrue(allows(coal, Items.COAL));
		assertFalse(allows(coal, Items.CHARCOAL));
	}

	@Test
	void bundleStandsForWhatIsInIt() {
		FrameFilter filter = filter(bundleOf(Items.COAL, Items.CHARCOAL));
		assertTrue(allows(filter, Items.COAL));
		assertTrue(allows(filter, Items.CHARCOAL));
		assertFalse(allows(filter, Items.DIRT));
		assertFalse(allows(filter, Items.BUNDLE));
	}

	@Test
	void emptyBundleReservesButLetsNothingIn() {
		FrameFilter filter = filter(bundleOf());
		assertFalse(filter.isEmpty());
		assertFalse(allows(filter, Items.COAL));
	}

	@Test
	void workBlockStandsForItsOwnRecipes() {
		FrameFilter blast = filter(new ItemStack(Items.BLAST_FURNACE));
		assertTrue(allows(blast, Items.RAW_IRON));
		assertFalse(allows(blast, Items.BLAST_FURNACE));
		assertFalse(allows(filter(new ItemStack(Items.FURNACE)), Items.RAW_IRON));
		assertFalse(allows(filter(new ItemStack(Items.SMOKER)), Items.RAW_IRON));
	}

	@Test
	void workBlockInABundleIsJustThatItem() {
		FrameFilter filter = filter(bundleOf(Items.FURNACE));
		assertTrue(allows(filter, Items.FURNACE));
		assertFalse(allows(filter, Items.RAW_IRON));
	}

	@Test
	void anyFrameCanLetAnItemIn() {
		FrameFilter filter = filter(new ItemStack(Items.COAL), new ItemStack(Items.BLAST_FURNACE));
		assertTrue(allows(filter, Items.COAL));
		assertTrue(allows(filter, Items.RAW_IRON));
	}

	@Test
	void groupFramesStandForUnstackables() {
		assertTrue(allows(filter(new ItemStack(Items.ENCHANTING_TABLE)), Items.ENCHANTED_BOOK));
		assertFalse(allows(filter(new ItemStack(Items.ENCHANTING_TABLE)), Items.BOOK));
		FrameFilter armour = filter(new ItemStack(Items.ARMOR_STAND));
		assertTrue(allows(armour, Items.IRON_HELMET));
		assertTrue(allows(armour, Items.ELYTRA));
		assertTrue(allows(armour, Items.DIAMOND_HORSE_ARMOR));
		assertFalse(allows(armour, Items.CARVED_PUMPKIN));
		assertFalse(allows(armour, Items.SADDLE));
		assertTrue(allows(filter(new ItemStack(Items.BREWING_STAND)), Items.SPLASH_POTION));
		assertFalse(allows(filter(new ItemStack(Items.BREWING_STAND)), Items.TIPPED_ARROW));
	}

	@Test
	void enderChestTakesUnstackablesAfterOtherFrames() {
		FrameFilter any = filter(new ItemStack(Items.ENDER_CHEST));
		assertTrue(allows(any, Items.MUSIC_DISC_CAT));
		assertFalse(allows(any, Items.ENDER_PEARL));
		assertFalse(any.allowsBeforeCatchAll(new ItemStack(Items.MUSIC_DISC_CAT)));
		assertTrue(filter(bundleOf(Items.ENDER_CHEST)).allowsBeforeCatchAll(new ItemStack(Items.ENDER_CHEST)));
	}
}

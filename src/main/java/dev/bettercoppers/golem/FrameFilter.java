package dev.bettercoppers.golem;

import dev.bettercoppers.home.Homes;
import dev.bettercoppers.item.QuotaSlipItem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * What the item frames on a container let through. A frame shows one item, or stands for many: a bundle for what is in
 * it, a furnace, smoker or blast furnace for what that block can cook, a grindstone, smithing table, enchanting table,
 * armor stand or brewing stand for a group of unstackables (which do not fit many to a bundle), an ender chest for any
 * unstackable no other frame takes.
 */
public final class FrameFilter {
	/** The blocks whose frames stand for a group of unstackables. The groups without tags come first, so unit tests reach them without bound tags. */
	private static final List<Item> GROUP_BLOCKS = List.of(Items.ENCHANTING_TABLE, Items.ARMOR_STAND, Items.BREWING_STAND, Items.GRINDSTONE, Items.SMITHING_TABLE);

	private final List<ItemStack> shown;
	private final Quota quota;
	private final BiPredicate<RecipeType<? extends AbstractCookingRecipe>, ItemStack> cooks;

	FrameFilter(List<ItemStack> shown, BiPredicate<RecipeType<? extends AbstractCookingRecipe>, ItemStack> cooks) {
		// A slip counts through its quota, so a blank one reserves nothing, like an empty frame.
		this.shown = shown.stream().filter(frame -> !(frame.getItem() instanceof QuotaSlipItem)).toList();
		this.quota = Quota.of(shown);
		this.cooks = cooks;
	}

	static FrameFilter of(ServerLevel level, List<ItemStack> shown) {
		return new FrameFilter(shown, (type, item) -> cooks(level, type, item));
	}

	/** The frames on the container at {@code pos}, on either half of a double chest. */
	public static FrameFilter at(ServerLevel level, BlockPos pos) {
		List<ItemStack> shown = new ArrayList<>();
		for (BlockPos part : Homes.sameContainer(level, pos)) {
			for (ItemFrame frame : level.getEntitiesOfClass(ItemFrame.class, new AABB(part).inflate(1.0), frame -> !frame.getItem().isEmpty() && hangsOn(frame).equals(part))) {
				shown.add(frame.getItem());
			}
		}
		return of(level, shown);
	}

	/** A frame hangs on the block behind it. */
	static BlockPos hangsOn(ItemFrame frame) {
		return frame.getPos().relative(frame.getDirection().getOpposite());
	}

	public boolean isEmpty() {
		return this.shown.isEmpty() && this.quota.isEmpty();
	}

	public boolean allows(ItemStack item) {
		return this.allows(item, true);
	}

	/** Like {@link #allows}, but ignoring ender chest frames, so that every other reservation comes first. */
	boolean allowsBeforeCatchAll(ItemStack item) {
		return this.allows(item, false);
	}

	private boolean allows(ItemStack item, boolean catchAll) {
		return this.quota.of(item.getItem()) > 0
			|| this.shown.stream().filter(frame -> catchAll || !frame.is(Items.ENDER_CHEST)).anyMatch(frame -> this.matches(frame, item));
	}

	/** The amounts set by any Quota Slips among the frames. */
	public Quota quota() {
		return this.quota;
	}

	private boolean matches(ItemStack frame, ItemStack item) {
		BundleContents bundle = frame.get(DataComponents.BUNDLE_CONTENTS);
		if (bundle != null) {
			return bundle.itemCopies().anyMatch(inside -> ItemStack.isSameItem(inside, item));
		}
		RecipeType<? extends AbstractCookingRecipe> cooking = cookingOf(frame);
		if (cooking != null) {
			return this.cooks.test(cooking, item);
		}
		if (GROUP_BLOCKS.contains(frame.getItem())) {
			return inGroup(frame.getItem(), item);
		}
		if (frame.is(Items.ENDER_CHEST)) {
			return item.getMaxStackSize() == 1;
		}
		return ItemStack.isSameItem(frame, item);
	}

	/** The block whose frame stands for this item's group of unstackables, or null if it is in none. */
	static @Nullable Item group(ItemStack item) {
		return GROUP_BLOCKS.stream().filter(block -> inGroup(block, item)).findFirst().orElse(null);
	}

	private static boolean inGroup(Item block, ItemStack item) {
		if (block == Items.ENCHANTING_TABLE) {
			return item.is(Items.ENCHANTED_BOOK);
		}
		if (block == Items.ARMOR_STAND) {
			Equippable equippable = item.get(DataComponents.EQUIPPABLE);
			return equippable != null && equippable.slot().isArmor() && item.getMaxStackSize() == 1;
		}
		if (block == Items.BREWING_STAND) {
			return item.is(Items.POTION) || item.is(Items.SPLASH_POTION) || item.is(Items.LINGERING_POTION);
		}
		if (block == Items.GRINDSTONE) {
			return item.is(ItemTags.SWORDS) || item.is(ItemTags.SPEARS) || item.is(Items.MACE) || item.is(Items.TRIDENT) || item.is(Items.BOW) || item.is(Items.CROSSBOW);
		}
		return item.is(ItemTags.PICKAXES) || item.is(ItemTags.AXES) || item.is(ItemTags.SHOVELS) || item.is(ItemTags.HOES)
			|| item.is(Items.SHEARS) || item.is(Items.BRUSH) || item.is(Items.FISHING_ROD) || item.is(Items.FLINT_AND_STEEL);
	}

	private static @Nullable RecipeType<? extends AbstractCookingRecipe> cookingOf(ItemStack frame) {
		if (frame.is(Items.FURNACE)) {
			return RecipeType.SMELTING;
		}
		if (frame.is(Items.BLAST_FURNACE)) {
			return RecipeType.BLASTING;
		}
		return frame.is(Items.SMOKER) ? RecipeType.SMOKING : null;
	}

	private static <T extends AbstractCookingRecipe> boolean cooks(ServerLevel level, RecipeType<T> type, ItemStack item) {
		return level.recipeAccess().getRecipeFor(type, new SingleRecipeInput(item.copyWithCount(1)), level).isPresent();
	}
}

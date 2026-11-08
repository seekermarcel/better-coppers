package dev.bettercoppers.golem;

import dev.bettercoppers.home.Homes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * What the item frames on a container let through. A frame shows one item, or stands for many: a bundle for what is in
 * it, a furnace, smoker or blast furnace for what that block can cook.
 */
public final class FrameFilter {
	private final List<ItemStack> shown;
	private final BiPredicate<RecipeType<? extends AbstractCookingRecipe>, ItemStack> cooks;

	FrameFilter(List<ItemStack> shown, BiPredicate<RecipeType<? extends AbstractCookingRecipe>, ItemStack> cooks) {
		this.shown = shown;
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
		return this.shown.isEmpty();
	}

	public boolean allows(ItemStack item) {
		return this.shown.stream().anyMatch(frame -> this.matches(frame, item));
	}

	private boolean matches(ItemStack frame, ItemStack item) {
		BundleContents bundle = frame.get(DataComponents.BUNDLE_CONTENTS);
		if (bundle != null) {
			return bundle.itemCopies().anyMatch(inside -> ItemStack.isSameItem(inside, item));
		}
		RecipeType<? extends AbstractCookingRecipe> cooking = cookingOf(frame);
		return cooking != null ? this.cooks.test(cooking, item) : ItemStack.isSameItem(frame, item);
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

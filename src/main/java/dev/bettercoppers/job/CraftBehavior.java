package dev.bettercoppers.job;

import dev.bettercoppers.golem.FrameFilter;
import dev.bettercoppers.golem.Inventories;
import dev.bettercoppers.golem.Quota;
import dev.bettercoppers.home.Homes;
import dev.bettercoppers.item.QuotaSlipItem;
import dev.bettercoppers.runningcosts.RunningCosts;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.CopperGolemState;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Crafts whatever is shown in item frames on or right next to its home chest, using only what is in that chest, one
 * item at a time and slowly. It stops once a stack of the result is in the chest, or the amount a Quota Slip on the
 * chest lists for it.
 */
public class CraftBehavior extends JobBehavior<CraftBehavior.Order> {
	private static final int FRAME_RANGE = 2;
	private static final int STOCK_LIMIT = 64;
	private static final int CRAFT_TICKS = 80;
	private static final int ENERGY_PER_CRAFT = 15;

	/** A recipe and the chest slot used for each of its ingredients. */
	record Order(CraftingRecipe recipe, int width, int height, List<ItemStack> grid, List<Integer> sourceSlots) {
	}

	private static @Nullable RecipeManager cachedFor;
	private static final Map<Item, List<CraftingRecipe>> RECIPES_BY_RESULT = new HashMap<>();

	public CraftBehavior() {
		super(Job.CRAFTER);
	}

	@Override
	protected @Nullable Order chooseTask(ServerLevel level, CopperGolem golem, BlockPos home) {
		Container chest = Homes.container(level, home);
		if (chest == null) {
			return null;
		}
		Quota quota = FrameFilter.at(level, home).quota();
		for (ItemFrame frame : level.getEntitiesOfClass(ItemFrame.class, new AABB(home).inflate(FRAME_RANGE))) {
			ItemStack wanted = frame.getItem();
			// A slip only sets amounts, it is never an order.
			if (wanted.isEmpty() || wanted.getItem() instanceof QuotaSlipItem) {
				continue;
			}
			int limit = quota.of(wanted.getItem()) > 0 ? quota.of(wanted.getItem()) : STOCK_LIMIT;
			if (Inventories.count(chest, stack -> stack.is(wanted.getItem())) >= limit) {
				continue;
			}
			for (CraftingRecipe recipe : recipesFor(level, wanted.getItem())) {
				Order order = plan(level, chest, recipe, wanted.getItem());
				if (order != null) {
					return order;
				}
			}
		}
		return null;
	}

	@Override
	protected boolean tickTask(ServerLevel level, CopperGolem golem, BlockPos home, Order order) {
		Container chest = Homes.container(level, home);
		if (chest == null) {
			return true;
		}
		if (!this.reach(level, golem, home)) {
			return false;
		}
		if (this.workTicks == 0) {
			if (this.hesitate(golem)) {
				return false;
			}
			chest.startOpen(golem);
			golem.setOpenedChestPos(home);
		}
		this.workTicks++;
		if (this.workTicks % 20 == 1) {
			// Rummaging in the chest and fiddling with the parts.
			golem.setState(this.workTicks / 20 % 2 == 0 ? CopperGolemState.GETTING_ITEM : CopperGolemState.DROPPING_ITEM);
			golem.playSound(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 0.3F, 1.2F);
		}
		if (this.workTicks < slowed(golem, CRAFT_TICKS)) {
			return false;
		}
		this.craft(level, golem, chest, order);
		if (chest.getEntitiesWithContainerOpen().contains(golem)) {
			chest.stopOpen(golem);
		}
		golem.clearOpenedChestPos();
		return true;
	}

	private void craft(ServerLevel level, CopperGolem golem, Container chest, Order order) {
		// The chest may have changed while the golem was busy: check the ingredients again.
		for (int i = 0; i < order.grid().size(); i++) {
			ItemStack needed = order.grid().get(i);
			if (!needed.isEmpty() && !ItemStack.isSameItemSameComponents(chest.getItem(order.sourceSlots().get(i)), needed)) {
				return;
			}
		}
		CraftingInput input = CraftingInput.of(order.width(), order.height(), order.grid());
		if (!order.recipe().matches(input, level)) {
			return;
		}
		ItemStack result = order.recipe().assemble(input);
		List<ItemStack> remainders = order.recipe().getRemainingItems(input);
		for (int i = 0; i < order.grid().size(); i++) {
			if (!order.grid().get(i).isEmpty()) {
				chest.removeItem(order.sourceSlots().get(i), 1);
			}
		}
		// What does not fit stays in the load and goes back when the chest has room.
		Load load = Load.of(golem);
		for (ItemStack stack : remainders) {
			load.stash(Inventories.insert(chest, stack));
		}
		load.stash(Inventories.insert(chest, result));
		golem.playSound(SoundEvents.COPPER_GOLEM_ITEM_DROP);
		RunningCosts.spend(golem, ENERGY_PER_CRAFT);
	}

	/** Picks a chest stack for every ingredient, or null if the chest lacks something. */
	private static @Nullable Order plan(ServerLevel level, Container chest, CraftingRecipe recipe, Item wanted) {
		PlacementInfo placement = recipe.placementInfo();
		if (placement.isImpossibleToPlace()) {
			return null;
		}
		List<Ingredient> ingredients = placement.ingredients();
		int[] used = new int[chest.getContainerSize()];
		List<Integer> chosenSlots = new ArrayList<>();
		for (Ingredient ingredient : ingredients) {
			int slot = findSlot(chest, ingredient, used);
			if (slot < 0) {
				return null;
			}
			used[slot]++;
			chosenSlots.add(slot);
		}

		int width;
		int height;
		List<ItemStack> grid = new ArrayList<>();
		List<Integer> sourceSlots = new ArrayList<>();
		if (recipe instanceof ShapedRecipe shaped) {
			width = shaped.getWidth();
			height = shaped.getHeight();
			for (int cell = 0; cell < width * height; cell++) {
				int index = placement.slotsToIngredientIndex().getInt(cell);
				if (index < 0) {
					grid.add(ItemStack.EMPTY);
					sourceSlots.add(-1);
				} else {
					int slot = chosenSlots.get(index);
					grid.add(chest.getItem(slot).copyWithCount(1));
					sourceSlots.add(slot);
				}
			}
		} else {
			width = Math.min(3, ingredients.size());
			height = (ingredients.size() + 2) / 3;
			for (int slot : chosenSlots) {
				grid.add(chest.getItem(slot).copyWithCount(1));
				sourceSlots.add(slot);
			}
			while (grid.size() < width * height) {
				grid.add(ItemStack.EMPTY);
				sourceSlots.add(-1);
			}
		}
		CraftingInput input = CraftingInput.of(width, height, grid);
		if (!recipe.matches(input, level) || !recipe.assemble(input).is(wanted)) {
			return null;
		}
		return new Order(recipe, width, height, grid, sourceSlots);
	}

	private static int findSlot(Container chest, Ingredient ingredient, int[] used) {
		for (int slot = 0; slot < chest.getContainerSize(); slot++) {
			ItemStack stack = chest.getItem(slot);
			if (!stack.isEmpty() && stack.getCount() > used[slot] && ingredient.test(stack)) {
				return slot;
			}
		}
		return -1;
	}

	/** Crafting recipes by the item they make, worked out once per recipe reload. */
	private static List<CraftingRecipe> recipesFor(ServerLevel level, Item result) {
		RecipeManager manager = level.getServer().getRecipeManager();
		if (manager != cachedFor) {
			cachedFor = manager;
			RECIPES_BY_RESULT.clear();
			var context = SlotDisplayContext.fromLevel(level);
			for (RecipeHolder<?> holder : manager.getRecipes()) {
				if (holder.value() instanceof CraftingRecipe recipe && !recipe.isSpecial()) {
					for (RecipeDisplay display : recipe.display()) {
						ItemStack shown = display.result().resolveForFirstStack(context);
						if (!shown.isEmpty()) {
							RECIPES_BY_RESULT.computeIfAbsent(shown.getItem(), item -> new ArrayList<>()).add(recipe);
						}
					}
				}
			}
		}
		return RECIPES_BY_RESULT.getOrDefault(result, List.of());
	}
}

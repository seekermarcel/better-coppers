package dev.bettercoppers.golem;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/** Small container and pouch helpers shared by the golem behaviours. */
public final class Inventories {
	private Inventories() {
	}

	/** Takes up to {@code limit} items of the first non-empty slot that matches. */
	public static ItemStack takeFirst(Container container, int limit, Predicate<ItemStack> filter) {
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			ItemStack stack = container.getItem(slot);
			if (!stack.isEmpty() && filter.test(stack)) {
				ItemStack taken = container.removeItem(slot, Math.min(stack.getCount(), limit));
				container.setChanged();
				return taken;
			}
		}
		return ItemStack.EMPTY;
	}

	public static boolean containsItem(Container container, ItemStack item) {
		return contains(container, stack -> ItemStack.isSameItem(stack, item));
	}

	public static boolean contains(Container container, Predicate<ItemStack> filter) {
		for (ItemStack stack : container) {
			if (!stack.isEmpty() && filter.test(stack)) {
				return true;
			}
		}
		return false;
	}

	public static boolean hasRoom(Container container, ItemStack item) {
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			ItemStack stack = container.getItem(slot);
			if (stack.isEmpty() ? container.canPlaceItem(slot, item)
				: ItemStack.isSameItemSameComponents(stack, item) && stack.getCount() < container.getMaxStackSize(stack)) {
				return true;
			}
		}
		return false;
	}

	public static boolean hasEmptySlot(Container container) {
		for (ItemStack stack : container) {
			if (stack.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	/** Moves as much of {@code stack} as fits into the container, topping up existing stacks first. Returns the rest. */
	public static ItemStack insert(Container container, ItemStack stack) {
		ItemStack rest = stack.copy();
		for (int slot = 0; slot < container.getContainerSize() && !rest.isEmpty(); slot++) {
			ItemStack existing = container.getItem(slot);
			if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, rest)) {
				int moved = Math.min(rest.getCount(), container.getMaxStackSize(existing) - existing.getCount());
				if (moved > 0) {
					existing.grow(moved);
					rest.shrink(moved);
					container.setItem(slot, existing);
				}
			}
		}
		for (int slot = 0; slot < container.getContainerSize() && !rest.isEmpty(); slot++) {
			if (container.getItem(slot).isEmpty() && container.canPlaceItem(slot, rest)) {
				int moved = Math.min(rest.getCount(), container.getMaxStackSize(rest));
				container.setItem(slot, rest.split(moved));
			}
		}
		container.setChanged();
		return rest;
	}

	/** Adds to a pouch, merging with existing stacks. Leaves {@code stack} untouched. */
	public static void addTo(List<ItemStack> pouch, ItemStack stack) {
		ItemStack rest = stack.copy();
		for (ItemStack existing : pouch) {
			if (rest.isEmpty()) {
				return;
			}
			if (ItemStack.isSameItemSameComponents(existing, rest) && existing.getCount() < existing.getMaxStackSize()) {
				int moved = Math.min(rest.getCount(), existing.getMaxStackSize() - existing.getCount());
				existing.grow(moved);
				rest.shrink(moved);
			}
		}
		if (!rest.isEmpty()) {
			pouch.add(rest);
		}
	}

	/** The first matching stack (not taken) in the pouches, then the cart; or an empty stack. */
	public static ItemStack find(List<List<ItemStack>> pouches, @Nullable Container cart, Predicate<ItemStack> filter) {
		for (List<ItemStack> pouch : pouches) {
			for (ItemStack stack : pouch) {
				if (!stack.isEmpty() && filter.test(stack)) {
					return stack;
				}
			}
		}
		if (cart != null) {
			for (ItemStack stack : cart) {
				if (!stack.isEmpty() && filter.test(stack)) {
					return stack;
				}
			}
		}
		return ItemStack.EMPTY;
	}

	/** Takes one matching item out of the pouches, then the cart; or an empty stack. */
	public static ItemStack takeOne(List<List<ItemStack>> pouches, @Nullable Container cart, Predicate<ItemStack> filter) {
		for (List<ItemStack> pouch : pouches) {
			for (ItemStack stack : pouch) {
				if (!stack.isEmpty() && filter.test(stack)) {
					ItemStack one = stack.split(1);
					pouch.removeIf(ItemStack::isEmpty);
					return one;
				}
			}
		}
		return cart == null ? ItemStack.EMPTY : takeFirst(cart, 1, filter);
	}

	public static int count(Iterable<ItemStack> stacks, Predicate<ItemStack> filter) {
		int count = 0;
		for (ItemStack stack : stacks) {
			if (filter.test(stack)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	/** Into the player's inventory, or at their feet if it is full. */
	public static void giveOrDrop(Player player, ItemStack stack) {
		if (!player.getInventory().add(stack)) {
			player.spawnAtLocation((ServerLevel) player.level(), stack);
		}
	}

	/** Every block entity inside {@code area}, from chunks that are already loaded. */
	public static void forEachContainer(ServerLevel level, AABB area, Consumer<BlockEntity> action) {
		for (int x = SectionPos.blockToSectionCoord(area.minX); x <= SectionPos.blockToSectionCoord(area.maxX); x++) {
			for (int z = SectionPos.blockToSectionCoord(area.minZ); z <= SectionPos.blockToSectionCoord(area.maxZ); z++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
				if (chunk == null) {
					continue;
				}
				for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
					BlockPos pos = blockEntity.getBlockPos();
					if (area.contains(pos.getX(), pos.getY(), pos.getZ())) {
						action.accept(blockEntity);
					}
				}
			}
		}
	}
}

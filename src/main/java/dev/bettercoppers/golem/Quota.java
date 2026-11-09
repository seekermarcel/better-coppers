package dev.bettercoppers.golem;

import dev.bettercoppers.item.QuotaSlipItem;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** How much of each item a chest should hold, from the Quota Slips hung on it. An item on two slips counts for the larger amount. */
public final class Quota {
	public static final Quota NONE = new Quota(Map.of());

	private final Map<Item, Integer> amounts;

	private Quota(Map<Item, Integer> amounts) {
		this.amounts = amounts;
	}

	static Quota of(List<ItemStack> shown) {
		Map<Item, Integer> amounts = new HashMap<>();
		for (ItemStack frame : shown) {
			if (frame.getItem() instanceof QuotaSlipItem) {
				QuotaSlipItem.listed(frame).forEach((item, amount) -> amounts.merge(item, amount, Math::max));
			}
		}
		return amounts.isEmpty() ? NONE : new Quota(amounts);
	}

	public boolean isEmpty() {
		return this.amounts.isEmpty();
	}

	/** The amount set for this item, 0 if none is. */
	public int of(Item item) {
		return this.amounts.getOrDefault(item, 0);
	}

	/** How many of this item the chest may still take. Unlimited for an item without a quota. */
	public int room(Container container, ItemStack item) {
		Integer amount = this.amounts.get(item.getItem());
		return amount == null ? Integer.MAX_VALUE : Math.max(0, amount - container.countItem(item.getItem()));
	}

	/** Moves as much of {@code stack} into the container as fits and the quota allows. Returns the rest. */
	public ItemStack insert(Container container, ItemStack stack) {
		int room = this.room(container, stack);
		if (room >= stack.getCount()) {
			return Inventories.insert(container, stack);
		}
		ItemStack rest = stack.copy();
		rest.grow(Inventories.insert(container, rest.split(room)).getCount());
		return rest;
	}

	/** What is missing from the chest, per item. Empty when every quota is met. */
	public Map<Item, Integer> shortageIn(Container container) {
		Map<Item, Integer> shortage = new HashMap<>();
		this.amounts.forEach((item, amount) -> {
			int missing = amount - container.countItem(item);
			if (missing > 0) {
				shortage.put(item, missing);
			}
		});
		return shortage;
	}
}

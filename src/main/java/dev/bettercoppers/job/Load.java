package dev.bettercoppers.job;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.bettercoppers.golem.CartChaining;
import dev.bettercoppers.golem.Inventories;
import dev.bettercoppers.registry.ModAttachments;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * What a golem with a paying job carries for it: pouch, supplies and the chained cart. A short-lived view, so get a new
 * one instead of keeping it. Reading never adds an attachment to the golem; the first write does.
 * <p>
 * Items are never lost: what does not fit in the chest stays in the load, and only {@link #dropAll} lets go of them.
 */
public final class Load {
	/** The saved part: the cart keeps its own items. */
	public record Stored(List<ItemStack> pouch, List<ItemStack> supplies) {
		public static final Codec<Stored> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("pouch", List.of()).forGetter(Stored::pouch),
					ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("supplies", List.of()).forGetter(Stored::supplies)
				)
				.apply(instance, (pouch, supplies) -> new Stored(withoutEmpty(pouch), withoutEmpty(supplies)))
		);

		private static List<ItemStack> withoutEmpty(List<ItemStack> stacks) {
			return new ArrayList<>(stacks.stream().filter(stack -> !stack.isEmpty()).toList());
		}
	}

	private @Nullable CopperGolem golem;
	private List<ItemStack> pouch;
	private List<ItemStack> supplies;
	private final @Nullable Container cart;

	Load(List<ItemStack> pouch, List<ItemStack> supplies, @Nullable Container cart) {
		this.pouch = pouch;
		this.supplies = supplies;
		this.cart = cart;
	}

	public static Load of(CopperGolem golem) {
		Stored stored = golem.getAttached(ModAttachments.LOAD);
		Load load = stored == null
			? new Load(new ArrayList<>(), new ArrayList<>(), CartChaining.findCart(golem))
			: new Load(stored.pouch(), stored.supplies(), CartChaining.findCart(golem));
		load.golem = golem;
		return load;
	}

	/** Into the cart first, then the pouch. Leaves {@code stack} untouched. */
	public void stash(ItemStack stack) {
		ItemStack rest = this.cart == null ? stack : Inventories.insert(this.cart, stack);
		if (!rest.isEmpty()) {
			this.attach();
			Inventories.addTo(this.pouch, rest);
		}
	}

	/** No empty slot left in the cart (or none chained) and the pouch reached {@code limit}. */
	public boolean isFull(int limit) {
		return (this.cart == null || !Inventories.hasEmptySlot(this.cart)) && Inventories.count(this.pouch, stack -> true) >= limit;
	}

	/** Pouch and cart are empty. Supplies stay with the golem, so they do not count. */
	public boolean hasNothingToBringHome() {
		return this.pouch.isEmpty() && (this.cart == null || this.cart.isEmpty());
	}

	/** The first matching stack (not taken) in the supplies, the pouch, then the cart; or an empty stack. */
	public ItemStack find(Predicate<ItemStack> filter) {
		return Inventories.find(List.of(this.supplies, this.pouch), this.cart, filter);
	}

	/** Takes one matching item, in the same order as {@link #find}; or an empty stack. */
	public ItemStack takeOne(Predicate<ItemStack> filter) {
		return Inventories.takeOne(List.of(this.supplies, this.pouch), this.cart, filter);
	}

	public int count(Predicate<ItemStack> filter) {
		return this.suppliesCount(filter) + Inventories.count(this.pouch, filter) + (this.cart == null ? 0 : Inventories.count(this.cart, filter));
	}

	/** Only the supplies, for restocking up to a reserve. */
	public int suppliesCount(Predicate<ItemStack> filter) {
		return Inventories.count(this.supplies, filter);
	}

	public void addSupplies(ItemStack stack) {
		this.attach();
		Inventories.addTo(this.supplies, stack);
	}

	/**
	 * Empties the pouch and the cart into the chest. {@code keep} says how many of a pouch stack go in the supplies
	 * instead. What does not fit stays in the load.
	 */
	public void unloadInto(Container chest, ToIntFunction<ItemStack> keep) {
		for (ItemStack stack : this.pouch) {
			int kept = Math.min(stack.getCount(), keep.applyAsInt(stack));
			if (kept > 0) {
				this.addSupplies(stack.split(kept));
			}
			stack.setCount(Inventories.insert(chest, stack).getCount());
		}
		this.pouch.removeIf(ItemStack::isEmpty);
		if (this.cart != null) {
			for (int slot = 0; slot < this.cart.getContainerSize(); slot++) {
				ItemStack stack = this.cart.getItem(slot);
				if (!stack.isEmpty()) {
					this.cart.setItem(slot, Inventories.insert(chest, stack));
				}
			}
		}
	}

	/** Drops pouch and supplies when the golem dies or turns into a statue. The cart stays where it is and keeps its items. */
	public void dropAll(ServerLevel level, CopperGolem golem) {
		this.pouch.forEach(stack -> golem.spawnAtLocation(level, stack));
		this.supplies.forEach(stack -> golem.spawnAtLocation(level, stack));
		this.pouch.clear();
		this.supplies.clear();
		golem.removeAttached(ModAttachments.LOAD);
	}

	/** Before the first write: saves these lists on the golem, or uses the ones another view saved since. */
	private void attach() {
		if (this.golem == null) {
			return;
		}
		Stored stored = this.golem.getAttached(ModAttachments.LOAD);
		if (stored == null) {
			this.golem.setAttached(ModAttachments.LOAD, new Stored(this.pouch, this.supplies));
		} else {
			this.pouch = stored.pouch();
			this.supplies = stored.supplies();
		}
	}
}

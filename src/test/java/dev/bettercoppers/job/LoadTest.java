package dev.bettercoppers.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LoadTest {
	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	private static List<ItemStack> stacks(ItemStack... stacks) {
		return new ArrayList<>(List.of(stacks));
	}

	private static int total(Load load, Item item) {
		return load.count(stack -> stack.is(item));
	}

	@Test
	void handOverGivesPouchAndSuppliesButLeavesTheCart() {
		SimpleContainer cart = new SimpleContainer(new ItemStack(Items.STONE, 3));
		Load load = new Load(stacks(new ItemStack(Items.DIRT, 2)), stacks(new ItemStack(Items.SAND, 1)), cart);
		List<ItemStack> received = new ArrayList<>();
		load.handOver(received::add);
		assertEquals(2, received.size());
		assertEquals(0, total(load, Items.DIRT) + total(load, Items.SAND));
		assertEquals(3, total(load, Items.STONE));
	}

	@Test
	void searchesSuppliesThenPouchThenCart() {
		// Same item in all three, told apart by the stack size.
		SimpleContainer cart = new SimpleContainer(new ItemStack(Items.STONE, 3));
		Load load = new Load(stacks(new ItemStack(Items.STONE, 2)), stacks(new ItemStack(Items.STONE, 1)), cart);
		assertEquals(1, load.find(stack -> stack.is(Items.STONE)).getCount());
		assertEquals(6, total(load, Items.STONE));
		assertEquals(1, load.suppliesCount(stack -> stack.is(Items.STONE)));

		load.takeOne(stack -> stack.is(Items.STONE));
		assertEquals(0, load.suppliesCount(stack -> stack.is(Items.STONE)));
		assertEquals(2, load.find(stack -> stack.is(Items.STONE)).getCount());

		load.takeOne(stack -> stack.is(Items.STONE));
		load.takeOne(stack -> stack.is(Items.STONE));
		assertEquals(3, load.find(stack -> stack.is(Items.STONE)).getCount());
		assertEquals(1, load.takeOne(stack -> stack.is(Items.STONE)).getCount());
		assertEquals(2, cart.getItem(0).getCount());
	}

	@Test
	void findsNothingAsEmptyStack() {
		Load load = new Load(stacks(), stacks(), null);
		assertTrue(load.find(stack -> true).isEmpty());
		assertTrue(load.takeOne(stack -> true).isEmpty());
	}

	@Test
	void stashesInCartThenPouch() {
		SimpleContainer cart = new SimpleContainer(1);
		List<ItemStack> pouch = stacks();
		Load load = new Load(pouch, stacks(), cart);
		ItemStack given = new ItemStack(Items.STONE, 70);
		load.stash(given);
		assertEquals(64, cart.getItem(0).getCount());
		assertEquals(6, pouch.getFirst().getCount());
		assertEquals(70, given.getCount());
	}

	@Test
	void isFullOnlyWithoutRoomInCartAndAtTheLimit() {
		assertFalse(new Load(stacks(new ItemStack(Items.STONE, 63)), stacks(), null).isFull(64));
		assertTrue(new Load(stacks(new ItemStack(Items.STONE, 64)), stacks(), null).isFull(64));
		assertFalse(new Load(stacks(new ItemStack(Items.STONE, 64)), stacks(), new SimpleContainer(1)).isFull(64));
		assertTrue(new Load(stacks(new ItemStack(Items.STONE, 64)), stacks(), new SimpleContainer(new ItemStack(Items.DIRT))).isFull(64));
	}

	@Test
	void hasNothingToBringHomeIgnoresSupplies() {
		assertTrue(new Load(stacks(), stacks(new ItemStack(Items.WHEAT_SEEDS)), new SimpleContainer(1)).hasNothingToBringHome());
		assertFalse(new Load(stacks(new ItemStack(Items.STONE)), stacks(), null).hasNothingToBringHome());
		assertFalse(new Load(stacks(), stacks(), new SimpleContainer(new ItemStack(Items.STONE))).hasNothingToBringHome());
	}

	@Test
	void unloadKeepsWhatKeepSays() {
		List<ItemStack> supplies = stacks(new ItemStack(Items.WHEAT_SEEDS, 2));
		Load load = new Load(stacks(new ItemStack(Items.WHEAT_SEEDS, 10), new ItemStack(Items.WHEAT, 5)), supplies, null);
		SimpleContainer chest = new SimpleContainer(9);
		load.unloadInto(chest, stack -> stack.is(Items.WHEAT_SEEDS) ? 4 - load.suppliesCount(other -> other.is(Items.WHEAT_SEEDS)) : -3);
		assertEquals(4, supplies.getFirst().getCount());
		assertEquals(8, chest.countItem(Items.WHEAT_SEEDS));
		assertEquals(5, chest.countItem(Items.WHEAT));
		assertTrue(load.hasNothingToBringHome());
	}

	@Test
	void overflowStaysInPouchAndCart() {
		SimpleContainer cart = new SimpleContainer(new ItemStack(Items.DIRT, 10), new ItemStack(Items.STONE, 64));
		List<ItemStack> pouch = stacks(new ItemStack(Items.DIRT, 60));
		Load load = new Load(pouch, stacks(), cart);
		SimpleContainer chest = new SimpleContainer(new ItemStack(Items.DIRT, 50));

		load.unloadInto(chest, stack -> 0);
		assertEquals(64, chest.getItem(0).getCount());
		assertEquals(46, pouch.getFirst().getCount());
		assertEquals(10, cart.getItem(0).getCount());
		assertEquals(64, cart.getItem(1).getCount());
		assertFalse(load.hasNothingToBringHome());

		// Room again: nothing was lost.
		SimpleContainer bigger = new SimpleContainer(3);
		load.unloadInto(bigger, stack -> 0);
		assertEquals(56, bigger.countItem(Items.DIRT));
		assertEquals(64, bigger.countItem(Items.STONE));
		assertTrue(load.hasNothingToBringHome());
	}

	@Test
	void codecRoundTrip() {
		Load.Stored stored = new Load.Stored(stacks(new ItemStack(Items.STONE, 7)), stacks(new ItemStack(Items.TORCH, 16)));
		Load.Stored read = Load.Stored.CODEC.parse(NbtOps.INSTANCE, Load.Stored.CODEC.encodeStart(NbtOps.INSTANCE, stored).getOrThrow()).getOrThrow();
		assertTrue(ItemStack.matches(stored.pouch().getFirst(), read.pouch().getFirst()));
		assertTrue(ItemStack.matches(stored.supplies().getFirst(), read.supplies().getFirst()));
		// Read lists stay writable.
		read.pouch().clear();
	}
}

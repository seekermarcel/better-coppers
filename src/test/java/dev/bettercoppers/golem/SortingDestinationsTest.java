package dev.bettercoppers.golem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.bettercoppers.golem.SortingDestinations.Found;
import dev.bettercoppers.golem.SortingDestinations.Kind;
import dev.bettercoppers.golem.SortingDestinations.Spot;
import dev.bettercoppers.golem.SortingDestinations.Tier;
import java.util.List;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SortingDestinationsTest {
	private static final Vec3 ORIGIN = Vec3.ZERO;

	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createWorldLookup()).forEach(DataComponentInitializers.PendingComponents::apply);
	}

	private static Spot spot(int x, Kind kind, SimpleContainer container, List<ItemStack> frames, BlockPos... parts) {
		BlockPos pos = new BlockPos(x, 0, 0);
		Set<BlockPos> all = parts.length == 0 ? Set.of(pos) : Set.of(parts);
		return new Spot(pos, kind, container, new FrameFilter(frames, (type, item) -> false), all);
	}

	private static SimpleContainer holding(ItemStack stack) {
		SimpleContainer container = new SimpleContainer(27);
		container.addItem(stack);
		return container;
	}

	private static SimpleContainer full() {
		SimpleContainer container = new SimpleContainer(1);
		container.setItem(0, new ItemStack(Items.DIRT, 64));
		return container;
	}

	@Test
	void tierOrder() {
		ItemStack coal = new ItemStack(Items.COAL);
		Spot empty = spot(1, Kind.CHEST, new SimpleContainer(27), List.of());
		Spot barrel = spot(2, Kind.BARREL, new SimpleContainer(27), List.of());
		Spot matching = spot(3, Kind.CHEST, holding(new ItemStack(Items.COAL)), List.of());
		Spot framed = spot(4, Kind.BARREL, new SimpleContainer(27), List.of(new ItemStack(Items.COAL)));

		assertEquals(Tier.FRAMED, SortingDestinations.find(List.of(empty, barrel, matching, framed), coal, ORIGIN).tier());
		assertEquals(Tier.MATCHING_CHEST, SortingDestinations.find(List.of(empty, barrel, matching), coal, ORIGIN).tier());
		assertEquals(Tier.BARREL, SortingDestinations.find(List.of(empty, barrel), coal, ORIGIN).tier());
		assertEquals(Tier.EMPTY_CHEST, SortingDestinations.find(List.of(empty), coal, ORIGIN).tier());
	}

	@Test
	void nearestFirst() {
		ItemStack coal = new ItemStack(Items.COAL);
		Spot far = spot(9, Kind.BARREL, new SimpleContainer(27), List.of());
		Spot near = spot(2, Kind.BARREL, new SimpleContainer(27), List.of());
		Found found = SortingDestinations.find(List.of(far, near), coal, ORIGIN);
		assertEquals(List.of(near, far), found.spots());
	}

	@Test
	void framedChestRefusesOtherItems() {
		Spot framed = spot(1, Kind.CHEST, new SimpleContainer(27), List.of(new ItemStack(Items.COAL)));
		assertNull(SortingDestinations.find(List.of(framed), new ItemStack(Items.DIRT), ORIGIN));
		assertNotNull(SortingDestinations.find(List.of(framed), new ItemStack(Items.COAL), ORIGIN));
	}

	@Test
	void fullChestSkipped() {
		Spot full = spot(1, Kind.BARREL, full(), List.of());
		assertNull(SortingDestinations.find(List.of(full), new ItemStack(Items.DIRT), ORIGIN));
	}

	@Test
	void decoysAvoidDestinationAndItsOtherHalf() {
		BlockPos a = new BlockPos(0, 0, 0);
		BlockPos b = new BlockPos(0, 0, 1);
		SimpleContainer shared = new SimpleContainer(54);
		Spot half1 = new Spot(a, Kind.CHEST, shared, new FrameFilter(List.of(), (type, item) -> false), Set.of(a, b));
		Spot half2 = new Spot(b, Kind.CHEST, shared, new FrameFilter(List.of(), (type, item) -> false), Set.of(a, b));
		Spot other = spot(5, Kind.BARREL, new SimpleContainer(27), List.of());
		ItemStack item = new ItemStack(Items.COAL);
		for (int i = 0; i < 20; i++) {
			List<BlockPos> decoys = SortingDestinations.decoys(List.of(half1, half2, other), half1, item, 3, ORIGIN, RandomSource.create(i));
			assertEquals(List.of(other.pos()), decoys);
		}
	}

	@Test
	void decoyCountCapped() {
		Spot destination = spot(0, Kind.CHEST, new SimpleContainer(27), List.of());
		List<Spot> all = new java.util.ArrayList<>(List.of(destination));
		for (int x = 1; x <= 8; x++) {
			all.add(spot(x, Kind.BARREL, new SimpleContainer(27), List.of()));
		}
		ItemStack item = new ItemStack(Items.COAL);
		assertEquals(0, SortingDestinations.decoys(all, destination, item, 0, ORIGIN, RandomSource.create(1)).size());
		assertEquals(2, SortingDestinations.decoys(all, destination, item, 2, ORIGIN, RandomSource.create(1)).size());
		assertEquals(3, SortingDestinations.decoys(all, destination, item, 3, ORIGIN, RandomSource.create(1)).size());
		// Only the 6 containers closest to the destination are candidates.
		assertEquals(6, SortingDestinations.decoys(all, destination, item, 20, ORIGIN, RandomSource.create(1)).size());
	}

	@Test
	void cartRule() {
		ItemStack delivered = new ItemStack(Items.COAL);
		Spot holdsIron = spot(1, Kind.CHEST, holding(new ItemStack(Items.IRON_INGOT)), List.of());
		ItemStack iron = new ItemStack(Items.IRON_INGOT);
		ItemStack dirt = new ItemStack(Items.DIRT);

		assertTrue(SortingDestinations.belongs(delivered, delivered, holdsIron, Tier.BARREL));
		assertFalse(SortingDestinations.belongs(iron, delivered, holdsIron, Tier.BARREL));
		assertTrue(SortingDestinations.belongs(iron, delivered, holdsIron, Tier.MATCHING_CHEST));
		assertFalse(SortingDestinations.belongs(dirt, delivered, holdsIron, Tier.MATCHING_CHEST));

		Spot framed = spot(2, Kind.CHEST, new SimpleContainer(27), List.of(new ItemStack(Items.DIRT)));
		assertTrue(SortingDestinations.belongs(dirt, delivered, framed, Tier.FRAMED));
		assertFalse(SortingDestinations.belongs(delivered, delivered, framed, Tier.FRAMED));
	}
}

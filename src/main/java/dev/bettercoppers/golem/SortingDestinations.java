package dev.bettercoppers.golem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The sorter's pure decisions: where an item goes, which containers to open as decoys, what the cart unloads. Needs no world. */
final class SortingDestinations {
	/** Decoys are picked from this many containers closest to the real destination. */
	private static final int DECOY_CANDIDATES = 6;

	private SortingDestinations() {
	}

	enum Kind {
		SOURCE,
		CHEST,
		BARREL
	}

	/**
	 * Where an item may go, in order of preference. A chest or barrel with item frames on it is reserved: it takes the
	 * items its frames show, and nothing else.
	 */
	enum Tier {
		FRAMED,
		MATCHING_CHEST,
		BARREL,
		EMPTY_CHEST;

		boolean accepts(Spot spot, ItemStack item) {
			if (this == FRAMED) {
				return spot.kind() != Kind.SOURCE && spot.frames().allows(item) && Inventories.hasRoom(spot.container(), item);
			}
			if (!spot.frames().isEmpty()) {
				return false;
			}
			return switch (this) {
				case MATCHING_CHEST -> spot.kind() == Kind.CHEST && Inventories.containsItem(spot.container(), item) && Inventories.hasRoom(spot.container(), item);
				case BARREL -> spot.kind() == Kind.BARREL && Inventories.hasRoom(spot.container(), item);
				case EMPTY_CHEST -> spot.kind() == Kind.CHEST && spot.container().isEmpty();
				case FRAMED -> false;
			};
		}
	}

	/**
	 * {@code frames}: the item frames hung on this container (either half of a double chest).
	 * {@code parts}: the positions of all halves of the container, including {@code pos}.
	 */
	record Spot(BlockPos pos, Kind kind, Container container, FrameFilter frames, Set<BlockPos> parts) {
	}

	/** The best tier that has a destination, and its spots, nearest first. */
	record Found(Tier tier, List<Spot> spots) {
	}

	static @Nullable Found find(List<Spot> spots, ItemStack item, Vec3 from) {
		for (Tier tier : Tier.values()) {
			List<Spot> accepting = new ArrayList<>(spots.stream().filter(spot -> tier.accepts(spot, item)).toList());
			if (!accepting.isEmpty()) {
				accepting.sort(Comparator.comparingDouble(spot -> spot.pos().distToCenterSqr(from)));
				return new Found(tier, accepting);
			}
		}
		return null;
	}

	/** Up to {@code count} containers to open before the destination, nearest to {@code from} first. Never the destination or its other half. */
	static List<BlockPos> decoys(List<Spot> spots, Spot destination, ItemStack item, int count, Vec3 from, RandomSource random) {
		if (count <= 0) {
			return List.of();
		}
		Set<BlockPos> excluded = new HashSet<>(destination.parts());
		List<Spot> candidates = new ArrayList<>();
		for (Spot spot : spots) {
			if (spot.kind() != Kind.SOURCE && !excluded.contains(spot.pos()) && !Inventories.containsItem(spot.container(), item)) {
				candidates.add(spot);
			}
		}
		candidates.sort(Comparator.comparingDouble(spot -> spot.pos().distSqr(destination.pos())));
		List<Spot> pool = new ArrayList<>(candidates.subList(0, Math.min(DECOY_CANDIDATES, candidates.size())));
		Util.shuffle(pool, random);

		List<BlockPos> chosen = new ArrayList<>();
		for (Spot spot : pool) {
			if (chosen.size() >= count) {
				break;
			}
			if (excluded.contains(spot.pos())) {
				continue;
			}
			chosen.add(spot.pos());
			excluded.addAll(spot.parts());
		}
		chosen.sort(Comparator.comparingDouble(pos -> pos.distToCenterSqr(from)));
		return chosen;
	}

	/** Whether a cart stack unloads here: same item as delivered, or (matching chest) anything the chest holds, or (framed) what the frames show. */
	static boolean belongs(ItemStack stack, ItemStack delivered, Spot destination, Tier tier) {
		if (tier == Tier.FRAMED) {
			return destination.frames().allows(stack);
		}
		return ItemStack.isSameItem(stack, delivered) || tier == Tier.MATCHING_CHEST && Inventories.containsItem(destination.container(), stack);
	}
}

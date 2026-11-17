package dev.bettercoppers.mining;

import dev.bettercoppers.api.MineSectionTemplate;
import dev.bettercoppers.api.MineSectionTemplate.Turn;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Every Section Kind a miner rolls, in the order they are rolled: the built-ins, then the add-ons' in registration order.
 * Level kinds have a {@link MineSectionTemplate}; the stairs have their own generators in {@link MineLayout}. The
 * Barrier is never rolled and is not here.
 */
public final class MineSections {
	/**
	 * A Section Kind with its Section Weight ({@code highWeight} while the Open End is above y 0) and which parent kinds
	 * it may follow; the parent is null at the entrance.
	 */
	public record Kind(Identifier id, int weight, int highWeight, @Nullable MineSectionTemplate template, Predicate<@Nullable Identifier> mayFollow) {
	}

	private static final Map<Identifier, Kind> KINDS = new LinkedHashMap<>();

	static {
		register(new Kind(MineLayout.STRAIGHT, 40, 40, middleRails(7)
			.columns(0, 7, -1, 1).support(2, -1, 1).support(6, -1, 1).torch(3, 0)
			.openEnd(8, 0, Turn.AHEAD).build(), parent -> true));
		// Each mirror image is a candidate of its own, so a Corner is 16 in all.
		register(new Kind(MineLayout.CORNER, 8, 8, MineSectionTemplate.builder()
			.columns(0, 2, -1, 1).rail(1, 1).rail(1, 0).rail(0, 0)
			.openEnd(1, 2, Turn.RIGHT).mirrorable().build(), parent -> !MineLayout.CORNER.equals(parent)));
		register(new Kind(MineLayout.STAIRS_DOWN, 12, 24, null, parent -> !MineLayout.STAIRS_UP.equals(parent)));
		register(new Kind(MineLayout.STAIRS_UP, 6, 3, null, parent -> !MineLayout.STAIRS_DOWN.equals(parent)));
		register(new Kind(MineLayout.CROSSING, 10, 10, middleRails(4)
			.columns(0, 4, -1, 1).columns(1, 3, -2, 2).pillar(1, -1).pillar(1, 1).pillar(3, -1).pillar(3, 1).torch(1, 0)
			.openEnd(5, 0, Turn.AHEAD).openEnd(2, -3, Turn.LEFT).openEnd(2, 3, Turn.RIGHT).build(), parent -> true));
		register(new Kind(MineLayout.SMALL_HALL, 5, 5, middleRails(6)
			.columns(0, 6, -2, 2).support(1, -2, 2).support(5, -2, 2).torch(2, 0).torch(4, 0)
			.openEnd(7, 0, Turn.AHEAD).openEnd(3, -3, Turn.LEFT).openEnd(3, 3, Turn.RIGHT).build(), parent -> true));
		register(new Kind(MineLayout.DEAD_END, 11, 11, middleRails(2)
			.columns(0, 3, -1, 1).support(1, -1, 1).torch(3, 0).build(), parent -> true));
	}

	private MineSections() {
	}

	/** A builder with rails down the middle of slices 0 to {@code to}. */
	private static MineSectionTemplate.Builder middleRails(int to) {
		MineSectionTemplate.Builder builder = MineSectionTemplate.builder();
		for (int f = 0; f <= to; f++) {
			builder.rail(f, 0);
		}
		return builder;
	}

	/** Adds a kind. Throws {@link IllegalArgumentException} if its id is taken. */
	public static void register(Kind kind) {
		if (KINDS.containsKey(kind.id()) || kind.id().equals(MineLayout.BARRIER)) {
			throw new IllegalArgumentException("Mine Section kind " + kind.id() + " is already registered");
		}
		KINDS.put(kind.id(), kind);
	}

	public static Collection<Kind> all() {
		return KINDS.values();
	}

	public static @Nullable Kind get(Identifier id) {
		return KINDS.get(id);
	}

	/** Whether this mod can lay out a section of {@code id}: false for the kind of an add-on that was removed. */
	public static boolean isKnown(Identifier id) {
		return KINDS.containsKey(id) || id.equals(MineLayout.BARRIER);
	}
}

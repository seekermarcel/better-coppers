package dev.bettercoppers.mining;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

/**
 * Picks the Section Kind for an Open End: a weighted pick without replacement among the kinds the follow rules allow,
 * seeded per Open End so it always rolls the same sequence. Weights are on a scale where the built-ins sum to 100.
 */
public final class SectionRoll {
	/** One kind to try, mirrored or not, and its Section Weight. */
	public record Candidate(Identifier kind, boolean mirrored, int weight) {
	}

	private SectionRoll() {
	}

	/**
	 * The candidates for an Open End of a {@code parent} section (null for the entrance), from {@link MineSections}: each
	 * kind its own follow rules allow (stairs never undo the stairs before them, a Corner never follows a Corner), a
	 * mirrorable kind twice, and the mine's {@code lastEnd} never gets a kind without Open Ends. Above y 0 ({@code high})
	 * the mine leans down.
	 */
	public static List<Candidate> candidates(@Nullable Identifier parent, boolean lastEnd, boolean high) {
		List<Candidate> candidates = new ArrayList<>();
		for (MineSections.Kind kind : MineSections.all()) {
			if (!kind.mayFollow().test(parent) || lastEnd && isDeadEnd(kind.id())) {
				continue;
			}
			int weight = high ? kind.highWeight() : kind.weight();
			candidates.add(new Candidate(kind.id(), false, weight));
			if (kind.template() != null && kind.template().mirrorable()) {
				candidates.add(new Candidate(kind.id(), true, weight));
			}
		}
		return candidates;
	}

	private static boolean isDeadEnd(Identifier kind) {
		return MineLayout.of(kind, new Mines.OpenEnd(BlockPos.ZERO, Direction.NORTH), false).openEnds().isEmpty();
	}

	/** All {@code candidates} in the order they are tried: each pick weighted among those not picked yet. */
	public static List<Candidate> order(List<Candidate> candidates, RandomSource random) {
		List<Candidate> left = new ArrayList<>(candidates);
		List<Candidate> order = new ArrayList<>();
		while (!left.isEmpty()) {
			int roll = random.nextInt(left.stream().mapToInt(Candidate::weight).sum());
			for (int i = 0; i < left.size(); i++) {
				roll -= left.get(i).weight();
				if (roll < 0) {
					order.add(left.remove(i));
					break;
				}
			}
		}
		return order;
	}

	/** The seed for an Open End's roll: the world seed, the mine's entrance, and the end's position and facing. */
	public static long seed(long worldSeed, BlockPos entrance, Mines.OpenEnd end) {
		long seed = worldSeed;
		seed = seed * 31 + entrance.asLong();
		seed = seed * 31 + end.pos().asLong();
		return seed * 31 + end.facing().get3DDataValue();
	}
}

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
	 * The candidates for an Open End of a {@code parent} section (null for the entrance). Stairs never undo the stairs
	 * before them, a Corner never follows a Corner, and the mine's {@code lastEnd} never gets a kind without Open Ends.
	 * Above y 0 ({@code high}) the mine leans down.
	 */
	public static List<Candidate> candidates(@Nullable Identifier parent, boolean lastEnd, boolean high) {
		List<Candidate> all = List.of(
			new Candidate(MineLayout.STRAIGHT, false, 40),
			new Candidate(MineLayout.CORNER, false, 8),
			new Candidate(MineLayout.CORNER, true, 8),
			new Candidate(MineLayout.STAIRS_DOWN, false, high ? 24 : 12),
			new Candidate(MineLayout.STAIRS_UP, false, high ? 3 : 6),
			new Candidate(MineLayout.CROSSING, false, 10),
			new Candidate(MineLayout.SMALL_HALL, false, 5),
			new Candidate(MineLayout.DEAD_END, false, 11));
		return all.stream().filter(candidate -> mayFollow(candidate.kind(), parent) && !(lastEnd && isDeadEnd(candidate.kind()))).toList();
	}

	private static boolean mayFollow(Identifier kind, @Nullable Identifier parent) {
		return !(kind.equals(MineLayout.STAIRS_UP) && MineLayout.STAIRS_DOWN.equals(parent))
			&& !(kind.equals(MineLayout.STAIRS_DOWN) && MineLayout.STAIRS_UP.equals(parent))
			&& !(kind.equals(MineLayout.CORNER) && MineLayout.CORNER.equals(parent));
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

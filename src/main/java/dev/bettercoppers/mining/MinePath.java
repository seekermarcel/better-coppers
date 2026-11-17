package dev.bettercoppers.mining;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;

/**
 * The Mine Path: from the entrance's opening along the start of each section up the tree ({@code parent} links) to a
 * target, merged into straight legs of at most {@link #MAX_LEG} blocks that break at every turn. A miner walks it leg by
 * leg, and the same way backwards to go home.
 */
final class MinePath {
	static final int MAX_LEG = 32;

	private MinePath() {
	}

	/** The legs from the opening of {@code mine} to {@code target}, an Open End or the start of a section. */
	static List<BlockPos> to(Mines.Mine mine, Mines.OpenEnd target) {
		List<BlockPos> points = new ArrayList<>();
		points.add(target.pos());
		for (int parent = mine.parentOf(target); parent >= 0; parent = mine.sections().get(parent).parent()) {
			points.add(mine.sections().get(parent).start().pos());
		}
		points.add(mine.entrance().below(2));
		return legs(points.reversed());
	}

	/** Keeps the first and last point and every point where the way turns or the leg would get longer than {@link #MAX_LEG}. */
	static List<BlockPos> legs(List<BlockPos> points) {
		List<BlockPos> legs = new ArrayList<>(List.of(points.getFirst()));
		for (int i = 1; i < points.size() - 1; i++) {
			BlockPos last = legs.getLast();
			BlockPos here = points.get(i);
			BlockPos next = points.get(i + 1);
			if (!straight(here.subtract(last), next.subtract(here)) || last.distSqr(next) > MAX_LEG * MAX_LEG) {
				legs.add(here);
			}
		}
		if (points.size() > 1) {
			legs.add(points.getLast());
		}
		return legs;
	}

	/** Whether going {@code b} after {@code a} keeps the same way. */
	private static boolean straight(BlockPos a, BlockPos b) {
		return a.cross(b).equals(BlockPos.ZERO) && a.getX() * b.getX() + a.getY() * b.getY() + a.getZ() * b.getZ() > 0;
	}
}

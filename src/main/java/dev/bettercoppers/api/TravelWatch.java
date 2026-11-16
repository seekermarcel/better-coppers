package dev.bettercoppers.api;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Tells a slow golem that is still getting somewhere apart from one that is stuck: only ticks in which it did not get
 * any closer to its target count. A long walk back from the home chest is fine; standing in front of a wall is not.
 * A watch from {@link #countingNewGround()} also counts stepping onto a block not stood on since the last reset as
 * progress, for long trips whose way may first lead away from the target.
 */
public final class TravelWatch {
	/** How much closer (in blocks) counts as progress. */
	private static final double PROGRESS = 0.5;

	private double best = Double.MAX_VALUE;
	private int stalled;
	private final @Nullable LongSet visited;

	public TravelWatch() {
		this.visited = null;
	}

	private TravelWatch(LongSet visited) {
		this.visited = visited;
	}

	/** A watch for which reaching new ground is progress too. */
	public static TravelWatch countingNewGround() {
		return new TravelWatch(new LongOpenHashSet());
	}

	public void reset() {
		this.best = Double.MAX_VALUE;
		this.stalled = 0;
		if (this.visited != null) {
			this.visited.clear();
		}
	}

	/** Records one tick of walking towards {@code target} and returns how many ticks in a row it made no progress. */
	public int tick(Entity walker, Vec3 target) {
		double distance = walker.position().distanceTo(target);
		boolean newGround = this.visited != null && this.visited.add(walker.blockPosition().asLong());
		if (distance < this.best - PROGRESS) {
			this.best = distance;
			this.stalled = 0;
		} else if (newGround) {
			this.stalled = 0;
		} else {
			this.stalled++;
		}
		return this.stalled;
	}
}

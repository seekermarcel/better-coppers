package dev.bettercoppers.api;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Tells a slow golem that is still getting somewhere apart from one that is stuck: only ticks in which it neither got
 * closer to its target nor stepped onto a block it had not stood on since the last reset count. A long walk back from
 * the home chest, or down a tunnel that first leads away, is fine; standing in front of a wall or pacing to and fro is not.
 */
public final class TravelWatch {
	/** How much closer (in blocks) counts as progress. */
	private static final double PROGRESS = 0.5;

	private double best = Double.MAX_VALUE;
	private int stalled;
	private final LongSet visited = new LongOpenHashSet();

	public void reset() {
		this.best = Double.MAX_VALUE;
		this.stalled = 0;
		this.visited.clear();
	}

	/** Records one tick of walking towards {@code target} and returns how many ticks in a row it made no progress. */
	public int tick(Entity walker, Vec3 target) {
		double distance = walker.position().distanceTo(target);
		boolean newGround = this.visited.add(walker.blockPosition().asLong());
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

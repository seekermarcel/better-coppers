package dev.bettercoppers.runningcosts;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Energy and rust progress of a working golem. Energy is spent on every bit of work and recharged on a Charging Pad;
 * rust progress adds up with the work done and makes the golem rust a stage further every so often.
 */
public final class GolemRunningCosts {
	public static final int MAX_ENERGY = 1000;

	public static final Codec<GolemRunningCosts> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
				Codec.intRange(0, MAX_ENERGY).optionalFieldOf("energy", MAX_ENERGY).forGetter(runningCosts -> runningCosts.energy),
				Codec.INT.optionalFieldOf("rust_progress", 0).forGetter(runningCosts -> runningCosts.rustProgress),
				Codec.BOOL.optionalFieldOf("recharging", false).forGetter(runningCosts -> runningCosts.recharging)
			)
			.apply(instance, GolemRunningCosts::new)
	);

	private int energy;
	private int rustProgress;
	private boolean recharging;

	public GolemRunningCosts() {
		this(MAX_ENERGY, 0, false);
	}

	private GolemRunningCosts(int energy, int rustProgress, boolean recharging) {
		this.energy = energy;
		this.rustProgress = rustProgress;
		this.recharging = recharging;
	}

	public int energy() {
		return this.energy;
	}

	public void setEnergy(int energy) {
		this.energy = Math.clamp(energy, 0, MAX_ENERGY);
		if (this.energy <= 0) {
			this.recharging = true;
		} else if (this.energy >= MAX_ENERGY) {
			this.recharging = false;
		}
	}

	/** It ran empty and has not been full since, so it does not work yet. */
	public boolean recharging() {
		return this.recharging;
	}

	/** Goes back to work with the energy it has. Without any, it keeps waiting. */
	public void stopRecharging() {
		this.recharging = this.energy <= 0;
	}

	public int rustProgress() {
		return this.rustProgress;
	}

	public void setRustProgress(int rustProgress) {
		this.rustProgress = Math.max(0, rustProgress);
	}
}

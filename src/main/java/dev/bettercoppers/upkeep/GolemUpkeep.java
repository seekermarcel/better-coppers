package dev.bettercoppers.upkeep;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Energy and wear of a working golem. Energy is spent on every bit of work and recharged on a Charging Pad;
 * wear adds up with the work done and makes the golem rust a stage further every so often.
 */
public final class GolemUpkeep {
	public static final int MAX_ENERGY = 1000;

	public static final Codec<GolemUpkeep> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
				Codec.intRange(0, MAX_ENERGY).optionalFieldOf("energy", MAX_ENERGY).forGetter(upkeep -> upkeep.energy),
				Codec.INT.optionalFieldOf("wear", 0).forGetter(upkeep -> upkeep.wear),
				Codec.BOOL.optionalFieldOf("recharging", false).forGetter(upkeep -> upkeep.recharging)
			)
			.apply(instance, GolemUpkeep::new)
	);

	private int energy;
	private int wear;
	private boolean recharging;

	public GolemUpkeep() {
		this(MAX_ENERGY, 0, false);
	}

	private GolemUpkeep(int energy, int wear, boolean recharging) {
		this.energy = energy;
		this.wear = wear;
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

	public int wear() {
		return this.wear;
	}

	public void setWear(int wear) {
		this.wear = Math.max(0, wear);
	}
}

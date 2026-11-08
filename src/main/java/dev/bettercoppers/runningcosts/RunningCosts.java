package dev.bettercoppers.runningcosts;

import dev.bettercoppers.api.GolemUpgrade;
import dev.bettercoppers.api.BetterCoppersApi;
import dev.bettercoppers.mixin.CopperGolemAccessor;
import dev.bettercoppers.golem.Errands;
import dev.bettercoppers.registry.ModAttachments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.level.block.WeatheringCopper;

/**
 * The running costs of a working golem (miner, farmer, forester, fisher, animal keeper, crafter). Golems that
 * only sort, with the mod's bundle or like vanilla, cost nothing.
 * <ul>
 * <li><b>Energy:</b> every bit of work uses some. At night, or when empty, the golem recharges standing on a
 * Charging Pad driven by a redstone clock (see {@link ChargeBehavior}). Without energy it does not work.</li>
 * <li><b>Rust:</b> work wears the golem down; every {@link #ENERGY_PER_RUST_STAGE} energy spent it rusts one stage. Rusty
 * golems are slower, fully oxidized or waxed golems do not work at all. An axe (by hand or from a dispenser) scrapes
 * them clean again.</li>
 * </ul>
 */
public final class RunningCosts {
	/** Energy spent until the golem rusts one stage: about three days of steady work. */
	public static final int ENERGY_PER_RUST_STAGE = 3000;

	private RunningCosts() {
	}

	public static GolemRunningCosts get(CopperGolem golem) {
		return golem.getAttachedOrCreate(ModAttachments.RUNNING_COSTS, GolemRunningCosts::new);
	}

	public static boolean isWaxed(CopperGolem golem) {
		return ((CopperGolemAccessor) golem).bettercoppers$getNextWeatheringTick() == -2L;
	}

	/** Daytime, not recharging after running empty, not fully oxidized and not waxed, and not off on an errand. */
	public static boolean canWork(ServerLevel level, CopperGolem golem) {
		return !level.isDarkOutside()
			&& !Errands.isBusy(golem)
			&& !get(golem).recharging()
			&& golem.getWeatherState() != WeatheringCopper.WeatherState.OXIDIZED
			&& !isWaxed(golem);
	}

	/** How fast a golem works and walks; rust makes it slower. */
	public static float speed(CopperGolem golem) {
		float speed = switch (golem.getWeatherState()) {
			case UNAFFECTED -> 1.0F;
			case EXPOSED -> 0.85F;
			case WEATHERED -> 0.7F;
			case OXIDIZED -> 0.5F;
		};
		for (GolemUpgrade upgrade : BetterCoppersApi.upgrades()) {
			speed *= upgrade.speed(golem);
		}
		return speed;
	}

	/** Uses up energy for a piece of work and lets the golem rust a bit. */
	public static void spend(CopperGolem golem, int energy) {
		GolemRunningCosts runningCosts = get(golem);
		runningCosts.setEnergy(runningCosts.energy() - energy);
		boolean rustProof = false;
		for (GolemUpgrade upgrade : BetterCoppersApi.upgrades()) {
			rustProof |= upgrade.preventsRust(golem);
			upgrade.onSpend(golem, energy);
		}
		if (rustProof) {
			return;
		}
		runningCosts.setRustProgress(runningCosts.rustProgress() + energy);
		if (runningCosts.rustProgress() >= ENERGY_PER_RUST_STAGE) {
			runningCosts.setRustProgress(0);
			WeatheringCopper.WeatherState state = golem.getWeatherState();
			if (state != WeatheringCopper.WeatherState.OXIDIZED) {
				golem.setWeatherState(state.next());
			}
		}
	}

	/**
	 * What an axe does to a copper golem: unwax it, or else scrape off one stage of rust. Returns false if there was
	 * nothing to do.
	 */
	public static boolean scrape(ServerLevel level, CopperGolem golem) {
		CopperGolemAccessor accessor = (CopperGolemAccessor) golem;
		if (isWaxed(golem)) {
			accessor.bettercoppers$setNextWeatheringTick(-1L);
			level.playSound(null, golem, SoundEvents.AXE_SCRAPE, golem.getSoundSource(), 1.0F, 1.0F);
			level.levelEvent(golem, 3004, golem.blockPosition(), 0);
			return true;
		}
		WeatheringCopper.WeatherState state = golem.getWeatherState();
		if (state == WeatheringCopper.WeatherState.UNAFFECTED) {
			return false;
		}
		accessor.bettercoppers$setNextWeatheringTick(-1L);
		golem.setWeatherState(state.previous());
		get(golem).setRustProgress(0);
		level.playSound(null, golem, SoundEvents.AXE_SCRAPE, golem.getSoundSource(), 1.0F, 1.0F);
		level.levelEvent(golem, 3005, golem.blockPosition(), 0);
		return true;
	}

	public static void playChargeSound(ServerLevel level, CopperGolem golem) {
		level.playSound(null, golem.blockPosition(), SoundEvents.COPPER_GOLEM_SPAWN, SoundSource.NEUTRAL, 0.3F, 1.6F);
	}
}

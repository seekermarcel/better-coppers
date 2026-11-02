package dev.bettercoppers.upkeep;

import dev.bettercoppers.api.GolemJob;
import dev.bettercoppers.api.GolemModifier;
import dev.bettercoppers.api.BetterCoppersApi;
import dev.bettercoppers.courier.CourierRoute;
import dev.bettercoppers.courier.Couriers;
import dev.bettercoppers.golem.Eyes;
import dev.bettercoppers.golem.Sorter;
import dev.bettercoppers.home.Homes;
import dev.bettercoppers.job.Job;
import dev.bettercoppers.job.Tools;
import dev.bettercoppers.job.WorkerData;
import dev.bettercoppers.job.WorkerInteractions;
import dev.bettercoppers.mining.MinerData;
import dev.bettercoppers.mining.MinerInteractions;
import dev.bettercoppers.mixin.CopperGolemAccessor;
import dev.bettercoppers.registry.ModAttachments;
import dev.bettercoppers.golem.Errands;
import dev.bettercoppers.repair.RepairBehavior;
import dev.bettercoppers.trim.GolemTrims;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.WeatheringCopper;

/**
 * The running costs of a working golem (miner, farmer, forester, fisher, animal keeper, crafter). Golems that
 * only sort, with the mod's bundle or like vanilla, cost nothing.
 * <ul>
 * <li><b>Energy:</b> every bit of work uses some. At night, or when empty, the golem recharges standing on a
 * Charging Pad driven by a redstone clock (see {@link ChargeBehavior}). Without energy it does not work.</li>
 * <li><b>Rust:</b> work wears the golem down; every {@link #WEAR_PER_STAGE} energy spent it rusts one stage. Rusty
 * golems are slower, fully oxidized or waxed golems do not work at all. An axe (by hand or from a dispenser) scrapes
 * them clean again.</li>
 * </ul>
 */
public final class Upkeep {
	/** Energy spent until the golem rusts one stage: about three days of steady work. */
	public static final int WEAR_PER_STAGE = 3000;

	private Upkeep() {
	}

	public static GolemUpkeep get(CopperGolem golem) {
		return golem.getAttachedOrCreate(ModAttachments.UPKEEP, GolemUpkeep::new);
	}

	/** Golems a player gave a job; only these have running costs. */
	public static boolean isJobGolem(CopperGolem golem) {
		return MinerData.isMiner(golem) || WorkerData.isWorker(golem) || BetterCoppersApi.jobOf(golem) != null;
	}

	public static boolean isWaxed(CopperGolem golem) {
		return ((CopperGolemAccessor) golem).bettercoppers$getNextWeatheringTick() == -2L;
	}

	/** Daytime, some energy left, not fully oxidized and not waxed, and not off on an errand. */
	public static boolean canWork(ServerLevel level, CopperGolem golem) {
		return !level.isDarkOutside()
			&& !Errands.isBusy(golem)
			&& get(golem).energy() > 0
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
		for (GolemModifier modifier : BetterCoppersApi.modifiers()) {
			speed *= modifier.speed(golem);
		}
		return speed;
	}

	/** Uses up energy for a piece of work and lets the golem rust a bit. */
	public static void spend(CopperGolem golem, int energy) {
		GolemUpkeep upkeep = get(golem);
		upkeep.setEnergy(upkeep.energy() - energy);
		boolean rustProof = false;
		for (GolemModifier modifier : BetterCoppersApi.modifiers()) {
			rustProof |= modifier.preventsRust(golem);
			modifier.onSpend(golem, energy);
		}
		if (rustProof) {
			return;
		}
		upkeep.setWear(upkeep.wear() + energy);
		if (upkeep.wear() >= WEAR_PER_STAGE) {
			upkeep.setWear(0);
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
		get(golem).setWear(0);
		level.playSound(null, golem, SoundEvents.AXE_SCRAPE, golem.getSoundSource(), 1.0F, 1.0F);
		level.levelEvent(golem, 3005, golem.blockPosition(), 0);
		return true;
	}

	/** Where the golem's work is centred: its home chest, or wherever it stands. */
	public static BlockPos homeOf(CopperGolem golem) {
		BlockPos home = Homes.get(golem);
		if (home != null) {
			return home;
		}
		GolemJob addonJob = BetterCoppersApi.jobOf(golem);
		BlockPos workplace = addonJob == null ? null : addonJob.workplace(golem);
		return workplace != null ? workplace : golem.blockPosition();
	}

	/** Any job at all, sorting included. */
	public static boolean hasJob(CopperGolem golem) {
		return Sorter.isSorter(golem) || Couriers.isCourier(golem) || isJobGolem(golem);
	}

	/** Job, home, energy and rust, for the status message. */
	public static Component status(ServerLevel level, CopperGolem golem) {
		Component job = jobName(golem);
		BlockPos home = Homes.get(golem);
		CourierRoute route = Couriers.route(golem);
		GolemJob addonJob = BetterCoppersApi.jobOf(golem);
		BlockPos workplace = addonJob == null ? null : addonJob.workplace(golem);
		Component homeText = route != null
			? Component.translatable("status.bettercoppers.route", route.from().toShortString(), route.to().toShortString())
			: workplace != null
				? Component.translatable("status.bettercoppers.workplace", workplace.getX(), workplace.getY(), workplace.getZ())
			: home == null
				? Component.translatable("status.bettercoppers.no_home")
				: Component.translatable("status.bettercoppers.home", home.getX(), home.getY(), home.getZ());
		if (!isJobGolem(golem)) {
			return Component.translatable("message.bettercoppers.status_free", job, homeText);
		}
		int percent = get(golem).energy() * 100 / GolemUpkeep.MAX_ENERGY;
		String rust = isWaxed(golem) ? "waxed" : golem.getWeatherState().getSerializedName();
		MutableComponent status = Component.translatable(
			"message.bettercoppers.status", job, homeText, percent, Component.translatable("rust.bettercoppers." + rust)
		);
		Item lost = Tools.lostTool(golem);
		if (lost != null) {
			status.append(", ").append(Component.translatable("status.bettercoppers.lost_tool", new ItemStack(lost).getHoverName()));
		}
		if (Eyes.tier(golem) > 0) {
			status.append(", ").append(Component.translatable("status.bettercoppers.eyes", Math.round(Eyes.multiplier(golem) * 100)));
		}
		for (GolemModifier modifier : BetterCoppersApi.modifiers()) {
			Component extra = modifier.status(golem);
			if (extra != null) {
				status.append(", ").append(extra);
			}
		}
		return status;
	}

	private static Component jobName(CopperGolem golem) {
		GolemJob addonJob = BetterCoppersApi.jobOf(golem);
		return addonJob != null ? addonJob.name() : Component.translatable("job.bettercoppers." + jobId(golem));
	}

	private static String jobId(CopperGolem golem) {
		if (MinerData.isMiner(golem)) {
			return "miner";
		}
		Job job = WorkerData.job(golem);
		if (job != null) {
			return job.id();
		}
		if (Couriers.isCourier(golem)) {
			return "courier";
		}
		return Sorter.isSorter(golem) ? "sorter" : "none";
	}

	public static void playChargeSound(ServerLevel level, CopperGolem golem) {
		level.playSound(null, golem.blockPosition(), SoundEvents.COPPER_GOLEM_SPAWN, SoundSource.NEUTRAL, 0.3F, 1.6F);
	}

	/** Drops everything a working golem carries for its job (on death or when turning into a statue). */
	public static void dropJobItems(CopperGolem golem) {
		MinerInteractions.dropAll(golem);
		WorkerInteractions.dropAll(golem);
		if (golem.level() instanceof ServerLevel level) {
			Eyes.drop(level, golem);
			RepairBehavior.dropCarried(level, golem);
			GolemTrims.dropAll(level, golem);
			for (GolemJob job : BetterCoppersApi.jobs()) {
				job.dropItems(level, golem);
			}
			for (GolemModifier modifier : BetterCoppersApi.modifiers()) {
				modifier.dropItems(level, golem);
			}
		}
	}
}

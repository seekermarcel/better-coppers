package dev.bettercoppers.job;

import dev.bettercoppers.api.BetterCoppersApi;
import dev.bettercoppers.api.GolemJob;
import dev.bettercoppers.api.GolemModifier;
import dev.bettercoppers.courier.Couriers;
import dev.bettercoppers.golem.Eyes;
import dev.bettercoppers.golem.Sorter;
import dev.bettercoppers.home.Homes;
import dev.bettercoppers.mining.MinerData;
import dev.bettercoppers.mining.MinerInteractions;
import dev.bettercoppers.registry.ModAttachments;
import dev.bettercoppers.repair.RepairBehavior;
import dev.bettercoppers.trim.GolemTrims;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/** What a golem's job is and where it belongs. Nothing here is saved: the answer comes from what it holds and its data. */
public final class Jobs {
	public enum JobKind { COURIER, ADDON, MINER, WORKER, SORTER, NONE }

	private Jobs() {
	}

	/**
	 * The golem's job, first match wins: a route (anything in hand is cargo), an add-on job, a job tool in hand or a lost
	 * one backed by the data a player's act created (ADR 0006), a bundle. Called for every golem, so it must not search
	 * for entities.
	 */
	public static JobKind of(CopperGolem golem) {
		if (Couriers.isCourier(golem)) {
			return JobKind.COURIER;
		}
		if (BetterCoppersApi.jobOf(golem) != null) {
			return JobKind.ADDON;
		}
		if (MinerData.get(golem) != null && (MinerData.hasPickaxe(golem) || Tools.hasLost(golem, Items.COPPER_PICKAXE))) {
			return JobKind.MINER;
		}
		if (WorkerData.job(golem) != null) {
			return JobKind.WORKER;
		}
		return Sorter.isSorter(golem) ? JobKind.SORTER : JobKind.NONE;
	}

	/** Any job at all, sorting included. Without one a golem must be exactly vanilla (ADR 0002). */
	public static boolean has(CopperGolem golem) {
		return of(golem) != JobKind.NONE;
	}

	/** A job with running costs, so not sorter or courier. */
	public static boolean isPaying(CopperGolem golem) {
		JobKind kind = of(golem);
		return kind == JobKind.MINER || kind == JobKind.WORKER || kind == JobKind.ADDON;
	}

	/** The job's name for the status message. */
	public static Component name(CopperGolem golem) {
		JobKind kind = of(golem);
		if (kind == JobKind.ADDON) {
			return BetterCoppersApi.jobOf(golem).name();
		}
		Job job = kind == JobKind.WORKER ? WorkerData.job(golem) : null;
		String id = switch (kind) {
			case COURIER -> "courier";
			case MINER -> "miner";
			case SORTER -> "sorter";
			default -> job != null ? job.id() : "none";
		};
		return Component.translatable("job.bettercoppers." + id);
	}

	/** Where the golem belongs: its add-on job's workplace, else its home, which stays linked underneath, else null. */
	public static @Nullable BlockPos place(CopperGolem golem) {
		GolemJob job = BetterCoppersApi.jobOf(golem);
		BlockPos workplace = job == null ? null : job.workplace(golem);
		return workplace != null ? workplace : Homes.get(golem);
	}

	/** Drops everything a working golem carries for its job (on death or when turning into a statue). */
	public static void dropJobItems(CopperGolem golem) {
		MinerInteractions.dropAll(golem);
		golem.removeAttached(ModAttachments.WORKER);
		if (golem.level() instanceof ServerLevel level) {
			Load.of(golem).dropAll(level, golem);
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

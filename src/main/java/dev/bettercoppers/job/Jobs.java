package dev.bettercoppers.job;

import dev.bettercoppers.api.BetterCoppersApi;
import dev.bettercoppers.api.GolemJob;
import dev.bettercoppers.courier.Couriers;
import dev.bettercoppers.golem.Sorter;
import dev.bettercoppers.home.Homes;
import dev.bettercoppers.mining.MinerData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
	 * one backed by the data a player's act created (ADR 0006), a bundle. Called for every golem, so an untouched golem
	 * must not get as far as {@link Load#of}, which searches for a cart.
	 */
	public static JobKind of(CopperGolem golem) {
		if (Couriers.isCourier(golem)) {
			return JobKind.COURIER;
		}
		if (BetterCoppersApi.jobOf(golem) != null) {
			return JobKind.ADDON;
		}
		boolean miner = MinerData.get(golem) != null;
		if (miner && (MinerData.hasPickaxe(golem) || Tools.hasLost(golem, Items.COPPER_PICKAXE))) {
			return JobKind.MINER;
		}
		if (WorkerData.job(golem) != null) {
			return JobKind.WORKER;
		}
		// Tool taken with a load left to bring home. Ending a job at once will make this impossible; then this step goes.
		if ((miner || WorkerData.get(golem) != null) && !Load.of(golem).hasNothingToBringHome()) {
			return miner ? JobKind.MINER : JobKind.WORKER;
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

	/**
	 * Where the golem belongs: its add-on job's workplace, else its home, which stays linked underneath. Null if neither,
	 * and for a courier, which belongs on its route.
	 */
	public static @Nullable BlockPos place(CopperGolem golem) {
		if (Couriers.isCourier(golem)) {
			return null;
		}
		GolemJob job = BetterCoppersApi.jobOf(golem);
		BlockPos workplace = job == null ? null : job.workplace(golem);
		return workplace != null ? workplace : Homes.get(golem);
	}
}

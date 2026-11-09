package dev.bettercoppers.golem;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import dev.bettercoppers.api.BetterCoppersApi;
import dev.bettercoppers.courier.CourierBehavior;
import dev.bettercoppers.job.CraftBehavior;
import dev.bettercoppers.job.FarmBehavior;
import dev.bettercoppers.job.FishBehavior;
import dev.bettercoppers.job.ForestBehavior;
import dev.bettercoppers.job.BeekeeperBehavior;
import dev.bettercoppers.job.KeeperBehavior;
import dev.bettercoppers.job.ToolFetchBehavior;
import dev.bettercoppers.mining.MineBehavior;
import dev.bettercoppers.repair.RepairBehavior;
import dev.bettercoppers.runningcosts.ChargeBehavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.animal.golem.CopperGolem;

public final class GolemBrain {
	private GolemBrain() {
	}

	/** The mod's idle behaviours, add-ons' included, put in front of vanilla's (see {@code CopperGolemAiMixin}). */
	public static ImmutableList<Pair<Integer, BehaviorControl<? super CopperGolem>>> modBehaviours() {
		ImmutableList.Builder<Pair<Integer, BehaviorControl<? super CopperGolem>>> behaviours = ImmutableList.builder();
		behaviours.add(
			Pair.of(0, new RepairBehavior()),
			Pair.of(0, new SortItemsBehavior()),
			Pair.of(0, new CourierBehavior()),
			Pair.of(0, new MineBehavior()),
			Pair.of(0, new ChargeBehavior()),
			Pair.of(0, new FarmBehavior()),
			Pair.of(0, new ForestBehavior()),
			Pair.of(0, new FishBehavior()),
			Pair.of(0, new KeeperBehavior()),
			Pair.of(0, new BeekeeperBehavior()),
			Pair.of(0, new CraftBehavior()),
			Pair.of(0, new ToolFetchBehavior())
		);
		BetterCoppersApi.behaviors().forEach(factory -> behaviours.add(Pair.of(0, factory.get())));
		// Last, so it sees the walk target the job behaviours just set.
		behaviours.add(Pair.of(0, new RideBehavior()));
		// After every job, and before vanilla's stroll, which it keeps from starting by setting a walk target.
		behaviours.add(Pair.of(1, new HomeStroll()));
		return behaviours.build();
	}
}

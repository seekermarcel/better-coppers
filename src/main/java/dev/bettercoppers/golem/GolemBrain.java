package dev.bettercoppers.golem;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import dev.bettercoppers.courier.CourierBehavior;
import dev.bettercoppers.job.CraftBehavior;
import dev.bettercoppers.job.FarmBehavior;
import dev.bettercoppers.job.FishBehavior;
import dev.bettercoppers.job.ForestBehavior;
import dev.bettercoppers.job.KeeperBehavior;
import dev.bettercoppers.mining.MineBehavior;
import dev.bettercoppers.repair.RepairBehavior;
import dev.bettercoppers.upkeep.ChargeBehavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.animal.golem.CopperGolem;

public final class GolemBrain {
	private GolemBrain() {
	}

	/** The mod's idle behaviours, put in front of vanilla's (see {@code CopperGolemAiMixin}). */
	public static ImmutableList<Pair<Integer, BehaviorControl<? super CopperGolem>>> modBehaviours() {
		return ImmutableList.of(
			Pair.of(0, new RepairBehavior()),
			Pair.of(0, new SortItemsBehavior()),
			Pair.of(0, new CourierBehavior()),
			Pair.of(0, new MineBehavior()),
			Pair.of(0, new ChargeBehavior()),
			Pair.of(0, new FarmBehavior()),
			Pair.of(0, new ForestBehavior()),
			Pair.of(0, new FishBehavior()),
			Pair.of(0, new KeeperBehavior()),
			Pair.of(0, new CraftBehavior()),
			// Last, so it sees the walk target the job behaviours just set.
			Pair.of(0, new RideBehavior())
		);
	}
}

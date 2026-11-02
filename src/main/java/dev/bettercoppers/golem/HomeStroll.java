package dev.bettercoppers.golem;

import com.google.common.collect.ImmutableMap;
import dev.bettercoppers.courier.Couriers;
import dev.bettercoppers.job.Jobs;
import dev.bettercoppers.upkeep.ChargeBehavior;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.phys.Vec3;

/**
 * Keeps idle golems near home: vanilla's random stroll has no anchor, so a golem with nothing to do drifts away from
 * its chest. Once it is more than {@link #RADIUS} blocks out, this walks it back. Inside that it strolls like vanilla,
 * and a golem no player linked to anything never starts this at all.
 */
public class HomeStroll extends Behavior<CopperGolem> {
	private static final int RADIUS = 6;
	private static final float SPEED = 1.0F;

	public HomeStroll() {
		// The same memories as vanilla's stroll: only between trips, never in the middle of one.
		super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS, MemoryStatus.VALUE_PRESENT));
	}

	@Override
	protected boolean checkExtraStartConditions(ServerLevel level, CopperGolem golem) {
		BlockPos place = Jobs.place(golem);
		return place != null
			&& !golem.isLeashed()
			&& !golem.isPassenger()
			&& !Errands.isBusy(golem)
			// A courier belongs on its route, and a charging golem on its pad, however far from home those are.
			&& !Couriers.isCourier(golem)
			&& !ChargeBehavior.isPad(level.getBlockState(golem.blockPosition().below()))
			&& isTooFar(golem.position(), Vec3.atBottomCenterOf(place));
	}

	@Override
	protected void start(ServerLevel level, CopperGolem golem, long timestamp) {
		Vec3 home = Vec3.atBottomCenterOf(Jobs.place(golem));
		Vec3 back = LandRandomPos.getPosTowards(golem, RADIUS, 3, home);
		golem.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(back != null ? back : home, SPEED, 0));
	}

	private static boolean isTooFar(Vec3 a, Vec3 b) {
		double dx = a.x - b.x;
		double dz = a.z - b.z;
		return dx * dx + dz * dz > RADIUS * RADIUS;
	}
}

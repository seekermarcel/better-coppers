package dev.bettercoppers.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.CopperGolemState;
import net.minecraft.world.phys.AABB;

/** Movement helpers and numbers shared by the mod's golem behaviours. */
public final class Golems {
	public static final float WALK_SPEED = 0.8F;
	public static final float HESITATE_CHANCE = 0.08F;
	/** Items carried before going home. */
	public static final int POUCH_LIMIT = 64;
	/** Seeds of each kind kept for replanting. */
	public static final int SEED_RESERVE = 16;
	/** Items taken in hand per trip. */
	public static final int HAND_LIMIT = 16;
	/** Squared distance at which a golem reaches a moving target. */
	public static final double REACH_SQR = 2.5 * 2.5;
	/** How long a golem stays at an open chest. */
	public static final int INTERACTION_TICKS = 60;
	/** Tick of a chest visit on which the golem's sound plays. */
	public static final int SOUND_TICK = 9;
	public static final int MAX_TRAVEL_TICKS = 300;
	/** How long an unreachable spot is skipped. */
	public static final int IGNORE_TICKS = 600;

	private Golems() {
	}

	public static boolean isInReach(CopperGolem golem, BlockPos pos) {
		return new AABB(pos).inflate(1.0, 1.5, 1.0).intersects(golem.getBoundingBox());
	}

	public static void stopInPlace(CopperGolem golem) {
		golem.getNavigation().stop();
		golem.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		golem.setXxa(0.0F);
		golem.setZza(0.0F);
		golem.setSpeed(0.0F);
		golem.setDeltaMovement(0.0, golem.getDeltaMovement().y, 0.0);
	}

	/** Rolls for a head shake; if it happens, starts it and returns how long to pause, else 0. */
	public static int hesitate(CopperGolem golem) {
		if (golem.getRandom().nextFloat() >= HESITATE_CHANCE) {
			return 0;
		}
		golem.setState(CopperGolemState.GETTING_NO_ITEM);
		golem.playSound(SoundEvents.COPPER_GOLEM_ITEM_NO_GET);
		return 30 + golem.getRandom().nextInt(30);
	}
}

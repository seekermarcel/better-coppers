package dev.bettercoppers.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.CopperGolemState;
import org.jspecify.annotations.Nullable;

/**
 * The stop at a chest: the golem stands still, looks at it, opens it, plays its animation and sound, and after the wait
 * the caller does its business and calls {@link #close}. The caller keeps the tick count, so it can also reset it.
 */
public final class ChestVisit {
	private ChestVisit() {
	}

	/** One tick of a visit; returns the new tick count. The animation is only read on the first tick. */
	public static int tick(CopperGolem golem, Container chest, BlockPos pos, int ticks, CopperGolemState animation) {
		Golems.stopInPlace(golem);
		golem.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(pos));
		ticks++;
		if (ticks == 1) {
			chest.startOpen(golem);
			golem.setOpenedChestPos(pos);
			golem.setState(animation);
		}
		if (ticks == Golems.SOUND_TICK) {
			SoundEvent sound = switch (golem.getState()) {
				case GETTING_ITEM -> SoundEvents.COPPER_GOLEM_ITEM_GET;
				case GETTING_NO_ITEM -> SoundEvents.COPPER_GOLEM_ITEM_NO_GET;
				case DROPPING_ITEM -> SoundEvents.COPPER_GOLEM_ITEM_DROP;
				case DROPPING_NO_ITEM -> SoundEvents.COPPER_GOLEM_ITEM_NO_DROP;
				case IDLE -> null;
			};
			if (sound != null) {
				golem.playSound(sound);
			}
		}
		return ticks;
	}

	/** Closes the chest if this golem has it open, and goes back to idle. */
	public static void close(CopperGolem golem, @Nullable Container chest) {
		if (chest != null && chest.getEntitiesWithContainerOpen().contains(golem)) {
			chest.stopOpen(golem);
		}
		golem.clearOpenedChestPos();
		golem.setState(CopperGolemState.IDLE);
	}
}

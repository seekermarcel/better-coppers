package dev.bettercoppers.mixin;

import dev.bettercoppers.golem.Errands;
import dev.bettercoppers.upkeep.Upkeep;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vanilla item transport is only for golems without a job, and pauses while one is off repairing. */
@Mixin(TransportItemsBetweenContainers.class)
public abstract class TransportItemsBetweenContainersMixin {
	@Inject(method = "checkExtraStartConditions(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/PathfinderMob;)Z", at = @At("HEAD"), cancellable = true)
	private void bettercoppers$skipJobGolems(ServerLevel level, PathfinderMob mob, CallbackInfoReturnable<Boolean> cir) {
		if (isBlocked(mob)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "canStillUse(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/PathfinderMob;J)Z", at = @At("HEAD"), cancellable = true)
	private void bettercoppers$stopJobGolems(ServerLevel level, PathfinderMob mob, long timestamp, CallbackInfoReturnable<Boolean> cir) {
		if (isBlocked(mob)) {
			cir.setReturnValue(false);
		}
	}

	private static boolean isBlocked(PathfinderMob mob) {
		return mob instanceof CopperGolem golem && (Upkeep.hasJob(golem) || Errands.isBusy(golem));
	}
}

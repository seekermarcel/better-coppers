package dev.bettercoppers.mixin;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import dev.bettercoppers.golem.SortItemsBehavior;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.CopperGolemAi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CopperGolemAi.class)
public abstract class CopperGolemAiMixin {
	/** Puts the mod's behaviours in front of vanilla's idle activity, which stays complete for golems without a job. */
	@Inject(method = "initIdleActivity", at = @At("RETURN"), cancellable = true)
	private static void bettercoppers$addBehaviours(CallbackInfoReturnable<ActivityData<CopperGolem>> cir) {
		ActivityData<CopperGolem> vanilla = cir.getReturnValue();
		ImmutableList<Pair<Integer, ? extends BehaviorControl<? super CopperGolem>>> behaviours = ImmutableList.<Pair<Integer, ? extends BehaviorControl<? super CopperGolem>>>builder()
			.addAll(SortItemsBehavior.modBehaviours())
			.addAll(vanilla.behaviorPriorityPairs())
			.build();
		cir.setReturnValue(new ActivityData<>(vanilla.activityType(), behaviours, vanilla.conditions(), vanilla.memoriesToEraseWhenStopped()));
	}
}

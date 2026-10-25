package dev.bettercoppers.combat;

import dev.bettercoppers.mixin.MobAccessor;
import dev.bettercoppers.mixin.NearestAttackableTargetGoalAccessor;
import dev.bettercoppers.upkeep.Upkeep;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.AbstractVillager;

/**
 * Copper golems are prey now: every monster that hunts villagers or iron golems hunts copper golems too. Iron golems
 * in turn stay close to the copper golems that live somewhere and go for whatever attacks them.
 */
public final class GolemGuards {
	private GolemGuards() {
	}

	public static void onEntityLoad(Entity entity, ServerLevel level) {
		if (entity instanceof IronGolem ironGolem) {
			addGuardGoals(ironGolem);
		} else if (entity instanceof Mob mob && !(entity instanceof CopperGolem)) {
			addHuntGoal(mob);
		}
	}

	/** Only golems with a job are worth hunting or guarding; the rest stay vanilla. */
	public static boolean isSettled(CopperGolem golem) {
		return golem.isAlive() && Upkeep.hasJob(golem);
	}

	private static void addHuntGoal(Mob mob) {
		GoalSelector targets = ((MobAccessor) mob).bettercoppers$getTargetSelector();
		int priority = -1;
		for (WrappedGoal wrapped : targets.getAvailableGoals()) {
			if (wrapped.getGoal() instanceof NearestAttackableTargetGoal<?> goal) {
				Class<?> type = ((NearestAttackableTargetGoalAccessor) goal).bettercoppers$getTargetType();
				if (type == CopperGolem.class) {
					// Already added when this mob loaded before.
					return;
				}
				if (IronGolem.class.isAssignableFrom(type) || AbstractVillager.class.isAssignableFrom(type)) {
					priority = priority < 0 ? wrapped.getPriority() : Math.min(priority, wrapped.getPriority());
				}
			}
		}
		if (priority >= 0) {
			targets.addGoal(priority, new NearestAttackableTargetGoal<>(mob, CopperGolem.class, true,
				(target, level) -> target instanceof CopperGolem golem && isSettled(golem)));
		}
	}

	private static void addGuardGoals(IronGolem golem) {
		MobAccessor accessor = (MobAccessor) golem;
		if (accessor.bettercoppers$getGoalSelector().getAvailableGoals().stream().anyMatch(wrapped -> wrapped.getGoal() instanceof GuardCopperGolemsGoal)) {
			return;
		}
		accessor.bettercoppers$getGoalSelector().addGoal(2, new GuardCopperGolemsGoal(golem));
		accessor.bettercoppers$getTargetSelector().addGoal(1, new DefendCopperGolemsTargetGoal(golem));
	}
}

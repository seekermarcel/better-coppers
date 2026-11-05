package dev.bettercoppers.job;

import dev.bettercoppers.entity.GolemFloat;
import dev.bettercoppers.golem.Eyes;
import dev.bettercoppers.golem.Golems;
import dev.bettercoppers.registry.ModEntities;
import dev.bettercoppers.runningcosts.RunningCosts;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.CopperGolemState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Stands on the shore of open water near its home and fishes, patiently and slowly: a catch every half minute or so,
 * from the vanilla fishing loot (fish and junk; golems never land treasure). Brings the catch home every few fish.
 */
public class FishBehavior extends JobBehavior<FishBehavior.Spot> {
	private static final int RADIUS = 10;
	private static final int MIN_OPEN_WATER = 6;
	private static final int MIN_WAIT = 400;
	private static final int EXTRA_WAIT = 600;
	private static final int ENERGY_PER_CATCH = 15;
	/** The float dips under this long before the catch comes in. */
	private static final int BITE_TICKS = 20;

	record Spot(BlockPos stand, BlockPos water) {
	}

	private @Nullable Spot spot;
	private int waitTicks;
	private @Nullable GolemFloat golemFloat;

	public FishBehavior() {
		super(Job.FISHER);
	}

	@Override
	protected int pouchLimit() {
		return 8;
	}

	@Override
	protected @Nullable Spot chooseTask(ServerLevel level, CopperGolem golem, BlockPos home) {
		if (this.spot == null || !isValid(level, this.spot) || this.isIgnored(level, this.spot.stand())) {
			this.spot = this.findSpot(level, home, Eyes.radius(golem, RADIUS));
		}
		return this.spot;
	}

	@Override
	protected boolean tickTask(ServerLevel level, CopperGolem golem, BlockPos home, Spot spot) {
		if (!isValid(level, spot)) {
			this.spot = null;
			this.reelIn();
			return true;
		}
		Vec3 offset = golem.position().subtract(Vec3.atBottomCenterOf(spot.stand()));
		if (this.workTicks == 0 && (offset.horizontalDistanceSqr() > 0.5 || Math.abs(offset.y) > 1.2)) {
			// It wants to stand right on the shore block, not just near it.
			this.reelIn();
			if (this.stalledTowards(golem, Vec3.atBottomCenterOf(spot.stand())) > Golems.MAX_TRAVEL_TICKS) {
				this.ignore(level, spot.stand());
				this.spot = null;
				return true;
			}
			BehaviorUtils.setWalkAndLookTargetMemories(golem, spot.stand(), Golems.WALK_SPEED * RunningCosts.speed(golem), 0);
			return false;
		}
		Golems.stopInPlace(golem);
		golem.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(spot.water()));
		if (this.workTicks == 0) {
			if (this.hesitate(golem)) {
				return false;
			}
			this.waitTicks = slowed(golem, MIN_WAIT + golem.getRandom().nextInt(EXTRA_WAIT));
			this.cast(level, golem, spot);
		}
		this.workTicks++;
		Vec3 floatPos = Vec3.atCenterOf(spot.water()).add(0.0, 0.4, 0.0);
		if (this.golemFloat == null || this.golemFloat.isRemoved()) {
			this.cast(level, golem, spot);
		}
		if (this.golemFloat != null) {
			this.golemFloat.keepAlive();
		}
		if (this.workTicks == 8) {
			// The arm comes back down after the throw.
			golem.setState(CopperGolemState.IDLE);
		}
		if (this.workTicks % 40 == 0) {
			level.sendParticles(ParticleTypes.FISHING, floatPos.x, floatPos.y, floatPos.z, 2, 0.1, 0.0, 0.1, 0.0);
		}
		if (this.workTicks == this.waitTicks - BITE_TICKS) {
			// A bite: the float goes under.
			if (this.golemFloat != null) {
				this.golemFloat.setBiting(true);
			}
			level.sendParticles(ParticleTypes.BUBBLE, floatPos.x, floatPos.y - 0.2, floatPos.z, 6, 0.1, 0.1, 0.1, 0.0);
			level.playSound(null, floatPos.x, floatPos.y, floatPos.z, SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.25F, 1.4F);
		}
		if (this.workTicks < this.waitTicks) {
			return false;
		}
		this.landCatch(level, golem, floatPos);
		this.reelIn();
		return true;
	}

	/** Throws the float out onto the water. */
	private void cast(ServerLevel level, CopperGolem golem, Spot spot) {
		this.reelIn();
		GolemFloat golemFloat = ModEntities.GOLEM_FLOAT.create(level, EntitySpawnReason.TRIGGERED);
		if (golemFloat == null) {
			return;
		}
		golemFloat.snapTo(spot.water().getX() + 0.5, spot.water().getY() + 0.7, spot.water().getZ() + 0.5, 0.0F, 0.0F);
		golemFloat.setOwner(golem);
		golemFloat.keepAlive();
		level.addFreshEntity(golemFloat);
		this.golemFloat = golemFloat;
		golem.setState(CopperGolemState.DROPPING_ITEM);
		golem.playSound(SoundEvents.FISHING_BOBBER_THROW, 0.5F, 0.6F);
	}

	private void reelIn() {
		if (this.golemFloat != null) {
			this.golemFloat.discard();
			this.golemFloat = null;
		}
	}

	@Override
	protected void stop(ServerLevel level, CopperGolem golem, long timestamp) {
		this.reelIn();
		super.stop(level, golem, timestamp);
	}

	private void landCatch(ServerLevel level, CopperGolem golem, Vec3 floatPos) {
		ItemStack rod = golem.getMainHandItem();
		LootParams params = new LootParams.Builder(level)
			.withParameter(LootContextParams.ORIGIN, floatPos)
			.withParameter(LootContextParams.TOOL, rod)
			.withParameter(LootContextParams.THIS_ENTITY, golem)
			.create(LootContextParamSets.FISHING);
		LootTable table = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);
		List<ItemStack> catches = table.getRandomItems(params);
		catches.forEach(Load.of(golem)::stash);
		level.sendParticles(ParticleTypes.SPLASH, floatPos.x, floatPos.y, floatPos.z, 12, 0.2, 0.1, 0.2, 0.0);
		level.playSound(null, floatPos.x, floatPos.y, floatPos.z, SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.5F, 1.0F);
		golem.setState(CopperGolemState.GETTING_ITEM);
		Tools.wear(golem, rod);
		RunningCosts.spend(golem, ENERGY_PER_CATCH);
		this.pause(20);
	}

	/** Open water near home: a water source with lots of water around it, and a dry shore block to stand on. */
	private @Nullable Spot findSpot(ServerLevel level, BlockPos home, int radius) {
		Spot best = null;
		double bestDistance = Double.MAX_VALUE;
		for (BlockPos mutable : BlockPos.betweenClosed(home.offset(-radius, -4, -radius), home.offset(radius, 2, radius))) {
			if (!level.isLoaded(mutable) || !isOpenWater(level, mutable)) {
				continue;
			}
			BlockPos water = mutable.immutable();
			for (Direction direction : Direction.Plane.HORIZONTAL) {
				Spot spot = new Spot(water.relative(direction).above(), water);
				double distance = spot.stand().distSqr(home);
				if (distance < bestDistance && isValid(level, spot) && !this.isIgnored(level, spot.stand())) {
					best = spot;
					bestDistance = distance;
				}
			}
		}
		return best;
	}

	private static boolean isOpenWater(ServerLevel level, BlockPos pos) {
		if (!level.getFluidState(pos).isSourceOfType(Fluids.WATER) || !level.getBlockState(pos.above()).isAir()) {
			return false;
		}
		int water = 0;
		for (BlockPos around : BlockPos.betweenClosed(pos.offset(-2, 0, -2), pos.offset(2, 0, 2))) {
			if (level.getFluidState(around).is(FluidTags.WATER)) {
				water++;
			}
		}
		return water >= MIN_OPEN_WATER;
	}

	/** The golem stands on a solid shore block next to the water, with room above it. */
	private static boolean isValid(ServerLevel level, Spot spot) {
		BlockPos stand = spot.stand();
		return level.isLoaded(stand)
			&& level.getFluidState(spot.water()).is(FluidTags.WATER)
			&& level.getBlockState(stand.below()).isFaceSturdy(level, stand.below(), Direction.UP)
			&& level.getBlockState(stand).getCollisionShape(level, stand).isEmpty()
			&& level.getFluidState(stand).isEmpty()
			&& level.getBlockState(stand.above()).getCollisionShape(level, stand.above()).isEmpty();
	}
}

package dev.bettercoppers.repair;

import com.google.common.collect.ImmutableMap;
import dev.bettercoppers.courier.CourierRoute;
import dev.bettercoppers.courier.Couriers;
import dev.bettercoppers.golem.Errands;
import dev.bettercoppers.golem.ChestVisit;
import dev.bettercoppers.golem.Golems;
import dev.bettercoppers.golem.Inventories;
import dev.bettercoppers.api.TravelWatch;
import dev.bettercoppers.guide.Guide;
import dev.bettercoppers.home.Homes;
import dev.bettercoppers.job.Jobs;
import dev.bettercoppers.registry.ModAttachments;
import dev.bettercoppers.upkeep.Upkeep;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.CopperGolemState;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Any copper golem with a home (or a courier route) looks after the iron golems around it: when one is badly hurt, it
 * drops what it is doing, takes iron ingots from its home chest (or the nearest chest or barrel that has some) and
 * patches the iron golem up, 25 health per ingot, like a player would. A golem with only a home also uses iron ingots
 * already in its hand. Only one copper golem tends each iron golem.
 */
public class RepairBehavior extends Behavior<CopperGolem> {
	private static final double PATIENT_RANGE = 16.0;
	private static final int CHEST_RANGE = 16;
	private static final float HEAL_PER_INGOT = 25.0F;
	/** Repairs start once an iron golem is down a whole ingot's worth (and a bit). */
	private static final float HURT_THRESHOLD = 30.0F;
	private static final int MAX_INGOTS_PER_TRIP = 4;
	private static final int SEARCH_INTERVAL = 40;
	private static final int CHEST_TICKS = 30;
	private static final int REPAIR_TICKS = 25;

	/** Which copper golem is looking after which iron golem. */
	private static final Map<UUID, UUID> CLAIMS = new HashMap<>();

	private @Nullable IronGolem patient;
	private @Nullable BlockPos supply;
	private int ticks;
	private final TravelWatch travel = new TravelWatch();

	public RepairBehavior() {
		super(ImmutableMap.of(MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_ABSENT));
	}

	@Override
	protected boolean checkExtraStartConditions(ServerLevel level, CopperGolem golem) {
		if ((level.getGameTime() + golem.getId()) % SEARCH_INTERVAL != 0 || golem.isLeashed() || golem.isPassenger() || !hasPlace(golem)) {
			return false;
		}
		// A sorter or courier finishes its delivery first; the hand of a job golem only holds its tool.
		if (!golem.getMainHandItem().isEmpty() && !Jobs.isPaying(golem) && !holdsIron(golem)) {
			return false;
		}
		IronGolem found = findPatient(level, golem);
		if (found == null) {
			return false;
		}
		boolean supplied = carried(golem) > 0 || holdsIron(golem);
		BlockPos chest = supplied ? null : findSupply(level, golem);
		if (!supplied && chest == null) {
			return false;
		}
		this.patient = found;
		this.supply = chest;
		return true;
	}

	@Override
	protected boolean canStillUse(ServerLevel level, CopperGolem golem, long timestamp) {
		return this.patient != null && !golem.isPanicking() && !golem.isLeashed() && hasPlace(golem) && needsRepair(this.patient);
	}

	@Override
	protected boolean timedOut(long timestamp) {
		return false;
	}

	@Override
	protected void start(ServerLevel level, CopperGolem golem, long timestamp) {
		Errands.setBusy(golem, true);
		CLAIMS.put(this.patient.getUUID(), golem.getUUID());
		this.ticks = 0;
		this.travel.reset();
		golem.getBrain().eraseMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS);
		if (holdsIron(golem)) {
			ItemStack hand = golem.getMainHandItem();
			int moved = Math.min(hand.getCount(), wanted(this.patient));
			hand.shrink(moved);
			setCarried(golem, carried(golem) + moved);
		}
	}

	@Override
	protected void stop(ServerLevel level, CopperGolem golem, long timestamp) {
		Errands.setBusy(golem, false);
		if (this.patient != null) {
			CLAIMS.remove(this.patient.getUUID(), golem.getUUID());
		}
		this.closeChest(level, golem);
		this.patient = null;
		this.supply = null;
		if (!Jobs.has(golem)) {
			this.returnLeftovers(level, golem);
		}
		golem.setState(CopperGolemState.IDLE);
		golem.getNavigation().stop();
		golem.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
	}

	@Override
	protected void tick(ServerLevel level, CopperGolem golem, long timestamp) {
		IronGolem target = this.patient;
		if (target == null) {
			return;
		}
		if (carried(golem) == 0) {
			this.tickFetch(level, golem, target);
		} else {
			this.tickRepair(level, golem, target);
		}
	}

	// --- fetching iron ---

	private void tickFetch(ServerLevel level, CopperGolem golem, IronGolem target) {
		if (this.supply == null) {
			this.supply = findSupply(level, golem);
			if (this.supply == null) {
				this.patient = null;
				return;
			}
		}
		Container chest = Homes.containerAt(level, this.supply);
		if (chest == null) {
			this.supply = null;
			return;
		}
		if (!Golems.isInReach(golem, this.supply)) {
			this.walkTo(golem, Vec3.atCenterOf(this.supply), this.supply);
			return;
		}
		this.ticks = ChestVisit.tick(golem, chest, this.supply, this.ticks, CopperGolemState.GETTING_ITEM);
		if (this.ticks < CHEST_TICKS) {
			return;
		}
		int wanted = wanted(target);
		int taken = 0;
		for (int slot = 0; slot < chest.getContainerSize() && taken < wanted; slot++) {
			ItemStack stack = chest.getItem(slot);
			if (stack.is(Items.IRON_INGOT)) {
				int amount = Math.min(stack.getCount(), wanted - taken);
				stack.shrink(amount);
				taken += amount;
			}
		}
		chest.setChanged();
		this.closeChest(level, golem);
		setCarried(golem, taken);
		this.ticks = 0;
		this.travel.reset();
		if (taken == 0) {
			this.supply = null;
		}
	}

	/** Rounded, matching when repairs stop (half an ingot's worth of damage left), so nothing is left over. */
	private static int wanted(IronGolem target) {
		return Mth.clamp(Math.round((target.getMaxHealth() - target.getHealth()) / HEAL_PER_INGOT), 1, MAX_INGOTS_PER_TRIP);
	}

	/** A golem with only a home counts iron ingots in its hand (vanilla may have lifted them from its chest) as supply. */
	private static boolean holdsIron(CopperGolem golem) {
		return golem.getMainHandItem().is(Items.IRON_INGOT) && !Jobs.has(golem) && Homes.get(golem) != null;
	}

	/** Iron a jobless golem has left over goes back to its empty hand, else to the home chest, so it is not stranded. */
	private void returnLeftovers(ServerLevel level, CopperGolem golem) {
		int count = carried(golem);
		if (count == 0) {
			return;
		}
		ItemStack iron = new ItemStack(Items.IRON_INGOT, count);
		if (golem.getMainHandItem().isEmpty()) {
			golem.setItemSlot(EquipmentSlot.MAINHAND, iron);
			setCarried(golem, 0);
			return;
		}
		BlockPos homePos = Homes.get(golem);
		Container home = homePos == null ? null : Homes.containerAt(level, homePos);
		if (home != null) {
			setCarried(golem, Inventories.insert(home, iron).getCount());
		}
	}

	// --- repairing ---

	private void tickRepair(ServerLevel level, CopperGolem golem, IronGolem target) {
		if (golem.distanceToSqr(target) > Golems.REACH_SQR) {
			this.ticks = 0;
			if (this.travel.tick(golem, target.position()) > Golems.MAX_TRAVEL_TICKS) {
				this.patient = null;
				return;
			}
			BehaviorUtils.setWalkAndLookTargetMemories(golem, target, Upkeep.speed(golem), 1);
			return;
		}
		Golems.stopInPlace(golem);
		golem.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
		this.ticks++;
		if (this.ticks == 1) {
			golem.setState(CopperGolemState.DROPPING_ITEM);
		}
		if (this.ticks < REPAIR_TICKS) {
			return;
		}
		target.heal(HEAL_PER_INGOT);
		Guide.awardNearby(level, target.position(), "repair");
		setCarried(golem, carried(golem) - 1);
		float pitch = 1.0F + (golem.getRandom().nextFloat() - golem.getRandom().nextFloat()) * 0.2F;
		level.playSound(null, target, SoundEvents.IRON_GOLEM_REPAIR, target.getSoundSource(), 1.0F, pitch);
		level.sendParticles(ParticleTypes.WAX_OFF, target.getX(), target.getY() + 1.4, target.getZ(), 8, 0.4, 0.5, 0.4, 0.0);
		golem.setState(CopperGolemState.IDLE);
		this.ticks = 0;
	}

	private void walkTo(CopperGolem golem, Vec3 goal, BlockPos block) {
		if (this.travel.tick(golem, goal) > Golems.MAX_TRAVEL_TICKS) {
			// Cannot get there; leave it to another golem, or to the next try.
			this.patient = null;
			return;
		}
		BehaviorUtils.setWalkAndLookTargetMemories(golem, block, Upkeep.speed(golem), 1);
	}

	private void closeChest(ServerLevel level, CopperGolem golem) {
		ChestVisit.close(golem, this.supply == null ? null : Homes.containerAt(level, this.supply));
	}

	// --- finding things ---

	/** Golems that belong somewhere look after the iron golems there; strays do not. */
	private static boolean hasPlace(CopperGolem golem) {
		return Homes.get(golem) != null || Couriers.isCourier(golem);
	}

	private static boolean needsRepair(IronGolem golem) {
		return golem.isAlive() && golem.getMaxHealth() - golem.getHealth() >= HEAL_PER_INGOT * 0.5F;
	}

	private static @Nullable IronGolem findPatient(ServerLevel level, CopperGolem golem) {
		IronGolem best = null;
		double bestDistance = Double.MAX_VALUE;
		for (IronGolem candidate : level.getEntitiesOfClass(IronGolem.class, golem.getBoundingBox().inflate(PATIENT_RANGE))) {
			if (!candidate.isAlive() || candidate.getMaxHealth() - candidate.getHealth() < HURT_THRESHOLD || isClaimedByOther(level, candidate, golem)) {
				continue;
			}
			double distance = golem.distanceToSqr(candidate);
			if (distance < bestDistance) {
				best = candidate;
				bestDistance = distance;
			}
		}
		return best;
	}

	private static boolean isClaimedByOther(ServerLevel level, IronGolem patient, CopperGolem golem) {
		UUID claimant = CLAIMS.get(patient.getUUID());
		if (claimant == null || claimant.equals(golem.getUUID())) {
			return false;
		}
		if (level.getEntity(claimant) instanceof CopperGolem other && other.isAlive() && Errands.isBusy(other)) {
			return true;
		}
		CLAIMS.remove(patient.getUUID());
		return false;
	}

	/** The home chest (or a courier's pick-up chest) if it has iron, else the nearest chest or barrel nearby that does. */
	private static @Nullable BlockPos findSupply(ServerLevel level, CopperGolem golem) {
		BlockPos home = Homes.get(golem);
		if (hasIron(level, home)) {
			return home;
		}
		CourierRoute route = Couriers.route(golem);
		if (route != null && hasIron(level, route.from())) {
			return route.from();
		}
		AABB area = new AABB(golem.blockPosition()).inflate(CHEST_RANGE, 6, CHEST_RANGE);
		BlockPos[] best = {null};
		double[] bestDistance = {Double.MAX_VALUE};
		Inventories.forEachBlockEntity(level, area, blockEntity -> {
			BlockPos pos = blockEntity.getBlockPos();
			double distance = pos.distToCenterSqr(golem.position());
			if (distance < bestDistance[0] && hasIron(level, pos)) {
				best[0] = pos;
				bestDistance[0] = distance;
			}
		});
		return best[0];
	}

	private static boolean hasIron(ServerLevel level, @Nullable BlockPos pos) {
		if (pos == null) {
			return false;
		}
		Container container = Homes.containerAt(level, pos);
		if (container == null) {
			return false;
		}
		for (ItemStack stack : container) {
			if (stack.is(Items.IRON_INGOT)) {
				return true;
			}
		}
		return false;
	}

	// --- carried iron ---

	public static int carried(CopperGolem golem) {
		Integer count = golem.getAttached(ModAttachments.CARRIED_IRON);
		return count == null ? 0 : count;
	}

	private static void setCarried(CopperGolem golem, int count) {
		if (count <= 0) {
			golem.removeAttached(ModAttachments.CARRIED_IRON);
		} else {
			golem.setAttached(ModAttachments.CARRIED_IRON, count);
		}
	}

	/** Hands the iron a golem was carrying to the player, when its job ends. */
	public static void giveCarried(Player player, CopperGolem golem) {
		int count = carried(golem);
		if (count > 0) {
			Inventories.giveOrDrop(player, new ItemStack(Items.IRON_INGOT, count));
			setCarried(golem, 0);
		}
	}

	/** Drops the iron a golem was carrying (on death or when it turns into a statue). */
	public static void dropCarried(ServerLevel level, CopperGolem golem) {
		int count = carried(golem);
		if (count > 0) {
			golem.spawnAtLocation(level, new ItemStack(Items.IRON_INGOT, count));
			setCarried(golem, 0);
		}
	}
}

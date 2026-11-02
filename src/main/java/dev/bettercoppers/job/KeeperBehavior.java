package dev.bettercoppers.job;

import dev.bettercoppers.golem.CartChaining;
import dev.bettercoppers.golem.Eyes;
import dev.bettercoppers.golem.Inventories;
import dev.bettercoppers.home.Homes;
import dev.bettercoppers.upkeep.Upkeep;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.CopperGolemState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Feeds the animals around its home with food from the home chest so they breed, two at a time, until a herd has
 * {@link #HERD_LIMIT} animals. It takes food along from the chest and comes back for more when it runs out.
 */
public class KeeperBehavior extends JobBehavior<KeeperBehavior.Feeding> {
	private static final int RADIUS = 10;
	private static final int HERD_LIMIT = 16;
	private static final int FOOD_RESERVE = 16;
	private static final int FEED_TICKS = 25;
	private static final int ENERGY_PER_FEED = 10;

	record Feeding(Animal animal) {
	}

	public KeeperBehavior() {
		super(Job.KEEPER);
	}

	@Override
	protected @Nullable Feeding chooseTask(ServerLevel level, CopperGolem golem, WorkerData data, BlockPos home) {
		List<Animal> animals = level.getEntitiesOfClass(Animal.class, new AABB(home).inflate(Eyes.radius(golem, RADIUS), 4, Eyes.radius(golem, RADIUS)), Animal::isAlive);
		Map<EntityType<?>, List<Animal>> herds = new HashMap<>();
		for (Animal animal : animals) {
			herds.computeIfAbsent(animal.getType(), type -> new ArrayList<>()).add(animal);
		}
		Animal best = null;
		boolean foodAtHome = false;
		for (List<Animal> herd : herds.values()) {
			if (herd.size() >= HERD_LIMIT) {
				continue;
			}
			List<Animal> ready = herd.stream().filter(KeeperBehavior::canBeFed).toList();
			boolean waitingPartner = herd.stream().anyMatch(Animal::isInLove);
			// Feeding one alone is a waste: needs a pair, or a partner already in love.
			if (ready.isEmpty() || ready.size() < 2 && !waitingPartner) {
				continue;
			}
			Animal animal = ready.stream().min(Comparator.comparingDouble(golem::distanceToSqr)).orElseThrow();
			if (Inventories.find(List.of(data.supplies()), CartChaining.findCart(golem), animal::isFood).isEmpty()) {
				foodAtHome |= homeHasFood(level, home, animal);
				continue;
			}
			if (best == null || golem.distanceToSqr(animal) < golem.distanceToSqr(best)) {
				best = animal;
			}
		}
		if (best == null && foodAtHome) {
			this.requestReturn();
		}
		return best == null ? null : new Feeding(best);
	}

	@Override
	protected boolean tickTask(ServerLevel level, CopperGolem golem, WorkerData data, BlockPos home, Feeding feeding) {
		Animal animal = feeding.animal();
		if (!animal.isAlive() || !canBeFed(animal)) {
			return true;
		}
		if (!this.reach(level, golem, animal)) {
			return false;
		}
		if (this.workTicks == 0) {
			if (this.hesitate(golem)) {
				return false;
			}
			golem.setState(CopperGolemState.DROPPING_ITEM);
		}
		if (++this.workTicks % 10 == 1) {
			golem.playSound(SoundEvents.BRUSH_GENERIC, 0.6F, 1.0F);
		}
		if (this.workTicks < slowed(golem, FEED_TICKS)) {
			return false;
		}
		ItemStack food = Inventories.takeOne(List.of(data.supplies()), CartChaining.findCart(golem), animal::isFood);
		if (!food.isEmpty()) {
			animal.setInLove(null);
			level.sendParticles(ParticleTypes.HEART, animal.getX(), animal.getY() + animal.getBbHeight(), animal.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
			Tools.wear(golem, golem.getMainHandItem());
			Upkeep.spend(golem, ENERGY_PER_FEED);
		}
		return true;
	}

	/** Grown up, not in love yet and not on breeding cooldown. */
	private static boolean canBeFed(Animal animal) {
		return !animal.isBaby() && animal.getAge() == 0 && animal.canFallInLove() && !animal.isInLove();
	}

	private static boolean homeHasFood(ServerLevel level, BlockPos home, Animal animal) {
		Container chest = Homes.container(level, home);
		return chest != null && Inventories.contains(chest, animal::isFood);
	}

	/** Takes food for the animals around: up to a stack's worth of each kind they eat. */
	@Override
	protected void restock(ServerLevel level, CopperGolem golem, WorkerData data, Container chest) {
		BlockPos home = golem.blockPosition();
		List<Animal> animals = level.getEntitiesOfClass(Animal.class, new AABB(home).inflate(Eyes.radius(golem, RADIUS) + 4, 4, Eyes.radius(golem, RADIUS) + 4), Animal::isAlive);
		for (int slot = 0; slot < chest.getContainerSize(); slot++) {
			ItemStack stack = chest.getItem(slot);
			if (stack.isEmpty() || animals.stream().noneMatch(animal -> animal.isFood(stack))) {
				continue;
			}
			int missing = FOOD_RESERVE - Inventories.count(data.supplies(), other -> other.is(stack.getItem()));
			if (missing > 0) {
				Inventories.addTo(data.supplies(), chest.removeItem(slot, Math.min(missing, stack.getCount())));
			}
		}
		chest.setChanged();
	}
}

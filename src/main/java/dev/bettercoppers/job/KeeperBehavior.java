package dev.bettercoppers.job;

import dev.bettercoppers.golem.Eyes;
import dev.bettercoppers.golem.Inventories;
import dev.bettercoppers.home.Homes;
import dev.bettercoppers.mixin.LivingEntityInvoker;
import dev.bettercoppers.runningcosts.RunningCosts;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.cow.AbstractCow;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.CopperGolemState;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Looks after the animals around its home. Feeds them from the home chest so they breed, two at a time, until a herd has
 * {@link #HERD_LIMIT} animals. With nothing to feed it shears sheep, then milks cows, goats and mooshrooms, if the chest
 * gave it shears, buckets or bowls.
 */
public class KeeperBehavior extends JobBehavior<KeeperBehavior.Task> {
	private static final int RADIUS = 10;
	private static final int HERD_LIMIT = 16;
	public static final int FOOD_RESERVE = 16;
	/** Few on purpose: milk and stew do not stack, so each one takes a chest slot. */
	public static final int VESSEL_RESERVE = 4;
	private static final int WORK_TICKS = 25;
	private static final int ENERGY_PER_ACTION = 10;
	private static final int MILK_COOLDOWN = 6000;
	private static final Predicate<ItemStack> SHEARS = stack -> stack.is(Items.SHEARS);
	private static final Predicate<ItemStack> BUCKET = stack -> stack.is(Items.BUCKET);
	private static final Predicate<ItemStack> BOWL = stack -> stack.is(Items.BOWL);
	// ponytail: in memory, so a restart allows one early milking; an attachment on the animal if that ever matters.
	private static final Map<Animal, Long> MILKABLE_AT = new WeakHashMap<>();

	enum Work {
		FEED, SHEAR, MILK
	}

	record Task(Animal animal, Work work) {
	}

	public KeeperBehavior() {
		super(Job.KEEPER);
	}

	@Override
	protected @Nullable Task chooseTask(ServerLevel level, CopperGolem golem, BlockPos home) {
		Load load = Load.of(golem);
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
			if (load.find(animal::isFood).isEmpty()) {
				foodAtHome |= homeHas(level, home, animal::isFood);
				continue;
			}
			if (best == null || golem.distanceToSqr(animal) < golem.distanceToSqr(best)) {
				best = animal;
			}
		}
		if (best != null) {
			return new Task(best, Work.FEED);
		}
		if (foodAtHome) {
			this.requestReturn();
			return null;
		}
		Task harvest = chooseHarvest(level, golem, load, animals);
		if (harvest == null && harvestWaitsAtHome(level, home, load, animals)) {
			this.requestReturn();
		}
		return harvest;
	}

	/** Shearing comes before milking, and only with what the golem already carries. */
	private static @Nullable Task chooseHarvest(ServerLevel level, CopperGolem golem, Load load, List<Animal> animals) {
		if (!load.find(SHEARS).isEmpty()) {
			Animal sheep = nearest(golem, animals, KeeperBehavior::canBeSheared);
			if (sheep != null) {
				return new Task(sheep, Work.SHEAR);
			}
		}
		boolean bucket = !load.find(BUCKET).isEmpty();
		boolean bowl = !load.find(BOWL).isEmpty();
		Animal milkable = nearest(golem, animals, animal -> canBeMilked(level, animal) && (bucket || bowl && animal instanceof MushroomCow));
		return milkable == null ? null : new Task(milkable, Work.MILK);
	}

	/** An animal is waiting for something the golem has run out of and the home chest still holds. */
	private static boolean harvestWaitsAtHome(ServerLevel level, BlockPos home, Load load, List<Animal> animals) {
		if (load.find(SHEARS).isEmpty() && homeHas(level, home, SHEARS) && animals.stream().anyMatch(KeeperBehavior::canBeSheared)) {
			return true;
		}
		if (load.find(BUCKET).isEmpty() && homeHas(level, home, BUCKET) && animals.stream().anyMatch(animal -> canBeMilked(level, animal))) {
			return true;
		}
		return load.find(BOWL).isEmpty() && homeHas(level, home, BOWL)
			&& animals.stream().anyMatch(animal -> animal instanceof MushroomCow && canBeMilked(level, animal));
	}

	private static @Nullable Animal nearest(CopperGolem golem, List<Animal> animals, Predicate<Animal> filter) {
		return animals.stream().filter(filter).min(Comparator.comparingDouble(golem::distanceToSqr)).orElse(null);
	}

	@Override
	protected boolean tickTask(ServerLevel level, CopperGolem golem, BlockPos home, Task task) {
		Animal animal = task.animal();
		if (!animal.isAlive() || !stillWanted(level, task)) {
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
		if (++this.workTicks % 10 == 1 && task.work() == Work.FEED) {
			golem.playSound(SoundEvents.BRUSH_GENERIC, 0.6F, 1.0F);
		}
		if (this.workTicks < slowed(golem, WORK_TICKS)) {
			return false;
		}
		boolean done = switch (task.work()) {
			case FEED -> feed(level, golem, animal);
			case SHEAR -> shear(level, golem, (Sheep) animal);
			case MILK -> milk(level, golem, animal);
		};
		if (done) {
			RunningCosts.spend(golem, ENERGY_PER_ACTION);
		}
		return true;
	}

	private static boolean stillWanted(ServerLevel level, Task task) {
		return switch (task.work()) {
			case FEED -> canBeFed(task.animal());
			case SHEAR -> canBeSheared(task.animal());
			case MILK -> canBeMilked(level, task.animal());
		};
	}

	private static boolean feed(ServerLevel level, CopperGolem golem, Animal animal) {
		if (Load.of(golem).takeOne(animal::isFood).isEmpty()) {
			return false;
		}
		animal.setInLove(null);
		level.sendParticles(ParticleTypes.HEART, animal.getX(), animal.getY() + animal.getBbHeight(), animal.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
		Tools.wear(golem, golem.getMainHandItem());
		return true;
	}

	/** Vanilla's shearing, except that the wool goes straight into the pouch. */
	private static boolean shear(ServerLevel level, CopperGolem golem, Sheep sheep) {
		Load load = Load.of(golem);
		ItemStack shears = load.find(SHEARS);
		if (shears.isEmpty()) {
			return false;
		}
		((LivingEntityInvoker) sheep).bettercoppers$dropFromShearingLootTable(level, BuiltInLootTables.SHEAR_SHEEP, shears, (server, wool) -> load.stash(wool));
		sheep.setSheared(true);
		level.playSound(null, sheep, SoundEvents.SHEEP_SHEAR, SoundSource.NEUTRAL, 1.0F, 1.0F);
		shears.hurtAndBreak(1, level, (ServerPlayer) null, item -> {
		});
		return true;
	}

	/** Stew from a mooshroom while the golem has a bowl, otherwise milk. Always plain stew, so a flower it was fed stays unused. */
	private static boolean milk(ServerLevel level, CopperGolem golem, Animal animal) {
		Load load = Load.of(golem);
		if (animal instanceof MushroomCow && !load.takeOne(BOWL).isEmpty()) {
			load.stash(new ItemStack(Items.MUSHROOM_STEW));
			golem.playSound(SoundEvents.MOOSHROOM_MILK);
		} else if (!load.takeOne(BUCKET).isEmpty()) {
			load.stash(new ItemStack(Items.MILK_BUCKET));
			golem.playSound(animal instanceof Goat ? SoundEvents.GOAT_MILK : SoundEvents.COW_MILK);
		} else {
			return false;
		}
		MILKABLE_AT.put(animal, level.getGameTime() + MILK_COOLDOWN);
		return true;
	}

	/** Grown up, not in love yet and not on breeding cooldown. */
	private static boolean canBeFed(Animal animal) {
		return !animal.isBaby() && animal.getAge() == 0 && animal.canFallInLove() && !animal.isInLove();
	}

	/** Only sheep: shearing a mooshroom would turn it into a plain cow. */
	private static boolean canBeSheared(Animal animal) {
		return animal instanceof Sheep sheep && sheep.readyForShearing();
	}

	private static boolean givesMilk(Animal animal) {
		return animal instanceof AbstractCow || animal instanceof Goat;
	}

	private static boolean canBeMilked(ServerLevel level, Animal animal) {
		return givesMilk(animal) && !animal.isBaby() && level.getGameTime() >= MILKABLE_AT.getOrDefault(animal, 0L);
	}

	private static boolean homeHas(ServerLevel level, BlockPos home, Predicate<ItemStack> filter) {
		Container chest = Homes.container(level, home);
		return chest != null && Inventories.contains(chest, filter);
	}

	/** Takes what the animals around call for: up to a stack's worth of each food they eat, shears, buckets and bowls. */
	@Override
	protected void restock(ServerLevel level, CopperGolem golem, Load load, Container chest) {
		BlockPos home = golem.blockPosition();
		List<Animal> animals = level.getEntitiesOfClass(Animal.class, new AABB(home).inflate(Eyes.radius(golem, RADIUS) + 4, 4, Eyes.radius(golem, RADIUS) + 4), Animal::isAlive);
		for (int slot = 0; slot < chest.getContainerSize(); slot++) {
			ItemStack stack = chest.getItem(slot);
			if (stack.isEmpty() || animals.stream().noneMatch(animal -> animal.isFood(stack))) {
				continue;
			}
			int missing = FOOD_RESERVE - load.suppliesCount(other -> other.is(stack.getItem()));
			if (missing > 0) {
				load.addSupplies(chest.removeItem(slot, Math.min(missing, stack.getCount())));
			}
		}
		if (animals.stream().anyMatch(Sheep.class::isInstance)) {
			takeUpTo(load, chest, SHEARS, 1);
		}
		if (animals.stream().anyMatch(KeeperBehavior::givesMilk)) {
			takeUpTo(load, chest, BUCKET, VESSEL_RESERVE);
		}
		if (animals.stream().anyMatch(MushroomCow.class::isInstance)) {
			takeUpTo(load, chest, BOWL, VESSEL_RESERVE);
		}
		chest.setChanged();
	}

	private static void takeUpTo(Load load, Container chest, Predicate<ItemStack> filter, int reserve) {
		for (int slot = 0; slot < chest.getContainerSize(); slot++) {
			ItemStack stack = chest.getItem(slot);
			int missing = reserve - load.suppliesCount(filter);
			if (filter.test(stack) && missing > 0) {
				load.addSupplies(chest.removeItem(slot, Math.min(missing, stack.getCount())));
			}
		}
	}
}

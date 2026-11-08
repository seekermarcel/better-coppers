package dev.bettercoppers.golem;

import com.google.common.collect.ImmutableMap;
import dev.bettercoppers.api.BetterCoppersApi;
import dev.bettercoppers.api.TravelWatch;
import dev.bettercoppers.golem.SortingDestinations.Found;
import dev.bettercoppers.golem.SortingDestinations.Kind;
import dev.bettercoppers.golem.SortingDestinations.Spot;
import dev.bettercoppers.golem.SortingDestinations.Tier;
import dev.bettercoppers.entity.CopperChestMinecart;
import dev.bettercoppers.home.Homes;
import dev.bettercoppers.job.Jobs;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.CopperGolemState;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A sorter's replacement for the vanilla "open every chest until one fits" search. The golem scans all containers in
 * range and knows where an item belongs: a container with an item frame showing it, else a chest that already holds
 * it, else any barrel with room, else an empty chest. To keep some charm it still opens 0-3 wrong containers per trip
 * before the right one.
 * <p>
 * With a {@link CopperChestMinecart} chained to it, the golem also loads every item related to the one it picked
 * (see {@link ItemFamilies}) into the cart and delivers the whole load before returning to the copper chest.
 */
public class SortItemsBehavior extends Behavior<CopperGolem> {
	private static final int HORIZONTAL_RANGE = 32;
	private static final int VERTICAL_RANGE = 8;
	private static final int IDLE_COOLDOWN = 140;
	private static final int MAX_QUEUE_TICKS = 600;
	private static final int MAX_DECOYS = 3;
	private static final int MAX_PATH_ATTEMPTS = 4;
	private static final int MAX_DELIVERY_ROUNDS = 8;
	private static final double QUEUE_DISTANCE = 3.0;
	private static final double KEEP_INTERACTING_DISTANCE = 2.0;

	private final TravelWatch travel = new TravelWatch();
	private int queueTicks;

	private enum Purpose {
		PICKUP,
		DECOY,
		DELIVER
	}

	private @Nullable BlockPos target;
	private Purpose purpose = Purpose.PICKUP;
	private Tier tier = Tier.MATCHING_CHEST;
	/** The current delivery target, with the frames on it. */
	private @Nullable Spot targetSpot;
	private boolean interacting;
	private int ticks;
	private final ArrayDeque<BlockPos> decoys = new ArrayDeque<>();
	private int decoyBudget;
	private boolean deliveryPrepared;
	private final Map<BlockPos, Long> ignoredUntil = new HashMap<>();

	public SortItemsBehavior() {
		super(ImmutableMap.of(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS, MemoryStatus.VALUE_ABSENT, MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_ABSENT));
	}

	@Override
	protected boolean checkExtraStartConditions(ServerLevel level, CopperGolem golem) {
		Jobs.JobKind kind = Jobs.of(golem);
		if (kind == Jobs.JobKind.COURIER || kind == Jobs.JobKind.ADDON) {
			// Their own behaviours decide when they rest; the idle cooldown below would stop them.
			return false;
		}
		if (kind == Jobs.JobKind.NONE) {
			// Vanilla transport handles golems without a job; a cooldown here would block it.
			return false;
		}
		if (Homes.get(golem) == null) {
			// A job but no home yet: vanilla's stroll needs the cooldown to wander about.
			if (golem.getBrain().getMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS).isEmpty()) {
				golem.getBrain().setMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS, IDLE_COOLDOWN);
			}
			return false;
		}
		return !golem.isLeashed() && isActiveSorter(level, golem);
	}

	@Override
	protected boolean canStillUse(ServerLevel level, CopperGolem golem, long timestamp) {
		return golem.getBrain().getMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS).isEmpty() && !golem.isPanicking() && !golem.isLeashed() && isActiveSorter(level, golem);
	}

	/** Its job is sorting, and it has a home. */
	private static boolean isActiveSorter(ServerLevel level, CopperGolem golem) {
		return Jobs.of(golem) == Jobs.JobKind.SORTER && !Errands.isBusy(golem) && Homes.valid(level, golem) != null;
	}

	/** A sorter only empties its own copper chest (either half, if it is a double chest). */
	private static boolean isOwnChest(ServerLevel level, CopperGolem golem, BlockPos pos) {
		BlockPos home = Homes.get(golem);
		return home != null && Homes.sameContainer(level, home).contains(pos);
	}

	@Override
	protected boolean timedOut(long timestamp) {
		return false;
	}

	@Override
	protected void start(ServerLevel level, CopperGolem golem, long timestamp) {
		if (golem.getNavigation() instanceof GroundPathNavigation navigation) {
			navigation.setCanPathToTargetsBelowSurface(true);
		}
	}

	@Override
	protected void stop(ServerLevel level, CopperGolem golem, long timestamp) {
		this.dropTarget(golem);
		golem.clearOpenedChestPos();
		golem.setState(CopperGolemState.IDLE);
		if (golem.getNavigation() instanceof GroundPathNavigation navigation) {
			navigation.setCanPathToTargetsBelowSurface(false);
		}
	}

	@Override
	protected void tick(ServerLevel level, CopperGolem golem, long timestamp) {
		long now = level.getGameTime();
		if (this.target == null && !this.chooseTarget(level, golem, now)) {
			this.dropTarget(golem);
			golem.getBrain().setMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS, IDLE_COOLDOWN);
			return;
		}
		Container container = Homes.containerAt(level, this.target);
		if (container == null) {
			this.endInteraction(golem);
			this.dropTarget(golem);
		} else if (this.interacting) {
			this.tickInteraction(level, golem, container, now);
		} else {
			this.tickTravel(level, golem, container, now);
		}
	}

	// --- planning ---

	private boolean chooseTarget(ServerLevel level, CopperGolem golem, long now) {
		CopperChestMinecart cart = CartChaining.findCart(golem);
		ItemStack hand = golem.getMainHandItem();
		if (hand.isEmpty() && cart != null && !cart.isEmpty()) {
			hand = Inventories.takeFirst(cart, BetterCoppersApi.carryLimit(golem, Golems.HAND_LIMIT), stack -> true);
			this.setHandItem(golem, hand);
		}

		List<Spot> spots = this.scan(level, golem, now);
		if (hand.isEmpty()) {
			this.decoys.clear();
			this.deliveryPrepared = false;
			List<Spot> sources = spots.stream().filter(spot -> spot.kind() == Kind.SOURCE && isOwnChest(level, golem, spot.pos()) && Inventories.contains(spot.container(), WorkingStock.sortableIn(level, spot.pos(), spot.container()))).toList();
			return this.targetNearestReachable(level, golem, now, sources, Purpose.PICKUP);
		}

		Found found = SortingDestinations.find(spots, hand, golem.position());
		if (found == null) {
			return false;
		}
		if (!this.deliveryPrepared) {
			this.deliveryPrepared = true;
			this.planDecoys(level.getRandom(), golem, spots, found.spots().get(0), hand, cart != null && !cart.isEmpty());
		}
		while (!this.decoys.isEmpty()) {
			BlockPos decoy = this.decoys.poll();
			if (Homes.containerAt(level, decoy) != null && this.canReach(level, golem, decoy)) {
				this.setTarget(golem, decoy, Purpose.DECOY);
				return true;
			}
		}
		// Unreachable spots end up ignored, so look again: a nearer tier full of them must not hide a reachable one.
		for (int round = 0; round < MAX_DELIVERY_ROUNDS && found != null; round++) {
			this.tier = found.tier();
			Found current = found;
			if (this.targetNearestReachable(level, golem, now, current.spots(), Purpose.DELIVER)) {
				this.targetSpot = current.spots().stream().filter(spot -> spot.pos().equals(this.target)).findFirst().orElse(null);
				return true;
			}
			spots = spots.stream().filter(spot -> !this.isIgnored(spot.pos(), now)).toList();
			found = SortingDestinations.find(spots, hand, golem.position());
		}
		this.targetSpot = null;
		return false;
	}

	/**
	 * Spreads the trip's decoy budget over its deliveries. The last delivery of a trip uses up whatever is left.
	 */
	private void planDecoys(RandomSource random, CopperGolem golem, List<Spot> spots, Spot destination, ItemStack item, boolean moreDeliveriesAfter) {
		this.decoys.clear();
		int count = moreDeliveriesAfter ? random.nextInt(this.decoyBudget + 1) : this.decoyBudget;
		this.decoyBudget -= count;
		this.decoys.addAll(SortingDestinations.decoys(spots, destination, item, count, golem.position(), random));
	}

	private boolean targetNearestReachable(ServerLevel level, CopperGolem golem, long now, List<Spot> spots, Purpose purpose) {
		List<Spot> candidates = new ArrayList<>(spots);
		candidates.sort(Comparator.comparingDouble(spot -> spot.pos().distToCenterSqr(golem.position())));
		for (int i = 0; i < Math.min(MAX_PATH_ATTEMPTS, candidates.size()); i++) {
			BlockPos pos = candidates.get(i).pos();
			if (this.canReach(level, golem, pos)) {
				this.setTarget(golem, pos, purpose);
				return true;
			}
			this.ignore(pos, now);
		}
		return false;
	}

	private void setTarget(CopperGolem golem, BlockPos pos, Purpose purpose) {
		this.target = pos;
		this.purpose = purpose;
		this.interacting = false;
		this.ticks = 0;
		this.travel.reset();
		this.queueTicks = 0;
		golem.clearOpenedChestPos();
		golem.setState(CopperGolemState.IDLE);
	}

	private void dropTarget(CopperGolem golem) {
		this.target = null;
		this.interacting = false;
		this.ticks = 0;
		golem.getNavigation().stop();
		golem.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
	}

	private void ignore(BlockPos pos, long now) {
		this.ignoredUntil.put(pos, now + Golems.IGNORE_TICKS);
	}

	private boolean isIgnored(BlockPos pos, long now) {
		Long until = this.ignoredUntil.get(pos);
		if (until == null) {
			return false;
		}
		if (until <= now) {
			this.ignoredUntil.remove(pos);
			return false;
		}
		return true;
	}

	// --- moving and interacting ---

	private void tickTravel(ServerLevel level, CopperGolem golem, Container container, long now) {
		if (isWithinReach(level, golem, this.target, QUEUE_DISTANCE) && isOpenedByOthers(container, golem)) {
			Golems.stopInPlace(golem);
			if (++this.queueTicks > MAX_QUEUE_TICKS) {
				this.ignore(this.target, now);
				this.dropTarget(golem);
			}
		} else if (isWithinReach(level, golem, this.target, interactionRange(golem))) {
			this.interacting = true;
			this.ticks = 0;
		} else if (this.travel.tick(golem, Vec3.atCenterOf(this.target)) > Golems.MAX_TRAVEL_TICKS) {
			this.ignore(this.target, now);
			this.dropTarget(golem);
		} else {
			BehaviorUtils.setWalkAndLookTargetMemories(golem, this.target, 1.0F, 0);
		}
	}

	private void tickInteraction(ServerLevel level, CopperGolem golem, Container container, long now) {
		if (!isWithinReach(level, golem, this.target, KEEP_INTERACTING_DISTANCE)) {
			this.endInteraction(golem);
			this.interacting = false;
			this.ticks = 0;
			return;
		}
		this.ticks = ChestVisit.tick(golem, container, this.target, this.ticks, this.animationFor(golem, container));
		if (this.ticks >= Golems.INTERACTION_TICKS) {
			switch (this.purpose) {
				case PICKUP -> this.pickUp(level, golem, container);
				case DELIVER -> this.deliver(golem, container, now);
				case DECOY -> {
				}
			}
			container.setChanged();
			ChestVisit.close(golem, container);
			this.dropTarget(golem);
		}
	}

	private void endInteraction(CopperGolem golem) {
		golem.clearOpenedChestPos();
		golem.setState(CopperGolemState.IDLE);
	}

	private CopperGolemState animationFor(CopperGolem golem, Container container) {
		return switch (this.purpose) {
			case PICKUP -> container.isEmpty() ? CopperGolemState.GETTING_NO_ITEM : CopperGolemState.GETTING_ITEM;
			case DELIVER -> Inventories.hasRoom(container, golem.getMainHandItem()) ? CopperGolemState.DROPPING_ITEM : CopperGolemState.DROPPING_NO_ITEM;
			case DECOY -> CopperGolemState.DROPPING_NO_ITEM;
		};
	}

	private void pickUp(ServerLevel level, CopperGolem golem, Container source) {
		ToIntFunction<ItemStack> mayLeave = WorkingStock.surplusIn(level, this.target, source);
		ItemStack kind = ItemStack.EMPTY;
		ItemStack hand = ItemStack.EMPTY;
		for (int slot = 0; slot < source.getContainerSize(); slot++) {
			ItemStack stack = source.getItem(slot);
			int amount = stack.isEmpty() ? 0 : Math.min(Math.min(stack.getCount(), BetterCoppersApi.carryLimit(golem, Golems.HAND_LIMIT)), mayLeave.applyAsInt(stack));
			if (amount > 0) {
				kind = stack.copy();
				hand = source.removeItem(slot, amount);
				break;
			}
		}
		if (kind.isEmpty()) {
			return;
		}
		CopperChestMinecart cart = CartChaining.findCart(golem);
		if (cart != null) {
			for (int slot = 0; slot < source.getContainerSize(); slot++) {
				ItemStack stack = source.getItem(slot);
				// Only the surplus above the working stock moves, never a whole stack.
				int amount = stack.isEmpty() || !ItemFamilies.related(kind, stack) ? 0 : Math.min(stack.getCount(), mayLeave.applyAsInt(stack));
				if (amount > 0) {
					ItemStack left = Inventories.insert(cart, stack.copyWithCount(amount));
					stack.shrink(amount - left.getCount());
					if (stack.isEmpty()) {
						source.setItem(slot, ItemStack.EMPTY);
					}
				}
			}
			cart.setChanged();
		}
		source.setChanged();
		this.setHandItem(golem, hand);
		this.decoyBudget = level.getRandom().nextInt(MAX_DECOYS + 1);
		this.deliveryPrepared = false;
		this.ignoredUntil.clear();
	}

	private void deliver(CopperGolem golem, Container destination, long now) {
		ItemStack hand = golem.getMainHandItem();
		ItemStack delivered = hand.copyWithCount(1);
		ItemStack left = Inventories.insert(destination, hand);
		this.setHandItem(golem, left);
		this.deliveryPrepared = false;

		CopperChestMinecart cart = CartChaining.findCart(golem);
		if (cart != null) {
			// Unload everything of the same kind, and into a sorted chest also anything else it already holds.
			for (int slot = 0; slot < cart.getContainerSize(); slot++) {
				ItemStack stack = cart.getItem(slot);
				if (!stack.isEmpty() && this.targetSpot != null && SortingDestinations.belongs(stack, delivered, this.targetSpot, this.tier)) {
					cart.setItem(slot, Inventories.insert(destination, stack));
				}
			}
			cart.setChanged();
		}
		if (!left.isEmpty()) {
			this.ignore(this.target, now);
		}
	}

	private void setHandItem(CopperGolem golem, ItemStack stack) {
		golem.setItemSlot(EquipmentSlot.MAINHAND, stack);
		golem.setGuaranteedDrop(EquipmentSlot.MAINHAND);
	}

	// --- world queries ---

	private List<Spot> scan(ServerLevel level, CopperGolem golem, long now) {
		int range = Eyes.radius(golem, HORIZONTAL_RANGE);
		AABB area = new AABB(golem.blockPosition()).inflate(range, VERTICAL_RANGE, range);
		List<Spot> spots = new ArrayList<>();
		Map<BlockPos, List<ItemStack>> frames = new HashMap<>();
		for (ItemFrame frame : level.getEntitiesOfClass(ItemFrame.class, area.inflate(1.0), frame -> !frame.getItem().isEmpty())) {
			frames.computeIfAbsent(FrameFilter.hangsOn(frame), pos -> new ArrayList<>()).add(frame.getItem());
		}
		Inventories.forEachBlockEntity(level, area, blockEntity -> {
			BlockPos pos = blockEntity.getBlockPos();
			if (this.isIgnored(pos, now)) {
				return;
			}
			Kind kind = kindOf(blockEntity.getBlockState());
			Container container = kind == null ? null : Homes.containerAt(level, pos);
			if (container != null) {
				List<ItemStack> shown = new ArrayList<>();
				for (BlockPos part : Homes.sameContainer(level, pos)) {
					shown.addAll(frames.getOrDefault(part, List.of()));
				}
				spots.add(new Spot(pos, kind, container, FrameFilter.of(level, shown), Set.copyOf(Homes.sameContainer(level, pos))));
			}
		});
		return spots;
	}

	private static @Nullable Kind kindOf(BlockState state) {
		if (state.is(BlockTags.COPPER_CHESTS)) {
			return Kind.SOURCE;
		}
		if (state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST)) {
			return Kind.CHEST;
		}
		return state.is(Blocks.BARREL) ? Kind.BARREL : null;
	}

	private static boolean isOpenedByOthers(Container container, CopperGolem golem) {
		return container.getEntitiesWithContainerOpen().stream().anyMatch(user -> user != golem);
	}

	private boolean canReach(ServerLevel level, CopperGolem golem, BlockPos pos) {
		if (isWithinReach(level, golem, pos, 1.0)) {
			return true;
		}
		Path path = golem.getNavigation().createPath(pos, 0);
		if (path == null || path.getEndNode() == null) {
			return false;
		}
		Vec3 end = Vec3.atBottomCenterOf(path.getEndNode().asBlockPos());
		return isWithinReach(level, golem, pos, 1.0, end);
	}

	private static double interactionRange(CopperGolem golem) {
		Path path = golem.getNavigation().getPath();
		return path != null && path.isDone() ? 1.0 : 0.5;
	}

	private static boolean isWithinReach(Level level, CopperGolem golem, BlockPos pos, double distance) {
		return isWithinReach(level, golem, pos, distance, golem.position());
	}

	private static boolean isWithinReach(Level level, CopperGolem golem, BlockPos pos, double distance, Vec3 feet) {
		AABB box = golem.getBoundingBox();
		AABB golemBox = AABB.ofSize(feet.add(0.0, box.getYsize() / 2.0, 0.0), box.getXsize(), box.getYsize(), box.getZsize());
		VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
		AABB blockBox = shape.isEmpty() ? new AABB(0, 0, 0, 1, 1, 1) : shape.bounds();
		return blockBox.inflate(distance, 0.5, distance).move(pos).intersects(golemBox);
	}
}

package dev.bettercoppers.courier;

import com.google.common.collect.ImmutableMap;
import dev.bettercoppers.api.BetterCoppersApi;
import dev.bettercoppers.entity.CopperChestMinecart;
import dev.bettercoppers.golem.CartChaining;
import dev.bettercoppers.golem.ChestVisit;
import dev.bettercoppers.golem.FrameFilter;
import dev.bettercoppers.golem.Golems;
import dev.bettercoppers.golem.Inventories;
import dev.bettercoppers.golem.Quota;
import dev.bettercoppers.api.TravelWatch;
import dev.bettercoppers.home.Homes;
import dev.bettercoppers.golem.Errands;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.CopperGolemState;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A courier walks to the first chest of its route, takes what it can carry (16 items, or a whole cart load with a
 * chained cart), walks to the second chest however far away it is, and puts everything in. While it travels it keeps
 * its chunks loaded. On the way it walks past the route's stops in turn. Stuck somewhere, it shakes its head, walks back
 * along its own trail (further each time it gets stuck again before the next stop) and tries again; it never gives up.
 * A destination with a quota changes the round: the courier waits there until something is missing and fetches only that.
 */
public class CourierBehavior extends Behavior<CopperGolem> {
	private static final int WAIT_TICKS = 200;
	private static final int STALL_TICKS = 200;
	private static final int DETOUR_TICKS = 100;
	/** Long enough to walk back 96 blocks. */
	private static final int BACKTRACK_TICKS = 600;
	/** From this many escapes in a row, its status says where it is stuck. */
	private static final int REPORT_ESCAPES = 2;
	/** Beyond this, a waiting courier still keeps its chunks loaded so it does not freeze on the way. */
	private static final double NEAR = 12.0;
	/**
	 * How far its path search reaches, instead of the follow range of 16. A target further away gets a path to the
	 * nearest point within this, which in a tunnel that first leads away is a dead end.
	 */
	private static final float PATH_SEARCH = 64.0F;

	private final TravelWatch travel = TravelWatch.countingNewGround();
	private int interactionTicks;
	private @Nullable Vec3 detour;
	private int detourTicks;
	private int detourLimit;
	/** The route end it is heading for, and the index of the next stop on the way. Not saved: after a restart it picks the nearest stop. */
	private @Nullable BlockPos goal;
	private int nextStop;
	private final Trail trail = new Trail();
	private int escapes;
	private int waitTicks;
	/** Bringing back what the destination's quota would not take. Not saved: after a restart it just tries the destination again. */
	private boolean returning;

	public CourierBehavior() {
		super(ImmutableMap.of(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS, MemoryStatus.VALUE_ABSENT, MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_ABSENT));
	}

	@Override
	protected boolean checkExtraStartConditions(ServerLevel level, CopperGolem golem) {
		return Couriers.isCourier(golem) && !golem.isLeashed() && !Errands.isBusy(golem);
	}

	@Override
	protected boolean canStillUse(ServerLevel level, CopperGolem golem, long timestamp) {
		return golem.getBrain().getMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS).isEmpty()
			&& !golem.isPanicking()
			&& this.checkExtraStartConditions(level, golem);
	}

	@Override
	protected boolean timedOut(long timestamp) {
		return false;
	}

	@Override
	protected void start(ServerLevel level, CopperGolem golem, long timestamp) {
		golem.getNavigation().setRequiredPathLength(PATH_SEARCH);
	}

	@Override
	protected void stop(ServerLevel level, CopperGolem golem, long timestamp) {
		golem.getNavigation().setRequiredPathLength(0.0F);
		this.endInteraction(level, golem);
		this.travel.reset();
		this.detour = null;
		this.waitTicks = 0;
		this.goal = null;
		golem.getNavigation().stop();
		golem.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
	}

	@Override
	protected void tick(ServerLevel level, CopperGolem golem, long timestamp) {
		CourierRoute route = Couriers.route(golem);
		if (route == null) {
			return;
		}
		boolean carrying = isCarrying(golem);
		if (!carrying) {
			this.returning = false;
		}
		Map<Item, Integer> shortage = Couriers.shortage(golem);
		boolean idleAtDestination = !carrying && shortage != null && shortage.isEmpty();
		BlockPos target = carrying && !this.returning || idleAtDestination ? route.to() : route.from();
		// Fetching for a quota it must reach the far chest, even with no player near. Idle at the destination it loads nothing.
		boolean fetching = !carrying && shortage != null && !shortage.isEmpty();
		if (carrying || fetching || golem.position().distanceTo(Vec3.atCenterOf(target)) > NEAR) {
			if (level.getGameTime() % 10 == 0) {
				Couriers.keepLoaded(level, golem);
			}
		}

		if (this.waitTicks > 0) {
			// Waiting happens here rather than on a brain cooldown, so the chunk ticket above stays fresh.
			if (--this.waitTicks == 0) {
				golem.setState(CopperGolemState.IDLE);
			}
			return;
		}
		if (this.detour != null) {
			this.tickDetour(golem);
			return;
		}
		BlockPos stop = this.nextStop(golem, route, target);
		if (stop != null || !Golems.isInReach(golem, target)) {
			BlockPos walkTo = stop != null ? stop : target;
			this.endInteraction(level, golem);
			this.trail.record(golem.blockPosition());
			if (this.travel.tick(golem, Vec3.atCenterOf(walkTo)) > STALL_TICKS) {
				this.startDetour(golem);
				return;
			}
			BehaviorUtils.setWalkAndLookTargetMemories(golem, walkTo, 1.0F, stop != null ? 0 : 1);
			return;
		}
		this.passed(golem);

		Container container = Homes.containerAt(level, target);
		if (container == null) {
			// The chest is gone: nothing to do until the player fixes the route.
			this.waitAWhile(golem, CopperGolemState.GETTING_NO_ITEM);
			return;
		}
		this.travel.reset();
		FrameFilter frames = FrameFilter.at(level, target);
		if (idleAtDestination) {
			this.checkQuota(golem, container, frames);
			return;
		}
		Predicate<ItemStack> wanted = carrying ? stack -> true : stack -> mayTake(frames, container, shortage, stack) > 0;
		this.interactionTicks = ChestVisit.tick(golem, container, target, this.interactionTicks, carrying ? CopperGolemState.DROPPING_ITEM : Inventories.contains(container, wanted) ? CopperGolemState.GETTING_ITEM : CopperGolemState.GETTING_NO_ITEM);
		if (this.interactionTicks < Golems.INTERACTION_TICKS) {
			return;
		}
		if (carrying && this.returning) {
			unload(golem, stack -> Inventories.insert(container, stack));
		} else if (carrying) {
			Quota quota = frames.quota();
			unload(golem, stack -> quota.isEmpty() || frames.allows(stack) ? quota.insert(container, stack) : stack);
			Couriers.setShortage(golem, quota.isEmpty() ? null : quota.shortageIn(container));
		} else {
			load(golem, container, frames, shortage);
		}
		container.setChanged();
		this.endInteraction(level, golem);
		if (carrying && isCarrying(golem)) {
			if (this.returning || frames.quota().isEmpty()) {
				// The chest is full: wait there and try again.
				this.waitAWhile(golem, CopperGolemState.DROPPING_NO_ITEM);
			} else {
				// More than the quota takes: holding on to it would block the items that are running out.
				this.returning = true;
			}
		} else if (!carrying && !isCarrying(golem)) {
			if (shortage != null) {
				// Nothing here that was missing: go back and see what is missing now.
				Couriers.setShortage(golem, Map.of());
			} else {
				this.waitAWhile(golem, CopperGolemState.IDLE);
			}
		}
	}

	/** At the destination with nothing to deliver: leaves once its quota lacks something, and waits there otherwise. */
	private void checkQuota(CopperGolem golem, Container container, FrameFilter frames) {
		Map<Item, Integer> shortage = frames.quota().isEmpty() ? null : frames.quota().shortageIn(container);
		Couriers.setShortage(golem, shortage);
		if (shortage != null && shortage.isEmpty()) {
			this.waitAWhile(golem, CopperGolemState.IDLE);
		}
	}

	/**
	 * How many of this stack's item may be taken from the pickup chest right now. Its frames limit what is taken, its
	 * quota stays, and with a known shortage only that is fetched.
	 */
	private static int mayTake(FrameFilter frames, Container from, @Nullable Map<Item, Integer> wants, ItemStack stack) {
		if (!frames.isEmpty() && !frames.allows(stack)) {
			return 0;
		}
		int surplus = from.countItem(stack.getItem()) - frames.quota().of(stack.getItem());
		return wants == null ? surplus : Math.min(surplus, wants.getOrDefault(stack.getItem(), 0));
	}

	private static void load(CopperGolem golem, Container from, FrameFilter frames, @Nullable Map<Item, Integer> shortage) {
		Map<Item, Integer> wants = shortage == null ? null : new HashMap<>(shortage);
		ItemStack hand = ItemStack.EMPTY;
		for (int slot = 0; slot < from.getContainerSize() && hand.isEmpty(); slot++) {
			ItemStack stack = from.getItem(slot);
			int amount = stack.isEmpty() ? 0 : Math.min(Math.min(stack.getCount(), BetterCoppersApi.carryLimit(golem, Golems.HAND_LIMIT)), mayTake(frames, from, wants, stack));
			if (amount > 0) {
				hand = from.removeItem(slot, amount);
				took(wants, hand.getItem(), amount);
			}
		}
		golem.setItemSlot(EquipmentSlot.MAINHAND, hand);
		golem.setGuaranteedDrop(EquipmentSlot.MAINHAND);
		CopperChestMinecart cart = CartChaining.findCart(golem);
		if (cart != null) {
			for (int slot = 0; slot < from.getContainerSize() && Inventories.hasEmptySlot(cart); slot++) {
				ItemStack stack = from.getItem(slot);
				int amount = stack.isEmpty() ? 0 : Math.min(stack.getCount(), mayTake(frames, from, wants, stack));
				if (amount > 0) {
					int moved = amount - Inventories.insert(cart, stack.copyWithCount(amount)).getCount();
					took(wants, stack.getItem(), moved);
					from.removeItem(slot, moved);
				}
			}
			cart.setChanged();
		}
	}

	private static void took(@Nullable Map<Item, Integer> wants, Item item, int amount) {
		if (wants != null) {
			wants.merge(item, -amount, Integer::sum);
		}
	}

	private static void unload(CopperGolem golem, UnaryOperator<ItemStack> put) {
		golem.setItemSlot(EquipmentSlot.MAINHAND, put.apply(golem.getMainHandItem()));
		CopperChestMinecart cart = CartChaining.findCart(golem);
		if (cart != null) {
			for (int slot = 0; slot < cart.getContainerSize(); slot++) {
				ItemStack stack = cart.getItem(slot);
				if (!stack.isEmpty()) {
					cart.setItem(slot, put.apply(stack));
				}
			}
			cart.setChanged();
		}
	}

	private static boolean isCarrying(CopperGolem golem) {
		CopperChestMinecart cart = CartChaining.findCart(golem);
		return !golem.getMainHandItem().isEmpty() || cart != null && !cart.isEmpty();
	}

	private void waitAWhile(CopperGolem golem, CopperGolemState state) {
		golem.setState(state);
		golem.getNavigation().stop();
		golem.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		this.waitTicks = WAIT_TICKS;
	}

	/**
	 * The next stop to walk to on the way to {@code end}, or null once all are passed. Heading for a new end, it starts
	 * at the stop nearest to it, so that a restart mid-way does not send it back to the first one.
	 */
	private @Nullable BlockPos nextStop(CopperGolem golem, CourierRoute route, BlockPos end) {
		List<BlockPos> stops = route.stopsTowards(end);
		if (!end.equals(this.goal)) {
			this.goal = end;
			this.nextStop = 0;
			for (int i = 1; i < stops.size(); i++) {
				if (stops.get(i).distSqr(golem.blockPosition()) < stops.get(this.nextStop).distSqr(golem.blockPosition())) {
					this.nextStop = i;
				}
			}
			this.passed(golem);
		}
		while (this.nextStop < stops.size() && Golems.isInReach(golem, stops.get(this.nextStop))) {
			this.nextStop++;
			this.passed(golem);
		}
		return this.nextStop < stops.size() ? stops.get(this.nextStop) : null;
	}

	/** Reached a stop or the end: a fresh start for the stall watch, the trail and the escapes. */
	private void passed(CopperGolem golem) {
		this.travel.reset();
		this.trail.clear();
		this.escapes = 0;
		Couriers.setStuckAt(golem, null);
	}

	/** Stuck: walks back along its trail, further each time in a row, or a few blocks anywhere with no trail yet. */
	private void startDetour(CopperGolem golem) {
		this.travel.reset();
		golem.setState(CopperGolemState.GETTING_NO_ITEM);
		golem.playSound(SoundEvents.COPPER_GOLEM_ITEM_NO_GET);
		if (++this.escapes >= REPORT_ESCAPES) {
			Couriers.setStuckAt(golem, golem.blockPosition());
		}
		BlockPos crumb = this.trail.backtrack(this.escapes);
		this.detour = crumb != null ? Vec3.atBottomCenterOf(crumb) : LandRandomPos.getPos(golem, 10, 7);
		this.detourLimit = crumb != null ? BACKTRACK_TICKS : DETOUR_TICKS;
		this.detourTicks = 0;
	}

	private void tickDetour(CopperGolem golem) {
		if (++this.detourTicks > this.detourLimit || this.detour == null || golem.position().distanceTo(this.detour) < 1.5) {
			this.detour = null;
			golem.setState(CopperGolemState.IDLE);
			return;
		}
		BehaviorUtils.setWalkAndLookTargetMemories(golem, BlockPos.containing(this.detour), 1.0F, 0);
	}

	private void endInteraction(ServerLevel level, CopperGolem golem) {
		if (this.interactionTicks == 0) {
			return;
		}
		this.interactionTicks = 0;
		CourierRoute route = Couriers.route(golem);
		if (route != null) {
			for (BlockPos pos : new BlockPos[] {route.from(), route.to()}) {
				Container container = Homes.containerAt(level, pos);
				if (container != null && container.getEntitiesWithContainerOpen().contains(golem)) {
					container.stopOpen(golem);
				}
			}
		}
		golem.clearOpenedChestPos();
		golem.setState(CopperGolemState.IDLE);
	}
}

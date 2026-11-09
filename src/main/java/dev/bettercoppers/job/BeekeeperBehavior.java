package dev.bettercoppers.job;

import dev.bettercoppers.golem.Eyes;
import dev.bettercoppers.golem.Inventories;
import dev.bettercoppers.home.Homes;
import dev.bettercoppers.runningcosts.RunningCosts;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.CopperGolemState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Takes the honey from the full bee nests and beehives around its home: a honey bottle while it has glass bottles, else
 * honeycomb. Only smoked hives count, so the bees stay inside and nobody gets stung.
 */
public class BeekeeperBehavior extends JobBehavior<BlockPos> {
	private static final int RADIUS = 10;
	private static final int HEIGHT = 4;
	public static final int BOTTLE_RESERVE = 16;
	private static final int HARVEST_TICKS = 25;
	private static final int ENERGY_PER_HARVEST = 10;
	/** What vanilla's harvest/beehive loot table gives for shears. */
	private static final int COMB_PER_HIVE = 3;
	private static final Predicate<ItemStack> BOTTLE = stack -> stack.is(Items.GLASS_BOTTLE);

	public BeekeeperBehavior() {
		super(Job.BEEKEEPER);
	}

	@Override
	protected @Nullable BlockPos chooseTask(ServerLevel level, CopperGolem golem, BlockPos home) {
		int radius = Eyes.radius(golem, RADIUS);
		BlockPos best = null;
		for (BlockPos mutable : BlockPos.betweenClosed(home.offset(-radius, -HEIGHT, -radius), home.offset(radius, HEIGHT, radius))) {
			if (isReady(level, mutable) && !this.isIgnored(level, mutable)
				&& (best == null || golem.blockPosition().distSqr(mutable) < golem.blockPosition().distSqr(best))) {
				best = mutable.immutable();
			}
		}
		if (best != null && Load.of(golem).find(BOTTLE).isEmpty() && homeHasBottle(level, home)) {
			this.requestReturn();
			return null;
		}
		return best;
	}

	@Override
	protected boolean tickTask(ServerLevel level, CopperGolem golem, BlockPos home, BlockPos pos) {
		if (!isReady(level, pos)) {
			return true;
		}
		if (!this.reach(level, golem, pos)) {
			return false;
		}
		if (this.workTicks == 0) {
			if (this.hesitate(golem)) {
				return false;
			}
			golem.setState(CopperGolemState.DROPPING_ITEM);
		}
		if (++this.workTicks < slowed(golem, HARVEST_TICKS)) {
			return false;
		}
		Load load = Load.of(golem);
		if (load.takeOne(BOTTLE).isEmpty()) {
			golem.playSound(SoundEvents.BEEHIVE_SHEAR);
			load.stash(new ItemStack(Items.HONEYCOMB, COMB_PER_HIVE));
			Tools.wear(golem, golem.getMainHandItem());
		} else {
			golem.playSound(SoundEvents.BOTTLE_FILL);
			load.stash(new ItemStack(Items.HONEY_BOTTLE));
		}
		BlockState state = level.getBlockState(pos);
		((BeehiveBlock) state.getBlock()).resetHoneyLevel(level, state, pos);
		RunningCosts.spend(golem, ENERGY_PER_HARVEST);
		return true;
	}

	/** A full hive over a lit campfire. Harvesting any other would send the bees after a nearby player. */
	private static boolean isReady(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.getBlock() instanceof BeehiveBlock
			&& state.getValue(BeehiveBlock.HONEY_LEVEL) >= BeehiveBlock.MAX_HONEY_LEVELS
			&& CampfireBlock.isSmokeyPos(level, pos);
	}

	private static boolean homeHasBottle(ServerLevel level, BlockPos home) {
		Container chest = Homes.container(level, home);
		return chest != null && Inventories.contains(chest, BOTTLE);
	}

	/** Takes glass bottles from the chest, up to {@link #BOTTLE_RESERVE}. */
	@Override
	protected void restock(ServerLevel level, CopperGolem golem, Load load, Container chest) {
		for (int slot = 0; slot < chest.getContainerSize(); slot++) {
			ItemStack stack = chest.getItem(slot);
			int missing = BOTTLE_RESERVE - load.suppliesCount(BOTTLE);
			if (BOTTLE.test(stack) && missing > 0) {
				load.addSupplies(chest.removeItem(slot, Math.min(missing, stack.getCount())));
			}
		}
		chest.setChanged();
	}
}

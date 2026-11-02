package dev.bettercoppers.golem;

import dev.bettercoppers.home.Homes;
import dev.bettercoppers.job.FarmBehavior;
import dev.bettercoppers.job.ForestBehavior;
import dev.bettercoppers.job.Job;
import dev.bettercoppers.job.Jobs;
import dev.bettercoppers.job.KeeperBehavior;
import dev.bettercoppers.job.WorkerData;
import dev.bettercoppers.mining.MineBehavior;
import dev.bettercoppers.mining.MinePlan;
import dev.bettercoppers.mining.MinerData;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

/**
 * What a sorter leaves in a home chest it shares with another golem, so that golem's job can carry on: its tool and
 * supplies. A reserve means the item is sortable only while the chest holds more than that many of it.
 */
public final class WorkingStock {
	/** The larger of the miner and farmer lookups this replaces. */
	private static final int SEARCH_RADIUS = 64;
	private static final int ALL = Integer.MAX_VALUE;

	private WorkingStock() {
	}

	/** How many of a stack's item a job keeps in the chest. */
	private interface Reserve {
		int of(ItemStack stack);
	}

	/** Which of the chest's items a sorter may take: those the golems sharing it (either half) do not need. */
	public static Predicate<ItemStack> sortableIn(ServerLevel level, BlockPos chest, Container container) {
		Set<BlockPos> halves = Homes.sameContainer(level, chest);
		List<Reserve> reserves = new ArrayList<>();
		for (CopperGolem golem : level.getEntitiesOfClass(CopperGolem.class, new AABB(chest).inflate(SEARCH_RADIUS))) {
			BlockPos home = Homes.get(golem);
			if (home == null || !halves.contains(home)) {
				continue;
			}
			Jobs.JobKind kind = Jobs.of(golem);
			Job job = kind == Jobs.JobKind.WORKER ? WorkerData.job(golem) : null;
			if (job != null) {
				reserves.add(reserveOf(level, golem, job));
			} else if (kind == Jobs.JobKind.MINER && isMarked(golem)) {
				reserves.add(stack -> stack.is(Items.COPPER_PICKAXE) || MineBehavior.isSupply(stack) ? ALL : 0);
			}
		}
		return stack -> reserves.stream().allMatch(reserve -> container.countItem(stack.getItem()) > reserve.of(stack));
	}

	private static Reserve reserveOf(ServerLevel level, CopperGolem golem, Job job) {
		// The keeper's own rule: food is what the animals around it eat.
		Predicate<ItemStack> food = job == Job.KEEPER ? KeeperBehavior.foodFor(level, golem) : stack -> false;
		return stack -> {
			if (job.isTool(stack)) {
				return ALL;
			}
			return switch (job) {
				case FARMER -> FarmBehavior.isSeed(stack) ? Golems.SEED_RESERVE : 0;
				case FORESTER -> stack.is(ItemTags.SAPLINGS) ? ForestBehavior.SAPLING_RESERVE : 0;
				case KEEPER -> food.test(stack) ? KeeperBehavior.FOOD_RESERVE : 0;
				default -> 0;
			};
		};
	}

	private static boolean isMarked(CopperGolem golem) {
		MinerData data = MinerData.get(golem);
		MinePlan plan = data == null ? null : data.plan();
		return plan != null && plan.isMarked();
	}
}

package dev.bettercoppers.golem;

import dev.bettercoppers.home.Homes;
import dev.bettercoppers.job.BeekeeperBehavior;
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
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

/**
 * What a sorter leaves in a home chest it shares with another golem, so that golem's job can carry on: its tool and
 * supplies. A reserve means the item is sortable only while the chest holds more than that many of it. A quota on the
 * chest counts as one more reserve.
 */
public final class WorkingStock {
	/** The larger of the miner and farmer lookups this replaces. */
	private static final int SEARCH_RADIUS = 64;
	private static final int ALL = Integer.MAX_VALUE;
	/** What breedable animals eat, so a keeper's food stays even when its herd is away, dead or not yet bred. */
	private static final List<TagKey<Item>> ANIMAL_FOOD = List.of(ItemTags.BEE_FOOD, ItemTags.SNIFFER_FOOD, ItemTags.FOX_FOOD,
		ItemTags.COW_FOOD, ItemTags.GOAT_FOOD, ItemTags.SHEEP_FOOD, ItemTags.WOLF_FOOD, ItemTags.CAT_FOOD, ItemTags.HORSE_FOOD,
		ItemTags.CAMEL_FOOD, ItemTags.ARMADILLO_FOOD, ItemTags.CHICKEN_FOOD, ItemTags.FROG_FOOD, ItemTags.HOGLIN_FOOD,
		ItemTags.LLAMA_FOOD, ItemTags.OCELOT_FOOD, ItemTags.PANDA_FOOD, ItemTags.PIG_FOOD, ItemTags.RABBIT_FOOD,
		ItemTags.STRIDER_FOOD, ItemTags.TURTLE_FOOD, ItemTags.AXOLOTL_FOOD);

	private WorkingStock() {
	}

	/** How many of a stack's item a job keeps in the chest. */
	private interface Reserve {
		int of(ItemStack stack);
	}

	/** Whether a sorter may take any of this stack's item from the chest. */
	public static Predicate<ItemStack> sortableIn(ServerLevel level, BlockPos chest, Container container) {
		ToIntFunction<ItemStack> surplus = surplusIn(level, chest, container);
		return stack -> surplus.applyAsInt(stack) > 0;
	}

	/**
	 * How many of this stack's item a sorter may take from the chest right now: what is above the largest reserve of the
	 * golems sharing it (either half). Counted live, so it shrinks as items leave.
	 */
	public static ToIntFunction<ItemStack> surplusIn(ServerLevel level, BlockPos chest, Container container) {
		Set<BlockPos> halves = Homes.sameContainer(level, chest);
		List<Reserve> reserves = new ArrayList<>();
		Quota quota = FrameFilter.at(level, chest).quota();
		reserves.add(stack -> quota.of(stack.getItem()));
		for (CopperGolem golem : level.getEntitiesOfClass(CopperGolem.class, new AABB(chest).inflate(SEARCH_RADIUS))) {
			BlockPos home = Homes.get(golem);
			if (home == null || !halves.contains(home)) {
				continue;
			}
			Jobs.JobKind kind = Jobs.of(golem);
			Job job = kind == Jobs.JobKind.WORKER ? WorkerData.job(golem) : null;
			if (job != null) {
				reserves.add(reserveOf(job));
			} else if (kind == Jobs.JobKind.MINER) {
				// The spare pickaxe stays with any miner; supplies only with a marked plan.
				boolean marked = isMarked(golem);
				reserves.add(stack -> stack.is(Items.COPPER_PICKAXE) || marked && MineBehavior.isSupply(stack) ? ALL : 0);
			}
		}
		return stack -> {
			int reserve = reserves.stream().mapToInt(r -> r.of(stack)).max().orElse(0);
			return reserve == ALL ? 0 : Math.max(0, container.countItem(stack.getItem()) - reserve);
		};
	}

	private static Reserve reserveOf(Job job) {
		return stack -> {
			if (job.isTool(stack)) {
				return ALL;
			}
			return switch (job) {
				case FARMER -> FarmBehavior.isSeed(stack) ? Golems.SEED_RESERVE : 0;
				case FORESTER -> stack.is(ItemTags.SAPLINGS) ? ForestBehavior.SAPLING_RESERVE : 0;
				case KEEPER -> ANIMAL_FOOD.stream().anyMatch(stack::is) ? KeeperBehavior.FOOD_RESERVE : 0;
				case BEEKEEPER -> stack.is(Items.GLASS_BOTTLE) ? BeekeeperBehavior.BOTTLE_RESERVE : 0;
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

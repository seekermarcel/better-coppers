package dev.bettercoppers.job;

import dev.bettercoppers.golem.Eyes;
import dev.bettercoppers.golem.Golems;
import dev.bettercoppers.golem.Inventories;
import dev.bettercoppers.home.Homes;
import dev.bettercoppers.runningcosts.RunningCosts;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.CopperGolemState;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jspecify.annotations.Nullable;

/**
 * Looks after every field around the golem's home copper chest: harvests ripe crops and replants them, picks pumpkins
 * and melons off their stems, cuts sugar cane and bamboo down to the bottom block, and plants seeds on empty farmland.
 * The harvest goes into the pouch or the chained cart and is brought to the home chest; seeds are kept in the supplies
 * for replanting.
 */
public class FarmBehavior extends JobBehavior<FarmBehavior.Task> {
	private static final int FIELD_RADIUS = 8;
	private static final int FIELD_HEIGHT = 3;
	private static final int WORK_TICKS = 25;
	private static final int STALK_MIN_HEIGHT = 3;
	private static final int ENERGY_PER_ACTION = 3;

	enum Kind {
		/** Ripe wheat, carrots, potatoes, beetroot, nether wart, pitcher crop or torchflower: harvest and replant. */
		HARVEST_CROP,
		/** A pumpkin or melon grown from a stem. */
		HARVEST_FRUIT,
		/** Sugar cane or bamboo: cut everything above the bottom block. */
		HARVEST_STALK,
		PLANT
	}

	record Task(Kind kind, BlockPos pos, @Nullable Item seed) {
	}

	private int workDuration;

	public FarmBehavior() {
		super(Job.FARMER);
	}

	// --- finding work ---

	@Override
	protected @Nullable Task chooseTask(ServerLevel level, CopperGolem golem, BlockPos home) {
		Load load = Load.of(golem);
		Task best = null;
		double bestDistance = Double.MAX_VALUE;
		boolean seedsAtHome = false;
		int radius = Eyes.radius(golem, FIELD_RADIUS);
		for (BlockPos mutable : BlockPos.betweenClosed(home.offset(-radius, -FIELD_HEIGHT, -radius), home.offset(radius, FIELD_HEIGHT, radius))) {
			if (!level.isLoaded(mutable)) {
				continue;
			}
			BlockPos pos = mutable.immutable();
			if (this.isIgnored(level, pos)) {
				continue;
			}
			Task found = this.taskAt(level, load, pos);
			if (found == null) {
				if (!seedsAtHome && level.getBlockState(pos).is(Blocks.FARMLAND) && level.getBlockState(pos.above()).isAir()) {
					Container chest = Homes.container(level, home);
					seedsAtHome = chest != null && Inventories.contains(chest, FarmBehavior::growsOnFarmland);
				}
				continue;
			}
			double distance = pos.distToCenterSqr(golem.position());
			if (distance < bestDistance) {
				best = found;
				bestDistance = distance;
			}
		}
		if (best == null && seedsAtHome && load.find(FarmBehavior::growsOnFarmland).isEmpty()) {
			// Empty farmland and seeds waiting in the chest: fetch them.
			this.requestReturn();
		}
		return best;
	}

	private @Nullable Task taskAt(ServerLevel level, Load load, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		Block block = state.getBlock();
		if (block instanceof CropBlock crop && crop.isMaxAge(state)) {
			return new Task(Kind.HARVEST_CROP, pos, null);
		}
		if (block instanceof NetherWartBlock && state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE) {
			return new Task(Kind.HARVEST_CROP, pos, null);
		}
		if (block instanceof PitcherCropBlock && state.getValue(PitcherCropBlock.HALF) == DoubleBlockHalf.LOWER && state.getValue(PitcherCropBlock.AGE) >= PitcherCropBlock.MAX_AGE) {
			return new Task(Kind.HARVEST_CROP, pos, null);
		}
		if (state.is(Blocks.TORCHFLOWER) && level.getBlockState(pos.below()).is(Blocks.FARMLAND)) {
			return new Task(Kind.HARVEST_CROP, pos, null);
		}
		if ((state.is(Blocks.PUMPKIN) || state.is(Blocks.MELON)) && isGrownFromStem(level, pos)) {
			return new Task(Kind.HARVEST_FRUIT, pos, null);
		}
		if ((state.is(Blocks.SUGAR_CANE) || state.is(Blocks.BAMBOO)) && !level.getBlockState(pos.below()).is(block) && stalkHeight(level, pos) >= STALK_MIN_HEIGHT) {
			return new Task(Kind.HARVEST_STALK, pos, null);
		}
		BlockPos above = pos.above();
		if (!level.getBlockState(above).isAir()) {
			return null;
		}
		if (state.is(Blocks.FARMLAND)) {
			Item seed = this.pickSeed(level, load, above, FarmBehavior::growsOnFarmland);
			return seed == null ? null : new Task(Kind.PLANT, above, seed);
		}
		if (state.is(Blocks.SOUL_SAND)) {
			Item seed = this.pickSeed(level, load, above, stack -> cropOf(stack) instanceof NetherWartBlock);
			return seed == null ? null : new Task(Kind.PLANT, above, seed);
		}
		return null;
	}

	private static boolean isGrownFromStem(ServerLevel level, BlockPos pos) {
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			BlockState neighbour = level.getBlockState(pos.relative(direction));
			if (neighbour.getBlock() instanceof AttachedStemBlock && neighbour.getValue(AttachedStemBlock.FACING) == direction.getOpposite()) {
				return true;
			}
		}
		return false;
	}

	private static int stalkHeight(ServerLevel level, BlockPos bottom) {
		Block block = level.getBlockState(bottom).getBlock();
		int height = 1;
		while (height < 32 && level.getBlockState(bottom.above(height)).is(block)) {
			height++;
		}
		return height;
	}

	/** Plants what the neighbours grow, so rows stay tidy; otherwise whatever seed it has. */
	private @Nullable Item pickSeed(ServerLevel level, Load load, BlockPos pos, Predicate<ItemStack> fits) {
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			Item neighbourSeed = level.getBlockState(pos.relative(direction)).getBlock().asItem();
			ItemStack sample = new ItemStack(neighbourSeed);
			if (isSeed(sample) && fits.test(sample) && !load.find(stack -> stack.is(neighbourSeed)).isEmpty()) {
				return neighbourSeed;
			}
		}
		ItemStack any = load.find(stack -> isSeed(stack) && fits.test(stack));
		return any.isEmpty() ? null : any.getItem();
	}

	private static @Nullable Block cropOf(ItemStack stack) {
		return stack.getItem() instanceof BlockItem blockItem ? blockItem.getBlock() : null;
	}

	/** Crops, pumpkin and melon stems and pitcher crops. */
	private static boolean growsOnFarmland(ItemStack stack) {
		Block block = cropOf(stack);
		return block instanceof CropBlock || block instanceof StemBlock || block instanceof PitcherCropBlock;
	}

	/** Anything a farmer can plant: what grows on farmland and nether wart for soul sand. */
	public static boolean isSeed(ItemStack stack) {
		return growsOnFarmland(stack) || cropOf(stack) instanceof NetherWartBlock;
	}

	// --- working ---

	@Override
	protected boolean tickTask(ServerLevel level, CopperGolem golem, BlockPos home, Task task) {
		BlockPos pos = task.pos();
		if (!this.reach(level, golem, pos)) {
			return false;
		}
		if (this.workTicks == 0) {
			if (this.taskAt(level, Load.of(golem), task.kind() == Kind.PLANT ? pos.below() : pos) == null) {
				// Someone else got there first.
				this.giveUp();
				return false;
			}
			if (this.hesitate(golem)) {
				return false;
			}
			this.workDuration = slowed(golem, WORK_TICKS + golem.getRandom().nextInt(15));
			golem.setState(task.kind() == Kind.PLANT ? CopperGolemState.DROPPING_ITEM : CopperGolemState.GETTING_ITEM);
		}
		this.workTicks++;
		if (this.workTicks % 10 == 1) {
			SoundType sound = level.getBlockState(task.kind() == Kind.PLANT ? pos.below() : pos).getSoundType();
			level.playSound(null, pos, sound.getHitSound(), SoundSource.NEUTRAL, sound.getVolume() * 0.5F, sound.getPitch() * 0.8F);
		}
		if (this.workTicks < this.workDuration) {
			return false;
		}
		switch (task.kind()) {
			case HARVEST_CROP -> this.harvestCrop(level, golem, pos);
			case HARVEST_FRUIT -> harvestBlocks(level, golem, List.of(pos));
			case HARVEST_STALK -> harvestStalk(level, golem, pos);
			case PLANT -> this.plant(level, golem, pos, task.seed());
		}
		return true;
	}

	private void harvestCrop(ServerLevel level, CopperGolem golem, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		Item seed = state.is(Blocks.TORCHFLOWER) ? Items.TORCHFLOWER_SEEDS : state.getBlock().asItem();
		harvestBlocks(level, golem, List.of(pos));
		this.plant(level, golem, pos, seed);
	}

	private static void harvestStalk(ServerLevel level, CopperGolem golem, BlockPos bottom) {
		List<BlockPos> stalk = new ArrayList<>();
		for (int up = stalkHeight(level, bottom) - 1; up >= 1; up--) {
			stalk.add(bottom.above(up));
		}
		harvestBlocks(level, golem, stalk);
	}

	/** Breaks the blocks (in the given order) and keeps their drops. */
	private static void harvestBlocks(ServerLevel level, CopperGolem golem, List<BlockPos> positions) {
		ItemStack tool = golem.getMainHandItem();
		Load load = Load.of(golem);
		for (BlockPos pos : positions) {
			BlockState state = level.getBlockState(pos);
			List<ItemStack> drops = Block.getDrops(state, level, pos, null, golem, tool);
			level.destroyBlock(pos, false, golem);
			if (state.getDestroySpeed(level, pos) > 0.0F) {
				Tools.wear(golem, tool);
			}
			drops.forEach(load::stash);
			RunningCosts.spend(golem, ENERGY_PER_ACTION);
		}
	}

	private void plant(ServerLevel level, CopperGolem golem, BlockPos pos, @Nullable Item seed) {
		if (!(seed instanceof BlockItem blockItem)) {
			return;
		}
		BlockState crop = blockItem.getBlock().defaultBlockState();
		if (!level.getBlockState(pos).isAir() || !crop.canSurvive(level, pos) || Load.of(golem).takeOne(stack -> stack.is(seed)).isEmpty()) {
			return;
		}
		level.setBlockAndUpdate(pos, crop);
		RunningCosts.spend(golem, ENERGY_PER_ACTION);
		level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(golem, crop));
		level.playSound(null, pos, crop.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 0.8F);
	}

	// --- seeds ---

	/** Keeps a handful of every seed for replanting, the rest goes in the chest. */
	@Override
	protected int keepOnUnload(Load load, ItemStack stack) {
		return isSeed(stack) ? Math.max(0, Golems.SEED_RESERVE - load.suppliesCount(other -> other.is(stack.getItem()))) : 0;
	}

	@Override
	protected void restock(ServerLevel level, CopperGolem golem, Load load, Container chest) {
		Set<Item> seedTypes = new HashSet<>();
		for (ItemStack stack : chest) {
			if (isSeed(stack)) {
				seedTypes.add(stack.getItem());
			}
		}
		for (Item seed : seedTypes) {
			int missing = Golems.SEED_RESERVE - load.suppliesCount(stack -> stack.is(seed));
			if (missing > 0) {
				load.addSupplies(Inventories.takeFirst(chest, missing, stack -> stack.is(seed)));
			}
		}
	}
}

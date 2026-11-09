package dev.bettercoppers.mining;

import com.google.common.collect.ImmutableMap;
import dev.bettercoppers.golem.Eyes;
import dev.bettercoppers.golem.ChestVisit;
import dev.bettercoppers.golem.Golems;
import dev.bettercoppers.golem.Inventories;
import dev.bettercoppers.api.TravelWatch;
import dev.bettercoppers.guide.Guide;
import dev.bettercoppers.home.Homes;
import dev.bettercoppers.job.JobBehavior;
import dev.bettercoppers.job.Jobs;
import dev.bettercoppers.job.Load;
import dev.bettercoppers.job.Tools;
import dev.bettercoppers.registry.ModAttachments;
import dev.bettercoppers.registry.ModTags;
import dev.bettercoppers.runningcosts.RunningCosts;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.CopperGolemState;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Works through a {@link MinePlan} step by step: dig, lay floor, build supports, lay rails, hang torches. Mined items go
 * into the golem's pouch or its chained cart; when full (or out of supplies) it walks back to the copper chest at the
 * entrance, unloads, and restocks torches, wood and rails.
 * <p>
 * Golems are not good at this: they dig slowly, sometimes stop to scratch their heads, and give up on blocks they
 * cannot reach. Water and lava are sealed off with filler blocks before they can flood the tunnel; with no filler the golem gives up
 * that Mine Section and carries on.
 */
public class MineBehavior extends Behavior<CopperGolem> {
	/** Slower than a player with the same pickaxe. */
	private static final float DIG_SLOWNESS = 2.0F;
	private static final int MIN_DIG_TICKS = 10;
	private static final int MAX_DIG_TICKS = 400;
	private static final int PLACE_TICKS = 20;
	private static final int ORE_BUDGET = 16;
	private static final int RECENT_DIGS = 32;
	private static final int FILLER_RESERVE = 16;
	private static final float WALK_SPEED = 0.9F;
	private static final int ENERGY_PER_DIG = 2;
	private static final int ENERGY_PER_BUILD = 1;
	/** How long a miner waits before it looks again for a chest. */
	private static final int IDLE_COOLDOWN = 200;
	/** How long it idles once the job is done and the load is home. */
	private static final int DONE_COOLDOWN = 140;

	private enum Mode {
		WORKING,
		RETURNING
	}

	private @Nullable MinePlan layoutPlan;
	private List<MineLayout.Step> steps = List.of();

	private Mode mode = Mode.WORKING;
	private boolean stopAfterReturn;
	/** Left the chest still holding something. Not reset in {@link #stop}: a scare does not send it back to work. */
	private boolean backedUp;

	private MineLayout.@Nullable Step work;
	private boolean extraWork;
	private boolean hesitated;
	private final TravelWatch travel = new TravelWatch();
	private int workTicks;
	private int digTicks;
	private int pauseTicks;

	private final ArrayDeque<MineLayout.Step> oreDigs = new ArrayDeque<>();
	private int oreBudget = ORE_BUDGET;
	private final ArrayDeque<MineLayout.Step> recentDigs = new ArrayDeque<>();
	/** The needs last found in the pouch; digs sharing them are not checked again as supplies get used up. */
	private MineLayout.@Nullable Needs checkedNeeds;
	/** Extra digs (ores, blockers) the golem could not get to; not tried again. */
	private final Set<BlockPos> unreachable = new HashSet<>();

	public MineBehavior() {
		super(ImmutableMap.of(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS, MemoryStatus.VALUE_ABSENT, MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_ABSENT));
	}

	@Override
	protected boolean checkExtraStartConditions(ServerLevel level, CopperGolem golem) {
		return this.hasWork(level, golem) && RunningCosts.canWork(level, golem);
	}

	@Override
	protected boolean canStillUse(ServerLevel level, CopperGolem golem, long timestamp) {
		return golem.getBrain().getMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS).isEmpty() && !golem.isPanicking() && this.hasWork(level, golem) && RunningCosts.canWork(level, golem);
	}

	private boolean hasWork(ServerLevel level, CopperGolem golem) {
		MinerData data = MinerData.get(golem);
		if (golem.isLeashed() || data == null || Homes.get(golem) == null || Jobs.of(golem) != Jobs.JobKind.MINER) {
			return false;
		}
		MinePlan plan = data.plan();
		if (MinerData.hasPickaxe(golem) && plan != null && plan.isMarked() && plan.progress() < this.steps(level, plan).size()) {
			return true;
		}
		// A load still has to go home, even when the plan is done or the pickaxe broke.
		return !Load.of(golem).hasNothingToBringHome();
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
		if (this.work != null) {
			level.destroyBlockProgress(golem.getId(), this.work.pos(), -1);
		}
		this.work = null;
		this.mode = Mode.WORKING;
		golem.clearOpenedChestPos();
		golem.setState(CopperGolemState.IDLE);
		golem.getNavigation().stop();
		golem.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		if (golem.getNavigation() instanceof GroundPathNavigation navigation) {
			navigation.setCanPathToTargetsBelowSurface(false);
		}
	}

	private List<MineLayout.Step> steps(ServerLevel level, MinePlan plan) {
		MinePlan key = plan.unstarted();
		if (!key.equals(this.layoutPlan)) {
			this.layoutPlan = key;
			this.steps = MineLayout.build(plan, level.getMinY());
			this.oreDigs.clear();
			this.recentDigs.clear();
			this.checkedNeeds = null;
			this.unreachable.clear();
		}
		return this.steps;
	}

	@Override
	protected void tick(ServerLevel level, CopperGolem golem, long timestamp) {
		if (this.pauseTicks > 0) {
			if (--this.pauseTicks == 0) {
				golem.setState(CopperGolemState.IDLE);
			}
			return;
		}
		MinerData data = MinerData.get(golem);
		if (data == null) {
			return;
		}
		BlockPos home = Homes.valid(level, golem);
		if (home == null) {
			// Chest broken or not loaded: keep the load and look again in a while, like the other paying jobs.
			golem.getBrain().setMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS, IDLE_COOLDOWN);
			return;
		}
		if (this.backedUp) {
			if (JobBehavior.waitBackedUp(level, golem, Homes.container(level, home))) {
				this.backedUp = false;
				this.startReturn(golem, false);
			}
			return;
		}
		if (this.mode == Mode.RETURNING) {
			this.tickReturn(level, golem, data, home);
			return;
		}
		MinePlan plan = data.plan();
		if (plan == null || !plan.isMarked() || !MinerData.hasPickaxe(golem)) {
			// Pickaxe broke or no plan yet: just bring the load home.
			this.startReturn(golem, true);
			return;
		}
		if (isExcited(golem)) {
			if (level.getGameTime() % 10 == 0) {
				level.sendParticles(ParticleTypes.WAX_ON, golem.getX(), golem.getY() + 1.3, golem.getZ(), 1, 0.1, 0.1, 0.1, 0.0);
			}
			if (this.work == null && this.oreDigs.stream().noneMatch(ore -> isDiamond(level.getBlockState(ore.pos())))) {
				// The whole vein is out: off home to show the diamonds.
				this.startReturn(golem, false);
				return;
			}
		}
		if (this.work == null && !this.chooseWork(level, golem, data, plan)) {
			return;
		}
		this.tickWork(level, golem, data, plan);
	}

	// --- choosing what to do next ---

	private boolean chooseWork(ServerLevel level, CopperGolem golem, MinerData data, MinePlan plan) {
		if (this.nextExtraDig(level, golem)) {
			return this.work != null;
		}
		this.oreBudget = Eyes.radius(golem, ORE_BUDGET);

		List<MineLayout.Step> steps = this.steps(level, plan);
		while (plan.progress() < steps.size()) {
			MineLayout.Step step = steps.get(plan.progress());
			if (!level.isLoaded(step.pos())) {
				return false;
			}
			if (this.isDone(level, golem, step)) {
				plan = this.advance(data, plan);
				continue;
			}
			Predicate<ItemStack> material = this.materialFor(step.action());
			if (material != null && Load.of(golem).find(material).isEmpty()) {
				if (this.homeHas(level, golem, material)) {
					this.startReturn(golem, false);
					return false;
				}
				// Nothing to build with anywhere: leave this one out.
				plan = this.advance(data, plan);
				continue;
			}
			if (step.action() == MineLayout.Action.DIG && Load.of(golem).isFull(Golems.POUCH_LIMIT)) {
				this.startReturn(golem, false);
				return false;
			}
			if (step.action() == MineLayout.Action.DIG && !this.checkSupplies(level, golem, step.needs())) {
				return false;
			}
			MineLayout.Step floor = this.floorFirst(level, golem, step);
			return floor != null ? this.setWork(floor, true) : this.setWork(step, false);
		}
		// Plan finished.
		this.startReturn(golem, true);
		return false;
	}

	/** Fallen blocks and ores come before the plan. Returns true if it decided (picked work, or went home to unload). */
	private boolean nextExtraDig(ServerLevel level, CopperGolem golem) {
		// Gravel or sand that fell into the tunnel comes out first.
		for (MineLayout.Step dug : this.recentDigs) {
			if (level.isLoaded(dug.pos()) && !this.unreachable.contains(dug.pos()) && level.getBlockState(dug.pos()).getBlock() instanceof FallingBlock) {
				this.setWork(dug, true);
				return true;
			}
		}
		while (!this.oreDigs.isEmpty()) {
			MineLayout.Step ore = this.oreDigs.poll();
			if (level.isLoaded(ore.pos()) && isOre(level.getBlockState(ore.pos())) && this.canDig(level, golem, ore.pos())) {
				if (Load.of(golem).isFull(Golems.POUCH_LIMIT)) {
					this.oreDigs.addFirst(ore);
					this.startReturn(golem, false);
				} else {
					this.setWork(ore, true);
				}
				return true;
			}
		}
		return false;
	}

	/** No supports, no digging: fetch them, or wait at home until someone fills the chest. False if it went home. */
	private boolean checkSupplies(ServerLevel level, CopperGolem golem, MineLayout.Needs needs) {
		if (needs == this.checkedNeeds) {
			return true;
		}
		if (!hasMaterials(golem, needs, null)) {
			Container homeChest = Homes.container(level, Homes.valid(level, golem));
			boolean available = homeChest != null && hasMaterials(golem, needs, homeChest);
			this.startReturn(golem, !available);
			return false;
		}
		this.checkedNeeds = needs;
		return true;
	}

	/**
	 * No floor where the golem should stand (the entrance at a ledge, the edge of a cave)? Then it lays one first, from
	 * one block further back, or it would never get there. Null if the floor is fine.
	 */
	private MineLayout.@Nullable Step floorFirst(ServerLevel level, CopperGolem golem, MineLayout.Step step) {
		BlockPos standFloor = step.stand().below();
		BlockState below = level.getBlockState(standFloor);
		if (!below.isFaceSturdy(level, standFloor, Direction.UP) && below.canBeReplaced() && !this.unreachable.contains(standFloor)
			&& !Load.of(golem).find(this.materialFor(MineLayout.Action.FLOOR)).isEmpty()) {
			return MineLayout.Step.extra(MineLayout.Action.FLOOR, standFloor, step.stand().relative(step.backOff()));
		}
		return null;
	}

	private boolean setWork(MineLayout.Step step, boolean extra) {
		this.work = step;
		this.extraWork = extra;
		this.hesitated = false;
		this.travel.reset();
		this.workTicks = 0;
		this.digTicks = 0;
		return true;
	}

	private void finishWork(CopperGolem golem, MinerData data) {
		if (!this.extraWork && data.plan() != null) {
			this.advance(data, data.plan());
		}
		this.work = null;
		golem.setState(CopperGolemState.IDLE);
	}

	private MinePlan advance(MinerData data, MinePlan plan) {
		MinePlan next = plan.withProgress(plan.progress() + 1);
		data.setPlan(next);
		return next;
	}

	private boolean isDone(ServerLevel level, CopperGolem golem, MineLayout.Step step) {
		BlockPos pos = step.pos();
		BlockState state = level.getBlockState(pos);
		return switch (step.action()) {
			case DIG -> state.isAir() || state.canBeReplaced() && state.getFluidState().isEmpty() || !this.canDig(level, golem, pos);
			case FLOOR -> state.isFaceSturdy(level, pos, Direction.UP);
			case FENCE -> state.is(BlockTags.WOODEN_FENCES) || !state.canBeReplaced();
			case BEAM -> state.is(BlockTags.PLANKS) || !state.canBeReplaced();
			case RAIL -> state.is(BlockTags.RAILS) || !state.canBeReplaced() || !Blocks.RAIL.defaultBlockState().canSurvive(level, pos);
			// Already bright (daylight or a closer torch) means no torch needed. A torch 4 blocks off gives exactly 10, and
			// the pillars of a hall stand that far apart: each of them still gets its own.
			case TORCH -> !state.canBeReplaced() || level.getMaxLocalRawBrightness(pos) > 10;
		};
	}

	/** Blocks the golem leaves alone: unbreakable ones, containers and anything its pickaxe cannot harvest, precious ores aside. */
	private boolean canDig(ServerLevel level, CopperGolem golem, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (state.isAir() || state.getDestroySpeed(level, pos) < 0.0F || state.hasBlockEntity()) {
			return false;
		}
		return !state.requiresCorrectToolForDrops() || golem.getMainHandItem().isCorrectToolForDrops(state) || isPreciousOre(state);
	}

	/** Too hard for a copper pickaxe by vanilla's rules, but miners are allowed to bring these home. */
	private static boolean isPreciousOre(BlockState state) {
		return state.is(ConventionalBlockTags.DIAMOND_ORES) || state.is(ConventionalBlockTags.GOLD_ORES)
			|| state.is(ConventionalBlockTags.REDSTONE_ORES) || state.is(ConventionalBlockTags.EMERALD_ORES);
	}

	private static boolean isDiamond(BlockState state) {
		return state.is(ConventionalBlockTags.DIAMOND_ORES);
	}

	private static boolean isExcited(CopperGolem golem) {
		return Boolean.TRUE.equals(golem.getAttached(ModAttachments.EXCITED));
	}

	private static boolean isOre(BlockState state) {
		return state.is(ConventionalBlockTags.ORES);
	}

	// --- doing it ---

	private void tickWork(ServerLevel level, CopperGolem golem, MinerData data, MinePlan plan) {
		MineLayout.Step step = this.work;
		BlockPos pos = step.pos();
		// Sight only matters for digging, and only on the way: from its standing spot the block is always meant to be
		// within reach, even if a fence post or beam is in the way of the view.
		boolean blocked = step.action() == MineLayout.Action.DIG && !isAtStand(golem, step.stand()) && !canSee(level, golem, pos);
		if (!Golems.isInReach(golem, pos) || blocked) {
			// Something fell into the tunnel behind the golem (gravel, sand)? Every few seconds of walking, check and clear
			// that first, then try again.
			int stalled = this.travel.tick(golem, Vec3.atBottomCenterOf(step.stand()));
			if (stalled > 0 && stalled % 100 == 0) {
				MineLayout.Step blocker = this.findObstruction(level, golem, data);
				if (blocker != null && !blocker.pos().equals(pos)) {
					this.setWork(blocker, true);
					return;
				}
			}
			if (stalled > Golems.MAX_TRAVEL_TICKS) {
				// Clumsy, but not stubborn: give up on this one.
				this.unreachable.add(pos);
				level.destroyBlockProgress(golem.getId(), pos, -1);
				this.finishWork(golem, data);
				return;
			}
			BehaviorUtils.setWalkAndLookTargetMemories(golem, step.stand(), WALK_SPEED * RunningCosts.speed(golem), 0);
			return;
		}
		Golems.stopInPlace(golem);
		golem.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(pos));

		if (step.action() == MineLayout.Action.DIG) {
			this.tickDig(level, golem, data, step);
			return;
		}
		if (++this.workTicks == 1) {
			golem.setState(CopperGolemState.DROPPING_ITEM);
		}
		if (this.workTicks >= PLACE_TICKS) {
			this.build(level, golem, step);
			this.finishWork(golem, data);
			this.pauseTicks = 10;
		}
	}

	private void tickDig(ServerLevel level, CopperGolem golem, MinerData data, MineLayout.Step step) {
		BlockPos pos = step.pos();
		if (this.digTicks == 0) {
			if (!this.canDig(level, golem, pos) || level.getBlockState(pos).isAir()) {
				this.finishWork(golem, data);
				return;
			}
			if (!this.sealFluids(level, golem, data, pos)) {
				return;
			}
			if (!this.hesitated) {
				// Now and then it stops, looks at the wall, shakes its head, then gets on with it.
				this.hesitated = true;
				this.pauseTicks = Golems.hesitate(golem);
				if (this.pauseTicks > 0) {
					return;
				}
			}
			this.digTicks = this.digDuration(level, golem, pos);
			golem.setState(CopperGolemState.GETTING_ITEM);
		}
		this.workTicks++;
		BlockState state = level.getBlockState(pos);
		level.destroyBlockProgress(golem.getId(), pos, Math.min(9, this.workTicks * 10 / this.digTicks));
		if (this.workTicks % 5 == 1) {
			SoundType sound = state.getSoundType();
			level.playSound(null, pos, sound.getHitSound(), SoundSource.NEUTRAL, sound.getVolume() * 0.5F, sound.getPitch() * 0.8F);
		}
		if (this.workTicks % 20 == 0) {
			// Restart the reaching animation so it reads as swinging the pickaxe.
			golem.setState(golem.getState() == CopperGolemState.GETTING_ITEM ? CopperGolemState.DROPPING_ITEM : CopperGolemState.GETTING_ITEM);
		}
		if (this.workTicks >= this.digTicks) {
			level.destroyBlockProgress(golem.getId(), pos, -1);
			this.dig(level, golem, step, state);
			this.finishWork(golem, data);
		}
	}

	private int digDuration(ServerLevel level, CopperGolem golem, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		float hardness = state.getDestroySpeed(level, pos);
		float speed = Math.max(1.0F, golem.getMainHandItem().getDestroySpeed(state));
		float clumsiness = DIG_SLOWNESS / RunningCosts.speed(golem) * (1.0F + golem.getRandom().nextFloat() * 0.6F);
		return Math.clamp((int) Math.ceil(hardness * 30.0F / speed * clumsiness), MIN_DIG_TICKS, MAX_DIG_TICKS);
	}

	private void dig(ServerLevel level, CopperGolem golem, MineLayout.Step step, BlockState state) {
		BlockPos pos = step.pos();
		ItemStack tool = golem.getMainHandItem();
		BlockEntity blockEntity = level.getBlockEntity(pos);
		List<ItemStack> drops = Block.getDrops(state, level, pos, blockEntity, golem, tool);
		level.destroyBlock(pos, false, golem);
		state.spawnAfterBreak(level, pos, tool, true);
		if (state.getDestroySpeed(level, pos) > 0.0F) {
			Tools.wear(golem, tool);
		}
		RunningCosts.spend(golem, ENERGY_PER_DIG);
		drops.forEach(Load.of(golem)::stash);
		if (isDiamond(state) && !isExcited(golem)) {
			golem.setAttached(ModAttachments.EXCITED, true);
			level.playSound(null, golem, SoundEvents.AMETHYST_BLOCK_CHIME, golem.getSoundSource(), 1.0F, 1.6F);
			level.playSound(null, golem, SoundEvents.COPPER_GOLEM_ITEM_GET, golem.getSoundSource(), 1.0F, 1.5F);
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, golem.getX(), golem.getY() + 1.2, golem.getZ(), 10, 0.3, 0.3, 0.3, 0.0);
			Guide.awardNearby(level, golem.position(), "diamonds");
		}

		this.recentDigs.addLast(step);
		if (this.recentDigs.size() > RECENT_DIGS) {
			this.recentDigs.removeFirst();
		}
		// Ores it can now see get dug out too, a few blocks into the vein, standing in the hole it just made.
		BlockPos stand = floorBelow(level, pos);
		for (Direction direction : Direction.values()) {
			BlockPos next = pos.relative(direction);
			boolean queued = this.oreDigs.stream().anyMatch(ore -> ore.pos().equals(next));
			if (this.oreBudget > 0 && !queued && level.isLoaded(next) && isOre(level.getBlockState(next))) {
				this.oreDigs.add(MineLayout.Step.extra(MineLayout.Action.DIG, next, stand));
				this.oreBudget--;
			}
		}
	}

	/**
	 * A block standing in a part of the tunnel that was already dug out (usually gravel or sand that fell in later),
	 * the closest one to the golem. Supports, rails and torches the golem built itself do not count.
	 */
	private MineLayout.@Nullable Step findObstruction(ServerLevel level, CopperGolem golem, MinerData data) {
		MinePlan plan = data.plan();
		if (plan == null) {
			return null;
		}
		Set<BlockPos> built = new HashSet<>();
		for (MineLayout.Step step : this.steps) {
			// Rails and torches have no collision, so only fence posts and beams need leaving out.
			if (step.action() == MineLayout.Action.FENCE || step.action() == MineLayout.Action.BEAM) {
				built.add(step.pos());
			}
		}
		MineLayout.Step best = null;
		double bestDistance = Double.MAX_VALUE;
		for (int i = 0; i < Math.min(plan.progress(), this.steps.size()); i++) {
			MineLayout.Step step = this.steps.get(i);
			BlockPos pos = step.pos();
			if (step.action() != MineLayout.Action.DIG || built.contains(pos) || this.unreachable.contains(pos) || !level.isLoaded(pos)) {
				continue;
			}
			BlockState state = level.getBlockState(pos);
			double distance = pos.distToCenterSqr(golem.position());
			if (!state.getCollisionShape(level, pos).isEmpty() && this.canDig(level, golem, pos) && distance < bestDistance) {
				best = step;
				bestDistance = distance;
			}
		}
		return best;
	}

	/** The lowest open block below {@code pos}, i.e. where a golem would stand. */
	private static BlockPos floorBelow(ServerLevel level, BlockPos pos) {
		BlockPos stand = pos;
		for (int i = 0; i < 3 && !level.getBlockState(stand.below()).isFaceSturdy(level, stand.below(), Direction.UP); i++) {
			stand = stand.below();
		}
		return stand;
	}

	/**
	 * Before opening a block, plugs any water or lava next to it. Returns false while it is still busy (or gave up).
	 */
	private boolean sealFluids(ServerLevel level, CopperGolem golem, MinerData data, BlockPos pos) {
		if (!level.getFluidState(pos).isEmpty()) {
			return this.plugOrFlee(level, golem, data, pos);
		}
		for (Direction direction : Direction.values()) {
			BlockPos next = pos.relative(direction);
			if (!level.getFluidState(next).isEmpty() && !golem.getBoundingBox().intersects(new AABB(next))) {
				return this.plugOrFlee(level, golem, data, next);
			}
		}
		return true;
	}

	private boolean plugOrFlee(ServerLevel level, CopperGolem golem, MinerData data, BlockPos pos) {
		ItemStack filler = Load.of(golem).takeOne(MineBehavior::isFiller);
		if (filler.isEmpty()) {
			// Nothing to plug the hole with: too dangerous, so this bit stays undug and the golem carries on.
			this.giveUp(golem, data);
			return false;
		}
		this.placeBlock(level, golem, pos, ((BlockItem) filler.getItem()).getBlock().defaultBlockState());
		golem.setState(CopperGolemState.DROPPING_ITEM);
		this.pauseTicks = 15;
		return false;
	}

	/** Extra work gives up only itself; a step of the plan gives up its Mine Section. */
	private void giveUp(CopperGolem golem, MinerData data) {
		MineLayout.Step step = this.work;
		this.work = null;
		golem.setState(CopperGolemState.IDLE);
		if (this.extraWork) {
			this.unreachable.add(step.pos());
		} else {
			data.setPlan(data.plan().givenUpAt(step.pos(), step.resumeAt()));
		}
	}

	private void build(ServerLevel level, CopperGolem golem, MineLayout.Step step) {
		BlockPos pos = step.pos();
		if (step.action() == MineLayout.Action.TORCH) {
			this.placeTorch(level, golem, pos, step.facing());
			return;
		}
		Predicate<ItemStack> material = this.materialFor(step.action());
		ItemStack item = material == null ? ItemStack.EMPTY : Load.of(golem).takeOne(material);
		if (step.action() == MineLayout.Action.FLOOR && item.isEmpty()) {
			// No planks left for a mineshaft floor: any filler will do.
			item = Load.of(golem).takeOne(MineBehavior::isFiller);
		}
		if (item.getItem() instanceof BlockItem blockItem) {
			this.placeBlock(level, golem, pos, blockItem.getBlock().defaultBlockState());
		}
	}

	private void placeTorch(ServerLevel level, CopperGolem golem, BlockPos pos, Direction facing) {
		BlockState wall = Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, facing);
		BlockState standing = Blocks.TORCH.defaultBlockState();
		BlockPos target = pos;
		BlockState state;
		if (wall.canSurvive(level, pos)) {
			state = wall;
		} else if (standing.canSurvive(level, pos)) {
			state = standing;
		} else if (level.getBlockState(pos.below()).canBeReplaced() && standing.canSurvive(level, pos.below())) {
			target = pos.below();
			state = standing;
		} else {
			return;
		}
		if (!Load.of(golem).takeOne(stack -> stack.is(Items.TORCH)).isEmpty()) {
			this.placeBlock(level, golem, target, state);
		}
	}

	private void placeBlock(ServerLevel level, CopperGolem golem, BlockPos pos, BlockState state) {
		BlockState placed = Block.updateFromNeighbourShapes(state, level, pos);
		level.setBlockAndUpdate(pos, placed);
		RunningCosts.spend(golem, ENERGY_PER_BUILD);
		level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(golem, placed));
		SoundType sound = placed.getSoundType();
		level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
	}

	// --- materials ---

	private @Nullable Predicate<ItemStack> materialFor(MineLayout.Action action) {
		return switch (action) {
			case DIG -> null;
			case FLOOR -> stack -> stack.is(ItemTags.PLANKS) || isFiller(stack);
			case FENCE -> stack -> stack.is(ItemTags.WOODEN_FENCES);
			case BEAM -> stack -> stack.is(ItemTags.PLANKS);
			case RAIL -> stack -> stack.is(Items.RAIL);
			case TORCH -> stack -> stack.is(Items.TORCH);
		};
	}

	/** Plain blocks a golem uses to fill holes and plug water or lava. */
	static boolean isFiller(ItemStack stack) {
		return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock().defaultBlockState().is(ModTags.FILLER);
	}

	// --- going home ---

	private void startReturn(CopperGolem golem, boolean stopAfter) {
		this.mode = Mode.RETURNING;
		this.stopAfterReturn = stopAfter;
		this.workTicks = 0;
		golem.setState(CopperGolemState.IDLE);
	}

	private void endReturn(CopperGolem golem) {
		this.mode = Mode.WORKING;
		golem.clearOpenedChestPos();
		golem.setState(CopperGolemState.IDLE);
		if (this.stopAfterReturn) {
			// Nothing left to do: idle for a while like a sorting golem with an empty chest.
			golem.getBrain().setMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS, DONE_COOLDOWN);
		}
	}

	private void tickReturn(ServerLevel level, CopperGolem golem, MinerData data, BlockPos home) {
		Container chest = Homes.container(level, home);
		// Hurrying when it has diamonds to show.
		this.workTicks = JobBehavior.tickReturn(golem, home, chest, this.workTicks, WALK_SPEED * (isExcited(golem) ? 1.3F : 1.0F));
		if (this.workTicks >= Golems.INTERACTION_TICKS) {
			// Keep some plain stone back for plugging leaks and patching floors.
			Load load = Load.of(golem);
			load.unloadInto(chest, stack -> isFiller(stack) ? FILLER_RESERVE - load.suppliesCount(MineBehavior::isFiller) : 0);
			if (golem.removeAttached(ModAttachments.EXCITED) != null) {
				level.sendParticles(ParticleTypes.HAPPY_VILLAGER, golem.getX(), golem.getY() + 1.2, golem.getZ(), 12, 0.4, 0.3, 0.4, 0.0);
				golem.playSound(SoundEvents.COPPER_GOLEM_ITEM_DROP, 1.0F, 1.5F);
			}
			MinePlan plan = data.plan();
			if (plan != null && !this.stopAfterReturn) {
				this.restock(load, this.steps(level, plan), chest);
			}
			if (!load.hasNothingToBringHome()) {
				this.backedUp = true;
				this.stopAfterReturn = false;
			}
			ChestVisit.close(golem, chest);
			this.endReturn(golem);
		}
	}

	private void restock(Load load, List<MineLayout.Step> steps, Container chest) {
		this.restockOne(load, chest, stack -> stack.is(Items.TORCH), 16);
		// Rails stay in the pouch until the digging is done, so it takes what the most demanding dig asks for.
		this.restockOne(load, chest, stack -> stack.is(Items.RAIL), steps.stream().mapToInt(step -> step.needs().rails()).max().orElse(0));
		this.restockOne(load, chest, stack -> stack.is(ItemTags.PLANKS), 32);
		this.restockOne(load, chest, stack -> stack.is(ItemTags.WOODEN_FENCES), steps.stream().anyMatch(step -> step.needs().fences() > 0) ? 16 : 0);
	}

	/** Whether the golem carries (plus {@code chest}, if given, holds) {@code needs}. */
	private static boolean hasMaterials(CopperGolem golem, MineLayout.Needs needs, @Nullable Container chest) {
		Load load = Load.of(golem);
		return countWith(load, chest, stack -> stack.is(Items.RAIL)) >= needs.rails()
			&& countWith(load, chest, stack -> stack.is(ItemTags.WOODEN_FENCES)) >= needs.fences()
			&& countWith(load, chest, stack -> stack.is(ItemTags.PLANKS)) >= needs.planks();
	}

	private static int countWith(Load load, @Nullable Container chest, Predicate<ItemStack> material) {
		return load.count(material) + (chest == null ? 0 : Inventories.count(chest, material));
	}

	private void restockOne(Load load, Container chest, Predicate<ItemStack> material, int target) {
		int have = load.suppliesCount(material);
		while (have < target) {
			ItemStack taken = Inventories.takeFirst(chest, target - have, material);
			if (taken.isEmpty()) {
				return;
			}
			have += taken.getCount();
			load.addSupplies(taken);
		}
	}

	private boolean homeHas(ServerLevel level, CopperGolem golem, Predicate<ItemStack> material) {
		BlockPos pos = Homes.valid(level, golem);
		Container chest = Homes.container(level, pos);
		return chest != null && Inventories.contains(chest, material);
	}

	/** What a miner takes along from its home chest; sorting golems leave these there. */
	public static boolean isSupply(ItemStack stack) {
		return stack.is(Items.TORCH) || stack.is(Items.RAIL) || stack.is(ItemTags.PLANKS) || stack.is(ItemTags.WOODEN_FENCES);
	}

	// --- movement ---

	private static boolean isAtStand(CopperGolem golem, BlockPos stand) {
		return golem.blockPosition().equals(stand) || golem.position().distanceToSqr(Vec3.atBottomCenterOf(stand)) < 0.8;
	}

	/** No digging through walls: at least one face of the block must be in view, like vanilla's chest check. */
	private static boolean canSee(ServerLevel level, CopperGolem golem, BlockPos pos) {
		Vec3 center = Vec3.atCenterOf(pos);
		for (Direction direction : Direction.values()) {
			Vec3 face = center.add(direction.getStepX() * 0.49, direction.getStepY() * 0.49, direction.getStepZ() * 0.49);
			BlockHitResult hit = level.clip(new ClipContext(golem.getEyePosition(), face, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, golem));
			if (hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos)) {
				return true;
			}
		}
		return false;
	}
}

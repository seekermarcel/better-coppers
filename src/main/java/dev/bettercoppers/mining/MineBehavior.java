package dev.bettercoppers.mining;

import com.google.common.collect.ImmutableMap;
import dev.bettercoppers.BetterCoppers;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Digs its mine section by section, each under a claim so that miners sharing a mine never dig the same one (see
 * {@link Mines#pick}). With nothing free it waits at home. Each section goes step
 * by step: dig, lay floor, build supports, lay rails, hang torches. Mined items go into the golem's pouch or its chained
 * cart; when full (or out of supplies) it walks back to its home chest, unloads, and restocks torches, wood and rails. Progress is saved with the section in {@link Mines}.
 * <p>
 * Golems are not good at this: they dig slowly, sometimes stop to scratch their heads, and give up on blocks they
 * cannot reach. An ore it digs next to starts an Ore Vein: face-connected ores, up to 64 and 8 blocks (more with Eyes)
 * from the section, dug before the section goes on; outside the section it breaks only ore. Water is sealed off with
 * filler blocks before it can flood the tunnel. Lava, or water with no filler left, cuts the Mine Section short: a
 * Barrier goes up at its last dug slice. Vein digging plugs lava too.
 * <p>
 * A broken entrance frame, noticed when a section starts and after unloading at home, closes the mine for this miner:
 * it goes home and looks for another.
 * <p>
 * It walks out and home along the {@link MinePath Mine Path}, and keeps its chunks loaded only while a player is near
 * the mine (ADR 0011).
 */
public class MineBehavior extends Behavior<CopperGolem> {
	/** Slower than a player with the same pickaxe. */
	private static final float DIG_SLOWNESS = 2.0F;
	private static final int MIN_DIG_TICKS = 10;
	private static final int MAX_DIG_TICKS = 400;
	private static final int PLACE_TICKS = 20;
	/** Ores a vein gives at most; Eyes don't change it. */
	private static final int VEIN_CAP = 64;
	/** How far, Chebyshev, a vein is followed from its section's box; Eyes scale it. */
	private static final int VEIN_REACH = 8;
	private static final int RECENT_DIGS = 32;
	private static final int FILLER_RESERVE = 16;
	private static final float WALK_SPEED = 0.9F;
	private static final int ENERGY_PER_DIG = 2;
	private static final int ENERGY_PER_BUILD = 1;
	/** How long a miner waits before it looks again for a chest. */
	private static final int IDLE_COOLDOWN = 200;
	/** How long it idles at home with nothing to do (no mine, or every tunnel has a miner) before it looks again. */
	private static final int DONE_COOLDOWN = 100;

	/** Loads and ticks the chunks around a miner, like a courier's (ADR 0011); expires two seconds after the last refresh. */
	public static final TicketType TICKET = Registry.register(
		BuiltInRegistries.TICKET_TYPE,
		BetterCoppers.id("miner"),
		new TicketType(40L, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION | TicketType.FLAG_KEEP_DIMENSION_ACTIVE)
	);
	private static final int TICKET_RADIUS = 3;
	/** How far beyond the Mine Range a player still counts as near the mine. */
	private static final int PLAYER_REACH = 32;

	public static void init() {
	}

	/**
	 * Keeps the chunks around {@code golem} loaded for two seconds, but only while a player (not a spectator) in its
	 * dimension is within the Mine Range + {@link #PLAYER_REACH} of {@code beam}: a mine nobody watches is no ore farm.
	 */
	private static void keepLoaded(ServerLevel level, CopperGolem golem, BlockPos beam) {
		if (isWatched(level.players(), beam, MineLimits.of(level).range())) {
			level.getChunkSource().addTicketWithRadius(TICKET, ChunkPos.containing(golem.blockPosition()), TICKET_RADIUS);
		}
	}

	/** A player who is not a spectator stands within {@code range} + {@link #PLAYER_REACH} of {@code beam}, horizontal Chebyshev. */
	public static boolean isWatched(List<? extends Player> players, BlockPos beam, int range) {
		return players.stream().anyMatch(player -> !player.isSpectator()
			&& Math.max(Math.abs(player.getBlockX() - beam.getX()), Math.abs(player.getBlockZ() - beam.getZ())) <= range + PLAYER_REACH);
	}

	private enum Mode {
		WORKING,
		RETURNING
	}

	private enum Travel {
		ARRIVED,
		WALKING,
		STUCK
	}

	/** Pathfinding reach on the Mine Path, as couriers have: a leg is at most 32 blocks, and tunnels wind. */
	private static final float PATH_SEARCH = 64.0F;
	/** How long it tries to get home, once stuck in the mine, before it shakes its head again. */
	private static final int STUCK_RETRY = 100;

	private Mines.@Nullable Section layoutSection;
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

	/** Vein ores to dig, nearest first. */
	private final ArrayDeque<BlockPos> oreDigs = new ArrayDeque<>();
	/** Every ore of a vein already found in this section, so touching it again doesn't start a new vein. */
	private final Set<BlockPos> veinOres = new HashSet<>();
	/** Walkway floor cells a vein went through: filled once the vein is out. */
	private final ArrayDeque<BlockPos> walkwayHoles = new ArrayDeque<>();
	/** The section's own cells to dig, its floor steps by cell, and its saved box (footprint and floor layer). */
	private Set<BlockPos> sectionDigs = Set.of();
	private Map<BlockPos, MineLayout.Step> sectionFloors = Map.of();
	private @Nullable BoundingBox sectionBox;
	private final ArrayDeque<MineLayout.Step> recentDigs = new ArrayDeque<>();
	/** The needs last found in the pouch; digs sharing them are not checked again as supplies get used up. */
	private MineLayout.@Nullable Needs checkedNeeds;
	/** Extra digs (ores, blockers) the golem could not get to; not tried again. */
	private final Set<BlockPos> unreachable = new HashSet<>();

	/** What is left of the Mine Path it walks, out to its work or home. */
	private final ArrayDeque<BlockPos> minePath = new ArrayDeque<>();
	/** Where the Mine Path out leads (a section's start or an Open End); null on the way home. */
	private Mines.@Nullable OpenEnd minePathTo;
	private final TravelWatch pathWatch = new TravelWatch();
	/** Work it got stuck on the way to: left out until it is home again. */
	private final Set<Mines.OpenEnd> skipped = new HashSet<>();

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
		if (MinerData.hasPickaxe(golem) && Mines.of(level, golem, false) != null) {
			return true;
		}
		// A load still has to go home, even when the mine is finished or the pickaxe broke.
		return !Load.of(golem).hasNothingToBringHome();
	}

	@Override
	protected boolean timedOut(long timestamp) {
		return false;
	}

	@Override
	protected void start(ServerLevel level, CopperGolem golem, long timestamp) {
		golem.getNavigation().setRequiredPathLength(PATH_SEARCH);
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
		this.minePath.clear();
		this.minePathTo = null;
		MinerData data = MinerData.get(golem);
		if (data != null) {
			data.setStuck(false);
		}
		golem.getNavigation().setRequiredPathLength(0.0F);
		golem.clearOpenedChestPos();
		golem.setState(CopperGolemState.IDLE);
		golem.getNavigation().stop();
		golem.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		if (golem.getNavigation() instanceof GroundPathNavigation navigation) {
			navigation.setCanPathToTargetsBelowSurface(false);
		}
	}

	private List<MineLayout.Step> steps(Mines.Section section) {
		Mines.Section key = section.at(0, false);
		if (!key.equals(this.layoutSection)) {
			this.layoutSection = key;
			this.steps = MineLayout.build(section);
			this.sectionDigs = this.steps.stream().filter(step -> step.action() == MineLayout.Action.DIG).map(MineLayout.Step::pos).collect(Collectors.toSet());
			this.sectionFloors = this.steps.stream().filter(step -> step.action() == MineLayout.Action.FLOOR)
				.collect(Collectors.toMap(MineLayout.Step::pos, step -> step, (a, b) -> a));
			this.sectionBox = section.box();
			this.oreDigs.clear();
			this.veinOres.clear();
			this.walkwayHoles.clear();
			this.recentDigs.clear();
			this.checkedNeeds = null;
			this.unreachable.clear();
		}
		return this.steps;
	}

	@Override
	protected void tick(ServerLevel level, CopperGolem golem, long timestamp) {
		MinerData data = MinerData.get(golem);
		if (data == null) {
			return;
		}
		if (level.getGameTime() % 10 == 0 && data.mine() != null) {
			keepLoaded(level, golem, data.mine());
		}
		if (this.pauseTicks > 0) {
			if (--this.pauseTicks == 0) {
				golem.setState(CopperGolemState.IDLE);
			}
			return;
		}
		// Any tick renews its claim, also on trips home and while it waits for room in the chest.
		Mines.get(level).renew(level, golem);
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
		Mines.Mine mine = MinerData.hasPickaxe(golem) ? Mines.of(level, golem, false) : null;
		if (mine == null) {
			// Pickaxe broke or no mine: just bring the load home.
			this.startReturn(golem, true);
			return;
		}
		if (this.work != null && !this.extraWork && Mines.get(level).claimed(mine, golem) < 0) {
			// Its claim lapsed and someone else may dig there now: pick again.
			level.destroyBlockProgress(golem.getId(), this.work.pos(), -1);
			this.work = null;
		}
		if (isExcited(golem)) {
			if (level.getGameTime() % 10 == 0) {
				level.sendParticles(ParticleTypes.WAX_ON, golem.getX(), golem.getY() + 1.3, golem.getZ(), 1, 0.1, 0.1, 0.1, 0.0);
			}
			if (this.work == null && this.oreDigs.stream().noneMatch(ore -> isDiamond(level.getBlockState(ore)))) {
				// The whole vein is out: off home to show the diamonds.
				this.startReturn(golem, false);
				return;
			}
		}
		if (this.work == null && !this.chooseWork(level, golem, data, mine)) {
			return;
		}
		this.tickWork(level, golem, data);
	}

	// --- choosing what to do next ---

	private boolean chooseWork(ServerLevel level, CopperGolem golem, MinerData data, Mines.Mine mine) {
		if (this.nextExtraDig(level, golem)) {
			return this.work != null;
		}

		Mines mines = Mines.get(level);
		int index = mines.pick(level, mine, golem, this.skipped);
		if (index < 0) {
			Mines.OpenEnd far = mines.unloadedWork(level, mine, golem, this.skipped).orElse(null);
			if (far != null) {
				// Free work only in unloaded chunks: head that way without claiming it, and pick once it has loaded.
				this.headFor(golem, mine, far, null);
				if (this.followMinePath(golem) == Travel.STUCK) {
					this.skip(far);
				}
				return false;
			}
			// Finished (the next look finds another one), or every tunnel has a miner: wait at home.
			this.startReturn(golem, true);
			return false;
		}
		mine = mines.find(mine.entrance(), mine.axis()).orElseThrow();
		Mines.Section section = mine.sections().get(index);
		this.headFor(golem, mine, section.start(), section);
		if (section.progress() == 0 && this.leaveIfBroken(level, golem, data, mine)) {
			return false;
		}
		List<MineLayout.Step> steps = this.steps(section);
		int progress = section.progress();
		while (progress < steps.size()) {
			MineLayout.Step step = steps.get(progress);
			if (!level.isLoaded(step.pos())) {
				return false;
			}
			if (this.isDone(level, golem, step)) {
				progress = this.moveTo(level, mine, index, progress + 1);
				continue;
			}
			Predicate<ItemStack> material = this.materialFor(step.action());
			if (material != null && Load.of(golem).find(material).isEmpty()) {
				// Rails and signs are optional: laid only if it carries some.
				if (step.action() != MineLayout.Action.RAIL && step.action() != MineLayout.Action.SIGN && this.homeHas(level, golem, material)) {
					this.startReturn(golem, false);
					return false;
				}
				// Nothing to build with anywhere: leave this one out.
				progress = this.moveTo(level, mine, index, progress + 1);
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
		// Section done: the next choice grows the mine.
		Mines.get(level).setProgress(mine, index, steps.size(), true);
		return false;
	}

	// --- the Mine Path ---

	/** Sets the Mine Path out to {@code target} unless it is set already or the golem is already in {@code section}. */
	private void headFor(CopperGolem golem, Mines.Mine mine, Mines.OpenEnd target, Mines.@Nullable Section section) {
		if (target.equals(this.minePathTo) || section != null && section.box().inflatedBy(1).isInside(golem.blockPosition())) {
			return;
		}
		this.minePathTo = target;
		this.setMinePath(golem, MinePath.to(mine, target));
	}

	/** Follows {@code path} from its point nearest the golem, or the next one if the golem is already past that. */
	private void setMinePath(CopperGolem golem, List<BlockPos> path) {
		this.minePath.clear();
		this.pathWatch.reset();
		int nearest = 0;
		for (int i = 1; i < path.size(); i++) {
			if (distanceSqr(golem, path.get(i)) < distanceSqr(golem, path.get(nearest))) {
				nearest = i;
			}
		}
		if (nearest + 1 < path.size() && distanceSqr(golem, path.get(nearest + 1)) < path.get(nearest).distSqr(path.get(nearest + 1))) {
			nearest++;
		}
		this.minePath.addAll(path.subList(nearest, path.size()));
	}

	/** Walks the Mine Path leg by leg. Stuck on a leg, it drops the rest of it. */
	private Travel followMinePath(CopperGolem golem) {
		while (!this.minePath.isEmpty() && distanceSqr(golem, this.minePath.peek()) < 4.0) {
			this.minePath.poll();
			this.pathWatch.reset();
		}
		if (this.minePath.isEmpty()) {
			return Travel.ARRIVED;
		}
		if (this.pathWatch.tick(golem, Vec3.atBottomCenterOf(this.minePath.peek())) > Golems.MAX_TRAVEL_TICKS) {
			this.minePath.clear();
			this.pathWatch.reset();
			return Travel.STUCK;
		}
		BehaviorUtils.setWalkAndLookTargetMemories(golem, this.minePath.peek(), WALK_SPEED * RunningCosts.speed(golem), 1);
		return Travel.WALKING;
	}

	/** Stuck on the way out: leaves {@code target} for this trip and lets go of its section, so the next pick finds other work. */
	private void skip(Mines.OpenEnd target) {
		this.skipped.add(target);
		this.minePathTo = null;
		this.minePath.clear();
		this.work = null;
	}

	private static double distanceSqr(CopperGolem golem, BlockPos pos) {
		return golem.position().distanceToSqr(Vec3.atBottomCenterOf(pos));
	}

	/** A broken frame closes the mine: forget it, go home, unload and look again. True if it was broken. */
	private boolean leaveIfBroken(ServerLevel level, CopperGolem golem, MinerData data, Mines.Mine mine) {
		if (!level.isLoaded(mine.entrance()) || MineEntrance.isIntact(level, mine.entrance(), mine.axis())) {
			return false;
		}
		Mines.get(level).releaseAll(mine);
		data.clearMine();
		this.startReturn(golem, true);
		return true;
	}

	/** Saves the progress of the section the golem works on and returns it. */
	private int moveTo(ServerLevel level, Mines.Mine mine, int index, int progress) {
		Mines.get(level).setProgress(mine, index, progress, false);
		return progress;
	}

	/** The golem's current mine, or null if it has none any more. */
	private static Mines.@Nullable Mine mine(CopperGolem golem) {
		return golem.level() instanceof ServerLevel level ? Mines.of(level, golem, false) : null;
	}

	/** The section it holds a claim on in its mine, or -1. */
	private static int claimed(CopperGolem golem, Mines.@Nullable Mine mine) {
		return mine == null ? -1 : Mines.get((ServerLevel) golem.level()).claimed(mine, golem);
	}

	/** Fallen blocks and ores come before the section. Returns true if it decided (picked work, or went home to unload). */
	private boolean nextExtraDig(ServerLevel level, CopperGolem golem) {
		// Gravel or sand that fell into the tunnel comes out first.
		for (MineLayout.Step dug : this.recentDigs) {
			if (level.isLoaded(dug.pos()) && !this.unreachable.contains(dug.pos()) && level.getBlockState(dug.pos()).getBlock() instanceof FallingBlock) {
				this.setWork(dug, true);
				return true;
			}
		}
		// The first ore it can stand next to, nearest first. The others wait until digging opens a way to them, or stay.
		for (Iterator<BlockPos> ores = this.oreDigs.iterator(); ores.hasNext(); ) {
			BlockPos ore = ores.next();
			if (!level.isLoaded(ore) || !isOre(level.getBlockState(ore)) || this.unreachable.contains(ore) || !this.canDig(level, golem, ore)) {
				ores.remove();
				continue;
			}
			BlockPos stand = standNextTo(level, golem, ore);
			if (stand == null) {
				continue;
			}
			if (Load.of(golem).isFull(Golems.POUCH_LIMIT)) {
				this.startReturn(golem, false);
			} else {
				ores.remove();
				this.setWork(MineLayout.Step.extra(MineLayout.Action.DIG, ore, stand), true);
			}
			return true;
		}
		// No ore to get at: patch the holes the vein left in the walkway. Holes elsewhere stay open.
		while (!this.walkwayHoles.isEmpty()) {
			MineLayout.Step floor = this.sectionFloors.get(this.walkwayHoles.poll());
			if (level.isLoaded(floor.pos()) && !this.isDone(level, golem, floor) && !Load.of(golem).find(this.materialFor(MineLayout.Action.FLOOR)).isEmpty()) {
				this.setWork(floor, true);
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
		Mines.Mine mine = mine(golem);
		int index = claimed(golem, mine);
		if (!this.extraWork && index >= 0) {
			this.moveTo((ServerLevel) golem.level(), mine, index, mine.sections().get(index).progress() + 1);
		}
		this.work = null;
		golem.setState(CopperGolemState.IDLE);
	}

	private boolean isDone(ServerLevel level, CopperGolem golem, MineLayout.Step step) {
		BlockPos pos = step.pos();
		BlockState state = level.getBlockState(pos);
		return switch (step.action()) {
			case DIG -> state.isAir() || state.canBeReplaced() && state.getFluidState().isEmpty() || !this.canDig(level, golem, pos);
			case FLOOR -> state.isFaceSturdy(level, pos, Direction.UP);
			// A rail in a Barrier's way is taken up; any other block there stays and takes the post's place.
			case FENCE -> state.is(BlockTags.WOODEN_FENCES) || !state.canBeReplaced() && !state.is(BlockTags.RAILS);
			case BEAM -> state.is(BlockTags.PLANKS) || !state.canBeReplaced();
			case RAIL -> state.is(BlockTags.RAILS) || !state.canBeReplaced() || !Blocks.RAIL.defaultBlockState().canSurvive(level, pos);
			// Already bright (daylight or a closer torch) means no torch needed. A torch 4 blocks off gives exactly 10, and
			// the pillars of a hall stand that far apart: each of them still gets its own.
			case TORCH -> !state.canBeReplaced() || level.getMaxLocalRawBrightness(pos) > 10;
			case SIGN -> !state.canBeReplaced() || !Blocks.OAK_SIGN.defaultBlockState().canSurvive(level, pos);
		};
	}

	/** Blocks the golem leaves alone: unbreakable ones, containers and anything its pickaxe cannot harvest, precious ores aside. */
	private boolean canDig(ServerLevel level, CopperGolem golem, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return canDig(state, state.getDestroySpeed(level, pos), golem.getMainHandItem());
	}

	/** {@code state}, as hard as {@code hardness}, is no air and a miner with {@code tool} may dig it. */
	static boolean canDig(BlockState state, float hardness, ItemStack tool) {
		if (state.isAir() || hardness < 0.0F || state.hasBlockEntity()) {
			return false;
		}
		return !state.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(state) || isPreciousOre(state);
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

	private void tickWork(ServerLevel level, CopperGolem golem, MinerData data) {
		if (!this.minePath.isEmpty()) {
			if (this.followMinePath(golem) == Travel.STUCK && this.minePathTo != null) {
				level.destroyBlockProgress(golem.getId(), this.work.pos(), -1);
				Mines.release(golem);
				this.skip(this.minePathTo);
			}
			return;
		}
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
				MineLayout.Step blocker = this.findObstruction(level, golem);
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
		if (this.sectionFloors.containsKey(pos)) {
			this.walkwayHoles.add(pos);
		}
		// An ore it can now see starts a vein, dug out before the section goes on.
		for (Direction direction : Direction.values()) {
			BlockPos next = pos.relative(direction);
			if (!this.veinOres.contains(next) && !this.sectionDigs.contains(next) && level.isLoaded(next) && isOre(level.getBlockState(next))) {
				this.followVein(level, golem, next);
			}
		}
	}

	/**
	 * Finds the vein of face-connected ores at {@code seed}, which touches a block just dug, and queues up to
	 * {@link #VEIN_CAP} of them nearest first. Only ores outside the section, within its reach and the mine's limits count.
	 * The rest of the vein is remembered too, so digging next to it later doesn't start it again.
	 */
	private void followVein(ServerLevel level, CopperGolem golem, BlockPos seed) {
		Mines.Mine mine = mine(golem);
		if (mine == null || this.sectionBox == null) {
			return;
		}
		int reach = Eyes.radius(golem, VEIN_REACH);
		MineLimits limits = MineLimits.of(level);
		int queued = 0;
		ArrayDeque<BlockPos> open = new ArrayDeque<>();
		open.add(seed.immutable());
		this.veinOres.add(seed.immutable());
		while (!open.isEmpty()) {
			BlockPos pos = open.poll();
			if (chebyshev(this.sectionBox, pos) > reach || !limits.inRange(mine.entrance(), pos) || !limits.aboveFloor(pos)) {
				continue;
			}
			if (queued < VEIN_CAP) {
				this.oreDigs.add(pos);
				queued++;
			}
			for (Direction direction : Direction.values()) {
				BlockPos touching = pos.relative(direction);
				if (!this.veinOres.contains(touching) && !this.sectionDigs.contains(touching) && level.isLoaded(touching) && isOre(level.getBlockState(touching))) {
					this.veinOres.add(touching);
					open.add(touching);
				}
			}
		}
	}

	/**
	 * Where the golem stands to dig {@code ore}: on solid ground in an open block beside it, the one nearest to the
	 * golem. Null if there is none yet.
	 */
	private static @Nullable BlockPos standNextTo(ServerLevel level, CopperGolem golem, BlockPos ore) {
		BlockPos best = null;
		for (Direction direction : Direction.values()) {
			BlockPos open = ore.relative(direction);
			if (!level.isLoaded(open) || !level.getBlockState(open).getCollisionShape(level, open).isEmpty() || !level.getFluidState(open).isEmpty()) {
				continue;
			}
			BlockPos stand = floorBelow(level, open);
			boolean ground = level.getBlockState(stand.below()).isFaceSturdy(level, stand.below(), Direction.UP)
				&& level.getBlockState(stand).getCollisionShape(level, stand).isEmpty();
			// It steps up one block at most: a stand higher than that waits until digging lowers it.
			if (ground && stand.getY() <= golem.getBlockY() + 1 && Golems.isInReach(golem.getDimensions(golem.getPose()).makeBoundingBox(Vec3.atBottomCenterOf(stand)), ore)
				&& (best == null || stand.distToCenterSqr(golem.position()) < best.distToCenterSqr(golem.position()))) {
				best = stand;
			}
		}
		return best;
	}

	/** How many blocks {@code pos} lies outside {@code box}, along the farthest axis. */
	private static int chebyshev(BoundingBox box, BlockPos pos) {
		int x = Math.max(0, Math.max(box.minX() - pos.getX(), pos.getX() - box.maxX()));
		int y = Math.max(0, Math.max(box.minY() - pos.getY(), pos.getY() - box.maxY()));
		int z = Math.max(0, Math.max(box.minZ() - pos.getZ(), pos.getZ() - box.maxZ()));
		return Math.max(x, Math.max(y, z));
	}

	/**
	 * A block standing in a part of the tunnel that was already dug out (usually gravel or sand that fell in later),
	 * the closest one to the golem. Supports, rails and torches the golem built itself do not count.
	 */
	private MineLayout.@Nullable Step findObstruction(ServerLevel level, CopperGolem golem) {
		Mines.Mine mine = mine(golem);
		int index = claimed(golem, mine);
		if (index < 0) {
			return null;
		}
		int progress = mine.sections().get(index).progress();
		Set<BlockPos> built = new HashSet<>();
		for (MineLayout.Step step : this.steps) {
			// Rails and torches have no collision, so only fence posts and beams need leaving out.
			if (step.action() == MineLayout.Action.FENCE || step.action() == MineLayout.Action.BEAM) {
				built.add(step.pos());
			}
		}
		MineLayout.Step best = null;
		double bestDistance = Double.MAX_VALUE;
		for (int i = 0; i < Math.min(progress, this.steps.size()); i++) {
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
		BlockPos fluid = level.getFluidState(pos).isEmpty() ? null : pos;
		for (Direction direction : Direction.values()) {
			BlockPos next = pos.relative(direction);
			if (fluid == null && !level.getFluidState(next).isEmpty() && !golem.getBoundingBox().intersects(new AABB(next))) {
				fluid = next;
			}
		}
		if (fluid == null) {
			return true;
		}
		BlockedReason reason = level.getFluidState(fluid).is(FluidTags.LAVA) ? BlockedReason.LAVA : BlockedReason.WATER;
		if (reason == BlockedReason.LAVA && !this.extraWork) {
			// The check found none, so it came later: the section stops here.
			this.giveUp(golem, data, reason);
			return false;
		}
		return this.plugOrFlee(level, golem, data, fluid, reason);
	}

	private boolean plugOrFlee(ServerLevel level, CopperGolem golem, MinerData data, BlockPos pos, BlockedReason reason) {
		ItemStack filler = Load.of(golem).takeOne(MineBehavior::isFiller);
		if (filler.isEmpty()) {
			// Nothing to plug the hole with: too dangerous, so this bit stays undug and the golem carries on.
			this.giveUp(golem, data, reason);
			return false;
		}
		this.placeBlock(level, golem, pos, ((BlockItem) filler.getItem()).getBlock().defaultBlockState());
		golem.setState(CopperGolemState.DROPPING_ITEM);
		this.pauseTicks = 15;
		return false;
	}

	/**
	 * Extra work gives up only itself; a step of the section cuts the section short for {@code reason}, its rails still
	 * go down, and then it builds the Barrier.
	 */
	private void giveUp(CopperGolem golem, MinerData data, BlockedReason reason) {
		MineLayout.Step step = this.work;
		this.work = null;
		golem.setState(CopperGolemState.IDLE);
		Mines.Mine mine = mine(golem);
		int index = claimed(golem, mine);
		if (this.extraWork || index < 0) {
			this.unreachable.add(step.pos());
		} else {
			Mines.get((ServerLevel) golem.level()).cutShort(mine, index, step, reason);
			this.moveTo((ServerLevel) golem.level(), mine, index, step.resumeAt());
		}
	}

	private void build(ServerLevel level, CopperGolem golem, MineLayout.Step step) {
		BlockPos pos = step.pos();
		if (step.action() == MineLayout.Action.TORCH) {
			this.placeTorch(level, golem, pos, step.facing());
			return;
		}
		if (step.action() == MineLayout.Action.SIGN) {
			this.placeSign(level, golem, pos, step.facing());
			return;
		}
		if (step.action() == MineLayout.Action.FENCE && level.getBlockState(pos).is(BlockTags.RAILS)) {
			if (Load.of(golem).find(this.materialFor(MineLayout.Action.FENCE)).isEmpty()) {
				return;
			}
			Load.of(golem).addSupplies(new ItemStack(level.getBlockState(pos).getBlock()));
			level.removeBlock(pos, false);
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

	/** The Barrier's sign: "Blocked:" and the reason, on both sides, read from the section it belongs to. */
	private void placeSign(ServerLevel level, CopperGolem golem, BlockPos pos, Direction facing) {
		Mines.Mine mine = mine(golem);
		int index = claimed(golem, mine);
		if (index < 0) {
			return;
		}
		ItemStack sign = Load.of(golem).takeOne(this.materialFor(MineLayout.Action.SIGN));
		if (!(sign.getItem() instanceof BlockItem item) || !(item.getBlock() instanceof StandingSignBlock)) {
			return;
		}
		BlockedReason reason = mine.sections().get(index).reason().orElse(BlockedReason.PLAYER);
		this.placeBlock(level, golem, pos, item.getBlock().defaultBlockState().setValue(StandingSignBlock.ROTATION, RotationSegment.convertToSegment(facing)));
		if (level.getBlockEntity(pos) instanceof SignBlockEntity entity) {
			SignText text = SignText.EMPTY.asMutable()
				.setLine(1, Component.translatable("mine.bettercoppers.blocked"))
				.setLine(2, reason.text())
				.asImmutable();
			entity.setText(text, SignTextSlot.FRONT);
			entity.setText(text, SignTextSlot.BACK);
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
			case SIGN -> stack -> stack.is(ItemTags.SIGNS);
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
		this.minePathHome(golem);
	}

	/** The Mine Path back from the section the golem is in to the opening; none outside the mine. */
	private void minePathHome(CopperGolem golem) {
		this.minePathTo = null;
		this.minePath.clear();
		this.pathWatch.reset();
		MinerData data = MinerData.get(golem);
		Mines.Mine mine = data == null || data.mine() == null ? null
			: Mines.get((ServerLevel) golem.level()).find(data.mine(), data.mineAxis()).orElse(null);
		if (mine == null) {
			return;
		}
		mine.sections().stream()
			.filter(section -> section.box().inflatedBy(1).isInside(golem.blockPosition()))
			.findFirst()
			.ifPresent(section -> this.setMinePath(golem, MinePath.to(mine, section.start()).reversed()));
	}

	private void endReturn(CopperGolem golem) {
		this.mode = Mode.WORKING;
		this.skipped.clear();
		golem.clearOpenedChestPos();
		golem.setState(CopperGolemState.IDLE);
		if (this.stopAfterReturn) {
			// Nothing left to do: idle for a while like a sorting golem with an empty chest.
			golem.getBrain().setMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS, DONE_COOLDOWN);
		}
	}

	private void tickReturn(ServerLevel level, CopperGolem golem, MinerData data, BlockPos home) {
		if (!this.minePath.isEmpty() && this.followMinePath(golem) != Travel.ARRIVED) {
			// Still on the Mine Path. Once stuck on it, it walks straight for home instead.
			return;
		}
		if (!Golems.isInReach(golem, home) && this.pathWatch.tick(golem, Vec3.atCenterOf(home)) > (data.isStuck() ? STUCK_RETRY : Golems.MAX_TRAVEL_TICKS)) {
			// Not even that works: it shakes its head, says so, and keeps trying, shaking its head again every 100 ticks.
			data.setStuck(true);
			this.pathWatch.reset();
			Golems.stopInPlace(golem);
			golem.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
			golem.setState(CopperGolemState.GETTING_NO_ITEM);
			golem.playSound(SoundEvents.COPPER_GOLEM_ITEM_NO_GET);
			this.pauseTicks = 40;
			return;
		}
		if (Golems.isInReach(golem, home)) {
			data.setStuck(false);
		}
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
			Mines.Mine mine = MinerData.hasPickaxe(golem) ? Mines.of(level, golem, false) : null;
			int index = claimed(golem, mine);
			if (mine != null && !this.stopAfterReturn && !this.leaveIfBroken(level, golem, data, mine) && index >= 0) {
				this.restock(load, this.steps(mine.sections().get(index)), chest);
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
		// Rails stay in the pouch until the digging is done, so it takes one for every rail of the section.
		this.restockOne(load, chest, stack -> stack.is(Items.RAIL), (int) steps.stream().filter(step -> step.action() == MineLayout.Action.RAIL).count());
		this.restockOne(load, chest, stack -> stack.is(ItemTags.PLANKS), 32);
		this.restockOne(load, chest, stack -> stack.is(ItemTags.WOODEN_FENCES),
			steps.stream().anyMatch(step -> step.needs().fences() > 0 || step.action() == MineLayout.Action.FENCE) ? 16 : 0);
		// One sign along, for a Barrier.
		this.restockOne(load, chest, stack -> stack.is(ItemTags.SIGNS), 1);
	}

	/** Whether the golem carries (plus {@code chest}, if given, holds) {@code needs}. */
	private static boolean hasMaterials(CopperGolem golem, MineLayout.Needs needs, @Nullable Container chest) {
		Load load = Load.of(golem);
		return countWith(load, chest, stack -> stack.is(ItemTags.WOODEN_FENCES)) >= needs.fences()
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
		return stack.is(Items.TORCH) || stack.is(Items.RAIL) || stack.is(ItemTags.PLANKS) || stack.is(ItemTags.WOODEN_FENCES) || stack.is(ItemTags.SIGNS);
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

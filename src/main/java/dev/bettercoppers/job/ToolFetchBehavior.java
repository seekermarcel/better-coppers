package dev.bettercoppers.job;

import com.google.common.collect.ImmutableMap;
import dev.bettercoppers.api.TravelWatch;
import dev.bettercoppers.golem.ChestVisit;
import dev.bettercoppers.golem.Golems;
import dev.bettercoppers.home.Homes;
import dev.bettercoppers.runningcosts.RunningCosts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.CopperGolemState;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A golem whose tool broke keeps its job: once it has brought home what it carried, it looks for the same tool in its
 * home chest, then in every chest and barrel within {@link #SEARCH_RANGE} blocks of home, walks there and takes it.
 * With no tool anywhere it shakes its head, waits by its chest and looks again every half minute.
 */
public class ToolFetchBehavior extends Behavior<CopperGolem> {
	private static final int SEARCH_RANGE = 16;
	private static final int RETRY_TICKS = 600;
	private static final int TAKE_TICKS = 20;
	private static final int SHAKE_TICKS = 20;

	private @Nullable BlockPos source;
	private long nextSearch;
	private int ticks;
	private int shake;
	private final TravelWatch travel = new TravelWatch();

	public ToolFetchBehavior() {
		super(ImmutableMap.of(MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_ABSENT));
	}

	@Override
	protected boolean checkExtraStartConditions(ServerLevel level, CopperGolem golem) {
		return level.getGameTime() >= this.nextSearch && this.wantsTool(level, golem);
	}

	@Override
	protected boolean canStillUse(ServerLevel level, CopperGolem golem, long timestamp) {
		return !golem.isPanicking() && (this.shake > 0 || this.source != null && this.wantsTool(level, golem));
	}

	@Override
	protected boolean timedOut(long timestamp) {
		return false;
	}

	/** The load goes home first; the job behaviours do that by themselves. */
	private boolean wantsTool(ServerLevel level, CopperGolem golem) {
		if (Tools.lostTool(golem) == null || golem.isLeashed() || Homes.get(golem) == null || !RunningCosts.canWork(level, golem) || !Load.of(golem).hasNothingToBringHome()) {
			return false;
		}
		Jobs.JobKind kind = Jobs.of(golem);
		return kind == Jobs.JobKind.MINER || kind == Jobs.JobKind.WORKER;
	}

	@Override
	protected void start(ServerLevel level, CopperGolem golem, long timestamp) {
		Item tool = Tools.lostTool(golem);
		this.source = tool == null ? null : findTool(level, golem, tool);
		this.ticks = 0;
		this.travel.reset();
		if (this.source == null) {
			// Nothing anywhere: a disappointed head shake, then wait.
			golem.setState(CopperGolemState.GETTING_NO_ITEM);
			this.shake = SHAKE_TICKS;
			golem.playSound(SoundEvents.COPPER_GOLEM_ITEM_NO_GET);
			level.sendParticles(ParticleTypes.SMOKE, golem.getX(), golem.getY() + 1.1, golem.getZ(), 4, 0.15, 0.1, 0.15, 0.0);
			this.nextSearch = level.getGameTime() + RETRY_TICKS;
		}
	}

	@Override
	protected void stop(ServerLevel level, CopperGolem golem, long timestamp) {
		ChestVisit.close(golem, this.source != null && this.ticks > 0 ? Homes.containerAt(level, this.source) : null);
		this.source = null;
		this.shake = 0;
	}

	@Override
	protected void tick(ServerLevel level, CopperGolem golem, long timestamp) {
		if (this.shake > 0) {
			// The head shake: stay put and let it play out. The stop that follows leaves it idle.
			this.shake--;
			Golems.stopInPlace(golem);
			return;
		}
		BlockPos source = this.source;
		Item tool = Tools.lostTool(golem);
		Container container = source == null ? null : container(level, golem, source);
		if (source == null || tool == null || container == null) {
			this.source = null;
			return;
		}
		if (!new AABB(source).inflate(1.0, 1.5, 1.0).intersects(golem.getBoundingBox())) {
			if (this.travel.tick(golem, Vec3.atCenterOf(source)) > Golems.MAX_TRAVEL_TICKS) {
				this.source = null;
				this.nextSearch = level.getGameTime() + RETRY_TICKS;
				return;
			}
			BehaviorUtils.setWalkAndLookTargetMemories(golem, source, 0.8F * RunningCosts.speed(golem), 1);
			return;
		}
		this.ticks = ChestVisit.tick(golem, container, source, this.ticks, CopperGolemState.GETTING_ITEM);
		if (this.ticks < TAKE_TICKS) {
			return;
		}
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			ItemStack stack = container.getItem(slot);
			if (stack.is(tool)) {
				golem.setItemSlot(EquipmentSlot.MAINHAND, stack.split(1));
				golem.setGuaranteedDrop(EquipmentSlot.MAINHAND);
				container.setChanged();
				Tools.clearLost(golem);
				break;
			}
		}
		// Taken, or someone was quicker: either way this trip is over.
		this.source = null;
	}

	private static @Nullable Container container(ServerLevel level, CopperGolem golem, BlockPos pos) {
		return pos.equals(Homes.get(golem)) ? Homes.container(level, pos) : Homes.containerAt(level, pos);
	}

	/** The home chest if it has the tool, otherwise the nearest chest or barrel around home that has it. */
	private static @Nullable BlockPos findTool(ServerLevel level, CopperGolem golem, Item tool) {
		BlockPos home = Homes.get(golem);
		if (home == null) {
			return null;
		}
		if (hasTool(Homes.container(level, home), tool)) {
			return home;
		}
		AABB area = new AABB(home).inflate(SEARCH_RANGE, 6, SEARCH_RANGE);
		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;
		for (ChunkPos chunkPos : ChunkPos.rangeClosed(ChunkPos.containing(home), SEARCH_RANGE / 16 + 1).toList()) {
			LevelChunk chunk = level.getChunkSource().getChunkNow(chunkPos.x(), chunkPos.z());
			if (chunk == null) {
				continue;
			}
			for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
				BlockPos pos = blockEntity.getBlockPos();
				double distance = pos.distToCenterSqr(Vec3.atCenterOf(home));
				if (distance < bestDistance && area.contains(Vec3.atCenterOf(pos)) && hasTool(Homes.containerAt(level, pos), tool)) {
					best = pos;
					bestDistance = distance;
				}
			}
		}
		return best;
	}

	private static boolean hasTool(@Nullable Container container, Item tool) {
		if (container == null) {
			return false;
		}
		for (ItemStack stack : container) {
			if (stack.is(tool)) {
				return true;
			}
		}
		return false;
	}
}

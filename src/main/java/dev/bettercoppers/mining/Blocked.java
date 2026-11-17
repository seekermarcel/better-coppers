package dev.bettercoppers.mining;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

/**
 * Whether a rolled Mine Section is Blocked, checked once in one tick before it is dug, over its footprint (the cells
 * it digs) and a 1-block shell around it, the parent's last slice left out.
 */
public final class Blocked {
	/** More water cells than this block a section. */
	static final int WATER_CELLS = 4;
	/** More open shell cells than this are a cave. */
	static final int CAVE_CELLS = 4;

	/** What the check reads of the world. */
	public interface Site {
		/** Null where the chunk is not loaded. */
		@Nullable BlockState state(BlockPos pos);

		boolean seesSky(BlockPos pos);

		boolean inStructure(BlockPos pos);

		/** A miner's copper pickaxe may dig it. */
		boolean diggable(BlockState state);

		/** The world, dug with a copper pickaxe. */
		static Site of(ServerLevel level) {
			ItemStack pickaxe = new ItemStack(Items.COPPER_PICKAXE);
			return new Site() {
				@Override
				public @Nullable BlockState state(BlockPos pos) {
					return level.isLoaded(pos) ? level.getBlockState(pos) : null;
				}

				@Override
				public boolean seesSky(BlockPos pos) {
					// The heightmap, not sky light: light lags behind block changes, and lava or glass would let it through.
					return pos.getY() >= level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
				}

				@Override
				public boolean inStructure(BlockPos pos) {
					return level.structureManager().getStructureWithPieceAt(pos, structure -> true).isValid();
				}

				@Override
				public boolean diggable(BlockState state) {
					return MineBehavior.canDig(state, state.getDestroySpeed(EmptyBlockGetter.INSTANCE, BlockPos.ZERO), pickaxe);
				}
			};
		}
	}

	private Blocked() {
	}

	/** Every cell the check reads is loaded. Unloaded cells are no verdict: the end waits. */
	public static boolean loaded(Site site, Mines.Section section) {
		List<MineLayout.Step> steps = MineLayout.build(section);
		Set<BlockPos> footprint = footprint(steps);
		return footprint.stream().allMatch(pos -> site.state(pos) != null) && shell(footprint, section.start()).stream().allMatch(pos -> site.state(pos) != null);
	}

	/**
	 * What blocks {@code section}, or null if it can be dug. {@code taken} says whether a box overlaps a saved section or
	 * a Mine Entrance. The first section (parent -1) may meet the sky: no Surface, and open shell cells under the sky
	 * are no cave. Call {@link #loaded} first.
	 */
	public static @Nullable BlockedReason check(Site site, Mines.Section section, MineLimits limits, BlockPos beam, Predicate<BoundingBox> taken) {
		List<MineLayout.Step> steps = MineLayout.build(section);
		if (!steps.stream().allMatch(step -> limits.inRange(beam, step.pos()))) {
			return BlockedReason.TOO_FAR;
		}
		Set<BlockPos> footprint = footprint(steps);
		if (!footprint.stream().allMatch(limits::aboveFloor)) {
			return BlockedReason.TOO_DEEP;
		}
		if (taken.test(section.box())) {
			return BlockedReason.ANOTHER_TUNNEL;
		}
		boolean first = section.parent() < 0;
		if (!first && footprint.stream().anyMatch(pos -> site.seesSky(pos) || !footprint.contains(pos.above()) && site.seesSky(pos.above()))) {
			return BlockedReason.SURFACE;
		}
		Set<BlockPos> shell = shell(footprint, section.start());
		int water = 0;
		int open = 0;
		for (BlockPos pos : footprint) {
			FluidState fluid = site.state(pos).getFluidState();
			if (fluid.getType().isSame(Fluids.LAVA)) {
				return BlockedReason.LAVA;
			}
			water += fluid.getType().isSame(Fluids.WATER) ? 1 : 0;
		}
		for (BlockPos pos : shell) {
			BlockState state = site.state(pos);
			FluidState fluid = state.getFluidState();
			if (fluid.getType().isSame(Fluids.LAVA)) {
				return BlockedReason.LAVA;
			}
			water += fluid.getType().isSame(Fluids.WATER) ? 1 : 0;
			if (fluid.isEmpty() && state.getCollisionShape(EmptyBlockGetter.INSTANCE, pos).isEmpty() && !(first && site.seesSky(pos))) {
				open++;
			}
		}
		if (water > WATER_CELLS) {
			return BlockedReason.WATER;
		}
		if (open > CAVE_CELLS) {
			return BlockedReason.CAVE;
		}
		if (footprint.stream().anyMatch(site::inStructure)) {
			return BlockedReason.STRUCTURE;
		}
		for (BlockPos pos : footprint) {
			BlockState state = site.state(pos);
			if (!state.canBeReplaced() && !site.diggable(state)) {
				return BlockedReason.HARD_ROCK;
			}
		}
		return null;
	}

	private static Set<BlockPos> footprint(List<MineLayout.Step> steps) {
		Set<BlockPos> footprint = new HashSet<>();
		steps.stream().filter(step -> step.action() == MineLayout.Action.DIG).forEach(step -> footprint.add(step.pos()));
		return footprint;
	}

	/** The cells touching the footprint, edges and corners too, but none in the parent's last slice. */
	private static Set<BlockPos> shell(Set<BlockPos> footprint, Mines.OpenEnd start) {
		Set<BlockPos> shell = new HashSet<>();
		BlockPos behind = start.pos().relative(start.facing().getOpposite());
		for (BlockPos pos : footprint) {
			for (BlockPos near : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
				boolean parentSlice = near.get(start.facing().getAxis()) == behind.get(start.facing().getAxis());
				if (!footprint.contains(near) && !parentSlice) {
					shell.add(near.immutable());
				}
			}
		}
		return shell;
	}
}

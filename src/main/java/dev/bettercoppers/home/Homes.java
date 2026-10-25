package dev.bettercoppers.home;

import dev.bettercoppers.registry.ModAttachments;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.jspecify.annotations.Nullable;

/**
 * Every golem with a job belongs to exactly one copper chest, its home, which the player links by hand (see
 * {@link HomeLinking}). A golem without a home waits.
 */
public final class Homes {
	private Homes() {
	}

	/** The linked home chest, if any. */
	public static @Nullable BlockPos get(CopperGolem golem) {
		return golem.getAttached(ModAttachments.HOME);
	}

	public static void set(CopperGolem golem, @Nullable BlockPos home) {
		if (home == null) {
			golem.removeAttached(ModAttachments.HOME);
		} else {
			golem.setAttached(ModAttachments.HOME, home);
		}
	}

	/** The home chest's inventory, or null if the home is gone or not loaded. */
	public static @Nullable Container container(Level level, @Nullable BlockPos pos) {
		if (pos == null || !level.isLoaded(pos)) {
			return null;
		}
		BlockState state = level.getBlockState(pos);
		return state.getBlock() instanceof ChestBlock chestBlock && state.is(BlockTags.COPPER_CHESTS)
			? ChestBlock.getContainer(chestBlock, state, level, pos, true)
			: null;
	}

	/** The golem's home if it still is a copper chest. */
	public static @Nullable BlockPos valid(ServerLevel level, CopperGolem golem) {
		BlockPos home = get(golem);
		return container(level, home) != null ? home : null;
	}

	/** Any chest (copper chests too) or barrel can be a route's end or a golem's source. */
	public static boolean canEndRoute(BlockState state) {
		return state.getBlock() instanceof ChestBlock || state.is(Blocks.BARREL);
	}

	/** Any chest or barrel's inventory, double chests combined; null if it is gone, locked, blocked or not loaded. */
	public static @Nullable Container containerAt(Level level, BlockPos pos) {
		if (!level.isLoaded(pos)) {
			return null;
		}
		BlockState state = level.getBlockState(pos);
		if (!canEndRoute(state)) {
			return null;
		}
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity instanceof BaseContainerBlockEntity base && base.isLocked()) {
			return null;
		}
		if (state.getBlock() instanceof ChestBlock chestBlock) {
			return ChestBlock.getContainer(chestBlock, state, level, pos, false);
		}
		return blockEntity instanceof Container container ? container : null;
	}

	/** Both halves of a double chest, or just the position itself. */
	public static Set<BlockPos> sameContainer(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
			return Set.of(pos, ChestBlock.getConnectedBlockPos(pos, state));
		}
		return Set.of(pos);
	}
}

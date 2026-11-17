package dev.bettercoppers.mining;

import dev.bettercoppers.guide.Guide;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Mine Entrance: two posts of wooden fences, each 2 high on a solid block, 1 apart, with 3 planks across their tops.
 * It is named by its middle beam and the axis the beam runs along. Only a player completing one registers it in
 * {@link Mines}: nothing scans for frames, so ones from commands, structures or worldgen never count.
 */
public final class MineEntrance {
	private MineEntrance() {
	}

	/** Both posts and the beam still stand. The opening may hold anything, such as the Barrier of a closed first end. */
	public static boolean isIntact(BlockGetter level, BlockPos beam, Direction.Axis axis) {
		Direction side = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
		for (int along = -1; along <= 1; along++) {
			if (!level.getBlockState(beam.relative(side, along)).is(BlockTags.PLANKS)) {
				return false;
			}
		}
		for (int along = -1; along <= 1; along += 2) {
			BlockPos top = beam.relative(side, along).below();
			if (!level.getBlockState(top).is(BlockTags.WOODEN_FENCES) || !level.getBlockState(top.below()).is(BlockTags.WOODEN_FENCES)) {
				return false;
			}
		}
		return true;
	}

	/** A frame as a player must build it: intact, each post on a solid block and the opening free to walk through. */
	static boolean isComplete(BlockGetter level, BlockPos beam, Direction.Axis axis) {
		if (!isIntact(level, beam, axis)) {
			return false;
		}
		Direction side = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
		for (int along = -1; along <= 1; along += 2) {
			BlockPos ground = beam.relative(side, along).below(3);
			if (!level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP)) {
				return false;
			}
		}
		return level.getBlockState(beam.below()).getCollisionShape(level, beam.below()).isEmpty()
			&& level.getBlockState(beam.below(2)).getCollisionShape(level, beam.below(2)).isEmpty();
	}

	/** After a player placed a block: registers every frame that block completed, with the player's feedback. */
	public static void onPlaced(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState placed) {
		if (!placed.is(BlockTags.PLANKS) && !placed.is(BlockTags.WOODEN_FENCES)) {
			return;
		}
		for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
			Direction side = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
			for (int along = -1; along <= 1; along++) {
				for (int up = 0; up <= 2; up++) {
					BlockPos beam = pos.relative(side, along).above(up);
					if (isComplete(level, beam, axis)) {
						Mines.get(level).register(beam, axis);
						built(player, level, beam);
					}
				}
			}
		}
	}

	private static void built(ServerPlayer player, ServerLevel level, BlockPos beam) {
		level.playSound(null, beam, SoundEvents.AXE_STRIP.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, beam.getX() + 0.5, beam.getY() - 1.0, beam.getZ() + 0.5, 12, 0.4, 0.6, 0.4, 0.0);
		player.sendOverlayMessage(Component.translatable("message.bettercoppers.mine_entrance_built"));
		Guide.award(player, "mine_entrance");
	}
}

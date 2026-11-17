package dev.bettercoppers.test;

import dev.bettercoppers.mining.MineEntrance;
import dev.bettercoppers.mining.Mines;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** A Mine Entrance counts only when a player places its last block. The frame stands along X on a stone floor. */
public class MineEntranceGameTests {
	private static final BlockPos BEAM = new BlockPos(2, 4, 2);

	private static void floor(GameTestHelper h) {
		for (int x = 0; x < 5; x++) {
			for (int z = 0; z < 5; z++) {
				h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	/** Everything but the beam's east plank, in mixed woods. */
	private static void frameButOnePlank(GameTestHelper h) {
		floor(h);
		h.setBlock(new BlockPos(1, 2, 2), Blocks.OAK_FENCE);
		h.setBlock(new BlockPos(1, 3, 2), Blocks.SPRUCE_FENCE);
		h.setBlock(new BlockPos(3, 2, 2), Blocks.DARK_OAK_FENCE);
		h.setBlock(new BlockPos(3, 3, 2), Blocks.OAK_FENCE);
		h.setBlock(new BlockPos(1, 4, 2), Blocks.BIRCH_PLANKS);
		h.setBlock(new BlockPos(2, 4, 2), Blocks.OAK_PLANKS);
	}

	/** A player places the item against the top of the block below {@code rel}. */
	private static void place(GameTestHelper h, ServerPlayer player, Item item, BlockPos rel) {
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
		BlockPos below = h.absolutePos(rel.below());
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(below).add(0, 0.5, 0), Direction.UP, below, false);
		player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
	}

	private static boolean registered(GameTestHelper h) {
		return Mines.get(h.getLevel()).find(h.absolutePos(BEAM), Direction.Axis.X).isPresent();
	}

	@GameTest
	public void a_frame_a_player_completes_registers(GameTestHelper h) {
		frameButOnePlank(h);
		h.assertFalse(registered(h), "half a frame is no mine");
		place(h, h.makeMockServerPlayerInLevel(), Items.ACACIA_PLANKS, new BlockPos(3, 4, 2));
		h.assertTrue(registered(h), "the player's last plank completes the frame");
		h.assertTrue(MineEntrance.isIntact(h.getLevel(), h.absolutePos(BEAM), Direction.Axis.X), "a registered frame is intact");
		h.succeed();
	}

	@GameTest
	public void a_frame_from_setblock_does_not_register(GameTestHelper h) {
		frameButOnePlank(h);
		h.setBlock(new BlockPos(3, 4, 2), Blocks.OAK_PLANKS);
		h.assertTrue(MineEntrance.isIntact(h.getLevel(), h.absolutePos(BEAM), Direction.Axis.X), "the frame stands");
		h.assertFalse(registered(h), "commands, structures and worldgen place blocks without a player");
		h.succeed();
	}

	@GameTest
	public void a_frame_with_a_blocked_opening_does_not_register(GameTestHelper h) {
		frameButOnePlank(h);
		h.setBlock(new BlockPos(2, 2, 2), Blocks.STONE);
		place(h, h.makeMockServerPlayerInLevel(), Items.OAK_PLANKS, new BlockPos(3, 4, 2));
		h.assertFalse(registered(h), "the opening must be free");
		h.succeed();
	}

	@GameTest
	public void a_fence_in_the_opening_keeps_the_frame_intact(GameTestHelper h) {
		frameButOnePlank(h);
		place(h, h.makeMockServerPlayerInLevel(), Items.OAK_PLANKS, new BlockPos(3, 4, 2));
		h.setBlock(new BlockPos(2, 2, 2), Blocks.OAK_FENCE);
		h.assertTrue(MineEntrance.isIntact(h.getLevel(), h.absolutePos(BEAM), Direction.Axis.X), "a Barrier in the opening");
		h.setBlock(new BlockPos(3, 3, 2), Blocks.AIR);
		h.assertFalse(MineEntrance.isIntact(h.getLevel(), h.absolutePos(BEAM), Direction.Axis.X), "a post is gone");
		h.succeed();
	}
}

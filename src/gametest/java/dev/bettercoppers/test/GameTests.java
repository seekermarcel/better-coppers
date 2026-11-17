package dev.bettercoppers.test;

import dev.bettercoppers.mining.Mines;
import dev.bettercoppers.runningcosts.GolemRunningCosts;
import dev.bettercoppers.runningcosts.RunningCosts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Shared helpers for the game tests; the tests themselves live in the other classes of this package. */
public class GameTests {
	/** Spawns an adult copper golem at a position relative to the test origin. */
	/**
	 * A Mine Entrance frame along X with its middle beam at {@code beam}, registered as if a player had placed its last
	 * block ({@code setBlock} alone never registers one). Returns the beam's absolute position.
	 */
	public static BlockPos mineEntrance(GameTestHelper h, BlockPos beam) {
		for (int x = -1; x <= 1; x++) {
			h.setBlock(beam.offset(x, 0, 0), Blocks.OAK_PLANKS);
		}
		for (int x = -1; x <= 1; x += 2) {
			h.setBlock(beam.offset(x, -1, 0), Blocks.OAK_FENCE);
			h.setBlock(beam.offset(x, -2, 0), Blocks.OAK_FENCE);
		}
		BlockPos absolute = h.absolutePos(beam);
		// A mine saved here by an earlier run is not this test's.
		Mines.get(h.getLevel()).forget(absolute, Direction.Axis.X);
		Mines.get(h.getLevel()).register(absolute, Direction.Axis.X);
		return absolute;
	}

	public static CopperGolem golem(GameTestHelper h, BlockPos rel) {
		return h.spawn(EntityTypes.COPPER_GOLEM, rel);
	}

	/** Gives the golem full energy so it is allowed to work. */
	public static void energize(CopperGolem g) {
		RunningCosts.get(g).setEnergy(GolemRunningCosts.MAX_ENERGY);
	}

	/** How many of this item lie on the ground in the test area. */
	public static int dropped(GameTestHelper h, Item item) {
		return h.getLevel().getEntitiesOfClass(ItemEntity.class, h.getBounds(), entity -> entity.getItem().is(item)).stream()
			.mapToInt(entity -> entity.getItem().getCount()).sum();
	}

	/** Places a chest-like block and returns its container. */
	public static Container chest(GameTestHelper h, BlockPos rel, Block chestBlock) {
		h.setBlock(rel, chestBlock);
		return (Container) h.getBlockEntity(rel, net.minecraft.world.level.block.entity.BlockEntity.class);
	}
}

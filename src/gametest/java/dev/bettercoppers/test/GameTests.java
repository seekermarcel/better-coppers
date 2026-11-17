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
	/**
	 * A stone hill for a mine heading north from a Mine Entrance at (4, 3, 6): x 2..7, y 0..4, z -3..5. Its first
	 * Straight runs to z -2, past the test area, whose barrier walls would block it as hard rock.
	 */
	public static void hill(GameTestHelper h) {
		BlockPos.betweenClosed(new BlockPos(2, 0, -3), new BlockPos(7, 4, 5)).forEach(pos -> h.setBlock(pos, Blocks.STONE));
	}

	/**
	 * Open ground for a flat scene, x 0..7, z -4..7: stone from y -6 to 0 for stairs down to dig into instead of the
	 * void, and the test area's barrier roof taken off, so the open air above counts as under the sky.
	 */
	public static void ground(GameTestHelper h) {
		BlockPos.betweenClosed(new BlockPos(0, -6, -4), new BlockPos(7, 0, 7)).forEach(pos -> h.setBlock(pos, Blocks.STONE));
		BlockPos.betweenClosed(new BlockPos(0, 1, -4), new BlockPos(7, 32, 7)).forEach(pos -> {
			if (h.getBlockState(pos).is(Blocks.BARRIER)) {
				h.setBlock(pos, Blocks.AIR);
			}
		});
	}

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

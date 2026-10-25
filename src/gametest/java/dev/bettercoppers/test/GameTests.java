package dev.bettercoppers.test;

import dev.bettercoppers.upkeep.GolemUpkeep;
import dev.bettercoppers.upkeep.Upkeep;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.level.block.Block;

/** Shared helpers for the game tests; the tests themselves live in the other classes of this package. */
public class GameTests {
	/** Spawns an adult copper golem at a position relative to the test origin. */
	public static CopperGolem golem(GameTestHelper h, BlockPos rel) {
		return h.spawn(EntityTypes.COPPER_GOLEM, rel);
	}

	/** Gives the golem full energy so it is allowed to work. */
	public static void energize(CopperGolem g) {
		Upkeep.get(g).setEnergy(GolemUpkeep.MAX_ENERGY);
	}

	/** Places a chest-like block and returns its container. */
	public static Container chest(GameTestHelper h, BlockPos rel, Block chestBlock) {
		h.setBlock(rel, chestBlock);
		return (Container) h.getBlockEntity(rel, net.minecraft.world.level.block.entity.BlockEntity.class);
	}
}

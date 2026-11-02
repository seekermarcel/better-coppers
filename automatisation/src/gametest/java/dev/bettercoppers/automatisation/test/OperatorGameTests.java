package dev.bettercoppers.automatisation.test;

import dev.bettercoppers.api.BetterCoppersApi;
import dev.bettercoppers.automatisation.registry.AutoItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Only a player can make a golem an operator. */
public class OperatorGameTests {
	private static Container chest(GameTestHelper h, BlockPos rel, net.minecraft.world.level.block.Block block) {
		h.setBlock(rel, block);
		return (Container) h.getBlockEntity(rel, BlockEntity.class);
	}

	@GameTest(maxTicks = 1200)
	public void untouched_golem_with_wrench_stays_jobless(GameTestHelper h) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
		chest(h, new BlockPos(2, 1, 4), Blocks.COPPER_CHEST.weathering().unaffected()).setItem(0, new ItemStack(AutoItems.COPPER_WRENCH));
		Container target = chest(h, new BlockPos(6, 1, 4), Blocks.CHEST);
		target.setItem(0, new ItemStack(AutoItems.COPPER_WRENCH));
		CopperGolem golem = h.spawn(EntityTypes.COPPER_GOLEM, new BlockPos(4, 1, 4));
		h.onEachTick(() -> h.assertTrue(BetterCoppersApi.jobOf(golem) == null, "untouched golem became an operator"));
		h.succeedWhen(() -> h.assertTrue(target.countItem(AutoItems.COPPER_WRENCH) == 2, "wrench not sorted yet"));
	}
}

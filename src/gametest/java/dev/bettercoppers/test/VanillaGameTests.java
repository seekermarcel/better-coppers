package dev.bettercoppers.test;

import dev.bettercoppers.home.Homes;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** Golems without a job must behave exactly like vanilla. */
public class VanillaGameTests {
	private static final BlockPos SOURCE = new BlockPos(2, 1, 4);

	/** Copper chest with 4 cobblestone and a chest with 1; returns the chest. */
	private static Container sortScene(GameTestHelper h) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
		GameTests.chest(h, SOURCE, Blocks.COPPER_CHEST.weathering().unaffected()).setItem(0, new ItemStack(Items.COBBLESTONE, 4));
		Container target = GameTests.chest(h, new BlockPos(6, 1, 4), Blocks.CHEST);
		target.setItem(0, new ItemStack(Items.COBBLESTONE));
		return target;
	}

	@GameTest(maxTicks = 1200)
	public void jobless_golem_sorts_like_vanilla(GameTestHelper h) {
		Container target = sortScene(h);
		GameTests.golem(h, new BlockPos(4, 1, 4));
		h.succeedWhen(() -> h.assertTrue(target.countItem(Items.COBBLESTONE) == 5, "cobblestone not sorted yet"));
	}

	@GameTest(maxTicks = 1200)
	public void jobless_golem_with_home_sorts_like_vanilla(GameTestHelper h) {
		Container target = sortScene(h);
		CopperGolem golem = GameTests.golem(h, new BlockPos(4, 1, 4));
		Homes.set(golem, h.absolutePos(SOURCE));
		h.succeedWhen(() -> h.assertTrue(target.countItem(Items.COBBLESTONE) == 5, "cobblestone not sorted yet"));
	}
}

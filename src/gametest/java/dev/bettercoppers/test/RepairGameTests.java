package dev.bettercoppers.test;

import dev.bettercoppers.home.Homes;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Describes how a copper golem with a home patches up a hurt iron golem. */
public class RepairGameTests {
	private static final Block COPPER_CHEST = Blocks.COPPER_CHEST.weathering().unaffected();

	@GameTest(maxTicks = 1200)
	public void golem_repairs_iron_golem_from_chest(GameTestHelper h) {
		ServerLevel level = h.getLevel();
		level.clockManager().setTotalTicks(level.dimensionType().defaultClock().orElseThrow(), 1000L);
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
		BlockPos home = new BlockPos(1, 1, 1);
		Container chest = GameTests.chest(h, home, COPPER_CHEST);
		chest.setItem(0, new ItemStack(Items.IRON_INGOT, 8));
		IronGolem patient = h.spawn(EntityTypes.IRON_GOLEM, new BlockPos(5, 1, 5));
		patient.setHealth(40.0F);
		CopperGolem golem = GameTests.golem(h, new BlockPos(3, 1, 3));
		GameTests.energize(golem);
		Homes.set(golem, h.absolutePos(home));
		// A golem with only a home sorts like vanilla, and vanilla can lift the whole ingot stack into its hand first;
		// repairs then refuse to start (see report). The cooldown keeps vanilla away; repairing clears it.
		golem.getBrain().setMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS, 2000);
		// 60 health missing is two ingots: 25 each, until under half an ingot's worth is left.
		h.succeedWhen(() -> {
			h.assertTrue(patient.getHealth() >= 90.0F, "iron golem not repaired yet: " + patient.getHealth());
			h.assertTrue(chest.countItem(Items.IRON_INGOT) == 6, "ingots left in chest: " + chest.countItem(Items.IRON_INGOT));
		});
	}
}

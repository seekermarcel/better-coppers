package dev.bettercoppers.test;

import dev.bettercoppers.golem.HomeStroll;
import dev.bettercoppers.home.Homes;
import dev.bettercoppers.mixin.CopperGolemAccessor;
import dev.bettercoppers.upkeep.Upkeep;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;

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

	@GameTest
	public void only_a_golem_with_a_home_is_walked_back(GameTestHelper h) {
		CopperGolem golem = GameTests.golem(h, new BlockPos(7, 1, 7));
		golem.getBrain().setMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS, 400);
		h.assertFalse(new HomeStroll().tryStart(h.getLevel(), golem, 0L), "walked a golem nobody linked");
		Homes.set(golem, h.absolutePos(new BlockPos(6, 1, 6)));
		h.assertFalse(new HomeStroll().tryStart(h.getLevel(), golem, 0L), "walked a golem already near home");
		Homes.set(golem, h.absolutePos(new BlockPos(0, 1, 0)));
		h.assertTrue(new HomeStroll().tryStart(h.getLevel(), golem, 0L), "did not walk a golem far from home");
		h.assertTrue(golem.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET), "no walk target");
		h.succeed();
	}

	@GameTest(maxTicks = 300)
	public void zombie_ignores_jobless_golem(GameTestHelper h) {
		Zombie zombie = h.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 1, 1));
		zombie.setPersistenceRequired();
		GameTests.golem(h, new BlockPos(5, 1, 1));
		h.onEachTick(() -> h.assertTrue(!(zombie.getTarget() instanceof CopperGolem), "zombie targets a jobless golem"));
		h.runAfterDelay(200, () -> {
			h.assertTrue(zombie.isAlive(), "zombie died");
			h.succeed();
		});
	}

	/** Dispenser with an iron axe facing east, powered one tick after the golem (if any) stands in front. */
	private static DispenserBlockEntity axeDispenser(GameTestHelper h) {
		h.setBlock(new BlockPos(2, 1, 2), Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST));
		DispenserBlockEntity dispenser = h.getBlockEntity(new BlockPos(2, 1, 2), DispenserBlockEntity.class);
		dispenser.setItem(0, new ItemStack(Items.IRON_AXE));
		h.setBlock(new BlockPos(2, 2, 2), Blocks.REDSTONE_BLOCK);
		return dispenser;
	}

	@GameTest(maxTicks = 100)
	public void axe_dispenser_without_golem_ejects(GameTestHelper h) {
		axeDispenser(h);
		h.succeedWhen(() -> h.assertTrue(
			h.getLevel().getEntitiesOfClass(ItemEntity.class, h.getBounds(), item -> item.getItem().is(Items.IRON_AXE)).size() == 1,
			"axe not ejected"));
	}

	@GameTest(maxTicks = 100)
	public void axe_dispenser_scrapes_waxed_golem(GameTestHelper h) {
		CopperGolem golem = GameTests.golem(h, new BlockPos(3, 1, 2));
		((CopperGolemAccessor) golem).bettercoppers$setNextWeatheringTick(-2L);
		DispenserBlockEntity dispenser = axeDispenser(h);
		h.succeedWhen(() -> {
			h.assertTrue(!Upkeep.isWaxed(golem), "golem still waxed");
			h.assertTrue(dispenser.getItem(0).is(Items.IRON_AXE), "axe left the dispenser");
		});
	}
}

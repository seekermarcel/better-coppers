package dev.bettercoppers.test;

import dev.bettercoppers.combat.DefendCopperGolemsTargetGoal;
import dev.bettercoppers.golem.HomeStroll;
import dev.bettercoppers.home.Homes;
import dev.bettercoppers.mixin.CopperGolemAccessor;
import dev.bettercoppers.runningcosts.RunningCosts;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.IronGolem;
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

	/**
	 * Whether the goal picks this monster. Asks the goal itself: vanilla's own target goal sends an iron golem after a
	 * zombie anyway, so its target alone says nothing. 200 tries beat the 1-in-5 start roll (a miss is 4e-20). Checking
	 * for this very monster keeps out a monster from a test running next door.
	 */
	private static boolean defendsAgainst(IronGolem guard, Zombie zombie) {
		DefendCopperGolemsTargetGoal goal = new DefendCopperGolemsTargetGoal(guard);
		guard.setTarget(null);
		for (int i = 0; i < 200; i++) {
			if (goal.canUse()) {
				goal.start();
				if (guard.getTarget() == zombie) {
					return true;
				}
			}
		}
		return false;
	}

	/** Runs the checks once the iron golem stands on the floor, as it cannot path to a monster before that. */
	private static void guardScene(GameTestHelper h, boolean job, GuardCheck check) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
		IronGolem guard = h.spawn(EntityTypes.IRON_GOLEM, new BlockPos(1, 1, 1));
		Zombie zombie = h.spawn(EntityTypes.ZOMBIE, new BlockPos(6, 1, 1));
		zombie.setPersistenceRequired();
		CopperGolem golem = GameTests.golem(h, new BlockPos(6, 1, 6));
		if (job) {
			golem.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.BUNDLE));
		}
		h.runAfterDelay(5, () -> {
			check.run(guard, zombie, golem);
			h.succeed();
		});
	}

	private interface GuardCheck {
		void run(IronGolem guard, Zombie zombie, CopperGolem golem);
	}

	@GameTest(maxTicks = 100)
	public void iron_golem_does_not_defend_jobless_golem(GameTestHelper h) {
		guardScene(h, false, (guard, zombie, golem) -> {
			zombie.setTarget(golem);
			h.assertFalse(defendsAgainst(guard, zombie), "iron golem goes for a monster hunting a jobless golem");
			zombie.setTarget(null);
			golem.setLastHurtByMob(zombie);
			h.assertFalse(defendsAgainst(guard, zombie), "iron golem goes for a monster that hit a jobless golem");
		});
	}

	@GameTest(maxTicks = 100)
	public void iron_golem_defends_golem_with_a_job(GameTestHelper h) {
		guardScene(h, true, (guard, zombie, golem) -> {
			zombie.setTarget(golem);
			h.assertTrue(defendsAgainst(guard, zombie), "iron golem ignores a monster hunting a golem with a job");
			zombie.setTarget(null);
			golem.setLastHurtByMob(zombie);
			h.assertTrue(defendsAgainst(guard, zombie), "iron golem ignores a monster that hit a golem with a job");
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
			h.assertTrue(!RunningCosts.isWaxed(golem), "golem still waxed");
			h.assertTrue(dispenser.getItem(0).is(Items.IRON_AXE), "axe left the dispenser");
		});
	}
}

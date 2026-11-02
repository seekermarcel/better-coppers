package dev.bettercoppers.test;

import dev.bettercoppers.home.Homes;
import dev.bettercoppers.repair.RepairBehavior;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

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
		// 60 health missing is two ingots: 25 each, until under half an ingot's worth is left.
		h.succeedWhen(() -> {
			h.assertTrue(patient.getHealth() >= 90.0F, "iron golem not repaired yet: " + patient.getHealth());
			// Vanilla may have lifted the ingots into the hand first: only the two used are gone.
			int left = chest.countItem(Items.IRON_INGOT) + (golem.getMainHandItem().is(Items.IRON_INGOT) ? golem.getMainHandItem().getCount() : 0);
			h.assertTrue(left == 6, "ingots left: " + left);
		});
	}

	/** A golem with only a home, holding {@code hand}. The cooldown keeps vanilla from moving it before the test looks. */
	private static CopperGolem homeOnlyGolem(GameTestHelper h, ItemStack hand) {
		h.getLevel().clockManager().setTotalTicks(h.getLevel().dimensionType().defaultClock().orElseThrow(), 1000L);
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
		BlockPos home = new BlockPos(1, 1, 1);
		GameTests.chest(h, home, COPPER_CHEST);
		CopperGolem golem = GameTests.golem(h, new BlockPos(3, 1, 3));
		GameTests.energize(golem);
		Homes.set(golem, h.absolutePos(home));
		golem.setItemSlot(EquipmentSlot.MAINHAND, hand);
		golem.getBrain().setMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS, 2000);
		return golem;
	}

	private static IronGolem hurtIronGolem(GameTestHelper h) {
		IronGolem patient = h.spawn(EntityTypes.IRON_GOLEM, new BlockPos(5, 1, 5));
		patient.setHealth(40.0F);
		return patient;
	}

	@GameTest(maxTicks = 1200)
	public void home_only_golem_repairs_with_iron_in_its_hand_and_an_empty_chest(GameTestHelper h) {
		homeOnlyGolem(h, new ItemStack(Items.IRON_INGOT, 8));
		IronGolem patient = hurtIronGolem(h);
		h.succeedWhen(() -> h.assertTrue(patient.getHealth() >= 90.0F, "iron golem not repaired yet: " + patient.getHealth()));
	}

	@GameTest(maxTicks = 400)
	public void home_only_golem_with_other_cargo_does_not_start_a_repair(GameTestHelper h) {
		CopperGolem golem = homeOnlyGolem(h, new ItemStack(Items.COBBLESTONE, 8));
		((Container) h.getBlockEntity(new BlockPos(1, 1, 1), BlockEntity.class)).setItem(0, new ItemStack(Items.IRON_INGOT, 8));
		IronGolem patient = hurtIronGolem(h);
		h.startSequence().thenIdle(300).thenExecute(() -> {
			h.assertTrue(patient.getHealth() == 40.0F, "repaired anyway: " + patient.getHealth());
			h.assertTrue(golem.getMainHandItem().is(Items.COBBLESTONE), "cargo changed");
		}).thenSucceed();
	}

	@GameTest(maxTicks = 1200)
	public void leftover_iron_goes_back_to_the_hand(GameTestHelper h) {
		CopperGolem golem = homeOnlyGolem(h, new ItemStack(Items.IRON_INGOT, 8));
		IronGolem patient = hurtIronGolem(h);
		h.succeedWhen(() -> {
			h.assertTrue(patient.getHealth() >= 90.0F, "iron golem not repaired yet: " + patient.getHealth());
			h.assertTrue(RepairBehavior.carried(golem) == 0, "iron still carried: " + RepairBehavior.carried(golem));
			h.assertTrue(golem.getMainHandItem().is(Items.IRON_INGOT) && golem.getMainHandItem().getCount() == 6, "hand: " + golem.getMainHandItem());
		});
	}
}

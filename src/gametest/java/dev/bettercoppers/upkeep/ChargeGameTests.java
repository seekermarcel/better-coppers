package dev.bettercoppers.upkeep;

import dev.bettercoppers.api.BetterCoppersApi;
import dev.bettercoppers.job.JobHandover;
import dev.bettercoppers.job.WorkerData;
import dev.bettercoppers.registry.ModBlocks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** Charging. In this package so it can drive the behaviour itself. */
public class ChargeGameTests {
	/** A job ended on the pad: nothing there can charge the golem any more, so the behaviour must let go. */
	@GameTest(maxTicks = 100)
	public void ending_the_job_on_a_pad_frees_the_golem(GameTestHelper h) {
		h.setBlock(new BlockPos(2, 0, 2), ModBlocks.CHARGING_PAD);
		CopperGolem golem = h.spawn(EntityTypes.COPPER_GOLEM, new BlockPos(2, 1, 2));
		Player player = h.makeMockPlayer(GameType.SURVIVAL);
		BetterCoppersApi.giveTool(player, golem, new ItemStack(Items.COPPER_HOE));
		WorkerData.getOrCreate(golem);
		Upkeep.get(golem).setEnergy(0);
		ChargeBehavior charge = new ChargeBehavior();
		long now = h.getLevel().getGameTime();
		h.assertTrue(charge.tryStart(h.getLevel(), golem, now), "did not start charging");
		charge.tickOrStop(h.getLevel(), golem, now);
		h.assertTrue(charge.getStatus() == Behavior.Status.RUNNING, "stopped while working");
		JobHandover.clear(player, golem);
		Upkeep.get(golem).setEnergy(10);
		charge.tickOrStop(h.getLevel(), golem, now + 1);
		h.assertTrue(charge.getStatus() == Behavior.Status.STOPPED, "still held on the pad");
		h.succeed();
	}
}

package dev.bettercoppers.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MinePatternTest {
	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void aSettingWithAStepAsWideAsItsRangeHasOnlyTwoValues() {
		MinePattern.Setting width = new MinePattern.Setting("hall_width", 9, 17, 8, 9);
		assertEquals(17, width.moved(9, 1));
		assertEquals(17, width.moved(17, 1));
		assertEquals(9, width.moved(17, -1));
		assertEquals(9, width.moved(9, -1));
	}

	@Test
	void mineshaftHasNoSettingAndGoesTheFullLength() {
		assertFalse(MinePattern.MINESHAFT.hasSetting());
		assertEquals(0, MinePattern.MINESHAFT.setting().moved(0, 1));
		assertEquals(MinePlan.MAX_LENGTH, MinePattern.MINESHAFT.maxLength());
	}

	@Test
	void aPlanSavesItsSetting() {
		MinePlan plan = MinePlan.blank(MinePattern.MINESHAFT, 16, 3);
		MinePlan saved = MinePlan.CODEC.parse(JsonOps.INSTANCE, MinePlan.CODEC.encodeStart(JsonOps.INSTANCE, plan).getOrThrow()).getOrThrow();
		assertEquals(3, saved.setting());
		assertEquals(3, saved.marked(BlockPos.ZERO, Direction.NORTH).withProgress(4).unstarted().setting());
	}

	@Test
	void aBlankPlanStartsAtThePatternsDefaultSetting() {
		assertEquals(0, MinePlan.blank(MinePattern.MINESHAFT, 16).setting());
	}
}

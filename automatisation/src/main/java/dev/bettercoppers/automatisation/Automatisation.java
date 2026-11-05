package dev.bettercoppers.automatisation;

import dev.bettercoppers.api.BetterCoppersApi;
import dev.bettercoppers.automatisation.charging.ChargingStationBlock;
import dev.bettercoppers.automatisation.golem.OperatorBehavior;
import dev.bettercoppers.automatisation.golem.Operators;
import dev.bettercoppers.automatisation.registry.AutoAttachments;
import dev.bettercoppers.automatisation.registry.AutoBlockEntities;
import dev.bettercoppers.automatisation.registry.AutoBlocks;
import dev.bettercoppers.automatisation.registry.AutoItems;
import dev.bettercoppers.automatisation.upgrade.Upgrades;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Better Coppers Automatisation: belts, funnels, chutes, presses, saws, grabber arms and a golem assembler. There is
 * no power network: every machine is worked by hand, by a copper golem holding a copper wrench. What the factory makes
 * goes back into the golems: upgrades, supplies for their jobs, bigger loads and new golems.
 */
public class Automatisation implements ModInitializer {
	public static final String MOD_ID = "bettercoppers_automatisation";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** Golem Handbook chapters, in order; their texts are in the lang files. */
	private static final String[] HANDBOOK_CHAPTERS = {"machines", "belts", "press", "arm", "wear", "upgrades", "assembler"};

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		AutoBlocks.init();
		AutoBlockEntities.init();
		AutoItems.init();
		AutoAttachments.init();
		BetterCoppersApi.registerJob(Operators.JOB);
		BetterCoppersApi.registerBehavior(OperatorBehavior::new);
		BetterCoppersApi.registerLinkTarget(Operators.LINK);
		BetterCoppersApi.registerUpgrade(Upgrades.UPGRADE);
		BetterCoppersApi.registerChargingSpot(ChargingStationBlock.PAD);
		UseEntityCallback.EVENT.register(Upgrades::onUseEntity);
		UseEntityCallback.EVENT.register(Operators::onUseEntity);
		for (String chapter : HANDBOOK_CHAPTERS) {
			BetterCoppersApi.registerHandbookChapter(id(chapter));
		}
		LOGGER.info("Better Coppers Automatisation initialised");
	}
}

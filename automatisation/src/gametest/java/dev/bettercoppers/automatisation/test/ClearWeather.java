package dev.bettercoppers.automatisation.test;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.world.level.gamerules.GameRules;

/** A thunderstorm counts as night and sends every working golem to rest, which fails whichever tests are running. */
public class ClearWeather implements ModInitializer {
	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			server.getGameRules().set(GameRules.ADVANCE_WEATHER, false, server);
			// The test world is kept between runs, so a storm from an earlier run may still be going.
			server.overworld().resetWeatherCycle();
			server.overworld().setRainLevel(0.0F);
			server.overworld().setThunderLevel(0.0F);
		});
	}
}

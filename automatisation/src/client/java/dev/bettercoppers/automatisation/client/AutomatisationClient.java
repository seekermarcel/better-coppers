package dev.bettercoppers.automatisation.client;

import dev.bettercoppers.automatisation.client.render.AssemblerRenderer;
import dev.bettercoppers.automatisation.client.render.BeltDriveRenderer;
import dev.bettercoppers.automatisation.client.render.GrabberArmRenderer;
import dev.bettercoppers.automatisation.client.render.PressRenderer;
import dev.bettercoppers.automatisation.client.render.SawRenderer;
import dev.bettercoppers.automatisation.registry.AutoBlockEntities;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public class AutomatisationClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BlockEntityRenderers.register(AutoBlockEntities.BELT_DRIVE, BeltDriveRenderer::new);
		BlockEntityRenderers.register(AutoBlockEntities.PRESS, PressRenderer::new);
		BlockEntityRenderers.register(AutoBlockEntities.SAW, SawRenderer::new);
		BlockEntityRenderers.register(AutoBlockEntities.GRABBER_ARM, GrabberArmRenderer::new);
		BlockEntityRenderers.register(AutoBlockEntities.GOLEM_ASSEMBLER, AssemblerRenderer::new);
	}
}

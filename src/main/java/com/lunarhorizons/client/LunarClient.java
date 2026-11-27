package com.lunarhorizons.client;

import com.lunarhorizons.LunarHorizons;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;

public final class LunarClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        RocketPilotHud.register();
        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register(OxygenHud::render);
        EntityRendererRegistry.register(LunarHorizons.ROCKET_ENTITY, RocketRenderer::new);
        EntityRendererRegistry.register(LunarHorizons.METEOR_ENTITY,MeteorRenderer::new);
        EntityRendererRegistry.register(LunarHorizons.HUGE_METEOR_ENTITY,MeteorRenderer::new);
        DimensionRenderingRegistry.registerSkyRenderer(LunarHorizons.MOON, LunarSky::render);
        DimensionRenderingRegistry.registerCloudRenderer(LunarHorizons.MOON, context -> {});
    }
}

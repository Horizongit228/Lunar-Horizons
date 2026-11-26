package com.lunarhorizons.client;

import com.lunarhorizons.LunarHorizons;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;

public final class LunarClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        DimensionRenderingRegistry.registerSkyRenderer(LunarHorizons.MOON, LunarSky::render);
        DimensionRenderingRegistry.registerCloudRenderer(LunarHorizons.MOON, context -> {});
    }
}

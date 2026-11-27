package com.lunarhorizons.client;

import com.lunarhorizons.LunarHorizons;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;

public final class LunarClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        RocketPilotHud.register();
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(com.lunarhorizons.ColonyStatus.ID,(payload,context)->context.client().execute(()->OxygenHud.habitat=payload.breathable()));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler,mc)->OxygenHud.habitat=false);
        net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(LunarHorizons.PRESSURE_GLASS,net.minecraft.client.render.RenderLayer.getTranslucent());
        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register(OxygenHud::render);
        EntityRendererRegistry.register(LunarHorizons.ROCKET_ENTITY, RocketRenderer::new);
        EntityRendererRegistry.register(LunarHorizons.METEOR_ENTITY,MeteorRenderer::new);
        EntityRendererRegistry.register(LunarHorizons.HUGE_METEOR_ENTITY,MeteorRenderer::new);
        DimensionRenderingRegistry.registerSkyRenderer(LunarHorizons.MOON, LunarSky::render);
        DimensionRenderingRegistry.registerCloudRenderer(LunarHorizons.MOON, context -> {});
    }
}

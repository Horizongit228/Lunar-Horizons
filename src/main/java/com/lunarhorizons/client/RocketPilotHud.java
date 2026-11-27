package com.lunarhorizons.client;

import com.lunarhorizons.LunarHorizons;
import com.lunarhorizons.RocketControl;
import com.lunarhorizons.RocketEntity;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

final class RocketPilotHud {
    private static KeyBinding launch,destination,cycle;
    static void register(){
        launch=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.lunar_horizons.launch",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_R,"category.lunar_horizons"));
        destination=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.lunar_horizons.destination",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_G,"category.lunar_horizons"));
        cycle=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.lunar_horizons.destination_cycle",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_H,"category.lunar_horizons"));
        ClientTickEvents.END_CLIENT_TICK.register(mc->{
            while(destination.wasPressed())if(mc.player!=null&&mc.currentScreen==null&&mc.player.getVehicle() instanceof RocketEntity r&&r.canChooseRoute())mc.setScreen(new RocketRouteScreen(r));
            while(cycle.wasPressed())if(mc.player!=null&&mc.currentScreen==null&&mc.player.getVehicle() instanceof RocketEntity r&&r.canChooseRoute()&&ClientPlayNetworking.canSend(com.lunarhorizons.RocketDestination.ID))ClientPlayNetworking.send(com.lunarhorizons.RocketDestination.INSTANCE);
            while(launch.wasPressed())if(mc.player!=null&&mc.currentScreen==null&&mc.player.getVehicle() instanceof RocketEntity&&ClientPlayNetworking.canSend(RocketControl.ID))
                ClientPlayNetworking.send(RocketControl.INSTANCE);
        });
        HudRenderCallback.EVENT.register(RocketPilotHud::render);
    }
    static void render(DrawContext ctx,RenderTickCounter ticks){
        MinecraftClient mc=MinecraftClient.getInstance();
        if(mc.player==null||mc.options.hudHidden||!(mc.player.getVehicle() instanceof RocketEntity r))return;
        int width=184,x=Math.max(2,ctx.getScaledWindowWidth()-width-10),y=10;
        int fuel=0;for(int i=0;i<36;i++){var stack=mc.player.getInventory().getStack(i);if(stack.isOf(LunarHorizons.FUEL))fuel+=stack.getCount();}
        String phase=switch(r.phase()){case RocketEntity.READY->"ready";case RocketEntity.COUNTDOWN->"countdown";case RocketEntity.ASCENT->"ascent";case RocketEntity.DESCENT->"descent";default->"idle";};
        int color=r.enginesOn()?0xfff4bc4d:0xff59cfe8;
        ctx.fill(x,y,x+width,y+116,0xe0182332);
        ctx.fill(x+1,y+1,x+width-1,y+2,color);
        ctx.drawTextWithShadow(mc.textRenderer,Text.translatable(r.advanced()?"hud.lunar_horizons.advanced_rocket":"hud.lunar_horizons.rocket"),x+6,y+6,color);
        Text status=Text.translatable("hud.lunar_horizons.rocket_"+phase,r.countdownSeconds());
        ctx.drawTextWithShadow(mc.textRenderer,status,x+6,y+18,0xffe6edf2);
        ctx.drawTextWithShadow(mc.textRenderer,Text.translatable("hud.lunar_horizons.rocket_altitude",r.flightAltitude()),x+6,y+30,0xffc7d7e7);
        ctx.drawTextWithShadow(mc.textRenderer,Text.translatable("hud.lunar_horizons.rocket_speed",String.format(java.util.Locale.ROOT,"%+.1f",r.flightSpeedTenths()/10.0)),x+6,y+42,0xffc7d7e7);
        ctx.drawTextWithShadow(mc.textRenderer,Text.translatable("hud.lunar_horizons.rocket_reserve",r.fuelReserve(),r.fuelCapacity()),x+6,y+54,0xffc7d7e7);
        for(int i=0;i<r.fuelCapacity();i++){int cell=172/r.fuelCapacity(),left=x+6+i*cell;ctx.fill(left,y+66,left+cell-2,y+72,i<r.fuelReserve()?0xff72b8bd:0xff354653);}
        ctx.drawTextWithShadow(mc.textRenderer,Text.translatable("hud.lunar_horizons.rocket_fuel",fuel),x+6,y+78,0xffc7d7e7);
        ctx.drawTextWithShadow(mc.textRenderer,Text.translatable("hud.lunar_horizons.destination_"+r.destination()),x+6,y+90,0xff80d2ed);
        if(r.canChooseRoute())ctx.drawTextWithShadow(mc.textRenderer,Text.translatable("hud.lunar_horizons.destination_hint",destination.getBoundKeyLocalizedText()),x+6,y+102,0xffc7d7e7);
        if(r.phase()==RocketEntity.ASCENT)
            ctx.drawCenteredTextWithShadow(mc.textRenderer,Text.translatable("hud.lunar_horizons.abort_hint",launch.getBoundKeyLocalizedText()),ctx.getScaledWindowWidth()/2,ctx.getScaledWindowHeight()-74,0xffffbf72);
        if(r.phase()==RocketEntity.READY||r.phase()==RocketEntity.COUNTDOWN)
            ctx.drawCenteredTextWithShadow(mc.textRenderer,Text.translatable(r.phase()==RocketEntity.READY?"hud.lunar_horizons.launch_hint":"hud.lunar_horizons.cancel_hint",launch.getBoundKeyLocalizedText()),ctx.getScaledWindowWidth()/2,ctx.getScaledWindowHeight()-74,0xff8be1ef);
    }
}

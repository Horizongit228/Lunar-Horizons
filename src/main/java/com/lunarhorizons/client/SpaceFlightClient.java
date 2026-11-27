package com.lunarhorizons.client;
import com.lunarhorizons.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
final class SpaceFlightClient {
    static void register(){
        ClientTickEvents.END_CLIENT_TICK.register(mc->{
            if(mc.player==null||(!SpaceFlight.eligible(mc.player)||OxygenHud.habitat))return;
            int flags=0;
            if(mc.currentScreen==null){
                if(mc.options.forwardKey.isPressed())flags|=1;
                if(mc.options.backKey.isPressed())flags|=2;
                if(mc.options.leftKey.isPressed())flags|=4;
                if(mc.options.rightKey.isPressed())flags|=8;
                if(mc.options.jumpKey.isPressed())flags|=16;
                if(mc.options.sneakKey.isPressed())flags|=32;
                if(mc.options.sprintKey.isPressed())flags|=64;
            }
            if(ClientPlayNetworking.canSend(SpaceFlight.Input.ID))ClientPlayNetworking.send(new SpaceFlight.Input(flags));
            if(flags!=0)mc.player.setVelocity(SpaceFlight.velocity(mc.player,flags));
            mc.player.fallDistance=0;
        });
        HudRenderCallback.EVENT.register((ctx,ticks)->{
            MinecraftClient mc=MinecraftClient.getInstance();
            if(mc.player==null||mc.options.hudHidden||(!SpaceFlight.eligible(mc.player)||OxygenHud.habitat))return;
            int x=ctx.getScaledWindowWidth()-164,y=10;
            ctx.fill(x,y,x+154,y+66,0xe0182332);
            ctx.drawTextWithShadow(mc.textRenderer,Text.translatable("hud.lunar_horizons.eva"),x+5,y+5,0xff83c2c7);
            ctx.drawTextWithShadow(mc.textRenderer,Text.translatable("hud.lunar_horizons.eva_speed",String.format(java.util.Locale.ROOT,"%.1f",mc.player.getVelocity().length()*20)),x+5,y+17,0xffe6edf2);
            ctx.drawTextWithShadow(mc.textRenderer,Text.translatable(SpaceFlight.canThrust(mc.player)?"hud.lunar_horizons.eva_thrust":"hud.lunar_horizons.eva_no_suit"),x+5,y+29,0xffe6edf2);
            int dist=(int)mc.player.getPos().distanceTo(new net.minecraft.util.math.Vec3d(0,113,-46));
            ctx.drawTextWithShadow(mc.textRenderer,Text.translatable("hud.lunar_horizons.eva_station",dist),x+5,y+42,0xff83c2c7);
            ctx.drawCenteredTextWithShadow(mc.textRenderer,Text.translatable("hud.lunar_horizons.eva_controls"),ctx.getScaledWindowWidth()/2,ctx.getScaledWindowHeight()-74,0xffa4c6d6);
        });
    }
}

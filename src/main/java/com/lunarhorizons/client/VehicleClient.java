package com.lunarhorizons.client;
import com.lunarhorizons.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.text.Text;
final class VehicleClient {
    private static Perspective previous;
    private static int gap;
    static void register(){
        ClientTickEvents.END_CLIENT_TICK.register(mc->{
            if(mc.player==null){return;}
            var vehicle=mc.player.getVehicle();
            boolean riding=vehicle instanceof RocketEntity||vehicle instanceof RoverEntity;
            if(riding){
                if(previous==null){previous=mc.options.getPerspective();mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);}
                gap=0;
            }else if(previous!=null&&++gap>=8){mc.options.setPerspective(previous);previous=null;gap=0;}
            if(vehicle instanceof RoverEntity&&ClientPlayNetworking.canSend(RoverControl.ID)){
                int input=0;
                if(mc.currentScreen==null){
                    if(mc.options.forwardKey.isPressed())input|=1;
                    if(mc.options.backKey.isPressed())input|=2;
                    if(mc.options.leftKey.isPressed())input|=4;
                    if(mc.options.rightKey.isPressed())input|=8;
                    if(mc.options.jumpKey.isPressed())input|=16;
                    if(mc.options.sprintKey.isPressed())input|=32;
                }
                ClientPlayNetworking.send(new RoverControl(input));
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler,mc)->{if(previous!=null)mc.options.setPerspective(previous);previous=null;gap=0;});
        HudRenderCallback.EVENT.register((ctx,ticks)->{
            var mc=MinecraftClient.getInstance();
            if(mc.player==null||mc.options.hudHidden||!(mc.player.getVehicle() instanceof RoverEntity r))return;
            int x=Math.max(2,ctx.getScaledWindowWidth()-182),y=10;
            int status=r.status();int color=(status&RoverEntity.LOW_ENERGY)!=0?0xffffa66e:0xff83c2c7;
            ctx.fill(x,y,x+172,y+68,0xe0182332);
            ctx.fill(x+1,y+1,x+171,y+2,color);
            ctx.drawTextWithShadow(mc.textRenderer,Text.translatable(r.advanced()?"hud.lunar_horizons.advanced_rover":"hud.lunar_horizons.rover"),x+6,y+5,0xff83c2c7);
            ctx.drawTextWithShadow(mc.textRenderer,Text.translatable("hud.lunar_horizons.rover_speed",String.format(java.util.Locale.ROOT,"%.1f",r.speed()/10.0)),x+6,y+17,0xffe6edf2);
            ctx.drawTextWithShadow(mc.textRenderer,Text.translatable("hud.lunar_horizons.rover_energy",r.energy(),r.capacity()),x+6,y+29,0xffe6edf2);
            ctx.fill(x+6,y+41,x+166,y+47,0xff354653);
            ctx.fill(x+6,y+41,x+6+160*r.energy()/r.capacity(),y+47,color);
            ctx.drawTextWithShadow(mc.textRenderer,Text.translatable((status&RoverEntity.LOW_ENERGY)!=0?"hud.lunar_horizons.rover_low":(status&RoverEntity.PRECISION)!=0?"hud.lunar_horizons.rover_precision":"hud.lunar_horizons.rover_drive"),x+6,y+54,color);
        });
    }
}

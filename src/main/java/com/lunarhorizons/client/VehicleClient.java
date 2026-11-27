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
                }
                ClientPlayNetworking.send(new RoverControl(input));
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler,mc)->{if(previous!=null)mc.options.setPerspective(previous);previous=null;gap=0;});
        HudRenderCallback.EVENT.register((ctx,ticks)->{
            var mc=MinecraftClient.getInstance();
            if(mc.player==null||mc.options.hudHidden||!(mc.player.getVehicle() instanceof RoverEntity r))return;
            int x=ctx.getScaledWindowWidth()-160,y=10;
            ctx.fill(x,y,x+150,y+50,0xe0182332);
            ctx.drawTextWithShadow(mc.textRenderer,Text.translatable(r.advanced()?"hud.lunar_horizons.advanced_rover":"hud.lunar_horizons.rover"),x+6,y+5,0xff83c2c7);
            ctx.drawTextWithShadow(mc.textRenderer,Text.translatable("hud.lunar_horizons.rover_speed",String.format(java.util.Locale.ROOT,"%.1f",r.speed()/10.0)),x+6,y+17,0xffe6edf2);
            ctx.drawTextWithShadow(mc.textRenderer,Text.translatable("hud.lunar_horizons.rover_energy",r.energy(),r.capacity()),x+6,y+29,0xffe6edf2);
            ctx.fill(x+6,y+41,x+144,y+45,0xff354653);
            ctx.fill(x+6,y+41,x+6+138*r.energy()/r.capacity(),y+45,0xff83c2c7);
        });
    }
}

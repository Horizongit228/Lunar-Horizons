package com.lunarhorizons.client;

import com.lunarhorizons.LunarHorizons;
import com.lunarhorizons.OxygenSystem;
import com.lunarhorizons.RocketEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

final class OxygenHud {
    static boolean habitat=false;
    static void render(DrawContext ctx,RenderTickCounter tickCounter){
        MinecraftClient mc=MinecraftClient.getInstance();
        if(mc.player==null||mc.world==null||mc.options.hudHidden||mc.player.isCreative()||mc.player.isSpectator())return;
        boolean cabin=mc.player.getVehicle() instanceof RocketEntity;
        boolean safe=habitat&&LunarHorizons.vacuum(mc.world);
        boolean protectedAir=cabin||safe;
        if(!cabin&&!LunarHorizons.vacuum(mc.world))return;
        int left=10,top=10,w=130;
        int remaining=OxygenSystem.remaining(mc.player),capacity=OxygenSystem.capacity(mc.player);
        boolean suit=OxygenSystem.hasSuit(mc.player)&&OxygenSystem.hasBackpack(mc.player);
        int color=protectedAir?0xff61e4bc:(!suit||remaining==0?0xfff05b60:remaining<=60?0xfff4bc4d:0xff59cfe8);
        ctx.fill(left,top,left+w,top+39,0xd0182332);
        ctx.fill(left+1,top+1,left+w-1,top+2,color);
        Text label=safe?Text.translatable("hud.lunar_horizons.habitat"):cabin?Text.translatable("hud.lunar_horizons.cabin"):!suit?Text.translatable("hud.lunar_horizons.suit_missing"):Text.literal("O₂  "+String.format(java.util.Locale.ROOT,"%02d:%02d",remaining/60,remaining%60));
        ctx.drawTextWithShadow(mc.textRenderer,label,left+6,top+6,color);
        ctx.fill(left+6,top+20,left+w-6,top+26,0xff344559);
        int filled=protectedAir?w-12:capacity==0?0:(int)((w-12)*(double)remaining/capacity);
        if(filled>0)ctx.fill(left+6,top+20,left+6+filled,top+26,color);
        ctx.drawTextWithShadow(mc.textRenderer,Text.translatable(protectedAir?"hud.lunar_horizons.safe":suit&&remaining>0?"hud.lunar_horizons.sealed":"hud.lunar_horizons.vacuum"),left+6,top+29,0xffd0dbe8);
    }
}

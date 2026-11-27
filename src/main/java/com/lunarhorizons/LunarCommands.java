package com.lunarhorizons;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;

final class LunarCommands {
    static void register(){
        CommandRegistrationCallback.EVENT.register((dispatcher,registryAccess,environment)->
            dispatcher.register(literal("lunar").requires(s->s.hasPermissionLevel(2))
                .then(literal("meteor")
                    .then(mode("normal",false))
                    .then(mode("huge",true))
                    .then(literal("status").executes(c->{
                        c.getSource().sendFeedback(()->Text.translatable(MeteorEvents.hasActive()?"message.lunar_horizons.meteor_active":"message.lunar_horizons.meteor_inactive"),false);return 1;
                    }))
                    .then(literal("cancel").executes(c->{
                        if(!MeteorEvents.hasActive()){c.getSource().sendError(Text.translatable("message.lunar_horizons.meteor_inactive"));return 0;}
                        MeteorEvents.clear();c.getSource().sendFeedback(()->Text.translatable("message.lunar_horizons.meteor_cancelled"),true);return 1;
                    })))));
    }
    private static LiteralArgumentBuilder<ServerCommandSource> mode(String name,boolean huge){
        return literal(name).executes(c->summon(c.getSource(),huge,null,null))
            .then(literal("at").then(argument("x",IntegerArgumentType.integer(-29999984,29999984))
                .then(argument("z",IntegerArgumentType.integer(-29999984,29999984))
                    .executes(c->summon(c.getSource(),huge,IntegerArgumentType.getInteger(c,"x"),IntegerArgumentType.getInteger(c,"z"))))));
    }
    private static int summon(ServerCommandSource s,boolean huge,Integer x,Integer z){
        if(!s.getWorld().getRegistryKey().equals(LunarHorizons.MOON)){
            s.sendError(Text.translatable("message.lunar_horizons.meteor_moon_only"));return 0;
        }
        if(MeteorEvents.hasActive()){s.sendError(Text.translatable("message.lunar_horizons.meteor_active"));return 0;}
        BlockPos p=x==null?MeteorEvents.summonNear(s.getWorld(),BlockPos.ofFloored(s.getPosition()),huge,s.getServer().getTicks())
            :MeteorEvents.summonAt(s.getWorld(),x,z,huge,s.getServer().getTicks());
        if(p==null){s.sendError(Text.translatable("message.lunar_horizons.meteor_no_site"));return 0;}
        s.sendFeedback(()->Text.translatable("message.lunar_horizons.meteor_summoned",p.getX(),p.getY(),p.getZ()),true);
        return 1;
    }
}

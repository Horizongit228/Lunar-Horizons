package com.lunarhorizons;

import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

final class MoonSurvival {
    private static final Identifier GRAVITY=LunarHorizons.id("moon_gravity"), FALL=LunarHorizons.id("moon_fall");
    private static void modifier(EntityAttributeInstance a,Identifier id,double value,boolean on){
        if(a==null)return;
        if(on&&!a.hasModifier(id))a.addTemporaryModifier(new EntityAttributeModifier(id,value,EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        if(!on&&a.hasModifier(id))a.removeModifier(id);
    }
    static int tankSlot(ServerPlayerEntity p){return OxygenSystem.tankSlot(p);}
    static void tick(MinecraftServer server){
        for(ServerPlayerEntity p:server.getPlayerManager().getPlayerList()){
            boolean moon=p.getWorld().getRegistryKey().equals(LunarHorizons.MOON);
            modifier(p.getAttributeInstance(EntityAttributes.GENERIC_GRAVITY),GRAVITY,-.75,moon);
            modifier(p.getAttributeInstance(EntityAttributes.GENERIC_FALL_DAMAGE_MULTIPLIER),FALL,-1,moon);
            if(!moon||!p.isAlive()||p.isCreative()||p.isSpectator()||p.getVehicle() instanceof RocketEntity||server.getTicks()%20!=0)continue;
            if(OxygenSystem.ready(p)){
                ItemStack tank=p.getInventory().getStack(OxygenSystem.tankSlot(p));
                tank.setDamage(Math.min(tank.getMaxDamage(),tank.getDamage()+1));p.getInventory().markDirty();
                int seconds=OxygenSystem.remaining(p);
                if(seconds<=60&&server.getTicks()%100==0){p.playSound(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(),.8f,1.8f);p.sendMessage(Text.translatable("message.lunar_horizons.oxygen_low"),true);}
            }else{
                if(server.getTicks()%40==0)p.damage(p.getDamageSources().drown(),2F);
                if(server.getTicks()%100==0){p.playSound(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(),.8f,.7f);p.sendMessage(Text.translatable("message.lunar_horizons.oxygen_danger"),true);}
            }
        }
    }
}

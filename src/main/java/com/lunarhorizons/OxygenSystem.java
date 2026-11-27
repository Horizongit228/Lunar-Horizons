package com.lunarhorizons;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.world.World;

public final class OxygenSystem {
    private OxygenSystem(){}
    public static boolean isTank(ItemStack s){return s.isOf(LunarHorizons.OXYGEN)||s.isOf(LunarHorizons.LARGE_OXYGEN)||s.isOf(LunarHorizons.EXTREME_OXYGEN);}
    public static boolean hasSuit(PlayerEntity p){
        return p.getEquippedStack(EquipmentSlot.HEAD).isOf(LunarHorizons.HELMET)
            && p.getEquippedStack(EquipmentSlot.CHEST).isOf(LunarHorizons.CHEST)
            && p.getEquippedStack(EquipmentSlot.LEGS).isOf(LunarHorizons.LEGS)
            && p.getEquippedStack(EquipmentSlot.FEET).isOf(LunarHorizons.BOOTS);
    }
    public static boolean hasBackpack(PlayerEntity p){for(int i=0;i<p.getInventory().size();i++)if(p.getInventory().getStack(i).isOf(LunarHorizons.BACKPACK))return true;return false;}
    public static int tankSlot(PlayerEntity p){
        for(int i=0;i<p.getInventory().size();i++){ItemStack s=p.getInventory().getStack(i);if(isTank(s)&&s.getDamage()<s.getMaxDamage())return i;}
        return -1;
    }
    public static int remaining(PlayerEntity p){int total=0;for(int i=0;i<p.getInventory().size();i++){ItemStack s=p.getInventory().getStack(i);if(isTank(s))total+=Math.max(0,s.getMaxDamage()-s.getDamage());}return total;}
    public static int capacity(PlayerEntity p){int total=0;for(int i=0;i<p.getInventory().size();i++){ItemStack s=p.getInventory().getStack(i);if(isTank(s))total+=s.getMaxDamage();}return total;}
    public static boolean ready(PlayerEntity p){return hasSuit(p)&&hasBackpack(p)&&tankSlot(p)>=0;}
    public static void refill(ServerPlayerEntity p,ItemStack tank){
        if(tank.getDamage()==0){p.sendMessage(Text.translatable("message.lunar_horizons.tank_full"),true);return;}
        if(!p.isCreative()&&!p.getWorld().getRegistryKey().equals(World.OVERWORLD)){
            int ice=-1;for(int i=0;i<36;i++)if((p.getInventory().getStack(i).isOf(Items.ICE)||p.getInventory().getStack(i).isOf(LunarHorizons.LUNAR_ICE.asItem()))){ice=i;break;}
            if(ice<0){p.sendMessage(Text.translatable("message.lunar_horizons.refill_ice"),true);return;}
            p.getInventory().getStack(ice).decrement(1);
        }
        tank.setDamage(0);p.getInventory().markDirty();
        p.playSound(SoundEvents.BLOCK_BREWING_STAND_BREW,1f,1f);
        p.sendMessage(Text.translatable("message.lunar_horizons.refilled"),true);
    }
}

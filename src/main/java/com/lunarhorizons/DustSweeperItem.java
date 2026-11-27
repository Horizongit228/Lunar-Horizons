package com.lunarhorizons;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/** Moon Sweepers homage: lifts exposed basalt dust without mining other materials. */
public final class DustSweeperItem extends Item {
    public DustSweeperItem(Settings settings){super(settings);}
    @Override public ActionResult useOnBlock(ItemUsageContext ctx){
        World world=ctx.getWorld();PlayerEntity player=ctx.getPlayer();
        BlockPos target=ctx.getBlockPos();
        if(player==null||player.isSpectator()||!player.getAbilities().allowModifyWorld||
            !world.getRegistryKey().equals(LunarHorizons.MOON)||ctx.getSide()!=Direction.UP||
            !world.getBlockState(target).isOf(LunarHorizons.BASALT_DUST)||!world.isAir(target.up()))return ActionResult.PASS;
        if(world.isClient)return ActionResult.SUCCESS;
        if(!(world instanceof ServerWorld w))return ActionResult.PASS;
        ItemStack stack=ctx.getStack();
        int count=0;
        int radius=player.isSneaking()?0:1;
        for(int dx=-radius;dx<=radius&&!stack.isEmpty();dx++)for(int dz=-radius;dz<=radius&&!stack.isEmpty();dz++){
            BlockPos p=target.add(dx,0,dz);
            if(!w.isChunkLoaded(p.getX()>>4,p.getZ()>>4)||!w.canPlayerModifyAt(player,p))continue;
            var state=w.getBlockState(p);
            if(!state.isOf(LunarHorizons.BASALT_DUST)||!w.isAir(p.up()))continue;
            if(w.breakBlock(p,!player.isCreative(),player)){
                count++;
                w.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK,state),p.getX()+.5,p.getY()+.9,p.getZ()+.5,12,.32,.15,.32,.025);
                if(!player.isCreative())stack.damage(1,player,ctx.getHand()==Hand.MAIN_HAND?EquipmentSlot.MAINHAND:EquipmentSlot.OFFHAND);
            }
        }
        if(count>0){
            player.playSound(SoundEvents.BLOCK_SAND_BREAK,.8f,1.3f);
            player.getItemCooldownManager().set(this,8);
            player.sendMessage(Text.translatable("message.lunar_horizons.dust_swept",count),true);
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }
}

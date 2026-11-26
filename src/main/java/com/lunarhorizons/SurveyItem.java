package com.lunarhorizons;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.Heightmap;

public final class SurveyItem extends Item {
    private final boolean radar;
    public SurveyItem(Settings settings,boolean radar){super(settings);this.radar=radar;}
    @Override public TypedActionResult<ItemStack> use(World world,PlayerEntity user,Hand hand){
        ItemStack stack=user.getStackInHand(hand);
        if(world.isClient)return TypedActionResult.success(stack);
        if(!(world instanceof ServerWorld w)||!world.getRegistryKey().equals(LunarHorizons.MOON)){
            user.sendMessage(Text.translatable("message.lunar_horizons.scan_moon"),true);return TypedActionResult.fail(stack);
        }
        BlockPos origin=user.getBlockPos(),best=null;double distance=Double.MAX_VALUE;
        if(radar){
            // Search loaded columns for surface beacons only, without generating chunks.
            for(int dx=-48;dx<=48;dx++)for(int dz=-48;dz<=48;dz++){
                int x=origin.getX()+dx,z=origin.getZ()+dz;
                if(!w.isChunkLoaded(x>>4,z>>4))continue;
                double d=dx*dx+dz*dz;if(d>=distance)continue;
                int top=w.getTopY(Heightmap.Type.MOTION_BLOCKING,x,z);
                for(int y=top-1;y>=top-4;y--){BlockPos p=new BlockPos(x,y,z);if(w.getBlockState(p).isOf(LunarHorizons.SURVEY_BEACON)){best=p;distance=d;break;}}
            }
        }else{
            for(int dx=-8;dx<=8;dx++)for(int dz=-8;dz<=8;dz++){
                if(!w.isChunkLoaded((origin.getX()+dx)>>4,(origin.getZ()+dz)>>4))continue;
                for(int dy=-8;dy<=8;dy++){
                    double d=dx*dx+dy*dy+dz*dz;if(d>64||d>=distance)continue;
                    BlockPos p=origin.add(dx,dy,dz);var s=w.getBlockState(p);
                    if(s.isOf(LunarHorizons.CRYSTAL_ORE)||s.isOf(LunarHorizons.TITANIUM_ORE)||s.isOf(LunarHorizons.HELIUM_ORE)||s.isOf(LunarHorizons.LUNAR_ICE)){best=p;distance=d;}
                }
            }
        }
        if(best==null)user.sendMessage(Text.translatable("message.lunar_horizons.scan_none"),true);
        else if(radar)user.sendMessage(Text.translatable("message.lunar_horizons.radar_found",best.getX(),best.getZ(),(int)Math.sqrt(distance)),true);
        else user.sendMessage(Text.translatable("message.lunar_horizons.mineral_found",(int)Math.ceil(Math.sqrt(distance)),w.getBlockState(best).getBlock().getName()),true);
        user.playSound(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(),.8f,best==null?.7f:1.6f);
        user.getItemCooldownManager().set(this,radar?100:60);
        return TypedActionResult.success(stack);
    }
}

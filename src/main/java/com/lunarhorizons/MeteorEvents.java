package com.lunarhorizons;

import java.util.*;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

/** Cosmetic falling meteor showers with recoverable fragments; no destructive explosions. */
final class MeteorEvents {
    private record Meteor(ServerWorld world,BlockPos pos,int start,int dx,int dz){}
    private static final List<Meteor> ACTIVE=new ArrayList<>();
    static void clear(){ACTIVE.clear();}
    static void tick(MinecraftServer server){
        int tick=server.getTicks();
        ServerWorld moon=server.getWorld(LunarHorizons.MOON);
        if(moon==null)return;
        if(tick%1200==0&&ACTIVE.isEmpty()&&moon.getRandom().nextInt(8)==0){
            List<ServerPlayerEntity> players=moon.getPlayers(p->!p.isSpectator()&&p.getVehicle()==null&&p.isAlive());
            if(!players.isEmpty()){
                ServerPlayerEntity p=players.get(moon.getRandom().nextInt(players.size()));
                if(p.getY()>=moon.getTopY(Heightmap.Type.MOTION_BLOCKING,p.getBlockX(),p.getBlockZ())-3){
                    for(int i=0;i<3;i++){
                        int x=p.getBlockX()+moon.getRandom().nextInt(33)-16,z=p.getBlockZ()+moon.getRandom().nextInt(33)-16;
                        if(!moon.isChunkLoaded(x>>4,z>>4))continue;
                        int y=moon.getTopY(Heightmap.Type.MOTION_BLOCKING,x,z);
                        BlockPos pos=new BlockPos(x,y,z);
                        if(ImpactCraterFeature.natural(moon,pos.down())&&moon.isAir(pos))ACTIVE.add(new Meteor(moon,pos,tick+i*30,moon.getRandom().nextInt(15)-7,moon.getRandom().nextInt(15)-7));
                    }
                    if(!ACTIVE.isEmpty())p.sendMessage(Text.translatable("message.lunar_horizons.meteor_warning"),false);
                }
            }
        }
        for(Iterator<Meteor> it=ACTIVE.iterator();it.hasNext();){
            Meteor meteor=it.next();int age=tick-meteor.start;
            if(age<0)continue;
            BlockPos p=meteor.pos;ServerWorld w=meteor.world;
            if(!w.isChunkLoaded(p.getX()>>4,p.getZ()>>4)){it.remove();continue;}
            if(age<60){
                double t=1-age/60.0,x=p.getX()+.5+meteor.dx*t,z=p.getZ()+.5+meteor.dz*t,y=p.getY()+.5+40*t;
                if(age%2==0){w.spawnParticles(ParticleTypes.FLAME,x,y,z,8,.22,.35,.22,.02);w.spawnParticles(ParticleTypes.SMOKE,x,y,z,5,.3,.4,.3,.02);}
            }else{
                w.spawnParticles(ParticleTypes.EXPLOSION,p.getX()+.5,p.getY()+.6,p.getZ()+.5,1,0,0,0,0);
                w.playSound(null,p,SoundEvents.ENTITY_GENERIC_EXPLODE.value(),SoundCategory.BLOCKS,1.3f,.8f);
                if(w.isAir(p)&&ImpactCraterFeature.natural(w,p.down()))w.spawnEntity(new ItemEntity(w,p.getX()+.5,p.getY()+.3,p.getZ()+.5,new ItemStack(LunarHorizons.METEORITE_FRAGMENT,1+w.getRandom().nextInt(3))));
                it.remove();
            }
        }
    }
}

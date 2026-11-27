package com.lunarhorizons;

import java.util.*;
import net.minecraft.block.Blocks;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

/** Live impacts: visible falling trails, excavated natural terrain and mineable remnants.
 * One world event roll per minute while an eligible player is on the surface.
 * Huge 1%, ordinary 10%, no event 89%. Never force-load chunks or use destructive explosions. */
final class MeteorEvents {
    private static final double HUGE_CHANCE=.01, NORMAL_CHANCE=.10;
    private static final int CHECK_INTERVAL=1200;
    private record Meteor(ServerWorld world,BlockPos pos,int start,int dx,int dz,boolean huge){}
    private record Column(BlockPos surface,int depth){}
    private static final List<Meteor> ACTIVE=new ArrayList<>();
    static void clear(){ACTIVE.clear();}
    static void tick(MinecraftServer server){
        int tick=server.getTicks();
        ServerWorld moon=server.getWorld(LunarHorizons.MOON);
        if(moon==null)return;
        if(tick%CHECK_INTERVAL==0&&ACTIVE.isEmpty()){
            List<ServerPlayerEntity> players=moon.getPlayers(p->!p.isSpectator()&&p.getVehicle()==null&&p.isAlive()
                &&p.getY()>=moon.getTopY(Heightmap.Type.MOTION_BLOCKING,p.getBlockX(),p.getBlockZ())-2);
            if(!players.isEmpty()){
                double roll=moon.getRandom().nextDouble();
                if(roll<HUGE_CHANCE+NORMAL_CHANCE){
                    boolean huge=roll<HUGE_CHANCE;
                    ServerPlayerEntity player=players.get(moon.getRandom().nextInt(players.size()));
                    summonNear(moon,player.getBlockPos(),huge,tick);
                }
            }
        }
        for(Iterator<Meteor> it=ACTIVE.iterator();it.hasNext();){
            Meteor meteor=it.next();int age=tick-meteor.start;BlockPos p=meteor.pos;ServerWorld w=meteor.world;
            if(!w.isChunkLoaded(p.getX()>>4,p.getZ()>>4)){it.remove();continue;}
            int duration=meteor.huge?120:65;
            if(age<duration){
                double t=1-age/(double)duration,x=p.getX()+.5+meteor.dx*t,z=p.getZ()+.5+meteor.dz*t,y=p.getY()+.5+(meteor.huge?85:48)*t;
                if(age%2==0){
                    double size=meteor.huge?1.8:.35;
                    w.spawnParticles(ParticleTypes.FLAME,x,y,z,meteor.huge?28:8,size,size,size,.03);
                    w.spawnParticles(ParticleTypes.SMOKE,x,y,z,meteor.huge?35:7,size,size,size,.025);
                    w.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK,LunarHorizons.METEORITE_ROCK.getDefaultState()),x,y,z,meteor.huge?45:12,size*.65,size*.65,size*.65,.015);
                }
            }else{
                impact(w,p,meteor.huge);
                it.remove();
            }
        }
    }
    static boolean hasActive(){return !ACTIVE.isEmpty();}
    static BlockPos summonNear(ServerWorld moon,BlockPos origin,boolean huge,int tick){
        if(!moon.getRegistryKey().equals(LunarHorizons.MOON)||hasActive())return null;
        for(int i=0;i<40;i++){
            double angle=moon.getRandom().nextDouble()*Math.PI*2;
            int distance=(huge?30:14)+moon.getRandom().nextInt(huge?19:15);
            int x=origin.getX()+(int)Math.round(Math.cos(angle)*distance);
            int z=origin.getZ()+(int)Math.round(Math.sin(angle)*distance);
            BlockPos p=summonAt(moon,x,z,huge,tick);
            if(p!=null)return p;
        }
        return null;
    }
    static BlockPos summonAt(ServerWorld moon,int x,int z,boolean huge,int tick){
        if(!moon.getRegistryKey().equals(LunarHorizons.MOON)||hasActive()||!moon.isChunkLoaded(x>>4,z>>4))return null;
        BlockPos p=new BlockPos(x,moon.getTopY(Heightmap.Type.MOTION_BLOCKING,x,z),z);
        if(!moon.getWorldBorder().contains(p)||plan(moon,p,huge)==null)return null;
        ACTIVE.add(new Meteor(moon,p,tick,moon.getRandom().nextInt(21)-10,moon.getRandom().nextInt(21)-10,huge));
        for(ServerPlayerEntity nearby:moon.getPlayers()){
            double dx=nearby.getX()-x,dz=nearby.getZ()-z;
            if(dx*dx+dz*dz<128*128)nearby.sendMessage(Text.translatable(huge?"message.lunar_horizons.huge_meteor_warning":"message.lunar_horizons.meteor_warning"),false);
        }
        return p;
    }
    /** Preflight the complete crater footprint and floor. Reject caves, structures and unloaded chunks. */
    private static List<Column> plan(ServerWorld w,BlockPos p,boolean huge){
        int radius=huge?12:3,maxDepth=huge?5:2;
        if(p.getY()<5||p.getY()>w.getTopY()-12||w.getTopY(Heightmap.Type.MOTION_BLOCKING,p.getX(),p.getZ())!=p.getY())return null;
        List<Column> cuts=new ArrayList<>();
        for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            double q=(dx*dx+dz*dz)/(double)(radius*radius);
            if(q>1)continue;
            int x=p.getX()+dx,z=p.getZ()+dz;
            if(!w.isChunkLoaded(x>>4,z>>4))return null;
            int y=w.getTopY(Heightmap.Type.MOTION_BLOCKING,x,z)-1;
            if(Math.abs(y-(p.getY()-1))>(huge?6:3))return null;
            BlockPos surface=new BlockPos(x,y,z);
            int depth=Math.max(0,(int)Math.round(maxDepth*(1-q)*(1-q)));
            for(int h=0;h<=depth+1;h++)if(!ImpactCraterFeature.natural(w,surface.down(h)))return null;
            if(!w.getWorldBorder().contains(surface)||!w.isAir(surface.up()))return null;
            cuts.add(new Column(surface,depth));
        }
        return cuts;
    }
    private static void impact(ServerWorld w,BlockPos pos,boolean huge){
        List<Column> cuts=plan(w,pos,huge);
        if(cuts==null)return; // A player may have built here during the warning.
        int centerY=pos.getY()-1-(huge?5:2);
        for(Column col:cuts){
            BlockPos surface=col.surface;
            for(int h=0;h<col.depth;h++)w.setBlockState(surface.down(h),Blocks.AIR.getDefaultState(),3);
            int dx=surface.getX()-pos.getX(),dz=surface.getZ()-pos.getZ();
            if(dx*dx+dz*dz<=(huge?9:1))w.setBlockState(surface.down(col.depth),LunarHorizons.METEORITE_ROCK.getDefaultState(),3);
        }
        // Irregular solid meteorite body resting on the center of the crater.
        int r=huge?3:1;
        for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++)for(int h=0;h<=r*2;h++){
            double q=(dx*dx+dz*dz+(h-r*.65)*(h-r*.65))/(double)(r*r);
            if(q>1.05)continue;
            BlockPos p=new BlockPos(pos.getX()+dx,centerY+h,pos.getZ()+dz);
            if(w.isAir(p))w.setBlockState(p,LunarHorizons.METEORITE_ROCK.getDefaultState(),3);
        }
        w.spawnParticles(ParticleTypes.EXPLOSION,pos.getX()+.5,pos.getY()+.6,pos.getZ()+.5,huge?18:2,huge?4:.5,huge?2:.3,huge?4:.5,0);
        w.spawnParticles(ParticleTypes.SMOKE,pos.getX()+.5,pos.getY()+.6,pos.getZ()+.5,huge?150:30,huge?5:1,huge?2:1,huge?5:1,.08);
        w.playSound(null,pos,SoundEvents.ENTITY_GENERIC_EXPLODE.value(),SoundCategory.BLOCKS,huge?5f:1.6f,huge?.55f:.9f);
        w.spawnEntity(new ItemEntity(w,pos.getX()+.5,centerY+(huge?6:3),pos.getZ()+.5,new ItemStack(LunarHorizons.METEORITE_FRAGMENT,huge?12+w.getRandom().nextInt(13):1+w.getRandom().nextInt(3))));
    }
}

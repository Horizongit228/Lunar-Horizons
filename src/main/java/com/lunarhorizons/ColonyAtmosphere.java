package com.lunarhorizons;
import java.util.*;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.state.property.Properties;
final class ColonyAtmosphere {
    static boolean breathable(ServerWorld w,BlockPos origin){
        if(!LunarHorizons.vacuum(w)||!ColonyGrid.loaded(w,origin)||!passable(w,origin))return false;
        Set<BlockPos> seen=new HashSet<>(),vents=new HashSet<>();ArrayDeque<BlockPos> queue=new ArrayDeque<>();
        queue.add(origin);seen.add(origin);boolean oxygen=false;
        while(!queue.isEmpty()){
            BlockPos p=queue.remove();
            for(Direction d:Direction.values()){
                BlockPos q=p.offset(d);
                if(!ColonyGrid.loaded(w,q))return false;
                BlockState s=w.getBlockState(q);
                if(s.isOf(LunarHorizons.OXYGEN_GENERATOR)){if(powered(w,q))oxygen=true;continue;}
                if(s.isOf(LunarHorizons.OXYGEN_VENT)){vents.add(q);continue;}
                if(s.isOf(LunarHorizons.AIRLOCK)?!s.get(Properties.OPEN):s.isOf(LunarHorizons.PRESSURE_GLASS)||s.isSideSolidFullSquare(w,q,d.getOpposite()))continue;
                if(!passable(w,q))return false;
                if(seen.contains(q))continue;
                if(seen.size()>=2048||Math.abs(q.getX()-origin.getX())>24||Math.abs(q.getY()-origin.getY())>24||Math.abs(q.getZ()-origin.getZ())>24)return false;
                seen.add(q);queue.add(q);
            }
        }
        if(oxygen)return true;
        for(BlockPos vent:vents)if(supplied(w,vent))return true;
        return false;
    }
    private static boolean passable(ServerWorld w,BlockPos p){
        BlockState s=w.getBlockState(p);
        if(!w.getFluidState(p).isEmpty())return false;
        if(s.isOf(LunarHorizons.PRESSURE_GLASS))return false;
        if(s.isOf(LunarHorizons.AIRLOCK))return s.get(Properties.OPEN);
        return s.isAir()||s.getBlock() instanceof ColonyConduitBlock||!s.isFullCube(w,p);
    }
    private static boolean powered(ServerWorld w,BlockPos p){
        return w.getBlockEntity(p) instanceof ColonyBlockEntity be&&be.kind()==ColonyBlockEntity.Kind.OXYGEN&&be.active&&be.lastPowered>=w.getTime()-40&&be.lastPowered<=w.getTime();
    }
    private static boolean supplied(ServerWorld w,BlockPos vent){
        Set<BlockPos> seen=new HashSet<>();ArrayDeque<BlockPos> queue=new ArrayDeque<>();
        seen.add(vent);queue.add(vent);
        while(!queue.isEmpty()){
            BlockPos p=queue.remove();
            for(Direction d:Direction.values()){
                BlockPos q=p.offset(d);if(!ColonyGrid.loaded(w,q)||seen.contains(q))continue;
                BlockState s=w.getBlockState(q);
                if(s.isOf(LunarHorizons.OXYGEN_GENERATOR)&&powered(w,q))return true;
                if(s.isOf(LunarHorizons.OXYGEN_PIPE)||s.isOf(LunarHorizons.OXYGEN_VENT)){
                    ColonyConduitBlock.refresh(w,q,s);
                    if(seen.size()>=512)return false;
                    seen.add(q);queue.add(q);
                }
            }
        }
        return false;
    }
}

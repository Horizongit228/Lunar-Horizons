package com.lunarhorizons;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.BlockState;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.TeleportTarget;
import static net.minecraft.server.command.CommandManager.literal;

final class StructureTeleport {
    private static final String[] SITES={"abandoned_outpost","crashed_satellite","buried_lab","lunar_geode","old_lander","relay_tower","radio_observatory","solar_farm","rover_garage","helium_station","abandoned_mine","ice_complex","emergency_bunker","data_center","alien_ruins","abandoned_colony","lunar_spaceport","reactor_complex","crashed_alien_ship","orbital_station"};
    static LiteralArgumentBuilder<ServerCommandSource> command(String name){
        var root=literal(name).executes(c->{
            c.getSource().sendFeedback(()->Text.translatable("message.lunar_horizons.structure_usage"),false);
            return 1;
        });
        for(String id:SITES){
            root.then(literal(id).executes(c->teleport(c.getSource(),id)));
            root.then(literal("lunar_horizons:"+id).executes(c->teleport(c.getSource(),id)));
        }
        return root;
    }
    private static int teleport(ServerCommandSource s,String id)throws CommandSyntaxException{
        var player=s.getPlayerOrThrow();
        if(player.hasVehicle()){s.sendError(Text.translatable("message.lunar_horizons.structure_dismount"));return 0;}
        boolean station=id.equals("orbital_station");
        ServerWorld world=s.getServer().getWorld(station?LunarHorizons.SPACE:LunarHorizons.MOON);
        if(world==null){s.sendError(Text.translatable("message.lunar_horizons.missing_world"));return 0;}
        BlockPos at;
        if(station){
            at=OrbitalStationFeature.dock(0).east(3);
            world.getChunk(at.getX()>>4,at.getZ()>>4);
            if(!safe(world,at))at=safeNear(world,OrbitalStationFeature.dock(0),6);
        }else{
            var registry=world.getRegistryManager().get(RegistryKeys.STRUCTURE);
            var entry=registry.getEntry(RegistryKey.of(RegistryKeys.STRUCTURE,LunarHorizons.id(id)));
            if(entry.isEmpty()){s.sendError(Text.translatable("message.lunar_horizons.structure_missing",id));return 0;}
            BlockPos center=BlockPos.ofFloored(player.getPos());
            var found=world.getChunkManager().getChunkGenerator().locateStructure(world,RegistryEntryList.of(entry.get()),center,100,false);
            if(found==null){s.sendError(Text.translatable("message.lunar_horizons.structure_missing",id));return 0;}
            // Locate returns a chunk-based structure reference. Land safely at its surface;
            // underground entrances are accessible from the surrounding terrain.
            BlockPos pos=found.getFirst().add(7,0,7);
            at=safeNear(world,pos,12);
        }
        if(at==null){s.sendError(Text.translatable("message.lunar_horizons.structure_unsafe",id));return 0;}
        BlockPos destination=at;
        player.teleportTo(new TeleportTarget(world,Vec3d.ofBottomCenter(destination),Vec3d.ZERO,player.getYaw(),player.getPitch(),TeleportTarget.NO_OP));
        player.fallDistance=0;
        s.sendFeedback(()->Text.translatable("message.lunar_horizons.structure_arrived",id,destination.getX(),destination.getY(),destination.getZ()),false);
        return 1;
    }
    private static boolean safe(ServerWorld w,BlockPos p){
        return w.getWorldBorder().contains(p)&&w.isInBuildLimit(p.up())
            &&w.getBlockState(p.down()).isSideSolidFullSquare(w,p.down(),net.minecraft.util.math.Direction.UP)
            &&w.isAir(p)&&w.isAir(p.up())&&w.getFluidState(p.down()).isEmpty();
    }
    private static BlockPos safeNear(ServerWorld w,BlockPos pos,int radius){
        for(int ring=0;ring<=radius;ring++)for(int x=-ring;x<=ring;x++)for(int z=-ring;z<=ring;z++){
            if(Math.max(Math.abs(x),Math.abs(z))!=ring)continue;
            int X=pos.getX()+x,Z=pos.getZ()+z;
            if(!w.getWorldBorder().contains(new BlockPos(X,w.getBottomY(),Z)))continue;
            w.getChunk(X>>4,Z>>4);
            int y=w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,X,Z);
            BlockPos candidate=new BlockPos(X,y,Z);
            if(safe(w,candidate))return candidate;
        }
        return null;
    }
    static int repair(ServerCommandSource s)throws CommandSyntaxException{
        var player=s.getPlayerOrThrow();ServerWorld w=player.getServerWorld();
        BlockPos center=player.getBlockPos();int count=0;
        for(BlockPos mutable:BlockPos.iterate(center.add(-16,-16,-16),center.add(16,16,16))){
            BlockPos p=mutable.toImmutable();
            if(!ColonyGrid.loaded(w,p))continue;
            BlockState old=w.getBlockState(p);
            if(old.getBlock() instanceof ColonyConduitBlock)ColonyConduitBlock.refresh(w,p,old);
            else if(old.getBlock() instanceof SolarPanelBlock)SolarPanelBlock.refresh(w,p,old);
            else if(old.getBlock() instanceof ColonyMachineBlock)ColonyMachineBlock.refresh(w,p,old);
            else continue;
            if(w.getBlockState(p)!=old)count++;
        }
        int repaired=count;
        s.sendFeedback(()->Text.translatable("message.lunar_horizons.conduits_repaired",repaired),false);
        return count;
    }
}

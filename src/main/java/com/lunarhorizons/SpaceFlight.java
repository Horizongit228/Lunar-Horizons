package com.lunarhorizons;

import java.util.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;

/** Low-thrust EVA assistance. Clients transmit only bounded key flags; server validates equipment and dimension. */
public final class SpaceFlight {
    public record Input(int flags) implements CustomPayload{
        public static final CustomPayload.Id<Input> ID=new CustomPayload.Id<>(LunarHorizons.id("eva_input"));
        public static final PacketCodec<RegistryByteBuf,Input> CODEC=PacketCodec.tuple(PacketCodecs.VAR_INT,Input::flags,Input::new);
        @Override public CustomPayload.Id<? extends CustomPayload> getId(){return ID;}
    }
    private record Command(int flags,int tick){}
    private static final Map<UUID,Command> COMMANDS=new HashMap<>();
    private static final Set<UUID> PRESSURIZED=new HashSet<>();
    static boolean stationGravity(ServerPlayerEntity p){return PRESSURIZED.contains(p.getUuid());}
    static void setStationGravity(ServerPlayerEntity p,boolean on){if(on)PRESSURIZED.add(p.getUuid());else PRESSURIZED.remove(p.getUuid());}
    static void register(){
        // Dropped cargo floats in vacuum too. Only undo the gravity flag this mod itself added.
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity,world)->{
            if(!(entity instanceof net.minecraft.entity.ItemEntity))return;
            if(world.getRegistryKey().equals(LunarHorizons.SPACE)){
                if(!entity.hasNoGravity()){entity.setNoGravity(true);entity.addCommandTag("lh_orbital_item");}
            }else if(entity.getCommandTags().contains("lh_orbital_item")){
                entity.setNoGravity(false);entity.removeCommandTag("lh_orbital_item");
            }
        });
        PayloadTypeRegistry.playC2S().register(Input.ID,Input.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Input.ID,(payload,context)->{
            var p=context.player();
            if(eligible(p)&&!stationGravity(p))COMMANDS.put(p.getUuid(),new Command(payload.flags()&127,p.getServer().getTicks()));
        });
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->{COMMANDS.remove(handler.player.getUuid());PRESSURIZED.remove(handler.player.getUuid());});
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server->{COMMANDS.clear();PRESSURIZED.clear();});
    }
    public static boolean eligible(PlayerEntity p){
        return p.getWorld().getRegistryKey().equals(LunarHorizons.SPACE)&&p.isAlive()&&!p.isSpectator()&&!p.hasVehicle()&&!p.getAbilities().flying;
    }
    public static boolean canThrust(PlayerEntity p){
        return p.isCreative()||OxygenSystem.hasSuit(p)&&OxygenSystem.hasBackpack(p);
    }
    public static Vec3d velocity(PlayerEntity p,int flags){
        Vec3d v=p.getVelocity();
        if((flags&64)!=0)return v.multiply(.55);
        if(!canThrust(p))return v;
        double yaw=Math.toRadians(p.getYaw());
        double forward=((flags&1)!=0?1:0)-((flags&2)!=0?1:0);
        double side=((flags&4)!=0?1:0)-((flags&8)!=0?1:0);
        double up=((flags&16)!=0?1:0)-((flags&32)!=0?1:0);
        Vec3d thrust=new Vec3d(-Math.sin(yaw)*forward+Math.cos(yaw)*side,up,Math.cos(yaw)*forward+Math.sin(yaw)*side);
        if(thrust.lengthSquared()>1)thrust=thrust.normalize();
        v=v.add(thrust.multiply(.055));
        double max=.65;
        return v.lengthSquared()>max*max?v.normalize().multiply(max):v;
    }
    static void tick(MinecraftServer server){
        for(ServerPlayerEntity p:server.getPlayerManager().getPlayerList()){
            if(!eligible(p)||stationGravity(p)){COMMANDS.remove(p.getUuid());continue;}
            if(p.getY()<-48||p.getY()>304){
                // Game-world vertical boundary rescue, not a way to travel between dimensions.
                ServerWorld w=p.getServerWorld();BlockPos dock=OrbitalStationFeature.dock(0);
                w.getChunk(dock.getX()>>4,dock.getZ()>>4);
                p.teleportTo(new TeleportTarget(w,Vec3d.ofBottomCenter(dock.east(3)),Vec3d.ZERO,p.getYaw(),0,TeleportTarget.NO_OP));
                p.fallDistance=0;RocketEntity.message(p,"eva_boundary");continue;
            }
            Command cmd=COMMANDS.get(p.getUuid());
            int flags=cmd!=null&&server.getTicks()-cmd.tick()<=10?cmd.flags():0;
            if(flags!=0&&server.getTicks()%3==0){
                p.setVelocity(velocity(p,flags));p.velocityModified=true;
            }
            p.fallDistance=0;
        }
    }
}

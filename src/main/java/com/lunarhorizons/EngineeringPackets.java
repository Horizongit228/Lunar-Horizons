package com.lunarhorizons;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class EngineeringPackets {
    private EngineeringPackets(){}
    public record Snapshot(BlockPos pos,boolean open,int kind,boolean active,boolean enabled,boolean fault,
                           int input,int output,int progress,int duration,int reserve,int energy,int capacity,
                           int production,int demand,int served,int stored,int networkCapacity,int nodes,boolean automation,int diagnostic) implements CustomPayload {
        public static final CustomPayload.Id<Snapshot> ID=new CustomPayload.Id<>(LunarHorizons.id("machine_snapshot"));
        public static final PacketCodec<RegistryByteBuf,Snapshot> CODEC=new PacketCodec<>(){
            public Snapshot decode(RegistryByteBuf b){return new Snapshot(b.readBlockPos(),b.readBoolean(),b.readVarInt(),b.readBoolean(),b.readBoolean(),b.readBoolean(),
                b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readBoolean(),b.readVarInt());}
            public void encode(RegistryByteBuf b,Snapshot s){
                b.writeBlockPos(s.pos);b.writeBoolean(s.open);b.writeVarInt(s.kind);b.writeBoolean(s.active);b.writeBoolean(s.enabled);b.writeBoolean(s.fault);
                for(int n:new int[]{s.input,s.output,s.progress,s.duration,s.reserve,s.energy,s.capacity,s.production,s.demand,s.served,s.stored,s.networkCapacity,s.nodes})b.writeVarInt(n);
                b.writeBoolean(s.automation);b.writeVarInt(s.diagnostic);
            }
        };
        @Override public CustomPayload.Id<? extends CustomPayload> getId(){return ID;}
    }
    public record Request(BlockPos pos,int action) implements CustomPayload{
        public static final CustomPayload.Id<Request> ID=new CustomPayload.Id<>(LunarHorizons.id("machine_request"));
        public static final PacketCodec<RegistryByteBuf,Request> CODEC=new PacketCodec<>(){
            public Request decode(RegistryByteBuf b){return new Request(b.readBlockPos(),b.readVarInt());}
            public void encode(RegistryByteBuf b,Request s){b.writeBlockPos(s.pos);b.writeVarInt(s.action);}
        };
        @Override public CustomPayload.Id<? extends CustomPayload> getId(){return ID;}
    }
    static void send(ServerPlayerEntity p,ColonyBlockEntity be,boolean open){
        if(!ServerPlayNetworking.canSend(p,Snapshot.ID))return;
        ServerPlayNetworking.send(p,new Snapshot(be.getPos(),open,be.kind().ordinal(),be.active,be.enabled,be.networkFault,
            be.input,be.output,be.progress,be.duration(),be.reserve,be.energy,be.capacity(),be.production,be.demand,be.served,be.stored,be.networkCapacity,be.networkNodes,be.automation,be.diagnose()));
    }
    static void register(){
        PayloadTypeRegistry.playS2C().register(Snapshot.ID,Snapshot.CODEC);
        PayloadTypeRegistry.playC2S().register(Request.ID,Request.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Request.ID,(payload,context)->{
            ServerPlayerEntity p=context.player();var w=p.getServerWorld();BlockPos pos=payload.pos;
            if(p.isSpectator()||!p.isAlive()||p.squaredDistanceTo(Vec3d.ofCenter(pos))>36||!ColonyGrid.loaded(w,pos)||!w.canPlayerModifyAt(p,pos)||!(w.getBlockEntity(pos) instanceof ColonyBlockEntity be))return;
            switch(payload.action){
                case 0 -> {}
                case 1 -> {be.enabled=!be.enabled;if(!be.enabled)be.setActive(false);be.markDirty();}
                case 2 -> be.loadInventory(p);
                case 3 -> be.collect(p);
                case 4 -> be.refillInventory(p);
                case 5 -> {if(be.supportsInput()||be.supportsOutput()){be.automation=!be.automation;be.markDirty();}}
                default -> {return;}
            }
            send(p,be,false);
        });
    }
}

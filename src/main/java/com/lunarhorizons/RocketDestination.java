package com.lunarhorizons;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
public record RocketDestination(int destination) implements CustomPayload {
    public static final RocketDestination INSTANCE=new RocketDestination(-1);
    public static final CustomPayload.Id<RocketDestination> ID=new CustomPayload.Id<>(LunarHorizons.id("rocket_destination"));
    public static final PacketCodec<RegistryByteBuf,RocketDestination> CODEC=PacketCodec.tuple(PacketCodecs.VAR_INT,RocketDestination::destination,RocketDestination::new);
    @Override public CustomPayload.Id<? extends CustomPayload> getId(){return ID;}
    static void register(){
        PayloadTypeRegistry.playC2S().register(ID,CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ID,(payload,context)->{
            var p=context.player();
            if(p.getVehicle() instanceof RocketEntity r&&r.getFirstPassenger()==p)r.requestDestination(p,payload.destination());
        });
    }
}

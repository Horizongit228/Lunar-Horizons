package com.lunarhorizons;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
public record RocketDestination() implements CustomPayload {
    public static final RocketDestination INSTANCE=new RocketDestination();
    public static final CustomPayload.Id<RocketDestination> ID=new CustomPayload.Id<>(LunarHorizons.id("rocket_destination"));
    public static final PacketCodec<RegistryByteBuf,RocketDestination> CODEC=PacketCodec.unit(INSTANCE);
    @Override public CustomPayload.Id<? extends CustomPayload> getId(){return ID;}
    static void register(){
        PayloadTypeRegistry.playC2S().register(ID,CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ID,(payload,context)->{
            var p=context.player();
            if(p.getVehicle() instanceof RocketEntity r&&r.getFirstPassenger()==p)r.requestDestination(p);
        });
    }
}

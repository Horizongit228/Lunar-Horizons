package com.lunarhorizons;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
public record RoverControl(int flags) implements CustomPayload {
    public static final CustomPayload.Id<RoverControl> ID=new CustomPayload.Id<>(LunarHorizons.id("rover_control"));
    public static final PacketCodec<RegistryByteBuf,RoverControl> CODEC=PacketCodec.tuple(PacketCodecs.VAR_INT,RoverControl::flags,RoverControl::new);
    @Override public CustomPayload.Id<? extends CustomPayload> getId(){return ID;}
    public static void register(){
        PayloadTypeRegistry.playC2S().register(ID,CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ID,(payload,context)->{
            var p=context.player();
            if(p.isAlive()&&p.getVehicle() instanceof RoverEntity rover&&rover.getFirstPassenger()==p)rover.control(p,payload.flags()&31);
        });
    }
}

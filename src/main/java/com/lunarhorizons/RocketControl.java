package com.lunarhorizons;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record RocketControl() implements CustomPayload {
    public static final RocketControl INSTANCE=new RocketControl();
    public static final CustomPayload.Id<RocketControl> ID=new CustomPayload.Id<>(LunarHorizons.id("rocket_launch"));
    public static final PacketCodec<RegistryByteBuf,RocketControl> CODEC=PacketCodec.unit(INSTANCE);
    @Override public CustomPayload.Id<? extends CustomPayload> getId(){return ID;}
    public static void register(){
        PayloadTypeRegistry.playC2S().register(ID,CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ID,(payload,context)->{
            var p=context.player();
            if(p.getVehicle() instanceof RocketEntity rocket && rocket.getFirstPassenger()==p)
                rocket.requestLaunch(p);
        });
    }
}

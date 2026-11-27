package com.lunarhorizons;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
public record ColonyStatus(boolean breathable) implements CustomPayload {
    public static final CustomPayload.Id<ColonyStatus> ID=new CustomPayload.Id<>(LunarHorizons.id("colony_status"));
    public static final PacketCodec<RegistryByteBuf,ColonyStatus> CODEC=PacketCodec.tuple(PacketCodecs.BOOL,ColonyStatus::breathable,ColonyStatus::new);
    @Override public CustomPayload.Id<? extends CustomPayload> getId(){return ID;}
    public static void register(){PayloadTypeRegistry.playS2C().register(ID,CODEC);}
    static void send(ServerPlayerEntity p,boolean safe){if(ServerPlayNetworking.canSend(p,ID))ServerPlayNetworking.send(p,new ColonyStatus(safe));}
}

package com.lunarhorizons;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public final class LunarGuideItem extends Item {
    public LunarGuideItem(Settings settings){super(settings);}
    public record Open() implements CustomPayload {
        public static final Open INSTANCE=new Open();
        public static final CustomPayload.Id<Open> ID=new CustomPayload.Id<>(LunarHorizons.id("guide_open"));
        public static final PacketCodec<RegistryByteBuf,Open> CODEC=PacketCodec.unit(INSTANCE);
        @Override public CustomPayload.Id<? extends CustomPayload> getId(){return ID;}
    }
    static void register(){PayloadTypeRegistry.playS2C().register(Open.ID,Open.CODEC);}
    @Override public TypedActionResult<ItemStack> use(World world,PlayerEntity user,Hand hand){
        if(user instanceof ServerPlayerEntity p&&ServerPlayNetworking.canSend(p,Open.ID))ServerPlayNetworking.send(p,Open.INSTANCE);
        return TypedActionResult.success(user.getStackInHand(hand),world.isClient);
    }
}

package com.lunarhorizons.mixin;
import com.lunarhorizons.LunarHorizons;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** The vanilla floating timeout is not applicable in the zero-g dimension. */
@Mixin(ServerPlayNetworkHandler.class)
abstract class SpaceFloatingMixin {
    @Shadow public ServerPlayerEntity player;
    @Shadow private boolean floating;
    @Shadow private int floatingTicks;
    @Inject(method="tick",at=@At("HEAD"))
    private void lunar$orbitFloating(CallbackInfo ci){
        if(player.getWorld().getRegistryKey().equals(LunarHorizons.SPACE)){floating=false;floatingTicks=0;}
    }
}

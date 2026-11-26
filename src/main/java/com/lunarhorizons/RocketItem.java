package com.lunarhorizons;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

final class RocketItem extends BlockItem {
    RocketItem(Block block, Settings settings) { super(block, settings); }

    @Override public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getSide() != Direction.UP || !context.getWorld().getBlockState(context.getBlockPos()).isOf(LunarHorizons.LAUNCH_PAD)) {
            if (context.getPlayer() instanceof ServerPlayerEntity player) RocketEntity.message(player, "big_pad");
            return ActionResult.FAIL;
        }
        BlockPos base = context.getBlockPos().up();
        if (context.getWorld().isClient) return ActionResult.SUCCESS;
        if (!(context.getWorld() instanceof ServerWorld world) || !(context.getPlayer() instanceof ServerPlayerEntity player) || player.isSpectator()) return ActionResult.FAIL;
        if (!RocketEntity.hasPad(world, base)) {
            RocketEntity.message(player, "big_pad"); return ActionResult.FAIL;
        }
        RocketEntity rocket = RocketEntity.deploy(world, base, player.getYaw());
        if (rocket == null) { RocketEntity.message(player, "big_blocked"); return ActionResult.FAIL; }
        if (!player.isCreative()) context.getStack().decrement(1);
        return ActionResult.CONSUME;
    }
}

package com.lunarhorizons;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
public final class RoverItem extends Item {
    public RoverItem(Settings settings){super(settings);}
    @Override public ActionResult useOnBlock(ItemUsageContext ctx){
        if(ctx.getSide()!=Direction.UP||ctx.getPlayer()==null||ctx.getPlayer().isSpectator())return ActionResult.FAIL;
        if(ctx.getWorld().isClient)return ActionResult.SUCCESS;
        if(!(ctx.getWorld() instanceof ServerWorld w)||!(ctx.getPlayer() instanceof ServerPlayerEntity p))return ActionResult.FAIL;
        BlockPos at=ctx.getBlockPos().up();
        if(!w.canPlayerModifyAt(p,at)||!w.getWorldBorder().contains(at)||!w.getBlockState(at.down()).isSideSolidFullSquare(w,at.down(),Direction.UP))return ActionResult.FAIL;
        RoverEntity rover=new RoverEntity(ctx.getStack().isOf(LunarHorizons.ADVANCED_ROVER_ITEM)?LunarHorizons.ADVANCED_ROVER_ENTITY:LunarHorizons.ROVER_ENTITY,w);
        rover.refreshPositionAndAngles(at.getX()+.5,at.getY(),at.getZ()+.5,p.getYaw(),0);
        if(!w.isSpaceEmpty(rover,rover.getBoundingBox())||!w.getOtherEntities(rover,rover.getBoundingBox()).isEmpty()){
            RocketEntity.message(p,"rover_blocked");return ActionResult.FAIL;
        }
        if(!w.spawnEntity(rover))return ActionResult.FAIL;
        if(!p.isCreative())ctx.getStack().decrement(1);
        RocketEntity.message(p,"rover_deployed");return ActionResult.CONSUME;
    }
}

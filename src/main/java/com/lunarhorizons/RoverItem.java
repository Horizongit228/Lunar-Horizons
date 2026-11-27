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
    private static final String CHARGE="LunarRoverCharge";
    static void storeCharge(net.minecraft.item.ItemStack stack,int energy){
        var old=stack.get(net.minecraft.component.DataComponentTypes.CUSTOM_DATA);
        var data=old==null?new net.minecraft.nbt.NbtCompound():old.copyNbt();
        data.putInt(CHARGE,energy);
        stack.set(net.minecraft.component.DataComponentTypes.CUSTOM_DATA,net.minecraft.component.type.NbtComponent.of(data));
    }
    private static int storedCharge(net.minecraft.item.ItemStack stack){
        var data=stack.get(net.minecraft.component.DataComponentTypes.CUSTOM_DATA);
        return data==null?0:Math.max(0,data.copyNbt().getInt(CHARGE));
    }
    public RoverItem(Settings settings){super(settings);}
    @Override public ActionResult useOnBlock(ItemUsageContext ctx){
        if(ctx.getSide()!=Direction.UP||ctx.getPlayer()==null||ctx.getPlayer().isSpectator())return ActionResult.FAIL;
        if(ctx.getWorld().isClient)return ActionResult.SUCCESS;
        if(!(ctx.getWorld() instanceof ServerWorld w)||!(ctx.getPlayer() instanceof ServerPlayerEntity p))return ActionResult.FAIL;
        BlockPos at=ctx.getBlockPos().up();
        if(!w.canPlayerModifyAt(p,at)||!w.getWorldBorder().contains(at)||!w.getBlockState(at.down()).isSideSolidFullSquare(w,at.down(),Direction.UP))return ActionResult.FAIL;
        RoverEntity rover=new RoverEntity(ctx.getStack().isOf(LunarHorizons.ADVANCED_ROVER_ITEM)?LunarHorizons.ADVANCED_ROVER_ENTITY:LunarHorizons.ROVER_ENTITY,w);
        rover.energy(storedCharge(ctx.getStack()));
        rover.refreshPositionAndAngles(at.getX()+.5,at.getY(),at.getZ()+.5,p.getYaw(),0);
        if(!w.isSpaceEmpty(rover,rover.getBoundingBox())||!w.getOtherEntities(rover,rover.getBoundingBox()).isEmpty()){
            RocketEntity.message(p,"rover_blocked");return ActionResult.FAIL;
        }
        if(!w.spawnEntity(rover))return ActionResult.FAIL;
        if(!p.isCreative())ctx.getStack().decrement(1);
        RocketEntity.message(p,"rover_charge",rover.energy(),rover.capacity());return ActionResult.CONSUME;
    }
}

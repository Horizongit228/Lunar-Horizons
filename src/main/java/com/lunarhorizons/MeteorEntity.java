package com.lunarhorizons;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/** A real network-tracked falling rock. Its flight and impact are owned by MeteorEvents. */
public final class MeteorEntity extends Entity {
    boolean managed;
    private int steps;
    private double tx,ty,tz;
    public MeteorEntity(EntityType<? extends MeteorEntity> type,World world){super(type,world);setNoGravity(true);}
    public boolean huge(){return getType()==LunarHorizons.HUGE_METEOR_ENTITY;}
    @Override protected void initDataTracker(DataTracker.Builder builder){}
    @Override protected void readCustomDataFromNbt(NbtCompound nbt){}
    @Override protected void writeCustomDataToNbt(NbtCompound nbt){}
    @Override public boolean isPushable(){return false;}
    @Override public boolean canUsePortals(boolean allowVehicles){return false;}
    @Override public void updateTrackedPositionAndAngles(double x,double y,double z,float yaw,float pitch,int interpolationSteps){
        if(!getWorld().isClient){super.updateTrackedPositionAndAngles(x,y,z,yaw,pitch,interpolationSteps);return;}
        tx=x;ty=y;tz=z;steps=Math.max(1,interpolationSteps);
    }
    @Override public void tick(){
        super.tick();
        if(getWorld().isClient){
            if(steps>0){setPosition(getX()+(tx-getX())/steps,getY()+(ty-getY())/steps,getZ()+(tz-getZ())/steps);steps--;}
        }else if(!managed)discard();
    }
}

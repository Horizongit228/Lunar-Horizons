package com.lunarhorizons;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
public final class RoverEntity extends Entity {
    public static final int CAPACITY=3600;
    private static final TrackedData<Integer> ENERGY=DataTracker.registerData(RoverEntity.class,TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SPEED=DataTracker.registerData(RoverEntity.class,TrackedDataHandlerRegistry.INTEGER);
    private int flags,lastInput=-100,steps;
    private double lx,ly,lz,drive=0,vertical=0;
    private float lyaw;
    private ServerPlayerEntity controller;
    public RoverEntity(EntityType<? extends RoverEntity> type,World world){super(type,world);}
    @Override protected void initDataTracker(DataTracker.Builder b){b.add(ENERGY,0);b.add(SPEED,0);}
    public int energy(){return getDataTracker().get(ENERGY);}
    public int speed(){return getDataTracker().get(SPEED);}
    private void energy(int e){getDataTracker().set(ENERGY,MathHelper.clamp(e,0,CAPACITY));}
    @Override protected void readCustomDataFromNbt(NbtCompound nbt){energy(nbt.getInt("Energy"));}
    @Override protected void writeCustomDataToNbt(NbtCompound nbt){nbt.putInt("Energy",energy());}
    @Override public boolean canHit(){return isAlive();}
    @Override public boolean isPushable(){return false;}
    @Override public boolean canUsePortals(boolean allowVehicles){return false;}
    @Override public boolean damage(DamageSource source,float amount){return false;}
    @Override protected boolean canAddPassenger(Entity p){return !hasPassengers()&&p instanceof PlayerEntity;}
    @Override public boolean couldAcceptPassenger(){return !hasPassengers();}
    @Override protected Vec3d getPassengerAttachmentPos(Entity passenger,EntityDimensions dimensions,float scale){return new Vec3d(0,.82,0);}
    @Override public ItemStack getPickBlockStack(){return new ItemStack(LunarHorizons.ROVER_ITEM);}
    @Override public float getStepHeight(){return 1.0f;}
    void control(ServerPlayerEntity p,int input){controller=p;flags=input;lastInput=age;}
    @Override public ActionResult interact(PlayerEntity player,Hand hand){
        if(hand!=Hand.MAIN_HAND||player.isSpectator())return ActionResult.PASS;
        if(getWorld().isClient)return ActionResult.SUCCESS;
        if(!(player instanceof ServerPlayerEntity p)||hasPassengers())return ActionResult.CONSUME;
        ItemStack held=p.getStackInHand(hand);
        if(held.isOf(LunarHorizons.POWER_CELL)){
            if(energy()<=CAPACITY-600){energy(energy()+600);if(!p.isCreative())held.decrement(1);}
            RocketEntity.message(p,"rover_charge",energy(),CAPACITY);return ActionResult.CONSUME;
        }
        if(p.isSneaking()&&held.isEmpty()){
            ItemStack item=new ItemStack(LunarHorizons.ROVER_ITEM);
            if(!p.getInventory().insertStack(item)&&!item.isEmpty())p.dropItem(item,false);
            discard();return ActionResult.CONSUME;
        }
        if(p.startRiding(this,true)){flags=0;lastInput=-100;controller=null;RocketEntity.message(p,"rover_controls");}
        return ActionResult.CONSUME;
    }
    @Override public Vec3d updatePassengerForDismount(LivingEntity p){
        if(getWorld() instanceof ServerWorld w){
            for(int y=0;y<=1;y++)for(Direction d:Direction.Type.HORIZONTAL){
                BlockPos pos=getBlockPos().offset(d,2).up(y);
                if(ColonyGrid.loaded(w,pos)&&w.getWorldBorder().contains(pos)&&w.getBlockState(pos.down()).isSolidBlock(w,pos.down())&&w.isAir(pos)&&w.isAir(pos.up()))return Vec3d.ofBottomCenter(pos);
            }
        }
        return super.updatePassengerForDismount(p);
    }
    @Override public void updateTrackedPositionAndAngles(double x,double y,double z,float yaw,float pitch,int count){
        if(!getWorld().isClient){super.updateTrackedPositionAndAngles(x,y,z,yaw,pitch,count);return;}
        lx=x;ly=y;lz=z;lyaw=yaw;steps=Math.max(1,count);
    }
    @Override public void tick(){
        super.tick();
        if(getWorld().isClient){
            if(steps>0){setPosition(getX()+(lx-getX())/steps,getY()+(ly-getY())/steps,getZ()+(lz-getZ())/steps);setYaw(getYaw()+MathHelper.wrapDegrees(lyaw-getYaw())/steps);steps--;}
            return;
        }
        if(!(getWorld() instanceof ServerWorld w))return;
        ServerPlayerEntity pilot=getFirstPassenger() instanceof ServerPlayerEntity p&&p.isAlive()?p:null;
        int input=pilot!=null&&controller==pilot&&age-lastInput<=8?flags:0;
        boolean powered=pilot!=null&&(pilot.isCreative()||energy()>0);
        int throttle=((input&1)!=0?1:0)-((input&2)!=0?1:0);
        int steer=((input&8)!=0?1:0)-((input&4)!=0?1:0);
        if(powered&&steer!=0)setYaw(getYaw()+steer*3.2f*(drive<-.02?-1:1));
        double target=powered?throttle*(throttle<0?.21:.43):0;
        if((input&16)!=0)target=0;
        drive+=(target-drive)*(target==0?.22:.12);
        if(Math.abs(drive)<.005)drive=0;
        double angle=Math.toRadians(getYaw());
        vertical=isOnGround()?-.04:Math.max(-1.1,vertical-(w.getRegistryKey().equals(LunarHorizons.MOON)?.022:.075));
        Vec3d movement=new Vec3d(-Math.sin(angle)*drive,vertical,Math.cos(angle)*drive);
        BlockPos next=BlockPos.ofFloored(getPos().add(movement));
        if(!ColonyGrid.loaded(w,next)||!w.getWorldBorder().contains(next)){movement=new Vec3d(0,vertical,0);drive=0;}
        double beforeX=getX(),beforeZ=getZ();
        move(MovementType.SELF,movement);
        double moved=Math.hypot(getX()-beforeX,getZ()-beforeZ);
        if(isOnGround())vertical=0;
        if(horizontalCollision)drive*=.35;
        getDataTracker().set(SPEED,(int)Math.round(moved*200));
        if(powered&&!pilot.isCreative()&&(moved>.012||steer!=0))energy(energy()-1);
        if(pilot==null&&age%20==0&&energy()<CAPACITY){
            // Park within two blocks of a colony battery. Transfers are lossless and bounded.
            BlockPos center=getBlockPos();
            outer:for(int dx=-2;dx<=2;dx++)for(int dy=-1;dy<=1;dy++)for(int dz=-2;dz<=2;dz++){
                BlockPos q=center.add(dx,dy,dz);
                if(!ColonyGrid.loaded(w,q))continue;
                if(w.getBlockEntity(q) instanceof ColonyBlockEntity be&&be.kind()==ColonyBlockEntity.Kind.BATTERY&&be.energy>0){
                    int charge=Math.min(Math.min(60,be.energy),CAPACITY-energy());be.energy-=charge;be.markDirty();energy(energy()+charge);break outer;
                }
            }
        }
        setVelocity(Vec3d.ZERO);
    }
}

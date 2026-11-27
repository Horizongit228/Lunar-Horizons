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
    public boolean advanced(){return getType()==LunarHorizons.ADVANCED_ROVER_ENTITY;}
    public int capacity(){return advanced()?10800:CAPACITY;}
    private static final TrackedData<Integer> ENERGY=DataTracker.registerData(RoverEntity.class,TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SPEED=DataTracker.registerData(RoverEntity.class,TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> STEERING=DataTracker.registerData(RoverEntity.class,TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> STATUS=DataTracker.registerData(RoverEntity.class,TrackedDataHandlerRegistry.INTEGER);
    public static final int PRECISION=1,CHARGING=2,SOLAR=4,LOW_ENERGY=8;
    private int flags,lastInput=-100,steps;
    private float wheelAngle,previousWheelAngle,steeringAngle,previousSteeringAngle;
    public float wheelAngle(float delta){return previousWheelAngle+(wheelAngle-previousWheelAngle)*delta;}
    public float steeringAngle(float delta){return previousSteeringAngle+(steeringAngle-previousSteeringAngle)*delta;}
    public int status(){return getDataTracker().get(STATUS);}
    private double lx,ly,lz,drive=0,vertical=0;
    private float lyaw;
    private ServerPlayerEntity controller;
    public RoverEntity(EntityType<? extends RoverEntity> type,World world){super(type,world);}
    @Override protected void initDataTracker(DataTracker.Builder b){b.add(ENERGY,0);b.add(SPEED,0);b.add(STEERING,0);b.add(STATUS,0);}
    public int energy(){return getDataTracker().get(ENERGY);}
    public int speed(){return getDataTracker().get(SPEED);}
    void energy(int e){getDataTracker().set(ENERGY,MathHelper.clamp(e,0,capacity()));}
    @Override protected void readCustomDataFromNbt(NbtCompound nbt){energy(nbt.getInt("Energy"));}
    @Override protected void writeCustomDataToNbt(NbtCompound nbt){nbt.putInt("Energy",energy());}
    @Override public boolean canHit(){return isAlive();}
    @Override public boolean isPushable(){return false;}
    @Override public boolean canUsePortals(boolean allowVehicles){return false;}
    @Override public boolean damage(DamageSource source,float amount){return false;}
    @Override protected boolean canAddPassenger(Entity p){return !hasPassengers()&&p instanceof PlayerEntity;}
    @Override public boolean couldAcceptPassenger(){return !hasPassengers();}
    @Override protected Vec3d getPassengerAttachmentPos(Entity passenger,EntityDimensions dimensions,float scale){return new Vec3d(0,advanced()?1.02:.82,0);}
    @Override public ItemStack getPickBlockStack(){return new ItemStack(advanced()?LunarHorizons.ADVANCED_ROVER_ITEM:LunarHorizons.ROVER_ITEM);}
    @Override public float getStepHeight(){return advanced()?1.25f:1.0f;}
    void control(ServerPlayerEntity p,int input){controller=p;flags=input&63;lastInput=age;}
    @Override public ActionResult interact(PlayerEntity player,Hand hand){
        if(hand!=Hand.MAIN_HAND||player.isSpectator())return ActionResult.PASS;
        if(getWorld().isClient)return ActionResult.SUCCESS;
        if(!(player instanceof ServerPlayerEntity p)||hasPassengers())return ActionResult.CONSUME;
        ItemStack held=p.getStackInHand(hand);
        if(held.isOf(LunarHorizons.POWER_CELL)){
            if(energy()<=capacity()-600){energy(energy()+600);if(!p.isCreative())held.decrement(1);}
            RocketEntity.message(p,"rover_charge",energy(),capacity());return ActionResult.CONSUME;
        }
        if(p.isSneaking()&&held.isEmpty()){
            ItemStack item=new ItemStack(advanced()?LunarHorizons.ADVANCED_ROVER_ITEM:LunarHorizons.ROVER_ITEM);
            RoverItem.storeCharge(item,energy());
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
            previousWheelAngle=wheelAngle;wheelAngle+=speed()/200f/(advanced()?.3125f:.30f);
            if(Math.abs(wheelAngle)>Math.PI*2){float shift=(float)(Math.PI*2)*Math.signum(wheelAngle);wheelAngle-=shift;previousWheelAngle-=shift;}
            previousSteeringAngle=steeringAngle;steeringAngle+=(getDataTracker().get(STEERING)/1000f-steeringAngle)*.32f;
            if(steps>0){setPosition(getX()+(lx-getX())/steps,getY()+(ly-getY())/steps,getZ()+(lz-getZ())/steps);setYaw(getYaw()+MathHelper.wrapDegrees(lyaw-getYaw())/steps);steps--;}
            return;
        }
        if(!(getWorld() instanceof ServerWorld w))return;
        ServerPlayerEntity pilot=getFirstPassenger() instanceof ServerPlayerEntity p&&p.isAlive()?p:null;
        if(pilot==null||controller!=pilot||age-lastInput>8){controller=null;flags=0;}
        int input=controller!=null?flags:0;
        boolean powered=pilot!=null&&(pilot.isCreative()||energy()>0);
        int throttle=((input&1)!=0?1:0)-((input&2)!=0?1:0);
        int steer=((input&8)!=0?1:0)-((input&4)!=0?1:0);
        boolean precision=(input&32)!=0,brake=(input&16)!=0;
        boolean grounded=isOnGround();
        if(powered&&steer!=0&&grounded&&!brake){
            float turn=(advanced()?3.8f:3.2f)*(float)(1-.42*Math.min(1,Math.abs(drive)/.62));
            setYaw(getYaw()+steer*turn*(drive<-.02?-1:1));
        }
        getDataTracker().set(STEERING,powered&&!brake?steer*360:0);
        double target=powered?throttle*(throttle<0?(advanced()?.30:.21):(advanced()?.62:.43)):0;
        if(precision)target*=.38;
        if(brake)target=0;
        double response=target==0?(brake?.55:.22):.12;
        if(grounded||brake)drive+=(target-drive)*response;
        else drive*=.995;
        if(Math.abs(drive)<.005)drive=0;
        double angle=Math.toRadians(getYaw());
        vertical=grounded?-.04:w.getRegistryKey().equals(LunarHorizons.SPACE)?Math.min(-.025,vertical):Math.max(-1.1,vertical-(w.getRegistryKey().equals(LunarHorizons.MOON)?.022:.075));
        Vec3d movement=new Vec3d(-Math.sin(angle)*drive,vertical,Math.cos(angle)*drive);
        BlockPos next=BlockPos.ofFloored(getPos().add(movement));
        if(!ColonyGrid.loaded(w,next)||!w.getWorldBorder().contains(next)){movement=Vec3d.ZERO;drive=0;vertical=0;}
        double beforeX=getX(),beforeZ=getZ();
        move(MovementType.SELF,movement);
        double moved=Math.hypot(getX()-beforeX,getZ()-beforeZ);
        if(isOnGround())vertical=0;
        if(horizontalCollision)drive*=.35;
        getDataTracker().set(SPEED,(int)Math.round(moved*200*(drive<0?-1:1)));
        if(powered&&!pilot.isCreative()&&grounded&&(throttle!=0&&!brake||steer!=0&&!brake))energy(energy()-1);
        boolean parked=pilot==null&&isOnGround()&&Math.abs(drive)<.005;
        int mode=precision?PRECISION:0;
        if(parked&&energy()<capacity()&&age%20!=0)mode|=status()&(CHARGING|SOLAR);
        boolean sunlight=advanced()&&parked&&age%20==0&&w.isSkyVisible(getBlockPos().up(2))&&(LunarHorizons.vacuum(w)||w.isDay());
        if(sunlight&&energy()<capacity()){mode|=SOLAR;energy(energy()+15);}
        if(parked&&age%20==0&&energy()<capacity()){
            // Select the nearest enabled battery, with deterministic tie breaking.
            BlockPos center=getBlockPos();
            ColonyBlockEntity nearest=null;int best=Integer.MAX_VALUE;
            for(int dx=-2;dx<=2;dx++)for(int dy=-1;dy<=1;dy++)for(int dz=-2;dz<=2;dz++){
                BlockPos q=center.add(dx,dy,dz);int distance=dx*dx+dy*dy+dz*dz;
                if(distance>=best||!ColonyGrid.loaded(w,q))continue;
                if(w.getBlockEntity(q) instanceof ColonyBlockEntity be&&be.kind()==ColonyBlockEntity.Kind.BATTERY&&be.enabled&&be.energy>0){nearest=be;best=distance;}
            }
            if(nearest!=null){
                mode|=CHARGING;
                if(age%20==0){
                    int charge=Math.min(Math.min(advanced()?120:60,nearest.energy),capacity()-energy());
                    nearest.energy-=charge;nearest.setActive(nearest.energy>0);nearest.markDirty();energy(energy()+charge);
                }
            }
        }
        if(energy()>=capacity())mode&=~(CHARGING|SOLAR);
        if(energy()*10<=capacity())mode|=LOW_ENERGY;
        getDataTracker().set(STATUS,mode);
        setVelocity(Vec3d.ZERO);
    }
}

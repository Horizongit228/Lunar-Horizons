package com.lunarhorizons;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;

public final class RocketEntity extends Entity {
    public static final int IDLE=0, COUNTDOWN=1, ASCENT=2, DESCENT=3, READY=4;
    private static final TrackedData<Integer> PHASE = DataTracker.registerData(RocketEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> ALTITUDE = DataTracker.registerData(RocketEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> COUNTDOWN_SYNC = DataTracker.registerData(RocketEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> FUEL_RESERVE = DataTracker.registerData(RocketEntity.class,TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> DESTINATION = DataTracker.registerData(RocketEntity.class,TrackedDataHandlerRegistry.INTEGER);
    public static final int EARTH_DEST=0,MOON_DEST=1,SPACE_DEST=2;
    private static final TrackedData<Integer> SPEED = DataTracker.registerData(RocketEntity.class,TrackedDataHandlerRegistry.INTEGER);
    public static final int FUEL_CAPACITY=8;
    public boolean advanced(){return getType()==LunarHorizons.ADVANCED_ROCKET_ENTITY;}
    public int fuelCapacity(){return advanced()?16:FUEL_CAPACITY;}
    public int hullHeight(){return height(advanced());}
    private static int height(boolean advanced){return advanced?12:8;}
    public double cabinHeight(){return advanced()?7.5:4.5;}
    private int countdownTicks(){return advanced()?60:COUNTDOWN_TICKS;}
    private static final int COUNTDOWN_TICKS = 100;
    private double lastFlightY=Double.NaN;
    private BlockPos landing = BlockPos.ORIGIN;
    private BlockPos home = null,moonHome=null;
    private int phaseTicks, missingPilotTicks;
    private int lerpSteps;
    private double lerpX, lerpY, lerpZ;
    private float lerpYaw;

    public RocketEntity(EntityType<? extends RocketEntity> type, World world) {
        super(type, world);
        setNoGravity(true);
    }

    @Override protected void initDataTracker(DataTracker.Builder builder) { builder.add(PHASE, IDLE); builder.add(ALTITUDE,0); builder.add(COUNTDOWN_SYNC,0); builder.add(FUEL_RESERVE,0); builder.add(SPEED,0);builder.add(DESTINATION,SPACE_DEST); }
    public int destination(){return getDataTracker().get(DESTINATION);}
    private void destination(int d){getDataTracker().set(DESTINATION,MathHelper.clamp(d,0,2));}
    private static int location(World w){return w.getRegistryKey().equals(LunarHorizons.SPACE)?SPACE_DEST:w.getRegistryKey().equals(LunarHorizons.MOON)?MOON_DEST:EARTH_DEST;}
    private static boolean travelWorld(World w){return w.getRegistryKey().equals(World.OVERWORLD)||LunarHorizons.vacuum(w);}
    public int phase() { return getDataTracker().get(PHASE); }
    private void phase(int value) { getDataTracker().set(PHASE,value); phaseTicks=0; missingPilotTicks=0; }
    public int flightAltitude(){return getDataTracker().get(ALTITUDE);}
    public int countdownSeconds(){return getDataTracker().get(COUNTDOWN_SYNC);}
    public int fuelReserve(){return getDataTracker().get(FUEL_RESERVE);}
    private void fuelReserve(int fuel){getDataTracker().set(FUEL_RESERVE,MathHelper.clamp(fuel,0,fuelCapacity()));}
    public int flightSpeedTenths(){return getDataTracker().get(SPEED);}
    public boolean warmingUp(){return phase()==COUNTDOWN&&countdownSeconds()<=2;}
    public boolean enginesOn() { return phase()==ASCENT || phase()==DESCENT; }

    @Override protected void readCustomDataFromNbt(NbtCompound nbt) {
        landing = nbt.contains("Landing") ? BlockPos.fromLong(nbt.getLong("Landing")) : getBlockPos();
        home = nbt.contains("Home") ? BlockPos.fromLong(nbt.getLong("Home")) : null;
        moonHome=nbt.contains("MoonHome")?BlockPos.fromLong(nbt.getLong("MoonHome")):null;
        destination(nbt.contains("Destination")?nbt.getInt("Destination"):location(getWorld())==SPACE_DEST?EARTH_DEST:SPACE_DEST);
        phase(MathHelper.clamp(nbt.getInt("Phase"), IDLE, READY));
        phaseTicks = Math.max(0, nbt.getInt("PhaseTicks"));
        fuelReserve(nbt.getInt("FuelReserve"));
    }
    @Override protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putLong("Landing",landing.asLong());
        if (home!=null) nbt.putLong("Home",home.asLong());
        if(moonHome!=null)nbt.putLong("MoonHome",moonHome.asLong());nbt.putInt("Destination",destination());
        nbt.putInt("Phase",phase());
        nbt.putInt("PhaseTicks",phaseTicks);
        nbt.putInt("FuelReserve",fuelReserve());
    }

    @Override public boolean canHit() { return isAlive(); }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canUsePortals(boolean allowVehicles) { return false; }
    @Override protected boolean canAddPassenger(Entity passenger) { return !hasPassengers() && passenger instanceof PlayerEntity; }
    @Override public boolean couldAcceptPassenger() { return !hasPassengers(); }
    @Override protected Vec3d getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
        return new Vec3d(0,cabinHeight(),0);
    }
    @Override public ItemStack getPickBlockStack() { return new ItemStack(advanced()?LunarHorizons.ADVANCED_ROCKET_ITEM:LunarHorizons.ROCKET_ITEM); }

    // Shift dismounts normally; during ascent/descent it is an emergency exit to ground.
    @Override public Vec3d updatePassengerForDismount(LivingEntity passenger) {
        if (getWorld() instanceof ServerWorld world) {
            if(world.getRegistryKey().equals(LunarHorizons.SPACE)){
                if(phase()==IDLE||phase()==READY||phase()==COUNTDOWN)return Vec3d.ofBottomCenter(landing.east(3));
                return new Vec3d(getX()+2.1,getY()+cabinHeight(),getZ());
            }
            BlockPos center = phase()==IDLE ? getBlockPos() : landing;
            for (int ring=2;ring<=5;ring++) for (int dx=-ring;dx<=ring;dx++) for (int dz=-ring;dz<=ring;dz++) {
                if (Math.max(Math.abs(dx),Math.abs(dz))!=ring) continue;
                int x=center.getX()+dx,z=center.getZ()+dz;
                int y=world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,x,z);
                BlockPos pos=new BlockPos(x,y,z);
                if (world.getWorldBorder().contains(pos) && world.isInBuildLimit(pos.up()) &&
                    world.getBlockState(pos.down()).isSolidBlock(world,pos.down()) &&
                    world.getBlockState(pos).isAir() && world.getBlockState(pos.up()).isAir() &&
                    world.getFluidState(pos.down()).isEmpty()) return Vec3d.ofBottomCenter(pos);
            }
            // Fallback to the edge of our pad, which is not a physical entity collision.
            return Vec3d.ofBottomCenter(center.east());
        }
        return super.updatePassengerForDismount(passenger);
    }

    @Override public boolean damage(DamageSource source, float amount) {
        // Vehicle is recovered via sneak-use, never duplicated through damage.
        return false;
    }

    static void message(ServerPlayerEntity player, String key, Object... args) {
        player.sendMessage(Text.translatable("message.lunar_horizons."+key,args),true);
    }
    private static int fuelSlot(ServerPlayerEntity player) {
        for(int i=0;i<player.getInventory().size();i++) if(player.getInventory().getStack(i).isOf(LunarHorizons.FUEL)) return i;
        return -1;
    }
    static boolean hasPad(ServerWorld world, BlockPos center) {
        for(int x=-1;x<=1;x++) for(int z=-1;z<=1;z++)
            if(!world.getBlockState(center.add(x,-1,z)).isOf(LunarHorizons.LAUNCH_PAD)) return false;
        return true;
    }
    private static Box boxAt(BlockPos p, double height) {
        return new Box(p.getX()-1,p.getY(),p.getZ()-1,p.getX()+2,p.getY()+height,p.getZ()+2);
    }
    private static boolean clear(ServerWorld world, BlockPos center, int height) {
        if(height<=0 || !world.isInBuildLimit(center) || !world.isInBuildLimit(center.up(height-1))) return false;
        for(int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) {
            if(!world.getWorldBorder().contains(center.add(x,0,z))) return false;
            for(int y=0;y<height;y++) if(!world.getBlockState(center.add(x,y,z)).isAir()) return false;
        }
        return true;
    }
    private static boolean occupied(ServerWorld world, BlockPos pos, int height) {
        return !world.getEntitiesByClass(RocketEntity.class,boxAt(pos,height), Entity::isAlive).isEmpty();
    }
    static RocketEntity deploy(ServerWorld world, BlockPos base, float yaw) {
        return deploy(world,base,yaw,false);
    }
    static RocketEntity deploy(ServerWorld world, BlockPos base,float yaw,boolean advanced){
        if(!hasPad(world,base)||!clear(world,base,height(advanced))||occupied(world,base,height(advanced)))return null;
        RocketEntity rocket=new RocketEntity(advanced?LunarHorizons.ADVANCED_ROCKET_ENTITY:LunarHorizons.ROCKET_ENTITY,world);
        rocket.landing=base.toImmutable();
        rocket.refreshPositionAndAngles(base.getX()+.5,base.getY(),base.getZ()+.5,Math.round(yaw/90f)*90f,0);
        return world.spawnEntity(rocket)?rocket:null;
    }

    static void convertLegacy(ServerPlayerEntity p,ServerWorld world,BlockPos pos) {
        if(!hasPad(world,pos)) {message(p,"big_pad");return;}
        var old=world.getBlockState(pos);
        world.removeBlock(pos,false);
        RocketEntity rocket=deploy(world,pos,p.getYaw());
        if(rocket==null) { world.setBlockState(pos,old);message(p,"big_blocked");return; }
        message(p,"converted");
        rocket.board(p,world);
    }

    @Override public ActionResult interact(PlayerEntity player, Hand hand) {
        if(hand!=Hand.MAIN_HAND || player.isSpectator()) return ActionResult.PASS;
        if(getWorld().isClient) return ActionResult.SUCCESS;
        if(!(player instanceof ServerPlayerEntity p) || !(getWorld() instanceof ServerWorld world)) return ActionResult.PASS;
        if(phase()!=IDLE || hasPassengers()) {message(p,"busy");return ActionResult.CONSUME;}
        ItemStack held=p.getStackInHand(hand);
        if(held.isOf(LunarHorizons.FUEL)){
            int add=Math.min(fuelCapacity()-fuelReserve(),p.isSneaking()?held.getCount():1);
            if(add<=0){message(p,"rocket_tank_full");return ActionResult.CONSUME;}
            fuelReserve(fuelReserve()+add);
            if(!p.isCreative())held.decrement(add);
            message(p,"rocket_refueled",fuelReserve(),fuelCapacity());
            return ActionResult.CONSUME;
        }
        if(p.isSneaking()) {
            if(fuelReserve()>0){
                ItemStack reserve=new ItemStack(LunarHorizons.FUEL,fuelReserve());
                if(!p.getInventory().insertStack(reserve)&&!reserve.isEmpty())p.dropItem(reserve,false);
                fuelReserve(0);
            }
            ItemStack stack = new ItemStack(advanced()?LunarHorizons.ADVANCED_ROCKET_ITEM:LunarHorizons.ROCKET_ITEM);
            if(!p.getInventory().insertStack(stack) && !stack.isEmpty()) p.dropItem(stack,false);
            discard();
            return ActionResult.CONSUME;
        }
        board(p,world);
        return ActionResult.CONSUME;
    }

    private boolean ready(ServerPlayerEntity p, ServerWorld world) {
        if(!travelWorld(world)){message(p,"wrong_world");return false;}
        if(destination()==location(world)){message(p,"wrong_destination");return false;}
        if(!hasPad(world,landing)){message(p,"big_pad");return false;}
        if(!clear(world,landing,hullHeight()) || world.getTopY()-landing.getY()<hullHeight()+10){message(p,"big_blocked");return false;}
        int height = Math.min(64, world.getTopY()-landing.getY());
        if(!clear(world,landing,height)){message(p,"launch_corridor");return false;}
        if(!p.isCreative() && fuelReserve()==0 && fuelSlot(p)<0){message(p,"need_fuel");return false;}
        if(!p.isCreative() && destination()!=EARTH_DEST &&
                !OxygenSystem.ready(p)){message(p,"need_suit");return false;}
        return true;
    }
    private void board(ServerPlayerEntity p,ServerWorld world) {
        if(phase()!=IDLE || hasPassengers()) return;
        if(!travelWorld(world)){message(p,"wrong_world");return;}
        if(p.startRiding(this,true)){if(destination()==location(world))destination(location(world)==SPACE_DEST?EARTH_DEST:SPACE_DEST);phase(READY);message(p,"ready_launch");message(p,"destination",Text.translatable("hud.lunar_horizons.destination_"+destination()));}
    }

    public void requestDestination(ServerPlayerEntity pilot){
        if(getWorld().isClient||getFirstPassenger()!=pilot||phase()!=READY||!pilot.isAlive())return;
        int next=(destination()+1)%3;if(next==location(getWorld()))next=(next+1)%3;
        destination(next);message(pilot,"destination",Text.translatable("hud.lunar_horizons.destination_"+next));
    }
    public void requestLaunch(ServerPlayerEntity pilot){
        if(getWorld().isClient || !(getWorld() instanceof ServerWorld world) || getFirstPassenger()!=pilot || !pilot.isAlive())return;
        if(phase()==READY && ready(pilot,world)){phase(COUNTDOWN);getDataTracker().set(COUNTDOWN_SYNC,countdownTicks()/20);message(pilot,"countdown",countdownTicks()/20);}
        else if(phase()==COUNTDOWN){phase(READY);getDataTracker().set(COUNTDOWN_SYNC,0);message(pilot,"launch_cancelled");}
        else if(phase()==ASCENT){phase(DESCENT);message(pilot,"rocket_abort");}
    }

    @Override public void updateTrackedPositionAndAngles(double x,double y,double z,float yaw,float pitch,int steps) {
        if(!getWorld().isClient){super.updateTrackedPositionAndAngles(x,y,z,yaw,pitch,steps);return;}
        lerpX=x;lerpY=y;lerpZ=z;lerpYaw=yaw;lerpSteps=Math.max(1,steps);
    }
    private void clientTick() {
        if(lerpSteps>0){
            setPosition(getX()+(lerpX-getX())/lerpSteps,getY()+(lerpY-getY())/lerpSteps,getZ()+(lerpZ-getZ())/lerpSteps);
            setYaw(getYaw()+MathHelper.wrapDegrees(lerpYaw-getYaw())/lerpSteps);
            lerpSteps--;
        }
    }

    @Override public void tick() {
        super.tick();
        if(getWorld().isClient){clientTick();return;}
        if(!(getWorld() instanceof ServerWorld world))return;
        setVelocity(Vec3d.ZERO);
        ServerPlayerEntity pilot=getFirstPassenger() instanceof ServerPlayerEntity p && p.isAlive()?p:null;
        phaseTicks++;
        int speed=Double.isNaN(lastFlightY)?0:(int)Math.round((getY()-lastFlightY)*200);
        getDataTracker().set(SPEED,enginesOn()?speed:0);
        lastFlightY=getY();
        getDataTracker().set(ALTITUDE,Math.max(0,(int)Math.ceil(getY()-landing.getY())));
        getDataTracker().set(COUNTDOWN_SYNC,phase()==COUNTDOWN?Math.max(0,(countdownTicks()-phaseTicks+19)/20):0);
        switch(phase()){
            case READY -> { if(pilot==null && ++missingPilotTicks>60)phase(IDLE); else if(pilot!=null)missingPilotTicks=0; }
            case IDLE -> { if(hasPassengers() && pilot!=null){pilot.stopRiding();} }
            case COUNTDOWN -> {
                if(pilot==null){phase(IDLE);getDataTracker().set(COUNTDOWN_SYNC,0);return;}
                missingPilotTicks=0;
                if(phaseTicks==1 || phaseTicks%20==0 && phaseTicks<countdownTicks()){
                    message(pilot,"countdown",Math.max(1,(countdownTicks()-phaseTicks+19)/20));
                    world.playSound(null,getBlockPos(),SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(),SoundCategory.NEUTRAL,.8f,1f);
                }
                if(phaseTicks>countdownTicks()-40 && phaseTicks%4==0) exhaust(world,false);
                if(phaseTicks>=countdownTicks()){
                    if(!ready(pilot,world)){phase(READY);return;}
                    if(!pilot.isCreative()){
                        if(fuelReserve()>0)fuelReserve(fuelReserve()-1);
                        else {pilot.getInventory().getStack(fuelSlot(pilot)).decrement(1);pilot.getInventory().markDirty();}
                    }
                    if(world.getRegistryKey().equals(World.OVERWORLD)) home=landing;
                    if(world.getRegistryKey().equals(LunarHorizons.MOON))moonHome=landing;
                    phase(ASCENT);
                    world.playSound(null,getBlockPos(),SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH,SoundCategory.NEUTRAL,3f,.6f);
                    message(pilot,"liftoff");
                }
            }
            case ASCENT -> {
                if(pilot==null){if(++missingPilotTicks>60)phase(DESCENT);return;}
                missingPilotTicks=0;
                double travel=Math.min(48,world.getTopY()-landing.getY()-hullHeight()-1);
                double rise=advanced()?Math.min(1.55,.08+phaseTicks*.015):Math.min(1.05,.045+phaseTicks*.0075);
                double next=Math.min(landing.getY()+travel,getY()+rise);
                // Entire sweep is checked for obstructions; no blocks are broken.
                if(!clear(world,landing,Math.min(world.getTopY()-landing.getY(),(int)Math.ceil(next-landing.getY())+hullHeight()))){message(pilot,"launch_corridor");phase(DESCENT);return;}
                setPosition(getX(),next,getZ());
                exhaust(world,true);
                if(phaseTicks%20==0)message(pilot,"altitude",(int)(getY()-landing.getY()));
                if(next>=landing.getY()+travel) transfer(world,pilot);
            }
            case DESCENT -> {
                double remaining=getY()-landing.getY();
                double descentSpeed=Math.min(advanced()?1.0:.72,Math.max(.035,remaining*.085));
                double next=Math.max(landing.getY(),getY()-descentSpeed);
                int y=(int)Math.floor(next);
                if(!clear(world,new BlockPos(landing.getX(),y,landing.getZ()),hullHeight()+1)){
                    // Stop over new terrain instead of clipping through a newly built roof.
                    BlockPos alternate=world.getRegistryKey().equals(LunarHorizons.SPACE)?findDock(world):findLanding(world,landing);
                    if(alternate==null){if(pilot!=null&&phaseTicks%40==0)message(pilot,"no_landing");return;}
                    landing=alternate;setPosition(landing.getX()+.5,landing.getY()+descentHeight(world,landing),landing.getZ()+.5);
                    return;
                }
                setPosition(getX(),next,getZ());
                exhaust(world,true);
                if(pilot!=null && phaseTicks%20==0) message(pilot,"landing_altitude",(int)Math.ceil(remaining));
                if(next<=landing.getY()+.001){
                    phase(IDLE);
                    world.playSound(null,getBlockPos(),SoundEvents.BLOCK_METAL_HIT,SoundCategory.NEUTRAL,1.3f,.65f);
                    if(pilot!=null){message(pilot,world.getRegistryKey().equals(LunarHorizons.SPACE)?"arrived_space":world.getRegistryKey().equals(LunarHorizons.MOON)?"arrived_moon":"arrived_earth");pilot.stopRiding();}
                }
            }
        }
    }

    private void exhaust(ServerWorld world, boolean full) {
        // Match the two distinct engine clusters to the renderer's quarter-block coordinates.
        double angle=Math.toRadians(getYaw());
        double[][] engines=advanced()?new double[][]{{0,0},{.80,0},{-.80,0},{0,.80},{0,-.80}}:new double[][]{{-.45,-.45},{-.45,.45},{.45,-.45},{.45,.45}};
        for(double[] engine:engines){double dx=engine[0],dz=engine[1];
            double x=getX()+dx*Math.cos(angle)-dz*Math.sin(angle),z=getZ()+dx*Math.sin(angle)+dz*Math.cos(angle);
            if(!world.getRegistryKey().equals(LunarHorizons.SPACE))world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,x,getY()-.12,z,full?4:1,.17,.12,.17,.025);
            if(full){
                world.spawnParticles(ParticleTypes.FLAME,x,getY()-.35,z,6,.12,.22,.12,.04);
                world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,x,getY()-.10,z,2,.07,.08,.07,.015);
            }
        }
        if(full && phaseTicks%20==0)world.playSound(null,getBlockPos(),SoundEvents.ENTITY_BLAZE_SHOOT,SoundCategory.NEUTRAL,1.3f,.55f);
    }

    private int descentHeight(ServerWorld world,BlockPos pos){return Math.min(36,world.getTopY()-pos.getY()-hullHeight()-1);}
    private BlockPos findLanding(ServerWorld world,BlockPos preferred) {
        for(int ring=0;ring<=5;ring++)for(int dx=-ring;dx<=ring;dx++)for(int dz=-ring;dz<=ring;dz++){
            if(Math.max(Math.abs(dx),Math.abs(dz))!=ring)continue;
            int x=preferred.getX()+dx*5,z=preferred.getZ()+dz*5;
            if(!world.getWorldBorder().contains(new BlockPos(x,world.getBottomY(),z)))continue;
            int y=world.getBottomY();
            for(int a=-1;a<=1;a++)for(int b=-1;b<=1;b++)y=Math.max(y,world.getTopY(Heightmap.Type.MOTION_BLOCKING,x+a,z+b));
            BlockPos pos=new BlockPos(x,y,z);
            boolean existing=hasPad(world,pos);
            if(!existing)pos=pos.up();
            int h=descentHeight(world,pos);
            if(h<12 || !clear(world,pos,h+hullHeight())||occupied(world,pos,h+hullHeight()))continue;
            if(!existing){
                if(!clear(world,pos.down(),1))continue;
                // Require a solid footing within three blocks under every pad column.
                boolean supported=true;
                for(int a=-1;a<=1;a++)for(int b=-1;b<=1;b++){
                    BlockPos footing=pos.add(a,-2,b);
                    boolean found=false;
                    for(int drop=0;drop<=2;drop++){
                        BlockPos q=footing.down(drop);
                        if(world.getBlockState(q).isSolidBlock(world,q)&&world.getFluidState(q).isEmpty()){found=true;break;}
                        if(!world.isAir(q))break;
                    }
                    if(!found)supported=false;
                }
                if(!supported)continue;
                for(int a=-1;a<=1;a++)for(int b=-1;b<=1;b++){
                    BlockPos footing=pos.add(a,-2,b);
                    for(int drop=0;drop<=2;drop++){
                        BlockPos q=footing.down(drop);
                        if(!world.isAir(q))break;
                        world.setBlockState(q,LunarHorizons.MOON_BRICKS.getDefaultState());
                    }
                    world.setBlockState(pos.add(a,-1,b),LunarHorizons.LAUNCH_PAD.getDefaultState());
                }
            }
            return pos;
        }
        return null;
    }

    private BlockPos findDock(ServerWorld world){
        for(int i=0;i<OrbitalStationFeature.DOCK_X.length;i++){
            BlockPos p=OrbitalStationFeature.dock(i);
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)world.getChunk((p.getX()+x)>>4,(p.getZ()+z)>>4);
            if(descentHeight(world,p)>=12&&hasPad(world,p)&&clear(world,p,descentHeight(world,p)+hullHeight())&&!occupied(world,p,descentHeight(world,p)+hullHeight()))return p;
        }
        return null;
    }
    private void transfer(ServerWorld source,ServerPlayerEntity pilot) {
        int goal=destination();
        if(source.getServer()==null){phase(DESCENT);return;}
        ServerWorld target=source.getServer().getWorld(goal==SPACE_DEST?LunarHorizons.SPACE:goal==MOON_DEST?LunarHorizons.MOON:World.OVERWORLD);
        if(target==null){message(pilot,"missing_world");phase(DESCENT);return;}
        BlockPos preferred=goal==EARTH_DEST?home:moonHome;
        if(preferred==null)preferred=source.getRegistryKey().equals(LunarHorizons.SPACE)?(home!=null?home:moonHome!=null?moonHome:BlockPos.ORIGIN):landing;
        BlockPos dest=goal==SPACE_DEST?findDock(target):findLanding(target,preferred);
        if(dest==null){message(pilot,"no_landing");phase(DESCENT);return;}
        RocketEntity arrival=new RocketEntity(advanced()?LunarHorizons.ADVANCED_ROCKET_ENTITY:LunarHorizons.ROCKET_ENTITY,target);
        arrival.landing=dest;arrival.home=home;arrival.moonHome=moonHome;arrival.destination(goal==SPACE_DEST?location(source):SPACE_DEST);arrival.fuelReserve(fuelReserve());arrival.phase(DESCENT);
        double x=dest.getX()+.5,y=dest.getY()+descentHeight(target,dest),z=dest.getZ()+.5;
        arrival.refreshPositionAndAngles(x,y,z,getYaw(),0);
        if(!target.spawnEntity(arrival)){phase(DESCENT);message(pilot,"no_landing");return;}
        pilot.stopRiding();
        pilot.fallDistance=0;
        pilot.teleportTo(new TeleportTarget(target,new Vec3d(x,y+arrival.cabinHeight(),z),Vec3d.ZERO,pilot.getYaw(),0,TeleportTarget.NO_OP));
        if(pilot.getWorld()!=target){arrival.discard();phase(DESCENT);return;}
        if(!pilot.startRiding(arrival,true)){
            pilot.teleportTo(new TeleportTarget(target,Vec3d.ofBottomCenter(dest.east(3)),Vec3d.ZERO,pilot.getYaw(),0,TeleportTarget.NO_OP));
        }
        pilot.fallDistance=0;
        discard();
        message(pilot,"approach");
    }
}

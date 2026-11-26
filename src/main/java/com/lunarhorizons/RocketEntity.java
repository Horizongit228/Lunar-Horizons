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
    public static final int IDLE=0, COUNTDOWN=1, ASCENT=2, DESCENT=3;
    private static final TrackedData<Integer> PHASE = DataTracker.registerData(RocketEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final int COUNTDOWN_TICKS = 100;
    private BlockPos landing = BlockPos.ORIGIN;
    private BlockPos home = null;
    private int phaseTicks, missingPilotTicks;
    private int lerpSteps;
    private double lerpX, lerpY, lerpZ;
    private float lerpYaw;

    public RocketEntity(EntityType<? extends RocketEntity> type, World world) {
        super(type, world);
        setNoGravity(true);
    }

    @Override protected void initDataTracker(DataTracker.Builder builder) { builder.add(PHASE, IDLE); }
    public int phase() { return getDataTracker().get(PHASE); }
    private void phase(int value) { getDataTracker().set(PHASE,value); phaseTicks=0; missingPilotTicks=0; }
    public boolean enginesOn() { return phase()==ASCENT || phase()==DESCENT; }

    @Override protected void readCustomDataFromNbt(NbtCompound nbt) {
        landing = nbt.contains("Landing") ? BlockPos.fromLong(nbt.getLong("Landing")) : getBlockPos();
        home = nbt.contains("Home") ? BlockPos.fromLong(nbt.getLong("Home")) : null;
        phase(MathHelper.clamp(nbt.getInt("Phase"), IDLE, DESCENT));
        phaseTicks = Math.max(0, nbt.getInt("PhaseTicks"));
    }
    @Override protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putLong("Landing",landing.asLong());
        if (home!=null) nbt.putLong("Home",home.asLong());
        nbt.putInt("Phase",phase());
        nbt.putInt("PhaseTicks",phaseTicks);
    }

    @Override public boolean canHit() { return isAlive(); }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canUsePortals(boolean allowVehicles) { return false; }
    @Override protected boolean canAddPassenger(Entity passenger) { return !hasPassengers() && passenger instanceof PlayerEntity; }
    @Override public boolean couldAcceptPassenger() { return !hasPassengers(); }
    @Override protected Vec3d getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
        return new Vec3d(0,4.5,0);
    }
    @Override public ItemStack getPickBlockStack() { return new ItemStack(LunarHorizons.ROCKET_ITEM); }

    // Shift dismounts normally; during ascent/descent it is an emergency exit to ground.
    @Override public Vec3d updatePassengerForDismount(LivingEntity passenger) {
        if (getWorld() instanceof ServerWorld world) {
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
        if(!world.isInBuildLimit(center) || !world.isInBuildLimit(center.up(height-1))) return false;
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
        if(!hasPad(world,base)||!clear(world,base,8)||occupied(world,base,8)) return null;
        RocketEntity rocket = new RocketEntity(LunarHorizons.ROCKET_ENTITY,world);
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
        if(p.isSneaking()) {
            ItemStack stack = new ItemStack(LunarHorizons.ROCKET_ITEM);
            if(!p.getInventory().insertStack(stack) && !stack.isEmpty()) p.dropItem(stack,false);
            discard();
            return ActionResult.CONSUME;
        }
        board(p,world);
        return ActionResult.CONSUME;
    }

    private boolean ready(ServerPlayerEntity p, ServerWorld world) {
        if(!world.getRegistryKey().equals(World.OVERWORLD)&&!world.getRegistryKey().equals(LunarHorizons.MOON)){message(p,"wrong_world");return false;}
        if(!hasPad(world,landing)){message(p,"big_pad");return false;}
        if(!clear(world,landing,8) || world.getTopY()-landing.getY()<18){message(p,"big_blocked");return false;}
        int height = Math.min(64, world.getTopY()-landing.getY());
        if(!clear(world,landing,height)){message(p,"launch_corridor");return false;}
        if(!p.isCreative() && fuelSlot(p)<0){message(p,"need_fuel");return false;}
        if(!p.isCreative() && world.getRegistryKey().equals(World.OVERWORLD) &&
                (!LunarHorizons.hasHelmet(p)||MoonSurvival.tankSlot(p)<0)){message(p,"need_suit");return false;}
        return true;
    }
    private void board(ServerPlayerEntity p,ServerWorld world) {
        if(phase()!=IDLE || hasPassengers() || !ready(p,world)) return;
        if(p.startRiding(this,true)){phase(COUNTDOWN);message(p,"seated");}
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
        switch(phase()){
            case IDLE -> { if(hasPassengers() && pilot!=null){pilot.stopRiding();} }
            case COUNTDOWN -> {
                if(pilot==null){ if(++missingPilotTicks>60)phase(IDLE); return; }
                missingPilotTicks=0;
                if(phaseTicks==1 || phaseTicks%20==0 && phaseTicks<COUNTDOWN_TICKS){
                    message(pilot,"countdown",Math.max(1,(COUNTDOWN_TICKS-phaseTicks+19)/20));
                    world.playSound(null,getBlockPos(),SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(),SoundCategory.NEUTRAL,.8f,1f);
                }
                if(phaseTicks>60 && phaseTicks%4==0) exhaust(world,false);
                if(phaseTicks>=COUNTDOWN_TICKS){
                    if(!ready(pilot,world)){phase(IDLE);pilot.stopRiding();return;}
                    if(!pilot.isCreative()){
                        pilot.getInventory().getStack(fuelSlot(pilot)).decrement(1);
                        pilot.getInventory().markDirty();
                    }
                    if(world.getRegistryKey().equals(World.OVERWORLD)) home=landing;
                    phase(ASCENT);
                    world.playSound(null,getBlockPos(),SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH,SoundCategory.NEUTRAL,3f,.6f);
                    message(pilot,"liftoff");
                }
            }
            case ASCENT -> {
                if(pilot==null){if(++missingPilotTicks>60)phase(DESCENT);return;}
                missingPilotTicks=0;
                double travel=Math.min(48,world.getTopY()-landing.getY()-9);
                double rise=Math.min(.85,.055+phaseTicks*.009);
                double next=Math.min(landing.getY()+travel,getY()+rise);
                // Entire sweep is checked for obstructions; no blocks are broken.
                if(!clear(world,landing,Math.min(world.getTopY()-landing.getY(),(int)Math.ceil(next-landing.getY())+8))){message(pilot,"launch_corridor");phase(DESCENT);return;}
                setPosition(getX(),next,getZ());
                exhaust(world,true);
                if(phaseTicks%20==0)message(pilot,"altitude",(int)(getY()-landing.getY()));
                if(next>=landing.getY()+travel) transfer(world,pilot);
            }
            case DESCENT -> {
                double remaining=getY()-landing.getY();
                double speed=Math.min(.55,Math.max(.055,remaining*.10));
                double next=Math.max(landing.getY(),getY()-speed);
                int y=(int)Math.floor(next);
                if(!clear(world,new BlockPos(landing.getX(),y,landing.getZ()),9)){
                    // Stop over new terrain instead of clipping through a newly built roof.
                    BlockPos alternate=findLanding(world,landing);
                    if(alternate==null){if(pilot!=null)message(pilot,"no_landing");return;}
                    landing=alternate;setPosition(landing.getX()+.5,landing.getY()+descentHeight(world,landing),landing.getZ()+.5);
                    return;
                }
                setPosition(getX(),next,getZ());
                exhaust(world,true);
                if(pilot!=null && phaseTicks%20==0) message(pilot,"landing_altitude",(int)Math.ceil(remaining));
                if(next<=landing.getY()+.001){
                    phase(IDLE);
                    world.playSound(null,getBlockPos(),SoundEvents.BLOCK_METAL_HIT,SoundCategory.NEUTRAL,1.3f,.65f);
                    if(pilot!=null){message(pilot,world.getRegistryKey().equals(LunarHorizons.MOON)?"arrived_moon":"arrived_earth");pilot.stopRiding();}
                }
            }
        }
    }

    private void exhaust(ServerWorld world, boolean full) {
        int count=full?10:4;
        world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,getX(),getY()+.1,getZ(),count,.65,.08,.65,.03);
        if(full) world.spawnParticles(ParticleTypes.FLAME,getX(),getY()-.25,getZ(),14,.42,.3,.42,.045);
        if(full && phaseTicks%20==0)world.playSound(null,getBlockPos(),SoundEvents.ENTITY_BLAZE_SHOOT,SoundCategory.NEUTRAL,1.3f,.55f);
    }

    private static int descentHeight(ServerWorld world,BlockPos pos){return Math.min(36,world.getTopY()-pos.getY()-9);}
    private static BlockPos findLanding(ServerWorld world,BlockPos preferred) {
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
            if(h<12 || !clear(world,pos,h+8)||occupied(world,pos,h+8))continue;
            if(!existing){
                if(!clear(world,pos.down(),1))continue;
                for(int a=-1;a<=1;a++)for(int b=-1;b<=1;b++)
                    world.setBlockState(pos.add(a,-1,b),LunarHorizons.LAUNCH_PAD.getDefaultState());
            }
            return pos;
        }
        return null;
    }

    private void transfer(ServerWorld source,ServerPlayerEntity pilot) {
        boolean moon=source.getRegistryKey().equals(World.OVERWORLD);
        if(source.getServer()==null){phase(DESCENT);return;}
        ServerWorld target=source.getServer().getWorld(moon?LunarHorizons.MOON:World.OVERWORLD);
        if(target==null){message(pilot,"missing_world");phase(DESCENT);return;}
        BlockPos dest=findLanding(target,!moon&&home!=null?home:landing);
        if(dest==null){message(pilot,"no_landing");phase(DESCENT);return;}
        RocketEntity arrival=new RocketEntity(LunarHorizons.ROCKET_ENTITY,target);
        arrival.landing=dest;arrival.home=home;arrival.phase(DESCENT);
        double x=dest.getX()+.5,y=dest.getY()+descentHeight(target,dest),z=dest.getZ()+.5;
        arrival.refreshPositionAndAngles(x,y,z,getYaw(),0);
        if(!target.spawnEntity(arrival)){phase(DESCENT);message(pilot,"no_landing");return;}
        pilot.stopRiding();
        pilot.fallDistance=0;
        pilot.teleportTo(new TeleportTarget(target,new Vec3d(x,y+4.5,z),Vec3d.ZERO,pilot.getYaw(),0,TeleportTarget.NO_OP));
        if(pilot.getWorld()!=target){arrival.discard();phase(DESCENT);return;}
        pilot.startRiding(arrival,true);
        pilot.fallDistance=0;
        discard();
        message(pilot,"approach");
    }
}

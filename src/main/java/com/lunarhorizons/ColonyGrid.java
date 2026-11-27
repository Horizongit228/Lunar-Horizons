package com.lunarhorizons;
import java.util.*;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/** Server-only, bounded face-connected electrical networks. One transaction per network per second. */
final class ColonyGrid {
    private static final int LIMIT=1024;
    private static final Map<ServerWorld,Frame> FRAMES=new WeakHashMap<>();
    private static final class Frame {long tick=Long.MIN_VALUE;final Set<BlockPos> visited=new HashSet<>();}
    static boolean loaded(ServerWorld w,BlockPos p){return p.getY()>=w.getBottomY()&&p.getY()<w.getTopY()&&w.isChunkLoaded(p.getX()>>4,p.getZ()>>4);}
    private static boolean conductor(BlockState s){return ColonyConduitBlock.powerPort(s)&&(!s.isOf(LunarHorizons.POWER_SWITCH)||s.get(net.minecraft.state.property.Properties.POWERED));}
    static void tick(World world,ColonyBlockEntity root){
        if(!(world instanceof ServerWorld w)||w.getTime()%20!=0)return;
        Frame f=FRAMES.computeIfAbsent(w,k->new Frame());
        if(f.tick!=w.getTime()){f.tick=w.getTime();f.visited.clear();}
        if(f.visited.contains(root.getPos()))return;
        ArrayDeque<BlockPos> queue=new ArrayDeque<>();Set<BlockPos> nodes=new HashSet<>();List<ColonyBlockEntity> machines=new ArrayList<>();
        queue.add(root.getPos());nodes.add(root.getPos());int solar=0;boolean overflow=false;
        while(!queue.isEmpty()){
            BlockPos p=queue.remove();BlockState state=w.getBlockState(p);ColonyConduitBlock.refresh(w,p,state);SolarPanelBlock.refresh(w,p,state);ColonyMachineBlock.refresh(w,p,w.getBlockState(p));
            if(w.getBlockEntity(p) instanceof ColonyBlockEntity be)machines.add(be);
            if((state.isOf(LunarHorizons.SOLAR_PANEL)||state.isOf(LunarHorizons.DAMAGED_SOLAR_PANEL)||state.isOf(LunarHorizons.ADVANCED_SOLAR_PANEL))&&w.isSkyVisible(p.up())&&(w.getRegistryKey().equals(LunarHorizons.MOON)||w.isDay()))solar+=state.isOf(LunarHorizons.ADVANCED_SOLAR_PANEL)?90:state.isOf(LunarHorizons.SOLAR_PANEL)?30:8;
            for(Direction dir:Direction.values()){
                BlockPos q=p.offset(dir);
                if(!loaded(w,q)||nodes.contains(q)||!conductor(w.getBlockState(q)))continue;
                if(nodes.size()>=LIMIT){overflow=true;continue;}
                nodes.add(q);queue.add(q);
            }
        }
        boolean overlaps=nodes.stream().anyMatch(f.visited::contains);
        f.visited.addAll(nodes);
        if(overflow||overlaps){for(var be:machines){be.networkFault=true;be.production=be.demand=be.served=be.stored=be.networkCapacity=0;be.networkNodes=nodes.size();be.setActive(false);}return;}
        machines.sort(Comparator.comparingLong(be->be.getPos().asLong()));
        for(var be:machines)be.automaticTransfer(w);
        int supply=solar;
        int target=0;
        for(var be:machines){be.networkFault=false;if(be.wants())target+=be.cost();if(be.kind()==ColonyBlockEntity.Kind.BATTERY&&be.enabled)target+=Math.min(120,be.capacity()-be.energy);}
        for(var be:machines)if(be.kind()==ColonyBlockEntity.Kind.GENERATOR){
            boolean needed=be.enabled&&supply<target;
            if(needed&&be.reserve==0&&be.input>0){be.input--;be.reserve=60;}
            boolean on=needed&&be.reserve>0;if(on){supply+=120;be.reserve--;be.markDirty();}be.setActive(on);
        }
        int generated=supply,available=supply,demand=0;
        List<ColonyBlockEntity> batteries=new ArrayList<>();
        for(var be:machines)if(be.kind()==ColonyBlockEntity.Kind.BATTERY){if(be.enabled){batteries.add(be);available+=be.energy;}else be.setActive(false);}
        // Oxygen and illumination take priority over processing.
        if(!machines.isEmpty())Collections.rotate(machines,(int)Math.floorMod(w.getTime()/20,machines.size()));
        machines.sort(Comparator.comparingInt(be->be.kind()==ColonyBlockEntity.Kind.OXYGEN?0:be.kind()==ColonyBlockEntity.Kind.LAMP?1:2));
        int spent=0;
        for(var be:machines){
            if(be.kind()==ColonyBlockEntity.Kind.GENERATOR||be.kind()==ColonyBlockEntity.Kind.BATTERY||be.kind()==ColonyBlockEntity.Kind.TERMINAL)continue;
            boolean wants=be.wants();int cost=be.cost();if(wants)demand+=cost;
            boolean on=wants&&available>=cost;
            if(on){available-=cost;spent+=cost;be.operate();}
            be.setActive(on);
        }
        int balance=generated-spent;
        for(var b:batteries){
            if(balance>=0){int add=Math.min(balance,b.capacity()-b.energy);b.energy+=add;balance-=add;}
            else {int take=Math.min(-balance,b.energy);b.energy-=take;balance+=take;}
            b.setActive(b.energy>0);b.markDirty();
        }
        int stored=batteries.stream().mapToInt(b->b.energy).sum();
        int capacity=batteries.stream().mapToInt(ColonyBlockEntity::capacity).sum();
        for(var be:machines){be.production=generated;be.demand=demand;be.served=spent;be.stored=stored;be.networkCapacity=capacity;be.networkNodes=nodes.size();if(be.kind()==ColonyBlockEntity.Kind.TERMINAL)be.setActive(be.enabled&&(generated>0||stored>0));}
    }
}

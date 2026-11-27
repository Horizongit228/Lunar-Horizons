package com.lunarhorizons;

import com.mojang.serialization.Codec;
import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/** One fixed orbital station. Each chunk independently evaluates the same blueprint,
 * including a one-block halo for conduit state calculation. No neighboring chunk writes. */
public final class OrbitalStationFeature extends Feature<DefaultFeatureConfig> {
    public static final int DECK=112;
    public static final int[] DOCK_X={0,-48,48};
    public static final int DOCK_Z=-46;
    public OrbitalStationFeature(Codec<DefaultFeatureConfig> codec){super(codec);}
    public static BlockPos dock(int i){return new BlockPos(DOCK_X[i],DECK+1,DOCK_Z);}
    private static final class Blueprint{
        final int cx,cz;
        final Map<BlockPos,BlockState> cells=new LinkedHashMap<>();
        final Map<BlockPos,Integer> supplies=new HashMap<>();
        Blueprint(int x,int z){cx=x;cz=z;}
        boolean nearby(int x,int z){return x>=cx-1&&x<=cx+16&&z>=cz-1&&z<=cz+16;}
        boolean local(BlockPos p){return p.getX()>=cx&&p.getX()<cx+16&&p.getZ()>=cz&&p.getZ()<cz+16;}
        void put(int x,int y,int z,Block b){put(x,y,z,b.getDefaultState());}
        void put(int x,int y,int z,BlockState state){if(nearby(x,z))cells.put(new BlockPos(x,DECK+y,z),state);}
        void fill(int x1,int y1,int z1,int x2,int y2,int z2,Block b){
            for(int x=Math.max(x1,cx-1);x<=Math.min(x2,cx+16);x++)for(int z=Math.max(z1,cz-1);z<=Math.min(z2,cz+16);z++)
                for(int y=y1;y<=y2;y++)put(x,y,z,b);
        }
        BlockState state(BlockPos p){return cells.getOrDefault(p,Blocks.AIR.getDefaultState());}
        void room(int x1,int z1,int x2,int z2,int low,int high){
            for(int x=Math.max(x1,cx-1);x<=Math.min(x2,cx+16);x++)for(int z=Math.max(z1,cz-1);z<=Math.min(z2,cz+16);z++)
                for(int y=low;y<=high;y++){
                    boolean edge=x==x1||x==x2||z==z1||z==z2;
                    put(x,y,z,y==low?LunarHorizons.HABITAT_FLOOR:y==high||edge?LunarHorizons.REINFORCED_HULL:Blocks.AIR);
                }
        }
        void door(int x,int y,int z,Direction dir){
            BlockState s=LunarHorizons.AIRLOCK.getDefaultState().with(Properties.HORIZONTAL_FACING,dir);
            put(x,y,z,s.with(Properties.DOUBLE_BLOCK_HALF,DoubleBlockHalf.LOWER));
            put(x,y+1,z,s.with(Properties.DOUBLE_BLOCK_HALF,DoubleBlockHalf.UPPER));
        }
        void chest(int x,int y,int z,int kind){
            put(x,y,z,Blocks.CHEST.getDefaultState().with(Properties.HORIZONTAL_FACING,Direction.SOUTH));
            if(nearby(x,z))supplies.put(new BlockPos(x,DECK+y,z),kind);
        }
        void bed(int x,int z){
            BlockState s=Blocks.GRAY_BED.getDefaultState().with(Properties.HORIZONTAL_FACING,Direction.SOUTH);
            put(x,1,z,s.with(net.minecraft.block.BedBlock.PART,BedPart.FOOT));
            put(x,1,z+1,s.with(net.minecraft.block.BedBlock.PART,BedPart.HEAD));
        }
        void power(int x,int z,int roof){
            put(x,1,z,LunarHorizons.OXYGEN_GENERATOR);
            put(x+1,1,z,LunarHorizons.INDUSTRIAL_BATTERY);
            put(x+2,1,z,LunarHorizons.ELECTRIC_REFILL);
            for(int y=2;y<roof;y++)put(x+1,y,z,LunarHorizons.POWER_CABLE);
            put(x+1,roof,z,LunarHorizons.POWER_FEEDTHROUGH);
            for(int a=x;a<=x+2;a++)for(int b=z-1;b<=z+1;b++)put(a,roof+1,b,LunarHorizons.ADVANCED_SOLAR_PANEL);
        }
        void layout(){
            Block metal=LunarHorizons.TITANIUM_BLOCK,hull=LunarHorizons.REINFORCED_HULL;
            // Pressurized main transit spine, divided into small independent compartments.
            room(-62,-4,62,4,0,5);
            for(int x=-60;x<=60;x+=8)put(x,4,0,LunarHorizons.LUNAR_LANTERN);
            for(int x:new int[]{-32,-16,16,32}){
                fill(x,1,-3,x,4,3,hull);door(x,1,0,Direction.WEST);
            }
            for(int x:new int[]{-48,-24,0,24,48})power(x-3,2,5);
            // Six large modules (21 x 19) and two observation lounges. Every connection has its own hatch.
            for(int x:DOCK_X)for(int side:new int[]{-1,1}){
                int z=side*23;
                int a=Math.min(side*4,z-side*9),b=Math.max(side*4,z-side*9);
                room(x-3,a,x+3,b,0,5);
                room(x-10,z-9,x+10,z+9,0,6);
                door(x,1,side*4,Direction.NORTH);
                door(x,1,z-side*9,Direction.NORTH);
                put(x,4,side*9,LunarHorizons.LUNAR_LANTERN);
                for(int u=x-8;u<=x+8;u+=4){
                    put(u,3,z-9,LunarHorizons.PRESSURE_GLASS);put(u,3,z+9,LunarHorizons.PRESSURE_GLASS);
                    put(u,5,z,LunarHorizons.LUNAR_LANTERN);
                }
                power(x-8,z+5,6);
                int type=(x+48)/48+(side>0?3:0);
                if(type==0||type==3){
                    for(int u=x-5;u<=x+5;u+=3){bed(u,z-5);bed(u,z+1);}
                }else if(type==1){
                    put(x+6,1,z+5,LunarHorizons.POWER_TERMINAL);
                    put(x+7,1,z+5,LunarHorizons.ADVANCED_ASSEMBLER);
                    put(x+8,1,z+5,LunarHorizons.ELECTRIC_REFINERY);
                }else if(type==2){
                    for(int u=x-4;u<=x+8;u+=3)put(u,1,z-5,LunarHorizons.INDUSTRIAL_BATTERY);
                }else if(type==4){
                    put(x+6,1,z+5,LunarHorizons.FUEL_SYNTHESIZER);
                    put(x+7,1,z+5,LunarHorizons.HELIUM_GENERATOR);
                    put(x+8,1,z+5,LunarHorizons.POWER_TERMINAL);
                }else {
                    for(int u=x-5;u<=x+7;u+=4){
                        put(u,1,z-5,LunarHorizons.INDUSTRIAL_GRATE);
                        put(u,2,z-5,LunarHorizons.CRYSTAL_STONE);
                    }
                }
                chest(x+8,1,z-7,type);
            }
            // Docking platforms and unpressurized catwalks. The rocket's ascent corridor is unobstructed.
            for(int x:DOCK_X){
                door(x,1,-32,Direction.NORTH);
                fill(x-2,0,-41,x+2,0,-33,LunarHorizons.INDUSTRIAL_GRATE);
                fill(x-6,0,-52,x+6,0,-40,metal);
                fill(x-4,0,-50,x+4,0,-42,LunarHorizons.LAUNCH_PAD);
                for(int u:new int[]{x-6,x+6})for(int z:new int[]{-51,-41})put(u,1,z,LunarHorizons.LUNAR_LANTERN);
                for(int z=-39;z<=-34;z++){put(x-2,1,z,Blocks.IRON_BARS);put(x+2,1,z,Blocks.IRON_BARS);}
                put(x+4,1,-40,LunarHorizons.SURVEY_BEACON);
            }
            // Massive photovoltaic wings and truss. Panels are attached to a load-bearing lattice.
            for(int side:new int[]{-1,1}){
                int lo=side<0?-76:63,hi=side<0?-63:76;
                fill(lo,-1,-1,hi,-1,1,metal);
                fill(lo,0,-1,hi,0,1,LunarHorizons.INDUSTRIAL_GRATE);
                int x=side*70;
                fill(x-1,-1,-45,x+1,-1,43,metal);fill(lo,0,2,hi,0,2,LunarHorizons.INDUSTRIAL_GRATE);
                for(int z:new int[]{-33,-15,15,33}){
                    fill(x-6,-1,z-7,x+6,-1,z+7,LunarHorizons.INDUSTRIAL_GRATE);
                    fill(x-6,0,z-7,x+6,0,z+7,LunarHorizons.ADVANCED_SOLAR_PANEL);
                }
                // Independent technical lounge at each end of the spine.
                // Supply the nearest spine power network through a sealed wall port.
                for(int u=Math.min(side*62,side<0?-48:48);u<=Math.max(side*62,side<0?-48:48);u++)put(u,1,2,u==side*62?LunarHorizons.POWER_FEEDTHROUGH:LunarHorizons.POWER_CABLE);
                for(int u=Math.min(side*63,x);u<=Math.max(side*63,x);u++)put(u,1,2,LunarHorizons.POWER_CABLE);
                put(x,0,2,LunarHorizons.POWER_CABLE);
                for(int z=-40;z<=40;z++)if(z>-26&&z<-22||z>-8&&z<8||z>22&&z<26)put(x,0,z,LunarHorizons.POWER_CABLE);
                int center=side*57;
                for(int z:new int[]{-4,4})for(int u=center-3;u<=center+3;u++)put(u,2,z,LunarHorizons.PRESSURE_GLASS);
            }
            // Central communications mast and lower structural keel.
            fill(-2,6,-2,2,9,2,metal);
            fill(0,10,0,0,24,0,Blocks.IRON_BARS);
            for(int x=-7;x<=7;x++)put(x,19,0,metal);
            put(0,25,0,LunarHorizons.SURVEY_BEACON);
            for(int x=-60;x<=60;x++){put(x,-2,0,metal);if(x%8==0)fill(x,-2,-3,x,-1,3,metal);}
        }
    }
    @Override public boolean generate(FeatureContext<DefaultFeatureConfig> ctx){
        if(!ctx.getWorld().toServerWorld().getRegistryKey().equals(LunarHorizons.SPACE))return false;
        int cx=ctx.getOrigin().getX()&~15,cz=ctx.getOrigin().getZ()&~15;
        if(cx>80||cx+15< -80||cz>53||cz+15< -53)return false;
        Blueprint p=new Blueprint(cx,cz);p.layout();
        StructureWorldAccess w=ctx.getWorld();
        var sides=new net.minecraft.state.property.BooleanProperty[]{Properties.DOWN,Properties.UP,Properties.NORTH,Properties.SOUTH,Properties.WEST,Properties.EAST};
        for(var e:p.cells.entrySet()){
            BlockPos pos=e.getKey();if(!p.local(pos))continue;
            BlockState s=e.getValue();
            if(s.getBlock() instanceof ColonyConduitBlock line)for(Direction d:Direction.values())s=s.with(sides[d.getId()],line.connects(p.state(pos.offset(d))));
            else if(s.getBlock() instanceof SolarPanelBlock)s=s.with(Properties.UP,ColonyConduitBlock.powerPort(p.state(pos.up())));
            else if(s.getBlock() instanceof ColonyMachineBlock m)s=s.with(Properties.UP,m.topConnection(p.state(pos.up())));
            w.setBlockState(pos,s,2);
        }
        // Seed initial oxygen reserves and batteries, so stations are usable but still consume resources.
        for(var e:p.cells.entrySet())if(p.local(e.getKey())&&e.getValue().isOf(LunarHorizons.OXYGEN_GENERATOR)&&w.getBlockEntity(e.getKey()) instanceof ColonyBlockEntity be){be.input=32;be.markDirty();}
        for(var e:p.cells.entrySet())if(p.local(e.getKey())&&e.getValue().isOf(LunarHorizons.INDUSTRIAL_BATTERY)&&w.getBlockEntity(e.getKey()) instanceof ColonyBlockEntity be){be.energy=be.capacity()/2;be.markDirty();}
        for(var e:p.supplies.entrySet())if(p.local(e.getKey())&&w.getBlockEntity(e.getKey()) instanceof ChestBlockEntity chest){
            int t=e.getValue();chest.setStack(0,new ItemStack(LunarHorizons.FUEL,8));
            chest.setStack(3,new ItemStack(LunarHorizons.LARGE_OXYGEN));chest.setStack(6,new ItemStack(LunarHorizons.LUNAR_ICE,16));
            chest.setStack(9,new ItemStack(LunarHorizons.POWER_CELL,2));
            chest.setStack(12,new ItemStack(Items.BREAD,8));
            if(t==0||t==3)chest.setStack(15,new ItemStack(LunarHorizons.BACKPACK));
            chest.markDirty();
        }
        return !p.cells.isEmpty();
    }
}

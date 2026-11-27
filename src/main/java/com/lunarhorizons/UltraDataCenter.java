package com.lunarhorizons;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/** Eight independent compute halls, each with a power plant and life support.
 * All coordinates are local to the persistent, chunk-split site blueprint. */
final class UltraDataCenter {
    private static final Block HULL=LunarHorizons.REINFORCED_HULL, FLOOR=LunarHorizons.HABITAT_FLOOR,
        PIPE=LunarHorizons.OXYGEN_PIPE, CABLE=LunarHorizons.POWER_CABLE, WIRE=LunarHorizons.POWER_WIRE,
        PORT=LunarHorizons.POWER_FEEDTHROUGH, VENT=LunarHorizons.OXYGEN_VENT, LIGHT=LunarHorizons.LUNAR_LANTERN;
    private static void room(ExplorationSiteFeature.Plan p,int x1,int z1,int x2,int z2,int roof){
        p.room(Math.min(x1,x2),z1,Math.max(x1,x2),z2,0,roof,HULL,FLOOR);
    }
    static void layout(ExplorationSiteFeature.Plan p,Random r){
        // 117 x 119 footprint, plus an exterior entrance ladder. No open launch columns.
        room(p,-5,-49,5,49,5);
        for(int z:new int[]{-24,0,24}){
            p.fill(-4,1,z,4,4,z,HULL);p.door(0,1,z,Direction.NORTH);
        }
        for(int z=-46;z<=46;z+=4){
            p.put(-3,4,z,LIGHT);p.put(3,4,z,LIGHT);
            p.put(-3,0,z,LunarHorizons.INDUSTRIAL_GRATE);p.put(3,0,z,LunarHorizons.INDUSTRIAL_GRATE);
        }
        for(int z:new int[]{-36,-12,12,36})for(int side:new int[]{-1,1})hall(p,side,z);

        // Entry security and mission control; all doors lead to a usable passage.
        room(p,-12,-59,12,-49,6);
        p.door(0,1,-59);p.door(0,1,-49);
        for(int x:new int[]{-9,-6,-3,3,6,9}){
            p.put(x,1,-55,LunarHorizons.POWER_TERMINAL);
            p.put(x,2,-56,LunarHorizons.PRESSURE_GLASS);
            p.put(x,5,-54,LIGHT);
        }
        p.fill(-9,1,-54,9,1,-54,WIRE);
        p.put(-9,1,-53,LunarHorizons.INDUSTRIAL_BATTERY);
        p.put(-8,1,-53,LunarHorizons.OXYGEN_GENERATOR);
        for(int y=2;y<6;y++)p.put(-9,y,-53,CABLE);
        p.put(-9,6,-53,PORT);
        p.fill(-10,7,-54,-8,7,-52,LunarHorizons.ADVANCED_SOLAR_PANEL);
        p.chest(-10,1,-57,22,Direction.SOUTH);p.chest(10,1,-57,22,Direction.SOUTH);
        p.put(-4,1,-58,Blocks.BARREL);p.put(4,1,-58,Blocks.CRAFTING_TABLE);
        for(int x=-10;x<=10;x+=4)if(x!=0)p.put(x,3,-59,LunarHorizons.PRESSURE_GLASS);
        p.fill(-2,7,-56,2,7,-52,LunarHorizons.TITANIUM_BLOCK);
        p.fill(0,8,-54,0,14,-54,Blocks.IRON_BARS);p.put(0,15,-54,LunarHorizons.SURVEY_BEACON);

        // Classified archive and a separate upper observation/control gallery.
        room(p,-12,49,12,59,10);p.door(0,1,49);
        p.fill(-11,5,50,11,5,58,FLOOR);
        p.fill(-10,1,54,10,4,54,HULL);p.door(0,1,54);
        for(int x:new int[]{-9,-6,6,9}){
            p.chest(x,1,57,23,Direction.NORTH);p.put(x,4,56,LIGHT);
            p.put(x,1,51,Blocks.BARREL);p.put(x,9,53,LIGHT);
            p.put(x,6,57,LunarHorizons.POWER_TERMINAL);
        }
        for(int y=1;y<=6;y++){
            p.put(10,y,51,HULL);
            p.put(9,y,51,Blocks.AIR);
        }
        // Ladder facing is rotated with the building, not the underlying terrain.
        Direction facing=Direction.WEST;
        for(int i=0;i<p.rot;i++)facing=facing.rotateYClockwise();
        for(int y=1;y<=6;y++)p.put(9,y,51,Blocks.LADDER.getDefaultState().with(Properties.HORIZONTAL_FACING,facing));
        p.put(9,7,51,Blocks.AIR);
        p.put(0,1,57,LunarHorizons.ADVANCED_ASSEMBLER);
        p.put(-1,1,57,LunarHorizons.SERVER_BLOCK);p.put(1,1,57,LunarHorizons.INDUSTRIAL_BATTERY);
        p.put(1,1,56,LunarHorizons.HELIUM_GENERATOR);
        p.put(1,2,57,CABLE);p.put(1,3,57,CABLE);p.put(1,4,57,CABLE);p.put(1,5,57,PORT);
        for(int x=-9;x<=9;x++)p.put(x,6,56,WIRE);
        p.put(1,6,56,WIRE);
        for(int y=6;y<10;y++)p.put(1,y,57,CABLE);
        p.put(1,10,57,PORT);p.fill(-2,11,55,3,11,58,LunarHorizons.ADVANCED_SOLAR_PANEL);
        for(int x=-9;x<=9;x+=3)p.put(x,7,59,LunarHorizons.PRESSURE_GLASS);
        p.put(0,11,54,LunarHorizons.SURVEY_BEACON);
    }
    private static void hall(ExplorationSiteFeature.Plan p,int s,int z){
        room(p,s*5,z-3,s*12,z+3,5);
        room(p,s*12,z-10,s*38,z+10,5);
        room(p,s*38,z-10,s*58,z+10,5);
        p.door(s*5,1,z,Direction.EAST);p.door(s*12,1,z,Direction.EAST);
        p.door(s*38,1,z,Direction.EAST);
        p.put(s*8,4,z,LIGHT);
        // Three rows, four two-high racks per row: 24 functional servers per hall.
        for(int u:new int[]{16,24,32}){
            int x=s*u;
            for(int dz:new int[]{-6,-2,2,6}){
                p.machine(x,1,z+dz,LunarHorizons.SERVER_BLOCK,s<0?Direction.WEST:Direction.EAST);
                p.machine(x,2,z+dz,LunarHorizons.SERVER_BLOCK,s<0?Direction.WEST:Direction.EAST);
            }
            for(int dz=-6;dz<=7;dz++)p.put(x,3,z+dz,CABLE);
            for(int dz=-8;dz<=8;dz+=4)p.put(s*(u+3),4,z+dz,LIGHT);
            for(int dz=-8;dz<=8;dz++)p.put(s*(u+2),0,z+dz,LunarHorizons.INDUSTRIAL_GRATE);
        }
        // Dedicated electrical trunk, with a sealed wall feedthrough.
        for(int u=16;u<=42;u++)p.put(s*u,3,z+7,u==38?PORT:CABLE);
        p.machine(s*42,1,z+7,LunarHorizons.INDUSTRIAL_BATTERY,Direction.NORTH);
        p.machine(s*43,1,z+7,LunarHorizons.HELIUM_GENERATOR,Direction.NORTH);
        p.machine(s*44,1,z+7,LunarHorizons.POWER_TERMINAL,Direction.NORTH);
        p.machine(s*41,1,z+7,LunarHorizons.OXYGEN_GENERATOR,Direction.NORTH);
        p.machine(s*40,1,z+7,LunarHorizons.ELECTRIC_REFILL,Direction.NORTH);
        for(int y=2;y<=4;y++)p.put(s*42,y,z+7,CABLE);
        p.put(s*42,5,z+7,PORT);
        for(int u=40;u<=45;u++)for(int dz=5;dz<=8;dz++)p.put(s*u,6,z+dz,LunarHorizons.ADVANCED_SOLAR_PANEL);
        // Continuous oxygen header uses a solid vent through the partition.
        for(int y=2;y<=4;y++)p.put(s*41,y,z+7,PIPE);
        for(int dz=-7;dz<=7;dz++)p.put(s*41,4,z+dz,PIPE);
        for(int u=16;u<=41;u++)p.put(s*u,4,z-7,(u==16||u==24||u==32||u==38)?VENT:PIPE);
        // Equipment repair bay, coolant reserves (ice), emergency supply lockers.
        for(int u:new int[]{46,50,54}){
            p.put(s*u,1,z-6,Blocks.CRAFTING_TABLE);p.put(s*u,1,z-8,Blocks.BARREL);
            p.put(s*u,4,z-4,LIGHT);p.put(s*u,4,z+4,LIGHT);
        }
        p.chest(s*55,1,z+7,22,Direction.NORTH);
        p.chest(s*55,1,z-7,22,Direction.SOUTH);
        p.fill(Math.min(s*48,s*52),1,z+2,Math.max(s*48,s*52),2,z+3,LunarHorizons.LUNAR_ICE);
        for(int dz=-8;dz<=8;dz+=4)p.put(s*58,3,z+dz,LunarHorizons.PRESSURE_GLASS);
        for(int u=14;u<=56;u+=6)for(int dz:new int[]{-10,10})p.put(s*u,6,z+dz,LunarHorizons.TITANIUM_BLOCK);
        for(int u:new int[]{14,36,56})for(int dz:new int[]{-8,8})p.put(s*u,6,z+dz,LIGHT);
    }
}

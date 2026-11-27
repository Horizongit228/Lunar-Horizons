package com.lunarhorizons;

import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.state.property.Properties;

/** Deterministic final blueprint pass. Never reads or writes live neighboring chunks. */
final class SiteFinisher {
    private SiteFinisher(){}
    private static final Block CABLE=LunarHorizons.POWER_CABLE, WIRE=LunarHorizons.POWER_WIRE,
        PIPE=LunarHorizons.OXYGEN_PIPE, LIGHT=LunarHorizons.LUNAR_LANTERN;
    private static boolean empty(ExplorationSiteFeature.Plan p,BlockPos q){
        BlockState s=p.blocks.get(q);return s!=null&&s.isAir();
    }
    private static void fit(ExplorationSiteFeature.Plan p,int x,int y,int z,Block b){
        if(empty(p,p.at(x,y,z)))p.put(x,y,z,b);
    }
    private static void shelf(ExplorationSiteFeature.Plan p,int x,int y,int z){
        fit(p,x,y,z,Blocks.BARREL);fit(p,x,y+1,z,LunarHorizons.INDUSTRIAL_GRATE);
    }
    private static void lamp(ExplorationSiteFeature.Plan p,int x,int y,int z){fit(p,x,y,z,LIGHT);}
    private static void equip(ExplorationSiteFeature.Plan p,int x,int y,int z,Block b){
        if(empty(p,p.at(x,y,z)))p.machine(x,y,z,b,Direction.SOUTH);
    }
    static void finish(ExplorationSiteFeature.Plan p,int kind){
        // Furnishing stays inside explicitly planned air, preserving existing loot and routes.
        switch(kind){
            case 0 -> { // Outpost: crew, lab, utility and rear workbench
                shelf(p,-10,1,0);shelf(p,-10,1,2);fit(p,5,1,-3,Blocks.CRAFTING_TABLE);
                equip(p,5,1,3,LunarHorizons.ELECTRIC_REFILL);
                shelf(p,-2,1,8);fit(p,-2,1,6,Blocks.SMOOTH_STONE_SLAB);lamp(p,0,3,2);
            }
            case 1 -> { // Crashed satellite: instrument body and antenna braces
                fit(p,0,2,0,LunarHorizons.POWER_TERMINAL);
                // The original broken solar wings and impact scar are intentional.
                for(int s:new int[]{-1,1})fit(p,s,1,0,LunarHorizons.BATTERY);
            }
            case 2 -> { // Two-floor research complex
                for(int y:new int[]{1,6})for(int z:new int[]{-8,-2,4,9}){
                    lamp(p,-5,y+3,z);lamp(p,5,y+3,z);
                    shelf(p,-13,y,z);shelf(p,13,y,z);
                }
                equip(p,8,6,-5,LunarHorizons.OXYGEN_GENERATOR);
                equip(p,9,6,-5,LunarHorizons.BATTERY);
                fit(p,-10,6,1,Blocks.CRAFTING_TABLE);fit(p,10,1,-6,Blocks.BARREL);
            }
            case 3 -> { // Natural geode: additional attached, not floating crystal points.
                List<BlockPos> candidates=new ArrayList<>(p.blocks.keySet());int count=0;
                for(BlockPos q:candidates){
                    if(count>=12||!empty(p,q)||Math.floorMod(q.asLong(),31)!=0)continue;
                    BlockState below=p.blocks.get(q.down());
                    if(below!=null&&below.isOf(LunarHorizons.CRYSTAL_STONE)){
                        p.blocks.put(q,LunarHorizons.CRYSTAL_CLUSTER.getDefaultState());count++;
                    }
                }
            }
            case 4 -> {fit(p,-1,4,-1,Blocks.SMOOTH_STONE_SLAB);fit(p,1,4,-1,Blocks.LEVER);lamp(p,0,6,-1);}
            case 5 -> {shelf(p,-3,1,0);fit(p,-2,1,2,Blocks.CRAFTING_TABLE);equip(p,3,1,-2,LunarHorizons.OXYGEN_GENERATOR);lamp(p,0,3,1);}
            case 6 -> { // Observatory control benches and communications racks
                for(int x=-12;x<=12;x+=6){fit(p,x,1,-11,Blocks.SMOOTH_STONE);fit(p,x,2,-11,LunarHorizons.POWER_TERMINAL);lamp(p,x,4,-10);}
                shelf(p,13,1,-13);equip(p,10,1,-6,LunarHorizons.OXYGEN_GENERATOR);
                fit(p,8,1,0,Blocks.CRAFTING_TABLE);lamp(p,7,4,-4);
            }
            case 7 -> {shelf(p,-4,1,-17);fit(p,-2,1,-18,Blocks.CRAFTING_TABLE);equip(p,3,1,-16,LunarHorizons.HELIUM_GENERATOR);lamp(p,4,3,-17);}
            case 8 -> { // Repair bays, storage and oxygen service point
                for(int x:new int[]{-9,9})for(int z:new int[]{-7,-3,2,7}){shelf(p,x,1,z);lamp(p,x,5,z);}
                equip(p,-8,1,8,LunarHorizons.ELECTRIC_REFILL);equip(p,-9,1,8,LunarHorizons.OXYGEN_GENERATOR);
                fit(p,7,1,8,Blocks.CRAFTING_TABLE);
            }
            case 9 -> { // Fuel production and instrument benches
                for(int x=-12;x<=12;x+=6){shelf(p,x,1,-12);lamp(p,x,5,-10);}
                equip(p,-8,1,-8,LunarHorizons.POWER_TERMINAL);equip(p,9,1,-8,LunarHorizons.OXYGEN_GENERATOR);
                fit(p,4,1,-8,Blocks.CRAFTING_TABLE);
            }
            case 10 -> { // Mine control and storage chambers
                equip(p,9,1,-5,LunarHorizons.BATTERY);equip(p,10,1,-5,LunarHorizons.HELIUM_GENERATOR);
                equip(p,11,1,-5,LunarHorizons.OXYGEN_GENERATOR);
                for(int x:new int[]{-12,-9,9,12}){shelf(p,x,1,13);lamp(p,x,3,10);}
                for(int z:new int[]{-10,-6,0,4})lamp(p,0,4,z);
                fit(p,-10,1,-4,Blocks.CRAFTING_TABLE);lamp(p,0,3,13);
            }
            case 11 -> { // Ice processing and tank service
                for(int x:new int[]{-11,-5,5,11}){shelf(p,x,1,-9);lamp(p,x,4,-7);equip(p,x,1,-4,LunarHorizons.ELECTRIC_REFILL);}
                fit(p,0,1,-9,Blocks.CRAFTING_TABLE);lamp(p,0,4,8);
            }
            case 12 -> { // Emergency bunks, food lockers and separate life support
                for(int x:new int[]{-8,-4,4,8}){shelf(p,x,1,3);lamp(p,x,4,7);}
                equip(p,6,1,-4,LunarHorizons.ELECTRIC_REFILL);fit(p,-6,1,-6,Blocks.CRAFTING_TABLE);
                for(int z:new int[]{-8,-2,4,10})lamp(p,0,4,z);
            }
            case 13 -> { // Data center already has full headers and rack wiring
                for(int z:new int[]{-11,-7,0,8,12})lamp(p,0,4,z);
                shelf(p,-14,1,12);shelf(p,14,1,12);
            }
            case 14 -> { // Alien ruin: deliberate ancient chambers, not modern pipes
                for(int x:new int[]{-13,-7,7,13})for(int z:new int[]{-10,10})
                    fit(p,x,6,z,LunarHorizons.CRYSTAL_STONE);
                for(int x:new int[]{-5,5})for(int z:new int[]{-11,-5,9,13})fit(p,x,1,z,Blocks.CHISELED_DEEPSLATE);
                for(int z:new int[]{-12,-6,12})fit(p,0,7,z,LunarHorizons.CRYSTAL_STONE);
            }
            case 15 -> { // Six colony wings get local oxygen sources
                for(int side:new int[]{-1,1})for(int z:new int[]{-28,0,28}){
                    int x=side*33;
                    equip(p,x-2,1,z+9,LunarHorizons.OXYGEN_GENERATOR);
                    equip(p,x-1,1,z+9,LunarHorizons.BATTERY);
                    lamp(p,x,5,z-8);
                }
                for(int z:new int[]{-33,-14,0,14,33})equip(p,-9,1,z,LunarHorizons.OXYGEN_GENERATOR);
            }
            case 16 -> { // Hangars: additional wall-side storage, clear center bay
                for(int s:new int[]{-1,1}){
                    int x=s*20;
                    for(int z:new int[]{-30,-26,-22,-18}){shelf(p,x-12,1,z);lamp(p,x+12,14,z);}
                }
                equip(p,-3,1,-28,LunarHorizons.OXYGEN_GENERATOR);equip(p,-2,1,-28,LunarHorizons.BATTERY);
                shelf(p,3,1,-21);lamp(p,0,4,-23);
            }
            case 17 -> { // Reactor cooling headers: real source and endpoint
                for(int s:new int[]{-1,1}){
                    int x=s*19;
                    equip(p,x,1,-12,LunarHorizons.OXYGEN_GENERATOR);
                    fit(p,x,2,-12,PIPE);fit(p,x,3,-12,PIPE);
                    // The last pipe may be in a storage partition: only replace a planned pipe.
                    BlockPos last=p.at(x,4,24);BlockState state=p.blocks.get(last);
                    if(state!=null&&state.isOf(PIPE))p.put(x,4,24,LunarHorizons.OXYGEN_VENT);
                    for(int z:new int[]{-8,0,8,16}){shelf(p,s*26,1,z);lamp(p,s*26,5,z);}
                }
                for(int x=-20;x<=20;x+=8){fit(p,x,1,-24,Blocks.SMOOTH_STONE_SLAB);lamp(p,x,5,-23);}
            }
            case 18 -> { // Alien ship: additional instrument pedestals, storage and rib lighting
                for(int x:new int[]{-34,-21,-7,7,21,34})for(int z:new int[]{-5,5}){
                    fit(p,x,2,z,Blocks.CHISELED_DEEPSLATE);fit(p,x,3,z,LunarHorizons.CRYSTAL_STONE);
                    fit(p,x,9,z,Blocks.CHISELED_DEEPSLATE);fit(p,x,10,z,LunarHorizons.CRYSTAL_STONE);
                }
            }
        }
        route(p,true);
        route(p,false);
    }
    private static boolean port(BlockState s,boolean oxygen){
        if(s==null)return false;
        return oxygen?s.isOf(PIPE)||s.isOf(LunarHorizons.OXYGEN_VENT)||s.isOf(LunarHorizons.OXYGEN_GENERATOR):ColonyConduitBlock.powerPort(s);
    }
    private static boolean source(BlockState s,boolean oxygen){
        if(oxygen)return s.isOf(LunarHorizons.OXYGEN_GENERATOR);
        return s.isOf(LunarHorizons.HELIUM_GENERATOR)||s.getBlock() instanceof SolarPanelBlock;
    }
    private static boolean clearRouteCell(ExplorationSiteFeature.Plan p,BlockPos q,boolean oxygen){
        if(!empty(p,q)||q.getY()<p.origin.getY()+1)return false;
        // Never obstruct ladder shafts, bed access, airlock thresholds, or a launch column.
        for(Direction d:Direction.values()){
            BlockState s=p.blocks.get(q.offset(d));
            if(s==null)continue;
            if(s.isOf(LunarHorizons.AIRLOCK)||s.isOf(Blocks.LADDER)||s.isOf(LunarHorizons.LAUNCH_PAD))return false;
        }
        if(p.kind==16)return false; // Preserve hangar and launch clearance; explicitly laid lines remain.
        return true;
    }
    private static void route(ExplorationSiteFeature.Plan p,boolean oxygen){
        // Multi-source shortest paths through planned air only; no terrain excavation.
        // A maximum of 40 blocks and 60k visited cells bounds planning cost.
        Map<BlockPos,BlockPos> parent=new HashMap<>();
        Map<BlockPos,Integer> distance=new HashMap<>();
        ArrayDeque<BlockPos> queue=new ArrayDeque<>();
        List<BlockPos> targets=new ArrayList<>();
        for(var e:p.blocks.entrySet()){
            BlockPos q=e.getKey();BlockState s=e.getValue();
            if(source(s,oxygen)){parent.put(q,q);distance.put(q,0);queue.add(q);}
            else if(port(s,oxygen))targets.add(q);
        }
        // Battery-only wrecks may still have a valid local circuit.
        if(queue.isEmpty()&&!oxygen)for(BlockPos q:targets){BlockState s=p.blocks.get(q);if(s.isOf(LunarHorizons.BATTERY)||s.isOf(LunarHorizons.INDUSTRIAL_BATTERY)){parent.put(q,q);distance.put(q,0);queue.add(q);}}
        while(!queue.isEmpty()&&parent.size()<60000){
            BlockPos q=queue.remove();int n=distance.get(q);if(n>=40)continue;
            for(Direction d:Direction.values()){
                BlockPos next=q.offset(d);if(parent.containsKey(next))continue;
                BlockState s=p.blocks.get(next);
                if(!port(s,oxygen)&&!clearRouteCell(p,next,oxygen))continue;
                // Do not route through a different utility's block or a sealed hull.
                parent.put(next,q);distance.put(next,n+1);queue.add(next);
            }
        }
        for(BlockPos target:targets){
            if(!parent.containsKey(target))continue;
            BlockPos q=target;
            for(int i=0;i<40;i++){
                BlockPos prev=parent.get(q);if(prev==null||prev.equals(q))break;
                if(empty(p,q))p.blocks.put(q,(oxygen?PIPE:q.getY()==p.origin.getY()+1?WIRE:CABLE).getDefaultState());
                q=prev;
            }
        }
        // Remove isolated leftover conduit fragments rather than generate floating cubes.
        for(BlockPos q:targets){
            BlockState s=p.blocks.get(q);
            if(s==null||!(s.getBlock() instanceof ColonyConduitBlock)||!port(s,oxygen))continue;
            boolean linked=false;
            for(Direction d:Direction.values())if(port(p.blocks.get(q.offset(d)),oxygen)){linked=true;break;}
            if(!linked)p.blocks.put(q,Blocks.AIR.getDefaultState());
        }
    }
}

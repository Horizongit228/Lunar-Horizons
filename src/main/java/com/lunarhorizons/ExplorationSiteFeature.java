package com.lunarhorizons;

import com.mojang.serialization.Codec;
import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.BedBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/** Existing site IDs, larger rotated plans, bounded to the origin and its immediate neighbor chunks.
 * All edits are preflighted before placement. No chunk loads or writes beyond that envelope. */
public final class ExplorationSiteFeature extends Feature<DefaultFeatureConfig>{
    private final int kind;
    public ExplorationSiteFeature(Codec<DefaultFeatureConfig> codec,int kind){super(codec);this.kind=kind;}
    private static BlockState air(){return Blocks.AIR.getDefaultState();}
    private static boolean rock(StructureWorldAccess w,BlockPos p){
        var s=w.getBlockState(p);
        return ImpactCraterFeature.natural(w,p)||s.isOf(LunarHorizons.CRYSTAL_ORE)||s.isOf(LunarHorizons.DARK_CRYSTAL_ORE)||s.isOf(LunarHorizons.TITANIUM_ORE)||s.isOf(LunarHorizons.HELIUM_ORE)||s.isOf(LunarHorizons.LUNAR_ICE);
    }
    private static int top(StructureWorldAccess w,int x,int z){return w.getTopY(Heightmap.Type.WORLD_SURFACE_WG,x,z);}
    private static BlockPos rotated(BlockPos origin,int rot,int x,int y,int z){
        return switch(rot){case 1->origin.add(-z,y,x);case 2->origin.add(-x,y,-z);case 3->origin.add(z,y,-x);default->origin.add(x,y,z);};
    }
    private static Direction rotated(int rot,Direction d){for(int i=0;i<rot;i++)d=d.rotateYClockwise();return d;}
    private final class Plan{
        final StructureWorldAccess w;final BlockPos origin;final int rot,cx,cz;
        final Map<BlockPos,BlockState> blocks=new LinkedHashMap<>();
        final Map<BlockPos,Integer> chests=new LinkedHashMap<>();
        Plan(StructureWorldAccess w,BlockPos origin,int rot,int cx,int cz){this.w=w;this.origin=origin;this.rot=rot;this.cx=cx;this.cz=cz;}
        BlockPos at(int x,int y,int z){return rotated(origin,rot,x,y,z);}
        void put(int x,int y,int z,Block b){put(x,y,z,b.getDefaultState());}
        void put(int x,int y,int z,BlockState s){blocks.put(at(x,y,z),s);}
        void fill(int x1,int y1,int z1,int x2,int y2,int z2,Block b){for(int x=x1;x<=x2;x++)for(int y=y1;y<=y2;y++)for(int z=z1;z<=z2;z++)put(x,y,z,b);}
        BlockState state(BlockPos p){BlockState state=blocks.get(p);return state==null?w.getBlockState(p):state;}
        void machine(int x,int y,int z,Block b,Direction facing){
            var s=b.getDefaultState();if(s.contains(Properties.HORIZONTAL_FACING))s=s.with(Properties.HORIZONTAL_FACING,rotated(rot,facing));
            put(x,y,z,s);
        }
        void chest(int x,int y,int z,int type,Direction facing){
            put(x,y,z,Blocks.CHEST.getDefaultState().with(Properties.HORIZONTAL_FACING,rotated(rot,facing)));chests.put(at(x,y,z),type);
        }
        void door(int x,int y,int z){door(x,y,z,Direction.NORTH);}
        void door(int x,int y,int z,Direction dir){
            var s=LunarHorizons.AIRLOCK.getDefaultState().with(Properties.HORIZONTAL_FACING,rotated(rot,dir));
            put(x,y,z,s.with(Properties.DOUBLE_BLOCK_HALF,net.minecraft.block.enums.DoubleBlockHalf.LOWER));put(x,y+1,z,s.with(Properties.DOUBLE_BLOCK_HALF,net.minecraft.block.enums.DoubleBlockHalf.UPPER));
        }
        void bed(int x,int y,int z,Direction dir){
            var s=Blocks.GRAY_BED.getDefaultState().with(BedBlock.FACING,rotated(rot,dir));
            put(x,y,z,s.with(BedBlock.PART,BedPart.FOOT));
            put(x+dir.getOffsetX(),y,z+dir.getOffsetZ(),s.with(BedBlock.PART,BedPart.HEAD));
        }
        void room(int x1,int z1,int x2,int z2,int floor,int roof,Block wall,Block deck){
            for(int x=x1;x<=x2;x++)for(int z=z1;z<=z2;z++)for(int y=floor;y<=roof;y++){
                boolean edge=x==x1||x==x2||z==z1||z==z2;
                put(x,y,z,y==floor?deck:y==roof||edge?wall:Blocks.AIR);
            }
        }
        boolean supported(int x,int y,int z,Block foundation,int limit){
            BlockPos p=at(x,y,z);
            for(int h=1;h<=limit+1;h++){
                BlockPos q=p.down(h);
                if(!state(q).isAir()&&(rock(w,q)||blocks.containsKey(q)))return true;
                if(!state(q).isAir()||h>limit)return false;
                blocks.put(q,foundation.getDefaultState());
            }
            return false;
        }
        boolean valid(){
            for(var e:blocks.entrySet()){
                BlockPos p=e.getKey();
                if(p.getX()<cx-16||p.getX()>cx+31||p.getZ()<cz-16||p.getZ()>cz+31||p.getY()<-60||p.getY()>185)return false;
                if(!w.isAir(p)&&!rock(w,p))return false;
            }
            return true;
        }
        boolean commit(Random random){
            if(!valid())return false;
            // Worldgen does not reliably send neighbor updates: compute connections from the complete plan.
            net.minecraft.state.property.BooleanProperty[] sides={Properties.DOWN,Properties.UP,Properties.NORTH,Properties.SOUTH,Properties.WEST,Properties.EAST};
            for(var e:blocks.entrySet()){
                BlockState s=e.getValue();BlockPos p=e.getKey();
                if(s.getBlock() instanceof ColonyConduitBlock line){
                    for(Direction d:Direction.values())s=s.with(sides[d.getId()],line.connects(state(p.offset(d))));
                }else if(s.getBlock() instanceof SolarPanelBlock)s=s.with(Properties.UP,ColonyConduitBlock.powerPort(state(p.up())));
                e.setValue(s);
            }
            Map<BlockPos,BlockState> changed=new LinkedHashMap<>();
            for(var e:blocks.entrySet()){
                BlockState old=w.getBlockState(e.getKey());
                if(old.equals(e.getValue()))continue;
                if(!w.setBlockState(e.getKey(),e.getValue(),2)){
                    List<BlockPos> undo=new ArrayList<>(changed.keySet());Collections.reverse(undo);
                    for(BlockPos q:undo)w.setBlockState(q,changed.get(q),2);
                    return false;
                }
                changed.put(e.getKey(),old);
            }
            for(var e:chests.entrySet())loot(w,e.getKey(),random,e.getValue());
            return true;
        }
    }
    @Override public boolean generate(FeatureContext<DefaultFeatureConfig> ctx){
        StructureWorldAccess w=ctx.getWorld();Random r=ctx.getRandom();
        int cx=ctx.getOrigin().getX()&~15,cz=ctx.getOrigin().getZ()&~15;
        int x=cx+7,z=cz+7,rot=r.nextInt(4);
        return switch(kind){
            case 0->outpost(w,r,cx,cz,x,z,rot);
            case 1->satellite(w,r,cx,cz,x,z,rot);
            case 2->laboratory(w,r,cx,cz,x,z,rot);
            case 3->geode(w,r,cx,cz,x,z,rot);
            case 4->lander(w,r,cx,cz,x,z,rot);
            case 5->relay(w,r,cx,cz,x,z,rot);
            default->expandedSite(w,r,cx,cz,x,z,rot);
        };
    }
    private static int surface(StructureWorldAccess w,int x,int z,int radius,int slope){
        int hi=-1000,lo=1000;
        for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            int y=top(w,x+dx,z+dz);BlockPos p=new BlockPos(x+dx,y-1,z+dz);
            if(!rock(w,p)||!rock(w,p.down()))return -1000;
            hi=Math.max(hi,y);lo=Math.min(lo,y);
        }
        return hi-lo<=slope&&hi>=8&&hi<=145?hi:-1000;
    }
    private boolean outpost(StructureWorldAccess w,Random r,int cx,int cz,int x,int z,int rot){
        int y=surface(w,x,z,11,6);if(y==-1000)return false;
        Plan p=new Plan(w,new BlockPos(x,y,z),rot,cx,cz);
        Block wall=LunarHorizons.HABITAT_HULL,deck=LunarHorizons.HABITAT_FLOOR;
        // Two wings and a rear laboratory, connected by a central airlock corridor.
        p.room(-11,-5,-3,5,0,4,wall,deck);p.room(3,-5,11,5,0,4,wall,deck);
        p.room(-3,4,3,11,0,4,wall,deck);p.room(-3,-2,3,4,0,4,wall,deck);
        p.fill(-3,1,-1,-3,2,1,Blocks.AIR);p.fill(3,1,-1,3,2,1,Blocks.AIR);p.fill(-1,1,4,1,2,4,Blocks.AIR);
        p.door(0,1,-2);p.door(0,1,-1);
        for(int i=-9;i<=-5;i+=2){p.put(i,2,-5,LunarHorizons.PRESSURE_GLASS);p.bed(i,1,3,Direction.SOUTH);}
        for(int i=5;i<=9;i+=2)p.put(i,2,-5,LunarHorizons.PRESSURE_GLASS);
        p.chest(-10,1,-4,0,Direction.SOUTH);p.chest(-2,1,10,1,Direction.NORTH);
        p.machine(7,1,3,LunarHorizons.OXYGEN_GENERATOR,Direction.NORTH);
        p.machine(8,1,3,LunarHorizons.BATTERY,Direction.NORTH);
        p.machine(9,1,3,LunarHorizons.POWER_TERMINAL,Direction.NORTH);
        p.put(7,1,2,LunarHorizons.OXYGEN_PIPE);p.put(7,1,1,LunarHorizons.OXYGEN_VENT);
        p.put(8,2,3,LunarHorizons.POWER_CABLE);p.put(8,3,3,LunarHorizons.POWER_CABLE);p.put(8,4,3,LunarHorizons.POWER_FEEDTHROUGH);
        for(int a=6;a<=10;a++)for(int b=2;b<=3;b++)p.put(a,5,b,(a==10&&r.nextBoolean())?LunarHorizons.DAMAGED_SOLAR_PANEL:LunarHorizons.SOLAR_PANEL);
        p.put(10,1,3,LunarHorizons.POWER_CABLE); // spare end of a power line
        for(int a=1;a<=8;a++)p.put(a,1,0,LunarHorizons.POWER_WIRE);
        p.put(8,1,1,LunarHorizons.POWER_WIRE);p.put(8,1,2,LunarHorizons.POWER_WIRE);
        p.machine(6,1,1,LunarHorizons.POWERED_LAMP,Direction.NORTH);
        for(int b=1;b<=8;b++)if(b!=3)p.put(1,1,b,LunarHorizons.POWER_WIRE); // repair this missing segment to energize the rear lab
        p.machine(2,1,9,LunarHorizons.ELECTRIC_REFINERY,Direction.WEST);
        p.machine(2,1,8,LunarHorizons.ELECTRIC_REFILL,Direction.WEST);
        p.put(-10,3,-4,LunarHorizons.LUNAR_LANTERN);p.put(10,3,-4,LunarHorizons.LUNAR_LANTERN);p.put(0,3,8,LunarHorizons.LUNAR_LANTERN);p.put(0,5,0,LunarHorizons.SURVEY_BEACON);
        // Only support the occupied floor, not the empty gaps between modules.
        for(int a=-11;a<=11;a++)for(int b=-5;b<=11;b++)if(p.blocks.containsKey(p.at(a,0,b))&&!p.supported(a,0,b,LunarHorizons.MOON_BRICKS,6))return false;
        // Walkable approach from the external door to the actual ground.
        BlockPos entrance=p.at(0,0,-3);int ground=top(w,entrance.getX(),entrance.getZ());
        if(y-ground>3)return false;
        for(int step=0;step<=y-ground;step++){int zz=-3-step;p.put(0,-step,zz,LunarHorizons.HABITAT_FLOOR);if(!p.supported(0,-step,zz,LunarHorizons.MOON_BRICKS,6))return false;}
        return p.commit(r);
    }
    private boolean satellite(StructureWorldAccess w,Random r,int cx,int cz,int x,int z,int rot){
        int highest=surface(w,x,z,10,7);if(highest==-1000)return false;
        int y=top(w,x,z)-1;Plan p=new Plan(w,new BlockPos(x,y,z),rot,cx,cz);
        int half= r.nextBoolean()?2:1,wing=7+r.nextInt(3);boolean broken=r.nextBoolean();
        // Long, shallow impact scar follows local terrain rather than flattening the landscape.
        for(int a=-10;a<=10;a++)for(int b=-4;b<=4;b++){
            double q=a*a/100.0+b*b/16.0;if(q>=1)continue;
            BlockPos col=p.at(a,0,b);int t=top(w,col.getX(),col.getZ())-y-1;
            int depth=q<.32?2:q<.75?1:0;
            for(int k=0;k<=depth;k++)if(!rock(w,p.at(a,t-k,b)))return false;
            for(int k=0;k<depth;k++)p.put(a,t-k,b,Blocks.AIR);
            if(depth>0)p.put(a,t-depth,b,LunarHorizons.EJECTA_ROCK);
        }
        p.fill(-half,0,-1,half,0,1,LunarHorizons.TITANIUM_BLOCK);
        p.fill(-half,1,-1,half,2,1,LunarHorizons.REINFORCED_HULL);
        p.fill(-half,1,0,half,1,0,Blocks.AIR);
        p.put(0,1,-1,Blocks.AIR);p.put(0,2,-1,Blocks.AIR);
        p.chest(0,1,0,2,Direction.NORTH);p.put(0,3,0,LunarHorizons.SURVEY_BEACON);
        p.put(0,4,0,Blocks.LIGHTNING_ROD);
        p.put(-half,3,0,Blocks.IRON_BARS);p.put(half,3,0,Blocks.IRON_BARS);
        for(int a=-half;a<=half;a++)for(int b=-1;b<=1;b++)if(!p.supported(a,0,b,LunarHorizons.TITANIUM_BLOCK,6))return false;
        for(int side:new int[]{-1,1}){
            int end=broken&&side==1?wing-3:wing;
            for(int a=half+1;a<=end;a++){
                p.put(side*a,1,0,LunarHorizons.TITANIUM_BLOCK);
                if(a>half+1)for(int b:new int[]{-1,1})p.put(side*a,1,b,(broken&&side==1||r.nextInt(6)==0)?LunarHorizons.DAMAGED_SOLAR_PANEL:LunarHorizons.SOLAR_PANEL);
            }
        }
        for(int i=0;i<12;i++){
            int a=r.nextInt(19)-9,b=r.nextInt(11)-5;if(Math.abs(a)<half+2||Math.abs(b)<2)continue;
            BlockPos col=p.at(a,0,b);int h=top(w,col.getX(),col.getZ())-y;
            while(h>-8&&p.state(p.at(a,h-1,b)).isAir())h--;
            if(p.state(p.at(a,h,b)).isAir()&&rock(w,p.at(a,h-1,b)))p.put(a,h,b,r.nextBoolean()?LunarHorizons.DAMAGED_SOLAR_PANEL:LunarHorizons.TITANIUM_BLOCK);
        }
        return p.commit(r);
    }
    private boolean laboratory(StructureWorldAccess w,Random r,int cx,int cz,int x,int z,int rot){
        BlockPos shaft=rotated(new BlockPos(x,0,z),rot,0,0,-6);int surface=top(w,shaft.getX(),shaft.getZ());
        if(surface<12||surface>145)return false;
        for(int attempt=0;attempt<5;attempt++){
            int y=Math.max(-48,surface-23-attempt*7);
            Plan p=new Plan(w,new BlockPos(x,y,z),rot,cx,cz);
            Block wall=LunarHorizons.REINFORCED_HULL,deck=LunarHorizons.HABITAT_FLOOR;
            // Require a solid natural cover above the entire building before excavating.
            int solid=0,total=0;
            for(int a=-11;a<=11;a++)for(int b=-7;b<=7;b++){total++;if(rock(w,p.at(a,11,b))&&rock(w,p.at(a,12,b)))solid++;}
            if(solid<total*.80)continue;
            p.room(-11,-7,11,7,0,5,wall,deck);p.room(-11,-7,11,7,5,10,wall,deck);
            for(int floor:new int[]{0,5}){
                p.fill(0,floor+1,-5,0,floor+4,7,wall);
                p.door(0,floor+1,0,Direction.WEST);
                p.put(-9,floor+3,-5,LunarHorizons.LUNAR_LANTERN);p.put(9,floor+3,5,LunarHorizons.LUNAR_LANTERN);
            }
            // Upper floor: sample archive and instrument room.
            for(int a=-9;a<=-3;a+=2){p.put(a,6,5,LunarHorizons.INDUSTRIAL_GRATE);p.put(a,7,5,LunarHorizons.PRESSURE_GLASS);}
            p.chest(-10,6,-5,3,Direction.SOUTH);p.chest(10,6,-5,4,Direction.SOUTH);
            p.machine(8,6,4,LunarHorizons.ELECTRIC_REFINERY,Direction.NORTH);p.machine(9,6,4,LunarHorizons.ADVANCED_ASSEMBLER,Direction.NORTH);
            p.machine(10,6,4,LunarHorizons.POWER_TERMINAL,Direction.NORTH);
            // Lower floor: oxygen plant, batteries and a locked-looking separate archive.
            p.machine(-9,1,4,LunarHorizons.OXYGEN_GENERATOR,Direction.NORTH);p.machine(-8,1,4,LunarHorizons.INDUSTRIAL_BATTERY,Direction.NORTH);
            p.machine(-7,1,4,LunarHorizons.HELIUM_GENERATOR,Direction.NORTH);
            p.put(-9,1,3,LunarHorizons.OXYGEN_PIPE);p.put(-9,1,2,LunarHorizons.OXYGEN_VENT);
            for(int a=-7;a<=-1;a++)p.put(a,1,5,LunarHorizons.POWER_CABLE);
            p.put(-1,1,4,LunarHorizons.POWER_CABLE);p.put(-1,1,3,LunarHorizons.POWER_CABLE);
            for(int h=2;h<=4;h++)p.put(-1,h,3,LunarHorizons.POWER_CABLE);
            p.put(-1,5,3,LunarHorizons.POWER_FEEDTHROUGH);
            p.put(-1,6,3,LunarHorizons.POWER_CABLE);
            // Upper rooms are separated by a sealed feedthrough, not a hole in the partition.
            p.put(0,6,3,LunarHorizons.POWER_FEEDTHROUGH);
            for(int a=1;a<=10;a++)p.put(a,6,3,LunarHorizons.POWER_WIRE);
            p.fill(5,1,3,10,4,3,wall);p.door(7,1,3);p.fill(5,1,4,5,4,6,wall);
            p.chest(10,1,6,5,Direction.NORTH);p.put(9,3,6,LunarHorizons.LUNAR_LANTERN);
            // One shaft joins both floors and reaches a small surface headhouse.
            int height=surface-y;
            for(int h=1;h<=height+3;h++){
                p.put(1,h,-6,wall);
                p.put(0,h,-6,Blocks.LADDER.getDefaultState().with(Properties.HORIZONTAL_FACING,rotated(rot,Direction.WEST)));
                if(h>=10){p.put(-1,h,-6,wall);p.put(0,h,-7,wall);p.put(0,h,-5,wall);}
            }
            p.room(-2,-8,2,-4,height,height+4,wall,deck);
            p.put(0,height,-6,Blocks.LADDER.getDefaultState().with(Properties.HORIZONTAL_FACING,rotated(rot,Direction.WEST)));
            for(int h=height+1;h<=height+2;h++)p.put(0,h,-6,Blocks.LADDER.getDefaultState().with(Properties.HORIZONTAL_FACING,rotated(rot,Direction.WEST)));
            p.put(1,height+1,-6,wall);p.put(1,height+2,-6,wall);
            p.door(0,height+1,-4);p.put(0,height+5,-6,LunarHorizons.SURVEY_BEACON);p.put(-1,height+3,-5,LunarHorizons.LUNAR_LANTERN);
            boolean supported=true;
            for(int a=-11;a<=11&&supported;a++)for(int b=-7;b<=7;b++)if(!p.supported(a,0,b,LunarHorizons.MOON_BRICKS,6)){supported=false;break;}
            if(!supported)continue;
            for(int a=-2;a<=2&&supported;a++)for(int b=-8;b<=-4;b++)if(!p.supported(a,height,b,wall,5)){supported=false;break;}
            if(!supported)continue;
            BlockPos approach=p.at(0,height,-3);
            // The headhouse door at z=-4 opens towards local z=-3.
            int ground=top(w,approach.getX(),approach.getZ());
            if(Math.abs(surface-ground)>3)continue;
            if(ground<=surface){
                for(int step=0;step<=surface-ground;step++){
                    p.put(0,height-step,-3+step,deck);
                    if(!p.supported(0,height-step,-3+step,wall,5)){supported=false;break;}
                }
            }else{
                // Carve only a short entrance ramp, never an arbitrary shaft in the landscape.
                for(int step=0;step<ground-surface;step++){
                    for(int h=height+1+step;h<=height+3+step;h++)p.put(0,h,-3+step,Blocks.AIR);
                    p.put(0,height+step,-3+step,deck);
                }
            }
            if(supported&&p.commit(r))return true;
        }
        return false;
    }
    private boolean lander(StructureWorldAccess w,Random r,int cx,int cz,int x,int z,int rot){
        int y=surface(w,x,z,6,4);if(y==-1000)return false;
        Plan p=new Plan(w,new BlockPos(x,y,z),rot,cx,cz);
        Block metal=LunarHorizons.TITANIUM_BLOCK,hull=LunarHorizons.HABITAT_HULL;
        // Four footpads with individual load-bearing supports, diagonal legs, engine bell.
        for(int a:new int[]{-1,1})for(int b:new int[]{-1,1}){
            int fx=a*4,fz=b*4;
            BlockPos foot=p.at(fx,0,fz);
            int h=top(w,foot.getX(),foot.getZ())-y;
            if(h>0||h<-4)return false;
            p.fill(fx-1,h,fz-1,fx+1,h,fz+1,metal);
            for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)if(!p.supported(fx+dx,h,fz+dz,metal,4))return false;
            p.fill(fx,h+1,fz,fx,1,fz,metal);
            p.fill(Math.min(a*3,fx),1,Math.min(b*3,fz),Math.max(a*3,fx),1,Math.max(b*3,fz),metal);
            p.put(a*3,2,b*3,metal);
            p.put(a*2,2,b*3,metal);p.put(a*2,3,b*2,metal);
            p.put(a*2,2,b*2,metal);
        }
        p.fill(-1,0,-1,1,0,1,LunarHorizons.REINFORCED_HULL);
        p.fill(-1,1,-1,1,1,1,metal);
        p.fill(-2,2,-2,2,2,2,metal);
        p.room(-2,-2,2,2,3,7,hull,LunarHorizons.HABITAT_FLOOR);
        p.put(-1,5,-2,LunarHorizons.PRESSURE_GLASS);p.put(1,5,-2,LunarHorizons.PRESSURE_GLASS);
        p.put(-2,5,0,LunarHorizons.PRESSURE_GLASS);p.put(2,5,0,LunarHorizons.PRESSURE_GLASS);
        p.door(0,4,-2);
        p.chest(-1,4,1,6,Direction.NORTH);
        p.machine(1,4,1,LunarHorizons.BATTERY,Direction.NORTH);
        p.put(0,4,1,LunarHorizons.POWER_TERMINAL);
        p.put(1,5,1,LunarHorizons.POWER_WIRE);p.put(1,6,1,LunarHorizons.POWER_WIRE);p.put(1,7,1,LunarHorizons.POWER_FEEDTHROUGH);
        p.put(1,8,1,LunarHorizons.SOLAR_PANEL);p.put(0,8,1,r.nextBoolean()?LunarHorizons.DAMAGED_SOLAR_PANEL:LunarHorizons.SOLAR_PANEL);
        p.put(-1,8,-1,LunarHorizons.SURVEY_BEACON);p.put(-1,9,-1,Blocks.LIGHTNING_ROD);
        p.put(0,6,1,LunarHorizons.LUNAR_LANTERN);
        // Ladder descends from the exterior hatch to a supported landing.
        BlockPos ladderBase=p.at(0,0,-3);
        int ladderGround=top(w,ladderBase.getX(),ladderBase.getZ())-y;
        for(int h=ladderGround;h<=3;h++){
            p.put(0,h,-2,metal);
            p.put(0,h,-3,Blocks.LADDER.getDefaultState().with(Properties.HORIZONTAL_FACING,rotated(rot,Direction.NORTH)));
        }
        p.put(0,3,-4,LunarHorizons.INDUSTRIAL_GRATE);
        
        // Ground equipment sits on natural terrain, independent of the cabin height.
        BlockPos cache=p.at(-5,0,0);int gy=top(w,cache.getX(),cache.getZ())-y;
        p.put(-5,gy,0,LunarHorizons.TITANIUM_BLOCK);p.chest(-5,gy+1,0,8,Direction.SOUTH);
        return p.commit(r);
    }
    private boolean relay(StructureWorldAccess w,Random r,int cx,int cz,int x,int z,int rot){
        int y=surface(w,x,z,6,5);if(y==-1000)return false;
        Plan p=new Plan(w,new BlockPos(x,y,z),rot,cx,cz);
        Block wall=LunarHorizons.REINFORCED_HULL,metal=LunarHorizons.TITANIUM_BLOCK,deck=LunarHorizons.HABITAT_FLOOR;
        p.room(-4,-3,4,3,0,4,wall,deck);p.door(0,1,-3);
        p.put(-2,2,-3,LunarHorizons.PRESSURE_GLASS);p.put(2,2,-3,LunarHorizons.PRESSURE_GLASS);
        p.machine(2,1,2,LunarHorizons.BATTERY,Direction.NORTH);
        p.machine(1,1,2,LunarHorizons.POWER_TERMINAL,Direction.NORTH);
        p.put(2,2,2,LunarHorizons.POWER_CABLE);p.put(2,3,2,LunarHorizons.POWER_CABLE);p.put(2,4,2,LunarHorizons.POWER_FEEDTHROUGH);
        for(int a=2;a<=4;a++)for(int b=1;b<=2;b++)p.put(a,5,b,r.nextInt(5)==0?LunarHorizons.DAMAGED_SOLAR_PANEL:LunarHorizons.SOLAR_PANEL);
        p.chest(-3,1,2,7,Direction.NORTH);p.put(-3,3,-2,LunarHorizons.LUNAR_LANTERN);
        // Four-legged antenna mast with periodic bracing and a climbable service column.
        int height=17+r.nextInt(5);
        for(int h=5;h<=height;h++){
            for(int a:new int[]{-1,1})for(int b:new int[]{-1,1})p.put(a,h,b,metal);
            if(h%4==1){p.fill(-1,h,-1,1,h,-1,metal);p.fill(-1,h,1,1,h,1,metal);p.put(-1,h,0,metal);}
            p.put(1,h,0,metal);
            p.put(2,h,0,Blocks.LADDER.getDefaultState().with(Properties.HORIZONTAL_FACING,rotated(rot,Direction.EAST)));
        }
        p.fill(-2,height+1,-2,2,height+1,2,LunarHorizons.INDUSTRIAL_GRATE);
        p.put(2,height+1,0,Blocks.LADDER.getDefaultState().with(Properties.HORIZONTAL_FACING,rotated(rot,Direction.EAST)));p.put(1,height+1,0,metal);
        p.put(0,height+2,0,LunarHorizons.SURVEY_BEACON);
        p.put(0,height+3,0,Blocks.LIGHTNING_ROD);
        for(int side:new int[]{-1,1}){
            p.fill(0,height-2,Math.min(side,side*4),0,height-2,Math.max(side,side*4),metal);
            p.put(0,height-1,side*4,Blocks.LIGHTNING_ROD);
        }
        BlockPos ladderBase=p.at(5,0,0);
        int ladderGround=top(w,ladderBase.getX(),ladderBase.getZ())-y;
        for(int h=ladderGround;h<=5;h++){
            if(h<0||h==5)p.put(4,h,0,metal);
            p.put(5,h,0,Blocks.LADDER.getDefaultState().with(Properties.HORIZONTAL_FACING,rotated(rot,Direction.EAST)));
        }
        for(int a=-4;a<=4;a++)for(int b=-3;b<=3;b++)if(!p.supported(a,0,b,LunarHorizons.MOON_BRICKS,6))return false;
        BlockPos entrance=p.at(0,0,-4);int g=top(w,entrance.getX(),entrance.getZ());
        if(y-g>3||g>y)return false;
        for(int step=0;step<=y-g;step++){p.put(0,-step,-4-step,deck);if(!p.supported(0,-step,-4-step,wall,6))return false;}
        return p.commit(r);
    }
    // The feature writer is limited to the origin and immediate neighbors. Large sites
    // use compact layouts of up to 45 blocks rather than writing into unloaded chunks.
    private boolean expandedSite(StructureWorldAccess w,Random r,int cx,int cz,int x,int z,int rot){
        int radius=switch(kind){case 6,9,10,11,13,14->11;case 7->13;case 8,12->9;default->21;};
        boolean buried=kind==10||kind==11||kind==12||kind==13||kind==14||kind==17;
        int y;
        if(buried){
            int surface=top(w,x,z);
            y=Math.max(-48,surface-(kind==17?38:kind==10?34:27)-r.nextInt(8));
            if(surface-y<18)return false;
        }else{
            // Sample the height field, then validate every actual foundation cell.
            int hi=-1000,lo=1000;
            for(int a=-radius;a<=radius;a+=3)for(int b=-radius;b<=radius;b+=3){
                int h=top(w,x+a,z+b);
                hi=Math.max(hi,h);lo=Math.min(lo,h);
            }
            if(hi-lo>10||hi<8||hi>145)return false;
            y=hi;
        }
        Plan p=new Plan(w,new BlockPos(x,y,z),rot,cx,cz);
        switch(kind){
            case 6->observatory(p,r);
            case 7->solarFarm(p,r);
            case 8->garage(p,r);
            case 9->heliumSite(p,r);
            case 10->mine(p,r);
            case 11->iceSite(p,r);
            case 12->bunker(p,r);
            case 13->dataCenter(p,r);
            case 14->ruins(p,r);
            case 15->colony(p,r);
            case 16->spaceport(p,r);
            case 17->reactor(p,r);
            case 18->alienShip(p,r);
            default->{return false;}
        }
        // Every structural floor at y=0 rests on natural rock or a bounded foundation.
        // Air in the plan is not a structural floor, and is never used as support.
        ArrayList<BlockPos> floors=new ArrayList<>();
        int cover=0,covered=0;
        for(var e:p.blocks.entrySet()){
            BlockPos q=e.getKey();
            if(q.getY()==y&&!e.getValue().isAir())floors.add(q);
            if(buried&&q.getY()==y+(kind==17?12:kind==14?6:5)&&!e.getValue().isAir()){
                cover++;
                if(rock(w,q.up(5))&&rock(w,q.up(6)))covered++;
            }
        }
        if(buried&&(cover==0||covered<cover*.65))return false;
        for(BlockPos q:floors){
            boolean support=false;
            for(int h=1;h<=12;h++){
                BlockPos below=q.down(h);
                if(rock(w,below)){support=true;break;}
                if(!w.isAir(below))return false;
                p.blocks.put(below,LunarHorizons.MOON_BRICKS.getDefaultState());
            }
            if(!support)return false;
        }
        if(buried&&!accessShaft(p,0,kind==17?-15:kind==12?-4:-5))return false;
        if(kind==8){if(!garageApproach(p))return false;}
        else if(!buried&&kind!=18&&!surfaceApproach(p,0,switch(kind){case 6,9->-11;case 7->-13;default->-21;}))return false;
        return p.commit(r);
    }
    private void light(Plan p,int x,int y,int z){p.put(x,y,z,LunarHorizons.LUNAR_LANTERN);}
    private void chamber(Plan p,int x1,int z1,int x2,int z2,int roof){
        p.room(x1,z1,x2,z2,0,roof,LunarHorizons.REINFORCED_HULL,LunarHorizons.HABITAT_FLOOR);
    }
    private void window(Plan p,int x,int z){p.put(x,2,z,LunarHorizons.PRESSURE_GLASS);}
    private void batteryRoof(Plan p,int x,int z,int roof,Random r){
        p.machine(x,1,z,LunarHorizons.BATTERY,Direction.NORTH);
        p.machine(x-1,1,z,LunarHorizons.POWER_TERMINAL,Direction.NORTH);
        for(int h=2;h<roof;h++)p.put(x,h,z,LunarHorizons.POWER_CABLE);
        p.put(x,roof,z,LunarHorizons.POWER_FEEDTHROUGH);
        for(int a=x-1;a<=x+1;a++)for(int b=z-1;b<=z+1;b++)
            p.put(a,roof+1,b,r.nextInt(7)==0?LunarHorizons.DAMAGED_SOLAR_PANEL:LunarHorizons.SOLAR_PANEL);
    }
    private boolean garageApproach(Plan p){
        for(int x=-3;x<=3;x++)for(int step=0;step<7;step++){
            int z=-9-step;BlockPos at=p.at(x,0,z);
            int ground=top(p.w,at.getX(),at.getZ())-p.origin.getY();
            int h=-step;
            if(ground>h+1)return false;
            if(h<ground-1)break;
            p.put(x,h,z,LunarHorizons.HABITAT_FLOOR);
            if(!p.supported(x,h,z,LunarHorizons.MOON_BRICKS,10))return false;
        }
        return true;
    }
    private boolean surfaceApproach(Plan p,int x,int z){
        BlockPos at=p.at(x,0,z);
        int delta=p.origin.getY()-top(p.w,at.getX(),at.getZ());
        if(delta<0||delta>10)return false;
        // A ladder on a solid column avoids long stairs crossing the feature boundary.
        for(int h=-delta;h<=0;h++){
            p.put(x,h,z,LunarHorizons.HABITAT_FLOOR);
            p.put(x,h,z-1,Blocks.LADDER.getDefaultState().with(Properties.HORIZONTAL_FACING,rotated(p.rot,Direction.NORTH)));
        }
        return p.supported(x,-delta,z,LunarHorizons.MOON_BRICKS,10);
    }
    private boolean accessShaft(Plan p,int x,int z){
        BlockPos q=p.at(x,0,z);
        int surface=top(p.w,q.getX(),q.getZ()),h=surface-p.origin.getY();
        if(h<12||h>110)return false;
        for(int y=1;y<=h+1;y++){
            p.put(x+1,y,z,LunarHorizons.REINFORCED_HULL);
            p.put(x,y,z,Blocks.LADDER.getDefaultState().with(Properties.HORIZONTAL_FACING,rotated(p.rot,Direction.WEST)));
            if(y>=5&&y<h){
                p.put(x-1,y,z,LunarHorizons.REINFORCED_HULL);
                p.put(x,y,z-1,LunarHorizons.REINFORCED_HULL);
                p.put(x,y,z+1,LunarHorizons.REINFORCED_HULL);
            }
        }
        p.room(x-2,z-2,x+2,z+2,h,h+4,LunarHorizons.HABITAT_HULL,LunarHorizons.HABITAT_FLOOR);
        for(int y=h;y<=h+1;y++){
            p.put(x+1,y,z,LunarHorizons.REINFORCED_HULL);
            p.put(x,y,z,Blocks.LADDER.getDefaultState().with(Properties.HORIZONTAL_FACING,rotated(p.rot,Direction.WEST)));
        }
        p.door(x,h+1,z-2);p.put(x,h+5,z,LunarHorizons.SURVEY_BEACON);
        light(p,x-1,h+3,z);
        for(int a=x-2;a<=x+2;a++)for(int b=z-2;b<=z+2;b++)if(a!=x||b!=z)if(!p.supported(a,h,b,LunarHorizons.MOON_BRICKS,8))return false;
        return true;
    }
    private void observatory(Plan p,Random r){
        chamber(p,-10,-10,10,2,4);p.door(0,1,-10);
        window(p,-5,-10);window(p,5,-10);
        p.fill(0,1,-8,0,3,-1,LunarHorizons.HABITAT_HULL);p.door(0,1,-4,Direction.WEST);
        batteryRoof(p,7,-5,4,r);
        p.machine(-7,1,-6,LunarHorizons.POWER_TERMINAL,Direction.SOUTH);
        for(int x=-8;x<=-2;x+=2)p.put(x,1,0,LunarHorizons.ADVANCED_ASSEMBLER);
        p.chest(-9,1,-8,9,Direction.SOUTH);light(p,-5,3,-3);
        // Parabolic bowl: rising circular rim, a receiver on a central mast.
        p.fill(-2,0,4,2,0,8,LunarHorizons.TITANIUM_BLOCK);
        p.fill(-1,1,5,1,6,7,LunarHorizons.TITANIUM_BLOCK);
        for(int x=-9;x<=9;x++)for(int z=-4;z<=4;z++){
            double d=x*x/81.0+z*z/16.0;if(d>1)continue;
            int y=7+(int)Math.round(d*5);
            p.put(x,y,6+z,LunarHorizons.TITANIUM_BLOCK);
        }
        p.fill(0,8,6,0,13,6,Blocks.IRON_BARS);p.put(0,14,6,LunarHorizons.SURVEY_BEACON);
    }
    private void solarFarm(Plan p,Random r){
        chamber(p,-4,-12,4,-7,4);p.door(0,1,-12);
        p.chest(-3,1,-10,10,Direction.SOUTH);light(p,-2,3,-9);
        p.machine(1,1,-9,LunarHorizons.INDUSTRIAL_BATTERY,Direction.NORTH);
        p.machine(2,1,-9,LunarHorizons.POWER_TERMINAL,Direction.NORTH);
        p.put(1,1,-8,LunarHorizons.POWER_CABLE);p.put(1,1,-7,LunarHorizons.POWER_FEEDTHROUGH);
        p.fill(-12,0,-6,12,0,10,LunarHorizons.INDUSTRIAL_GRATE);
        for(int z=-5;z<=9;z++)p.put(1,1,z,LunarHorizons.POWER_CABLE);
        for(int z=-5;z<=9;z+=4)for(int x=-11;x<=11;x++){
            if(x==1)continue;
            p.put(x,1,z,r.nextInt(8)==0?LunarHorizons.DAMAGED_SOLAR_PANEL:LunarHorizons.SOLAR_PANEL);
            p.put(x,1,z+1,r.nextInt(10)==0?LunarHorizons.DAMAGED_SOLAR_PANEL:LunarHorizons.SOLAR_PANEL);
        }
    }
    private void garage(Plan p,Random r){
        chamber(p,-8,-8,8,8,5);p.fill(-3,1,-8,3,3,-8,Blocks.AIR);
        for(int x:new int[]{-5,5}){
            p.fill(x-2,0,-5,x+2,0,5,LunarHorizons.INDUSTRIAL_GRATE);
            p.machine(x,1,6,LunarHorizons.INDUSTRIAL_BATTERY,Direction.NORTH);
            p.put(x,1,5,LunarHorizons.POWER_CABLE);light(p,x,4,0);
        }
        batteryRoof(p,7,5,5,r);
        p.fill(6,1,6,6,1,7,LunarHorizons.POWER_WIRE);
        p.chest(-7,1,7,11,Direction.NORTH);
        p.machine(-3,1,7,LunarHorizons.ELECTRIC_REFINERY,Direction.NORTH);
        // Static maintenance stand with wheel assemblies; a rover item is in the locker.
        p.fill(-5,1,-1,-3,1,1,LunarHorizons.TITANIUM_BLOCK);
        for(int x:new int[]{-6,-2})for(int z:new int[]{-1,1})p.put(x,1,z,Blocks.BLACK_CONCRETE);
    }
    private void heliumSite(Plan p,Random r){
        chamber(p,-10,-10,10,0,5);p.door(0,1,-10);
        batteryRoof(p,8,-3,5,r);
        for(int x:new int[]{-7,-3,3}){
            p.machine(x,1,-2,LunarHorizons.FUEL_SYNTHESIZER,Direction.NORTH);
            p.put(x,1,-1,LunarHorizons.POWER_CABLE);
            p.put(x,2,-2,LunarHorizons.INDUSTRIAL_GRATE);light(p,x,4,-4);
            p.fill(x-1,0,3,x+1,0,8,LunarHorizons.TITANIUM_BLOCK);
            p.fill(x,1,5,x,8,5,LunarHorizons.TITANIUM_BLOCK);
            p.fill(x-1,8,5,x+1,8,5,LunarHorizons.REINFORCED_HULL);
            p.put(x,7,4,Blocks.LIGHTNING_ROD);
        }
        p.fill(-7,1,-1,8,1,-1,LunarHorizons.POWER_CABLE);
        p.put(8,1,-2,LunarHorizons.POWER_CABLE);
        p.chest(-9,1,-8,12,Direction.SOUTH);
        p.machine(7,1,-8,LunarHorizons.HELIUM_GENERATOR,Direction.SOUTH);for(int z=-7;z<=-4;z++)p.put(7,1,z,LunarHorizons.POWER_WIRE);p.put(7,1,-3,LunarHorizons.POWER_TERMINAL);
    }
    private void mine(Plan p,Random r){
        chamber(p,-10,-8,10,5,5);
        p.room(-2,4,2,10,0,4,LunarHorizons.MOON_BRICKS,LunarHorizons.HABITAT_FLOOR);
        p.fill(-1,1,5,1,3,5,Blocks.AIR);
        for(int x=-8;x<=8;x++)p.put(x,1,1,Blocks.RAIL.getDefaultState().with(Properties.RAIL_SHAPE,(p.rot%2==0?net.minecraft.block.enums.RailShape.EAST_WEST:net.minecraft.block.enums.RailShape.NORTH_SOUTH)));
        for(int x:new int[]{-8,-4,4,8}){p.fill(x,1,4,x,4,4,LunarHorizons.TITANIUM_BLOCK);light(p,x,4,0);}
        for(int x=-8;x<=8;x+=4)p.put(x,2,10,r.nextBoolean()?LunarHorizons.TITANIUM_ORE:LunarHorizons.HELIUM_ORE);
        p.chest(-9,1,-5,13,Direction.SOUTH);p.machine(8,1,-5,LunarHorizons.ELECTRIC_REFINERY,Direction.SOUTH);
    }
    private void iceSite(Plan p,Random r){
        chamber(p,-10,-8,10,8,5);
        for(int x:new int[]{-7,0,7}){
            p.fill(x-2,1,3,x+2,3,6,LunarHorizons.LUNAR_ICE);
            p.machine(x,1,-2,LunarHorizons.OXYGEN_GENERATOR,Direction.NORTH);
            p.put(x,1,-1,LunarHorizons.OXYGEN_PIPE);p.put(x,1,0,LunarHorizons.OXYGEN_VENT);
            light(p,x,4,1);
        }
        p.machine(8,1,-6,LunarHorizons.HELIUM_GENERATOR,Direction.SOUTH);for(int x=-7;x<=7;x++)p.put(x,1,-3,LunarHorizons.POWER_WIRE);p.put(7,1,-4,LunarHorizons.POWER_WIRE);p.put(7,1,-5,LunarHorizons.POWER_WIRE);
        p.machine(7,1,-6,LunarHorizons.BATTERY,Direction.SOUTH);
        p.chest(-9,1,-5,14,Direction.SOUTH);
    }
    private void bunker(Plan p,Random r){
        chamber(p,-8,-6,8,8,5);
        p.fill(-8,1,0,8,4,0,LunarHorizons.REINFORCED_HULL);p.door(0,1,0);
        for(int x=-6;x<=6;x+=3)p.bed(x,1,5,Direction.SOUTH);
        p.chest(-7,1,-4,15,Direction.SOUTH);p.chest(7,1,6,15,Direction.NORTH);
        p.machine(5,1,-4,LunarHorizons.OXYGEN_GENERATOR,Direction.SOUTH);
        p.machine(4,1,-4,LunarHorizons.BATTERY,Direction.SOUTH);
        p.machine(3,1,-4,LunarHorizons.HELIUM_GENERATOR,Direction.SOUTH);
        light(p,-5,4,-3);light(p,5,4,4);
    }
    private void dataCenter(Plan p,Random r){
        chamber(p,-10,-8,10,9,5);
        for(int x:new int[]{-7,-3,3,7})for(int z=-2;z<=6;z+=2){
            p.put(x,1,z,LunarHorizons.REINFORCED_HULL);
            p.put(x,2,z,LunarHorizons.POWER_TERMINAL);
            p.put(x,3,z,LunarHorizons.INDUSTRIAL_GRATE);
            p.put(x,4,z,LunarHorizons.OXYGEN_PIPE);
        }
        p.fill(-7,4,0,7,4,0,LunarHorizons.OXYGEN_PIPE);
        p.chest(9,1,7,16,Direction.WEST);
        p.machine(8,1,-5,LunarHorizons.ADVANCED_ASSEMBLER,Direction.SOUTH);
        p.machine(9,1,-5,LunarHorizons.INDUSTRIAL_BATTERY,Direction.SOUTH);light(p,0,4,5);
    }
    private void ruins(Plan p,Random r){
        p.room(-10,-8,10,10,0,6,Blocks.POLISHED_DEEPSLATE,Blocks.DEEPSLATE_TILES);
        p.fill(-2,1,-8,2,3,-8,Blocks.AIR);
        for(int x:new int[]{-7,7})for(int z:new int[]{-3,3,8}){
            p.fill(x,1,z,x,5,z,Blocks.CHISELED_DEEPSLATE);
            p.put(x,5,z,LunarHorizons.CRYSTAL_STONE);
        }
        for(int x=-6;x<=6;x++)for(int z=2;z<=6;z++)if(Math.abs(x)==6||z==2||z==6)p.put(x,1,z,Blocks.CRYING_OBSIDIAN);
        p.fill(-1,1,4,1,1,4,Blocks.OBSIDIAN);
        p.put(0,2,4,LunarHorizons.DARK_CRYSTAL_ORE);p.chest(0,1,8,17,Direction.NORTH);
    }
    private void colony(Plan p,Random r){
        // Three wings and a central command hall connected by sealed corridors.
        chamber(p,-20,-16,-7,1,5);chamber(p,7,-16,20,1,5);
        chamber(p,-8,6,8,20,5);chamber(p,-6,-20,6,5,5);
        p.room(-7,-3,7,1,0,4,LunarHorizons.HABITAT_HULL,LunarHorizons.HABITAT_FLOOR);
        p.fill(-7,1,-2,-7,3,0,Blocks.AIR);p.fill(-6,1,-2,-6,3,0,Blocks.AIR);p.fill(6,1,-2,6,3,0,Blocks.AIR);p.fill(7,1,-2,7,3,0,Blocks.AIR);
        p.room(-2,1,2,6,0,4,LunarHorizons.HABITAT_HULL,LunarHorizons.HABITAT_FLOOR);
        p.fill(-1,1,6,1,3,6,Blocks.AIR);p.fill(-1,1,1,1,3,1,Blocks.AIR);p.door(0,1,-20);
        for(int x=-18;x<=-9;x+=3)p.bed(x,1,-12,Direction.SOUTH);
        p.chest(-19,1,-14,18,Direction.SOUTH);
        batteryRoof(p,17,-6,5,r);p.machine(16,1,-8,LunarHorizons.OXYGEN_GENERATOR,Direction.NORTH);
        p.machine(15,1,-8,LunarHorizons.HELIUM_GENERATOR,Direction.NORTH);
        p.chest(19,1,-14,18,Direction.SOUTH);
        p.machine(5,1,17,LunarHorizons.ADVANCED_ASSEMBLER,Direction.NORTH);
        p.machine(6,1,17,LunarHorizons.ELECTRIC_REFINERY,Direction.NORTH);
        p.chest(-6,1,18,18,Direction.NORTH);p.put(0,6,-3,LunarHorizons.SURVEY_BEACON);
        light(p,-13,4,-8);light(p,13,4,-8);light(p,0,4,-10);light(p,0,4,13);
    }
    private void spaceport(Plan p,Random r){
        chamber(p,-20,-20,20,-10,6);p.fill(-4,1,-20,4,4,-20,Blocks.AIR);p.fill(-4,1,-10,4,4,-10,Blocks.AIR);
        p.fill(-20,0,-9,20,0,20,LunarHorizons.HABITAT_FLOOR);
        for(int x:new int[]{-11,11}){
            p.fill(x-5,0,2,x+5,0,12,LunarHorizons.LAUNCH_PAD);
            for(int a:new int[]{-5,5})for(int b:new int[]{2,12})light(p,x+a,1,b);
        }
        p.fill(-2,1,15,2,11,19,LunarHorizons.TITANIUM_BLOCK);
        for(int h=1;h<=12;h++){p.put(3,h,17,LunarHorizons.TITANIUM_BLOCK);p.put(4,h,17,Blocks.LADDER.getDefaultState().with(Properties.HORIZONTAL_FACING,rotated(p.rot,Direction.EAST)));}
        p.room(-4,13,4,20,12,16,LunarHorizons.REINFORCED_HULL,LunarHorizons.HABITAT_FLOOR);
        p.put(0,17,17,LunarHorizons.SURVEY_BEACON);p.fill(4,13,16,4,14,18,Blocks.AIR);
        p.machine(-15,1,-12,LunarHorizons.FUEL_SYNTHESIZER,Direction.NORTH);
        p.machine(-14,1,-12,LunarHorizons.HELIUM_GENERATOR,Direction.NORTH);
        batteryRoof(p,16,-14,6,r);p.chest(18,1,-18,19,Direction.SOUTH);
        p.chest(-18,1,-18,19,Direction.SOUTH);
    }
    private void reactor(Plan p,Random r){
        chamber(p,-20,-18,20,20,12);
        // Control room and coolant gallery separated from the dormant reactor.
        p.room(-18,-16,18,-7,0,5,LunarHorizons.HABITAT_HULL,LunarHorizons.HABITAT_FLOOR);
        p.door(0,1,-7);
        p.chest(-16,1,-14,20,Direction.SOUTH);
        for(int x=-12;x<=12;x+=6)p.machine(x,1,-10,LunarHorizons.POWER_TERMINAL,Direction.NORTH);
        for(int x=-8;x<=8;x++)for(int z=-3;z<=13;z++){
            int d=x*x+(z-5)*(z-5);
            if(d<=64)p.put(x,1,z,LunarHorizons.TITANIUM_BLOCK);
            if(d<=64&&d>=36)for(int y=2;y<=9;y++)p.put(x,y,z,y%3==0?LunarHorizons.INDUSTRIAL_GRATE:LunarHorizons.REINFORCED_HULL);
            if(d<=20&&d>=9)p.put(x,2,z,LunarHorizons.CRYSTAL_STONE);
            if(d<=8)for(int y=2;y<=8;y++)p.put(x,y,z,LunarHorizons.CRYSTAL_STONE);
        }
        for(int side:new int[]{-1,1}){
            p.machine(side*13,1,5,LunarHorizons.INDUSTRIAL_BATTERY,Direction.NORTH);
            for(int z=1;z<=12;z++)p.put(side*12,1,z,LunarHorizons.POWER_CABLE);
            p.fill(side*10,3,-2,side*10,3,12,LunarHorizons.OXYGEN_PIPE);
            light(p,side*16,6,5);
        }
    }
    private void alienShip(Plan p,Random r){
        // Flattened elliptical hull, torn flank, interior ribs and segmented decks.
        for(int x=-21;x<=21;x++)for(int z=-9;z<=9;z++){
            double d=x*x/441.0+z*z/81.0;if(d>1)continue;
            p.put(x,0,z,Blocks.POLISHED_DEEPSLATE);
            for(int y=1;y<=8;y++){
                boolean shell=d>.78||y==8||y==1;
                if(x>4&&x<10&&z>4&&y>=2&&y<=6)shell=false;
                p.put(x,y,z,shell?(d>.9?Blocks.OBSIDIAN:Blocks.POLISHED_DEEPSLATE):Blocks.AIR);
            }
            if(x%7==0&&d<.65)for(int y=2;y<=7;y++)if(z!=0)p.put(x,y,z,Blocks.CHISELED_DEEPSLATE);
        }
        p.fill(5,1,4,9,6,9,Blocks.AIR);p.fill(5,1,4,9,1,8,Blocks.DEEPSLATE_TILES);
        for(int x:new int[]{-14,0,14}){
            p.put(x,2,0,LunarHorizons.CRYSTAL_STONE);
            p.put(x,3,0,LunarHorizons.DARK_CRYSTAL_ORE);
            p.put(x,6,0,Blocks.CRYING_OBSIDIAN);
        }
        p.chest(-15,2,2,21,Direction.SOUTH);p.chest(14,2,-2,21,Direction.NORTH);
    }

    private boolean geode(StructureWorldAccess w,Random r,int cx,int cz,int x,int z,int rot){
        for(int attempt=0;attempt<5;attempt++){
            int rx=5+r.nextInt(8),rz=Math.max(4,rx-2+r.nextInt(3)),ry=Math.max(4,(int)(rx*(.65+r.nextDouble()*.22)));
            int y=-39+r.nextInt(25);double phase=r.nextDouble()*Math.PI*2;
            if(y-ry-2<-60)continue;
            Plan p=new Plan(w,new BlockPos(x,y,z),rot,cx,cz);
            int solid=0,total=0;
            for(int a=-rx-1;a<=rx+1;a++)for(int b=-rz-1;b<=rz+1;b++)for(int h=-ry-1;h<=ry+1;h++){
                double e=Math.sqrt(a*a/(double)(rx*rx)+b*b/(double)(rz*rz)+h*h/(double)(ry*ry));
                double ripple=.045*Math.sin(a*.55+b*.31+phase)*Math.cos(h*.63-phase);
                e+=ripple;if(e>1)continue;
                if(e>.75){total++;if(rock(w,p.at(a,h,b)))solid++;}
                Block block=e>.87?LunarHorizons.MOON_ROCK:e>.72?LunarHorizons.ANORTHOSITE:e>.57?LunarHorizons.CRYSTAL_STONE:Blocks.AIR;
                if(e>.57&&e<.72&&r.nextInt(rx>=9?110:150)==0)block=LunarHorizons.DARK_CRYSTAL_ORE;
                p.put(a,h,b,block);
            }
            if(total==0||solid<total*.90)continue;
            // Natural crystal pillars, tapering to a cluster, anchored to the inner floor.
            for(int i=0;i<12;i++){
                int a=r.nextInt(2*rx-1)-rx+1,b=r.nextInt(2*rz-1)-rz+1;
                for(int h=-ry+1;h<ry-1;h++){
                    if(!p.state(p.at(a,h,b)).isAir()||p.state(p.at(a,h-1,b)).isAir())continue;
                    int tall=1+r.nextInt(3);
                    for(int k=0;k<tall&&p.state(p.at(a,h+k,b)).isAir()&&p.state(p.at(a,h+k+1,b)).isAir();k++)p.put(a,h+k,b,k==tall-1?LunarHorizons.CRYSTAL_CLUSTER:LunarHorizons.CRYSTAL_STONE);
                    break;
                }
            }
            if(p.commit(r))return true;
        }
        return false;
    }
    private static void loot(StructureWorldAccess w,BlockPos pos,Random r,int type){
        if(!(w.getBlockEntity(pos) instanceof ChestBlockEntity chest))return;
        if(type!=8)chest.setStack(0,ExpeditionJournal.create(type,r,pos));
        chest.setStack(4,new ItemStack(LunarHorizons.TITANIUM_INGOT,1+r.nextInt(4)));
        switch(type){
            case 0->{chest.setStack(8,new ItemStack(LunarHorizons.OXYGEN));chest.setStack(12,new ItemStack(LunarHorizons.LUNAR_ICE,2+r.nextInt(4)));chest.setStack(18,new ItemStack(Items.BREAD,2+r.nextInt(4)));}
            case 1->{chest.setStack(8,new ItemStack(LunarHorizons.CIRCUIT,1+r.nextInt(2)));chest.setStack(12,new ItemStack(LunarHorizons.POWER_CELL));}
            case 2->{chest.setStack(8,new ItemStack(LunarHorizons.METEORITE_FRAGMENT,1+r.nextInt(3)));chest.setStack(12,new ItemStack(LunarHorizons.CIRCUIT,1+r.nextInt(3)));chest.setStack(18,new ItemStack(LunarHorizons.FUEL));}
            case 3->{chest.setStack(8,new ItemStack(LunarHorizons.CRYSTAL,2+r.nextInt(4)));chest.setStack(12,new ItemStack(LunarHorizons.LUNAR_ICE,2+r.nextInt(3)));}
            case 4->{chest.setStack(8,new ItemStack(LunarHorizons.CIRCUIT,1+r.nextInt(3)));chest.setStack(12,new ItemStack(LunarHorizons.MACHINE_FRAME));}
            case 6->{chest.setStack(8,new ItemStack(LunarHorizons.OXYGEN));chest.setStack(12,new ItemStack(LunarHorizons.FUEL,1+r.nextInt(2)));chest.setStack(18,new ItemStack(LunarHorizons.POWER_CELL));}
            case 8->{chest.setStack(8,new ItemStack(LunarHorizons.OXYGEN));chest.setStack(12,new ItemStack(LunarHorizons.FUEL));}
            case 7->{chest.setStack(8,new ItemStack(LunarHorizons.CIRCUIT,1+r.nextInt(2)));chest.setStack(12,new ItemStack(LunarHorizons.POWER_CELL));chest.setStack(18,new ItemStack(LunarHorizons.BACKPACK));}
            case 9->{chest.setStack(8,new ItemStack(LunarHorizons.CIRCUIT,1+r.nextInt(4)));chest.setStack(12,new ItemStack(LunarHorizons.POWER_CELL,1+r.nextInt(4)));chest.setStack(18,new ItemStack(LunarHorizons.CRYSTAL,1+r.nextInt(4)));}
            case 10->{chest.setStack(8,new ItemStack(LunarHorizons.POWER_CELL,1+r.nextInt(4)));chest.setStack(12,new ItemStack(LunarHorizons.CIRCUIT,1+r.nextInt(4)));chest.setStack(18,new ItemStack(LunarHorizons.TITANIUM_INGOT,1+r.nextInt(4)));}
            case 11->{chest.setStack(8,new ItemStack(LunarHorizons.ROVER_ITEM));chest.setStack(12,new ItemStack(LunarHorizons.POWER_CELL,1+r.nextInt(4)));chest.setStack(18,new ItemStack(LunarHorizons.MACHINE_FRAME,1+r.nextInt(4)));}
            case 12->{chest.setStack(8,new ItemStack(LunarHorizons.HELIUM,1+r.nextInt(4)));chest.setStack(12,new ItemStack(LunarHorizons.FUEL,1+r.nextInt(4)));chest.setStack(18,new ItemStack(LunarHorizons.MACHINE_FRAME,1+r.nextInt(4)));}
            case 13->{chest.setStack(8,new ItemStack(LunarHorizons.RAW_TITANIUM,1+r.nextInt(4)));chest.setStack(12,new ItemStack(LunarHorizons.HELIUM,1+r.nextInt(4)));chest.setStack(18,new ItemStack(LunarHorizons.TITANIUM_DRILL));}
            case 14->{chest.setStack(8,new ItemStack(LunarHorizons.LARGE_OXYGEN));chest.setStack(12,new ItemStack(LunarHorizons.OXYGEN_FILTER,1+r.nextInt(4)));chest.setStack(18,new ItemStack(LunarHorizons.POWER_CELL,1+r.nextInt(4)));}
            case 15->{chest.setStack(8,new ItemStack(LunarHorizons.HELMET));chest.setStack(12,new ItemStack(LunarHorizons.LARGE_OXYGEN));chest.setStack(18,new ItemStack(LunarHorizons.BACKPACK));}
            case 16->{chest.setStack(8,new ItemStack(LunarHorizons.CIRCUIT,1+r.nextInt(4)));chest.setStack(12,new ItemStack(LunarHorizons.POWER_CELL,1+r.nextInt(4)));chest.setStack(18,new ItemStack(LunarHorizons.DARK_CRYSTAL,1+r.nextInt(2)));}
            case 17->{chest.setStack(8,new ItemStack(LunarHorizons.DARK_CRYSTAL,1+r.nextInt(2)));chest.setStack(12,new ItemStack(LunarHorizons.METEORITE_FRAGMENT,1+r.nextInt(4)));chest.setStack(18,new ItemStack(LunarHorizons.CRYSTAL,1+r.nextInt(4)));}
            case 18->{chest.setStack(8,new ItemStack(LunarHorizons.EXTREME_OXYGEN));chest.setStack(12,new ItemStack(LunarHorizons.CIRCUIT,1+r.nextInt(4)));chest.setStack(18,new ItemStack(LunarHorizons.MACHINE_FRAME,1+r.nextInt(4)));}
            case 19->{chest.setStack(8,new ItemStack(LunarHorizons.FUEL,1+r.nextInt(4)));chest.setStack(12,new ItemStack(LunarHorizons.POWER_CELL,1+r.nextInt(4)));chest.setStack(18,new ItemStack(LunarHorizons.CIRCUIT,1+r.nextInt(4)));}
            case 20->{chest.setStack(8,new ItemStack(LunarHorizons.DARK_CRYSTAL,1+r.nextInt(2)));chest.setStack(12,new ItemStack(LunarHorizons.HELIUM,1+r.nextInt(4)));chest.setStack(18,new ItemStack(LunarHorizons.POWER_CELL,1+r.nextInt(4)));}
            case 21->{chest.setStack(8,new ItemStack(LunarHorizons.DARK_CRYSTAL,1+r.nextInt(2)));chest.setStack(12,new ItemStack(LunarHorizons.METEORITE_FRAGMENT,1+r.nextInt(4)));chest.setStack(18,new ItemStack(LunarHorizons.CRYSTAL,1+r.nextInt(4)));}
            case 5->{chest.setStack(8,new ItemStack(LunarHorizons.DARK_CRYSTAL,1+r.nextInt(2)));chest.setStack(12,new ItemStack(LunarHorizons.HELIUM,1+r.nextInt(3)));chest.setStack(18,new ItemStack(LunarHorizons.LARGE_OXYGEN));}
        }
        chest.markDirty();
    }
}

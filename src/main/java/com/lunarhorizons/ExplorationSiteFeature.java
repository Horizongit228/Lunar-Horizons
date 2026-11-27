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
            default->false;
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
            case 5->{chest.setStack(8,new ItemStack(LunarHorizons.DARK_CRYSTAL,1+r.nextInt(2)));chest.setStack(12,new ItemStack(LunarHorizons.HELIUM,1+r.nextInt(3)));chest.setStack(18,new ItemStack(LunarHorizons.LARGE_OXYGEN));}
        }
        chest.markDirty();
    }
}

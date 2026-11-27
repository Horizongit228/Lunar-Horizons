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

/** Deterministic expedition layout planner. The legacy feature IDs are retained for save compatibility. */
public final class ExplorationSiteFeature extends Feature<DefaultFeatureConfig>{
    private final int kind;
    public ExplorationSiteFeature(Codec<DefaultFeatureConfig> codec,int kind){super(codec);this.kind=kind;}
    private static BlockState air(){return Blocks.AIR.getDefaultState();}
    private static boolean rock(Terrain w,BlockPos p){
        var s=w.getBlockState(p);
        return s.isOf(LunarHorizons.MOON_ROCK)||s.isOf(LunarHorizons.REGOLITH)||s.isOf(LunarHorizons.HIGHLAND_ROCK)||s.isOf(LunarHorizons.FROSTED_REGOLITH)||s.isOf(LunarHorizons.DARK_REGOLITH)||s.isOf(LunarHorizons.BASALT_DUST)||s.isOf(LunarHorizons.ANORTHOSITE)||s.isOf(LunarHorizons.EJECTA_ROCK)||s.isOf(LunarHorizons.CRYSTAL_ORE)||s.isOf(LunarHorizons.DARK_CRYSTAL_ORE)||s.isOf(LunarHorizons.TITANIUM_ORE)||s.isOf(LunarHorizons.HELIUM_ORE)||s.isOf(LunarHorizons.LUNAR_ICE);
    }
    private static int top(Terrain w,int x,int z){return w.top(x,z);}
    private static BlockPos rotated(BlockPos origin,int rot,int x,int y,int z){
        return switch(rot){case 1->origin.add(-z,y,x);case 2->origin.add(-x,y,-z);case 3->origin.add(z,y,-x);default->origin.add(x,y,z);};
    }
    private static Direction rotated(int rot,Direction d){for(int i=0;i<rot;i++)d=d.rotateYClockwise();return d;}
    static final class Plan{
        final Terrain w;final BlockPos origin;final int rot,cx,cz;
        final Map<BlockPos,BlockState> blocks=new LinkedHashMap<>();
        final Map<BlockPos,Integer> chests=new LinkedHashMap<>();
        Plan(Terrain w,BlockPos origin,int rot,int cx,int cz){this.w=w;this.origin=origin;this.rot=rot;this.cx=cx;this.cz=cz;}
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
                if(Math.abs(p.getX()-origin.getX())>63||Math.abs(p.getZ()-origin.getZ())>63||p.getY()<-60||p.getY()>185)return false;
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
                else if(s.getBlock() instanceof ColonyMachineBlock m)s=s.with(Properties.UP,m.topConnection(state(p.up())));
                e.setValue(s);
            }
            chests.entrySet().removeIf(e->!blocks.getOrDefault(e.getKey(),air()).isOf(Blocks.CHEST));
            w.result=this;
            return true;
        }
    }
    /** Coarse noise-height sampler for planning only; actual supports are reconciled during placement. */
    static final class Terrain{
        final net.minecraft.world.gen.structure.Structure.Context context;
        final Map<Long,Integer> samples=new HashMap<>(),heights=new HashMap<>();
        Plan result;
        Terrain(net.minecraft.world.gen.structure.Structure.Context context){this.context=context;}
        int sample(int x,int z){
            long key=((long)x<<32)^(z&0xffffffffL);
            return samples.computeIfAbsent(key,k->context.chunkGenerator().getHeightOnGround(x,z,Heightmap.Type.WORLD_SURFACE_WG,context.world(),context.noiseConfig())+ImpactCraterFeature.estimateDelta(context.seed(),x,z));
        }
        int top(int x,int z){
            long key=((long)x<<32)^(z&0xffffffffL);
            Integer cached=heights.get(key);if(cached!=null)return cached;
            int a=Math.floorDiv(x,4)*4,b=Math.floorDiv(z,4)*4;
            double u=(x-a)/4.0,v=(z-b)/4.0;
            int h=(int)Math.round((1-u)*(1-v)*sample(a,b)+u*(1-v)*sample(a+4,b)+(1-u)*v*sample(a,b+4)+u*v*sample(a+4,b+4));
            heights.put(key,h);return h;
        }
        BlockState getBlockState(BlockPos p){return p.getY()<top(p.getX(),p.getZ())?LunarHorizons.MOON_ROCK.getDefaultState():air();}
        boolean isAir(BlockPos p){return getBlockState(p).isAir();}
    }
    @Override public boolean generate(FeatureContext<DefaultFeatureConfig> ctx){
        // Old placed_feature resources are intentionally inert; the structure registry owns placement.
        return false;
    }
    static Plan prepare(int kind,net.minecraft.world.gen.structure.Structure.Context context){
        Terrain terrain=new Terrain(context);
        ExplorationSiteFeature planner=new ExplorationSiteFeature(DefaultFeatureConfig.CODEC,kind);
        int cx=context.chunkPos().getStartX(),cz=context.chunkPos().getStartZ();
        Random random=context.random();
        planner.layout(terrain,random,cx,cz,cx+7,cz+7,random.nextInt(4));
        return terrain.result;
    }
    private boolean layout(Terrain w,Random r,int cx,int cz,int x,int z,int rot){
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
    private static int surface(Terrain w,int x,int z,int radius,int slope){
        int hi=-1000,lo=1000;
        for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            int y=top(w,x+dx,z+dz);BlockPos p=new BlockPos(x+dx,y-1,z+dz);
            if(!rock(w,p)||!rock(w,p.down()))return -1000;
            hi=Math.max(hi,y);lo=Math.min(lo,y);
        }
        return hi-lo<=slope&&hi>=8&&hi<=145?hi:-1000;
    }
    private boolean outpost(Terrain w,Random r,int cx,int cz,int x,int z,int rot){
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
    private boolean satellite(Terrain w,Random r,int cx,int cz,int x,int z,int rot){
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
    private boolean laboratory(Terrain w,Random r,int cx,int cz,int x,int z,int rot){
        BlockPos shaft=rotated(new BlockPos(x,0,z),rot,0,0,-6);int surface=top(w,shaft.getX(),shaft.getZ());
        if(surface<12||surface>145)return false;
        for(int attempt=0;attempt<5;attempt++){
            int y=Math.max(-48,surface-23-attempt*7);
            Plan p=new Plan(w,new BlockPos(x,y,z),rot,cx,cz);
            Block wall=LunarHorizons.REINFORCED_HULL,deck=LunarHorizons.HABITAT_FLOOR;
            // Require a solid natural cover above the entire building before excavating.
            int solid=0,total=0;
            for(int a=-15;a<=15;a++)for(int b=-11;b<=11;b++){total++;if(rock(w,p.at(a,11,b))&&rock(w,p.at(a,12,b)))solid++;}
            if(solid<total*.80)continue;
            p.room(-15,-11,15,11,0,5,wall,deck);p.room(-15,-11,15,11,5,10,wall,deck);
            for(int floor:new int[]{0,5}){
                p.fill(0,floor+1,-11,0,floor+4,11,wall);
                p.door(0,floor+1,0,Direction.WEST);
                p.fill(-14,floor+1,7,-1,floor+4,7,wall);p.door(-7,floor+1,7);
                p.fill(1,floor+1,7,14,floor+4,7,wall);p.door(7,floor+1,7);
                p.put(-13,floor+3,9,LunarHorizons.LUNAR_LANTERN);p.put(13,floor+3,9,LunarHorizons.LUNAR_LANTERN);
                p.put(-9,floor+3,-5,LunarHorizons.LUNAR_LANTERN);p.put(9,floor+3,5,LunarHorizons.LUNAR_LANTERN);
            }
            // Upper floor: sample archive and instrument room.
            for(int a=-9;a<=-3;a+=2){p.put(a,6,5,LunarHorizons.INDUSTRIAL_GRATE);p.put(a,7,5,LunarHorizons.PRESSURE_GLASS);}
            p.chest(-10,6,-5,3,Direction.SOUTH);p.chest(10,6,-5,4,Direction.SOUTH);
            p.chest(-13,6,9,3,Direction.NORTH);p.machine(12,6,9,LunarHorizons.ADVANCED_ASSEMBLER,Direction.NORTH);
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
            p.chest(10,1,6,5,Direction.NORTH);p.machine(-12,1,9,LunarHorizons.ELECTRIC_REFILL,Direction.NORTH);p.put(9,3,6,LunarHorizons.LUNAR_LANTERN);
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
            for(int a=-15;a<=15&&supported;a++)for(int b=-11;b<=11;b++)if(!p.supported(a,0,b,LunarHorizons.MOON_BRICKS,6)){supported=false;break;}
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
    private boolean lander(Terrain w,Random r,int cx,int cz,int x,int z,int rot){
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
    private boolean relay(Terrain w,Random r,int cx,int cz,int x,int z,int rot){
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
    // Native starts support large layouts. Pieces are subsequently divided at chunk boundaries.
    private boolean expandedSite(Terrain w,Random r,int cx,int cz,int x,int z,int rot){
        int radius=switch(kind){case 6->17;case 7->21;case 8->12;case 9,10,11,13,14->17;case 12->12;case 15->44;case 16->36;case 17->30;case 18->55;default->21;};
        boolean buried=kind==10||kind==11||kind==12||kind==13||kind==14||kind==17;
        int y;
        if(buried){
            int surface=top(w,x,z);
            y=Math.max(-48,surface-(kind==17?46:kind==10?34:27)-r.nextInt(8));
            if(surface-y<(kind==17?26:18))return false;
        }else{
            // Sample the height field, then validate every actual foundation cell.
            int hi=-1000,lo=1000;
            for(int a=-radius;a<=radius;a+=4)for(int b=-radius;b<=radius;b+=4){
                int h=top(w,x+a,z+b);
                hi=Math.max(hi,h);lo=Math.min(lo,h);
            }
            if(hi-lo>(kind>=15?30:14)||hi<8||hi>145)return false;
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
            if(buried&&q.getY()==y+(kind==17?18:kind==14?8:5)&&!e.getValue().isAir()){
                cover++;
                if(rock(w,q.up(5))&&rock(w,q.up(6)))covered++;
            }
        }
        if(buried&&(cover==0||covered<cover*.65))return false;
        for(BlockPos q:floors){
            boolean support=false;
            for(int h=1;h<=32;h++){
                BlockPos below=q.down(h);
                if(rock(w,below)){support=true;break;}
                if(!w.isAir(below))return false;
                p.blocks.put(below,LunarHorizons.MOON_BRICKS.getDefaultState());
            }
            if(!support)return false;
        }
        if(buried&&!accessShaft(p,0,kind==17?-24:kind==12?-4:-5))return false;
        if(kind==8){if(!garageApproach(p))return false;}
        else if(!buried&&kind!=18&&!surfaceApproach(p,0,switch(kind){case 6->-17;case 7->-21;case 9->-16;case 15->-43;case 16->-35;default->-21;}))return false;
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
            int z=-12-step;BlockPos at=p.at(x,0,z);
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
        if(delta < -3||delta>32)return false;
        if(delta<0){for(int h=1;h<=3;h++)p.put(x,h,z,Blocks.AIR);delta=0;}
        // A ladder on a solid column avoids long stairs crossing the feature boundary.
        for(int h=-delta;h<=0;h++){
            p.put(x,h,z,LunarHorizons.HABITAT_FLOOR);
            p.put(x,h,z-1,Blocks.LADDER.getDefaultState().with(Properties.HORIZONTAL_FACING,rotated(p.rot,Direction.NORTH)));
        }
        return p.supported(x,-delta,z,LunarHorizons.MOON_BRICKS,32);
    }
    private boolean accessShaft(Plan p,int x,int z){
        BlockPos q=p.at(x,0,z);
        int surface=top(p.w,q.getX(),q.getZ()),h=surface-p.origin.getY();
        if(h<12||h>110)return false;
        for(int y=1;y<=h+1;y++){
            p.put(x+1,y,z,LunarHorizons.REINFORCED_HULL);
            p.put(x,y,z,Blocks.LADDER.getDefaultState().with(Properties.HORIZONTAL_FACING,rotated(p.rot,Direction.WEST)));
            if(y>=6&&y<h){
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
        BlockPos approach=p.at(x,h,z-3);
        int delta=h-(top(p.w,approach.getX(),approach.getZ())-p.origin.getY());
        if(delta>5||delta< -3)return false;
        for(int step=0;step<=Math.max(0,delta);step++){
            p.put(x,h-step,z-3-step,LunarHorizons.HABITAT_FLOOR);
            if(!p.supported(x,h-step,z-3-step,LunarHorizons.MOON_BRICKS,10))return false;
        }
        if(delta<0)for(int a=1;a<=3;a++)p.put(x,h+a,z-3,Blocks.AIR);
        return true;
    }
    private void observatory(Plan p,Random r){
        chamber(p,-15,-16,15,2,5);p.door(0,1,-16);
        window(p,-5,-16);window(p,5,-16);
        p.fill(0,1,-8,0,3,-1,LunarHorizons.HABITAT_HULL);p.door(0,1,-4,Direction.WEST);
        batteryRoof(p,12,-6,5,r);
        p.machine(-7,1,-6,LunarHorizons.POWER_TERMINAL,Direction.SOUTH);
        for(int x=-8;x<=-2;x+=2)p.put(x,1,0,LunarHorizons.ADVANCED_ASSEMBLER);
        p.chest(-9,1,-8,9,Direction.SOUTH);light(p,-5,3,-3);
        // Parabolic bowl: rising circular rim, a receiver on a central mast.
        p.fill(-3,0,5,3,0,11,LunarHorizons.TITANIUM_BLOCK);
        p.fill(-1,1,7,1,7,9,LunarHorizons.TITANIUM_BLOCK);
        for(int x=-14;x<=14;x++)for(int z=-8;z<=8;z++){
            double d=x*x/196.0+z*z/64.0;if(d>1)continue;
            int y=8+(int)Math.round(d*6);
            p.put(x,y,8+z,LunarHorizons.TITANIUM_BLOCK);
        }
        p.fill(0,9,8,0,15,8,Blocks.IRON_BARS);p.put(0,16,8,LunarHorizons.SURVEY_BEACON);
    }
    private void solarFarm(Plan p,Random r){
        chamber(p,-6,-20,6,-14,4);p.door(0,1,-20);
        p.chest(-5,1,-18,10,Direction.SOUTH);light(p,-2,3,-17);
        p.machine(1,1,-16,LunarHorizons.INDUSTRIAL_BATTERY,Direction.NORTH);
        p.machine(2,1,-16,LunarHorizons.POWER_TERMINAL,Direction.NORTH);
        p.put(1,1,-15,LunarHorizons.POWER_CABLE);p.put(1,1,-14,LunarHorizons.POWER_FEEDTHROUGH);
        p.fill(-20,0,-13,20,0,20,LunarHorizons.INDUSTRIAL_GRATE);
        for(int z=-13;z<=19;z++)p.put(1,1,z,LunarHorizons.POWER_CABLE);
        for(int z=-13;z<=19;z+=4)for(int x=-19;x<=19;x++){
            if(x==1)continue;
            p.put(x,1,z,r.nextInt(8)==0?LunarHorizons.DAMAGED_SOLAR_PANEL:LunarHorizons.SOLAR_PANEL);
            p.put(x,1,z+1,r.nextInt(10)==0?LunarHorizons.DAMAGED_SOLAR_PANEL:LunarHorizons.SOLAR_PANEL);
        }
    }
    private void garage(Plan p,Random r){
        chamber(p,-11,-11,11,11,6);p.fill(-3,1,-11,3,4,-11,Blocks.AIR);
        for(int x:new int[]{-5,5}){
            p.fill(x-2,0,-5,x+2,0,5,LunarHorizons.INDUSTRIAL_GRATE);
            p.machine(x,1,6,LunarHorizons.INDUSTRIAL_BATTERY,Direction.NORTH);
            p.put(x,1,5,LunarHorizons.POWER_CABLE);light(p,x,4,0);
        }
        batteryRoof(p,9,8,6,r);
        p.fill(6,1,6,6,1,7,LunarHorizons.POWER_WIRE);
        p.chest(-7,1,7,11,Direction.NORTH);
        p.machine(-3,1,7,LunarHorizons.ELECTRIC_REFINERY,Direction.NORTH);
        // Static maintenance stand with wheel assemblies; a rover item is in the locker.
        p.fill(-5,1,-1,-3,1,1,LunarHorizons.TITANIUM_BLOCK);
        for(int x:new int[]{-6,-2})for(int z:new int[]{-1,1})p.put(x,1,z,Blocks.BLACK_CONCRETE);
    }
    private void heliumSite(Plan p,Random r){
        chamber(p,-15,-15,15,0,6);p.door(0,1,-15);
        batteryRoof(p,8,-3,6,r);
        for(int x:new int[]{-12,-7,-3,3,12}){
            p.machine(x,1,-2,LunarHorizons.FUEL_SYNTHESIZER,Direction.NORTH);
            p.put(x,1,-1,LunarHorizons.POWER_CABLE);
            p.put(x,2,-2,LunarHorizons.INDUSTRIAL_GRATE);light(p,x,4,-4);
            p.fill(x-1,0,3,x+1,0,8,LunarHorizons.TITANIUM_BLOCK);
            p.fill(x,1,5,x,8,5,LunarHorizons.TITANIUM_BLOCK);
            p.fill(x-1,8,5,x+1,8,5,LunarHorizons.REINFORCED_HULL);
            p.put(x,7,4,Blocks.LIGHTNING_ROD);
        }
        p.fill(-12,1,-1,12,1,-1,LunarHorizons.POWER_CABLE);
        p.put(8,1,-2,LunarHorizons.POWER_CABLE);
        p.chest(-9,1,-8,12,Direction.SOUTH);
        p.machine(7,1,-8,LunarHorizons.HELIUM_GENERATOR,Direction.SOUTH);for(int z=-7;z<=-4;z++)p.put(7,1,z,LunarHorizons.POWER_WIRE);p.put(7,1,-3,LunarHorizons.POWER_TERMINAL);
    }
    private void mine(Plan p,Random r){
        chamber(p,-15,-14,15,5,5);
        p.room(-2,4,2,15,0,4,LunarHorizons.MOON_BRICKS,LunarHorizons.HABITAT_FLOOR);
        p.room(-14,8,-2,14,0,4,LunarHorizons.MOON_BRICKS,LunarHorizons.HABITAT_FLOOR);
        p.room(2,8,14,14,0,4,LunarHorizons.MOON_BRICKS,LunarHorizons.HABITAT_FLOOR);
        p.fill(-2,1,10,-2,3,12,Blocks.AIR);p.fill(2,1,10,2,3,12,Blocks.AIR);
        p.fill(-1,1,5,1,3,5,Blocks.AIR);
        for(int x=-8;x<=8;x++)p.put(x,1,1,Blocks.RAIL.getDefaultState().with(Properties.RAIL_SHAPE,(p.rot%2==0?net.minecraft.block.enums.RailShape.EAST_WEST:net.minecraft.block.enums.RailShape.NORTH_SOUTH)));
        for(int x:new int[]{-8,-4,4,8}){p.fill(x,1,4,x,4,4,LunarHorizons.TITANIUM_BLOCK);light(p,x,4,0);}
        for(int x=-12;x<=12;x+=4)p.put(x,2,14,r.nextBoolean()?LunarHorizons.TITANIUM_ORE:LunarHorizons.HELIUM_ORE);
        p.chest(-9,1,-5,13,Direction.SOUTH);p.machine(8,1,-5,LunarHorizons.ELECTRIC_REFINERY,Direction.SOUTH);
    }
    private void iceSite(Plan p,Random r){
        chamber(p,-14,-12,14,12,5);
        for(int x:new int[]{-11,-5,5,11}){
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
        chamber(p,-11,-11,11,13,5);
        p.fill(-11,1,0,11,4,0,LunarHorizons.REINFORCED_HULL);p.door(0,1,0);
        for(int x=-8;x<=8;x+=4){p.bed(x,1,5,Direction.SOUTH);p.bed(x,1,9,Direction.SOUTH);}
        p.chest(-7,1,-4,15,Direction.SOUTH);p.chest(7,1,6,15,Direction.NORTH);
        p.machine(5,1,-4,LunarHorizons.OXYGEN_GENERATOR,Direction.SOUTH);
        p.machine(4,1,-4,LunarHorizons.BATTERY,Direction.SOUTH);
        p.machine(3,1,-4,LunarHorizons.HELIUM_GENERATOR,Direction.SOUTH);
        light(p,-5,4,-3);light(p,5,4,4);
    }
    private void dataCenter(Plan p,Random r){
        chamber(p,-15,-14,15,14,5);
        for(int x:new int[]{-12,-8,-4,4,8,12})for(int z=-4;z<=10;z+=2){
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
        p.room(-17,-16,17,16,0,8,Blocks.POLISHED_DEEPSLATE,Blocks.DEEPSLATE_TILES);
        p.fill(-2,1,-16,2,3,-16,Blocks.AIR);
        for(int x:new int[]{-13,-7,7,13})for(int z:new int[]{-10,-3,3,10}){
            p.fill(x,1,z,x,5,z,Blocks.CHISELED_DEEPSLATE);
            p.put(x,5,z,LunarHorizons.CRYSTAL_STONE);
        }
        for(int x=-6;x<=6;x++)for(int z=2;z<=6;z++)if(Math.abs(x)==6||z==2||z==6)p.put(x,1,z,Blocks.CRYING_OBSIDIAN);
        p.fill(-1,1,4,1,1,4,Blocks.OBSIDIAN);
        p.put(0,2,4,LunarHorizons.DARK_CRYSTAL_ORE);p.chest(0,1,8,17,Direction.NORTH);
    }
    // Expedition city: 89 x 85 blocks, six distinct buildings plus transit spine.
    private void colony(Plan p,Random r){
        Block wall=LunarHorizons.HABITAT_HULL,deck=LunarHorizons.HABITAT_FLOOR;
        chamber(p,-12,-42,12,42,6);
        for(int side:new int[]{-1,1}){
            int min=side<0?-44:22,max=side<0?-22:44;
            for(int z:new int[]{-28,0,28}){
                chamber(p,min,z-11,max,z+11,6);
                int inner=side<0?-22:22;
                int a=Math.min(inner,side*12),b=Math.max(inner,side*12);
                p.room(a,z-2,b,z+2,0,4,wall,deck);
                for(int edge:new int[]{inner,side*12})p.fill(edge,1,z-1,edge,3,z+1,Blocks.AIR);
                int center=(min+max)/2;
                for(int xx=min+2;xx<=max-2;xx+=5){
                    p.put(xx,3,z-11,LunarHorizons.PRESSURE_GLASS);p.put(xx,3,z+11,LunarHorizons.PRESSURE_GLASS);
                }
                light(p,center,5,z-5);light(p,center,5,z+5);
                int tx=side<0?min+2:max-2;
                if(z==-28&&side<0){
                    for(int xx=min+3;xx<=max-3;xx+=4){p.bed(xx,1,z-7,Direction.SOUTH);p.bed(xx,1,z+6,Direction.SOUTH);}
                    p.machine(tx,1,z+8,LunarHorizons.OXYGEN_GENERATOR,Direction.NORTH);
                }else if(z==-28){
                    batteryRoof(p,max-4,z+5,6,r);
                    p.machine(max-5,1,z+3,LunarHorizons.HELIUM_GENERATOR,Direction.NORTH);
                    p.machine(max-6,1,z+3,LunarHorizons.OXYGEN_GENERATOR,Direction.NORTH);
                    p.put(max-5,1,z+4,LunarHorizons.POWER_WIRE);p.put(max-4,1,z+4,LunarHorizons.POWER_WIRE);
                }else if(z==0&&side<0){
                    for(int xx=min+3;xx<=max-3;xx+=4)p.machine(xx,1,z+8,LunarHorizons.INDUSTRIAL_BATTERY,Direction.NORTH);
                    p.fill(min+3,1,z+7,max-3,1,z+7,LunarHorizons.POWER_CABLE);
                    p.machine(tx,1,z-8,LunarHorizons.POWER_TERMINAL,Direction.SOUTH);
                }else if(z==0){
                    for(int xx=min+3;xx<=max-3;xx+=4){
                        p.machine(xx,1,z+8,LunarHorizons.ELECTRIC_REFINERY,Direction.NORTH);
                        p.machine(xx,1,z-8,LunarHorizons.ADVANCED_ASSEMBLER,Direction.SOUTH);
                    }
                }else if(side<0){
                    for(int xx=min+3;xx<=max-3;xx+=4){p.put(xx,1,z+5,LunarHorizons.INDUSTRIAL_GRATE);p.put(xx,2,z+5,LunarHorizons.PRESSURE_GLASS);p.put(xx,3,z+5,LunarHorizons.CRYSTAL_STONE);}
                    p.machine(tx,1,z-8,LunarHorizons.ADVANCED_ASSEMBLER,Direction.SOUTH);
                }else{
                    p.fill(min+3,0,z-8,max-3,0,z+8,LunarHorizons.INDUSTRIAL_GRATE);
                    p.machine(tx,1,z+8,LunarHorizons.INDUSTRIAL_BATTERY,Direction.NORTH);
                    p.machine(tx-1,1,z+8,LunarHorizons.ELECTRIC_REFILL,Direction.NORTH);
                }
                p.chest(tx,1,z-9,18,Direction.SOUTH);
                p.fill(min+1,1,z+2,max-1,5,z+2,wall);p.door(center,1,z+2);
            }
        }
        // Subdivide the central hall into separately pressurizable sections.
        for(int z:new int[]{-21,-7,7,21}){p.fill(-11,1,z,11,5,z,wall);p.door(0,1,z);p.put(10,1,z,LunarHorizons.POWER_FEEDTHROUGH);}
        // Open transit corridor with partition airlocks and windows at the ends.
        p.door(0,1,-42);p.door(0,1,-39);p.fill(-1,1,-39,-1,2,-39,wall);p.fill(1,1,-39,1,2,-39,wall);
        for(int z=-35;z<=35;z+=7){light(p,0,5,z);p.put(-12,3,z,LunarHorizons.PRESSURE_GLASS);p.put(12,3,z,LunarHorizons.PRESSURE_GLASS);}
        for(int z=-35;z<=35;z++)if(z!=0&&z!=-21&&z!=-7&&z!=7&&z!=21)p.put(10,1,z,LunarHorizons.POWER_CABLE);
        for(int z:new int[]{-28,0,28})for(int x=11;x<=21;x++)p.put(x,1,z,LunarHorizons.POWER_WIRE);
        p.machine(8,1,-32,LunarHorizons.POWER_TERMINAL,Direction.WEST);
        p.machine(8,1,-31,LunarHorizons.OXYGEN_GENERATOR,Direction.WEST);
        p.put(9,1,-31,LunarHorizons.POWER_WIRE);p.chest(-9,1,-36,18,Direction.SOUTH);
        p.put(0,7,0,LunarHorizons.SURVEY_BEACON);
        colonyDetail(p,r);
    }

    // 71 x 71 spaceport: two 17 x 17 launch platforms, two actual hangars, service hub.
    private void spaceport(Plan p,Random r){
        Block deck=LunarHorizons.HABITAT_FLOOR,metal=LunarHorizons.TITANIUM_BLOCK;
        p.fill(-35,0,-34,35,0,35,deck);
        for(int side:new int[]{-1,1}){
            int center=side*20;
            chamber(p,center-14,-34,center+14,-14,16);
            p.fill(center-6,1,-14,center+6,13,-14,Blocks.AIR);
            p.fill(center-5,1,-34,center+5,13,-34,Blocks.AIR);
            for(int x=center-11;x<=center+11;x+=6){light(p,x,15,-25);p.put(x,3,-34,LunarHorizons.PRESSURE_GLASS);}
            p.machine(center-10,1,-29,LunarHorizons.FUEL_SYNTHESIZER,Direction.SOUTH);
            p.machine(center-9,1,-29,LunarHorizons.HELIUM_GENERATOR,Direction.SOUTH);
            batteryRoof(p,center+10,-29,16,r);
            p.chest(center-12,1,-32,19,Direction.SOUTH);
            p.fill(center-8,0,5,center+8,0,21,LunarHorizons.LAUNCH_PAD);
            for(int x:new int[]{center-8,center+8})for(int z:new int[]{5,21}){
                light(p,x,1,z);p.put(x,2,z,Blocks.LIGHTNING_ROD);
            }
            // Fuel line from hangar to the pad, with a service conduit alongside the runway.
            for(int z=-13;z<=5;z++)p.put(center-10,1,z,LunarHorizons.POWER_CABLE);
            for(int h=1;h<=8;h++)p.put(center+10,h,24,metal);
            p.fill(center+7,8,24,center+13,8,24,metal);
            for(int h=1;h<=7;h++)p.put(center+9,h,24,LunarHorizons.INDUSTRIAL_GRATE);
        }
        chamber(p,-5,-34,5,-17,5);p.door(0,1,-34);p.door(0,1,-17);
        p.machine(3,1,-29,LunarHorizons.POWER_TERMINAL,Direction.WEST);p.chest(-3,1,-30,19,Direction.SOUTH);
        for(int z=-13;z<=29;z++)for(int x=-2;x<=2;x++)p.put(x,0,z,LunarHorizons.INDUSTRIAL_GRATE);
        // Control tower with stairs/ladder access and panoramic windows.
        p.fill(-3,1,26,3,14,32,metal);
        p.room(-7,23,7,35,15,20,LunarHorizons.REINFORCED_HULL,deck);
        for(int x=-6;x<=6;x++){p.put(x,17,23,LunarHorizons.PRESSURE_GLASS);p.put(x,17,35,LunarHorizons.PRESSURE_GLASS);}
        for(int z=24;z<=34;z++){p.put(-7,17,z,LunarHorizons.PRESSURE_GLASS);p.put(7,17,z,LunarHorizons.PRESSURE_GLASS);}
        for(int h=1;h<=16;h++){
            p.put(3,h,29,metal);p.put(4,h,29,Blocks.LADDER.getDefaultState().with(Properties.HORIZONTAL_FACING,rotated(p.rot,Direction.EAST)));
        }
        p.put(4,17,29,Blocks.AIR);p.put(4,15,29,Blocks.LADDER.getDefaultState().with(Properties.HORIZONTAL_FACING,rotated(p.rot,Direction.EAST)));p.machine(0,16,30,LunarHorizons.POWER_TERMINAL,Direction.NORTH);
        p.put(0,21,29,LunarHorizons.SURVEY_BEACON);p.fill(0,22,29,0,25,29,Blocks.IRON_BARS);
        spaceportDetail(p,r);
    }

    // Add interior fittings only to planned air. Never replace machines, loot, doors or a structural shell.
    private void furnishing(Plan p,int x,int y,int z,Block block){
        BlockState s=p.blocks.get(p.at(x,y,z));
        if(s!=null&&s.isAir())p.put(x,y,z,block);
    }
    private void colonyDetail(Plan p,Random r){
        Block armor=LunarHorizons.REINFORCED_HULL, floor=LunarHorizons.HABITAT_FLOOR;
        // A continuous layered spine roof with skylights above the circulation lane.
        for(int z=-40;z<=40;z++){
            for(int x:new int[]{-12,12})p.put(x,7,z,armor);
            if(z%7==0){
                for(int x=-11;x<=11;x++)if(Math.abs(x)>2)p.put(x,7,z,LunarHorizons.TITANIUM_BLOCK);
                for(int x=-2;x<=2;x++)if(z!=0&&z!=-21&&z!=-7&&z!=7&&z!=21)p.put(x,6,z,LunarHorizons.PRESSURE_GLASS);
            }
            if(z%4==0){p.put(-2,0,z,LunarHorizons.INDUSTRIAL_GRATE);p.put(2,0,z,LunarHorizons.INDUSTRIAL_GRATE);}
        }
        // Six wings: individually recognizable roof silhouettes and furnished rooms.
        for(int side:new int[]{-1,1})for(int z:new int[]{-28,0,28}){
            int min=side<0?-44:22,max=side<0?-22:44,center=(min+max)/2;
            for(int x=min;x<=max;x++){
                p.put(x,7,z-11,armor);p.put(x,7,z+11,armor);
                if((x-min)%5==0)for(int dz=-10;dz<=10;dz++)p.put(x,7,z+dz,LunarHorizons.TITANIUM_BLOCK);
            }
            for(int dz=-10;dz<=10;dz++){
                p.put(min,7,z+dz,armor);p.put(max,7,z+dz,armor);
            }
            // Two-height windows with external reinforcement ribs.
            for(int xx=min+2;xx<=max-2;xx+=5)for(int zz:new int[]{z-11,z+11}){
                p.put(xx,2,zz,LunarHorizons.PRESSURE_GLASS);p.put(xx,3,zz,LunarHorizons.PRESSURE_GLASS);
            }
            for(int dz=-9;dz<=9;dz+=6)for(int x:new int[]{min,max}){
                p.put(x,2,z+dz,LunarHorizons.PRESSURE_GLASS);p.put(x,3,z+dz,LunarHorizons.PRESSURE_GLASS);
            }
            // Lit floor strips and ceiling lamps leave the center circulation aisle clear.
            for(int dz=-9;dz<=9;dz++){
                if(dz!=2)p.put(center,0,z+dz,LunarHorizons.INDUSTRIAL_GRATE);
                if(dz%4==0)furnishing(p,center,5,z+dz,LunarHorizons.LUNAR_LANTERN);
            }
            if(z==-28&&side<0){
                // Crew quarters: lockers, bedside storage and mess tables.
                for(int xx=min+3;xx<=max-3;xx+=4){
                    furnishing(p,xx+1,1,z-8,Blocks.BARREL);furnishing(p,xx+1,1,z+7,Blocks.BARREL);
                    furnishing(p,xx,1,z-3,Blocks.SMOOTH_STONE_SLAB);furnishing(p,xx,1,z+4,Blocks.SMOOTH_STONE_SLAB);
                }
                for(int xx=min+4;xx<=max-4;xx+=5)furnishing(p,xx,1,z-1,Blocks.OAK_STAIRS);
            }else if(z==-28){
                // Power wing: cooling equipment and service lockers, attached PV bank.
                for(int xx=min+3;xx<=max-3;xx+=4){
                    furnishing(p,xx,1,z-7,LunarHorizons.INDUSTRIAL_BATTERY);
                    furnishing(p,xx,2,z-7,LunarHorizons.POWER_CABLE);
                }
                // Single exposed array at roof level, connected to the existing power feedthrough.
                for(int xx=min+3;xx<=max-3;xx++)p.put(xx,7,z+5,LunarHorizons.ADVANCED_SOLAR_PANEL);
            }else if(z==0&&side<0){
                for(int xx=min+3;xx<=max-3;xx+=4){
                    furnishing(p,xx,1,z-5,Blocks.IRON_BLOCK);
                    furnishing(p,xx,2,z-5,Blocks.IRON_BARS);
                    furnishing(p,xx,1,z+4,Blocks.BARREL);
                }
            }else if(z==0){
                for(int xx=min+3;xx<=max-3;xx+=4){
                    furnishing(p,xx,1,z-5,Blocks.CRAFTING_TABLE);
                    furnishing(p,xx,1,z+5,Blocks.BARREL);
                    furnishing(p,xx,4,z-8,LunarHorizons.LUNAR_LANTERN);
                }
            }else if(side<0){
                // Research benches, sample lockers and a glazed specimen wall.
                for(int xx=min+3;xx<=max-3;xx+=4){
                    furnishing(p,xx,1,z-5,Blocks.SMOOTH_STONE);
                    furnishing(p,xx,2,z-5,LunarHorizons.CRYSTAL_CLUSTER);
                    furnishing(p,xx,1,z+8,Blocks.BARREL);
                }
            }else{
                // Maintenance storage with shelf frames.
                for(int xx=min+3;xx<=max-3;xx+=4){
                    furnishing(p,xx,1,z-6,Blocks.BARREL);
                    furnishing(p,xx,2,z-6,Blocks.BARREL);
                    furnishing(p,xx,3,z-6,LunarHorizons.INDUSTRIAL_GRATE);
                    furnishing(p,xx,1,z+5,Blocks.CRAFTING_TABLE);
                }
            }
            // Close the large transit openings with functional two-block airlocks.
            int inner=side<0?-22:22;
            for(int edge:new int[]{inner,side*12}){
                p.fill(edge,1,z-1,edge,3,z+1,armor);p.door(edge,1,z,side<0?Direction.WEST:Direction.EAST);
                if(side>0)p.put(edge,1,z+1,LunarHorizons.POWER_FEEDTHROUGH);
            }
            for(int xx=Math.min(inner,side*12)+1;xx<Math.max(inner,side*12);xx++){
                p.put(xx,0,z,LunarHorizons.INDUSTRIAL_GRATE);
                if(side>0){p.put(xx,1,z+1,LunarHorizons.POWER_WIRE);if(xx==side*12+1||xx==inner-1)p.put(xx,1,z,LunarHorizons.POWER_WIRE);}
                if(xx%3==0)furnishing(p,xx,3,z,LunarHorizons.LUNAR_LANTERN);
            }
        }
        // Wayfinding and benches in the main spine, without blocking doors or wiring.
        for(int z:new int[]{-33,-25,-17,-10,-3,3,10,17,25,33}){
            furnishing(p,-8,1,z,Blocks.SMOOTH_STONE_SLAB);
            furnishing(p,-7,1,z,Blocks.SMOOTH_STONE_SLAB);
            furnishing(p,7,1,z,Blocks.BARREL);
            furnishing(p,0,4,z,LunarHorizons.LUNAR_LANTERN);
        }
        // Central communications mast, with a stepped equipment platform.
        p.fill(-3,7,-3,3,7,3,armor);
        p.fill(-1,8,-1,1,11,1,LunarHorizons.TITANIUM_BLOCK);
        p.fill(-3,11,0,3,11,0,Blocks.IRON_BARS);
        p.fill(0,11,-3,0,11,3,Blocks.IRON_BARS);
        p.fill(0,12,0,0,15,0,Blocks.IRON_BARS);p.put(0,16,0,LunarHorizons.SURVEY_BEACON);
    }

    private void spaceportDetail(Plan p,Random r){
        Block armor=LunarHorizons.REINFORCED_HULL, metal=LunarHorizons.TITANIUM_BLOCK;
        // Hangars accept a full 12-block Mk II with an extra block of door headroom.
        for(int side:new int[]{-1,1}){
            int center=side*20;
            for(int x=center-14;x<=center+14;x++){
                p.put(x,17,-34,armor);p.put(x,17,-14,armor);
                if((x-center+14)%5==0)for(int z=-33;z<=-15;z++)p.put(x,17,z,metal);
            }
            // Tall exterior buttresses and high ribbon glazing.
            for(int z=-32;z<=-16;z+=4)for(int x:new int[]{center-14,center+14}){
                for(int y=1;y<=15;y++)if(y==7||y==8||y==9)p.put(x,y,z,LunarHorizons.PRESSURE_GLASS);
                else p.put(x,y,z,armor);
            }
            // Service gantry on each side, leaving the central vehicle bay unobstructed.
            for(int x:new int[]{center-11,center+11}){
                for(int z=-31;z<=-17;z++){
                    p.put(x,9,z,LunarHorizons.INDUSTRIAL_GRATE);
                    if(z%4==0){
                        for(int y=1;y<=8;y++)furnishing(p,x,y,z,Blocks.IRON_BARS);
                        furnishing(p,x,10,z,LunarHorizons.LUNAR_LANTERN);
                    }
                }
                for(int y=1;y<=10;y++){
                    p.put(x,y,-32,metal);
                    p.put(x+(x<center?1:-1),y,-32,Blocks.LADDER.getDefaultState().with(Properties.HORIZONTAL_FACING,rotated(p.rot,x<center?Direction.EAST:Direction.WEST)));
                }
            }
            // Workshop tools, storage, and bays along the walls.
            for(int z=-27;z<=-17;z+=3){
                furnishing(p,center-12,1,z,Blocks.CRAFTING_TABLE);
                furnishing(p,center+12,1,z,Blocks.BARREL);
                furnishing(p,center+12,2,z,Blocks.BARREL);
            }
            for(int z=-33;z<=-15;z++)for(int x:new int[]{center-5,center+5})p.put(x,0,z,LunarHorizons.INDUSTRIAL_GRATE);
            // Launch pad border and taxiway: keep 17x17 pad itself and its central air clear.
            for(int z=5;z<=21;z++)for(int x:new int[]{center-9,center+9}){
                p.put(x,0,z,Blocks.YELLOW_CONCRETE);if(z%4==1)light(p,x,1,z);
            }
            for(int x=center-9;x<=center+9;x++)for(int z:new int[]{4,22})p.put(x,0,z,Blocks.YELLOW_CONCRETE);
            for(int z=-12;z<=3;z++){
                p.put(center-4,0,z,Blocks.YELLOW_CONCRETE);p.put(center+4,0,z,Blocks.YELLOW_CONCRETE);
                if(z%3==0)p.put(center,0,z,Blocks.WHITE_CONCRETE);
            }
            // Elevated service tower and cantilever, entirely outside the rocket clearance.
            int sx=center+10;
            p.fill(sx-1,1,24,sx+1,13,24,armor);
            p.fill(sx-3,13,23,sx+3,13,25,LunarHorizons.INDUSTRIAL_GRATE);
            for(int y=1;y<=14;y++)p.put(sx,y,25,Blocks.LADDER.getDefaultState().with(Properties.HORIZONTAL_FACING,rotated(p.rot,Direction.SOUTH)));
            for(int x=sx-3;x<=sx+3;x++)p.put(x,14,23,Blocks.IRON_BARS);
            light(p,sx,14,24);
            // Separate propellant tanks and their pipe headers outside the taxiway.
            for(int zz:new int[]{-8,-2}){
                p.fill(center+11,1,zz,center+13,4,zz+2,metal);
                p.fill(center+11,5,zz,center+13,5,zz+2,armor);
                p.put(center+12,6,zz+1,LunarHorizons.OXYGEN_PIPE);
            }
        }
        // Public apron markings and additional lighting, away from launch columns.
        for(int z=-13;z<=22;z++){
            if(z%3==0){p.put(-4,0,z,Blocks.WHITE_CONCRETE);p.put(4,0,z,Blocks.WHITE_CONCRETE);}
            if(z%7==0){light(p,-6,1,z);light(p,6,1,z);}
        }
        // Tower control consoles, aerials and balcony guard rails.
        for(int x=-5;x<=5;x+=2){
            furnishing(p,x,16,24,LunarHorizons.POWER_TERMINAL);
            furnishing(p,x,16,26,Blocks.SMOOTH_STONE_SLAB);
            furnishing(p,x,19,29,LunarHorizons.LUNAR_LANTERN);
        }
        for(int x=-6;x<=6;x++){p.put(x,21,23,Blocks.IRON_BARS);p.put(x,21,35,Blocks.IRON_BARS);}
        for(int z=24;z<=34;z++){p.put(-7,21,z,Blocks.IRON_BARS);p.put(7,21,z,Blocks.IRON_BARS);}
        p.fill(-5,21,29,-5,24,29,Blocks.IRON_BARS);p.fill(5,21,29,5,24,29,Blocks.IRON_BARS);
        p.put(-5,25,29,LunarHorizons.LUNAR_LANTERN);p.put(5,25,29,LunarHorizons.LUNAR_LANTERN);
    }

    // 59 x 61, high reactor hall with a separate control gallery, cooling and backup power.
    private void reactor(Plan p,Random r){
        Block wall=LunarHorizons.REINFORCED_HULL,deck=LunarHorizons.HABITAT_FLOOR;
        chamber(p,-29,-30,29,30,18);
        p.room(-27,-28,27,-16,0,6,LunarHorizons.HABITAT_HULL,deck);
        p.door(0,1,-16);p.chest(-25,1,-25,20,Direction.SOUTH);
        for(int x=-22;x<=22;x+=5)p.machine(x,1,-20,LunarHorizons.POWER_TERMINAL,Direction.NORTH);
        for(int x=-14;x<=14;x++)for(int z=-8;z<=20;z++){
            int d=x*x+(z-6)*(z-6);
            if(d<=196)p.put(x,1,z,LunarHorizons.TITANIUM_BLOCK);
            if(d<=196&&d>=144)for(int y=2;y<=14;y++){
                boolean hatch=(Math.abs(x)<=1&&z<0&&y<5);
                if(!hatch)p.put(x,y,z,y%4==0?LunarHorizons.INDUSTRIAL_GRATE:wall);
            }
            if(d<=81&&d>=49)p.put(x,2,z,LunarHorizons.CRYSTAL_STONE);
            if(d<=16)for(int y=2;y<=12;y++)p.put(x,y,z,LunarHorizons.CRYSTAL_STONE);
        }
        p.fill(-2,1,-15,2,1,-6,LunarHorizons.INDUSTRIAL_GRATE);
        for(int side:new int[]{-1,1}){
            int x=side*23;
            for(int z=-10;z<=24;z+=6){
                p.machine(x,1,z,LunarHorizons.INDUSTRIAL_BATTERY,Direction.NORTH);
                p.machine(x-side,1,z,LunarHorizons.HELIUM_GENERATOR,Direction.NORTH);
                light(p,x,7,z);
            }
            for(int z=-12;z<=24;z++){
                p.put(x-side*2,1,z,LunarHorizons.POWER_CABLE);
                p.put(x-side*4,4,z,LunarHorizons.OXYGEN_PIPE);
            }
            p.room(side<0?-27:17,22,side<0?-17:27,28,0,5,wall,deck);
            p.door(side*22,1,22);
            p.chest(side*25,1,26,20,Direction.NORTH);
        }
        for(int x=-25;x<=25;x+=10)light(p,x,17,0);
    }

    // 111 x 45 ship; tapered silhouette, two decks, hull breach and traversable spine.
    private void alienShip(Plan p,Random r){
        Block shell=Blocks.POLISHED_DEEPSLATE,dark=Blocks.OBSIDIAN,floor=Blocks.DEEPSLATE_TILES;
        for(int x=-55;x<=55;x++)for(int z=-22;z<=22;z++){
            double d=x*x/3025.0+z*z/484.0;
            if(d>1)continue;
            int roof=4+(int)Math.round(13*Math.sqrt(1-d));
            p.put(x,0,z,shell);
            for(int y=1;y<=roof;y++){
                double shape=d+Math.pow((y-1.0)/Math.max(roof,1)*.52,2);
                boolean rim=d>.86||y==roof||shape>1;
                if(rim)p.put(x,y,z,d>.96?dark:shell);
                else if(y==1||y==8&&roof>=12)p.put(x,y,z,floor);
                else p.put(x,y,z,Blocks.AIR);
            }
            if(x%14==0&&d<.73&&Math.abs(z)>3){
                int max=Math.min(roof-1,13);
                for(int y=2;y<=max;y++)if(y!=8)p.put(x,y,z,Blocks.CHISELED_DEEPSLATE);
            }
        }
        // Passage through the ribs and decks; damaged flank opens to ground.
        p.fill(-43,2,-2,43,5,2,Blocks.AIR);p.fill(-36,9,-2,36,11,2,Blocks.AIR);
        p.fill(9,2,7,17,6,22,Blocks.AIR);
        p.fill(9,1,7,17,1,22,floor);
        for(int x:new int[]{-34,-14,14,34}){
            for(int z:new int[]{-7,7}){
                p.fill(x-1,2,z-1,x+1,2,z+1,Blocks.CRYING_OBSIDIAN);
                p.put(x,3,z,LunarHorizons.CRYSTAL_STONE);
                p.put(x,4,z,LunarHorizons.DARK_CRYSTAL_ORE);
            }
        }
        for(int x:new int[]{-21,21}){
            for(int h=2;h<=9;h++){p.put(x+1,h,0,shell);p.put(x,h,0,Blocks.LADDER.getDefaultState().with(Properties.HORIZONTAL_FACING,rotated(p.rot,Direction.WEST)));}
            p.put(x,10,0,Blocks.AIR);
        }
        // Entry ramp down from the breach; no need to enter through an unbroken wall.
        BlockPos at=p.at(13,0,23);int ground=top(p.w,at.getX(),at.getZ())-p.origin.getY();
        for(int h=Math.max(-36,ground);h<=1;h++){
            p.put(13,h,22,shell);
            p.put(13,h,23,Blocks.LADDER.getDefaultState().with(Properties.HORIZONTAL_FACING,rotated(p.rot,Direction.SOUTH)));
        }
        p.put(13,2,22,Blocks.AIR);
        p.chest(-42,2,0,21,Direction.SOUTH);p.chest(41,2,0,21,Direction.NORTH);
        p.chest(-27,9,3,21,Direction.WEST);p.chest(25,9,-3,21,Direction.EAST);
    }

    private boolean geode(Terrain w,Random r,int cx,int cz,int x,int z,int rot){
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
    static void loot(StructureWorldAccess w,BlockPos pos,Random r,int type){
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

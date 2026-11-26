package com.lunarhorizons;

import com.mojang.serialization.Codec;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/** Eroded, descending cave mouths rather than vertical spiral stair shafts.
 * Only cuts a route after finding an actual cave floor and validating the whole path.
 * Never writes outside its generating chunk or replaces ores and structures. */
public final class CaveEntranceFeature extends Feature<DefaultFeatureConfig> {
    private record Column(int x,int z){}
    private record Cut(int floor,int height,double distance){}
    public CaveEntranceFeature(Codec<DefaultFeatureConfig> codec){super(codec);}
    private static boolean replaceable(StructureWorldAccess w,BlockPos p){
        return w.isAir(p)||ImpactCraterFeature.natural(w,p);
    }
    private static boolean caveFloor(StructureWorldAccess w,BlockPos p){
        if(!ImpactCraterFeature.natural(w,p.down())||!w.isAir(p)||!w.isAir(p.up()))return false;
        int neighbors=0;
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
            BlockPos q=p.add(dx,0,dz);
            if(w.isAir(q)&&w.isAir(q.up())&&ImpactCraterFeature.natural(w,q.down()))neighbors++;
        }
        if(neighbors<3)return false;
        for(int h=2;h<=6;h++)if(ImpactCraterFeature.natural(w,p.up(h)))return true;
        return false;
    }
    private static int height(StructureWorldAccess w,int x,int z){
        return w.getTopY(Heightmap.Type.WORLD_SURFACE_WG,x,z);
    }
    @Override public boolean generate(FeatureContext<DefaultFeatureConfig> c){
        StructureWorldAccess w=c.getWorld();Random r=c.getRandom();
        int cx=c.getOrigin().getX()&~15,cz=c.getOrigin().getZ()&~15;
        // Search inward from a chunk edge. Length 8–11, grade at most 1 block per 2 horizontal blocks.
        for(int attempt=0;attempt<48;attempt++){
            int side=r.nextInt(4),along=4+r.nextInt(8),length=8+r.nextInt(4),bend=r.nextBoolean()?1:-1;
            int dx=side==0?1:side==1?-1:0,dz=side==2?1:side==3?-1:0;
            int sx=cx+(dx==1?2:dx==-1?13:along),sz=cz+(dz==1?2:dz==-1?13:along);
            int ex=sx+dx*length,ez=sz+dz*length;
            int start=height(w,sx,sz),endSurface=height(w,ex,ez);
            if(start<10||start>150||!ImpactCraterFeature.natural(w,new BlockPos(sx,start-1,sz)))continue;
            int end=-999;
            for(int drop=3;drop<=length/2;drop++){
                int y=start-drop;
                if(y<=endSurface-3&&caveFloor(w,new BlockPos(ex,y,ez))){end=y;break;}
            }
            if(end==-999)continue;
            Map<Column,Cut> cuts=new LinkedHashMap<>();
            for(int i=0;i<=length;i++){
                double t=(double)i/length;
                int wiggle=(int)Math.round(bend*Math.sin(t*Math.PI));
                int x=sx+dx*i+dz*wiggle,z=sz+dz*i-dx*wiggle;
                int floor=(int)Math.round(start+(end-start)*t)-1;
                // Broad irregular mouth; a narrower rounded passage deeper inside.
                int radius=i<=2?2:1;
                for(int a=-radius;a<=radius;a++)for(int b=-radius;b<=radius;b++){
                    int xx=x+a,zz=z+b;
                    if(xx<cx||xx>=cx+16||zz<cz||zz>=cz+16)continue;
                    double dist=Math.hypot(a,b);
                    if(dist>radius+.15)continue;
                    int h=dist>1.3?2:3;
                    if(i==1&&dist<.1)h=4;
                    Column col=new Column(xx,zz);
                    Cut previous=cuts.get(col);
                    if(previous==null||dist<previous.distance())cuts.put(col,new Cut(floor,h,dist));
                }
            }
            boolean valid=true;
            for(var entry:cuts.entrySet()){
                Column col=entry.getKey();Cut cut=entry.getValue();
                BlockPos floor=new BlockPos(col.x(),cut.floor(),col.z());
                // Keep the walkable natural floor, never build artificial steps.
                if(cut.distance()<.1&&!ImpactCraterFeature.natural(w,floor)){valid=false;break;}
                for(int h=1;h<=cut.height();h++)if(!replaceable(w,floor.up(h))){valid=false;break;}
                if(!valid)break;
            }
            if(!valid)continue;
            for(var entry:cuts.entrySet()){
                Column col=entry.getKey();Cut cut=entry.getValue();
                BlockPos floor=new BlockPos(col.x(),cut.floor(),col.z());
                for(int h=1;h<=cut.height();h++){
                    BlockPos p=floor.up(h);
                    if(!w.isAir(p))w.setBlockState(p,Blocks.AIR.getDefaultState(),2);
                }
            }
            return true;
        }
        return false;
    }
}

package com.lunarhorizons;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/** One deterministic, connected lava-tube graph, evaluated independently in each chunk.
 * No global noise voids, no neighbor writes, no chunk-load-order dependence in graph topology.
 * Every lattice node connects east and south; rare ramps originate at those same nodes. */
public final class LunarCaveNetworkFeature extends Feature<DefaultFeatureConfig>{
    private static final int GRID=64, RAMP_LENGTH=192;
    private record Node(double x,double y,double z,boolean entrance,int direction,double bend){}
    public LunarCaveNetworkFeature(Codec<DefaultFeatureConfig> codec){super(codec);}
    private static Node node(long seed,int gx,int gz){
        Random r=new Random(seed^((long)gx*341873128712L)^((long)gz*132897987541L)^0x4c554e41524cL);
        double x=gx*(double)GRID+16+r.nextInt(33),z=gz*(double)GRID+16+r.nextInt(33);
        double y=r.nextInt(5)==0?-38+r.nextInt(11):-17+r.nextInt(32);
        return new Node(x,y,z,r.nextInt(7)==0,r.nextInt(4),r.nextDouble()*2-1);
    }
    private static void ellipsoid(StructureWorldAccess w,int cx,int cz,double x,double y,double z,double rx,double ry){
        if(x+rx<cx||x-rx>cx+15||z+rx<cz||z-rx>cz+15)return;
        int minX=Math.max(cx,(int)Math.ceil(x-rx)),maxX=Math.min(cx+15,(int)Math.floor(x+rx));
        int minZ=Math.max(cz,(int)Math.ceil(z-rx)),maxZ=Math.min(cz+15,(int)Math.floor(z+rx));
        int minY=Math.max(-53,(int)Math.ceil(y-ry)),maxY=Math.min(170,(int)Math.floor(y+ry));
        BlockPos.Mutable p=new BlockPos.Mutable();
        for(int xx=minX;xx<=maxX;xx++)for(int zz=minZ;zz<=maxZ;zz++){
            double horizontal=(xx-x)*(xx-x)/(rx*rx)+(zz-z)*(zz-z)/(rx*rx);
            if(horizontal>=1)continue;
            for(int yy=minY;yy<=maxY;yy++){
                if(horizontal+(yy-y)*(yy-y)/(ry*ry)>=1)continue;
                p.set(xx,yy,zz);
                if(ImpactCraterFeature.natural(w,p))w.setBlockState(p,Blocks.CAVE_AIR.getDefaultState(),2);
            }
        }
    }
    private static void tube(StructureWorldAccess w,int cx,int cz,Node a,Node b,boolean ramp){
        double dx=b.x-a.x,dz=b.z-a.z,dy=b.y-a.y,len=Math.hypot(dx,dz);
        double bend=(ramp?5:7)*a.bend,perpX=-dz/len,perpZ=dx/len;
        if(Math.max(a.x,b.x)+Math.abs(bend)+6<cx||Math.min(a.x,b.x)-Math.abs(bend)-6>cx+15||
           Math.max(a.z,b.z)+Math.abs(bend)+6<cz||Math.min(a.z,b.z)-Math.abs(bend)-6>cz+15)return;
        int steps=(int)Math.ceil(Math.hypot(len,dy)/.85);
        for(int i=0;i<=steps;i++){
            double t=i/(double)steps,s=Math.sin(Math.PI*t),wave=Math.sin(2*Math.PI*t);
            double x=a.x+dx*t+perpX*bend*s,z=a.z+dz*t+perpZ*bend*s;
            double y=a.y+dy*t+(ramp?0:1.6*wave);
            double rx=(ramp?2.45:2.85)+.45*Math.sin(t*9+a.bend*3);
            double ry=(ramp?2.25:2.55)+.3*Math.cos(t*7+a.bend);
            ellipsoid(w,cx,cz,x,y,z,rx,ry);
        }
    }
    @Override public boolean generate(FeatureContext<DefaultFeatureConfig> c){
        StructureWorldAccess w=c.getWorld();
        int cx=c.getOrigin().getX()&~15,cz=c.getOrigin().getZ()&~15;
        int gx=Math.floorDiv(cx,GRID),gz=Math.floorDiv(cz,GRID);
        long seed=w.getSeed();
        for(int a=-4;a<=4;a++)for(int b=-4;b<=4;b++){
            int ix=gx+a,iz=gz+b;
            Node n=node(seed,ix,iz);
            // Shared endpoints make all normal tubes and their chambers one graph.
            if(Math.abs(a)<=2&&Math.abs(b)<=2){
                ellipsoid(w,cx,cz,n.x,n.y,n.z,4.6+Math.abs(n.bend)*1.5,3.8);
                tube(w,cx,cz,n,node(seed,ix+1,iz),false);
                tube(w,cx,cz,n,node(seed,ix,iz+1),false);
            }
            if(n.entrance&&n.y>=-17){
                double dx=n.direction==0?1:n.direction==1?-1:0,dz=n.direction==2?1:n.direction==3?-1:0;
                // Inclined tunnel climbs into open air, preserving solid floor and ceiling elsewhere.
                Node end=new Node(n.x+dx*RAMP_LENGTH,145,n.z+dz*RAMP_LENGTH,false,0,0);
                tube(w,cx,cz,n,end,true);
            }
        }
        return true;
    }
}

package com.lunarhorizons;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/** Coordinate-seeded impact basins, evaluated per column so rims cross chunk borders. */
public final class ImpactCraterFeature extends Feature<DefaultFeatureConfig> {
    private static final int GRID=80;
    public ImpactCraterFeature(Codec<DefaultFeatureConfig> codec){super(codec);}
    static boolean natural(StructureWorldAccess w,BlockPos p){
        var s=w.getBlockState(p);
        return s.isOf(LunarHorizons.MOON_ROCK)||s.isOf(LunarHorizons.REGOLITH)||s.isOf(LunarHorizons.HIGHLAND_ROCK)||s.isOf(LunarHorizons.FROSTED_REGOLITH)||s.isOf(LunarHorizons.DARK_REGOLITH)||s.isOf(LunarHorizons.BASALT_DUST)||s.isOf(LunarHorizons.ANORTHOSITE)||s.isOf(LunarHorizons.EJECTA_ROCK);
    }
    private record Crater(int x,int z,int r,int depth,boolean meteorite){}
    @Override public boolean generate(FeatureContext<DefaultFeatureConfig> c){
        StructureWorldAccess w=c.getWorld();
        int ox=c.getOrigin().getX()&~15,oz=c.getOrigin().getZ()&~15;
        java.util.List<Crater> craters=new java.util.ArrayList<>();
        int gx=Math.floorDiv(ox,GRID),gz=Math.floorDiv(oz,GRID);
        for(int a=-1;a<=1;a++)for(int b=-1;b<=1;b++){
            int rx=gx+a,rz=gz+b;
            Random random=new Random(w.getSeed() ^ ((long)rx*341873128712L) ^ ((long)rz*132897987541L));
            if(random.nextFloat()>.65f)continue;
            int x=rx*GRID+12+random.nextInt(GRID-24),z=rz*GRID+12+random.nextInt(GRID-24),r=12+random.nextInt(18);
            craters.add(new Crater(x,z,r,Math.max(4,r/3),random.nextInt(5)==0));
        }
        boolean changed=false;
        for(int x=ox;x<ox+16;x++)for(int z=oz;z<oz+16;z++){
            double cut=0,rim=0;boolean meteor=false;
            for(Crater q:craters){
                double t=Math.hypot(x-q.x,z-q.z)/q.r;
                if(t<1){cut=Math.max(cut,q.depth*(1-t*t)*(1-t*t));if(q.meteorite&&t<.12)meteor=true;}
                else if(t<1.17)rim=Math.max(rim,2.2*(1-Math.abs(t-1.08)/.09));
            }
            int depth=(int)Math.round(cut),raise=(int)Math.round(rim);
            if(depth<=0&&raise<=0)continue;
            int top=w.getTopY(Heightmap.Type.WORLD_SURFACE_WG,x,z);
            if(top<0||top>170)continue;
            BlockPos surface=new BlockPos(x,top-1,z);
            if(!natural(w,surface))continue;
            var surfaceState=w.getBlockState(surface);
            if(depth>0){
                int removed=0;
                for(int y=0;y<depth;y++){BlockPos p=surface.down(y);if(!natural(w,p))break;w.setBlockState(p,Blocks.AIR.getDefaultState(),2);removed++;}
                BlockPos floor=surface.down(removed);
                if(natural(w,floor))w.setBlockState(floor,meteor?LunarHorizons.METEORITE_ROCK.getDefaultState():surfaceState,2);
                changed|=removed>0;
            }else if(raise>0){
                for(int y=1;y<=raise;y++){BlockPos p=surface.up(y);if(!w.isAir(p))break;w.setBlockState(p,surfaceState,2);changed=true;}
            }
        }
        return changed;
    }
}

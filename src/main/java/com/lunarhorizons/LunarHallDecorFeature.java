package com.lunarhorizons;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/** Local hall decorations. Generated after the cave network and ores, before structures. */
public final class LunarHallDecorFeature extends Feature<DefaultFeatureConfig>{
    private static final int GRID=64;
    public LunarHallDecorFeature(Codec<DefaultFeatureConfig> codec){super(codec);}
    private static boolean floorAir(StructureWorldAccess w,BlockPos p){
        return ImpactCraterFeature.natural(w,p.down())&&w.isAir(p)&&w.isAir(p.up())&&w.isAir(p.up(2));
    }
    private static boolean roofAir(StructureWorldAccess w,BlockPos p){
        return ImpactCraterFeature.natural(w,p.up())&&w.isAir(p)&&w.isAir(p.down())&&w.isAir(p.down(2));
    }
    @Override public boolean generate(FeatureContext<DefaultFeatureConfig> c){
        StructureWorldAccess w=c.getWorld();
        int cx=c.getOrigin().getX()&~15,cz=c.getOrigin().getZ()&~15;
        int gx=Math.floorDiv(cx,GRID),gz=Math.floorDiv(cz,GRID);
        int placed=0;
        for(int a=-1;a<=1;a++)for(int b=-1;b<=1;b++){
            var h=LunarCaveNetworkFeature.hall(w.getSeed(),gx+a,gz+b);
            if(h.x()+h.rx()*1.14<cx||h.x()-h.rx()*1.14>cx+15||
                h.z()+h.rz()*1.14<cz||h.z()-h.rz()*1.14>cz+15)continue;
            // Each column independently seeded; nothing depends on neighbor decoration.
            for(int x=cx;x<cx+16;x++)for(int z=cz;z<cz+16;z++){
                double edge=LunarCaveNetworkFeature.edge(h,x,z);
                if(edge<.42||edge>.93)continue; // open central floor and junctions
                Random r=new Random(w.getSeed()^((long)x*341873128712L)^((long)z*132897987541L)^0x4445434f52L);
                int roll=r.nextInt(100);
                if(roll>=(h.theme()==1?4:h.theme()==2?3:2))continue;
                int y=LunarCaveNetworkFeature.floor(h,x,z)+1;
                BlockPos p=new BlockPos(x,y,z);
                int roof=LunarCaveNetworkFeature.ceiling(h,x,z);
                if(roof-y<4)continue;
                if(roll==0&&h.theme()!=0&&floorAir(w,p)){
                    // Sparse mineral patches, never replacing an ore or an existing decoration.
                    w.setBlockState(p.down(),LunarHorizons.CRYSTAL_STONE.getDefaultState(),2);
                    w.setBlockState(p,LunarHorizons.CRYSTAL_CLUSTER.getDefaultState(),2);placed++;
                }else if(roll==1&&floorAir(w,p)){
                    BlockState stone=LunarHorizons.MOON_ROCK.getDefaultState();
                    int height=Math.min(3,roof-y-2);
                    for(int i=0;i<height&&w.isAir(p.up(i));i++)w.setBlockState(p.up(i),stone,2);
                    placed++;
                }else{
                    // Hanging lava-rock fingers, with sufficient headroom below.
                    BlockPos q=new BlockPos(x,roof,z);
                    if(roof-y>=6&&roofAir(w,q)){
                        BlockState stone=LunarHorizons.MOON_ROCK.getDefaultState();
                        int height=1+r.nextInt(2);
                        for(int i=0;i<height&&w.isAir(q.down(i));i++)w.setBlockState(q.down(i),stone,2);
                        placed++;
                    }
                }
            }
        }
        return placed>0;
    }
}

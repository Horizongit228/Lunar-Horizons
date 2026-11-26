package com.lunarhorizons;

import com.mojang.serialization.Codec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

public final class CrystalGrottoFeature extends Feature<DefaultFeatureConfig>{
    public CrystalGrottoFeature(Codec<DefaultFeatureConfig> codec){super(codec);}
    @Override public boolean generate(FeatureContext<DefaultFeatureConfig> c){
        StructureWorldAccess w=c.getWorld();Random r=c.getRandom();BlockPos p=c.getOrigin();
        int placed=0;
        for(int i=0;i<10;i++){
            int dx=r.nextInt(9)-4,dz=r.nextInt(9)-4;
            BlockPos q=p.add(dx,0,dz);
            // Stay inside the generating chunk.
            if((q.getX()>>4)!=(p.getX()>>4)||(q.getZ()>>4)!=(p.getZ()>>4))continue;
            for(int dy=7;dy>=-7;dy--){
                BlockPos a=q.up(dy), below=a.down();
                if(!w.isAir(a)||!ImpactCraterFeature.natural(w,below))continue;
                if(w.getBlockState(a.up()).isAir()&&w.getBlockState(a.up(2)).isAir() && a.getY() < 42){
                    boolean roof=false;
                    for(int h=3;h<=18;h++) if(ImpactCraterFeature.natural(w,a.up(h))){roof=true;break;}
                    if(!roof)break;
                    // Clusters are spaced out; keep the passage walkable.
                    boolean crowded=false;
                    for(int nx=-1;nx<=1;nx++)for(int nz=-1;nz<=1;nz++){
                        if(((a.getX()+nx)>>4)!=(p.getX()>>4)||((a.getZ()+nz)>>4)!=(p.getZ()>>4))continue;
                        if(w.getBlockState(a.add(nx,0,nz)).isOf(LunarHorizons.CRYSTAL_CLUSTER))crowded=true;
                    }
                    if(crowded)break;
                    w.setBlockState(below,LunarHorizons.CRYSTAL_STONE.getDefaultState(),2);
                    if(r.nextInt(7)==0&&w.isAir(a.up(3))){
                        w.setBlockState(a,LunarHorizons.CRYSTAL_STONE.getDefaultState(),2);
                        w.setBlockState(a.up(),LunarHorizons.CRYSTAL_CLUSTER.getDefaultState(),2);
                    }else w.setBlockState(a,LunarHorizons.CRYSTAL_CLUSTER.getDefaultState(),2);
                    placed++;
                }
                break;
            }
            if(placed>=3)break;
        }
        return placed>0;
    }
}

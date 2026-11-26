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
        for(int i=0;i<24;i++){
            int dx=r.nextInt(7)-3,dz=r.nextInt(7)-3;
            BlockPos q=p.add(dx,0,dz);
            // Stay inside the generating chunk.
            if((q.getX()>>4)!=(p.getX()>>4)||(q.getZ()>>4)!=(p.getZ()>>4))continue;
            for(int dy=7;dy>=-7;dy--){
                BlockPos a=q.up(dy), below=a.down();
                if(!w.isAir(a)||!w.getBlockState(below).isOf(LunarHorizons.MOON_ROCK)&&!w.getBlockState(below).isOf(LunarHorizons.REGOLITH))continue;
                if(w.getBlockState(a.up()).isAir()&&w.getBlockState(a.up(2)).isAir()){
                    w.setBlockState(below,LunarHorizons.CRYSTAL_STONE.getDefaultState(),2);
                    if(r.nextInt(4)==0&&w.isAir(a.up(3))){
                        w.setBlockState(a,LunarHorizons.CRYSTAL_STONE.getDefaultState(),2);
                        w.setBlockState(a.up(),LunarHorizons.CRYSTAL_CLUSTER.getDefaultState(),2);
                    }else w.setBlockState(a,LunarHorizons.CRYSTAL_CLUSTER.getDefaultState(),2);
                    placed++;
                }
                break;
            }
        }
        return placed>0;
    }
}

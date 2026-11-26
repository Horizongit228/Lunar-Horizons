package com.lunarhorizons;

import com.mojang.serialization.Codec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/** Mineral stems growing on sheltered cave floors, never on the open lunar surface. */
public final class LunarStemFeature extends Feature<DefaultFeatureConfig> {
    public LunarStemFeature(Codec<DefaultFeatureConfig> codec) { super(codec); }
    @Override public boolean generate(FeatureContext<DefaultFeatureConfig> c) {
        StructureWorldAccess world=c.getWorld();
        Random random=c.getRandom();
        BlockPos origin=c.getOrigin();
        int placed=0;
        for(int i=0;i<8 && placed<3;i++){
            BlockPos column=origin.add(random.nextInt(7)-3,0,random.nextInt(7)-3);
            if((column.getX()>>4)!=(origin.getX()>>4)||(column.getZ()>>4)!=(origin.getZ()>>4))continue;
            for(int y=8;y>=-8;y--){
                BlockPos pos=column.up(y),floor=pos.down();
                if(pos.getY()<-56 || pos.getY()>38 || !world.isAir(pos) || !world.isAir(pos.up()))continue;
                var state=world.getBlockState(floor);
                if(!ImpactCraterFeature.natural(world,floor))continue;
                boolean roof=false;
                for(int h=2;h<=24;h++){
                    var above=world.getBlockState(pos.up(h));
                    if(ImpactCraterFeature.natural(world,pos.up(h))){roof=true;break;}
                }
                if(!roof)continue;
                world.setBlockState(pos,LunarHorizons.LUNAR_STEMS.getDefaultState(),2);
                placed++;
                break;
            }
        }
        return placed>0;
    }
}

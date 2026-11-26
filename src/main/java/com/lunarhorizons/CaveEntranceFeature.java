package com.lunarhorizons;

import com.mojang.serialization.Codec;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/** An occasional narrow spiral entrance, only when it meets a real shallow cave.
 * Never edits a neighboring chunk, ores, bedrock, or non-natural blocks. */
public final class CaveEntranceFeature extends Feature<DefaultFeatureConfig> {
    private static final int[][] RING={{-1,-1},{0,-1},{1,-1},{1,0},{1,1},{0,1},{-1,1},{-1,0}};
    public CaveEntranceFeature(Codec<DefaultFeatureConfig> codec){super(codec);}
    private static boolean natural(StructureWorldAccess w,BlockPos p){
        var s=w.getBlockState(p);
        return s.isAir()||s.isOf(LunarHorizons.MOON_ROCK)||s.isOf(LunarHorizons.REGOLITH);
    }
    private static boolean chamber(StructureWorldAccess w,BlockPos p){
        if(!w.isAir(p)||!w.isAir(p.up())||!w.isAir(p.down()))return false;
        int open=0;
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)
            if(w.isAir(p.add(dx,0,dz))&&w.isAir(p.add(dx,1,dz)))open++;
        return open>=5;
    }
    @Override public boolean generate(FeatureContext<DefaultFeatureConfig> c){
        StructureWorldAccess w=c.getWorld();Random r=c.getRandom();
        int cx=c.getOrigin().getX() & ~15,cz=c.getOrigin().getZ() & ~15;
        int offset=r.nextInt(8);
        for(int attempt=0;attempt<24;attempt++){
            int x=cx+3+r.nextInt(10),z=cz+3+r.nextInt(10);
            int top=w.getTopY(Heightmap.Type.WORLD_SURFACE_WG,x,z);
            if(top<16||top>150)continue;
            int cave=-999;
            // Keep entrances short; deeper caves are reached via natural tunnels or mining.
            for(int y=top-5;y>=Math.max(-20,top-34);y--){
                if(chamber(w,new BlockPos(x,y,z))){cave=y;break;}
            }
            if(cave==-999)continue;
            boolean safe=true;
            for(int y=cave;y<=top+1&&safe;y++)for(int dx=-1;dx<=1&&safe;dx++)for(int dz=-1;dz<=1;dz++)
                if(!natural(w,new BlockPos(x+dx,y,z+dz))){safe=false;break;}
            if(!safe)continue;
            // Leave one natural stone ledge per height, in a continuous spiral.
            // If the bottom intersects air, leave it open to the cave.
            for(int y=cave;y<=top+1;y++){
                int[] step=RING[Math.floorMod(y-cave+offset,8)];
                for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
                    BlockPos p=new BlockPos(x+dx,y,z+dz);
                    if(y<top&&y>cave&&dx==step[0]&&dz==step[1]&&!w.isAir(p))continue;
                    w.setBlockState(p,Blocks.AIR.getDefaultState(),2);
                }
            }
            return true;
        }
        return false;
    }
}

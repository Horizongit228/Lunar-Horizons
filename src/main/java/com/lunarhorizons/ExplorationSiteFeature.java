package com.lunarhorizons;

import com.mojang.serialization.Codec;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/** Small, self-contained exploration sites; only natural terrain or air may be replaced. */
public final class ExplorationSiteFeature extends Feature<DefaultFeatureConfig>{
    private final int kind;
    public ExplorationSiteFeature(Codec<DefaultFeatureConfig> codec,int kind){super(codec);this.kind=kind;}
    private static boolean replaceable(StructureWorldAccess w,BlockPos p){return w.isAir(p)||ImpactCraterFeature.natural(w,p);}
    private static void set(StructureWorldAccess w,BlockPos p,BlockState s){w.setBlockState(p,s,2);}
    @Override public boolean generate(FeatureContext<DefaultFeatureConfig> c){
        StructureWorldAccess w=c.getWorld();Random r=c.getRandom();
        int x=(c.getOrigin().getX()&~15)+8,z=(c.getOrigin().getZ()&~15)+8;
        int radius=kind==1?3:kind==3?4:3;
        int y=w.getTopY(Heightmap.Type.WORLD_SURFACE_WG,x,z);
        if(kind>=2)y=kind==2?-18+r.nextInt(25):-32+r.nextInt(22);
        if(y<-50||y>160)return false;
        BlockPos p=new BlockPos(x,y,z);
        for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++)for(int h=-1;h<=5;h++){
            BlockPos pos=p.add(dx,h,dz);
            if(!replaceable(w,pos))return false;
            if(kind<2&&h==-1&&!ImpactCraterFeature.natural(w,pos))return false;
            if(kind<2&&h>=0&&!w.isAir(pos))return false;
        }
        if(kind==3){
            for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++)for(int h=-1;h<=5;h++){
                double v=dx*dx/16.0+dz*dz/16.0+(h-2)*(h-2)/9.0;
                if(v>1)continue;
                set(w,p.add(dx,h,dz),v>.63?LunarHorizons.CRYSTAL_STONE.getDefaultState():Blocks.AIR.getDefaultState());
            }
            // An opening to mine through and a limited number of crystals.
            for(int dx=2;dx<=4;dx++)for(int h=1;h<=2;h++)set(w,p.add(dx,h,0),Blocks.AIR.getDefaultState());
            for(int i=0;i<5;i++){int dx=r.nextInt(5)-2,dz=r.nextInt(5)-2;BlockPos at=p.add(dx,0,dz);if(w.isAir(at)&&!w.isAir(at.down()))set(w,at,LunarHorizons.CRYSTAL_CLUSTER.getDefaultState());}
            return true;
        }
        if(kind==1){
            for(int dx=-3;dx<=3;dx++)for(int dz=-1;dz<=1;dz++)set(w,p.add(dx,0,dz),Math.abs(dx)<2?LunarHorizons.TITANIUM_BLOCK.getDefaultState():Blocks.BLUE_STAINED_GLASS.getDefaultState());
            set(w,p.up(),LunarHorizons.TITANIUM_BLOCK.getDefaultState());
            set(w,p.up(2),LunarHorizons.SURVEY_BEACON.getDefaultState());
            loot(w,p.add(0,0,2),r);
            return true;
        }
        // Outpost and laboratory: compact rooms with doorway, light and supply chest.
        for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)for(int h=-1;h<=3;h++){
            boolean shell=h==-1||h==3||Math.abs(dx)==3||Math.abs(dz)==3;
            BlockState s=shell?LunarHorizons.MOON_BRICKS.getDefaultState():Blocks.AIR.getDefaultState();
            if(h==1&&Math.abs(dx)==3&&dz==0)s=Blocks.GLASS.getDefaultState();
            if(dz==-3&&dx==0&&(h==0||h==1))s=Blocks.AIR.getDefaultState();
            set(w,p.add(dx,h,dz),s);
        }
        set(w,p.add(-2,2,2),LunarHorizons.LUNAR_LANTERN.getDefaultState());
        set(w,p.add(2,0,2),LunarHorizons.REFILL_STATION.getDefaultState());
        set(w,p.up(4),LunarHorizons.SURVEY_BEACON.getDefaultState());
        loot(w,p.add(-2,0,-1),r);
        return true;
    }
    private void loot(StructureWorldAccess w,BlockPos pos,Random r){
        set(w,pos,Blocks.CHEST.getDefaultState());
        if(w.getBlockEntity(pos) instanceof ChestBlockEntity chest){
            chest.setStack(0,new ItemStack(LunarHorizons.TITANIUM_INGOT,1+r.nextInt(3)));
            chest.setStack(4,new ItemStack(kind==1?LunarHorizons.METEORITE_FRAGMENT:LunarHorizons.LUNAR_ICE,1+r.nextInt(3)));
            chest.setStack(8,new ItemStack(LunarHorizons.FUEL,1+r.nextInt(2)));
            chest.setStack(12,new ItemStack(LunarHorizons.LUNAR_STICK,2+r.nextInt(4)));
            chest.setStack(17,new ItemStack(LunarHorizons.OXYGEN));
            chest.setStack(22,new ItemStack(kind==2?LunarHorizons.HELIUM:LunarHorizons.CRYSTAL,1+r.nextInt(2)));
            chest.markDirty();
        }
    }
}

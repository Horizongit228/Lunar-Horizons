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
        int cx=c.getOrigin().getX()&~15,cz=c.getOrigin().getZ()&~15;
        if(kind==1)return satellite(w,r,cx,cz);
        int radius=kind==3?4:3;
        BlockPos p=null;
        // The entire envelope stays in this chunk. Search several alternatives.
        for(int attempt=0;attempt<24&&p==null;attempt++){
            int margin=radius+2;
            int x=cx+margin+r.nextInt(16-2*margin),z=cz+margin+r.nextInt(16-2*margin);
            if(kind<2)p=surfaceSite(w,x,z,radius);
            else {
                int y=kind==2?-27+r.nextInt(39):-45+r.nextInt(27);
                BlockPos candidate=new BlockPos(x,y,z);
                if(buriedSite(w,candidate,radius))p=candidate;
            }
        }
        if(p==null)return false;
        if(kind<2){
            // A supported foundation, never a floating platform or a filled cave.
            for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
                BlockPos col=p.add(dx,-1,dz);
                for(int h=0;h<=2;h++){
                    BlockPos q=col.down(h);
                    if(!w.isAir(q))break;
                    set(w,q,LunarHorizons.MOON_BRICKS.getDefaultState());
                }
            }
        }
        if(kind==3){
            for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++)for(int h=-1;h<=5;h++){
                double v=dx*dx/16.0+dz*dz/16.0+(h-2)*(h-2)/9.0;
                if(v>1)continue;
                set(w,p.add(dx,h,dz),v>.63?LunarHorizons.CRYSTAL_STONE.getDefaultState():Blocks.AIR.getDefaultState());
            }
            // Keep the geode shell intact; players discover it by mining.
            for(int i=0;i<5;i++){int dx=r.nextInt(5)-2,dz=r.nextInt(5)-2;BlockPos at=p.add(dx,0,dz);if(w.isAir(at)&&!w.isAir(at.down()))set(w,at,LunarHorizons.CRYSTAL_CLUSTER.getDefaultState());}
            return true;
        }
        // Outpost and laboratory: compact rooms with doorway, light and supply chest.
        for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)for(int h=-1;h<=3;h++){
            boolean shell=h==-1||h==3||Math.abs(dx)==3||Math.abs(dz)==3;
            BlockState s=shell?LunarHorizons.MOON_BRICKS.getDefaultState():Blocks.AIR.getDefaultState();
            if(h==1&&Math.abs(dx)==3&&dz==0)s=Blocks.GLASS.getDefaultState();
            if(dz==-3&&dx==0&&(h==0||h==1))s=kind==2?LunarHorizons.MOON_BRICKS.getDefaultState():Blocks.AIR.getDefaultState();
            set(w,p.add(dx,h,dz),s);
        }
        set(w,p.add(-2,2,2),LunarHorizons.LUNAR_LANTERN.getDefaultState());
        set(w,p.add(2,0,2),LunarHorizons.REFILL_STATION.getDefaultState());
        set(w,p.up(4),LunarHorizons.SURVEY_BEACON.getDefaultState());
        loot(w,p.add(-2,0,-2),r);
        return true;
    }
    /** Entire 15x15 footprint stays inside its generating chunk. */
    private boolean satellite(StructureWorldAccess w,Random r,int cx,int cz){
        int x=cx+7,z=cz+7;
        BlockPos p=surfaceSite(w,x,z,7);
        if(p==null)return false;
        boolean damaged=r.nextBoolean(),rotated=r.nextBoolean();
        // Preflight a shallow impact scar. Never cut into a cavern or remove ore.
        for(int dx=-6;dx<=6;dx++)for(int dz=-6;dz<=6;dz++){
            double q=(dx*dx+dz*dz)/36.0;if(q>=1)continue;
            int y=w.getTopY(Heightmap.Type.WORLD_SURFACE_WG,x+dx,z+dz)-1;
            BlockPos surface=new BlockPos(x+dx,y,z+dz);
            if(!ImpactCraterFeature.natural(w,surface)||!ImpactCraterFeature.natural(w,surface.down())||!ImpactCraterFeature.natural(w,surface.down(2)))return false;
        }
        for(int dx=-6;dx<=6;dx++)for(int dz=-6;dz<=6;dz++){
            double q=(dx*dx+dz*dz)/36.0;if(q>=1)continue;
            int y=w.getTopY(Heightmap.Type.WORLD_SURFACE_WG,x+dx,z+dz)-1;
            BlockPos surface=new BlockPos(x+dx,y,z+dz);
            int depth=q<.29?2:q<.75?1:0;
            for(int j=0;j<depth;j++)set(w,surface.down(j),Blocks.AIR.getDefaultState());
            if(depth>0)set(w,surface.down(depth),LunarHorizons.EJECTA_ROCK.getDefaultState());
        }
        // Central body on individual grounded struts, not a solid artificial platform.
        BlockPos center=p.down(1);
        for(int u=-1;u<=1;u++)for(int v=-1;v<=1;v++){
            BlockPos base=at(center,u,-1,v,rotated);
            for(int h=0;h<5;h++){
                BlockPos q=base.down(h);
                if(!w.isAir(q))break;
                set(w,q,LunarHorizons.TITANIUM_BLOCK.getDefaultState());
            }
            set(w,at(center,u,0,v,rotated),LunarHorizons.TITANIUM_BLOCK.getDefaultState());
            if(Math.abs(u)==1||Math.abs(v)==1){
                if(!(v==-1&&u==0)&&!(damaged&&u==1&&v==1))
                    set(w,at(center,u,1,v,rotated),LunarHorizons.TITANIUM_BLOCK.getDefaultState());
            }
            if(!damaged||u!=1||v!=1)set(w,at(center,u,2,v,rotated),LunarHorizons.TITANIUM_BLOCK.getDefaultState());
        }
        loot(w,at(center,0,1,0,rotated),r);
        set(w,at(center,0,3,0,rotated),LunarHorizons.SURVEY_BEACON.getDefaultState());
        set(w,at(center,0,4,0,rotated),Blocks.LIGHTNING_ROD.getDefaultState());
        for(int side:new int[]{-1,1}){
            set(w,at(center,2*side,1,0,rotated),Blocks.IRON_BARS.getDefaultState());
            for(int u=3;u<=6;u++)for(int v=-1;v<=1;v++){
                boolean broken=damaged&&side==1&&u>=5;
                if(broken&&r.nextBoolean())continue;
                set(w,at(center,side*u,1,v,rotated),(broken?LunarHorizons.DAMAGED_SOLAR_PANEL:LunarHorizons.SOLAR_PANEL).getDefaultState());
            }
        }
        // Debris rests directly on the modified terrain; no unsupported loose blocks.
        for(int j=0;j<(damaged?10:5);j++){
            int dx=r.nextInt(13)-6,dz=r.nextInt(13)-6;
            if(dx*dx+dz*dz<16||dx*dx+dz*dz>42)continue;
            int y=w.getTopY(Heightmap.Type.WORLD_SURFACE_WG,x+dx,z+dz);
            BlockPos q=new BlockPos(x+dx,y,z+dz);
            if(w.isAir(q)&&ImpactCraterFeature.natural(w,q.down()))
                set(w,q,(r.nextBoolean()?LunarHorizons.DAMAGED_SOLAR_PANEL:LunarHorizons.TITANIUM_BLOCK).getDefaultState());
        }
        return true;
    }
    private static BlockPos at(BlockPos p,int u,int y,int v,boolean rotate){return rotate?p.add(-v,y,u):p.add(u,y,v);}
    private static BlockPos surfaceSite(StructureWorldAccess w,int x,int z,int radius){
        int high=Integer.MIN_VALUE,low=Integer.MAX_VALUE;
        for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            int top=w.getTopY(Heightmap.Type.WORLD_SURFACE_WG,x+dx,z+dz);
            high=Math.max(high,top);low=Math.min(low,top);
            // At least three natural blocks directly under every surface column.
            for(int h=1;h<=3;h++)if(!ImpactCraterFeature.natural(w,new BlockPos(x+dx,top-h,z+dz)))return null;
        }
        if(high-low>2||high<10||high>160)return null;
        BlockPos p=new BlockPos(x,high,z);
        for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++)for(int h=0;h<=5;h++)
            if(!w.isAir(p.add(dx,h,dz)))return null;
        return p;
    }
    private static boolean buriedSite(StructureWorldAccess w,BlockPos p,int radius){
        // All six faces plus a two-block natural buffer. Interior may be rock or ore,
        // but never open cave air or another structure. Ores in the room volume are
        // rejected too, to avoid deleting valuable deposits.
        int rim=radius+2;
        for(int dx=-rim;dx<=rim;dx++)for(int dz=-rim;dz<=rim;dz++)for(int h=-3;h<=7;h++){
            if(!ImpactCraterFeature.natural(w,p.add(dx,h,dz)))return false;
        }
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

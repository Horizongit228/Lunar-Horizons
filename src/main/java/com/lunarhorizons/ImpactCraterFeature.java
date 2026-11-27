package com.lunarhorizons;
import com.mojang.serialization.Codec;
import java.util.*;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;
public final class ImpactCraterFeature extends Feature<DefaultFeatureConfig>{
    private static final int GRID=144;
    public ImpactCraterFeature(Codec<DefaultFeatureConfig> codec){super(codec);}
    static boolean natural(StructureWorldAccess w,BlockPos p){
        var s=w.getBlockState(p);
        return s.isOf(LunarHorizons.MOON_ROCK)||s.isOf(LunarHorizons.REGOLITH)||s.isOf(LunarHorizons.HIGHLAND_ROCK)||s.isOf(LunarHorizons.FROSTED_REGOLITH)||s.isOf(LunarHorizons.DARK_REGOLITH)||s.isOf(LunarHorizons.BASALT_DUST)||s.isOf(LunarHorizons.ANORTHOSITE)||s.isOf(LunarHorizons.EJECTA_ROCK);
    }

    private record Crater(double x,double z,double rx,double rz,double depth,double phase,boolean meteor){}
    private static Crater crater(long seed,int gx,int gz){
        Random r=new Random(seed^((long)gx*341873128712L)^((long)gz*132897987541L)^0x435241544552L);
        if(r.nextDouble()>.68)return null;
        double radius=10+r.nextDouble()*25;
        if(r.nextInt(8)==0)radius=36+r.nextDouble()*4;
        double x=gx*(double)GRID+72+r.nextInt(29)-14,z=gz*(double)GRID+72+r.nextInt(29)-14;
        return new Crater(x,z,radius,radius*(.76+r.nextDouble()*.24),Math.max(3,radius*(.17+r.nextDouble()*.13)),r.nextDouble()*Math.PI*2,r.nextInt(5)==0);
    }
    private static double distance(Crater c,int x,int z){
        double dx=x-c.x,dz=z-c.z,cs=Math.cos(c.phase),sn=Math.sin(c.phase);
        double a=(dx*cs-dz*sn)/c.rx,b=(dx*sn+dz*cs)/c.rz,angle=Math.atan2(b,a);
        return Math.hypot(a,b)/(1+.065*Math.sin(angle*3+c.phase)+.035*Math.cos(angle*5-c.phase));
    }
    private static double smooth(double t){t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}
    /** Same deterministic displacement as the terrain feature, sampled without loading chunks. */
    static int estimateDelta(long seed,int x,int z){
        int gx=Math.floorDiv(x,GRID),gz=Math.floorDiv(z,GRID);
        Crater c=null;double t=1.5;
        for(int a=-1;a<=1;a++)for(int b=-1;b<=1;b++){
            Crater q=crater(seed,gx+a,gz+b);
            if(q==null)continue;double v=distance(q,x,z);
            if(v<t){c=q;t=v;}
        }
        if(c==null||t>1.28)return 0;
        double irregular=.5*Math.sin(x*.31+c.phase)*Math.cos(z*.27-c.phase);
        double displacement=-c.depth*(1-smooth((t-.20)/.80));
        if(t>.80)displacement+=Math.min(2.6,c.rx*.065)*Math.exp(-Math.pow((t-.99)/.13,2))*(.85+.15*Math.sin(Math.atan2(z-c.z,x-c.x)*4+c.phase));
        if(t<1.17)displacement+=irregular*(1-smooth((t-.80)/.37));
        if(t>1.13)displacement*=1-smooth((t-1.13)/.15);
        return (int)Math.round(displacement);
    }
    @Override public boolean generate(FeatureContext<DefaultFeatureConfig> ctx){
        StructureWorldAccess w=ctx.getWorld();int ox=ctx.getOrigin().getX()&~15,oz=ctx.getOrigin().getZ()&~15;
        List<Crater> list=new ArrayList<>();int gx=Math.floorDiv(ox,GRID),gz=Math.floorDiv(oz,GRID);
        for(int a=-1;a<=1;a++)for(int b=-1;b<=1;b++){Crater c=crater(w.getSeed(),gx+a,gz+b);if(c!=null)list.add(c);}
        boolean changed=false;
        for(int x=ox;x<ox+16;x++)for(int z=oz;z<oz+16;z++){
            Crater c=null;double t=1.5;
            for(Crater q:list){double v=distance(q,x,z);if(v<t){c=q;t=v;}}
            if(c==null||t>1.28)continue;
            double irregular=.5*Math.sin(x*.31+c.phase)*Math.cos(z*.27-c.phase);
            double displacement=-c.depth*(1-smooth((t-.20)/.80));
            if(t>.80)displacement+=Math.min(2.6,c.rx*.065)*Math.exp(-Math.pow((t-.99)/.13,2))*(.85+.15*Math.sin(Math.atan2(z-c.z,x-c.x)*4+c.phase));
            if(t<1.17)displacement+=irregular*(1-smooth((t-.80)/.37));
            if(t>1.13)displacement*=1-smooth((t-1.13)/.15);
            int delta=(int)Math.round(displacement);
            int top=w.getTopY(Heightmap.Type.WORLD_SURFACE_WG,x,z);
            if(top<0||top>170)continue;
            BlockPos surface=new BlockPos(x,top-1,z);
            if(!natural(w,surface))continue;
            BlockState original=w.getBlockState(surface);
            boolean valid=true;
            if(delta<0)for(int h=0;h<=-delta;h++){if(!natural(w,surface.down(h))){valid=false;break;}}
            if(delta>0)for(int h=1;h<=delta;h++){if(!w.isAir(surface.up(h))){valid=false;break;}}
            if(!valid)continue;
            if(delta<0)for(int h=0;h<-delta;h++)w.setBlockState(surface.down(h),Blocks.AIR.getDefaultState(),2);
            if(delta>0)for(int h=1;h<=delta;h++)w.setBlockState(surface.up(h),original,2);
            BlockPos floor=surface.up(delta);
            double blend=.86+.11*Math.sin(x*.73+z*.37+c.phase)+.06*Math.cos(z*.51);
            if(t<blend&&natural(w,floor)){
                BlockState material=c.meteor&&t<.075?LunarHorizons.METEORITE_ROCK.getDefaultState():LunarHorizons.EJECTA_ROCK.getDefaultState();
                w.setBlockState(floor,material,2);changed=true;
            }else if(delta!=0&&natural(w,floor))w.setBlockState(floor,original,2);
            changed|=delta!=0;
        }
        return changed;
    }
}

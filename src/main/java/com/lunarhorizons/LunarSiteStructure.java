package com.lunarhorizons;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

/** Vanilla structure starts, with one persistent, chunk-local piece per occupied chunk. */
public final class LunarSiteStructure extends Structure {
    public static final MapCodec<LunarSiteStructure> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
        configCodecBuilder(i),Codec.intRange(0,19).fieldOf("site").forGetter(s->s.kind)
    ).apply(i,LunarSiteStructure::new));
    private final int kind;
    public LunarSiteStructure(Config config,int kind){super(config);this.kind=kind;}
    @Override public StructureType<?> getType(){return LunarHorizons.LUNAR_SITE_TYPE;}
    @Override protected Optional<StructurePosition> getStructurePosition(Context context){
        // Plan once, before any blocks are written. No reads of neighboring live chunks.
        ExplorationSiteFeature.Plan plan=ExplorationSiteFeature.prepare(kind,context);
        if(plan==null||plan.blocks.isEmpty())return Optional.empty();
        Map<Long,Map<BlockPos,BlockState>> chunks=new LinkedHashMap<>();
        for(var e:plan.blocks.entrySet()){
            BlockPos p=e.getKey();
            long key=ChunkPos.toLong(p.getX()>>4,p.getZ()>>4);
            chunks.computeIfAbsent(key,k->new LinkedHashMap<>()).put(p,e.getValue());
        }
        List<LunarSitePiece> pieces=new ArrayList<>(chunks.size());
        for(var e:chunks.entrySet()){
            Map<BlockPos,Integer> loot=new LinkedHashMap<>();
            for(var c:plan.chests.entrySet())if(e.getValue().containsKey(c.getKey()))loot.put(c.getKey(),c.getValue());
            pieces.add(new LunarSitePiece(e.getValue(),loot,plan.origin.getY(),kind,context.seed()));
        }
        return Optional.of(new StructurePosition(plan.origin,collector->pieces.forEach(collector::addPiece)));
    }
}

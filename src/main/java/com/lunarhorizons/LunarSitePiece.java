package com.lunarhorizons;

import java.util.*;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.*;
import net.minecraft.registry.Registries;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

/** A single occupied chunk's palette and packed block positions, saved in the structure start NBT. */
public final class LunarSitePiece extends StructurePiece {
    private final List<BlockState> palette;
    private final int[] cells,loot;
    private final int baseY,kind,cx,cz;
    private final long seed;
    private static BlockBox bounds(Map<BlockPos,BlockState> blocks){
        int x=Integer.MAX_VALUE,y=Integer.MAX_VALUE,z=Integer.MAX_VALUE,X=Integer.MIN_VALUE,Y=Integer.MIN_VALUE,Z=Integer.MIN_VALUE;
        for(BlockPos p:blocks.keySet()){
            x=Math.min(x,p.getX());y=Math.min(y,p.getY());z=Math.min(z,p.getZ());
            X=Math.max(X,p.getX());Y=Math.max(Y,p.getY());Z=Math.max(Z,p.getZ());
        }
        // Include terrain support below the floor in this piece's vertical extent.
        return new BlockBox(x,Math.max(-60,y-40),z,X,Y,Z);
    }
    private static int pack(BlockPos p){return ((p.getY()+64)<<8)|((p.getZ()&15)<<4)|(p.getX()&15);}
    private BlockPos unpack(int n){return new BlockPos(cx+(n&15),(n>>>8)-64,cz+((n>>>4)&15));}
    LunarSitePiece(Map<BlockPos,BlockState> blocks,Map<BlockPos,Integer> chests,int baseY,int kind,long seed){
        super(LunarHorizons.LUNAR_SITE_PIECE,0,bounds(blocks));
        setOrientation(null);
        this.cx=getBoundingBox().getMinX()&~15;this.cz=getBoundingBox().getMinZ()&~15;
        this.baseY=baseY;this.kind=kind;this.seed=seed;
        Map<BlockState,Integer> ids=new LinkedHashMap<>();
        cells=new int[blocks.size()*2];int i=0;
        for(var e:blocks.entrySet()){
            cells[i++]=pack(e.getKey());
            cells[i++]=ids.computeIfAbsent(e.getValue(),s->ids.size());
        }
        palette=List.copyOf(ids.keySet());
        loot=new int[chests.size()*2];i=0;
        for(var e:chests.entrySet()){loot[i++]=pack(e.getKey());loot[i++]=e.getValue();}
    }
    public LunarSitePiece(NbtCompound nbt){
        super(LunarHorizons.LUNAR_SITE_PIECE,nbt);
        cx=nbt.getInt("CX");cz=nbt.getInt("CZ");baseY=nbt.getInt("Floor");kind=nbt.getInt("Site");seed=nbt.getLong("Seed");
        cells=nbt.getIntArray("Cells");loot=nbt.getIntArray("Loot");
        ArrayList<BlockState> states=new ArrayList<>();
        NbtList list=nbt.getList("Palette",10);
        for(int i=0;i<list.size();i++)states.add(NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(),list.getCompound(i)));
        palette=List.copyOf(states);
    }
    @Override protected void writeNbt(StructureContext context,NbtCompound nbt){
        nbt.putInt("CX",cx);nbt.putInt("CZ",cz);nbt.putInt("Floor",baseY);nbt.putInt("Site",kind);nbt.putLong("Seed",seed);
        nbt.putIntArray("Cells",cells);nbt.putIntArray("Loot",loot);
        NbtList list=new NbtList();
        for(BlockState state:palette)list.add(NbtHelper.fromBlockState(state));
        nbt.put("Palette",list);
    }
    void visitCandidates(List<BlockPos> out,BlockPos center){
        Map<Integer,BlockState> states=new HashMap<>();
        for(int i=0;i+1<cells.length;i+=2){
            int index=cells[i+1];if(index>=0&&index<palette.size())states.put(cells[i],palette.get(index));
        }
        for(var e:states.entrySet()){
            int n=e.getKey();
            if(!e.getValue().isAir())continue;
            BlockState below=states.get(n-256),above=states.get(n+256);
            if(below==null||below.isAir()||above==null||!above.isAir())continue;
            BlockPos pos=unpack(n);
            if(Math.abs(pos.getX()-center.getX())<=16&&Math.abs(pos.getZ()-center.getZ())<=16)out.add(pos);
        }
    }
    @Override public void generate(StructureWorldAccess world,StructureAccessor accessor,ChunkGenerator generator,Random random,BlockBox box,ChunkPos chunk,BlockPos pivot){
        // The native structure engine may call this piece while visiting nearby chunks.
        if(chunk.getStartX()!=cx||chunk.getStartZ()!=cz)return;
        // First write every planned cell inside the current chunk. Never access a neighbor chunk.
        for(int i=0;i+1<cells.length;i+=2){
            BlockPos p=unpack(cells[i]);int index=cells[i+1];
            if(index<0||index>=palette.size()||!box.contains(p))continue;
            BlockState s=palette.get(index);
            world.setBlockState(p,s,2);
        }
        // Reconcile actual terrain after crater and cave features. Lowest floor per column
        // is supported down to existing solid rock, at most to the protected bedrock level.
        if(kind!=3){
            int[] floors=new int[256];Arrays.fill(floors,Integer.MAX_VALUE);
            for(int i=0;i+1<cells.length;i+=2){
                int index=cells[i+1];if(index<0||index>=palette.size())continue;
                BlockPos p=unpack(cells[i]);BlockState s=palette.get(index);
                if(p.getY()>baseY||s.isAir()||!s.isFullCube(world,p))continue;
                int col=cells[i]&255;floors[col]=Math.min(floors[col],p.getY());
            }
            for(int col=0;col<256;col++){
                if(floors[col]==Integer.MAX_VALUE)continue;
                BlockPos p=new BlockPos(cx+(col&15),floors[col]-1,cz+(col>>>4));
                while(p.getY()>=Math.max(-60,floors[col]-40)&&box.contains(p)){
                    BlockState old=world.getBlockState(p);
                    if(old.isFullCube(world,p))break;
                    world.setBlockState(p,LunarHorizons.MOON_BRICKS.getDefaultState(),2);p=p.down();
                }
            }
        }
        // Seed the rare facility with finite startup supplies. Never modify old sites.
        if(kind==19)for(int i=0;i+1<cells.length;i+=2){
            BlockPos p=unpack(cells[i]);if(!box.contains(p))continue;
            if(world.getBlockEntity(p) instanceof ColonyBlockEntity be){
                switch(be.kind()){
                    case BATTERY -> be.energy=be.capacity()/2;
                    case OXYGEN -> be.input=16;
                    case GENERATOR -> be.input=8;
                    case SERVER -> {if(Math.floorMod(p.asLong()^seed,4)==0)be.input=1;}
                    default -> {}
                }
                be.markDirty();
            }
        }
        // Stable per-chest loot stream, independent of chunk visitation order.
        for(int i=0;i+1<loot.length;i+=2){
            BlockPos p=unpack(loot[i]);if(box.contains(p)&&world.getBlockState(p).isOf(Blocks.CHEST))
                ExplorationSiteFeature.loot(world,p,Random.create(seed^p.asLong()^0x4c554e41524cL),loot[i+1]);
        }
    }
}

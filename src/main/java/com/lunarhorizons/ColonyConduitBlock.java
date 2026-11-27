package com.lunarhorizons;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.server.world.ServerWorld;

/** Six independently connected arms, with matching collision and selection boxes. */
public final class ColonyConduitBlock extends Block {
    private static final BooleanProperty[] SIDES={Properties.DOWN,Properties.UP,Properties.NORTH,Properties.SOUTH,Properties.WEST,Properties.EAST};
    private final boolean oxygen;
    private final VoxelShape[] shapes=new VoxelShape[64];
    public ColonyConduitBlock(Settings settings,boolean oxygen){this(settings,oxygen,oxygen?5.5:6.5);}
    public ColonyConduitBlock(Settings settings,boolean oxygen,double lo){
        super(settings);this.oxygen=oxygen;
        BlockState s=getStateManager().getDefaultState();for(BooleanProperty p:SIDES)s=s.with(p,false);setDefaultState(s);
        double hi=16-lo;
        // Electrical lines share a low axis that intersects the frame of a 2/16 or 3/16 solar panel.
        double yLo=oxygen?lo:1.5-(8-lo),yHi=oxygen?hi:1.5+(8-lo);
        for(int mask=0;mask<64;mask++){
            VoxelShape shape=createCuboidShape(lo,yLo,lo,hi,yHi,hi);
            for(Direction d:Direction.values())if((mask&(1<<d.getId()))!=0){
                double x1=d==Direction.WEST?0:lo,x2=d==Direction.EAST?16:hi;
                double y1=d==Direction.DOWN?0:yLo,y2=d==Direction.UP?16:yHi;
                double z1=d==Direction.NORTH?0:lo,z2=d==Direction.SOUTH?16:hi;
                shape=VoxelShapes.union(shape,createCuboidShape(x1,y1,z1,x2,y2,z2));
            }
            shapes[mask]=shape;
        }
    }
    @Override protected void appendProperties(StateManager.Builder<Block,BlockState> b){b.add(SIDES);}
    boolean connects(BlockState s){
        if(oxygen)return s.isOf(LunarHorizons.OXYGEN_PIPE)||s.isOf(LunarHorizons.OXYGEN_VENT)||s.isOf(LunarHorizons.OXYGEN_GENERATOR);
        return powerPort(s);
    }
    static boolean powerPort(BlockState s){
        return s.isOf(LunarHorizons.POWER_WIRE)||s.isOf(LunarHorizons.POWER_CABLE)||s.getBlock() instanceof ColonyMachineBlock||s.isOf(LunarHorizons.SOLAR_PANEL)||s.isOf(LunarHorizons.DAMAGED_SOLAR_PANEL)||s.isOf(LunarHorizons.ADVANCED_SOLAR_PANEL)||s.isOf(LunarHorizons.POWER_SWITCH)||s.isOf(LunarHorizons.POWER_FEEDTHROUGH);
    }
    private BlockState connections(BlockState s,WorldView w,BlockPos p){
        for(Direction d:Direction.values())s=s.with(SIDES[d.getId()],connects(w.getBlockState(p.offset(d))));return s;
    }
    @Override public BlockState getPlacementState(ItemPlacementContext ctx){return connections(getDefaultState(),ctx.getWorld(),ctx.getBlockPos());}
    @Override protected BlockState getStateForNeighborUpdate(BlockState s,Direction d,BlockState neighbor,WorldAccess w,BlockPos pos,BlockPos other){
        return s.with(SIDES[d.getId()],connects(neighbor));
    }
    static void refresh(ServerWorld w,BlockPos p,BlockState s){
        if(!(s.getBlock() instanceof ColonyConduitBlock pipe))return;
        BlockState next=s;
        for(Direction d:Direction.values()){
            BlockPos q=p.offset(d);
            if(ColonyGrid.loaded(w,q))next=next.with(SIDES[d.getId()],pipe.connects(w.getBlockState(q)));
        }
        if(next!=s)w.setBlockState(p,next,Block.NOTIFY_LISTENERS);
    }
    private VoxelShape shape(BlockState s){int mask=0;for(int i=0;i<6;i++)if(s.get(SIDES[i]))mask|=1<<i;return shapes[mask];}
    @Override protected VoxelShape getOutlineShape(BlockState s,BlockView w,BlockPos p,ShapeContext c){return shape(s);}
    @Override protected VoxelShape getCollisionShape(BlockState s,BlockView w,BlockPos p,ShapeContext c){return shape(s);}
    @Override protected BlockState rotate(BlockState s,net.minecraft.util.BlockRotation r){BlockState out=s;for(Direction d:Direction.values())out=out.with(SIDES[r.rotate(d).getId()],s.get(SIDES[d.getId()]));return out;}
    @Override protected BlockState mirror(BlockState s,net.minecraft.util.BlockMirror m){BlockState out=s;for(Direction d:Direction.values())out=out.with(SIDES[m.apply(d).getId()],s.get(SIDES[d.getId()]));return out;}
}

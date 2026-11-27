package com.lunarhorizons;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;

public final class SolarPanelBlock extends Block {
    private static final VoxelShape NORMAL=createCuboidShape(0,0,0,16,3,16);
    private static final VoxelShape DAMAGED=createCuboidShape(0,0,0,16,2,16);
    private static final VoxelShape NORMAL_PORT=VoxelShapes.union(NORMAL,createCuboidShape(6.5,3,6.5,9.5,16,9.5));
    private static final VoxelShape DAMAGED_PORT=VoxelShapes.union(DAMAGED,createCuboidShape(6.5,2,6.5,9.5,16,9.5));
    private final boolean damaged;
    public SolarPanelBlock(Settings settings,boolean damaged){
        super(settings);this.damaged=damaged;
        setDefaultState(getStateManager().getDefaultState().with(Properties.UP,false));
    }
    @Override protected void appendProperties(StateManager.Builder<Block,BlockState> builder){builder.add(Properties.UP);}
    @Override public BlockState getPlacementState(ItemPlacementContext ctx){
        return getDefaultState().with(Properties.UP,ColonyConduitBlock.powerPort(ctx.getWorld().getBlockState(ctx.getBlockPos().up())));
    }
    @Override protected BlockState getStateForNeighborUpdate(BlockState state,Direction direction,BlockState neighbor,WorldAccess world,BlockPos pos,BlockPos other){
        return direction==Direction.UP?state.with(Properties.UP,ColonyConduitBlock.powerPort(neighbor)):state;
    }
    static void refresh(ServerWorld w,BlockPos pos,BlockState state){
        if(!(state.getBlock() instanceof SolarPanelBlock)||!ColonyGrid.loaded(w,pos.up()))return;
        BlockState next=state.with(Properties.UP,ColonyConduitBlock.powerPort(w.getBlockState(pos.up())));
        if(next!=state)w.setBlockState(pos,next,Block.NOTIFY_LISTENERS);
    }
    private VoxelShape shape(BlockState state){return state.get(Properties.UP)?(damaged?DAMAGED_PORT:NORMAL_PORT):(damaged?DAMAGED:NORMAL);}
    @Override protected VoxelShape getOutlineShape(BlockState state,BlockView world,BlockPos pos,ShapeContext context){return shape(state);}
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockView world,BlockPos pos,ShapeContext context){return shape(state);}
}

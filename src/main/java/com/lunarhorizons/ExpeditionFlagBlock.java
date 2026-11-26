package com.lunarhorizons;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

/** Rigid, vacuum-compatible expedition flag mounted on a metal post. */
public final class ExpeditionFlagBlock extends Block {
    public static final DirectionProperty FACING=Properties.HORIZONTAL_FACING;
    private static final VoxelShape NS=Block.createCuboidShape(1,0,6,15,16,10);
    private static final VoxelShape EW=Block.createCuboidShape(6,0,1,10,16,15);
    public ExpeditionFlagBlock(Settings settings){
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING,Direction.NORTH));
    }
    @Override protected void appendProperties(StateManager.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override public BlockState getPlacementState(ItemPlacementContext ctx){
        BlockState state=getDefaultState().with(FACING,ctx.getHorizontalPlayerFacing().getOpposite());
        return state.canPlaceAt(ctx.getWorld(),ctx.getBlockPos())?state:null;
    }
    @Override protected boolean canPlaceAt(BlockState state,WorldView world,BlockPos pos){
        return world.getBlockState(pos.down()).isSideSolidFullSquare(world,pos.down(),Direction.UP);
    }
    @Override protected BlockState getStateForNeighborUpdate(BlockState state,Direction direction,BlockState neighborState,WorldAccess world,BlockPos pos,BlockPos neighborPos){
        return direction==Direction.DOWN&&!state.canPlaceAt(world,pos)?Blocks.AIR.getDefaultState():super.getStateForNeighborUpdate(state,direction,neighborState,world,pos,neighborPos);
    }
    @Override protected VoxelShape getOutlineShape(BlockState state,BlockView world,BlockPos pos,ShapeContext context){
        return state.get(FACING).getAxis()==Direction.Axis.Z?NS:EW;
    }
    @Override protected BlockState rotate(BlockState state,BlockRotation rotation){return state.with(FACING,rotation.rotate(state.get(FACING)));}
    @Override protected BlockState mirror(BlockState state,BlockMirror mirror){return state.rotate(mirror.getRotation(state.get(FACING)));}
}

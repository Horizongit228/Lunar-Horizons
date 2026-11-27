package com.lunarhorizons;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
public final class SolarPanelBlock extends Block {
    private static final VoxelShape NORMAL=createCuboidShape(0,0,0,16,3,16);
    private static final VoxelShape DAMAGED=createCuboidShape(0,0,0,16,2,16);
    private final boolean damaged;
    public SolarPanelBlock(Settings settings,boolean damaged){super(settings);this.damaged=damaged;}
    @Override protected VoxelShape getOutlineShape(BlockState state,BlockView world,BlockPos pos,ShapeContext context){return damaged?DAMAGED:NORMAL;}
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockView world,BlockPos pos,ShapeContext context){return damaged?DAMAGED:NORMAL;}
}

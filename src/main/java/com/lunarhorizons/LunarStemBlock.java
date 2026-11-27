package com.lunarhorizons;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

/** Approximate the three visible stalks with narrow collision boxes, not a full cube. */
public final class LunarStemBlock extends Block {
    private static final VoxelShape SHAPE=VoxelShapes.union(
        createCuboidShape(2,0,5,5,5,7),
        createCuboidShape(0,5,5,3.5,10,7),
        createCuboidShape(7,0,7,9,15,9),
        createCuboidShape(11,0,10,14,5,12),
        createCuboidShape(13,5,10,16,9,12),
        createCuboidShape(7,8,7,9,12,12));
    public LunarStemBlock(Settings settings){super(settings);}
    @Override protected VoxelShape getOutlineShape(BlockState state,BlockView world,BlockPos pos,ShapeContext context){return SHAPE;}
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockView world,BlockPos pos,ShapeContext context){return SHAPE;}
}

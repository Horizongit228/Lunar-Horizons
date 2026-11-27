package com.lunarhorizons;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

/** Physical bounds generated from every cuboid in crystal_cluster.json. */
public final class LunarCrystalClusterBlock extends Block {
    private static final VoxelShape SHAPE=VoxelShapes.union(
        createCuboidShape(2,0,2,14,2,14),
        createCuboidShape(2.5,1,3.5,5.5,9,6.5),
        createCuboidShape(3,9,4,5,10,6),
        createCuboidShape(3.5,10,4.5,4.5,11,5.5),
        createCuboidShape(6.5,1,6.5,9.5,13,9.5),
        createCuboidShape(7,13,7,9,14,9),
        createCuboidShape(7.5,14,7.5,8.5,15,8.5),
        createCuboidShape(9.5,1,2.5,12.5,6,5.5),
        createCuboidShape(10,6,3,12,7,5),
        createCuboidShape(10.5,7,3.5,11.5,8,4.5),
        createCuboidShape(2.5,1,9.5,5.5,5,12.5),
        createCuboidShape(3,5,10,5,6,12),
        createCuboidShape(3.5,6,10.5,4.5,7,11.5),
        createCuboidShape(10.5,1,9.5,13.5,8,12.5),
        createCuboidShape(11,8,10,13,9,12),
        createCuboidShape(11.5,9,10.5,12.5,10,11.5));
    public LunarCrystalClusterBlock(Settings settings){super(settings);}
    @Override protected VoxelShape getOutlineShape(BlockState state,BlockView world,BlockPos pos,ShapeContext context){return SHAPE;}
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockView world,BlockPos pos,ShapeContext context){return SHAPE;}
}

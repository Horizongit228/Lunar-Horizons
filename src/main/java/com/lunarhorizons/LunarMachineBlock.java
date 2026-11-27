package com.lunarhorizons;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

/** Physical bounds match the inset model; non-opaque registration preserves adjacent faces. */
public final class LunarMachineBlock extends Block {
    private static final VoxelShape STATION=VoxelShapes.union(
        createCuboidShape(0,0,0,16,2,16),createCuboidShape(1,2,1,15,14,15),
        createCuboidShape(0,14,0,16,16,16),
        createCuboidShape(0,2,0,1,14,1),createCuboidShape(15,2,0,16,14,1),
        createCuboidShape(0,2,15,1,14,16),createCuboidShape(15,2,15,16,14,16));
    private static final VoxelShape LANTERN=VoxelShapes.union(
        createCuboidShape(0,0,0,16,2,16),createCuboidShape(1,2,1,15,14,15),
        createCuboidShape(0,14,0,16,16,16),
        createCuboidShape(0,2,0,2,14,2),createCuboidShape(14,2,0,16,14,2),
        createCuboidShape(0,2,14,2,14,16),createCuboidShape(14,2,14,16,14,16));
    private static final VoxelShape BEACON=VoxelShapes.union(
        createCuboidShape(0,0,0,16,3,16),createCuboidShape(1,3,1,15,12,15),
        createCuboidShape(2,12,2,14,14,14),createCuboidShape(5,14,5,11,16,11));
    private final VoxelShape shape;
    public LunarMachineBlock(Settings settings,int kind){super(settings);shape=kind==0?STATION:kind==1?LANTERN:BEACON;}
    @Override protected VoxelShape getOutlineShape(BlockState state,BlockView world,BlockPos pos,ShapeContext context){return shape;}
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockView world,BlockPos pos,ShapeContext context){return shape;}
}

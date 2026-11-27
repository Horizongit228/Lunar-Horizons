package com.lunarhorizons;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
public final class ColonyAirlockBlock extends Block {
    public ColonyAirlockBlock(Settings settings){super(settings);setDefaultState(getStateManager().getDefaultState().with(Properties.OPEN,false));}
    @Override protected void appendProperties(StateManager.Builder<Block,BlockState> b){b.add(Properties.OPEN);}
    @Override protected VoxelShape getCollisionShape(BlockState s,BlockView w,BlockPos p,ShapeContext c){return s.get(Properties.OPEN)?VoxelShapes.empty():VoxelShapes.fullCube();}
    @Override protected VoxelShape getOutlineShape(BlockState s,BlockView w,BlockPos p,ShapeContext c){return s.get(Properties.OPEN)?createCuboidShape(0,0,0,2,16,16):VoxelShapes.fullCube();}
    static void toggle(ServerWorld w,BlockPos p,ServerPlayerEntity player){
        if(!w.canPlayerModifyAt(player,p))return;
        BlockState s=w.getBlockState(p);boolean open=!s.get(Properties.OPEN);
        w.setBlockState(p,s.with(Properties.OPEN,open),3);
        // Paired vertical blocks make a two-block-high passage.
        for(BlockPos q:new BlockPos[]{p.up(),p.down()}){BlockState other=w.getBlockState(q);if(other.isOf(LunarHorizons.AIRLOCK)&&other.get(Properties.OPEN)!=open)w.setBlockState(q,other.with(Properties.OPEN,open),3);}
    }
}

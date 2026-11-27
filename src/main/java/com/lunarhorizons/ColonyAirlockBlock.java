package com.lunarhorizons;
import net.minecraft.block.Block;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;

public final class ColonyAirlockBlock extends Block {
    private static final VoxelShape NS_CLOSED=createCuboidShape(0,0,6,16,16,10);
    private static final VoxelShape EW_CLOSED=createCuboidShape(6,0,0,10,16,16);
    private static final VoxelShape NS_OPEN=VoxelShapes.union(createCuboidShape(0,0,6,2,16,10),createCuboidShape(14,0,6,16,16,10));
    private static final VoxelShape EW_OPEN=VoxelShapes.union(createCuboidShape(6,0,0,10,16,2),createCuboidShape(6,0,14,10,16,16));
    public ColonyAirlockBlock(Settings settings){super(settings);setDefaultState(getStateManager().getDefaultState().with(Properties.OPEN,false).with(Properties.HORIZONTAL_FACING,Direction.NORTH).with(Properties.DOUBLE_BLOCK_HALF,DoubleBlockHalf.LOWER));}
    @Override protected void appendProperties(StateManager.Builder<Block,BlockState> b){b.add(Properties.OPEN,Properties.HORIZONTAL_FACING,Properties.DOUBLE_BLOCK_HALF);}
    @Override public BlockState getPlacementState(ItemPlacementContext ctx){
        BlockState below=ctx.getWorld().getBlockState(ctx.getBlockPos().down());
        if(below.isOf(this))return getDefaultState().with(Properties.HORIZONTAL_FACING,below.get(Properties.HORIZONTAL_FACING)).with(Properties.OPEN,below.get(Properties.OPEN)).with(Properties.DOUBLE_BLOCK_HALF,DoubleBlockHalf.UPPER);
        return getDefaultState().with(Properties.HORIZONTAL_FACING,ctx.getHorizontalPlayerFacing().getOpposite());
    }
    private VoxelShape shape(BlockState s){boolean ns=s.get(Properties.HORIZONTAL_FACING).getAxis()==Direction.Axis.Z;return s.get(Properties.OPEN)?(ns?NS_OPEN:EW_OPEN):(ns?NS_CLOSED:EW_CLOSED);}
    @Override protected VoxelShape getCollisionShape(BlockState s,BlockView w,BlockPos p,ShapeContext c){return shape(s);}
    @Override protected VoxelShape getOutlineShape(BlockState s,BlockView w,BlockPos p,ShapeContext c){return shape(s);}
    @Override protected BlockState rotate(BlockState s,net.minecraft.util.BlockRotation r){return s.with(Properties.HORIZONTAL_FACING,r.rotate(s.get(Properties.HORIZONTAL_FACING)));}
    @Override protected BlockState mirror(BlockState s,net.minecraft.util.BlockMirror m){return s.rotate(m.getRotation(s.get(Properties.HORIZONTAL_FACING)));}
    @Override protected ActionResult onUse(BlockState s,World w,BlockPos p,PlayerEntity player,BlockHitResult hit){
        if(player.isSpectator())return ActionResult.PASS;
        if(w.isClient)return ActionResult.SUCCESS;
        boolean rotate=player.isSneaking()&&player.getMainHandStack().isEmpty();
        Direction facing=rotate?s.get(Properties.HORIZONTAL_FACING).rotateYClockwise():s.get(Properties.HORIZONTAL_FACING);
        boolean open=rotate?s.get(Properties.OPEN):!s.get(Properties.OPEN);
        // Keep the existing two separately crafted blocks compatible with old worlds.
        BlockPos mate=w.getBlockState(p.down()).isOf(this)?p.down():p.up();
        if(w.getBlockState(mate).isOf(this)){
            BlockState other=w.getBlockState(mate);
            w.setBlockState(mate,other.with(Properties.OPEN,open).with(Properties.HORIZONTAL_FACING,facing).with(Properties.DOUBLE_BLOCK_HALF,mate.getY()>p.getY()?DoubleBlockHalf.UPPER:DoubleBlockHalf.LOWER),3);
        }
        w.setBlockState(p,s.with(Properties.OPEN,open).with(Properties.HORIZONTAL_FACING,facing).with(Properties.DOUBLE_BLOCK_HALF,w.getBlockState(p.down()).isOf(this)?DoubleBlockHalf.UPPER:DoubleBlockHalf.LOWER),3);
        w.playSound(null,p,open?SoundEvents.BLOCK_IRON_DOOR_OPEN:SoundEvents.BLOCK_IRON_DOOR_CLOSE,SoundCategory.BLOCKS,.8f,1f);
        return ActionResult.SUCCESS;
    }
}

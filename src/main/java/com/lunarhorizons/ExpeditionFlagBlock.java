package com.lunarhorizons;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

/** 2x2 flag. Part 0 is the pole base, 1 lower cloth, 2 upper pole, 3 upper cloth.
 * Part 4 is a legacy single-block flag, kept for old saves. */
public final class ExpeditionFlagBlock extends Block {
    public static final DirectionProperty FACING=Properties.HORIZONTAL_FACING;
    public static final IntProperty PART=IntProperty.of("part",0,4);
    private static final VoxelShape[] NORTH={
        VoxelShapes.union(createCuboidShape(0,0,5,6,1.5,11),createCuboidShape(2,1.5,7,4,16,9),createCuboidShape(4,11,7,16,16,9)),
        createCuboidShape(0,11,7,16,16,9),
        VoxelShapes.union(createCuboidShape(2,0,7,4,16,9),createCuboidShape(4,0,7,16,15,9)),
        createCuboidShape(0,0,7,16,15,9),
        createCuboidShape(1,0,6,15,16,10)
    };
    public ExpeditionFlagBlock(Settings settings){
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING,Direction.NORTH).with(PART,4));
    }
    @Override protected void appendProperties(StateManager.Builder<Block,BlockState> builder){builder.add(FACING,PART);}
    private static BlockPos at(BlockPos root,Direction facing,int part){
        return root.offset(facing.rotateYClockwise(),part&1).up(part>>1);
    }
    private static BlockPos root(BlockPos pos,BlockState state){
        int part=state.get(PART);
        return part==4?pos:pos.offset(state.get(FACING).rotateYClockwise().getOpposite(),part&1).down(part>>1);
    }
    private boolean matches(WorldView world,BlockPos pos,Direction facing,int part){
        BlockState s=world.getBlockState(pos);
        return s.isOf(this)&&s.get(FACING)==facing&&s.get(PART)==part;
    }
    private static boolean supported(WorldView world,BlockPos pos){
        return world.getBlockState(pos.down()).isSideSolidFullSquare(world,pos.down(),Direction.UP);
    }
    @Override public BlockState getPlacementState(ItemPlacementContext ctx){
        BlockPos pos=ctx.getBlockPos();Direction facing=ctx.getHorizontalPlayerFacing().getOpposite();
        World world=ctx.getWorld();
        if(pos.getY()+1>=world.getTopY()||!supported(world,pos))return null;
        for(int i=1;i<4;i++){
            BlockPos p=at(pos,facing,i);
            if(!world.getWorldBorder().contains(p)||!world.getBlockState(p).isAir())return null;
        }
        return getDefaultState().with(FACING,facing).with(PART,0);
    }
    @Override public void onPlaced(World world,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack){
        if(!world.isClient&&state.get(PART)==0){
            for(int i=1;i<4;i++)world.setBlockState(at(pos,state.get(FACING),i),state.with(PART,i),Block.NOTIFY_LISTENERS);
        }
    }
    @Override protected boolean canPlaceAt(BlockState state,WorldView world,BlockPos pos){
        return state.get(PART)!=0&&state.get(PART)!=4||supported(world,pos);
    }
    @Override protected BlockState getStateForNeighborUpdate(BlockState state,Direction direction,BlockState neighborState,WorldAccess world,BlockPos pos,BlockPos neighborPos){
        int part=state.get(PART);Direction facing=state.get(FACING);
        if((part==0||part==4)&&direction==Direction.DOWN&&!supported(world,pos))return Blocks.AIR.getDefaultState();
        if(part!=4){
            BlockPos base=root(pos,state);
            for(int i=0;i<4;i++){
                if(i==part)continue;
                BlockPos other=at(base,facing,i);
                if(other.equals(neighborPos)&&!matches(world,other,facing,i))return Blocks.AIR.getDefaultState();
            }
        }
        return super.getStateForNeighborUpdate(state,direction,neighborState,world,pos,neighborPos);
    }
    @Override public BlockState onBreak(World world,BlockPos pos,BlockState state,PlayerEntity player){
        int part=state.get(PART);
        if(!world.isClient&&part!=0&&part!=4){
            BlockPos base=root(pos,state);
            if(matches(world,base,state.get(FACING),0))world.breakBlock(base,!player.isCreative(),player);
        }
        return super.onBreak(world,pos,state,player);
    }
    @Override protected VoxelShape getOutlineShape(BlockState state,BlockView world,BlockPos pos,ShapeContext context){
        VoxelShape source=NORTH[state.get(PART)];
        int turns=switch(state.get(FACING)){case EAST->1;case SOUTH->2;case WEST->3;default->0;};
        VoxelShape[] result={VoxelShapes.empty()};
        source.forEachBox((x1,y1,z1,x2,y2,z2)->{
            double a=x1,b=z1,c=x2,d=z2;
            for(int i=0;i<turns;i++){double na=1-d,nb=a,nc=1-b,nd=c;a=na;b=nb;c=nc;d=nd;}
            result[0]=VoxelShapes.union(result[0],VoxelShapes.cuboid(a,y1,b,c,y2,d));
        });
        return result[0];
    }
    @Override protected BlockState rotate(BlockState state,BlockRotation rotation){return state.with(FACING,rotation.rotate(state.get(FACING)));}
    @Override protected BlockState mirror(BlockState state,BlockMirror mirror){return state.rotate(mirror.getRotation(state.get(FACING)));}
}

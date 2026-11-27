package com.lunarhorizons;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
public final class ColonyMachineBlock extends Block implements BlockEntityProvider {
    final ColonyBlockEntity.Kind kind;
    public ColonyMachineBlock(Settings settings,ColonyBlockEntity.Kind kind){
        super(settings);this.kind=kind;setDefaultState(getStateManager().getDefaultState().with(Properties.LIT,false).with(Properties.HORIZONTAL_FACING,net.minecraft.util.math.Direction.NORTH).with(Properties.UP,false));
    }
    @Override protected void appendProperties(StateManager.Builder<Block,BlockState> builder){builder.add(Properties.LIT,Properties.HORIZONTAL_FACING,Properties.UP);}
    @Override public BlockState getPlacementState(net.minecraft.item.ItemPlacementContext ctx){
        return getDefaultState().with(Properties.HORIZONTAL_FACING,ctx.getHorizontalPlayerFacing().getOpposite()).with(Properties.UP,topConnection(ctx.getWorld().getBlockState(ctx.getBlockPos().up())));
    }
    @Override protected BlockState rotate(BlockState s,net.minecraft.util.BlockRotation r){return s.with(Properties.HORIZONTAL_FACING,r.rotate(s.get(Properties.HORIZONTAL_FACING)));}
    @Override protected BlockState mirror(BlockState s,net.minecraft.util.BlockMirror m){return s.rotate(m.getRotation(s.get(Properties.HORIZONTAL_FACING)));}
    private boolean topConnection(BlockState s){
        return s.isOf(LunarHorizons.POWER_CABLE)||s.isOf(LunarHorizons.POWER_WIRE)||(kind==ColonyBlockEntity.Kind.OXYGEN&&s.isOf(LunarHorizons.OXYGEN_PIPE));
    }
    @Override protected BlockState getStateForNeighborUpdate(BlockState s,net.minecraft.util.math.Direction d,BlockState neighbor,net.minecraft.world.WorldAccess w,BlockPos pos,BlockPos other){
        return d==net.minecraft.util.math.Direction.UP?s.with(Properties.UP,topConnection(neighbor)):s;
    }
    static void refresh(net.minecraft.server.world.ServerWorld w,BlockPos pos,BlockState s){
        if(!(s.getBlock() instanceof ColonyMachineBlock m)||!ColonyGrid.loaded(w,pos.up()))return;
        BlockState next=s.with(Properties.UP,m.topConnection(w.getBlockState(pos.up())));
        if(next!=s)w.setBlockState(pos,next,Block.NOTIFY_LISTENERS);
    }
    @Override public BlockEntity createBlockEntity(BlockPos pos,BlockState state){return new ColonyBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world,BlockState state,BlockEntityType<T> type){
        if(world.isClient||!type.equals(LunarHorizons.COLONY_ENTITY))return null;
        return (w,p,s,be)->{if(be instanceof ColonyBlockEntity machine)ColonyGrid.tick(w,machine);};
    }
}

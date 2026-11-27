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
        super(settings);this.kind=kind;setDefaultState(getStateManager().getDefaultState().with(Properties.LIT,false).with(Properties.HORIZONTAL_FACING,net.minecraft.util.math.Direction.NORTH));
    }
    @Override protected void appendProperties(StateManager.Builder<Block,BlockState> builder){builder.add(Properties.LIT,Properties.HORIZONTAL_FACING);}
    @Override public BlockState getPlacementState(net.minecraft.item.ItemPlacementContext ctx){
        return getDefaultState().with(Properties.HORIZONTAL_FACING,ctx.getHorizontalPlayerFacing().getOpposite());
    }
    @Override protected BlockState rotate(BlockState s,net.minecraft.util.BlockRotation r){return s.with(Properties.HORIZONTAL_FACING,r.rotate(s.get(Properties.HORIZONTAL_FACING)));}
    @Override protected BlockState mirror(BlockState s,net.minecraft.util.BlockMirror m){return s.rotate(m.getRotation(s.get(Properties.HORIZONTAL_FACING)));}
    @Override public BlockEntity createBlockEntity(BlockPos pos,BlockState state){return new ColonyBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world,BlockState state,BlockEntityType<T> type){
        if(world.isClient||!type.equals(LunarHorizons.COLONY_ENTITY))return null;
        return (w,p,s,be)->{if(be instanceof ColonyBlockEntity machine)ColonyGrid.tick(w,machine);};
    }
}

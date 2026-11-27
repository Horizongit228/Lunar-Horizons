package com.lunarhorizons;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
public final class PowerSwitchBlock extends Block {
    public PowerSwitchBlock(Settings settings){super(settings);setDefaultState(getStateManager().getDefaultState().with(Properties.POWERED,true));}
    @Override protected void appendProperties(StateManager.Builder<Block,BlockState> b){b.add(Properties.POWERED);}
}

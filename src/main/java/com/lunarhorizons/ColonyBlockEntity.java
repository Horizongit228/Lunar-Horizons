package com.lunarhorizons;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class ColonyBlockEntity extends BlockEntity {
    enum Kind {BATTERY,GENERATOR,OXYGEN,REFINERY,FUEL,REFILL,LAMP,TERMINAL,ASSEMBLER}
    int energy,input,output,progress,reserve,production,demand,stored;
    boolean active;
    boolean enabled=true,networkFault=false,automation=false;
    int served,networkCapacity,networkNodes;
    long lastPowered=Long.MIN_VALUE;
    public ColonyBlockEntity(BlockPos pos,BlockState state){super(LunarHorizons.COLONY_ENTITY,pos,state);}
    Kind kind(){return ((ColonyMachineBlock)getCachedState().getBlock()).kind;}
    int capacity(){return kind()==Kind.BATTERY?(getCachedState().isOf(LunarHorizons.INDUSTRIAL_BATTERY)?14400:3600):0;}
    int cost(){return switch(kind()){case OXYGEN->20;case REFINERY->45;case FUEL->60;case REFILL->25;case LAMP->8;case ASSEMBLER->50;default->0;};}
    int duration(){return switch(kind()){case REFINERY->6;case FUEL->10;case REFILL->5;case ASSEMBLER->8;default->1;};}
    Item outputItem(){return switch(kind()){case REFINERY->LunarHorizons.TITANIUM_INGOT;case FUEL->LunarHorizons.FUEL;case ASSEMBLER->LunarHorizons.CIRCUIT;default->Items.AIR;};}
    boolean accepts(ItemStack stack){return switch(kind()){
        case GENERATOR,FUEL->stack.isOf(LunarHorizons.HELIUM);
        case OXYGEN,REFILL->stack.isOf(LunarHorizons.LUNAR_ICE.asItem())||stack.isOf(Items.ICE);
        case REFINERY->stack.isOf(LunarHorizons.RAW_TITANIUM);
        case ASSEMBLER->stack.isOf(LunarHorizons.TITANIUM_INGOT);
        default->false;
    };}
    boolean supportsInput(){return switch(kind()){case GENERATOR,OXYGEN,REFINERY,FUEL,REFILL,ASSEMBLER->true;default->false;};}
    boolean supportsOutput(){return outputItem()!=Items.AIR;}
    void automaticTransfer(net.minecraft.server.world.ServerWorld w){
        if(!automation||!enabled)return;
        // Chests, barrels and hoppers: one item per direction each second.
        var above=ColonyGrid.loaded(w,pos.up())?w.getBlockEntity(pos.up()):null;
        if(supportsInput()&&input<64&&ColonyGrid.loaded(w,pos.up())&&isStorage(above)&&above instanceof net.minecraft.inventory.Inventory inv){
            for(int i=0;i<inv.size();i++){
                ItemStack stack=inv.getStack(i);
                if(!accepts(stack))continue;
                ItemStack taken=inv.removeStack(i,1);
                if(!taken.isEmpty()){input++;inv.markDirty();markDirty();}
                break;
            }
        }
        if(!supportsOutput()||output<=0||!ColonyGrid.loaded(w,pos.down()))return;
        var below=w.getBlockEntity(pos.down());
        if(!isStorage(below)||!(below instanceof net.minecraft.inventory.Inventory inv))return;
        ItemStack product=new ItemStack(outputItem());
        for(int i=0;i<inv.size();i++){
            ItemStack stack=inv.getStack(i);
            if(!inv.isValid(i,product))continue;
            if(!stack.isEmpty()&&ItemStack.areItemsAndComponentsEqual(stack,product)&&stack.getCount()<Math.min(stack.getMaxCount(),inv.getMaxCountPerStack())){
                stack.increment(1);output--;inv.markDirty();markDirty();return;
            }
        }
        // Fill existing matching stacks before occupying another slot.
        for(int i=0;i<inv.size();i++)if(inv.getStack(i).isEmpty()&&inv.isValid(i,product)){
            inv.setStack(i,product);output--;inv.markDirty();markDirty();return;
        }
    }
    private static boolean isStorage(net.minecraft.block.entity.BlockEntity be){
        return be instanceof net.minecraft.block.entity.ChestBlockEntity||be instanceof net.minecraft.block.entity.BarrelBlockEntity||be instanceof net.minecraft.block.entity.HopperBlockEntity;
    }
    int diagnose(){
        if(networkFault)return 1;
        if(!enabled)return 2;
        if(active)return 3;
        if(kind()==Kind.BATTERY)return energy==0?4:0;
        if(kind()==Kind.TERMINAL)return 4;
        if((kind()==Kind.GENERATOR||kind()==Kind.OXYGEN)&&reserve==0&&input==0)return 5;
        if(kind()==Kind.GENERATOR)return 6;
        if((kind()==Kind.REFINERY||kind()==Kind.FUEL||kind()==Kind.REFILL||kind()==Kind.ASSEMBLER)&&output>=32)return 7;
        if(supportsInput()&&input==0&&reserve==0)return 5;
        return 8;
    }
    boolean wants(){if(!enabled)return false;return switch(kind()){
        case OXYGEN->reserve>0||input>0;
        case LAMP->true;
        case REFINERY,FUEL,REFILL,ASSEMBLER->input>0&&output<32;
        default->false;
    };}
    void operate(){
        if(kind()==Kind.LAMP)return; // Lighting does not change persistent machine data.
        if(kind()==Kind.OXYGEN){if(reserve<=0&&input>0){input--;reserve=120;}if(reserve>0)reserve--;}
        else if(kind()!=Kind.LAMP&&++progress>=duration()){progress=0;input--;output++;}
        markDirty();
    }
    void setActive(boolean on){
        if(on&&world!=null)lastPowered=world.getTime();
        if(active!=on){active=on;markDirty();}
        if(world!=null&&getCachedState().get(Properties.LIT)!=on)world.setBlockState(pos,getCachedState().with(Properties.LIT,on),2);
    }
    void loadInventory(ServerPlayerEntity p){
        for(int i=0;i<p.getInventory().size()&&input<64;i++){
            ItemStack stack=p.getInventory().getStack(i);
            if(!accepts(stack))continue;
            int add=Math.min(64-input,stack.getCount());input+=add;
            if(!p.isCreative())stack.decrement(add);
        }
        p.getInventory().markDirty();markDirty();
    }
    void collect(ServerPlayerEntity p){
        if(output<=0||outputItem()==Items.AIR)return;
        ItemStack stack=new ItemStack(outputItem(),output);
        p.getInventory().insertStack(stack);output=stack.getCount();p.getInventory().markDirty();markDirty();
    }
    void refillInventory(ServerPlayerEntity p){
        if(kind()!=Kind.REFILL||output<=0)return;
        for(int i=0;i<p.getInventory().size();i++){
            ItemStack stack=p.getInventory().getStack(i);
            if(OxygenSystem.isTank(stack)&&stack.getDamage()>0){stack.setDamage(0);output--;markDirty();p.getInventory().markDirty();return;}
        }
    }
    void interact(ServerPlayerEntity p,ItemStack held){
        if(kind()==Kind.BATTERY&&held.isOf(LunarHorizons.POWER_CELL)){
            int cells=Math.min((capacity()-energy)/600,p.isSneaking()?held.getCount():1);
            if(cells>0){energy+=cells*600;if(!p.isCreative())held.decrement(cells);markDirty();p.getInventory().markDirty();}
            p.sendMessage(Text.translatable("message.lunar_horizons.battery_charge",energy,capacity()),true);
            return;
        }
        if(accepts(held)&&input<64){
            int add=Math.min(64-input,p.isSneaking()?held.getCount():1);
            input+=add;if(!p.isCreative())held.decrement(add);
        }else if(kind()==Kind.REFILL&&OxygenSystem.isTank(held)&&held.getDamage()>0&&output>0){
            held.setDamage(0);output--;
        }else if(p.isSneaking()&&held.isEmpty()&&output>0&&outputItem()!=Items.AIR){
            ItemStack stack=new ItemStack(outputItem(),output);
            p.getInventory().insertStack(stack);output=stack.getCount();
        }
        markDirty();p.getInventory().markDirty();
        p.sendMessage(Text.translatable("message.lunar_horizons.colony_status",input,output,energy,capacity(),active?Text.translatable("message.lunar_horizons.online"):Text.translatable("message.lunar_horizons.offline")),true);
        if(kind()==Kind.TERMINAL)p.sendMessage(Text.translatable("message.lunar_horizons.grid_status",production,demand,stored),false);
    }
    @Override protected void writeNbt(NbtCompound nbt,RegistryWrapper.WrapperLookup lookup){
        super.writeNbt(nbt,lookup);nbt.putInt("Energy",energy);nbt.putInt("Input",input);nbt.putInt("Output",output);nbt.putInt("Progress",progress);nbt.putInt("Reserve",reserve);nbt.putBoolean("Enabled",enabled);nbt.putBoolean("Automation",automation);
    }
    @Override protected void readNbt(NbtCompound nbt,RegistryWrapper.WrapperLookup lookup){
        super.readNbt(nbt,lookup);energy=Math.max(0,Math.min(capacity(),nbt.getInt("Energy")));input=Math.max(0,Math.min(64,nbt.getInt("Input")));output=Math.max(0,Math.min(32,nbt.getInt("Output")));progress=Math.max(0,Math.min(duration()-1,nbt.getInt("Progress")));reserve=Math.max(0,Math.min(kind()==Kind.GENERATOR?60:kind()==Kind.OXYGEN?120:0,nbt.getInt("Reserve")));active=false;enabled=!nbt.contains("Enabled")||nbt.getBoolean("Enabled");automation=nbt.getBoolean("Automation");
    }
}

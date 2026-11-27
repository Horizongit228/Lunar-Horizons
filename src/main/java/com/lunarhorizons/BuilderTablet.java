package com.lunarhorizons;

import java.util.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class BuilderTablet extends Item {
    public BuilderTablet(Settings settings){super(settings);}
    public record Part(int x,int y,int z,Block block){}
    public record Plan(String id,List<Part> parts){
        public Map<Item,Integer> cost(){
            Map<Item,Integer> out=new LinkedHashMap<>();
            for(Part part:parts)out.merge(part.block.asItem(),1,Integer::sum);
            return out;
        }
    }
    private static Part p(int x,int y,int z,Block block){return new Part(x,y,z,block);}
    public static List<Plan> plans(){
        List<Part> pad=new ArrayList<>();
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)pad.add(p(x,0,z,LunarHorizons.LAUNCH_PAD));
        return List.of(
            new Plan("pad",List.copyOf(pad)),
            new Plan("power",List.of(
                p(-1,0,-1,LunarHorizons.SOLAR_PANEL),p(0,0,-1,LunarHorizons.SOLAR_PANEL),p(1,0,-1,LunarHorizons.SOLAR_PANEL),
                p(-1,0,0,LunarHorizons.POWER_CABLE),p(0,0,0,LunarHorizons.BATTERY),p(1,0,0,LunarHorizons.POWER_CABLE),
                p(0,0,1,LunarHorizons.POWER_TERMINAL))),
            new Plan("oxygen",List.of(
                p(-1,0,-1,LunarHorizons.SOLAR_PANEL),p(0,0,-1,LunarHorizons.SOLAR_PANEL),
                p(-1,0,0,LunarHorizons.POWER_CABLE),p(0,0,0,LunarHorizons.OXYGEN_GENERATOR),
                p(1,0,0,LunarHorizons.OXYGEN_PIPE),p(2,0,0,LunarHorizons.OXYGEN_VENT),
                p(0,0,1,LunarHorizons.BATTERY))),
            new Plan("factory",List.of(
                p(-1,0,-1,LunarHorizons.ADVANCED_SOLAR_PANEL),p(0,0,-1,LunarHorizons.ADVANCED_SOLAR_PANEL),
                p(-1,0,0,LunarHorizons.POWER_CABLE),p(0,0,0,LunarHorizons.ELECTRIC_REFINERY),p(1,0,0,LunarHorizons.FUEL_SYNTHESIZER),
                p(-1,0,1,LunarHorizons.BATTERY),p(0,0,1,LunarHorizons.POWER_CABLE),p(1,0,1,LunarHorizons.POWER_TERMINAL)))
        );
    }
    public record Open(BlockPos pos) implements CustomPayload {
        public static final CustomPayload.Id<Open> ID=new CustomPayload.Id<>(LunarHorizons.id("builder_open"));
        public static final PacketCodec<RegistryByteBuf,Open> CODEC=new PacketCodec<>(){
            public Open decode(RegistryByteBuf b){return new Open(b.readBlockPos());}
            public void encode(RegistryByteBuf b,Open v){b.writeBlockPos(v.pos);}
        };
        @Override public CustomPayload.Id<? extends CustomPayload> getId(){return ID;}
    }
    public record Build(int index) implements CustomPayload {
        public static final CustomPayload.Id<Build> ID=new CustomPayload.Id<>(LunarHorizons.id("builder_build"));
        public static final PacketCodec<RegistryByteBuf,Build> CODEC=PacketCodec.tuple(PacketCodecs.VAR_INT,Build::index,Build::new);
        @Override public CustomPayload.Id<? extends CustomPayload> getId(){return ID;}
    }
    private record Pending(ServerWorld world,BlockPos origin,Direction facing,long tick){}
    private static final Map<ServerPlayerEntity,Pending> PENDING=new WeakHashMap<>();
    static void register(){
        PayloadTypeRegistry.playS2C().register(Open.ID,Open.CODEC);
        PayloadTypeRegistry.playC2S().register(Build.ID,Build.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Build.ID,(payload,context)->build(context.player(),payload.index));
    }
    @Override public ActionResult useOnBlock(ItemUsageContext ctx){
        if(ctx.getSide()!=Direction.UP||ctx.getPlayer()==null||ctx.getPlayer().isSpectator())return ActionResult.FAIL;
        if(ctx.getWorld().isClient)return ActionResult.SUCCESS;
        if(!(ctx.getPlayer() instanceof ServerPlayerEntity player)||!(ctx.getWorld() instanceof ServerWorld w))return ActionResult.FAIL;
        BlockPos at=ctx.getBlockPos().up();
        if(!player.getAbilities().allowModifyWorld||!w.canPlayerModifyAt(player,at)||!ServerPlayNetworking.canSend(player,Open.ID))return ActionResult.FAIL;
        PENDING.put(player,new Pending(w,at,player.getHorizontalFacing(),w.getTime()));
        ServerPlayNetworking.send(player,new Open(at));return ActionResult.CONSUME;
    }
    private static BlockPos at(Pending target,Part part){
        Direction forward=target.facing,right=forward.rotateYClockwise();
        return target.origin.offset(right,part.x).offset(forward,-part.z).up(part.y);
    }
    private static void tell(ServerPlayerEntity p,String key,Object...args){p.sendMessage(Text.translatable("message.lunar_horizons.builder_"+key,args),false);}
    private static void build(ServerPlayerEntity player,int index){
        Pending target=PENDING.remove(player);List<Plan> plans=plans();
        if(target==null||index<0||index>=plans.size()||player.getServerWorld()!=target.world||!player.isAlive()||player.isSpectator()||!player.getAbilities().allowModifyWorld||target.world.getTime()-target.tick>1200||
            !(player.getMainHandStack().isOf(LunarHorizons.BUILDER_TABLET)||player.getOffHandStack().isOf(LunarHorizons.BUILDER_TABLET))||
            player.squaredDistanceTo(Vec3d.ofCenter(target.origin))>36){tell(player,"expired");return;}
        ServerWorld w=target.world;Plan plan=plans.get(index);
        Map<BlockPos,BlockState> states=new LinkedHashMap<>();
        for(Part part:plan.parts){
            BlockPos pos=at(target,part);
            if(!ColonyGrid.loaded(w,pos)||!ColonyGrid.loaded(w,pos.down())||!w.getWorldBorder().contains(pos)||!w.canPlayerModifyAt(player,pos)||
                !w.isAir(pos)||!w.getFluidState(pos).isEmpty()||!w.getOtherEntities(null,new Box(pos)).isEmpty()||
                !w.getBlockState(pos.down()).isSideSolidFullSquare(w,pos.down(),Direction.UP)){
                tell(player,"blocked",pos.getX(),pos.getY(),pos.getZ());return;
            }
            BlockState s=part.block.getDefaultState();
            if(s.contains(Properties.HORIZONTAL_FACING))s=s.with(Properties.HORIZONTAL_FACING,target.facing.getOpposite());
            states.put(pos,s);
        }
        Map<Item,Integer> cost=plan.cost();
        if(!player.isCreative())for(var entry:cost.entrySet()){
            int count=0;for(int i=0;i<36;i++){ItemStack stack=player.getInventory().getStack(i);if(stack.isOf(entry.getKey()))count+=stack.getCount();}
            if(count<entry.getValue()){tell(player,"missing",new ItemStack(entry.getKey()).getName(),entry.getValue()-count);return;}
        }
        List<BlockPos> placed=new ArrayList<>();
        for(var entry:states.entrySet()){
            if(!w.setBlockState(entry.getKey(),entry.getValue(),3)){
                for(BlockPos pos:placed)w.removeBlock(pos,false);
                tell(player,"failed");return;
            }
            placed.add(entry.getKey());
        }
        for(BlockPos pos:placed)ColonyConduitBlock.refresh(w,pos,w.getBlockState(pos));
        if(!player.isCreative())for(var entry:cost.entrySet()){
            int left=entry.getValue();
            for(int i=0;i<36&&left>0;i++){ItemStack stack=player.getInventory().getStack(i);if(!stack.isOf(entry.getKey()))continue;int take=Math.min(left,stack.getCount());stack.decrement(take);left-=take;}
        }
        player.getInventory().markDirty();tell(player,"built",Text.translatable("builder.lunar_horizons."+plan.id));
    }
}

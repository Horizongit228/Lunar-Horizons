package com.lunarhorizons.client;

import com.lunarhorizons.BuilderTablet;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

final class BuilderScreen extends Screen{
    private static final int W=306,H=214;
    private final BlockPos origin;
    private int selected,left,top;
    private ButtonWidget build;
    private BuilderScreen(BlockPos origin){super(Text.translatable("builder.lunar_horizons.title"));this.origin=origin;}
    static void register(){
        ClientPlayNetworking.registerGlobalReceiver(BuilderTablet.Open.ID,(payload,context)->context.client().execute(()->{
            if(context.client().player!=null)context.client().setScreen(new BuilderScreen(payload.pos()));
        }));
    }
    private Text tr(String key,Object...args){return Text.translatable("builder.lunar_horizons."+key,args);}
    private boolean enough(){
        if(client==null||client.player==null)return false;if(client.player.isCreative())return true;
        for(var e:BuilderTablet.plans().get(selected).cost().entrySet()){
            int n=0;for(int i=0;i<36;i++){var s=client.player.getInventory().getStack(i);if(s.isOf(e.getKey()))n+=s.getCount();}
            if(n<e.getValue())return false;
        }
        return true;
    }
    @Override protected void init(){
        left=(width-W)/2;top=(height-H)/2;
        var plans=BuilderTablet.plans();
        for(int i=0;i<plans.size();i++){final int index=i;
            addDrawableChild(ButtonWidget.builder(tr(plans.get(i).id()),b->{selected=index;build.active=enough();}).dimensions(left+8+(i%2)*146,top+24+(i/2)*22,142,20).build());
        }
        build=addDrawableChild(ButtonWidget.builder(tr("build"),b->{if(ClientPlayNetworking.canSend(BuilderTablet.Build.ID)){ClientPlayNetworking.send(new BuilderTablet.Build(selected));close();}}).dimensions(left+8,top+186,W-16,20).build());
        build.active=enough();
    }
    @Override public void tick(){build.active=enough();}
    @Override public boolean shouldPause(){return false;}
        // Screen.render normally applies the vanilla background blur before its widgets.
    // Our custom panel is drawn first; do not blur it when rendering the widgets.
    @Override public void renderBackground(DrawContext c,int mouseX,int mouseY,float delta){}
    @Override public void render(DrawContext c,int mx,int my,float delta){
        c.fill(0,0,width,height,0x99070d14);c.fill(left,top,left+W,top+H,0xff17232d);
        c.drawTextWithShadow(textRenderer,title,left+8,top+9,0xff83c2c7);
        var plan=BuilderTablet.plans().get(selected);
        c.drawTextWithShadow(textRenderer,tr(plan.id()),left+8,top+72,0xffdce9ec);
        c.drawTextWithShadow(textRenderer,Text.literal(origin.getX()+", "+origin.getY()+", "+origin.getZ()),left+177,top+72,0xff9aaeb9);
        for(var part:plan.parts()){
            int x=left+12+(part.x()+1)*23,y=top+89+(part.z()+1)*23;
            c.fill(x,y,x+22,y+22,0xff35434e);c.drawItem(new ItemStack(part.block()),x+3,y+3);
        }
        int row=0;
        for(var e:plan.cost().entrySet()){
            int x=left+115+(row%3)*59,y=top+89+(row/3)*22;
            c.drawItem(new ItemStack(e.getKey()),x,y);
            c.drawTextWithShadow(textRenderer,Text.literal("×"+e.getValue()),x+18,y+5,0xffdce9ec);row++;
        }
        c.drawTextWithShadow(textRenderer,tr(enough()?"ready":"missing"),left+8,top+161,enough()?0xff85c7b6:0xffff927f);
        c.drawTextWithShadow(textRenderer,tr("ground"),left+8,top+173,0xff9aaeb9);
        super.render(c,mx,my,delta);
        row=0;for(var e:plan.cost().entrySet()){
            int x=left+115+(row%3)*59,y=top+89+(row/3)*22;
            if(mx>=x&&mx<x+52&&my>=y&&my<y+20)c.drawTooltip(textRenderer,new ItemStack(e.getKey()).getName(),mx,my);row++;
        }
    }
}

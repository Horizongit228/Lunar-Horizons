package com.lunarhorizons.client;

import com.lunarhorizons.RocketEntity;
import com.lunarhorizons.RocketDestination;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** A direct route selector. The server validates every selection. */
final class RocketRouteScreen extends Screen {
    private final RocketEntity rocket;
    private static final int W=252,H=164;
    RocketRouteScreen(RocketEntity rocket){
        super(Text.translatable("screen.lunar_horizons.route_title"));
        this.rocket=rocket;
    }
    private static Text tr(String key,Object...args){return Text.translatable("screen.lunar_horizons.route_"+key,args);}
    @Override protected void init(){
        int x=(width-W)/2,y=(height-H)/2;
        for(int i=0;i<3;i++){
            final int goal=i;
            boolean here=goal==rocket.currentLocation();
            boolean selected=goal==rocket.destination();
            Text label=tr(here?"here":selected?"selected":"option",tr("place_"+goal));
            var button=addDrawableChild(ButtonWidget.builder(label,b->{
                if(ClientPlayNetworking.canSend(RocketDestination.ID))ClientPlayNetworking.send(new RocketDestination(goal));
                close();
            }).dimensions(x+16,y+38+i*24,W-32,20).build());
            button.active=!here&&!selected&&rocket.canChooseRoute()&&ClientPlayNetworking.canSend(RocketDestination.ID);
        }
        addDrawableChild(ButtonWidget.builder(tr("back"),b->close()).dimensions(x+16,y+136,W-32,20).build());
    }
    @Override public boolean shouldPause(){return false;}
    @Override public void tick(){
        if(client==null||client.player==null||client.player.getVehicle()!=rocket||!rocket.canChooseRoute())close();
    }
    @Override public void renderBackground(DrawContext c,int mouseX,int mouseY,float delta){}
    @Override public void render(DrawContext c,int mouseX,int mouseY,float delta){
        int x=(width-W)/2,y=(height-H)/2;
        c.fill(0,0,width,height,0x99070d14);
        c.fill(x,y,x+W,y+H,0xff17232d);
        c.fill(x,y,x+W,y+2,0xff81bfc3);
        c.drawCenteredTextWithShadow(textRenderer,title,width/2,y+9,0xffdce9ec);
        c.drawCenteredTextWithShadow(textRenderer,tr("from",tr("place_"+rocket.currentLocation())),width/2,y+23,0xff81bfc3);
        c.drawCenteredTextWithShadow(textRenderer,tr(rocket.phase()==RocketEntity.COUNTDOWN?"countdown":rocket.phase()==RocketEntity.ASCENT?"ascent":"hint"),width/2,y+117,0xffe6c18a);
        super.render(c,mouseX,mouseY,delta);
    }
}

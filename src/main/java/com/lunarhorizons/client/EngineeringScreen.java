package com.lunarhorizons.client;
import com.lunarhorizons.EngineeringPackets;
import com.lunarhorizons.ColonyMachineBlock;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

/** Live machine control console. All inventory transfers and toggles are validated server-side. */
final class EngineeringScreen extends Screen{
    private EngineeringPackets.Snapshot data;
    private final RegistryKey<World> dimension;
    private int tick,stale,left,top;
    private ButtonWidget toggle,load,collect,refill,automatic;
    private static final int W=292,H=234;
    private EngineeringScreen(EngineeringPackets.Snapshot data){
        super(Text.translatable("screen.lunar_horizons.engineering"));this.data=data;
        dimension=MinecraftClient.getInstance().world.getRegistryKey();
    }
    static void register(){
        ClientPlayNetworking.registerGlobalReceiver(EngineeringPackets.Snapshot.ID,(payload,context)->context.client().execute(()->{
            var mc=context.client();if(mc.world==null||mc.player==null)return;
            if(payload.open())mc.setScreen(new EngineeringScreen(payload));
            else if(mc.currentScreen instanceof EngineeringScreen s&&s.data.pos().equals(payload.pos())){s.data=payload;s.stale=0;s.updateButtons();}
        }));
    }
    private void request(int action){if(ClientPlayNetworking.canSend(EngineeringPackets.Request.ID))ClientPlayNetworking.send(new EngineeringPackets.Request(data.pos(),action));}
    private static Text tr(String key,Object...args){return Text.translatable("screen.lunar_horizons."+key,args);}
    private ButtonWidget button(String name,int x,int y,int width,int action){
        return addDrawableChild(ButtonWidget.builder(tr(name),b->request(action)).dimensions(left+x,top+y,width,20).build());
    }
    @Override protected void init(){
        left=(width-W)/2;top=(height-H)/2;stale=0;request(0);
        toggle=button("toggle",10,158,132,1);
        load=button("load",150,158,132,2);
        collect=button("collect",10,182,132,3);
        refill=button("refill",150,182,132,4);
        automatic=button("auto_off",10,206,132,5);
        addDrawableChild(ButtonWidget.builder(tr("help"),b->client.setScreen(new LunarGuideScreen(this,9+data.kind()))).dimensions(left+150,top+206,132,20).build());
        updateButtons();
    }
    private void updateButtons(){
        if(toggle==null)return;
        toggle.setMessage(tr(data.enabled()?"disable":"enable"));
        int k=data.kind();
        load.active=(k==1||k==2||k==3||k==4||k==5||k==8)&&data.input()<64;
        collect.active=(k==3||k==4||k==8)&&data.output()>0;
        refill.active=k==5&&data.output()>0;
        automatic.active=k==1||k==2||k==3||k==4||k==5||k==8;
        automatic.setMessage(tr(data.automation()?"auto_on":"auto_off"));
    }
    @Override public boolean shouldPause(){return false;}
    @Override public void tick(){
        if(client==null||client.player==null||client.world==null||!client.world.getRegistryKey().equals(dimension)
            ||client.player.squaredDistanceTo(Vec3d.ofCenter(data.pos()))>36
            ||!(client.world.getBlockState(data.pos()).getBlock() instanceof ColonyMachineBlock)||++stale>80){close();return;}
        if(++tick%20==0)request(0);
    }
    private void bar(DrawContext c,int x,int y,int w,int value,int max,int color){
        c.fill(x,y,x+w,y+7,0xff35434e);if(max>0&&value>0)c.fill(x,y,x+(int)((long)w*Math.min(max,value)/max),y+7,color);
    }
    private void line(DrawContext c,Text t,int x,int y,int color){c.drawTextWithShadow(textRenderer,t,left+x,top+y,color);}
    // Screen.render normally applies the vanilla background blur before its widgets.
    // Our custom panel is drawn first; do not blur it when rendering the widgets.
    @Override public void renderBackground(DrawContext c,int mouseX,int mouseY,float delta){}
    @Override public void render(DrawContext c,int mx,int my,float delta){
        c.fill(0,0,width,height,0x99070d14);c.fill(left,top,left+W,top+H,0xff17232d);
        c.fill(left,top,left+W,top+2,0xff81bfc3);
        if(client!=null&&client.world!=null)line(c,client.world.getBlockState(data.pos()).getBlock().getName(),10,9,0xffdce9ec);
        line(c,tr(data.fault()?"fault":!data.enabled()?"disabled":data.active()?"running":"standby"),10,23,data.fault()?0xffff927f:data.active()?0xff85c7b6:0xffb5c1c8);
        line(c,tr("machine_io_"+data.kind()),10,37,0xffa8c9ca);
        line(c,tr("input_output",data.input(),data.output()),10,49,0xffdce9ec);
        line(c,tr("progress",data.progress(),data.duration()),10,61,0xffb5c1c8);bar(c,left+10,top+73,272,data.progress(),data.duration(),0xff81bfc3);
        line(c,tr("reserve",data.reserve(),data.energy(),data.capacity()),10,84,0xffdce9ec);
        line(c,tr("power",data.production(),data.served(),data.demand()),10,96,0xffdce9ec);
        line(c,tr("storage",data.stored(),data.networkCapacity()),10,108,0xffdce9ec);bar(c,left+10,top+120,272,data.stored(),data.networkCapacity(),0xffc7a263);
        line(c,tr("nodes",data.nodes()),10,134,0xff9aaeb9);
        line(c,tr("diagnostic_"+data.diagnostic()),10,145,data.diagnostic()==3?0xff85c7b6:0xffe6c18a);
        super.render(c,mx,my,delta);
    }
}

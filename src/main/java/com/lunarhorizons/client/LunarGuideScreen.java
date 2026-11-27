package com.lunarhorizons.client;
import com.lunarhorizons.LunarGuideItem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.text.OrderedText;
import net.minecraft.util.Identifier;
import java.util.ArrayList;
import java.util.List;

final class LunarGuideScreen extends Screen{
    private static final int TEXT_PAGES=18,DIAGRAMS=4,RECIPE_START=TEXT_PAGES+DIAGRAMS,W=304,H=216;
    private record Recipe(String result,int count,String[] slots){}
    private static final Recipe[] RECIPES={
        new Recipe("lunar_horizons:solar_panel",2,new String[]{"minecraft:glass","minecraft:glass","minecraft:glass","minecraft:coal","minecraft:redstone","minecraft:coal","lunar_horizons:titanium_ingot","lunar_horizons:titanium_ingot","lunar_horizons:titanium_ingot"}),
        new Recipe("lunar_horizons:advanced_solar_panel",1,new String[]{"minecraft:glass","minecraft:glass","minecraft:glass","lunar_horizons:solar_panel","lunar_horizons:control_circuit","lunar_horizons:solar_panel","lunar_horizons:titanium_ingot","lunar_horizons:titanium_ingot","lunar_horizons:titanium_ingot"}),
        new Recipe("lunar_horizons:energy_battery",1,new String[]{"lunar_horizons:titanium_ingot","lunar_horizons:power_cell","lunar_horizons:titanium_ingot","lunar_horizons:power_cell","lunar_horizons:machine_frame","lunar_horizons:power_cell","lunar_horizons:titanium_ingot","lunar_horizons:control_circuit","lunar_horizons:titanium_ingot"}),
        new Recipe("lunar_horizons:industrial_battery",1,new String[]{"lunar_horizons:titanium_ingot","lunar_horizons:power_cell","lunar_horizons:titanium_ingot","lunar_horizons:energy_battery","lunar_horizons:control_circuit","lunar_horizons:energy_battery","lunar_horizons:titanium_ingot","lunar_horizons:power_cell","lunar_horizons:titanium_ingot"}),
        new Recipe("lunar_horizons:helium_generator",1,new String[]{"lunar_horizons:titanium_ingot","lunar_horizons:control_circuit","lunar_horizons:titanium_ingot","lunar_horizons:power_cell","lunar_horizons:machine_frame","lunar_horizons:power_cell","lunar_horizons:titanium_ingot","minecraft:redstone","lunar_horizons:titanium_ingot"}),
        new Recipe("lunar_horizons:power_cable",12,new String[]{"minecraft:iron_ingot","minecraft:iron_ingot","minecraft:iron_ingot","minecraft:copper_ingot","minecraft:copper_ingot","minecraft:copper_ingot","minecraft:iron_ingot","minecraft:iron_ingot","minecraft:iron_ingot"}),
        new Recipe("lunar_horizons:power_switch",2,new String[]{"minecraft:iron_ingot","minecraft:redstone","minecraft:iron_ingot","lunar_horizons:power_cable","minecraft:lever","lunar_horizons:power_cable","minecraft:iron_ingot","minecraft:iron_ingot","minecraft:iron_ingot"}),
        new Recipe("lunar_horizons:power_feedthrough",3,new String[]{"lunar_horizons:habitat_hull","lunar_horizons:habitat_hull","lunar_horizons:habitat_hull","lunar_horizons:power_cable","lunar_horizons:power_cable","lunar_horizons:power_cable","lunar_horizons:habitat_hull","lunar_horizons:habitat_hull","lunar_horizons:habitat_hull"}),
        new Recipe("lunar_horizons:oxygen_generator",1,new String[]{"lunar_horizons:titanium_ingot","lunar_horizons:oxygen_filter","lunar_horizons:titanium_ingot","lunar_horizons:control_circuit","lunar_horizons:machine_frame","lunar_horizons:control_circuit","lunar_horizons:titanium_ingot","lunar_horizons:power_cell","lunar_horizons:titanium_ingot"}),
        new Recipe("lunar_horizons:oxygen_pipe",8,new String[]{"minecraft:iron_ingot","minecraft:iron_ingot","minecraft:iron_ingot","minecraft:glass","minecraft:air","minecraft:glass","minecraft:iron_ingot","minecraft:iron_ingot","minecraft:iron_ingot"}),
        new Recipe("lunar_horizons:oxygen_vent",2,new String[]{"minecraft:iron_ingot","minecraft:iron_ingot","minecraft:iron_ingot","lunar_horizons:oxygen_filter","lunar_horizons:control_circuit","lunar_horizons:oxygen_filter","minecraft:iron_ingot","minecraft:iron_ingot","minecraft:iron_ingot"}),
        new Recipe("lunar_horizons:electric_refill",1,new String[]{"lunar_horizons:titanium_ingot","lunar_horizons:oxygen_filter","lunar_horizons:titanium_ingot","lunar_horizons:control_circuit","lunar_horizons:machine_frame","lunar_horizons:control_circuit","lunar_horizons:titanium_ingot","minecraft:iron_ingot","lunar_horizons:titanium_ingot"}),
        new Recipe("lunar_horizons:electric_refinery",1,new String[]{"lunar_horizons:titanium_ingot","lunar_horizons:control_circuit","lunar_horizons:titanium_ingot","minecraft:iron_ingot","lunar_horizons:machine_frame","minecraft:iron_ingot","minecraft:iron_ingot","lunar_horizons:power_cell","minecraft:iron_ingot"}),
        new Recipe("lunar_horizons:fuel_synthesizer",1,new String[]{"lunar_horizons:titanium_ingot","lunar_horizons:power_cell","lunar_horizons:titanium_ingot","lunar_horizons:control_circuit","lunar_horizons:machine_frame","lunar_horizons:control_circuit","lunar_horizons:titanium_ingot","lunar_horizons:helium_3","lunar_horizons:titanium_ingot"}),
        new Recipe("lunar_horizons:advanced_assembler",1,new String[]{"lunar_horizons:titanium_ingot","lunar_horizons:control_circuit","lunar_horizons:titanium_ingot","lunar_horizons:power_cell","lunar_horizons:machine_frame","lunar_horizons:power_cell","lunar_horizons:titanium_ingot","lunar_horizons:dark_crystal","lunar_horizons:titanium_ingot"}),
        new Recipe("lunar_horizons:powered_lamp",2,new String[]{"minecraft:iron_ingot","minecraft:glass","minecraft:iron_ingot","minecraft:glass","lunar_horizons:control_circuit","minecraft:glass","minecraft:iron_ingot","minecraft:redstone","minecraft:iron_ingot"}),
        new Recipe("lunar_horizons:power_terminal",1,new String[]{"minecraft:iron_ingot","minecraft:glass","minecraft:iron_ingot","lunar_horizons:control_circuit","lunar_horizons:machine_frame","lunar_horizons:control_circuit","minecraft:iron_ingot","lunar_horizons:power_cell","minecraft:iron_ingot"}),
        new Recipe("lunar_horizons:habitat_hull",8,new String[]{"minecraft:iron_ingot","minecraft:iron_ingot","minecraft:iron_ingot","lunar_horizons:titanium_ingot","minecraft:air","lunar_horizons:titanium_ingot","minecraft:iron_ingot","minecraft:iron_ingot","minecraft:iron_ingot"}),
        new Recipe("lunar_horizons:pressure_glass",5,new String[]{"lunar_horizons:titanium_ingot","minecraft:glass","lunar_horizons:titanium_ingot","minecraft:glass","minecraft:glass","minecraft:glass","lunar_horizons:titanium_ingot","minecraft:glass","lunar_horizons:titanium_ingot"}),
        new Recipe("lunar_horizons:airlock",2,new String[]{"lunar_horizons:titanium_ingot","lunar_horizons:control_circuit","lunar_horizons:titanium_ingot","lunar_horizons:habitat_hull","lunar_horizons:machine_frame","lunar_horizons:habitat_hull","lunar_horizons:titanium_ingot","lunar_horizons:control_circuit","lunar_horizons:titanium_ingot"}),
        new Recipe("lunar_horizons:control_circuit",2,new String[]{"minecraft:copper_ingot","minecraft:redstone","minecraft:copper_ingot","minecraft:glass","lunar_horizons:titanium_ingot","minecraft:glass","minecraft:copper_ingot","minecraft:redstone","minecraft:copper_ingot"}),
        new Recipe("lunar_horizons:power_cell",2,new String[]{"minecraft:iron_ingot","minecraft:redstone","minecraft:iron_ingot","minecraft:copper_ingot","lunar_horizons:lunar_crystal","minecraft:copper_ingot","minecraft:iron_ingot","minecraft:redstone","minecraft:iron_ingot"}),
        new Recipe("lunar_horizons:oxygen_filter",2,new String[]{"minecraft:air","minecraft:white_wool","minecraft:air","minecraft:iron_ingot","minecraft:charcoal","minecraft:iron_ingot","minecraft:air","minecraft:white_wool","minecraft:air"}),
        new Recipe("lunar_horizons:machine_frame",2,new String[]{"lunar_horizons:titanium_ingot","minecraft:iron_ingot","lunar_horizons:titanium_ingot","minecraft:iron_ingot","minecraft:air","minecraft:iron_ingot","lunar_horizons:titanium_ingot","minecraft:iron_ingot","lunar_horizons:titanium_ingot"}),
        new Recipe("lunar_horizons:rover",1,new String[]{"lunar_horizons:titanium_ingot","lunar_horizons:control_circuit","lunar_horizons:titanium_ingot","lunar_horizons:power_cell","lunar_horizons:machine_frame","lunar_horizons:power_cell","minecraft:iron_ingot","minecraft:redstone","minecraft:iron_ingot"})
,
        new Recipe("lunar_horizons:builder_tablet",1,new String[]{"lunar_horizons:titanium_ingot","minecraft:glass","lunar_horizons:titanium_ingot","lunar_horizons:control_circuit","lunar_horizons:power_cell","lunar_horizons:control_circuit","lunar_horizons:titanium_ingot","lunar_horizons:machine_frame","lunar_horizons:titanium_ingot"}),
        new Recipe("lunar_horizons:advanced_rover",1,new String[]{"lunar_horizons:advanced_solar_panel","lunar_horizons:control_circuit","lunar_horizons:advanced_solar_panel","lunar_horizons:power_cell","lunar_horizons:rover","lunar_horizons:power_cell","lunar_horizons:titanium_ingot","lunar_horizons:machine_frame","lunar_horizons:titanium_ingot"}),
        new Recipe("lunar_horizons:advanced_rocket",1,new String[]{"lunar_horizons:titanium_ingot","lunar_horizons:control_circuit","lunar_horizons:titanium_ingot","lunar_horizons:machine_frame","lunar_horizons:rocket","lunar_horizons:machine_frame","lunar_horizons:power_cell","lunar_horizons:titanium_ingot","lunar_horizons:power_cell"})
,
        new Recipe("lunar_horizons:power_wire",16,new String[]{"minecraft:copper_ingot","minecraft:copper_ingot","minecraft:copper_ingot","minecraft:redstone","minecraft:redstone","minecraft:redstone","minecraft:air","minecraft:air","minecraft:air"})
    };
    private static final int TOTAL=RECIPE_START+RECIPES.length;
    private final Screen parent;
    private int page,offset,left,top;
    private ButtonWidget previous,next,up,down;
    LunarGuideScreen(Screen parent,int page){super(Text.translatable("guide.lunar_horizons.title"));this.parent=parent;this.page=Math.max(0,Math.min(TOTAL-1,page));}
    static void register(){
        ClientPlayNetworking.registerGlobalReceiver(LunarGuideItem.Open.ID,(payload,context)->context.client().execute(()->{
            if(context.client().player!=null)context.client().setScreen(new LunarGuideScreen(null,0));
        }));
    }
    private Text tr(String key,Object...args){return Text.translatable("guide.lunar_horizons."+key,args);}
    private ButtonWidget button(String key,int x,int y,int w,Runnable action){
        return addDrawableChild(ButtonWidget.builder(tr(key),b->action.run()).dimensions(left+x,top+y,w,18).build());
    }
    @Override protected void init(){
        left=(width-W)/2;top=(height-H)/2;
        button("contents",8,25,54,()->go(0));button("bases",66,25,54,()->go(1));
        button("machines",124,25,58,()->go(9));button("diagrams",186,25,54,()->go(TEXT_PAGES));button("recipes",244,25,52,()->go(RECIPE_START));
        previous=button("previous",8,190,67,()->go(page-1));next=button("next",229,190,67,()->go(page+1));
        up=button("up",80,190,29,()->{offset=Math.max(0,offset-6);updateButtons();});
        down=button("down",195,190,29,()->{offset=Math.min(maxOffset(),offset+6);updateButtons();});updateButtons();
    }
    private void go(int p){page=Math.max(0,Math.min(TOTAL-1,p));offset=0;updateButtons();}
    private List<OrderedText> lines(){
        List<OrderedText> result=new ArrayList<>();
        if(page<TEXT_PAGES)for(String line:tr("page."+page+".body").getString().split("\\n",-1)){
            if(line.isEmpty())result.add(Text.empty().asOrderedText());else result.addAll(textRenderer.wrapLines(Text.literal(line),280));
        }
        return result;
    }
    private int maxOffset(){return Math.max(0,lines().size()-10);}
    private void updateButtons(){if(previous==null)return;previous.active=page>0;next.active=page<TOTAL-1;up.active=offset>0;down.active=offset<maxOffset();}
    @Override public boolean shouldPause(){return false;}
    @Override public void close(){if(client!=null)client.setScreen(parent);}
    private static ItemStack stack(String id){return new ItemStack(Registries.ITEM.get(Identifier.of(id)));}
    // Screen.render normally applies the vanilla background blur before its widgets.
    // Our custom panel is drawn first; do not blur it when rendering the widgets.
    @Override public void renderBackground(DrawContext c,int mouseX,int mouseY,float delta){}
    private void label(DrawContext c,String key,int x,int y,int color){
        c.drawCenteredTextWithShadow(textRenderer,tr("diagram."+key),left+x,top+y,color);
    }
    private void node(DrawContext c,String id,String key,int x,int y){
        c.fill(left+x-14,top+y-14,left+x+14,top+y+14,0xff354753);
        c.fill(left+x-13,top+y-13,left+x+13,top+y+13,0xff233540);
        c.drawItem(stack(id),left+x-8,top+y-8);
        label(c,key,x,y+17,0xffdce9ec);
    }
    private void link(DrawContext c,int x1,int y1,int x2,int y2,int color){
        c.fill(left+Math.min(x1,x2)-1,top+Math.min(y1,y2)-1,left+Math.max(x1,x2)+2,top+Math.max(y1,y2)+2,color);
    }
    private void diagram(DrawContext c,int n){
        final int power=0xffc19b61,oxygen=0xff77c4d2,items=0xffa0c6a9;
        label(c,"title."+n,W/2,50,0xff83c2c7);
        switch(n){
            case 0 -> {
                link(c,48,88,256,88,power);link(c,152,88,152,139,power);
                node(c,"lunar_horizons:solar_panel","solar",48,88);
                node(c,"lunar_horizons:energy_battery","battery",152,88);
                node(c,"lunar_horizons:oxygen_generator","oxygen",256,88);
                node(c,"lunar_horizons:power_terminal","terminal",152,139);
                label(c,"power_legend",152,169,power);
            }
            case 1 -> {
                link(c,48,88,256,88,oxygen);link(c,48,88,48,136,power);
                node(c,"lunar_horizons:oxygen_generator","oxygen",48,88);
                node(c,"lunar_horizons:oxygen_pipe","pipe",152,88);
                node(c,"lunar_horizons:oxygen_vent","vent",256,88);
                node(c,"lunar_horizons:power_wire","power",48,136);
                c.fill(left+211,top+119,left+291,top+158,0xff304750);
                label(c,"room",251,132,0xffb7dadd);
                label(c,"sealed",251,144,0xffb7dadd);
                label(c,"oxygen_legend",152,169,oxygen);
            }
            case 2 -> {
                link(c,152,79,152,150,items);link(c,48,115,152,115,power);
                c.drawTextWithShadow(textRenderer,Text.literal("↓"),left+170,top+93,items);
                c.drawTextWithShadow(textRenderer,Text.literal("↓"),left+170,top+130,items);
                node(c,"minecraft:chest","input",152,78);
                node(c,"lunar_horizons:electric_refinery","machine",152,115);
                node(c,"minecraft:chest","output",152,152);
                node(c,"lunar_horizons:power_wire","power",48,115);
                c.drawTextWithShadow(textRenderer,tr("diagram.auto"),left+197,top+110,0xffa0c6a9);
            }
            default -> {
                // Top-down floor plan; ceiling and floor must also be solid.
                int ox=left+22,oy=top+66,cell=15;
                for(int z=0;z<7;z++)for(int x=0;x<7;x++){
                    int col=(x==0||x==6||z==0||z==6)?0xff83949c:0xff2e4650;
                    if(z==6&&x==3)col=0xffbba16b;
                    if(z==0&&x>=2&&x<=4)col=0xff638eaa;
                    if(x==0&&z==3)col=0xff76bdc8;
                    if(x==6&&z==2)col=power;
                    c.fill(ox+x*cell,oy+z*cell,ox+x*cell+cell-1,oy+z*cell+cell-1,col);
                }
                c.fill(ox+cell+6,oy+cell*2+6,ox+cell*6+8,oy+cell*2+9,power);
                c.fill(ox+cell+6,oy+cell*2+6,ox+cell+9,oy+cell*3+8,power);
                c.drawItem(stack("lunar_horizons:oxygen_generator"),ox+cell,oy+cell*3);
                c.drawItem(stack("lunar_horizons:powered_lamp"),ox+cell*4,oy+cell*2);
                String[] keys={"wall","window","airlock","vent","feedthrough","roof"};
                int[] colors={0xff83949c,0xff638eaa,0xffbba16b,oxygen,power,0xffdce9ec};
                for(int i=0;i<keys.length;i++){
                    int y=top+70+i*17;c.fill(left+146,y,left+155,y+9,colors[i]);
                    c.drawTextWithShadow(textRenderer,tr("diagram."+keys[i]),left+161,y,0xffdce9ec);
                }
            }
        }
    }
    @Override public void render(DrawContext c,int mx,int my,float delta){
        c.fill(0,0,width,height,0x99070d14);c.fill(left,top,left+W,top+H,0xff17232d);
        c.fill(left,top,left+W,top+2,0xff81bfc3);
        c.drawTextWithShadow(textRenderer,title,left+9,top+8,0xffdce9ec);
        c.drawTextWithShadow(textRenderer,tr("edition"),left+W-8-textRenderer.getWidth(tr("edition")),top+8,0xff9aaeb9);
        int tab=page==0?0:page<9?1:page<TEXT_PAGES?2:page<RECIPE_START?3:4;
        int[] starts={8,66,124,186,244},sizes={54,54,58,54,52};
        c.fill(left+starts[tab],top+44,left+starts[tab]+sizes[tab],top+46,0xff81bfc3);
        if(page<TEXT_PAGES){
            c.drawTextWithShadow(textRenderer,tr("page."+page+".title"),left+12,top+50,0xff83c2c7);
            var lines=lines();int end=Math.min(lines.size(),offset+10);
            for(int i=offset;i<end;i++)c.drawTextWithShadow(textRenderer,lines.get(i),left+12,top+67+(i-offset)*11,0xffdce9ec);
        }else if(page<RECIPE_START){
            diagram(c,page-TEXT_PAGES);
        }else{
            Recipe recipe=RECIPES[page-RECIPE_START];ItemStack result=stack(recipe.result);result.setCount(recipe.count);
            c.drawTextWithShadow(textRenderer,result.getName(),left+12,top+50,0xff83c2c7);
            for(int i=0;i<9;i++){
                int x=left+20+(i%3)*24,y=top+68+(i/3)*24;
                c.fill(x,y,x+22,y+22,0xff35434e);c.fill(x+1,y+1,x+21,y+21,0xff26343e);
                ItemStack item=stack(recipe.slots[i]);if(!item.isEmpty())c.drawItem(item,x+3,y+3);
            }
            c.drawTextWithShadow(textRenderer,Text.literal("→"),left+109,top+98,0xffdce9ec);
            c.drawItem(result,left+132,top+94);
            c.drawTextWithShadow(textRenderer,Text.literal("×"+recipe.count),left+155,top+99,0xffdce9ec);
            c.drawTextWithShadow(textRenderer,tr("recipe_hint"),left+12,top+154,0xff9aaeb9);
            c.drawTextWithShadow(textRenderer,tr("recipe_hover"),left+12,top+166,0xff9aaeb9);
        }
        c.drawCenteredTextWithShadow(textRenderer,Text.literal((page+1)+" / "+TOTAL),left+W/2,top+195,0xffdce9ec);
        super.render(c,mx,my,delta);
        if(page>=RECIPE_START){
            Recipe recipe=RECIPES[page-RECIPE_START];
            for(int i=0;i<9;i++){int x=left+20+(i%3)*24,y=top+68+(i/3)*24;
                if(mx>=x&&mx<x+22&&my>=y&&my<y+22){ItemStack item=stack(recipe.slots[i]);if(!item.isEmpty())c.drawTooltip(textRenderer,item.getName(),mx,my);}
            }
        }
    }
}

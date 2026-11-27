package com.lunarhorizons.client;

import com.lunarhorizons.RocketEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import java.util.ArrayList;
import java.util.List;

/** Separate eight- and twelve-block rocket models with transparent cockpits.
 * Model coordinates are quarter-block units, with negative Y pointing up. */
public final class RocketRenderer extends EntityRenderer<RocketEntity> {
    private static final Identifier ALLOY=Identifier.of("lunar_horizons","textures/entity/rocket_alloy.png");
    private static final Identifier CLEAR=Identifier.ofVanilla("textures/block/white_concrete.png");
    private static final int IVORY=0xffdeded5, TITANIUM=0xffa4adb1, GRAPHITE=0xff424a51, RED=0xff92544a, GOLD=0xffb19a6f;
    private static final int SIDES=12;
    private record Piece(ModelPart model,int color,boolean glass,boolean lit){}
    private record VehicleModel(List<Piece> hull,List<Piece> windows,List<Piece> flames) {
        VehicleModel(){this(new ArrayList<>(),new ArrayList<>(),new ArrayList<>());}
    }
    private final VehicleModel mk1=new VehicleModel(),mk2=new VehicleModel();
    private static final int BLUE=0xff375c79, WHITE=0xffe6ebed, COPPER=0xffb67c50, CYAN=0xffa5edf0;

    public RocketRenderer(EntityRendererFactory.Context context){
        super(context);
        shadowRadius=1.45f;
        buildExpedition(mk1);
        buildOrbital(mk2);
    }
    // Mk I: squat, ivory/red expedition lander, four bells and four wide landing legs.
    private static void buildExpedition(VehicleModel m){
        var h=m.hull;
        ring(h,3.8f,-8,-3,.4f,GRAPHITE,false);
        ring(h,4.35f,-17.8f,-8,.35f,IVORY,false);
        for(float y:new float[]{-17.8f,-12.5f,-8.6f})ring(h,4.49f,y,y+.55f,.4f,RED,false);
        deck(h,4.3f,-17.9f,.55f,TITANIUM);
        deck(h,4,-4.2f,.45f,GRAPHITE);
        cabin(m,3.65f,-25.5f,-18f,RED,IVORY,3);
        nose(h,3.65f,-25.5f,-31.6f,IVORY,RED,9);
        box(h,TITANIUM,0,0,0,0,-.17f,-32,-.17f,.34f,.65f,.34f);
        for(int i=0;i<4;i++){
            float a=(float)(i*Math.PI/2);
            box(h,GRAPHITE,a,0,0,0,-1.15f,-15.8f,4.2f,2.3f,4.8f,.42f);
            box(h,TITANIUM,a,0,0,0,-.9f,-15.35f,4.58f,1.8f,3.5f,.17f);
            for(int j=0;j<4;j++)box(h,RED,a,0,0,0,-.7f,-15+j*.78f,4.73f,1.4f,.22f,.13f);
            box(h,TITANIUM,a,0,0,0,-.72f,-7,3.8f,1.44f,3.2f,.95f);
            box(h,GRAPHITE,a,0,0,0,-.46f,-4.6f,4.55f,.92f,4.1f,.75f);
            box(h,IVORY,a,0,0,0,-1.12f,-.5f,4.05f,2.24f,.5f,1.8f);
            box(h,GOLD,a,0,0,0,-.31f,-3.9f,5.25f,.62f,1.7f,.28f);
            h.add(part(i%2==0?CYAN:0xffffab6f,false,true,a,0,0,0,new float[][]{{-.25f,-10.65f,4.45f,.5f,.3f,.14f}}));
        }
        for(float x:new float[]{-1.8f,1.8f})for(float z:new float[]{-1.8f,1.8f})engine(m,x,z,1.0f,false);
    }
    // Mk II: tall 3-section orbital ship, slender upper stage, four tanks and five engines.
    private static void buildOrbital(VehicleModel m){
        var h=m.hull;
        ring(h,4.9f,-7,-3.2f,.42f,GRAPHITE,false);
        ring(h,4.65f,-19,-7,.36f,TITANIUM,false);
        ring(h,4.79f,-19,-17.9f,.43f,BLUE,false);
        deck(h,4.6f,-4.1f,.4f,GRAPHITE);
        deck(h,4.55f,-19,.45f,BLUE);
        ring(h,4.0f,-30,-19,.32f,WHITE,false);
        for(float y:new float[]{-29.9f,-24.4f,-19.8f})ring(h,4.16f,y,y+.6f,.38f,BLUE,false);
        cabin(m,3.35f,-38.5f,-30,BLUE,GRAPHITE,4);
        nose(h,3.35f,-38.5f,-46.8f,BLUE,TITANIUM,12);
        box(h,TITANIUM,0,0,0,0,-.18f,-48,-.18f,.36f,1.4f,.36f);
        for(int i=0;i<4;i++){
            float a=(float)(i*Math.PI/2);
            // External propellant pods, ribbed cooling banks and retractable skid frames.
            box(h,BLUE,a,0,0,0,-1.15f,-17.5f,4.5f,2.3f,9.5f,1.12f);
            box(h,WHITE,a,0,0,0,-.88f,-16.9f,5.55f,1.76f,8.2f,.21f);
            for(int j=0;j<8;j++)box(h,GRAPHITE,a,0,0,0,-.9f,-16.5f+j*.97f,5.7f,1.8f,.28f,.12f);
            box(h,GRAPHITE,a,0,0,0,-1.25f,-27.9f,3.9f,2.5f,6.2f,.25f);
            for(int j=0;j<7;j++)box(h,TITANIUM,a,0,0,0,-1.03f,-27.5f+j*.82f,4.12f,2.06f,.24f,.2f);
            box(h,COPPER,a,0,0,0,-.22f,-23.9f,4.35f,.44f,5.9f,.22f);
            box(h,BLUE,a,0,0,0,-.9f,-7,4.35f,1.8f,4.2f,.85f);
            box(h,TITANIUM,a,0,0,0,-.35f,-4.5f,5.1f,.7f,4.2f,.56f);
            box(h,GRAPHITE,a,0,0,0,-1.02f,-.4f,4.3f,2.04f,.4f,1.48f);
            h.add(part(CYAN,false,true,a,0,0,0,new float[][]{{-.65f,-20.3f,4.72f,1.3f,.26f,.12f},{-.26f,-8.3f,5.62f,.52f,.28f,.13f}}));
            // Small attitude-control thruster clusters high on the upper stage.
            box(h,GRAPHITE,a,0,0,0,-.72f,-29.3f,4.02f,1.44f,.9f,.72f);
            box(h,TITANIUM,a,0,0,0,-.5f,-29.05f,4.72f,.42f,.42f,.16f);
            box(h,TITANIUM,a,0,0,0,.08f,-29.05f,4.72f,.42f,.42f,.16f);
        }
        engine(m,0,0,1.32f,true);
        for(float[] p:new float[][]{{3.2f,0},{-3.2f,0},{0,3.2f},{0,-3.2f}})engine(m,p[0],p[1],.82f,true);
    }
    private static void box(List<Piece> out,int color,float angle,float x,float y,float z,float bx,float by,float bz,float w,float h,float d){
        out.add(part(color,false,false,angle,x,y,z,new float[][]{{bx,by,bz,w,h,d}}));
    }
    private static void cabin(VehicleModel m,float radius,float top,float floor,int trim,int panel,int solidEvery){
        var h=m.hull;
        deck(h,radius,floor-.2f,.4f,GRAPHITE);
        ring(h,radius,top,top+.7f,.35f,trim,false);
        ring(h,radius,floor-.85f,floor,.35f,trim,false);
        deck(h,radius,top-.25f,.4f,panel);
        float half=(float)(radius*Math.tan(Math.PI/SIDES)),height=floor-top-1.55f;
        for(int i=0;i<SIDES;i++){
            float a=(float)(i*2*Math.PI/SIDES);
            box(h,TITANIUM,a,0,0,0,-half-.1f,top+.7f,radius-.2f,.22f,height,.36f);
            if(i%solidEvery==1)box(h,panel,a,0,0,0,-half+.12f,top+.7f,radius-.2f,half*2-.24f,height,.24f);
            else{
                m.windows.add(part(0x554c91af,true,false,a,0,0,0,new float[][]{{-half+.12f,top+.7f,radius-.16f,half*2-.24f,height,.08f}}));
                box(h,TITANIUM,a,0,0,0,-half+.12f,top+height*.48f,radius-.13f,half*2-.24f,.17f,.13f);
            }
        }
        box(h,GRAPHITE,0,0,0,0,-1.1f,floor-.55f,-.4f,2.2f,.55f,1.85f);
        box(h,GRAPHITE,0,0,0,0,-1.1f,floor-2.6f,1.05f,2.2f,2.6f,.35f);
        box(h,panel,0,0,0,0,-2.05f,floor-1.45f,-radius+.18f,4.1f,1.45f,.7f);
        h.add(part(CYAN,false,true,0,0,0,0,new float[][]{{-1.65f,floor-1.5f,-radius+.35f,1.2f,.14f,.38f},{.4f,floor-1.5f,-radius+.35f,1.2f,.14f,.38f}}));
    }
    private static void nose(List<Piece> h,float radius,float base,float tip,int main,int accent,int segments){
        float step=(base-tip)/segments;
        for(int i=0;i<segments;i++){
            float t=(float)i/segments,rad=Math.max(.22f,radius*(1-t*t));
            float bottom=base-i*step;
            int color=i==0||i>=segments-2?accent:main;
            ring(h,rad,bottom-step-.02f,bottom,.33f,color,false);
            deck(h,Math.max(.15f,rad-.08f),bottom-step,.17f,color);
        }
    }
    private static void engine(VehicleModel m,float x,float z,float size,boolean orbital){
        var h=m.hull;
        box(h,GRAPHITE,0,x,0,z,-.55f*size,-3.7f,-.55f*size,1.1f*size,1.3f,1.1f*size);
        nozzle(h,x,z,.64f*size,-2.8f,-1.75f,COPPER);
        nozzle(h,x,z,.91f*size,-1.75f,-.72f,GRAPHITE);
        nozzle(h,x,z,1.18f*size,-.72f,-.15f,TITANIUM);
        box(h,GRAPHITE,0,x,0,z,-.5f*size,-2.6f,-.5f*size,size,.18f,size);
        m.flames.add(part(orbital?0x9b54adff:0xbdf78b3a,true,true,0,x,0,z,new float[][]{{-.74f*size,-.2f,-.74f*size,1.48f*size,1.9f,1.48f*size},{-.42f*size,1.7f,-.42f*size,.84f*size,1.4f,.84f*size},{-.17f*size,3.1f,-.17f*size,.34f*size,1.0f,.34f*size}}));
        m.flames.add(part(0xe6d7faff,true,true,0,x,0,z,new float[][]{{-.33f*size,-.2f,-.33f*size,.66f*size,1.6f,.66f*size},{-.14f*size,1.4f,-.14f*size,.28f*size,1.2f,.28f*size}}));
    }
    private static void nozzle(List<Piece> out,float x,float z,float radius,float top,float bottom,int color){
        for(int i=0;i<8;i++){
            float angle=(float)(i*Math.PI/4),half=radius*.4143f;
            out.add(part(color,false,false,angle,x,0,z,new float[][]{{-half-.04f,top,radius-.18f,half*2+.08f,bottom-top,.2f}}));
        }
    }
    private static void ring(List<Piece> out,float radius,float top,float bottom,float thickness,int color,boolean glass){
        float half=(float)(radius*Math.tan(Math.PI/SIDES));
        for(int i=0;i<SIDES;i++)out.add(part(color,glass,false,(float)(i*2*Math.PI/SIDES),0,0,0,
            new float[][]{{-half-.035f,top,radius-thickness,half*2+.07f,bottom-top,thickness}}));
    }
    private static void deck(List<Piece> out,float radius,float top,float height,int color){
        // Three overlapping, rotated rectangles approximate a twelve-sided solid disk.
        for(int i=0;i<3;i++)out.add(part(color,false,false,(float)(i*Math.PI/3),0,0,0,new float[][]{{-radius*.57735f,top,-radius,1.1547f*radius,height,2*radius}}));
    }
    private static Piece part(int color,boolean glass,boolean lit,float angle,float x,float y,float z,float[][] boxes){
        ModelData data=new ModelData();
        ModelPartBuilder builder=ModelPartBuilder.create();
        for(float[] b:boxes)builder.uv(0,0).cuboid(b[0],b[1],b[2],b[3],b[4],b[5]);
        ModelPart model=data.getRoot().addChild("piece",builder,ModelTransform.of(x,y,z,0,angle,0)).createPart(128,128);
        return new Piece(model,color,glass,lit);
    }
    private static void draw(Piece p,MatrixStack matrices,VertexConsumerProvider vertices,int light){
        RenderLayer layer=p.glass?RenderLayer.getEntityTranslucent(CLEAR):RenderLayer.getEntityCutoutNoCull(ALLOY);
        p.model.render(matrices,vertices.getBuffer(layer),p.lit?0x00F000F0:light,OverlayTexture.DEFAULT_UV,p.color);
    }
    @Override public Identifier getTexture(RocketEntity entity){return ALLOY;}
    @Override public void render(RocketEntity entity,float yaw,float tickDelta,MatrixStack matrices,VertexConsumerProvider vertices,int light){
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
        matrices.scale(-4,-4,4);
        VehicleModel model=entity.advanced()?mk2:mk1;
        for(Piece p:model.hull)draw(p,matrices,vertices,light);
        for(Piece p:model.windows)draw(p,matrices,vertices,light);
        if(entity.enginesOn()||entity.warmingUp()){
            matrices.push();
            float pulse=.88f+.12f*(float)Math.sin((entity.age+tickDelta)*1.6);
            matrices.scale(1,pulse*(entity.warmingUp()?.23f:entity.phase()==RocketEntity.DESCENT?.65f:1f),1);
            for(Piece p:model.flames)draw(p,matrices,vertices,light);
            matrices.pop();
        }
        matrices.pop();
        super.render(entity,yaw,tickDelta,matrices,vertices,light);
    }
}

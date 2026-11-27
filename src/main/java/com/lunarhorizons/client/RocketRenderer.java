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

/** Eight-block tall twelve-sided expedition lander with a transparent cockpit.
 * Model coordinates are quarter-block units, with negative Y pointing up. */
public final class RocketRenderer extends EntityRenderer<RocketEntity> {
    private static final Identifier ALLOY=Identifier.of("lunar_horizons","textures/entity/rocket_alloy.png");
    private static final Identifier CLEAR=Identifier.ofVanilla("textures/block/white_concrete.png");
    private static final int IVORY=0xffdeded5, TITANIUM=0xffa4adb1, GRAPHITE=0xff424a51, RED=0xff92544a, GOLD=0xffb19a6f;
    private static final int SIDES=12;
    private record Piece(ModelPart model,int color,boolean glass,boolean lit){}
    private final List<Piece> hull=new ArrayList<>(),windows=new ArrayList<>(),flames=new ArrayList<>();

    public RocketRenderer(EntityRendererFactory.Context context){
        super(context);
        shadowRadius=1.45f;
        // Hollow segmented cylindrical shell; separate rings visibly divide the stages.
        ring(hull,4.0f,-17.8f,-7.2f,.25f,IVORY,false);
        ring(hull,3.8f,-7.2f,-4.0f,.35f,GRAPHITE,false);
        ring(hull,4.13f,-17.8f,-17.1f,.30f,TITANIUM,false);
        ring(hull,4.13f,-10.0f,-9.6f,.30f,TITANIUM,false);
        ring(hull,4.1f,-8.6f,-7.8f,.3f,RED,false);
        ring(hull,4.12f,-4.8f,-4.0f,.30f,TITANIUM,false);
        deck(hull,3.8f,-4.1f,.45f,GRAPHITE);
        deck(hull,3.95f,-18.0f,.55f,TITANIUM);
        deck(hull,3.65f,-25.6f,.65f,IVORY);
        // Panoramic cabin: twelve facets, narrow pillars, a real interior at the seat height.
        ring(hull,3.95f,-19.0f,-18.0f,.3f,RED,false);
        ring(hull,3.75f,-25.5f,-24.7f,.28f,TITANIUM,false);
        for(int i=0;i<SIDES;i++){
            double angle=2*Math.PI*i/SIDES;
            float radius=3.78f,half=(float)(radius*Math.tan(Math.PI/SIDES));
            float a=(float)angle;
            hull.add(part(TITANIUM,false,false,a,0,0,0,new float[][]{{-half-.12f,-24.7f,radius-.16f,.24f,5.7f,.34f}}));
            if(i%3==1){
                hull.add(part(IVORY,false,false,a,0,0,0,new float[][]{{-half,-24.7f,radius-.2f,half*2,5.7f,.25f}}));
            }else{
                windows.add(part(0x465d879e,true,false,a,0,0,0,new float[][]{{-half+.12f,-24.7f,radius-.16f,half*2-.24f,5.7f,.08f}}));
            }
        }
        hull.add(part(GRAPHITE,false,false,0,0,0,0,new float[][]{
            {-1.35f,-18.6f,-.2f,2.7f,.55f,2.0f},{-1.35f,-21.0f,1.35f,2.7f,2.4f,.42f},
            {-2.3f,-19.45f,-3.3f,4.6f,1.4f,.65f}
        }));
        hull.add(part(0xff83c2c7,false,true,0,0,0,0,new float[][]{
            {-1.8f,-19.52f,-3.17f,1.3f,.14f,.48f},{.48f,-19.52f,-3.17f,1.3f,.14f,.48f}
        }));
        // Multi-stage conical nose, stepped at quarter-block scale.
        float[] radii={3.65f,3.4f,3.1f,2.75f,2.35f,1.9f,1.4f,.85f,.36f};
        for(int i=0;i<radii.length;i++){
            float bottom=-25.55f-i*.70f;
            ring(hull,radii[i],bottom-.72f,bottom,.38f,i==0||i>=7?RED:IVORY,false);
            deck(hull,radii[i]-.10f,bottom-.72f,.24f,i>=7?RED:IVORY);
        }
        hull.add(part(TITANIUM,false,false,0,0,0,0,new float[][]{{-.15f,-32f,-.15f,.3f,.5f,.3f}}));
        // Four radial equipment bays, access panels, handholds, and landing gear.
        for(int i=0;i<4;i++){
            float angle=i*(float)Math.PI/2;
            hull.add(part(GRAPHITE,false,false,angle,0,0,0,new float[][]{
                {-1.10f,-16.4f,3.98f,2.2f,4.4f,.24f},
                {-.85f,-15.95f,4.22f,1.7f,.42f,.18f},
                {-.85f,-12.9f,4.22f,1.7f,.42f,.18f},
                {-.64f,-7.1f,3.62f,1.28f,4.7f,.73f}
            }));
            hull.add(part(TITANIUM,false,false,angle,0,0,0,new float[][]{
                {-.86f,-15.45f,4.22f,1.72f,2.2f,.14f},
                {-.17f,-13.1f,4.41f,.34f,.45f,.13f},
                {-.57f,-7.3f,4.3f,1.14f,3.1f,.50f},
                {-.44f,-4.3f,4.7f,.88f,3.5f,.56f},
                {-1.07f,-.55f,4.0f,2.14f,.55f,1.95f}
            }));
            hull.add(part(RED,false,false,angle,0,0,0,new float[][]{
                {-.70f,-6.8f,4.85f,1.40f,1.8f,.48f}
            }));
            hull.add(part(GOLD,false,false,angle,0,0,0,new float[][]{
                {-.33f,-4.0f,5.28f,.66f,1.5f,.25f}
            }));
            hull.add(part(i%2==0?0xff8fc6bd:0xffc19b72,false,true,angle,0,0,0,new float[][]{
                {-.32f,-11.65f,4.16f,.64f,.34f,.12f}
            }));
        }
        // Four recessed flared bells, with hollow dark openings and silver nozzle lips.
        for(float x:new float[]{-1.8f,1.8f})for(float z:new float[]{-1.8f,1.8f}){
            hull.add(part(GRAPHITE,false,false,0,x,0,z,new float[][]{{-.69f,-3.75f,-.69f,1.38f,1.05f,1.38f}}));
            nozzle(x,z,.78f,-2.75f,-1.7f,TITANIUM);
            nozzle(x,z,1.03f,-1.7f,-.75f,GRAPHITE);
            nozzle(x,z,1.24f,-.75f,-.28f,TITANIUM);
            hull.add(part(0xff242b2e,false,false,0,x,0,z,new float[][]{{-.76f,-2.6f,-.76f,1.52f,.18f,1.52f}}));
            flames.add(part(0xbcf78b3a,true,true,0,x,0,z,new float[][]{
                {-.79f,-.35f,-.79f,1.58f,1.8f,1.58f},{-.54f,1.45f,-.54f,1.08f,1.35f,1.08f},{-.22f,2.8f,-.22f,.44f,1.2f,.44f}
            }));
            flames.add(part(0xe6b8ebff,true,true,0,x,0,z,new float[][]{
                {-.40f,-.35f,-.40f,.8f,1.45f,.8f},{-.18f,1.10f,-.18f,.36f,1.55f,.36f}
            }));
        }
    }

    private void nozzle(float x,float z,float radius,float top,float bottom,int color){
        for(int i=0;i<8;i++){
            float angle=(float)(i*Math.PI/4),half=radius*.4143f;
            // Rotated panels have their pivot at the individual nozzle center.
            hull.add(part(color,false,false,angle,x,0,z,new float[][]{{-half-.04f,top,radius-.18f,half*2+.08f,bottom-top,.2f}}));
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
        for(Piece p:hull)draw(p,matrices,vertices,light);
        for(Piece p:windows)draw(p,matrices,vertices,light);
        if(entity.enginesOn()||entity.warmingUp()){
            matrices.push();
            float pulse=.88f+.12f*(float)Math.sin((entity.age+tickDelta)*1.6);
            matrices.scale(1,pulse*(entity.warmingUp()?.23f:entity.phase()==RocketEntity.DESCENT?.65f:1f),1);
            for(Piece p:flames)draw(p,matrices,vertices,light);
            matrices.pop();
        }
        matrices.pop();
        super.render(entity,yaw,tickDelta,matrices,vertices,light);
    }
}

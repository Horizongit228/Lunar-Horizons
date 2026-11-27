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

/** Separate full-size vehicle model; one model unit is 1/4 of a block. */
public final class RocketRenderer extends EntityRenderer<RocketEntity> {
    private static final Identifier TEXTURE = Identifier.of("lunar_horizons","textures/entity/rocket_alloy.png");
    private static final Identifier CLEAR_TEXTURE = Identifier.ofVanilla("textures/block/white_concrete.png");
    private record Piece(ModelPart model,int color,boolean glass) {}
    private final Piece[] pieces;
    private final Piece flameOuter, flameCore;

    public RocketRenderer(EntityRendererFactory.Context context) {
        super(context);
        shadowRadius=1.45f;
        flameOuter=part(0xeaff8833,true,plumes(1.0f,5.0f));
        flameCore=part(0xfaa1ebff,true,plumes(.43f,3.8f));
        pieces = new Piece[]{
            // Fuselage (the 8-block height is modeled as 32 units).
            part(0xffe3e1d7,false,new float[][]{
                {-4,-17,-3,8,13,6},{-3,-17,-4,6,13,8},
                {-4,-18,-3,8,1,6},{-3,-18,-4,6,1,8},
                {-4,-26,-3,8,1,6},{-3,-26,-4,6,1,8},
                {-3,-27,-3,6,1,6},
                // White skin and corner posts around the open cabin.
                {-4,-25,-4,1,7,1},{3,-25,-4,1,7,1},
                {-4,-25,3,1,7,1},{3,-25,3,1,7,1},
                // Fuel pipe covers and landing legs.
                {-5,-9,-1,1,4,2},{4,-9,-1,1,4,2},
                {-1,-9,-5,2,4,1},{-1,-9,4,2,4,1}
            }),
            part(0xff925449,false,new float[][]{
                {-4.1f,-14,-3.1f,8.2f,2,6.2f},{-3.1f,-14,-4.1f,6.2f,2,8.2f},
                {-4.1f,-18.5f,-3.1f,8.2f,.5f,6.2f},
                {-3.1f,-18.5f,-4.1f,6.2f,.5f,8.2f},
                // Tapered nose cone.
                {-3,-29,-3,6,2,6},{-2.5f,-30,-2.5f,5,1,5},
                {-1.5f,-31,-1.5f,3,1,3},{-.75f,-32,-.75f,1.5f,1,1.5f},
                // Four radial fins, individually stepped.
                {-6,-5,-.5f,2,3,1},{-5,-8,-.5f,1,3,1},
                {4,-5,-.5f,2,3,1},{4,-8,-.5f,1,3,1},
                {-.5f,-5,-6,1,3,2},{-.5f,-8,-5,1,3,1},
                {-.5f,-5,4,1,3,2},{-.5f,-8,4,1,3,1}
            }),
            part(0xff4b5358,false,new float[][]{
                {-3.6f,-4,-3.6f,7.2f,1,7.2f},
                {-3.2f,-16,-4.1f,6.4f,.35f,.2f},{-3.2f,-10,-4.1f,6.4f,.35f,.2f},
                {-4.1f,-16,-3.2f,.2f,.35f,6.4f},{4,-16,-3.2f,.2f,.35f,6.4f},
                {-3.2f,-16,4,6.4f,.35f,.2f},
                // Cockpit seat and a low instrument console.
                {-1.5f,-18.5f,-.5f,3,.5f,2.4f},{-1.5f,-21,1.2f,3,2.5f,.5f},
                {-2,-19.8f,-3,4,1.8f,.9f},
                // Four engine bells.
                {-3,-3,-3,2.4f,2.4f,2.4f},{.6f,-3,-3,2.4f,2.4f,2.4f},
                {-3,-3,.6f,2.4f,2.4f,2.4f},{.6f,-3,.6f,2.4f,2.4f,2.4f}
            }),
            part(0xff8e9ba1,false,new float[][]{
                {-3.25f,-1,-3.25f,2.9f,1,2.9f},{.35f,-1,-3.25f,2.9f,1,2.9f},
                {-3.25f,-1,.35f,2.9f,1,2.9f},{.35f,-1,.35f,2.9f,1,2.9f},
                {-4.05f,-25,-4.05f,8.1f,.4f,.3f},{-4.05f,-25,3.75f,8.1f,.4f,.3f},
                {-4.05f,-25,-3.75f,.3f,.4f,7.5f},{3.75f,-25,-3.75f,.3f,.4f,7.5f},
                {-4.05f,-19,-4.05f,8.1f,.4f,.3f},{-4.05f,-19,3.75f,8.1f,.4f,.3f},
                {-4.05f,-19,-3.75f,.3f,.4f,7.5f},{3.75f,-19,-3.75f,.3f,.4f,7.5f},
                {-6,-2,-1,2,1,2},{4,-2,-1,2,1,2},{-1,-2,-6,2,1,2},{-1,-2,4,2,1,2}
            }),
            part(0xff84b6ba,false,new float[][]{
                {-1.5f,-20,-2.95f,1.25f,.2f,.55f},{.15f,-20,-2.95f,1.25f,.2f,.55f},
                {-1,-12,-4.15f,2,1.25f,.15f},{-4.15f,-12,-1,.15f,1.25f,2},
                {4,-12,-1,.15f,1.25f,2},{-1,-12,4,2,1.25f,.15f}
            }),
            // External service panels, reinforcement ribs and landing struts.
            part(0xff9fa9aa,false,new float[][]{
                {-3,-15.5f,-4.18f,6,.45f,.25f},{-3,-7.2f,-4.18f,6,.45f,.25f},
                {-3,-15.5f,3.93f,6,.45f,.25f},{-3,-7.2f,3.93f,6,.45f,.25f},
                {-4.18f,-15.5f,-3,.25f,.45f,6},{3.93f,-15.5f,-3,.25f,.45f,6},
                {-4.18f,-7.2f,-3,.25f,.45f,6},{3.93f,-7.2f,-3,.25f,.45f,6},
                {-5.6f,-6,-.7f,.8f,4.1f,1.4f},{4.8f,-6,-.7f,.8f,4.1f,1.4f},
                {-.7f,-6,-5.6f,1.4f,4.1f,.8f},{-.7f,-6,4.8f,1.4f,4.1f,.8f},
                {-3.4f,-26.5f,-3.4f,6.8f,.3f,6.8f}
            }),
            part(0xff40494e,false,new float[][]{
                {-2.7f,-16.8f,-4.23f,1.25f,4.5f,.2f},{1.45f,-16.8f,-4.23f,1.25f,4.5f,.2f},
                {-2.7f,-16.8f,4.03f,1.25f,4.5f,.2f},{1.45f,-16.8f,4.03f,1.25f,4.5f,.2f},
                {-4.23f,-16.8f,-2.7f,.2f,4.5f,1.25f},{4.03f,-16.8f,1.45f,.2f,4.5f,1.25f}
            }),
            part(0xffb99c68,false,new float[][]{
                {-2.7f,-12.1f,-4.3f,1.25f,.3f,.14f},{1.45f,-12.1f,-4.3f,1.25f,.3f,.14f},
                {-3.9f,-3.7f,-3.9f,7.8f,.35f,7.8f}
            }),
            // Windows allow an actual first-person view from inside the cockpit.
            part(0x385d8da5,true,new float[][]{
                {-3,-24.6f,-3.92f,6,5.6f,.14f},{-3,-24.6f,3.78f,6,5.6f,.14f},
                {-3.92f,-24.6f,-3,.14f,5.6f,6},{3.78f,-24.6f,-3,.14f,5.6f,6}
            })
        };
    }

    private static float[][] plumes(float radius,float length){
        float[][] boxes=new float[12][];int i=0;
        for(float x:new float[]{-1.8f,1.8f})for(float z:new float[]{-1.8f,1.8f}){
            boxes[i++]=new float[]{x-radius,0,z-radius,2*radius,length*.42f,2*radius};
            boxes[i++]=new float[]{x-radius*.66f,length*.42f,z-radius*.66f,1.32f*radius,length*.33f,1.32f*radius};
            boxes[i++]=new float[]{x-radius*.3f,length*.75f,z-radius*.3f,.6f*radius,length*.25f,.6f*radius};
        }
        return boxes;
    }
    private static Piece part(int color,boolean glass,float[][] boxes) {
        ModelData data=new ModelData();
        ModelPartBuilder builder=ModelPartBuilder.create();
        int index=0;
        for(float[] b:boxes){ int u=(index%3)*32, v=((index/3)%3)*32; builder.uv(u,v).cuboid(b[0],b[1],b[2],b[3],b[4],b[5]); index++; }
        ModelPart model=data.getRoot().addChild("piece",builder,ModelTransform.NONE).createPart(128,128);
        return new Piece(model,color,glass);
    }

    @Override public Identifier getTexture(RocketEntity entity){return TEXTURE;}
    @Override public void render(RocketEntity entity,float yaw,float tickDelta,MatrixStack matrices,VertexConsumerProvider vertices,int light){
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
        // ModelPart uses positive Y down, while world coordinates use positive Y up.
        matrices.scale(-4,-4,4);
        for(Piece piece:pieces){
            RenderLayer layer=piece.glass?RenderLayer.getEntityTranslucent(CLEAR_TEXTURE):RenderLayer.getEntityCutoutNoCull(TEXTURE);
            piece.model.render(matrices,vertices.getBuffer(layer),light,OverlayTexture.DEFAULT_UV,piece.color);
        }
        if(entity.enginesOn()||entity.warmingUp()){
            matrices.push();
            float pulse=.80f+.20f*(float)Math.sin((entity.age+tickDelta)*1.3);
            matrices.scale(1,pulse*(entity.warmingUp()?.22f:entity.phase()==RocketEntity.DESCENT?.66f:1f),1);
            for(Piece flame:new Piece[]{flameOuter,flameCore})
                flame.model.render(matrices,vertices.getBuffer(RenderLayer.getEntityTranslucent(CLEAR_TEXTURE)),0x00F000F0,OverlayTexture.DEFAULT_UV,flame.color);
            matrices.pop();
        }
        matrices.pop();
        super.render(entity,yaw,tickDelta,matrices,vertices,light);
    }
}

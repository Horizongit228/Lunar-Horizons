package com.lunarhorizons.client;

import com.lunarhorizons.LunarHorizons;
import com.lunarhorizons.MeteorEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

/** Irregular rotating solid meteorite, using the existing meteorite material atlas. */
public final class MeteorRenderer extends EntityRenderer<MeteorEntity>{
    private static final float[][] ROCK={
        {0,0,0,1.0f},{-.38f,.16f,.12f,.62f},{.43f,-.08f,-.14f,.58f},
        {.10f,.40f,-.19f,.60f},{-.13f,-.35f,.18f,.54f},{.12f,.06f,.43f,.57f},
        {-.18f,-.13f,-.43f,.52f}
    };
    public MeteorRenderer(EntityRendererFactory.Context context){super(context);shadowRadius=.5f;}
    @Override public Identifier getTexture(MeteorEntity entity){return LunarHorizons.id("textures/block/meteorite_rock.png");}
    @Override public void render(MeteorEntity entity,float yaw,float delta,MatrixStack matrices,VertexConsumerProvider vertices,int light){
        matrices.push();
        float size=entity.huge()?3.4f:.88f,age=entity.age+delta;
        matrices.translate(0,size*.68f,0);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(age*3.1f));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(age*1.7f));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(19));
        matrices.scale(size,size,size);
        var renderer=MinecraftClient.getInstance().getBlockRenderManager();
        var state=LunarHorizons.METEORITE_ROCK.getDefaultState();
        for(float[] b:ROCK){
            matrices.push();
            matrices.translate(b[0]-b[3]/2,b[1]-b[3]/2,b[2]-b[3]/2);
            matrices.scale(b[3],b[3],b[3]);
            renderer.renderBlockAsEntity(state,matrices,vertices,light,OverlayTexture.DEFAULT_UV);
            matrices.pop();
        }
        matrices.pop();
        super.render(entity,yaw,delta,matrices,vertices,light);
    }
}

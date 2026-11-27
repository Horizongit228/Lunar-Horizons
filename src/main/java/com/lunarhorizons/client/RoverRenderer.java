package com.lunarhorizons.client;
import com.lunarhorizons.RoverEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import java.util.ArrayList;
import java.util.List;

/** Open-cab six-wheel electric expedition rover, approx. two blocks across. */
public final class RoverRenderer extends EntityRenderer<RoverEntity>{
    private static final Identifier METAL=Identifier.of("lunar_horizons","textures/entity/rocket_alloy.png");
    private record Piece(ModelPart model,int color,boolean light){}
    private final List<Piece> body=new ArrayList<>(),wheels=new ArrayList<>();
    public RoverRenderer(EntityRendererFactory.Context context){
        super(context);shadowRadius=.9f;
        body.add(part(0xff3d464b,false,0,0,0,new float[][]{
            {-12,-7,-13,24,3,26},{-10,-9,-11,20,2,22},
            {-12,-9,-12,24,2,3},{-12,-9,9,24,2,3},
            {-7,-15,2,14,6,9},{-6,-17,7,12,2,3},
            {-5,-13,-10,10,4,5}
        }));
        body.add(part(0xffd0d2cb,false,0,0,0,new float[][]{
            {-10,-12,-12,20,3,6},{-10,-12,6,20,3,6},
            {-10,-10,-6,3,3,12},{7,-10,-6,3,3,12},
            {-9,-16,-11,18,4,3},{-7,-11,3,14,2,7}
        }));
        body.add(part(0xff8a999d,false,0,0,0,new float[][]{
            {-11,-20,9,1,11,1},{10,-20,9,1,11,1},
            {-11,-20,-6,1,11,1},{10,-20,-6,1,11,1},
            {-11,-20,-6,22,1,1},{-11,-20,9,22,1,1},
            {-11,-20,-5,1,1,14},{10,-20,-5,1,1,14},
            {-10,-15,11,1,5,1},{9,-15,11,1,5,1},
            {8,-19,8,1,1,1},{8,-24,8,.35f,5,.35f}
        }));
        body.add(part(0xffb79c62,false,0,0,0,new float[][]{
            {-10,-12,-12,20,.8f,.3f},{-10,-12,11.8f,20,.8f,.3f},
            {-10,-11,-6,.35f,1,12},{9.7f,-11,-6,.35f,1,12}
        }));
        body.add(part(0xffb8e3dd,true,0,0,0,new float[][]{
            {-8,-14,-12.1f,4,1.6f,.4f},{4,-14,-12.1f,4,1.6f,.4f},
            {-4,-15.7f,-8.2f,8,.35f,1.6f}
        }));
        body.add(part(0xff9f5145,true,0,0,0,new float[][]{
            {-8,-11,12,3,1,.35f},{5,-11,12,3,1,.35f}
        }));
        for(int side:new int[]{-1,1})for(int z:new int[]{-10,0,10}){
            float x=side*12.2f;
            body.add(part(0xff78878e,false,0,0,0,new float[][]{{side<0?-13:8,-6.5f,z-1,5,2,2}}));
            List<float[]> tire=new ArrayList<>();
            for(int i=0;i<8;i++){
                double angle=i*Math.PI/4;
                float yy=(float)Math.cos(angle)*2.1f,zz=(float)Math.sin(angle)*2.1f;
                tire.add(new float[]{-2.0f,yy-2.7f,zz-2.7f,4,5.4f,5.4f});
            }
            wheels.add(part(0xff343b40,false,x,-4.8f,z,tire.toArray(float[][]::new)));
            wheels.add(part(0xffa6afb1,false,x,-4.8f,z,new float[][]{{side<0?-2.25f:1.75f,-2.4f,-2.4f,.5f,4.8f,4.8f}}));
            wheels.add(part(0xff59676f,false,x,-4.8f,z,new float[][]{{side<0?-2.55f:2.05f,-.8f,-.8f,.5f,1.6f,1.6f}}));
        }
    }
    private static Piece part(int color,boolean light,float x,float y,float z,float[][] boxes){
        ModelData data=new ModelData();ModelPartBuilder b=ModelPartBuilder.create();
        for(float[] p:boxes)b.uv(0,0).cuboid(p[0],p[1],p[2],p[3],p[4],p[5]);
        return new Piece(data.getRoot().addChild("piece",b,ModelTransform.pivot(x,y,z)).createPart(128,128),color,light);
    }
    private static void draw(Piece p,MatrixStack matrices,VertexConsumerProvider vertices,int light){
        p.model.render(matrices,vertices.getBuffer(RenderLayer.getEntityCutoutNoCull(METAL)),p.light?0x00F000F0:light,OverlayTexture.DEFAULT_UV,p.color);
    }
    @Override public Identifier getTexture(RoverEntity entity){return METAL;}
    @Override public void render(RoverEntity entity,float yaw,float tickDelta,MatrixStack matrices,VertexConsumerProvider vertices,int light){
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw+180));
        matrices.scale(-1,-1,1);
        for(Piece p:body)draw(p,matrices,vertices,light);
        for(Piece p:wheels)draw(p,matrices,vertices,light);
        matrices.pop();
        super.render(entity,yaw,tickDelta,matrices,vertices,light);
    }
}

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

/** Independent six-wheel expedition and eight-wheel survey rover models. */
public final class RoverRenderer extends EntityRenderer<RoverEntity>{
    private static final Identifier METAL=Identifier.of("lunar_horizons","textures/entity/rocket_alloy.png");
    private record Piece(ModelPart model,int color,boolean light){}
    private record Wheel(Piece piece,int axle){}
    private record Vehicle(List<Piece> body,List<Wheel> wheels){
        Vehicle(){this(new ArrayList<>(),new ArrayList<>());}
    }
    private final Vehicle mk1=new Vehicle(),mk2=new Vehicle();
    private static final int IVORY=0xffd0d2cb, BLACK=0xff333d43, STEEL=0xff9aa9b0, BLUE=0xff365d79, GOLD=0xffb79c62;
    public RoverRenderer(EntityRendererFactory.Context context){
        super(context);shadowRadius=1.05f;
        expedition(mk1);survey(mk2);
    }
    private static void add(Vehicle v,int color,boolean lit,float[]... boxes){
        v.body.add(part(color,lit,0,0,0,boxes));
    }
    private static void expedition(Vehicle v){
        // Low, lightweight six-wheel rover with an exposed tubular roll cage.
        add(v,BLACK,false,new float[]{-10,-7,-13,20,3,26},new float[]{-9,-10,-11,18,3,22});
        add(v,IVORY,false,new float[]{-10,-12,-12,20,2,6},new float[]{-10,-12,6,20,2,6},
            new float[]{-10,-10,-6,2,3,12},new float[]{8,-10,-6,2,3,12},new float[]{-8,-15,-11,16,3,4});
        add(v,GOLD,false,new float[]{-10,-12,-12.1f,20,.7f,.25f},new float[]{-10,-12,11.9f,20,.7f,.25f});
        add(v,BLACK,false,new float[]{-6,-13,1,12,3,7},new float[]{-6,-17,7,12,5,2},new float[]{-5,-14,-8,10,3,3});
        for(int s:new int[]{-1,1}){
            float x=s<0?-10:9;
            add(v,STEEL,false,new float[]{x,-20,-6,1,10,1},new float[]{x,-20,9,1,10,1},new float[]{x,-20,-5,1,1,14},
                new float[]{x,-14,-10,1,3,4});
            for(int z:new int[]{-10,0,10}){
                add(v,STEEL,false,new float[]{s<0?-13:8,-6.5f,z-1,5,2,2});
                wheel(v,s*12.2f,-4.8f,z,4.8f,4,s,z==-10?1:z==10?-1:0);
            }
        }
        add(v,STEEL,false,new float[]{-9,-20,-6,18,1,1},new float[]{-9,-20,9,18,1,1},new float[]{8,-24,8,.35f,5,.35f});
        add(v,0xffd7f0dc,true,new float[]{-8,-14,-12.2f,4,1.5f,.4f},new float[]{4,-14,-12.2f,4,1.5f,.4f});
        add(v,0xffcf6b50,true,new float[]{-8,-11,12,3,1,.35f},new float[]{5,-11,12,3,1,.35f});
        add(v,STEEL,false,new float[]{-7,-11,12,14,1,1},new float[]{-7,-11,-13,14,1,1});
    }
    private static void survey(Vehicle v){
        // Mk II has its own wider chassis, independent eight-wheel suspension,
        // elevated solar canopy, armored lockers and a separate instrument mast.
        add(v,BLACK,false,new float[]{-12,-9,-15,24,4,30},new float[]{-10,-12,-13,20,3,26},
            new float[]{-13,-8,-16,26,2,2},new float[]{-13,-8,14,26,2,2});
        add(v,BLUE,false,new float[]{-12,-14,-14,24,4,7},new float[]{-12,-14,7,24,4,7},
            new float[]{-12,-11,-7,3,2,14},new float[]{9,-11,-7,3,2,14});
        add(v,IVORY,false,new float[]{-10,-16,-13,20,2,6},new float[]{-10,-17,8,20,3,6});
        add(v,STEEL,false,new float[]{-8,-15,-8,16,2,3},new float[]{-7,-14,1,14,2,6});
        add(v,BLACK,false,new float[]{-6,-16,1,12,2,7},new float[]{-6,-20,7,12,5,2});
        for(int s:new int[]{-1,1}){
            float x=s<0?-12:11;
            add(v,STEEL,false,new float[]{x,-24,-8,1,12,1},new float[]{x,-24,9,1,12,1},new float[]{x,-24,-7,1,1,16},
                new float[]{x,-17,-12,1,4,4});
            for(float z:new float[]{-13.5f,-4.5f,4.5f,13.5f}){
                add(v,STEEL,false,new float[]{s<0?-15:10,-8,z-1,5,2.4f,2},new float[]{s<0?-12:11,-12,z-.8f,1,4.5f,1.6f});
                wheel(v,s*14.2f,-5f,z,5f,4.6f,s,z< -10?1:z>10?-1:0);
            }
            add(v,BLUE,false,new float[]{s<0?-12:8,-18,8,4,4,5});
            add(v,STEEL,false,new float[]{s<0?-12.15f:11.85f,-17,9,.3f,1.8f,3});
        }
        add(v,STEEL,false,new float[]{-12,-25,-9,24,1,20});
        add(v,0xff234762,false,new float[]{-11,-25.6f,-8,22,.7f,18});
        for(int i=0;i<6;i++)add(v,STEEL,false,new float[]{-11+i*4.4f,-25.75f,-8,.22f,.2f,18});
        for(int z=-8;z<=10;z+=6)add(v,STEEL,false,new float[]{-11,-25.75f,z,22,.2f,.22f});
        add(v,STEEL,false,new float[]{8,-27,10,1,3,1},new float[]{8,-28,9,3,1,3});
        add(v,0xff8fe9ef,true,new float[]{-10,-15,-14.2f,5,1.8f,.4f},new float[]{5,-15,-14.2f,5,1.8f,.4f},new float[]{9,-28,8.8f,1,.5f,.3f});
        add(v,0xffcf6b50,true,new float[]{-10,-13,14,4,1.2f,.4f},new float[]{6,-13,14,4,1.2f,.4f});
        for(int x=-8;x<=8;x+=4)add(v,BLACK,false,new float[]{x,-17.1f,11,2,.25f,3});
    }
    private static void wheel(Vehicle v,float x,float y,float z,float radius,float width,int side,int axle){
        // Octagonal tread with a visibly separate hub; front/rear axles steer.
        float half=width/2;
        List<float[]> tire=new ArrayList<>();
        tire.add(new float[]{-half,-radius,-radius*.55f,width,radius*.32f,radius*1.1f});
        tire.add(new float[]{-half,-radius*.68f,-radius*.88f,width,radius*1.36f,radius*1.76f});
        tire.add(new float[]{-half,radius*.68f,-radius*.55f,width,radius*.32f,radius*1.1f});
        for(int i=0;i<3;i++){
            float xx=-half+i*width/3;
            tire.add(new float[]{xx,-radius*.5f,-radius*.94f,.38f,radius,.35f});
            tire.add(new float[]{xx,-radius*.5f,radius*.59f,.38f,radius,.35f});
        }
        v.wheels.add(new Wheel(part(BLACK,false,x,y,z,tire.toArray(float[][]::new)),axle));
        v.wheels.add(new Wheel(part(STEEL,false,x,y,z,new float[][]{{side<0?-half-.35f:half-.15f,-radius*.48f,-radius*.48f,.5f,radius*.96f,radius*.96f}}),axle));
        v.wheels.add(new Wheel(part(BLUE,false,x,y,z,new float[][]{{side<0?-half-.6f:half+.1f,-radius*.19f,-radius*.19f,.5f,radius*.38f,radius*.38f}}),axle));
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
        Vehicle v=entity.advanced()?mk2:mk1;
        for(Piece p:v.body)draw(p,matrices,vertices,light);
        for(Wheel w:v.wheels){Piece p=w.piece;p.model.pitch=entity.wheelAngle(tickDelta);p.model.yaw=entity.steeringAngle(tickDelta)*w.axle*(w.axle<0?.45f:1f);draw(p,matrices,vertices,light);}
        matrices.pop();
        super.render(entity,yaw,tickDelta,matrices,vertices,light);
    }
}

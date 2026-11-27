package com.lunarhorizons.client;

import com.lunarhorizons.LunarHorizons;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.Random;

final class LunarSky {
    private static final Identifier EARTH = LunarHorizons.id("textures/environment/earth.png");
    private static final int COUNT = 2600;
    private static final float[][] STARS = makeStars();

    private static float[][] makeStars() {
        Random random = new Random(41021L);
        float[][] result = new float[COUNT][];
        for (int i = 0; i < COUNT; i++) {
            float y = random.nextFloat()*2-1;
            double az = random.nextDouble()*Math.PI*2;
            float radius = (float)Math.sqrt(1-y*y);
            float x = radius*(float)Math.cos(az), z = radius*(float)Math.sin(az);
            float magnitude=random.nextFloat();
            float size = .025f+.115f*magnitude*magnitude*magnitude;
            float light = .16f+.84f*magnitude*magnitude;
            float tint=random.nextFloat();
            result[i] = new float[]{x,y,z,size,light,tint};
        }
        return result;
    }

    static void render(WorldRenderContext context) {
        Matrix4f matrix = context.positionMatrix();
        float oldStart = RenderSystem.getShaderFogStart(), oldEnd = RenderSystem.getShaderFogEnd();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1,1,1,1);
        RenderSystem.setShaderFogStart(10000);
        RenderSystem.setShaderFogEnd(10001);
        try {
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            BufferBuilder bg = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            float s = 110;
            float[][] corners = {{-s,-s,-s},{s,-s,-s},{s,s,-s},{-s,s,-s},{-s,-s,s},{s,-s,s},{s,s,s},{-s,s,s}};
            int[][] faces = {{0,1,2,3},{4,5,6,7},{0,4,7,3},{1,5,6,2},{3,2,6,7},{0,1,5,4}};
            for (int[] face:faces) for (int i:face) {
                float[] c=corners[i]; bg.vertex(matrix,c[0],c[1],c[2]).color(0,0,0,255);
            }
            BufferRenderer.drawWithGlobalProgram(bg.end());
            BufferBuilder stars = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            for (float[] star:STARS) {
                Vector3f normal = new Vector3f(star[0],star[1],star[2]);
                Vector3f right = new Vector3f(0,Math.abs(star[1])>.98f?0:1,Math.abs(star[1])>.98f?1:0).cross(normal).normalize().mul(star[3]);
                Vector3f up = new Vector3f(normal).cross(right).normalize().mul(star[3]);
                Vector3f center = new Vector3f(normal).mul(94);
                int light = (int)(star[4]*255);
                for (int[] sign:new int[][]{{-1,-1},{1,-1},{1,1},{-1,1}}) {
                    Vector3f p = new Vector3f(center).fma(sign[0],right).fma(sign[1],up);
                    int red=(int)(light*(star[5]<.28f?.86f:1f));
                    int blue=(int)(light*(star[5]>.72f?.84f:1f));
                    stars.vertex(matrix,p.x,p.y,p.z).color(red,light,blue,255);
                }
            }
            BufferRenderer.drawWithGlobalProgram(stars.end());
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            RenderSystem.setShaderTexture(0, EARTH);
            // Fixed celestial direction: Earth is visible above the lunar horizon.
            Matrix4f earth = new Matrix4f(matrix).rotateY((float)Math.toRadians(32)).rotateX((float)Math.toRadians(38));
            BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
            float r=7.5f, z=-90;
            b.vertex(earth,-r,r,z).texture(0,0);
            b.vertex(earth,-r,-r,z).texture(0,1);
            b.vertex(earth,r,-r,z).texture(1,1);
            b.vertex(earth,r,r,z).texture(1,0);
            BufferRenderer.drawWithGlobalProgram(b.end());
        } finally {
            RenderSystem.setShaderFogStart(oldStart);
            RenderSystem.setShaderFogEnd(oldEnd);
            RenderSystem.setShaderColor(1,1,1,1);
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
        }
    }
}

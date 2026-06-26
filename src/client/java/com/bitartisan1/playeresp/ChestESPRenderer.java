package com.bitartisan1.playeresp;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.entity.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;

public class ChestESPRenderer implements WorldRenderEvents.AfterTranslucent {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    @Override
    public void afterTranslucent(WorldRenderContext context) {
        if (mc.world == null || mc.player == null) return;
        if (!PlayerESPClient.isChestESPEnabled()) return;

        MatrixStack matrices = context.matrixStack();
        Vec3d cam = context.camera().getPos();
        ClientWorld world = mc.world;

        BlockPos playerPos = mc.player.getBlockPos();
        int chunkRadius = 4;
        ChunkPos centerChunk = new ChunkPos(playerPos);

        for (int cx = centerChunk.x - chunkRadius; cx <= centerChunk.x + chunkRadius; cx++) {
            for (int cz = centerChunk.z - chunkRadius; cz <= centerChunk.z + chunkRadius; cz++) {
                WorldChunk chunk = world.getChunkManager().getWorldChunk(cx, cz);
                if (chunk == null) continue;

                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    float[] color = getColor(be);
                    if (color == null) continue;

                    BlockPos pos = be.getPos();
                    double x = pos.getX() - cam.x;
                    double y = pos.getY() - cam.y;
                    double z = pos.getZ() - cam.z;

                    matrices.push();
                    matrices.translate(x, y, z);
                    renderBox(matrices, color);
                    matrices.pop();
                }
            }
        }
    }

    private float[] getColor(BlockEntity be) {
        if (be instanceof TrappedChestBlockEntity) return new float[]{1.0f, 0.2f, 0.2f};
        if (be instanceof ChestBlockEntity)        return new float[]{1.0f, 0.8f, 0.0f};
        if (be instanceof EnderChestBlockEntity)   return new float[]{0.5f, 0.0f, 1.0f};
        if (be instanceof ShulkerBoxBlockEntity)   return new float[]{1.0f, 0.4f, 0.8f};
        if (be instanceof BarrelBlockEntity)       return new float[]{0.6f, 0.4f, 0.1f};
        return null;
    }

    private void renderBox(MatrixStack matrices, float[] color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        Matrix4f m = matrices.peek().getPositionMatrix();
        float r = color[0], g = color[1], b = color[2];

        addLine(buffer, m, 0,0,0,  1,0,0,  r,g,b);
        addLine(buffer, m, 1,0,0,  1,0,1,  r,g,b);
        addLine(buffer, m, 1,0,1,  0,0,1,  r,g,b);
        addLine(buffer, m, 0,0,1,  0,0,0,  r,g,b);

        addLine(buffer, m, 0,1,0,  1,1,0,  r,g,b);
        addLine(buffer, m, 1,1,0,  1,1,1,  r,g,b);
        addLine(buffer, m, 1,1,1,  0,1,1,  r,g,b);
        addLine(buffer, m, 0,1,1,  0,1,0,  r,g,b);

        addLine(buffer, m, 0,0,0,  0,1,0,  r,g,b);
        addLine(buffer, m, 1,0,0,  1,1,0,  r,g,b);
        addLine(buffer, m, 1,0,1,  1,1,1,  r,g,b);
        addLine(buffer, m, 0,0,1,  0,1,1,  r,g,b);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private void addLine(BufferBuilder buf, Matrix4f m,
                         float x1, float y1, float z1,
                         float x2, float y2, float z2,
                         float r, float g, float b) {
        buf.vertex(m, x1, y1, z1).color(r, g, b, 1.0f);
        buf.vertex(m, x2, y2, z2).color(r, g, b, 1.0f);
    }
}

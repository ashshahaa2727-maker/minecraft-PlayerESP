package com.bitartisan1.playeresp;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.List;

public class PlayerESPRenderer implements WorldRenderEvents.AfterTranslucent {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    @Override
    public void afterTranslucent(WorldRenderContext context) {
        if (mc.world == null || mc.player == null) return;

        MatrixStack matrices = context.matrixStack();
        Vec3d cameraPos = context.camera().getPos();
        float tickDelta = context.tickCounter().getTickDelta(true);

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;

            if (!PlayerESPCommand.isWhitelisted(player.getName().getString())) {
                continue;
            }

            double x = MathHelper.lerp(tickDelta, player.prevX, player.getX()) - cameraPos.x;
            double y = MathHelper.lerp(tickDelta, player.prevY, player.getY()) - cameraPos.y;
            double z = MathHelper.lerp(tickDelta, player.prevZ, player.getZ()) - cameraPos.z;

            matrices.push();
            matrices.translate(x, y, z);

            if (PlayerESPClient.isShowHitbox()) {
                renderHitbox(matrices, player);
            }

            if (PlayerESPClient.isShowName()) {
                renderPlayerName(matrices, player, context.camera().getYaw(), context.camera().getPitch());
            }

            matrices.pop();
        }

        // Render orbit particles around target
        renderOrbitParticles(matrices, cameraPos);

        // Render hit particles
        renderHitParticles(matrices, cameraPos);
    }

    // ===================== ORBIT PARTICLES =====================

    private void renderOrbitParticles(MatrixStack matrices, Vec3d cameraPos) {
        List<TargetESPManager.OrbitParticle> particles = TargetESPManager.getOrbitParticles();
        if (particles.isEmpty()) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Tessellator tessellator = Tessellator.getInstance();

        for (TargetESPManager.OrbitParticle p : particles) {
            double dx = p.pos.x - cameraPos.x;
            double dy = p.pos.y - cameraPos.y;
            double dz = p.pos.z - cameraPos.z;

            matrices.push();
            matrices.translate(dx, dy, dz);

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            float s = p.size;

            // Draw a small glowing diamond shape
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            buffer.vertex(matrix,  0,  s,  0).color(p.r, p.g, p.b, 0.9f);
            buffer.vertex(matrix,  s,  0,  0).color(p.r, p.g, p.b, 0.9f);
            buffer.vertex(matrix,  0, -s,  0).color(p.r, p.g, p.b, 0.9f);
            buffer.vertex(matrix, -s,  0,  0).color(p.r, p.g, p.b, 0.9f);
            BufferRenderer.drawWithGlobalProgram(buffer.end());

            // Second face (perpendicular)
            buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            buffer.vertex(matrix, 0,  s,  0).color(p.r, p.g, p.b, 0.9f);
            buffer.vertex(matrix, 0,  0,  s).color(p.r, p.g, p.b, 0.9f);
            buffer.vertex(matrix, 0, -s,  0).color(p.r, p.g, p.b, 0.9f);
            buffer.vertex(matrix, 0,  0, -s).color(p.r, p.g, p.b, 0.9f);
            BufferRenderer.drawWithGlobalProgram(buffer.end());

            matrices.pop();
        }

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    // ===================== HIT PARTICLES =====================

    private void renderHitParticles(MatrixStack matrices, Vec3d cameraPos) {
        List<TargetESPManager.HitParticle> particles = TargetESPManager.getHitParticles();
        if (particles.isEmpty()) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Tessellator tessellator = Tessellator.getInstance();

        for (TargetESPManager.HitParticle p : particles) {
            double dx = p.pos.x - cameraPos.x;
            double dy = p.pos.y - cameraPos.y;
            double dz = p.pos.z - cameraPos.z;

            matrices.push();
            matrices.translate(dx, dy, dz);

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            float s = p.size;
            float a = p.alpha;

            // Draw a glowing quad (flat facing up-ish)
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            buffer.vertex(matrix, -s, -s,  0).color(p.r, p.g, p.b, a);
            buffer.vertex(matrix,  s, -s,  0).color(p.r, p.g, p.b, a);
            buffer.vertex(matrix,  s,  s,  0).color(p.r, p.g, p.b, a);
            buffer.vertex(matrix, -s,  s,  0).color(p.r, p.g, p.b, a);
            BufferRenderer.drawWithGlobalProgram(buffer.end());

            // Second quad perpendicular
            buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            buffer.vertex(matrix, 0, -s, -s).color(p.r, p.g, p.b, a);
            buffer.vertex(matrix, 0, -s,  s).color(p.r, p.g, p.b, a);
            buffer.vertex(matrix, 0,  s,  s).color(p.r, p.g, p.b, a);
            buffer.vertex(matrix, 0,  s, -s).color(p.r, p.g, p.b, a);
            BufferRenderer.drawWithGlobalProgram(buffer.end());

            matrices.pop();
        }

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    // ===================== HITBOX =====================

    private void renderHitbox(MatrixStack matrices, PlayerEntity player) {
        matrices.push();

        float[] color = getColorFromName(player.getDisplayName().getString());

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        float width = player.getWidth();
        float height = player.getHeight();

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        addLine(buffer, matrix, -width/2, 0, -width/2, -width/2, 0, width/2, color);
        addLine(buffer, matrix, -width/2, 0, width/2, width/2, 0, width/2, color);
        addLine(buffer, matrix, width/2, 0, width/2, width/2, 0, -width/2, color);
        addLine(buffer, matrix, width/2, 0, -width/2, -width/2, 0, -width/2, color);

        addLine(buffer, matrix, -width/2, height, -width/2, -width/2, height, width/2, color);
        addLine(buffer, matrix, -width/2, height, width/2, width/2, height, width/2, color);
        addLine(buffer, matrix, width/2, height, width/2, width/2, height, -width/2, color);
        addLine(buffer, matrix, width/2, height, -width/2, -width/2, height, -width/2, color);

        addLine(buffer, matrix, -width/2, 0, -width/2, -width/2, height, -width/2, color);
        addLine(buffer, matrix, -width/2, 0, width/2, -width/2, height, width/2, color);
        addLine(buffer, matrix, width/2, 0, width/2, width/2, height, width/2, color);
        addLine(buffer, matrix, width/2, 0, -width/2, width/2, height, -width/2, color);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        matrices.pop();
    }

    private void addLine(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float[] color) {
        buffer.vertex(matrix, x1, y1, z1).color(color[0], color[1], color[2], 1.0f);
        buffer.vertex(matrix, x2, y2, z2).color(color[0], color[1], color[2], 1.0f);
    }

    // ===================== NAME TAG =====================

    private void renderPlayerName(MatrixStack matrices, PlayerEntity player, float cameraYaw, float cameraPitch) {
        matrices.push();

        matrices.translate(0, player.getHeight() + 0.5, 0);
        matrices.multiply(new org.joml.Quaternionf().rotationY(-cameraYaw * 0.017453292F));
        matrices.multiply(new org.joml.Quaternionf().rotationX(cameraPitch * 0.017453292F));

        double distance = mc.player.distanceTo(player);
        float scale = (float) Math.max(0.02, Math.min(distance * 0.002, 0.1));
        matrices.scale(-scale, -scale, scale);

        TextRenderer textRenderer = mc.textRenderer;
        Text displayName = player.getDisplayName();
        String nameText = displayName.getString();

        int textWidth = textRenderer.getWidth(nameText);
        int x = -textWidth / 2;
        int y = 0;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        int padding = 2;
        buffer.vertex(matrix, x - padding, y - padding, 0).color(0, 0, 0, 0.5f);
        buffer.vertex(matrix, x - padding, y + textRenderer.fontHeight + padding, 0).color(0, 0, 0, 0.5f);
        buffer.vertex(matrix, x + textWidth + padding, y + textRenderer.fontHeight + padding, 0).color(0, 0, 0, 0.5f);
        buffer.vertex(matrix, x + textWidth + padding, y - padding, 0).color(0, 0, 0, 0.5f);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        textRenderer.draw(nameText, x, y, 0xFFFFFF, false, matrices.peek().getPositionMatrix(), immediate, TextRenderer.TextLayerType.SEE_THROUGH, 0, 15728880);
        immediate.draw();

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        matrices.pop();
    }

    // ===================== COLOR =====================

    private float[] getColorFromName(String name) {
        float[] customColor = PlayerESPCommand.getCustomColor();
        if (customColor != null) {
            return new float[]{customColor[0], customColor[1], customColor[2]};
        }

        for (Formatting format : Formatting.values()) {
            if (name.contains(format.toString())) {
                return getColorFromFormatting(format);
            }
        }

        return new float[]{1.0f, 0.0f, 0.0f};
    }

    private float[] getColorFromFormatting(Formatting format) {
        switch (format) {
            case BLACK: return new float[]{0.0f, 0.0f, 0.0f};
            case DARK_BLUE: return new float[]{0.0f, 0.0f, 0.5f};
            case DARK_GREEN: return new float[]{0.0f, 0.5f, 0.0f};
            case DARK_AQUA: return new float[]{0.0f, 0.5f, 0.5f};
            case DARK_RED: return new float[]{0.5f, 0.0f, 0.0f};
            case DARK_PURPLE: return new float[]{0.5f, 0.0f, 0.5f};
            case GOLD: return new float[]{1.0f, 0.5f, 0.0f};
            case GRAY: return new float[]{0.5f, 0.5f, 0.5f};
            case DARK_GRAY: return new float[]{0.25f, 0.25f, 0.25f};
            case BLUE: return new float[]{0.3f, 0.3f, 1.0f};
            case GREEN: return new float[]{0.3f, 1.0f, 0.3f};
            case AQUA: return new float[]{0.3f, 1.0f, 1.0f};
            case RED: return new float[]{1.0f, 0.3f, 0.3f};
            case LIGHT_PURPLE: return new float[]{1.0f, 0.3f, 1.0f};
            case YELLOW: return new float[]{1.0f, 1.0f, 0.3f};
            case WHITE: default: return new float[]{1.0f, 1.0f, 1.0f};
        }
    }
}

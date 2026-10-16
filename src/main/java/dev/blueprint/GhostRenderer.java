package dev.blueprint;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.blueprint.schematic.GhostModel.Ghost;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Zeichnet die Schematic als halbtransparente, farbige Bloecke (Oberflaeche, nahe der Kamera).
 * Aufbau nach dem Fabric-Doku-Beispiel "Rendering in the World" (26.2): Extraktion, dann Zeichnen
 * nach dem transparenten Terrain mit dem vanilla Debug-Pipeline-Typ DEBUG_FILLED_BOX.
 * Es sind einfarbige Wuerfel, keine echten Block-Texturen/-Modelle.
 */
public final class GhostRenderer {
    private GhostRenderer() {}

    private static final int MAX_BOXES = 1500;      // 1500 * 24 Vertices * 16 Byte ~ 576 KB
    private static final double RANGE = 48.0;
    private static final float ALPHA = 0.35f;

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static final StagedVertexBuffer BUFFER =
        new StagedVertexBuffer(() -> "Blueprint Ghost Buffer", RenderType.SMALL_BUFFER_SIZE);

    private static volatile List<Ghost> requested = List.of();
    private static volatile boolean visible = true;
    private static List<Ghost> frameGhosts = List.of(); // Zustand der Extraktionsphase

    public static void init() {
        LevelExtractionEvents.END_EXTRACTION.register(GhostRenderer::extract);
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(GhostRenderer::renderAndDraw);
    }

    public static void setGhosts(List<Ghost> ghosts) { requested = List.copyOf(ghosts); }
    public static boolean toggleVisible() { visible = !visible; return visible; }
    public static boolean isVisible() { return visible; }

    private static void extract(LevelExtractionContext context) {
        frameGhosts = visible ? requested : List.of();
    }

    private static void renderAndDraw(LevelRenderContext context) {
        List<Ghost> ghosts = frameGhosts;
        if (ghosts.isEmpty()) return;

        RenderPipeline pipeline = RenderPipelines.DEBUG_FILLED_BOX;
        VertexFormat format = pipeline.getVertexFormatBinding(0);
        assert format != null;
        PrimitiveTopology primitive = pipeline.getPrimitiveTopology();
        StagedVertexBuffer.Draw draw = BUFFER.appendDraw(format, primitive,
            primitive == PrimitiveTopology.QUADS ? RenderSystem.getProjectionType().vertexSorting() : null);

        PoseStack matrices = context.poseStack();
        Vec3 cam = context.levelState().cameraRenderState.pos;
        matrices.pushPose();
        matrices.translate(-cam.x, -cam.y, -cam.z);
        VertexConsumer builder = BUFFER.getVertexBuilder(draw);
        double r2 = RANGE * RANGE;
        int drawn = 0;
        for (Ghost g : ghosts) {
            if (drawn >= MAX_BOXES) break;
            double dx = g.x() + 0.5 - cam.x, dy = g.y() + 0.5 - cam.y, dz = g.z() + 0.5 - cam.z;
            if (dx * dx + dy * dy + dz * dz > r2) continue;
            box(matrices.last().pose(), builder, g.x(), g.y(), g.z(), g.x() + 1, g.y() + 1, g.z() + 1,
                g.r(), g.g(), g.b(), ALPHA);
            drawn++;
        }
        matrices.popPose();

        BUFFER.upload();
        StagedVertexBuffer.ExecuteInfo info = BUFFER.getExecuteInfo(draw);
        if (info != null && drawn > 0) submit(Minecraft.getInstance(), info, pipeline);
        BUFFER.endFrame();
    }

    private static void box(Matrix4fc m, VertexConsumer b, float x0, float y0, float z0,
                            float x1, float y1, float z1, float r, float g, float bl, float a) {
        // vorne (+z)
        b.addVertex(m, x0, y0, z1).setColor(r, g, bl, a); b.addVertex(m, x1, y0, z1).setColor(r, g, bl, a);
        b.addVertex(m, x1, y1, z1).setColor(r, g, bl, a); b.addVertex(m, x0, y1, z1).setColor(r, g, bl, a);
        // hinten (-z)
        b.addVertex(m, x1, y0, z0).setColor(r, g, bl, a); b.addVertex(m, x0, y0, z0).setColor(r, g, bl, a);
        b.addVertex(m, x0, y1, z0).setColor(r, g, bl, a); b.addVertex(m, x1, y1, z0).setColor(r, g, bl, a);
        // links (-x)
        b.addVertex(m, x0, y0, z0).setColor(r, g, bl, a); b.addVertex(m, x0, y0, z1).setColor(r, g, bl, a);
        b.addVertex(m, x0, y1, z1).setColor(r, g, bl, a); b.addVertex(m, x0, y1, z0).setColor(r, g, bl, a);
        // rechts (+x)
        b.addVertex(m, x1, y0, z1).setColor(r, g, bl, a); b.addVertex(m, x1, y0, z0).setColor(r, g, bl, a);
        b.addVertex(m, x1, y1, z0).setColor(r, g, bl, a); b.addVertex(m, x1, y1, z1).setColor(r, g, bl, a);
        // oben
        b.addVertex(m, x0, y1, z1).setColor(r, g, bl, a); b.addVertex(m, x1, y1, z1).setColor(r, g, bl, a);
        b.addVertex(m, x1, y1, z0).setColor(r, g, bl, a); b.addVertex(m, x0, y1, z0).setColor(r, g, bl, a);
        // unten
        b.addVertex(m, x0, y0, z0).setColor(r, g, bl, a); b.addVertex(m, x1, y0, z0).setColor(r, g, bl, a);
        b.addVertex(m, x1, y0, z1).setColor(r, g, bl, a); b.addVertex(m, x0, y0, z1).setColor(r, g, bl, a);
    }

    private static void submit(Minecraft client, StagedVertexBuffer.ExecuteInfo info, RenderPipeline pipeline) {
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
            .writeTransform(RenderSystem.getModelViewMatrixCopy(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
        RenderTarget target = client.gameRenderer.mainRenderTarget();
        GpuTextureView color = target.getColorTextureView();
        assert color != null;
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> BlueprintClient.MOD_ID + " ghost rendering", color, Optional.empty(),
                target.getDepthTextureView(), OptionalDouble.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            pass.setVertexBuffer(0, info.vertexBuffer().slice());
            pass.setIndexBuffer(info.indexBuffer(), info.indexType());
            pass.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0);
        }
    }
}

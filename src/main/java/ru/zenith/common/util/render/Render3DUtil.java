package ru.zenith.common.util.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4i;
import org.lwjgl.opengl.GL11;
import ru.zenith.common.QuickImports;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.common.util.math.ProjectionUtil;
import ru.zenith.implement.events.render.WorldRenderEvent;
import net.minecraft.entity.effect.StatusEffects;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public class Render3DUtil implements QuickImports {
    private final Map<VoxelShape, Pair<List<Box>, List<Line>>> SHAPE_OUTLINES = new HashMap<>();
    private final Map<VoxelShape, List<Box>> SHAPE_BOXES = new HashMap<>();
    public final List<Texture> TEXTURE_DEPTH = new ArrayList<>();
    public final List<Texture> TEXTURE = new ArrayList<>();
    public final List<Line> LINE_DEPTH = new ArrayList<>();
    public final List<Line> LINE = new ArrayList<>();
    public final List<Quad> QUAD_DEPTH = new ArrayList<>();
    public final List<Quad> QUAD = new ArrayList<>();
    @Setter
    public Matrix4f lastProjMat = new Matrix4f();
    @Setter
    public MatrixStack.Entry lastWorldSpaceMatrix = new MatrixStack().peek();

    private final Identifier captureId = Identifier.of("textures/capture.png"), bloom = Identifier.of("textures/bloom.png");

    public void onWorldRender(WorldRenderEvent e) {
        if (!TEXTURE.isEmpty()) {
            Set<Identifier> identifiers = TEXTURE.stream().map(texture -> texture.id).collect(Collectors.toCollection(LinkedHashSet::new));
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_CONSTANT_ALPHA);
            identifiers.forEach(id -> {
                RenderSystem.setShaderTexture(0, id);
                RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
                BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                TEXTURE.stream().filter(texture -> texture.id.equals(id)).forEach(tex -> quadTexture(tex.entry, buffer, tex.x, tex.y, tex.width, tex.height, tex.color));
                BufferRenderer.drawWithGlobalProgram(buffer.end());
            });
            RenderSystem.disableBlend();
            TEXTURE.clear();
        }
        if (!TEXTURE_DEPTH.isEmpty()) {
            Set<Identifier> identifiers = TEXTURE_DEPTH.stream().map(texture -> texture.id).collect(Collectors.toCollection(LinkedHashSet::new));
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_CONSTANT_ALPHA);
            identifiers.forEach(id -> {
                RenderSystem.setShaderTexture(0, id);
                RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
                BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                TEXTURE_DEPTH.stream().filter(texture -> texture.id.equals(id)).forEach(tex -> quadTexture(tex.entry, buffer, tex.x, tex.y, tex.width, tex.height, tex.color));
                BufferRenderer.drawWithGlobalProgram(buffer.end());
            });
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            TEXTURE_DEPTH.clear();
        }
        if (!LINE.isEmpty()) {
            GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
            Set<Float> widths = LINE.stream().map(line -> line.width).collect(Collectors.toCollection(LinkedHashSet::new));
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_CONSTANT_ALPHA);
            RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
            widths.forEach(width -> {
                RenderSystem.lineWidth(width);
                BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
                LINE.stream().filter(line -> line.width == width).forEach(line -> vertexLine(line.entry, buffer, line.start.toVector3f(), line.end.toVector3f(), line.colorStart, line.colorEnd));
                BufferRenderer.drawWithGlobalProgram(buffer.end());
            });
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            LINE.clear();
            GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
        }
        if (!QUAD.isEmpty()) {
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_CONSTANT_ALPHA);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            QUAD.forEach(quad -> vertexQuad(quad.entry, buffer, quad.x, quad.y, quad.w, quad.z, quad.color));
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            QUAD.clear();
        }
        if (!LINE_DEPTH.isEmpty()) {
            GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
            Set<Float> widths = LINE_DEPTH.stream().map(line -> line.width).collect(Collectors.toCollection(LinkedHashSet::new));
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_CONSTANT_ALPHA);
            RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
            widths.forEach(width -> {
                RenderSystem.lineWidth(width);
                BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
                LINE_DEPTH.stream().filter(line -> line.width == width).forEach(line -> vertexLine(line.entry, buffer, line.start.toVector3f(), line.end.toVector3f(), line.colorStart, line.colorEnd));
                BufferRenderer.drawWithGlobalProgram(buffer.end());
            });
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            LINE_DEPTH.clear();
            GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
        }
        if (!QUAD_DEPTH.isEmpty()) {
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_CONSTANT_ALPHA);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            QUAD_DEPTH.forEach(quad -> vertexQuad(quad.entry, buffer, quad.x, quad.y, quad.w, quad.z, quad.color));
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            QUAD_DEPTH.clear();
        }
    }

    public void drawShape(BlockPos blockPos, VoxelShape voxelShape, int color, float width) {
        drawShape(blockPos, voxelShape, color, width, true, false);
    }

    public void drawShape(BlockPos blockPos, VoxelShape voxelShape, int color, float width, boolean fill, boolean depth) {
        if (SHAPE_BOXES.containsKey(voxelShape)) {
            SHAPE_BOXES.get(voxelShape).forEach(box -> {
                box = box.offset(blockPos);
                if (ProjectionUtil.canSee(box)) drawBox(box, color, width, true, fill, depth);
            });
            return;
        }
        SHAPE_BOXES.put(voxelShape, voxelShape.getBoundingBoxes());
    }

    public void drawShapeAlternative(BlockPos blockPos, VoxelShape voxelShape, int color, float width, boolean fill, boolean depth) {
        Vec3d vec3d = Vec3d.of(blockPos);
        if (ProjectionUtil.canSee(new Box(blockPos))) {
            if (SHAPE_OUTLINES.containsKey(voxelShape)) {
                Pair<List<Box>, List<Line>> pair = SHAPE_OUTLINES.get(voxelShape);
                if (fill) pair.getLeft().forEach(box -> drawBox(box.offset(vec3d), color, width, false, true, depth));
                pair.getRight().forEach(line -> drawLine(line.start.add(vec3d), line.end.add(vec3d), color, width, depth));
                return;
            }
            List<Line> lines = new ArrayList<>();
            voxelShape.forEachEdge((minX, minY, minZ, maxX, maxY, maxZ) -> lines.add(new Line(null, new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ), 0, 0, 0)));
            SHAPE_OUTLINES.put(voxelShape, new Pair<>(voxelShape.getBoundingBoxes(), lines));
        }
    }

    public void drawBox(Box box, int color, float width) {
        drawBox(box, color, width, true, true, false);
    }

    public void drawBox(Box box, int color, float width, boolean line, boolean fill, boolean depth) {
        drawBox(null, box, color, width, line, fill, depth) ;
    }

    public void drawBox(MatrixStack.Entry entry, Box box, int color, float width, boolean line, boolean fill, boolean depth) {
        box = box.expand(1e-3);

        double x1 = box.minX;
        double y1 = box.minY;
        double z1 = box.minZ;
        double x2 = box.maxX;
        double y2 = box.maxY;
        double z2 = box.maxZ;

        if (fill) {
            int fillColor = ColorUtil.multAlpha(color, 0.1f);
            drawQuad(entry, new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z1), new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2), fillColor, depth);
            drawQuad(entry, new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z1), new Vec3d(x2, y1, z1), fillColor, depth);
            drawQuad(entry, new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z1), new Vec3d(x2, y2, z2), new Vec3d(x2, y1, z2), fillColor, depth);
            drawQuad(entry, new Vec3d(x1, y1, z2), new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z2), new Vec3d(x1, y2, z2), fillColor, depth);
            drawQuad(entry, new Vec3d(x1, y1, z1), new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z2), new Vec3d(x1, y2, z1), fillColor, depth);
            drawQuad(entry, new Vec3d(x1, y2, z1), new Vec3d(x1, y2, z2), new Vec3d(x2, y2, z2), new Vec3d(x2, y2, z1), fillColor, depth);
        }

        if (line) {
            drawLine(entry, x1, y1, z1, x2, y1, z1, color, width, depth);
            drawLine(entry, x2, y1, z1, x2, y1, z2, color, width, depth);
            drawLine(entry, x2, y1, z2, x1, y1, z2, color, width, depth);
            drawLine(entry, x1, y1, z2, x1, y1, z1, color, width, depth);
            drawLine(entry, x1, y1, z2, x1, y2, z2, color, width, depth);
            drawLine(entry, x1, y1, z1, x1, y2, z1, color, width, depth);
            drawLine(entry, x2, y1, z2, x2, y2, z2, color, width, depth);
            drawLine(entry, x2, y1, z1, x2, y2, z1, color, width, depth);
            drawLine(entry, x1, y2, z1, x2, y2, z1, color, width, depth);
            drawLine(entry, x2, y2, z1, x2, y2, z2, color, width, depth);
            drawLine(entry, x2, y2, z2, x1, y2, z2, color, width, depth);
            drawLine(entry, x1, y2, z2, x1, y2, z1, color, width, depth);
        }
    }

    public void vertexLine(MatrixStack matrices, VertexConsumer buffer, Vec3d start, Vec3d end, int startColor, int endColor) {
        vertexLine(matrices.peek(), buffer, start.toVector3f(), end.toVector3f(), startColor, endColor);
    }

    public void vertexLine(MatrixStack.Entry entry, VertexConsumer buffer, Vector3f start, Vector3f end, int startColor, int endColor) {
        if (entry == null) entry = lastWorldSpaceMatrix;
        Vector3f vec = getNormal(start, end);
        buffer.vertex(entry, start).color(startColor).normal(entry, vec);
        buffer.vertex(entry, end).color(endColor).normal(entry, vec);
    }

    public void vertexQuad(MatrixStack.Entry entry, VertexConsumer buffer, Vec3d vec1, Vec3d vec2, Vec3d vec3, Vec3d vec4, int color) {
        vertexQuad(entry, buffer, vec1.toVector3f(), vec2.toVector3f(), vec3.toVector3f(), vec4.toVector3f(), color);
    }

    public void vertexQuad(MatrixStack.Entry entry, VertexConsumer buffer, Vector3f vec1, Vector3f vec2, Vector3f vec3, Vector3f vec4, int color) {
        if (entry == null) entry = lastWorldSpaceMatrix;
        buffer.vertex(entry, vec1).color(color);
        buffer.vertex(entry, vec2).color(color);
        buffer.vertex(entry, vec3).color(color);
        buffer.vertex(entry, vec4).color(color);
    }

    public void quadTexture(MatrixStack.Entry entry, BufferBuilder buffer, float x, float y, float width, float height, Vector4i color) {
        buffer.vertex(entry, x, y + height, 0).texture(0, 0).color(color.x);
        buffer.vertex(entry, x + width, y + height, 0).texture(0, 1).color(color.y);
        buffer.vertex(entry, x + width, y, 0).texture(1, 1).color(color.w);
        buffer.vertex(entry, x, y, 0).texture(1, 0).color(color.z);
    }

    public @NotNull Vector3f getNormal(Vector3f start, Vector3f end) {
        Vector3f normal = new Vector3f(start).sub(end);
        float sqrt = MathHelper.sqrt(normal.lengthSquared());
        return normal.div(sqrt);
    }

    public void drawCube(LivingEntity lastTarget, float anim, float red) {
        float size = 2.2F - anim;

        Camera camera = mc.getEntityRenderDispatcher().camera;
        Vec3d vec = MathUtil.interpolate(lastTarget).subtract(camera.getPos());

        MatrixStack matrix = new MatrixStack();
        matrix.push();
        matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
        matrix.translate(vec.x, vec.y + lastTarget.getBoundingBox().getLengthY() / 2, vec.z);
        matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathUtil.interpolate(prevEspValue, espValue)));
        MatrixStack.Entry entry = matrix.peek().copy();
        Render3DUtil.drawTexture(entry, captureId, -size / 2, -size / 2, size, size, ColorUtil.multRedAndAlpha(new Vector4i(ColorUtil.fade(90), ColorUtil.fade(0), ColorUtil.fade(180), ColorUtil.fade(270)), 1 + red * 10, anim), false);
        matrix.pop();
    }

    public void drawCircle(MatrixStack matrix, LivingEntity lastTarget, float anim, float red) {
        double cs = MathUtil.interpolate(circleStep - 0.15F, circleStep);
        Vec3d target = MathUtil.interpolate(lastTarget);
        boolean canSee = Objects.requireNonNull(mc.player).canSee(lastTarget);

        GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
        if (canSee) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
        } else RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_CONSTANT_ALPHA);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0, size = 90; i <= size; i++) {
            float width = lastTarget.getWidth() * 0.9F;
            float height = lastTarget.getHeight();
            double yAnim = MathUtil.absSinAnimation(cs) * height;
            double yAnim2 = MathUtil.absSinAnimation(cs - 0.45) * height;
            Vec3d cosSin = MathUtil.cosSin(i, size, width);
            Vec3d nextCosSin = MathUtil.cosSin(i + 1, size, width);
            int color = ColorUtil.multRed(ColorUtil.fade(i * 4), 1 + red * 10);

            Render3DUtil.vertexLine(matrix, buffer, target.add(cosSin.x, cosSin.y + yAnim, cosSin.z), target.add(cosSin.x, cosSin.y + yAnim2, cosSin.z),
                    ColorUtil.multAlpha(color, 0.6F * anim), ColorUtil.multAlpha(color, 0));
            Render3DUtil.drawLine(target.add(cosSin.x, cosSin.y + yAnim, cosSin.z), target.add(nextCosSin.x, nextCosSin.y + yAnim, nextCosSin.z), ColorUtil.multAlpha(color, anim), 3, canSee);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        if (canSee) {
            RenderSystem.depthMask(true);
            RenderSystem.disableDepthTest();
        } else RenderSystem.enableDepthTest();
        GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
    }

    public void drawGhosts(LivingEntity lastTarget, float anim, float red, float speed) {
        Camera camera = mc.getEntityRenderDispatcher().camera;
        Vec3d vec = MathUtil.interpolate(lastTarget).subtract(camera.getPos());
        boolean canSee = mc.player.canSee(lastTarget);
        double iAge = MathUtil.interpolate(mc.player.age - 1, mc.player.age);
        float halfHeight = lastTarget.getHeight() / 2 + 0.1F;
        float width = lastTarget.getWidth();

        for (int j = 0; j < 3; j++) {
            for (int i = 0, length = 10; i <= length; i++) {
                double radians = Math.toRadians(((i / 2F + iAge * speed) * length + (j * 120)) % (length * 360));
                double sinQuad = Math.sin(Math.toRadians(iAge * 2.5f * speed + i * (j + halfHeight)) * 2) / 2;

                float offset = ((float) (i + length) / (length + length));
                MatrixStack matrices = new MatrixStack();
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
                matrices.translate(vec.x + Math.cos(radians) * width, (vec.y + halfHeight + sinQuad), vec.z + Math.sin(radians) * width);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                MatrixStack.Entry entry = matrices.peek().copy();
                int color = ColorUtil.multRedAndAlpha(ColorUtil.fade((int) offset * 180), 1 + red * 10, offset * anim);
                float scale = 0.5f * offset;
                Render3DUtil.drawTexture(entry, bloom, -scale / 2, -scale / 2, scale, scale, new Vector4i(color), canSee);
            }
        }
    }

    // НОВЫЙ МЕТОД ДЛЯ КРИСТАЛЛОВ
    public void drawCrystals(MatrixStack matrixStack, LivingEntity entity, float animProgress, float hurtProgress,
                             float crystalSpeed, float crystalSize, float crystalOrbit, int crystalCount) {
        if (entity == null || animProgress <= 0) return;

        Camera camera = mc.getEntityRenderDispatcher().camera;
        Vec3d targetPos = MathUtil.interpolate(entity).subtract(camera.getPos());
        boolean canSee = mc.player.canSee(entity);
        double time = System.currentTimeMillis() / 1000.0;
        float entityHeight = entity.getHeight();

        // Основные кристаллы на внешней орбите
        for (int i = 0; i < crystalCount; i++) {
            double angle = time * crystalSpeed + i * (2 * Math.PI / crystalCount);
            double radius = crystalOrbit * (1.0 + 0.15 * Math.sin(time * 2.5 + i * 0.7));

            // Плавное движение по вертикали
            double verticalOffset = 0.4 * Math.sin(time * 2.0 + i * 1.3);
            float pulse = (float)(0.8 + 0.2 * Math.sin(time * 3.0 + i));

            // Позиция кристалла
            double crystalX = Math.cos(angle) * radius;
            double crystalZ = Math.sin(angle) * radius;
            double crystalY = verticalOffset;

            renderSingleCrystal(
                    matrixStack, camera, targetPos,
                    crystalX, crystalY + entityHeight * 0.6, crystalZ,
                    time, i, animProgress, hurtProgress, entity, canSee, pulse, 1.0f, crystalSize
            );
        }

        // Внутренние кристаллы (меньшие, с другой анимацией)
        int innerCrystals = crystalCount / 2;
        for (int i = 0; i < innerCrystals; i++) {
            double angle = time * (crystalSpeed * 1.8) + i * (2 * Math.PI / innerCrystals) + 0.3;
            double radius = crystalOrbit * 0.6 * (1.0 + 0.1 * Math.sin(time * 3.5 + i));

            double crystalX = Math.cos(angle) * radius;
            double crystalZ = Math.sin(angle) * radius;
            double crystalY = 0.3 * Math.cos(time * 2.8 + i * 2);

            renderSingleCrystal(
                    matrixStack, camera, targetPos,
                    crystalX, crystalY + entityHeight * 0.7, crystalZ,
                    time, i + crystalCount, animProgress, hurtProgress, entity, canSee, 0.6f, 0.7f, crystalSize
            );
        }

        // Центральные кристаллы вокруг головы
        int centerCrystals = 4;
        for (int i = 0; i < centerCrystals; i++) {
            double angle = time * (crystalSpeed * 2.2) + i * (2 * Math.PI / centerCrystals);
            double radius = 0.3 * (1.0 + 0.05 * Math.sin(time * 4.0 + i));

            double crystalX = Math.cos(angle) * radius;
            double crystalZ = Math.sin(angle) * radius;
            double crystalY = 0.1 * Math.sin(time * 5.0 + i);

            renderSingleCrystal(
                    matrixStack, camera, targetPos,
                    crystalX, crystalY + entityHeight * 0.9, crystalZ,
                    time, i + crystalCount + innerCrystals, animProgress, hurtProgress, entity, canSee, 0.4f, 0.5f, crystalSize
            );
        }
    }

    private void renderSingleCrystal(MatrixStack matrixStack, Camera camera, Vec3d targetPos,
                                     double crystalX, double crystalY, double crystalZ,
                                     double time, int index, float animProgress,
                                     float hurtProgress, LivingEntity entity, boolean canSee,
                                     float scaleMultiplier, float alphaMultiplier, float baseCrystalSize) {

        // Создаем матрицу преобразования
        MatrixStack matrices = new MatrixStack();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
        matrices.translate(
                targetPos.x + crystalX,
                targetPos.y + crystalY,
                targetPos.z + crystalZ
        );

        // Вращение кристаллов вокруг своей оси
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(time * 50 + index * 30)));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float)(time * 40 + index * 25)));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(time * 30 + index * 20)));

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

        MatrixStack.Entry entry = matrices.peek().copy();

        // Динамический цвет с пульсацией
        int baseColor = getCrystalColor(entity, hurtProgress, index, time);

        // Эффект пульсации
        float pulse = (float)(0.6 + 0.4 * Math.sin(time * 5.0 + index * 0.5));
        int pulseColor = multiplyColorBrightness(baseColor, pulse);

        // Альфа-канал с анимацией
        float alpha = animProgress * alphaMultiplier * (0.4f + 0.6f * (float)Math.sin(time * 6.0 + index * 0.3));
        int finalColor = multiplyColorAlpha(pulseColor, alpha);

        // Размер кристалла с пульсацией
        float baseSize = baseCrystalSize * 2.5f;
        float animatedSize = baseSize * scaleMultiplier * (0.7f + 0.3f * (float)Math.sin(time * 4.0 + index));

        // Рисуем основной кристалл
        Render3DUtil.drawTexture(
                entry,
                bloom,
                -animatedSize / 2,
                -animatedSize / 2,
                animatedSize,
                animatedSize,
                new Vector4i(finalColor, finalColor, finalColor, finalColor),
                canSee
        );

        // Добавляем свечение (больший полупрозрачный слой)
        float glowSize = animatedSize * 2.2f;
        int glowColor = multiplyColorAlpha(finalColor, 0.15f);

        Render3DUtil.drawTexture(
                entry,
                bloom,
                -glowSize / 2,
                -glowSize / 2,
                glowSize,
                glowSize,
                new Vector4i(glowColor, glowColor, glowColor, glowColor),
                canSee
        );

        // Энергетическое ядро (маленький яркий центр)
        float coreSize = animatedSize * 0.3f;
        int coreColor = multiplyColorBrightness(finalColor, 1.8f);
        coreColor = multiplyColorAlpha(coreColor, 0.9f);

        Render3DUtil.drawTexture(
                entry,
                bloom,
                -coreSize / 2,
                -coreSize / 2,
                coreSize,
                coreSize,
                new Vector4i(coreColor, coreColor, coreColor, coreColor),
                canSee
        );
    }

    private int getCrystalColor(LivingEntity entity, float hurtProgress, int index, double time) {
        // Базовый цвет - радужный градиент
        int hue = (int)((System.currentTimeMillis() / 50 + index * 30) % 360);
        int baseColor = fadeColor(hue);

        // Эффект получения урона - красное свечение
        if (hurtProgress > 0) {
            int hurtColor = 0xFFFF5555; // Ярко-красный
            return overlayColor(baseColor, hurtColor, hurtProgress * 0.8f);
        }

        // Эффект отравления - кислотно-зеленый
        if (entity.hasStatusEffect(StatusEffects.POISON)) {
            int poisonColor = 0xFF55FF55;
            float pulse = (float)(0.5 + 0.5 * Math.sin(time * 8.0));
            return overlayColor(baseColor, poisonColor, 0.6f * pulse);
        }

        // Эффект слабости - тускло-фиолетовый
        if (entity.hasStatusEffect(StatusEffects.WEAKNESS)) {
            int weaknessColor = 0xFFAA55AA;
            return overlayColor(baseColor, weaknessColor, 0.4f);
        }

        // Эффект горения - оранжевый
        if (entity.isOnFire()) {
            int fireColor = 0xFFFFAA00;
            float firePulse = (float)(0.3 + 0.7 * Math.sin(time * 10.0));
            return overlayColor(baseColor, fireColor, 0.5f * firePulse);
        }

        return baseColor;
    }

    // Вспомогательные методы для работы с цветами
    private int fadeColor(int hue) {
        // Простая реализация fade эффекта
        float normalizedHue = (hue % 360) / 360.0f;
        int r = (int)(Math.sin(normalizedHue * Math.PI * 2 + 0) * 127 + 128);
        int g = (int)(Math.sin(normalizedHue * Math.PI * 2 + 2) * 127 + 128);
        int b = (int)(Math.sin(normalizedHue * Math.PI * 2 + 4) * 127 + 128);
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    private int multiplyColorBrightness(int color, float multiplier) {
        int a = (color >> 24) & 0xFF;
        int r = (int)(((color >> 16) & 0xFF) * multiplier);
        int g = (int)(((color >> 8) & 0xFF) * multiplier);
        int b = (int)((color & 0xFF) * multiplier);

        r = MathHelper.clamp(r, 0, 255);
        g = MathHelper.clamp(g, 0, 255);
        b = MathHelper.clamp(b, 0, 255);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int multiplyColorAlpha(int color, float alpha) {
        int a = (int)(((color >> 24) & 0xFF) * alpha);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        a = MathHelper.clamp(a, 0, 255);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int overlayColor(int baseColor, int overlayColor, float strength) {
        int r1 = (baseColor >> 16) & 0xFF;
        int g1 = (baseColor >> 8) & 0xFF;
        int b1 = baseColor & 0xFF;
        int a1 = (baseColor >> 24) & 0xFF;

        int r2 = (overlayColor >> 16) & 0xFF;
        int g2 = (overlayColor >> 8) & 0xFF;
        int b2 = overlayColor & 0xFF;

        int r = (int)(r1 + (r2 - r1) * strength);
        int g = (int)(g1 + (g2 - g1) * strength);
        int b = (int)(b1 + (b2 - b1) * strength);

        r = MathHelper.clamp(r, 0, 255);
        g = MathHelper.clamp(g, 0, 255);
        b = MathHelper.clamp(b, 0, 255);

        return (a1 << 24) | (r << 16) | (g << 8) | b;
    }

    private float espValue = 1f,espSpeed = 1f, prevEspValue, circleStep;
    private boolean flipSpeed;

    public void updateTargetEsp() {
        prevEspValue = espValue;
        espValue += espSpeed;
        if (espSpeed > 25) flipSpeed = true;
        if (espSpeed < -25) flipSpeed = false;
        espSpeed = flipSpeed ? espSpeed - 0.5f : espSpeed + 0.5f;
        circleStep += 0.15f;
    }

    public void drawLine(MatrixStack.Entry entry, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int color, float width, boolean depth) {
        drawLine(entry, new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ), color, color, width, depth);
    }

    public void drawLine(Vec3d start, Vec3d end, int color, float width, boolean depth) {
        drawLine(null, start, end, color, color, width, depth);
    }

    public void drawLine(MatrixStack.Entry entry, Vec3d start, Vec3d end, int colorStart, int colorEnd, float width, boolean depth) {
        Line line = new Line(entry, start, end, colorStart, colorEnd, width);
        if (depth) LINE_DEPTH.add(line); else LINE.add(line);
    }

    public void drawQuad(Vec3d x, Vec3d y, Vec3d w, Vec3d z, int color, boolean depth) {
        drawQuad(null,x,y,w,z,color,depth);
    }

    public void drawQuad(MatrixStack.Entry entry, Vec3d x, Vec3d y, Vec3d w, Vec3d z, int color, boolean depth) {
        Quad quad = new Quad(entry, x, y, w, z, color);
        if (depth) QUAD_DEPTH.add(quad); else QUAD.add(quad);
    }

    public void drawTexture(MatrixStack.Entry entry, Identifier id, float x, float y, float width, float height, Vector4i color, boolean depth) {
        Texture texture = new Texture(entry, id, x, y, width, height, color);
        if (depth) TEXTURE_DEPTH.add(texture); else TEXTURE.add(texture);
    }

    public record Texture(MatrixStack.Entry entry, Identifier id, float x, float y, float width, float height, Vector4i color) {}
    public record Line(MatrixStack.Entry entry, Vec3d start, Vec3d end, int colorStart, int colorEnd, float width) {}
    public record Quad(MatrixStack.Entry entry, Vec3d x, Vec3d y, Vec3d w, Vec3d z, int color) {}
}
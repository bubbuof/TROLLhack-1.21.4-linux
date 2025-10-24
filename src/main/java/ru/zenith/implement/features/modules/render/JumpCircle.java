package ru.zenith.implement.features.modules.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4i;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.math.Counter;
import ru.zenith.common.util.render.Render3DUtil;
import ru.zenith.implement.events.player.TickEvent;
import ru.zenith.implement.events.render.WorldRenderEvent;

import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class JumpCircle extends Module {

    private final List<Circle> circles = new ArrayList<>();
    final Identifier circleTexture = Identifier.of("textures/circle.png");
    boolean wasOnGround = true;

    public JumpCircle() {
        super("JumpCircle", ModuleCategory.RENDER);
    }

    @EventHandler
    public void onUpdate(TickEvent event) {
        if (mc.player == null) return;

        boolean isOnGround = mc.player.isOnGround();

        if (wasOnGround && !isOnGround) {
            Vec3d pos = new Vec3d(
                    mc.player.getX(),
                    Math.floor(mc.player.getY()) + 0.001,
                    mc.player.getZ()
            );
            circles.add(new Circle(pos, new Counter()));
        }

        wasOnGround = isOnGround;

        circles.removeIf(c -> c.timer.passedMs(3000));
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        renderCircles();
    }

    private void renderCircles() {
        if (circles.isEmpty()) return;

        for (Circle circle : circles) {
            renderSingleCircle(circle);
        }
    }

    private void renderSingleCircle(Circle circle) {
        float lifeTime = (float) circle.timer.getPassedTimeMs();
        float maxTime = 3000f;
        float progress = lifeTime / maxTime;

        if (progress >= 1f) return;

        float scale = progress * 2f;
        float alpha = 1f - (progress * progress);

        int baseColor = ColorUtil.fade((int)(progress * 360f));
        int color = ColorUtil.multAlpha(baseColor, alpha);

        Camera camera = mc.getEntityRenderDispatcher().camera;
        Vec3d cameraPos = camera.getPos();
        Vec3d circlePos = circle.pos();

        MatrixStack matrixStack = new MatrixStack();

        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));

        matrixStack.translate(circlePos.x - cameraPos.x, circlePos.y - cameraPos.y, circlePos.z - cameraPos.z);

        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));

        MatrixStack.Entry entry = matrixStack.peek();
        Vector4i colors = new Vector4i(color, color, color, color);

        Render3DUtil.drawTexture(entry, circleTexture, -scale/2, -scale/2, scale, scale, colors, true);
    }

    public record Circle(Vec3d pos, Counter timer) {}
}

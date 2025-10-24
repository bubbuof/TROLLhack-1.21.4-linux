package ru.zenith.implement.features.modules.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.ValueSetting;
import ru.zenith.api.repository.friend.FriendUtils;
import ru.zenith.api.system.animation.Animation;
import ru.zenith.api.system.animation.Direction;
import ru.zenith.api.system.animation.implement.DecelerateAnimation;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.implement.events.player.TickEvent;
import ru.zenith.implement.events.render.DrawEvent;

import java.util.List;

import static net.minecraft.client.render.VertexFormat.DrawMode.QUADS;
import static net.minecraft.client.render.VertexFormats.POSITION_TEXTURE_COLOR;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Arrows extends Module {
    Identifier iconId = Identifier.of("textures/arrow.png");
    Animation radiusAnim = new DecelerateAnimation().setMs(150).setValue(12);

    ValueSetting radiusSetting = new ValueSetting("Radius", "Radius of arrows")
            .setValue(50).range(30, 100);

    ValueSetting sizeSetting = new ValueSetting("Size", "Size of arrows")
            .setValue(16).range(8, 20);

    public Arrows() {
        super("Arrows", "Arrows", ModuleCategory.RENDER);
        setup(radiusSetting,sizeSetting);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        radiusAnim.setDirection(mc.player.isSprinting() ? Direction.FORWARDS : Direction.BACKWARDS);
    }

    @EventHandler
    public void onDraw(DrawEvent e) {
        MatrixStack matrix = e.getDrawContext().getMatrices();
        List<AbstractClientPlayerEntity> players = mc.world.getPlayers().stream().filter(p -> p != mc.player).toList();

        float middleW = mc.getWindow().getScaledWidth() / 2f;
        float middleH = mc.getWindow().getScaledHeight() / 2f;
        float posY = middleH - radiusSetting.getValue() - radiusAnim.getOutput().floatValue();
        float size = sizeSetting.getValue();

        if (!mc.options.hudHidden && mc.options.getPerspective().equals(Perspective.FIRST_PERSON) && !players.isEmpty()) {
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_CONSTANT_ALPHA);
            RenderSystem.setShaderTexture(0, iconId);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            BufferBuilder buffer = tessellator.begin(QUADS, POSITION_TEXTURE_COLOR);

            players.forEach(player -> {
                int color = FriendUtils.isFriend(player) ? ColorUtil.getFriendColor() : ColorUtil.getClientColor();
                float yaw = getRotations(player) - mc.player.getYaw();
                matrix.push();
                matrix.translate(middleW, middleH, 0.0F);
                matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(yaw));
                matrix.translate(-middleW, -middleH, 0.0F);
                Matrix4f matrix4f = matrix.peek().getPositionMatrix();
                buffer.vertex(matrix4f, middleW - (size / 2f), posY + size, 0).texture(0f, 1f).color(ColorUtil.multAlpha(ColorUtil.multDark(color,0.4F), 0.5F));
                buffer.vertex(matrix4f, middleW + size / 2f, posY + size, 0).texture(1f, 1f).color(ColorUtil.multAlpha(ColorUtil.multDark(color,0.4F), 0.5F));
                buffer.vertex(matrix4f, middleW + size / 2f, posY, 0).texture(1f, 0).color(color);
                buffer.vertex(matrix4f, middleW - (size / 2f), posY, 0).texture(0, 0).color(color);
                matrix.translate(middleW, middleH, 0.0F);
                matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-yaw));
                matrix.translate(-middleW, -middleH, 0.0F);
                matrix.pop();
            });

            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
    }

    public static float getRotations(Entity entity) {
        double x = MathUtil.interpolate(entity.prevX, entity.getX()) - MathUtil.interpolate(mc.player.prevX, mc.player.getX());
        double z = MathUtil.interpolate(entity.prevZ, entity.getZ()) - MathUtil.interpolate(mc.player.prevZ, mc.player.getZ());
        return (float) -(Math.atan2(x, z) * (180 / Math.PI));
    }
}

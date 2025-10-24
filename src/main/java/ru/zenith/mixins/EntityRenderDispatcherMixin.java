package ru.zenith.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.zenith.api.repository.friend.FriendUtils;
import ru.zenith.common.QuickImports;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.common.util.math.ProjectionUtil;
import ru.zenith.common.util.render.Render3DUtil;
import ru.zenith.core.Main;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin implements QuickImports {

    @ModifyExpressionValue(method = "render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/EntityRenderer;)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/entity/state/EntityRenderState;invisible:Z"))
    private boolean renderHitboxHook(boolean original, @Local(ordinal = 0, argsOnly = true) Entity entity) {
        return entity instanceof ArmorStandEntity;
    }

    @Inject(method = "renderHitbox", at = @At("HEAD"), cancellable = true)
    private static void renderHitboxHook(MatrixStack matrices, VertexConsumer vertices, Entity entity, float tickDelta, float red, float green, float blue, CallbackInfo ci) {
        if (!Main.getInstance().getBoxESPRepository().entities.containsKey(entity.getType())) renderBox(entity);
        ci.cancel();
    }

    @Unique
    private static void renderBox(Entity entity) {
        if (entity != mc.player || !mc.options.getPerspective().equals(Perspective.FIRST_PERSON)) {
            int color = FriendUtils.isFriend(entity) ? ColorUtil.getFriendColor() : ColorUtil.getClientColor();
            Vec3d offset = MathUtil.interpolate(entity).subtract(entity.getPos());
            Box box = entity.getBoundingBox().offset(offset);
            if (ProjectionUtil.canSee(box)) {
                if (entity instanceof LivingEntity living) {
                    float width = entity.getWidth();
                    Vec3d eyeMin = entity.getEyePos().add(offset).add(-width / 2, 0, -width / 2);
                    Render3DUtil.drawBox(box, ColorUtil.multRed(color, 1 + living.hurtTime), 2, true, true, true);
                    Render3DUtil.drawLine(eyeMin, eyeMin.add(width, 0, 0), ColorUtil.RED, 2, true);
                    Render3DUtil.drawLine(eyeMin.add(width, 0, 0), eyeMin.add(width, 0, width), ColorUtil.RED, 2, true);
                    Render3DUtil.drawLine(eyeMin, eyeMin.add(0, 0, width), ColorUtil.RED, 2, true);
                    Render3DUtil.drawLine(eyeMin.add(0, 0, width), eyeMin.add(width, 0, width), ColorUtil.RED, 2, true);
                } else {
                    Render3DUtil.drawBox(box, color, 2, true, true, true);
                }
            }
        }
    }
}

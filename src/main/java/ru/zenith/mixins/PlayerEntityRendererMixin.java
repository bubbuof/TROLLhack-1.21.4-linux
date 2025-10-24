package ru.zenith.mixins;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.zenith.common.QuickImports;
import ru.zenith.implement.features.modules.render.RampageDance;

@Mixin(LivingEntityRenderer.class)
public class PlayerEntityRendererMixin implements QuickImports {

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"))
    private void onRenderEntity(LivingEntityRenderState renderState, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, CallbackInfo ci) {
        RampageDance rampageDance = RampageDance.getInstance();
        
        // Apply dance animation if active and this is a player model (width check of 0.6F for players)
        // This will apply to ALL players, including our own player when viewed from third person
        if (rampageDance != null && rampageDance.isDancing() && renderState.width == 0.6F) {
            // Cast this to LivingEntityRenderer to access getModel()
            LivingEntityRenderer<?, ?, ?> renderer = (LivingEntityRenderer<?, ?, ?>) (Object) this;
            EntityModel<?> model = renderer.getModel();
            if (model instanceof PlayerEntityModel playerModel) {
                applyDanceAnimation(playerModel, rampageDance);
            }
        }
    }

    private void applyDanceAnimation(PlayerEntityModel model, RampageDance dance) {
        // Apply arm movements
        model.rightArm.pitch = dance.getRightArmPitch();
        model.leftArm.pitch = dance.getLeftArmPitch();
        
        // Apply leg movements
        model.rightLeg.pitch = dance.getRightLegPitch();
        model.leftLeg.pitch = dance.getLeftLegPitch();
        
        // Apply body rotation
        model.body.yaw = dance.getBodyYaw() * 0.017453292F; // Convert degrees to radians
        
        // Apply head movement
        model.head.pitch = dance.getHeadPitch();
        
        // Add some extra flair for specific dance styles
        String style = dance.danceStyle.getSelected();
        switch (style) {
            case "Aggressive" -> {
                // More dramatic arm positions
                model.rightArm.roll = dance.getRightArmPitch() * 0.5f;
                model.leftArm.roll = -dance.getLeftArmPitch() * 0.5f;
            }
            case "Twerk" -> {
                // Emphasize hip movement
                model.body.pitch = Math.abs(dance.getBodyYaw()) * 0.01f;
            }
            case "Crazy" -> {
                // Random additional movements
                model.rightArm.yaw = dance.getRightArmPitch() * 0.3f;
                model.leftArm.yaw = -dance.getLeftArmPitch() * 0.3f;
            }
        }
    }
}

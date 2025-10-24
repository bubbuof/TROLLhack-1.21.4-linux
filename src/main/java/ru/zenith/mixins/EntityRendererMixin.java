package ru.zenith.mixins;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.zenith.implement.features.modules.render.EntityESP;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<S extends EntityRenderState> {
    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void renderLabelIfPresent(S state, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (EntityESP.getInstance().isState() && canRemove((int) (state.width * 100), EntityESP.getInstance())) {
            ci.cancel();
        }
    }

    @Unique
    private boolean canRemove(int width, EntityESP esp) {
       return switch (width) {
           case 60 -> esp.entityType.isSelected("Player");
           case 98 -> esp.entityType.isSelected("TNT");
           default -> false;
       };
    }
}
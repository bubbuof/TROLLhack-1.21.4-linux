package ru.zenith.mixins;

import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FogShape;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.zenith.api.event.EventManager;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.implement.events.render.FogEvent;
import ru.zenith.implement.features.modules.render.NoRender;

@Mixin(BackgroundRenderer.class)
public class BackGroundRendererMixin {

    @Inject(method = "getFogModifier(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/BackgroundRenderer$StatusEffectFogModifier;", at = @At("HEAD"), cancellable = true)
    private static void onGetFogModifier(Entity entity, float tickDelta, CallbackInfoReturnable<Object> info) {
        NoRender noRender = NoRender.getInstance();
        if (noRender.isState() && noRender.modeSetting.isSelected("Bad Effects")) info.setReturnValue(null);
    }

    @Inject(method = "getFogColor", at = @At(value = "HEAD"), cancellable = true)
    private static void getFogColorHook(Camera camera, float tickDelta, ClientWorld world, int clampedViewDistance, float skyDarkness, CallbackInfoReturnable<Vector4f> cir) {
        FogEvent event = new FogEvent();
        EventManager.callEvent(event);
        if (event.isCancelled()) {
            int color = event.getColor();
            cir.setReturnValue(new Vector4f(ColorUtil.redf(color), ColorUtil.greenf(color), ColorUtil.bluef(color), ColorUtil.alphaf(color)));
        }
    }

    @Inject(method = "applyFog", at = @At(value = "HEAD"), cancellable = true)
    private static void modifyFog(Camera camera, BackgroundRenderer.FogType fogType, Vector4f vector4f, float viewDistance, boolean thickenFog, float tickDelta, CallbackInfoReturnable<Fog> cir) {
        FogEvent event = new FogEvent();
        EventManager.callEvent(event);
        if (event.isCancelled()) {
            int color = event.getColor();
            cir.setReturnValue(new Fog(2.0F, event.getDistance(),  FogShape.CYLINDER, ColorUtil.redf(color), ColorUtil.greenf(color), ColorUtil.bluef(color), ColorUtil.alphaf(color)));
        }
    }
}

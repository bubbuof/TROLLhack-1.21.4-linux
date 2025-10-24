package ru.zenith.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import ru.zenith.implement.features.modules.render.WorldTweaks;

@Mixin(LightmapTextureManager.class)
public class LightmapTextureManagerMixin {

    @ModifyExpressionValue(method = "update(F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;"))
    private Object injectXRayFullBright(Object original) {
        WorldTweaks tweaks = WorldTweaks.getInstance();
        if (tweaks.isState() && tweaks.modeSetting.isSelected("Bright")) {
            return Math.max((double) original, tweaks.brightSetting.getValue() * 10);
        }
        return original;
    }
}
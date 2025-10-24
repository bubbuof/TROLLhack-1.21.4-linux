package ru.zenith.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.zenith.api.event.EventManager;
import ru.zenith.common.QuickImports;
import ru.zenith.implement.events.player.BoundingBoxControlEvent;
import ru.zenith.implement.events.player.PlayerVelocityStrafeEvent;

@Mixin(Entity.class)
public abstract class EntityMixin implements QuickImports {

    @Shadow private Box boundingBox;

    @Redirect(method = "updateVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;movementInputToVelocity(Lnet/minecraft/util/math/Vec3d;FF)Lnet/minecraft/util/math/Vec3d;"))
    public Vec3d hookVelocity(Vec3d movementInput, float speed, float yaw) {
        if ((Object) this == mc.player) {
            PlayerVelocityStrafeEvent event = new PlayerVelocityStrafeEvent(movementInput, speed, yaw, Entity.movementInputToVelocity(movementInput, speed, yaw));
            EventManager.callEvent(event);
            return event.getVelocity();
        }
        return Entity.movementInputToVelocity(movementInput, speed, yaw);
    }

    @Inject(method = "getBoundingBox", at = @At("HEAD"), cancellable = true)
    public final void getBoundingBox(CallbackInfoReturnable<Box> cir) {
        BoundingBoxControlEvent event = new BoundingBoxControlEvent(boundingBox, (Entity) (Object) this);
        EventManager.callEvent(event);
        cir.setReturnValue(event.getBox());
    }

    @ModifyExpressionValue(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isControlledByPlayer()Z"))
    public boolean isControlledByPlayerHook(boolean original) {
        if ((Object) this == mc.player) return false;
        return original;
    }
}

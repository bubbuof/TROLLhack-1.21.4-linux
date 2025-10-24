package ru.zenith.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import ru.zenith.api.event.EventManager;
import ru.zenith.common.util.entity.PlayerInventoryComponent;
import ru.zenith.implement.events.player.InputEvent;
import ru.zenith.implement.features.modules.combat.killaura.rotation.RotationPlan;
import ru.zenith.implement.features.modules.combat.killaura.rotation.Angle;
import ru.zenith.implement.features.modules.combat.killaura.rotation.RotationController;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin extends InputMixin {

    @ModifyExpressionValue(method = "tick", at = @At(value = "NEW", target = "(ZZZZZZZ)Lnet/minecraft/util/PlayerInput;"))
    private PlayerInput tickHook(PlayerInput original) {
        InputEvent event = new InputEvent(original);
        EventManager.callEvent(event);
        PlayerInventoryComponent.input(event);
        return transformInput(event.getInput());
    }

    @Unique
    private PlayerInput transformInput(PlayerInput input) {
        RotationController rotationController = RotationController.INSTANCE;
        Angle angle = rotationController.getCurrentAngle();
        RotationPlan configurable = rotationController.getCurrentRotationPlan();

        if (mc.player == null || angle == null || configurable == null || !(configurable.isMoveCorrection() && configurable.isFreeCorrection())) {
            return input;
        }

        float deltaYaw = mc.player.getYaw() - angle.getYaw();
        float z = KeyboardInput.getMovementMultiplier(input.forward(), input.backward());
        float x = KeyboardInput.getMovementMultiplier(input.left(), input.right());
        float newX = x * MathHelper.cos(deltaYaw * 0.017453292f) - z * MathHelper.sin(deltaYaw * 0.017453292f);
        float newZ = z * MathHelper.cos(deltaYaw * 0.017453292f) + x * MathHelper.sin(deltaYaw * 0.017453292f);
        int movementSideways = Math.round(newX), movementForward = Math.round(newZ);

        return new PlayerInput(movementForward > 0F, movementForward < 0F, movementSideways > 0F, movementSideways < 0F, input.jump(), input.sneak(), input.sprint());
    }
}

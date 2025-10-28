package ru.zenith.implement.features.modules.movement;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.ValueSetting;
import ru.zenith.implement.events.player.TickEvent;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.math.Vec3d;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Jesus extends Module {

    final ValueSetting melonBallSpeed = new ValueSetting("Melon Ball Speed", "Base speed on water/lava")
            .range(0.1f, 1.0f)
            .setValue(0.44f);

    public Jesus() {
        super("Jesus", "Jesus", ModuleCategory.MOVEMENT);
        setup(melonBallSpeed);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (mc.player.isTouchingWater() || mc.player.isInLava()) {
            StatusEffectInstance speedEffect = mc.player.getStatusEffect(StatusEffects.SPEED);
            StatusEffectInstance slownessEffect = mc.player.getStatusEffect(StatusEffects.SLOWNESS);
            ItemStack offHandItem = mc.player.getOffHandStack();
            String itemName = offHandItem.getName().getString();
            float appliedSpeed = 0f;
            float baseSpeed = melonBallSpeed.getValue();

            if (itemName.contains("Ломтик Дыни") || itemName.contains("Melon Slice") &&
                    speedEffect != null && speedEffect.getAmplifier() == 2) {
                appliedSpeed = 0.4283f * 1.15f;
            } else {
                if (speedEffect != null) {
                    if (speedEffect.getAmplifier() == 2) {
                        appliedSpeed = baseSpeed * 1.15f;
                    } else if (speedEffect.getAmplifier() == 1) {
                        appliedSpeed = baseSpeed;
                    }
                } else {
                    appliedSpeed = baseSpeed * 0.68f;
                }
            }

            if (slownessEffect != null) {
                appliedSpeed *= 0.85f;
            }

            setSpeed(appliedSpeed);

            // Check if player is moving
            boolean isMoving = mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed() ||
                    mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed();

            if (!isMoving) {
                // Stop horizontal movement
                Vec3d motion = mc.player.getVelocity();
                mc.player.setVelocity(0, motion.y, 0);
            }

            // Vertical movement
            double verticalMotion = mc.options.jumpKey.isPressed() ? 0.019 : 0.003;
            Vec3d currentMotion = mc.player.getVelocity();
            mc.player.setVelocity(currentMotion.x, verticalMotion, currentMotion.z);
        }
    }

    private void setSpeed(float speed) {
        if (mc.player == null) return;

        double yaw = Math.toRadians(mc.player.getYaw());
        double x = -Math.sin(yaw) * speed;
        double z = Math.cos(yaw) * speed;

        if (mc.player.forwardSpeed > 0) {
            Vec3d currentMotion = mc.player.getVelocity();
            mc.player.setVelocity(x, currentMotion.y, z);
        } else if (mc.player.forwardSpeed < 0) {
            Vec3d currentMotion = mc.player.getVelocity();
            mc.player.setVelocity(-x, currentMotion.y, -z);
        } else if (mc.player.sidewaysSpeed != 0) {
            double sideYaw = yaw + (mc.player.sidewaysSpeed > 0 ? -Math.PI / 2 : Math.PI / 2);
            double sideX = -Math.sin(sideYaw) * speed;
            double sideZ = Math.cos(sideYaw) * speed;
            Vec3d currentMotion = mc.player.getVelocity();
            mc.player.setVelocity(sideX, currentMotion.y, sideZ);
        }
    }
}
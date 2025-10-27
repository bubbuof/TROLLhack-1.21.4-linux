package ru.zenith.implement.features.modules.movement;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.implement.events.player.TickEvent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Jesus extends Module {

    final float melonBallSpeed = 0.44F;

    public Jesus() {
        super("Jesus", "Jesus", ModuleCategory.MOVEMENT);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (mc.player.isInLava() || mc.player.isTouchingWater()) {
            StatusEffectInstance speedEffect = mc.player.getStatusEffect(StatusEffects.SPEED);
            StatusEffectInstance slownessEffect = mc.player.getStatusEffect(StatusEffects.SLOWNESS);

            ItemStack offHandItem = mc.player.getOffHandStack();
            String itemName = offHandItem.getName().getString();

            float appliedSpeed = 0F;

            // Проверка на ломтик дыни и эффект скорости 2 уровня
            if (itemName.contains("Ломтик Дыни") && speedEffect != null && speedEffect.getAmplifier() == 2) {
                appliedSpeed = 0.4283F * 1.15F;
            }
            else {
                if (speedEffect != null) {
                    if (speedEffect.getAmplifier() == 2) {
                        appliedSpeed = melonBallSpeed * 1.15F;
                    }
                    else if (speedEffect.getAmplifier() == 1) {
                        appliedSpeed = melonBallSpeed;
                    }
                }
                else {
                    appliedSpeed = melonBallSpeed * 0.68F;
                }
            }

            // Учет эффекта медлительности
            if (slownessEffect != null) {
                appliedSpeed *= 0.85f;
            }

            setSpeed(appliedSpeed);

            // Остановка при отсутствии движения
            boolean isMoving = mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed()
                    || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed();

            if (!isMoving) {
                mc.player.setVelocity(0.0, mc.player.getVelocity().y, 0.0);
            }

            // Вертикальное движение
            mc.player.setVelocity(
                    mc.player.getVelocity().x,
                    mc.options.jumpKey.isPressed() ? 0.019 : 0.003,
                    mc.player.getVelocity().z
            );
        }
    }

    // Метод для установки скорости (аналогично MoveUtility.setSpeed)
    private void setSpeed(float speed) {
        if (mc.player == null) return;

        float forward = mc.player.input.movementForward;
        float sideways = mc.player.input.movementSideways;
        float yaw = mc.player.getYaw();

        if (forward == 0 && sideways == 0) {
            mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
            return;
        }

        double rad = Math.toRadians(forward != 0 ? yaw + (forward > 0 ? 0 : 180) + (sideways > 0 ? -45 : sideways < 0 ? 45 : 0) : yaw + (sideways > 0 ? -90 : 90));
        double x = Math.sin(rad) * speed;
        double z = Math.cos(rad) * speed;

        mc.player.setVelocity(x, mc.player.getVelocity().y, z);
    }

    @Override
    public void activate() {
        super.activate();
    }

    @Override
    public void deactivate() {
        super.deactivate();
    }
}
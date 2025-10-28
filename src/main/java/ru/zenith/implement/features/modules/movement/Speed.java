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
import net.minecraft.util.Formatting;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Speed extends Module {

    final ValueSetting melonBallSpeed = new ValueSetting("Melon Ball Speed", "Base speed for melon ball")
            .range(0.1f, 1.0f)
            .setValue(0.36f);

    public Speed() {
        super("Speed", "Speed", ModuleCategory.MOVEMENT);
        setup(melonBallSpeed);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        ItemStack offHandItem = mc.player.getOffHandStack();
        StatusEffectInstance speedEffect = mc.player.getStatusEffect(StatusEffects.SPEED);
        StatusEffectInstance slownessEffect = mc.player.getStatusEffect(StatusEffects.SLOWNESS);
        String itemName = offHandItem.getName().getString();

        float appliedSpeed = 0;
        float baseSpeed = melonBallSpeed.getValue();

        if (speedEffect != null) {
            if (speedEffect.getAmplifier() == 2) {
                appliedSpeed = baseSpeed * 1.155f;
                if (itemName.contains("Ломтик Дыни") || itemName.contains("Melon Slice")) {
                    if (speedEffect != null && speedEffect.getAmplifier() == 2) {
                        appliedSpeed = 0.41755f;
                    } else {
                        appliedSpeed = 0.41755f * 0.52f;
                    }
                }
            } else if (speedEffect.getAmplifier() == 1) {
                appliedSpeed = baseSpeed;
            }
        } else {
            appliedSpeed = baseSpeed * 0.68f;
        }

        if (slownessEffect != null) {
            appliedSpeed *= 0.835f;
        }

        if (!mc.player.isOnGround()) {
            appliedSpeed *= 1.435f;
        }

        setSpeed(appliedSpeed);
    }

    private void setSpeed(float speed) {
        if (mc.player == null) return;

        double yaw = Math.toRadians(mc.player.getYaw());
        double x = -Math.sin(yaw) * speed;
        double z = Math.cos(yaw) * speed;

        if (mc.player.forwardSpeed > 0) {
            mc.player.setVelocity(x, mc.player.getVelocity().y, z);
        } else if (mc.player.forwardSpeed < 0) {
            mc.player.setVelocity(-x, mc.player.getVelocity().y, -z);
        } else if (mc.player.sidewaysSpeed != 0) {
            double sideYaw = yaw + (mc.player.sidewaysSpeed > 0 ? -Math.PI / 2 : Math.PI / 2);
            double sideX = -Math.sin(sideYaw) * speed;
            double sideZ = Math.cos(sideYaw) * speed;
            mc.player.setVelocity(sideX, mc.player.getVelocity().y, sideZ);
        }
    }
}
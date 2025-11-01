package ru.zenith.implement.features.modules.movement;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.BooleanSetting;
import ru.zenith.api.feature.module.setting.implement.SelectSetting;
import ru.zenith.api.feature.module.setting.implement.ValueSetting;
import ru.zenith.implement.events.player.TickEvent;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Speed extends Module {

    private final SelectSetting mode = new SelectSetting("Режим", "Способ ускорения")
            .value("MetaHVH", "GrimCollision", "GrimDistance")
            .selected("MetaHVH");

    // Настройки для MetaHVH
    private final ValueSetting melonBallSpeed = new ValueSetting("Melon Ball Speed", "Базовая скорость для Melon Ball")
            .range(0.1f, 1.0f)
            .setValue(0.36f)
            .visible(() -> mode.isSelected("MetaHVH"));

    // Настройки для Grim режимов
    private final BooleanSetting onlyPlayers = new BooleanSetting("Только Игроки", "Ускорение только от игроков")
            .setValue(false)
            .visible(() -> !mode.isSelected("MetaHVH"));
    private final ValueSetting radius = new ValueSetting("Радиус", "Радиус коллизии для ускорения")
            .setValue(1.0f).range(0.5f, 2.0f)
            .visible(() -> !mode.isSelected("MetaHVH"));
    private final ValueSetting distance = new ValueSetting("Дистанция", "Дистанция для ускорения")
            .setValue(2.5f).range(1.0f, 4.0f)
            .visible(() -> !mode.isSelected("MetaHVH"));
    private final ValueSetting grimSpeed = new ValueSetting("Скорость", "Множитель скорости для Grim режимов")
            .setValue(3.0f).range(1.0f, 5.0f)
            .visible(() -> !mode.isSelected("MetaHVH"));

    public Speed() {
        super("Speed", "Speed", ModuleCategory.MOVEMENT);
        setup(mode, melonBallSpeed, onlyPlayers, grimSpeed, distance, radius);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        switch (mode.getSelected()) {
            case "MetaHVH" -> handleMetaHVHMode();
            case "GrimDistance" -> handleGrimDistanceMode();
            case "GrimCollision" -> handleGrimCollisionMode();
        }
    }

    private void handleMetaHVHMode() {
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

    private void handleGrimDistanceMode() {
        for (PlayerEntity ent : mc.world.getPlayers()) {
            if (ent != mc.player && mc.player.squaredDistanceTo(ent) <= distance.getValue() * distance.getValue()) {
                float p = mc.world.getBlockState(mc.player.getBlockPos()).getBlock().getSlipperiness();
                float f = mc.player.isOnGround() ? p * 0.91f : 0.91f;
                float f2 = mc.player.isOnGround() ? p : 0.99f;

                double[] motion = forward((grimSpeed.getValue() * 0.01f) * f * f2);
                mc.player.addVelocity(motion[0], 0.0, motion[1]);
                break;
            }
        }
    }

    private void handleGrimCollisionMode() {
        if (!isMoving()) return;

        double[] motion = new double[]{0.0, 0.0};
        int collisions = 0;

        for (Entity ent : mc.world.getEntities()) {
            if (onlyPlayers.isValue() && !(ent instanceof PlayerEntity)) continue;
            if (ent != mc.player && (ent instanceof LivingEntity || ent instanceof PlayerEntity || ent instanceof BoatEntity)) {
                double dist = mc.player.squaredDistanceTo(ent);
                double influence = Math.max(0, radius.getValue() - Math.sqrt(dist));
                if (mc.player.getBoundingBox().expand(radius.getValue()).intersects(ent.getBoundingBox())) {
                    collisions++;
                    double[] forward = forward(grimSpeed.getValue() * 0.01f * influence);
                    motion[0] += forward[0];
                    motion[1] += forward[1];
                }
            }
        }

        if (collisions > 0) {
            double smoothFactor = 0.6;
            motion[0] = motion[0] + (motion[0] - motion[0]) * smoothFactor;
            motion[1] = motion[1] + (motion[1] - motion[1]) * smoothFactor;
            double maxSpeed = grimSpeed.getValue() * 0.02f;
            motion[0] = Math.max(Math.min(motion[0], maxSpeed), -maxSpeed);
            motion[1] = Math.max(Math.min(motion[1], maxSpeed), -maxSpeed);

            mc.player.addVelocity(motion[0], 0.0, motion[1]);
        }
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

    private double[] forward(double speed) {
        float yaw = (float) Math.toRadians(mc.player.getYaw());
        return new double[]{
                -Math.sin(yaw) * speed,
                Math.cos(yaw) * speed
        };
    }

    private boolean isMoving() {
        return mc.player.forwardSpeed != 0 || mc.player.sidewaysSpeed != 0;
    }
}
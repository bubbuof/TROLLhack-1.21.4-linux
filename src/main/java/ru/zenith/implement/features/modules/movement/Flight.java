package ru.zenith.implement.features.modules.movement;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.ValueSetting;
import ru.zenith.api.feature.module.setting.implement.SelectSetting;
import ru.zenith.api.feature.module.setting.implement.BooleanSetting;
import ru.zenith.common.util.other.Instance;
import ru.zenith.implement.events.player.TickEvent;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Flight extends Module {

    public static Flight getInstance() {
        return Instance.get(Flight.class);
    }

    SelectSetting mode = new SelectSetting("Mode", "Flight mode")
            .value("Creative", "Vanilla", "Jetpack")
            .selected("Creative");

    ValueSetting speed = new ValueSetting("Speed", "Flight speed")
            .setValue(1.0f).range(0.1f, 5.0f);

    ValueSetting verticalSpeed = new ValueSetting("Vertical Speed", "Up/Down speed")
            .setValue(1.0f).range(0.1f, 3.0f)
            .visible(() -> !mode.isSelected("Creative"));

    BooleanSetting antiKick = new BooleanSetting("Anti Kick", "Prevent being kicked for flying");
    BooleanSetting noFall = new BooleanSetting("No Fall", "Prevent fall damage");

    public Flight() {
        super("Flight", ModuleCategory.MOVEMENT);
        setup(mode, speed, verticalSpeed, antiKick, noFall);
    }

    @Override
    public void activate() {
        if (mc.player != null) {
            mc.player.getAbilities().flying = false;
        }
        super.activate();
    }

    @Override
    public void deactivate() {
        if (mc.player != null) {
            mc.player.getAbilities().flying = false;
            mc.player.getAbilities().setFlySpeed(0.05f);
        }
        super.deactivate();
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null) return;

        switch (mode.getSelected()) {
            case "Creative":
                handleCreative();
                break;
            case "Vanilla":
                handleVanilla();
                break;
            case "Jetpack":
                handleJetpack();
                break;
        }

        handleAntiKick();
        handleNoFall();
    }

    private void handleCreative() {
        mc.player.getAbilities().flying = true;
        mc.player.getAbilities().setFlySpeed(speed.getValue() * 0.05f);
    }

    private void handleVanilla() {
        double motionY = 0;

        if (mc.options.jumpKey.isPressed()) {
            motionY = verticalSpeed.getValue();
        } else if (mc.options.sneakKey.isPressed()) {
            motionY = -verticalSpeed.getValue();
        }

        Vec3d velocity = getMovementInput(speed.getValue());
        mc.player.setVelocity(velocity.x, motionY, velocity.z);
    }

    private void handleJetpack() {
        if (mc.options.jumpKey.isPressed()) {
            mc.player.setVelocity(mc.player.getVelocity().x, verticalSpeed.getValue(), mc.player.getVelocity().z);
        }

        Vec3d velocity = getMovementInput(speed.getValue());
        mc.player.setVelocity(velocity.x, mc.player.getVelocity().y, velocity.z);
    }

    private Vec3d getMovementInput(double speed) {
        double motionX = 0;
        double motionZ = 0;

        if (mc.options.forwardKey.isPressed()) {
            float yaw = (float) Math.toRadians(mc.player.getYaw());
            motionX -= Math.sin(yaw) * speed;
            motionZ += Math.cos(yaw) * speed;
        }
        if (mc.options.backKey.isPressed()) {
            float yaw = (float) Math.toRadians(mc.player.getYaw());
            motionX += Math.sin(yaw) * speed;
            motionZ -= Math.cos(yaw) * speed;
        }
        if (mc.options.leftKey.isPressed()) {
            float yaw = (float) Math.toRadians(mc.player.getYaw() - 90);
            motionX -= Math.sin(yaw) * speed;
            motionZ += Math.cos(yaw) * speed;
        }
        if (mc.options.rightKey.isPressed()) {
            float yaw = (float) Math.toRadians(mc.player.getYaw() + 90);
            motionX -= Math.sin(yaw) * speed;
            motionZ += Math.cos(yaw) * speed;
        }

        return new Vec3d(motionX, 0, motionZ);
    }

    private void handleAntiKick() {
        if (!antiKick.isValue()) return;

        // Анти-кик: периодически "падаем" чтобы обойти проверки
        if (mc.player.age % 80 == 0) {
            mc.player.setVelocity(mc.player.getVelocity().x, -0.04, mc.player.getVelocity().z);
        }
    }

    private void handleNoFall() {
        if (!noFall.isValue()) return;

        // Отключаем урон от падения - просто сбрасываем fallDistance
        if (mc.player.fallDistance > 2f) {
            mc.player.fallDistance = 0;

            // Альтернативный способ - отправка пакета на земле
            if (mc.player.age % 10 == 0) {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(
                        mc.player.getX(),
                        mc.player.getY(),
                        mc.player.getZ(),
                        mc.player.getYaw(),
                        mc.player.getPitch(),
                        true,
                        false  // Дополнительный параметр для изменения позиции
                ));
            }
        }
    }
}
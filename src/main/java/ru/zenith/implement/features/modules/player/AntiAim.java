package ru.zenith.implement.features.modules.player;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.*;
import ru.zenith.implement.events.player.TickEvent;
import net.minecraft.client.option.KeyBinding;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AntiAim extends Module {

    private static AntiAim instance;

    public static AntiAim getInstance() {
        if (instance == null) {
            instance = new AntiAim();
        }
        return instance;
    }

    // Pitch режимы
    final SelectSetting pitchMode = new SelectSetting("Pitch Mode", "Режим изменения pitch")
            .value("None", "RandomAngle", "Spin", "Sinus", "Fixed", "Static", "Jitter")
            .selected("None");

    // Yaw режимы
    final SelectSetting yawMode = new SelectSetting("Yaw Mode", "Режим изменения yaw")
            .value("None", "RandomAngle", "Spin", "Sinus", "Fixed", "Static", "Jitter")
            .selected("None");

    // Настройки
    final ValueSetting speed = new ValueSetting("Speed", "Скорость анимации")
            .range(1.0f, 45.0f)
            .setValue(1.0f);

    final ValueSetting yawDelta = new ValueSetting("Yaw Delta", "Изменение yaw")
            .range(-360.0f, 360.0f)
            .setValue(60.0f);

    final ValueSetting pitchDelta = new ValueSetting("Pitch Delta", "Изменение pitch")
            .range(-90.0f, 90.0f)
            .setValue(10.0f);

    final ValueSetting yawOffset = new ValueSetting("Yaw Offset", "Смещение yaw")
            .range(-180.0f, 180.0f)
            .setValue(0.0f);

    final BooleanSetting bodySync = new BooleanSetting("Body Sync", "Синхронизация с телом")
            .setValue(true);

    final BooleanSetting allowInteract = new BooleanSetting("Allow Interact", "Разрешить взаимодействие")
            .setValue(true);

    // Состояния
    float rotationYaw = 0.0f;
    float rotationPitch = 0.0f;
    float pitchSinStep = 0.0f;
    float yawSinStep = 0.0f;

    public AntiAim() {
        super("AntiAim", "AntiAim", ModuleCategory.PLAYER);
        setup(pitchMode, yawMode, speed, yawDelta, pitchDelta, yawOffset, bodySync, allowInteract);
        instance = this;
    }

    @Override
    public void activate() {
        super.activate();
        if (mc.player != null) {
            rotationYaw = mc.player.getYaw();
            rotationPitch = mc.player.getPitch();
        }
        pitchSinStep = 0.0f;
        yawSinStep = 0.0f;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null) return;

        // Проверяем можно ли применять AntiAim
        if (allowInteract.isValue()) {
            KeyBinding attackKey = mc.options.attackKey;
            KeyBinding useKey = mc.options.useKey;
            if ((attackKey != null && attackKey.isPressed()) || (useKey != null && useKey.isPressed())) {
                return;
            }
        }

        // Обработка Yaw режимов
        if (!pitchMode.isSelected("None")) {
            processYawModes();
        }

        // Обработка Pitch режимов
        if (!yawMode.isSelected("None")) {
            processPitchModes();
        }

        // Применяем повороты
        applyRotations();
    }

    private void processYawModes() {
        switch (yawMode.getSelected()) {
            case "RandomAngle" -> {
                if (mc.player.age % speed.getValue() == 0) {
                    rotationYaw = randomBetween(0.0f, 360.0f);
                }
            }
            case "Spin" -> {
                if (mc.player.age % speed.getValue() == 0) {
                    rotationYaw += yawDelta.getValue();
                    if (rotationYaw > 360.0f) rotationYaw = 0.0f;
                    if (rotationYaw < 0.0f) rotationYaw = 360.0f;
                }
            }
            case "Sinus" -> {
                yawSinStep += speed.getValue() / 10.0f;
                rotationYaw = (float) (mc.player.getYaw() + yawDelta.getValue() * Math.sin(yawSinStep) + yawOffset.getValue());
            }
            case "Fixed" -> rotationYaw = yawDelta.getValue();
            case "Static" -> rotationYaw = mc.player.getYaw() % 360.0f + yawDelta.getValue();
            case "Jitter" -> {
                int step = (int) (speed.getValue() * 2.0f);
                if (mc.player.age % step == 0) {
                    rotationYaw = yawDelta.getValue() / 2.0f + yawOffset.getValue() + mc.player.getYaw();
                }
                if (mc.player.age % step == speed.getValue()) {
                    rotationYaw = -yawDelta.getValue() / 2.0f + yawOffset.getValue() + mc.player.getYaw();
                }
            }
        }
    }

    private void processPitchModes() {
        switch (pitchMode.getSelected()) {
            case "RandomAngle" -> {
                if (mc.player.age % speed.getValue() == 0) {
                    rotationPitch = randomBetween(-90.0f, 90.0f);
                }
            }
            case "Spin" -> {
                if (mc.player.age % speed.getValue() == 0) {
                    rotationPitch += pitchDelta.getValue();
                    if (rotationPitch > 90.0f) rotationPitch = -90.0f;
                    if (rotationPitch < -90.0f) rotationPitch = 90.0f;
                }
            }
            case "Sinus" -> {
                pitchSinStep += speed.getValue() / 10.0f;
                rotationPitch = (float) (mc.player.getPitch() + pitchDelta.getValue() * Math.sin(pitchSinStep));
                rotationPitch = clamp(rotationPitch, -90.0f, 90.0f);
            }
            case "Fixed" -> rotationPitch = pitchDelta.getValue();
            case "Static" -> rotationPitch = clamp(mc.player.getPitch() + pitchDelta.getValue(), -90.0f, 90.0f);
            case "Jitter" -> {
                int step = (int) (speed.getValue() * 2.0f);
                if (mc.player.age % step == 0) {
                    rotationPitch = pitchDelta.getValue() / 2.0f;
                }
                if (mc.player.age % step == speed.getValue()) {
                    rotationPitch = -pitchDelta.getValue() / 2.0f;
                }
            }
        }
    }

    private void applyRotations() {
        boolean spoofYaw = !yawMode.isSelected("None");
        boolean spoofPitch = !pitchMode.isSelected("None");

        if (spoofYaw || spoofPitch) {
            float spoofedYaw = spoofYaw ? rotationYaw : mc.player.getYaw();
            float spoofedPitch = spoofPitch ? clamp(rotationPitch, -90.0f, 90.0f) : mc.player.getPitch();

            // Устанавливаем повороты напрямую
            mc.player.setYaw(spoofedYaw);
            mc.player.setPitch(spoofedPitch);

            // Если есть система RotationManager, можно использовать её:
            // RotationManager.INSTANCE.setRotation(new RotationTarget(...));
        }
    }

    private float randomBetween(float min, float max) {
        return (float) (Math.random() * (max - min) + min);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
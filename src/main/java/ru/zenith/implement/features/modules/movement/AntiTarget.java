package ru.zenith.implement.features.modules.movement;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.*;
import ru.zenith.implement.events.player.TickEvent;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AntiTarget extends Module {

    private static AntiTarget instance;

    public static AntiTarget getInstance() {
        if (instance == null) {
            instance = new AntiTarget();
        }
        return instance;
    }

    public double boosterSpeed = 1.0;

    final ValueSetting pitch = new ValueSetting("Pitch", "Угол наклона")
            .range(10.0f, 60.0f)
            .setValue(35.0f);

    final ValueSetting yaw = new ValueSetting("Yaw", "Угол поворота")
            .range(-180.0f, 180.0f)
            .setValue(0.0f);

    public AntiTarget() {
        super("AntiTarget", "AntiTarget", ModuleCategory.MOVEMENT);
        setup(pitch, yaw);
        instance = this;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null) return;

        // Проверяем что игрок летит на элитрах (альтернативные методы)
        if (isUsingElytra()) {
            try {
                // Устанавливаем углы поворота для уклонения
                mc.player.setPitch(-pitch.getValue());
                mc.player.setYaw(yaw.getValue());
            } catch (Throwable ignored) {
            }

            // Устанавливаем фиксированную скорость (без зависимости от ElytraBooster)
            this.boosterSpeed = 1.0;
        }
    }

    private boolean isUsingElytra() {
        if (mc.player == null) return false;

        // Попробуйте один из этих вариантов:

        // Вариант 1: Проверка через isFallFlying (если доступен)
        // return mc.player.isFallFlying();

        // Вариант 2: Проверка через экипировку и состояние
        // return mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA
        //        && mc.player.isFallFlying();

        // Вариант 3: Универсальная проверка через скорость и положение
        return !mc.player.isOnGround() &&
                !mc.player.isSubmergedInWater() &&
                mc.player.getVelocity().y < -0.1;

        // Вариант 4: Простая проверка через NBT или состояние
        // return mc.player.getDataTracker().get(Entity.FALL_FLYING);
    }

    // Геттер для скорости бустера (может использоваться другими модулями)
    public double getBoosterSpeed() {
        return boosterSpeed;
    }
}
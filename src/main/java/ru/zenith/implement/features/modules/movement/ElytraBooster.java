package ru.zenith.implement.features.modules.movement;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.BooleanSetting;
import ru.zenith.api.feature.module.setting.implement.ValueSetting;
import ru.zenith.common.util.other.Instance;
import ru.zenith.implement.events.player.RotationUpdateEvent;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ElytraBooster extends Module {
    public static ElytraBooster getInstance() {
        return Instance.get(ElytraBooster.class);
    }

    // Настройки
    BooleanSetting autoFirework = new BooleanSetting("Auto Firework", "Автоматически использовать фейерверки")
            .setValue(true);

    ValueSetting boostDelay = new ValueSetting("Boost Delay", "Задержка между ускорениями")
            .setValue(500f).range(100f, 2000f).visible(() -> autoFirework.isValue());

    BooleanSetting smartBoost = new BooleanSetting("Smart Boost", "Умное использование фейерверков")
            .setValue(true);

    ValueSetting minHeight = new ValueSetting("Min Height", "Минимальная высота для ускорения")
            .setValue(10f).range(0f, 50f).visible(() -> smartBoost.isValue());

    BooleanSetting optimizeAngle = new BooleanSetting("Optimize Angle", "Оптимизировать угол полета")
            .setValue(true);

    long lastBoostTime = 0;

    public ElytraBooster() {
        super("ElytraBooster","ElytraBooster", ModuleCategory.MOVEMENT);
        setup(autoFirework, boostDelay, smartBoost, minHeight, optimizeAngle);
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent event) {
        if (mc.player == null || !isElytraFlying()) return;

        // Оптимизация угла полета
        if (optimizeAngle.isValue()) {
            optimizeFlightAngle();
        }

        // Авто-фейерверки
        if (autoFirework.isValue() && shouldUseFirework()) {
            useFirework();
        }
    }

    private boolean isElytraFlying() {
        if (mc.player == null) return false;

        // Проверка полета на элитрах через скорость и наличие элитры
        if (mc.player.getInventory().getArmorStack(2).getItem() == Items.ELYTRA) {
            double velocityY = mc.player.getVelocity().y;
            boolean isGliding = velocityY < -0.1 && Math.abs(velocityY) < 1.0;
            boolean hasHorizontalSpeed = mc.player.getVelocity().horizontalLengthSquared() > 0.1;

            return isGliding || hasHorizontalSpeed;
        }

        return false;
    }

    private boolean shouldUseFirework() {
        if (System.currentTimeMillis() - lastBoostTime < boostDelay.getValue()) {
            return false;
        }

        // Проверка умного использования
        if (smartBoost.isValue()) {
            // Не использовать если слишком низко
            if (mc.player.getY() < minHeight.getValue()) {
                return false;
            }

            // Использовать только когда летим горизонтально или вниз
            return mc.player.getPitch() >= -30;
        }

        return true;
    }

    private void useFirework() {
        int fireworkSlot = findFireworkSlot();
        if (fireworkSlot == -1) return;

        int currentSlot = mc.player.getInventory().selectedSlot;

        // Переключаемся на фейерверк и используем
        mc.player.getInventory().selectedSlot = fireworkSlot;
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);

        // Возвращаем слот
        mc.player.getInventory().selectedSlot = currentSlot;

        lastBoostTime = System.currentTimeMillis();
    }

    private int findFireworkSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.FIREWORK_ROCKET) {
                return i;
            }
        }
        return -1;
    }

    private void optimizeFlightAngle() {
        // Оптимизация угла для максимальной скорости
        // Обычно это небольшой наклон вниз (-10 до -20 градусов)
        float currentPitch = mc.player.getPitch();
        float optimalPitch = -15f; // Оптимальный угол для скорости

        if (Math.abs(currentPitch - optimalPitch) > 5) {
            mc.player.setPitch(optimalPitch);
        }
    }
}
package ru.zenith.implement.features.modules.movement;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.setting.implement.BooleanSetting;
import ru.zenith.common.util.other.Instance;
import ru.zenith.implement.events.player.TickEvent;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoSprint extends Module {
    public static AutoSprint getInstance() {
        return Instance.get(AutoSprint.class);
    }

    private final BooleanSetting multiDirectionSetting = new BooleanSetting("Multi-Directional", "Sprint in all directions")
            .setValue(false);

    // Убираем все лишние настройки которые могут флагать
    private final BooleanSetting onlyForwardSetting = new BooleanSetting("Only Forward", "Sprint only when moving forward")
            .setValue(true);

    public AutoSprint() {
        super("AutoSprint", "Automatically sprints for you", ModuleCategory.MOVEMENT);
        setup(multiDirectionSetting, onlyForwardSetting);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        // Не трогаем если игрок сам нажал спринт
        if (mc.options.sprintKey.isPressed()) return;

        // Проверяем можно ли спринтовать
        if (!canSprint()) {
            return;
        }

        // Проверяем движение
        if (!shouldSprint()) {
            return;
        }

        // ВКЛЮЧАЕМ СПРИНТ ПРОСТО И БЕЗОПАСНО
        mc.player.setSprinting(true);
    }

    private boolean canSprint() {
        if (mc.player == null) return false;

        // Минимальные проверки - только то что действительно важно
        boolean isFlying = mc.player.getAbilities().flying;
        boolean isRiding = mc.player.getVehicle() != null;
        boolean isInWater = mc.player.isTouchingWater();
        boolean isInLava = mc.player.isInLava();
        boolean isClimbing = mc.player.isClimbing();
        boolean isSneaking = mc.player.isSneaking();

        // Только критические ограничения
        if (isFlying || isRiding || isInWater || isInLava || isClimbing || isSneaking) {
            return false;
        }

        // Голод проверяем всегда (как в ваниле)
        if (mc.player.getHungerManager().getFoodLevel() <= 6) {
            return false;
        }

        return true;
    }

    private boolean shouldSprint() {
        if (mc.player == null) return false;

        // Multi-directional
        if (multiDirectionSetting.isValue()) {
            return mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0;
        }

        // Only forward (по умолчанию)
        if (onlyForwardSetting.isValue()) {
            return mc.player.input.movementForward > 0;
        }

        // Любое движение
        return mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0;
    }

    // Геттер для mixin
    public BooleanSetting getIgnoreHungerSetting() {
        // Возвращаем фиктивную настройку всегда false
        return new BooleanSetting("dummy", "").setValue(false);
    }
}
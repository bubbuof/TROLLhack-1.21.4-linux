package ru.zenith.implement.features.modules.movement;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.SelectSetting;
import ru.zenith.implement.events.player.MoveEvent;
import ru.zenith.implement.events.player.TickEvent;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoSlow extends Module {

    // Статическая ссылка на экземпляр
    private static NoSlow instance;

    final SelectSetting mode = new SelectSetting("Mode", "NoSlow mode")
            .value("Vanilla")
            .value("HolyWorld")
            .value("Grim Latest");

    // Добавим настройку для миксина
    public final SelectSetting slowTypeSetting = new SelectSetting("Slow Type", "Type of slow down to prevent")
            .value("Using Item")
            .value("Other");

    public static int ticks = 0;

    public NoSlow() {
        super("NoSlow", "NoSlow", ModuleCategory.MOVEMENT);
        setup(mode, slowTypeSetting);
        instance = this;
    }

    // Статический метод для доступа из миксинов
    public static NoSlow getInstance() {
        return instance;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mode.isSelected("Grim Latest") && mc.player != null) {
            if (mc.player.isUsingItem()) {
                ticks++;
            } else {
                ticks = 0;
            }
        }
    }

    @EventHandler
    public void onMove(MoveEvent event) {
        if (mc.player == null) return;

        String modeValue = mode.getSelected();
        switch (modeValue) {
            case "Vanilla":
                // Убираем замедление путем увеличения скорости
                if (mc.player.isUsingItem()) {
                    event.setMovement(event.getMovement().multiply(1.2));
                }
                break;
            case "HolyWorld":
                if (mc.player.isUsingItem()) {
                    Hand activeHand = mc.player.getActiveHand();

                    // Исправленный конструктор пакета для 1.21.4
                    mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(activeHand, 0, 0.0F, 0.0F));
                    mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(
                            activeHand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND, 0, 0.0F, 0.0F));

                    // Убираем замедление
                    event.setMovement(event.getMovement().multiply(1.2));
                }
                break;
            case "Grim Latest":
                if (mc.player.isUsingItem() && ticks >= 2) {
                    // Убираем замедление
                    event.setMovement(event.getMovement().multiply(1.2));
                    ticks = 0;
                }
                break;
        }
    }
}
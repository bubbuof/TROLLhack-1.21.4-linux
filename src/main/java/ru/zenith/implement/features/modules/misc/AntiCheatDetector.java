package ru.zenith.implement.features.modules.misc;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.*;
import ru.zenith.implement.events.packet.PacketEvent;
import net.minecraft.text.Text;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AntiCheatDetector extends Module {

    private static AntiCheatDetector instance;

    public static AntiCheatDetector getInstance() {
        if (instance == null) {
            instance = new AntiCheatDetector();
        }
        return instance;
    }

    final BooleanSetting acDetector = new BooleanSetting("AcDetector", "Детектит античит на сервере")
            .setValue(false);

    final BooleanSetting showTransactions = new BooleanSetting("Show Transactions", "Показывать транзакции")
            .setValue(true);

    final BooleanSetting showFlags = new BooleanSetting("Show Flags", "Показывать флаги АЧ")
            .setValue(true);

    final BooleanSetting showVelocity = new BooleanSetting("Show Velocity", "Показывать велосити")
            .setValue(false);

    final AtomicInteger ticks = new AtomicInteger(0);

    public AntiCheatDetector() {
        super("AntiCheatDetector", "AntiCheatDetector", ModuleCategory.MISC);
        setup(acDetector, showTransactions, showFlags, showVelocity);
        instance = this;
    }

    @Override
    public void deactivate() {
        super.deactivate();
        ticks.set(0);
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (!acDetector.isValue()) return;

        if (event.getType() == PacketEvent.Type.RECEIVE) {
            if (showTransactions.isValue()) {
                Object packet = event.getPacket();
                boolean looksLikeConfirm = false;

                // Проверяем типы пакетов транзакций
                String packetName = packet.getClass().getSimpleName().toLowerCase();
                if (packetName.contains("confirm") && packetName.contains("screen") ||
                        packetName.contains("screen") && (packetName.contains("content") || packetName.contains("set") || packetName.contains("update")) ||
                        packetName.contains("handler") && (packetName.contains("content") || packetName.contains("slot") || packetName.contains("property"))) {
                    looksLikeConfirm = true;
                }

                if (looksLikeConfirm) {
                    int actionId = safeGetActionId(packet);
                    if (!acDetector.isValue()) {
                        chat("transaction: " + actionId);
                    } else {
                        int t = ticks.incrementAndGet();
                        String acName = detectAC(actionId, t);
                        chat("AC: " + acName);
                    }

                    if (ticks.get() > 300) {
                        chat("Античит не найден, выключаю модуль");
                        deactivate();
                    }
                }
            }
        }
    }

    private void chat(String message) {
        try {
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal(message), false);
            }
        } catch (Throwable ignored) {
        }
    }

    private int safeGetActionId(Object packet) {
        try {
            // Пробуем получить actionId через метод
            try {
                Object value = packet.getClass().getMethod("getActionId").invoke(packet);
                if (value instanceof Number number) {
                    return number.intValue();
                }
            } catch (NoSuchMethodException ignored) {
            }

            // Пробуем получить через поля
            String[] fieldNames = {"actionId", "actionNumber", "action", "slotId"};
            for (String fieldName : fieldNames) {
                try {
                    Field field = packet.getClass().getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object value = field.get(packet);
                    if (value instanceof Number number) {
                        return number.intValue();
                    }
                } catch (NoSuchFieldException ignored) {
                }
            }
        } catch (Throwable ignored) {
        }

        return 0;
    }

    private String detectAC(int actionId, int tickCount) {
        if (actionId < 0 && actionId > -100 && tickCount < 30) {
            return "GrimAC";
        } else if (tickCount > 30 && actionId < -100) {
            return "Polar";
        } else if (actionId >= 100 && actionId < 500) {
            return "Matrix";
        } else if (actionId == 1488) {
            return "NeetAC обнаружен, выключаюсь";
        } else if (actionId < -23764 && actionId > -10000000) {
            return "Vulcan";
        } else {
            return "Не удалось обнаружить античит";
        }
    }
}
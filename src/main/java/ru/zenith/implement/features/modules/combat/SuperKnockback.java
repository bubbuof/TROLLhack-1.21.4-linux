package ru.zenith.implement.features.modules.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.*;
import ru.zenith.implement.events.player.AttackEvent;
import ru.zenith.implement.events.player.TickEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class SuperKnockback extends Module {

    private static SuperKnockback instance;

    public static SuperKnockback getInstance() {
        if (instance == null) {
            instance = new SuperKnockback();
        }
        return instance;
    }

    // Основные настройки
    final ValueSetting chance = new ValueSetting("Chance", "Шанс срабатывания")
            .range(0.0f, 100.0f)
            .setValue(100.0f);

    final ValueSetting delayMs = new ValueSetting("Delay", "Задержка между атаками")
            .range(0.0f, 500.0f)
            .setValue(0.0f);

    final ValueSetting hurtTimeMax = new ValueSetting("HurtTime", "Максимальное время получения урона цели")
            .range(0.0f, 10.0f)
            .setValue(10.0f);

    // Режимы
    final SelectSetting mode = new SelectSetting("Mode", "Метод увеличения отдачи")
            .value("WTap", "SprintTap", "SprintTap2", "Old", "Silent", "Packet", "SneakPacket")
            .selected("Old");

    // Настройки для SprintTap2
    final ValueSetting stopTicks = new ValueSetting("PressBackTicks", "Тики остановки спринта")
            .range(1.0f, 5.0f)
            .setValue(1.0f)
            .visible(() -> mode.isSelected("SprintTap2"));

    final ValueSetting unSprintTicks = new ValueSetting("ReleaseBackTicks", "Тики возобновления спринта")
            .range(1.0f, 5.0f)
            .setValue(2.0f)
            .visible(() -> mode.isSelected("SprintTap2"));

    // Условия
    final BooleanSetting onlyGround = new BooleanSetting("OnlyGround", "Только на земле")
            .setValue(false);

    final BooleanSetting onlyMove = new BooleanSetting("OnlyMove", "Только при движении")
            .setValue(true);

    final BooleanSetting onlyMoveForward = new BooleanSetting("OnlyMoveForward", "Только при движении вперед")
            .setValue(true);

    // Состояния
    long lastAttackAt = 0L;
    int ticks = 0;
    int sprint2Ticks = 0;

    public SuperKnockback() {
        super("SuperKnockback", "SuperKnockback", ModuleCategory.COMBAT);
        setup(chance, delayMs, hurtTimeMax, mode, stopTicks, unSprintTicks, onlyGround, onlyMove, onlyMoveForward);
        instance = this;
    }

    @Override
    public void deactivate() {
        ticks = 0;
        sprint2Ticks = 0;
        super.deactivate();
    }

    @EventHandler
    public void onAttack(AttackEvent event) {
        Entity target = event.getEntity();
        if (target instanceof LivingEntity livingTarget) {
            if (mc.player != null) {
                if (livingTarget.hurtTime <= hurtTimeMax.getValue()) {
                    if (!onlyGround.isValue() || mc.player.isOnGround()) {
                        if (onlyMove.isValue()) {
                            boolean moving = mc.player.input != null &&
                                    (mc.player.input.movementSideways != 0.0F || mc.player.input.movementForward != 0.0F);
                            if (!moving) {
                                return;
                            }

                            if (onlyMoveForward.isValue() && mc.player.input.movementForward <= 0.0F) {
                                return;
                            }
                        }

                        if (chance.getValue() < 100.0f) {
                            int roll = (int)(Math.random() * 100.0f);
                            if (roll >= chance.getValue()) {
                                return;
                            }
                        }

                        long delay = (long) delayMs.getValue();
                        if (System.currentTimeMillis() - lastAttackAt >= delay) {
                            switch (mode.getSelected()) {
                                case "Packet" -> {
                                    stopSprinting();
                                    startSprinting();
                                }
                                case "SneakPacket" -> {
                                    stopSprinting();
                                    startSneaking();
                                    startSprinting();
                                    stopSneaking();
                                }
                                case "Old" -> {
                                    if (mc.player.isSprinting()) {
                                        stopSprinting();
                                    }
                                    startSprinting();
                                    stopSprinting();
                                    startSprinting();
                                    mc.player.setSprinting(true);
                                }
                                case "SprintTap", "Silent" -> {
                                    ticks = 2;
                                }
                                case "SprintTap2" -> {
                                    sprint2Ticks = 0;
                                }
                                case "WTap" -> {
                                    if (mc.player.isSprinting()) {
                                        stopSprinting();
                                    }
                                    startSprinting();
                                }
                            }

                            lastAttackAt = System.currentTimeMillis();
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null) return;

        // Обработка SprintTap и Silent режимов
        if (mode.isSelected("SprintTap") || mode.isSelected("Silent")) {
            if (ticks == 2) {
                mc.player.setSprinting(false);
                stopSprinting();
                ticks--;
            } else if (ticks == 1) {
                if (mc.player.input != null && mc.player.input.movementForward > 0.0F) {
                    mc.player.setSprinting(true);
                }
                startSprinting();
                ticks--;
            }
        }

        // Обработка SprintTap2 режима
        if (mode.isSelected("SprintTap2")) {
            sprint2Ticks++;
            if (sprint2Ticks == stopTicks.getValue()) {
                boolean nowSprint = !mc.player.isSprinting();
                mc.player.setSprinting(nowSprint);
                if (nowSprint) {
                    startSprinting();
                } else {
                    stopSprinting();
                }
                mc.player.setVelocity(0.0, mc.player.getVelocity().y, 0.0);
            } else if (sprint2Ticks >= unSprintTicks.getValue()) {
                mc.player.setSprinting(false);
                stopSprinting();
                sprint2Ticks = 0;
            }
        }
    }

    private void startSprinting() {
        if (mc.player != null && mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
        }
    }

    private void stopSprinting() {
        if (mc.player != null && mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        }
    }

    private void startSneaking() {
        if (mc.player != null && mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
        }
    }

    private void stopSneaking() {
        if (mc.player != null && mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
        }
    }
}
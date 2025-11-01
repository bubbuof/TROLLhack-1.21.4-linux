package ru.zenith.implement.features.modules.combat;

import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.SelectSetting;
import ru.zenith.implement.events.packet.PacketEvent;
import ru.zenith.implement.events.player.TickEvent;

public class Velocity extends Module {

    private final SelectSetting mode = new SelectSetting("Режим", "Способ обхода анти-кибка")
            .value("Cancel", "Grim Skip", "Grim Cancel", "Funtime")
            .selected("Cancel");

    private int skip = 0;
    private boolean cancel;
    private boolean damaged;

    public Velocity() {
        super("Velocity", "Velocity", ModuleCategory.COMBAT);
        setup(mode);
    }

    @Override
    public void activate() {
        skip = 0;
        cancel = false;
        damaged = false;
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (mc.player == null || mc.world == null) return;

        // Обработка удочки (общая для всех режимов)
        if (event.getPacket() instanceof EntityStatusS2CPacket statusPacket) {
            if (statusPacket.getStatus() == 31 && statusPacket.getEntity(mc.world) instanceof FishingBobberEntity hook) {
                if (hook.getHookedEntity() == mc.player) {
                    event.setCancelled(true);
                }
            }
        }

        switch (mode.getSelected()) {
            case "Cancel" -> handleCancelMode(event);
            case "Grim Skip" -> handleGrimSkipMode(event);
            case "Grim Cancel" -> handleGrimCancelMode(event);
            case "Funtime" -> handleFuntimeMode(event);
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null) return;

        switch (mode.getSelected()) {
            case "Grim Cancel" -> handleGrimCancelTick();
            case "Funtime" -> handleFuntimeTick();
        }
    }

    private void handleCancelMode(PacketEvent event) {
        // Просто отменяем все velocity и explosion пакеты
        if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket) {
            event.setCancelled(true);
        }
        if (event.getPacket() instanceof ExplosionS2CPacket) {
            event.setCancelled(true);
        }
    }

    private void handleGrimSkipMode(PacketEvent event) {
        if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket) {
            // Уменьшаем velocity вручную
            mc.player.setVelocity(
                    mc.player.getVelocity().x * 0.2,
                    mc.player.getVelocity().y * 0.2,
                    mc.player.getVelocity().z * 0.2
            );
            mc.player.velocityModified = true;
            event.setCancelled(true);
        }
    }

    private void handleGrimCancelMode(PacketEvent event) {
        if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket) {
            event.setCancelled(true);
            cancel = true;
        }
        if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            skip = 3;
        }
    }

    private void handleFuntimeMode(PacketEvent event) {
        if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket) {
            if (skip >= 2) return;
            event.setCancelled(true);
            damaged = true;
        }
        if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            skip = 3;
        }
    }

    private void handleGrimCancelTick() {
        if (cancel) {
            skip--;
            if (skip <= 0) {
                BlockPos pos = mc.player.getBlockPos();
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
                        mc.player.getX(),
                        mc.player.getY(),
                        mc.player.getZ(),
                        mc.player.getYaw(),
                        mc.player.getPitch(),
                        mc.player.isOnGround(),
                        false
                ));
                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
                        pos,
                        Direction.UP
                ));
                cancel = false;
            }
        }
    }

    private void handleFuntimeTick() {
        skip--;
        if (damaged) {
            BlockPos pos = mc.player.getBlockPos();
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
                    mc.player.getX(),
                    mc.player.getY(),
                    mc.player.getZ(),
                    mc.player.getYaw(),
                    mc.player.getPitch(),
                    mc.player.isOnGround(),
                    false
            ));
            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
                    pos,
                    Direction.UP
            ));
            damaged = false;
        }
    }
}
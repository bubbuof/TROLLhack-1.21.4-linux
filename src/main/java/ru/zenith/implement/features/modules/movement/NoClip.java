package ru.zenith.implement.features.modules.movement;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.SelectSetting;
import ru.zenith.api.feature.module.setting.implement.ValueSetting;
import ru.zenith.implement.events.player.TickEvent;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoClip extends Module {

    final List<Packet<?>> bufferedPackets = new ArrayList<>();
    final SelectSetting releaseMode = new SelectSetting("Mode", "Release mode when exiting blocks")
            .value("Simple", "Double", "Desync", "None");

    final ValueSetting semiPackets = new ValueSetting("Packet Count", "Number of packets to send")
            .setValue(2.0f).range(1.0f, 15.0f);

    boolean semiPacketSent;
    boolean skipReleaseOnDisable;

    public NoClip() {
        super("NoClip", "NoClip", ModuleCategory.MOVEMENT);
        setup(releaseMode, semiPackets);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        // Упрощенная проверка на нахождение внутри блока
        Box shrunkBox = mc.player.getBoundingBox().shrink(0.001D, 0.001D, 0.001D);
        boolean isInsideBlock = isInsideBlock(shrunkBox);
        boolean noSolidInAABB = !isInsideBlock;

        if (!semiPacketSent && isInsideBlock) {
            double x = mc.player.getX();
            double y = mc.player.getY();
            double z = mc.player.getZ();
            float yaw = mc.player.getYaw();
            float pitch = mc.player.getPitch();
            boolean onGround = mc.player.isOnGround();

            for (int i = 0; i < semiPackets.getInt(); i++) {
                // Используем правильный конструктор для 1.21.4
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
                        x, y, z, yaw, pitch, onGround, false
                ));
            }
            semiPacketSent = true;
            return;
        }

        if (semiPacketSent && noSolidInAABB) {
            skipReleaseOnDisable = true;
            setState(false);
        }
    }

    // Упрощенный метод проверки нахождения внутри блока
    private boolean isInsideBlock(Box box) {
        // Проверяем несколько точек внутри bounding box
        double centerX = (box.minX + box.maxX) / 2;
        double centerY = (box.minY + box.maxY) / 2;
        double centerZ = (box.minZ + box.maxZ) / 2;

        // Проверяем центр и углы
        return !mc.world.getBlockState(mc.player.getBlockPos()).isAir() ||
                !mc.world.getBlockState(mc.player.getBlockPos().down()).isAir() ||
                !mc.world.getBlockState(mc.player.getBlockPos().up()).isAir();
    }

    @Override
    public void deactivate() {
        if (!skipReleaseOnDisable && semiPacketSent) {
            if (!releaseMode.isSelected("None")) {
                runReleaseSequence(releaseMode.getSelected());
            } else {
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
                        mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                        mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround(), false
                ));
            }
        }

        if (mc.player != null && mc.player.networkHandler != null && !bufferedPackets.isEmpty()) {
            for (Packet<?> packet : bufferedPackets) {
                mc.player.networkHandler.sendPacket(packet);
            }
            bufferedPackets.clear();
        }

        super.deactivate();
    }

    @Override
    public void activate() {
        super.activate();
        bufferedPackets.clear();
        semiPacketSent = false;
        skipReleaseOnDisable = false;
    }

    private void runReleaseSequence(String mode) {
        if (mc.player == null || mc.player.networkHandler == null) return;

        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();

        switch (mode.toLowerCase()) {
            case "simple": {
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x - 5000, y, z - 5000, yaw, pitch, false, false));
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, mc.player.isOnGround(), false));
                break;
            }
            case "double": {
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x - 5000, y, z - 5000, yaw, pitch, false, false));
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x + 5000, y, z + 5000, yaw, pitch, false, false));
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, mc.player.isOnGround(), false));
                break;
            }
            case "desync": {
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x, y + 0.0625, z, yaw, pitch, false, false));
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, false, false));
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x, y + 0.03125, z, yaw, pitch, true, false));
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, mc.player.isOnGround(), false));
                break;
            }
            default: {
                break;
            }
        }
    }
}
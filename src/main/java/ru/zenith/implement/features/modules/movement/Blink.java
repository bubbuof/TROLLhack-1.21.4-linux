package ru.zenith.implement.features.modules.movement;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.util.math.Box;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.ValueSetting;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.entity.PlayerIntersectionUtil;
import ru.zenith.common.util.other.StopWatch;
import ru.zenith.common.util.render.Render3DUtil;
import ru.zenith.implement.events.packet.PacketEvent;
import ru.zenith.implement.events.render.WorldRenderEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Blink extends Module {
    private final List<Packet<?>> packets = new CopyOnWriteArrayList<>();
    private final ValueSetting delaySetting = new ValueSetting("Delay", "Server position update delay (ms)")
            .setValue(500).range(100, 2000);

    private final StopWatch packetTimer = new StopWatch();
    private Box serverBox; // Позиция на СЕРВЕРЕ (с задержкой)

    public Blink() {
        super("Blink", ModuleCategory.MOVEMENT);
        setup(delaySetting);
    }

    @Override
    public void activate() {
        updateServerBox();
        packetTimer.reset();
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (PlayerIntersectionUtil.nullCheck()) return;

        switch (e.getPacket()) {
            case PlayerRespawnS2CPacket respawn -> {
                setState(false);
                sendAllPackets();
            }
            case GameJoinS2CPacket join -> {
                setState(false);
                sendAllPackets();
            }
            case ClientStatusC2SPacket status when status.getMode().equals(ClientStatusC2SPacket.Mode.PERFORM_RESPAWN) -> {
                setState(false);
                sendAllPackets();
            }
            default -> {
                if (e.isSend()) {
                    // Все исходящие пакеты задерживаем
                    packets.add(e.getPacket());
                    e.cancel();

                    // Пакеты движения отправляем на сервер только по таймеру
                    if (e.getPacket() instanceof net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket) {
                        if (packetTimer.finished(delaySetting.getInt())) {
                            sendBufferedPackets();
                            updateServerBox(); // Обновляем СЕРВЕРНУЮ позицию
                            packetTimer.reset();
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        // Отрисовываем только СЕРВЕРНУЮ позицию (где нас видят другие игроки)
        if (serverBox != null) {
            Render3DUtil.drawBox(serverBox, ColorUtil.getColor(255, 0, 0), 2);
        }
    }

    private void updateServerBox() {
        if (mc.player != null) {
            serverBox = mc.player.getBoundingBox();
        }
    }

    // Отправляем накопленные пакеты на сервер
    private void sendBufferedPackets() {
        packets.forEach(PlayerIntersectionUtil::sendPacketWithOutEvent);
        packets.clear();
    }

    // Отправляем ВСЕ пакеты при выключении
    private void sendAllPackets() {
        sendBufferedPackets();
        serverBox = null;
    }

    @Override
    public void deactivate() {
        sendAllPackets();
    }
}
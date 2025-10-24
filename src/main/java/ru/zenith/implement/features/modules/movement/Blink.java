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
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.entity.PlayerIntersectionUtil;
import ru.zenith.common.util.render.Render3DUtil;
import ru.zenith.implement.events.packet.PacketEvent;
import ru.zenith.implement.events.render.WorldRenderEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Blink extends Module {
    private final List<Packet<?>> packets = new CopyOnWriteArrayList<>();
    private Box box;

    public Blink() {
        super("Blink", ModuleCategory.MOVEMENT);
        setup();
    }

    @Override
    public void activate() {
        box = mc.player.getBoundingBox();
    }

    @Override
    public void deactivate() {
        packets.forEach(PlayerIntersectionUtil::sendPacketWithOutEvent);
        packets.clear();
    }

    
    @EventHandler
    public void onPacket(PacketEvent e) {
        if (PlayerIntersectionUtil.nullCheck()) return;
        switch (e.getPacket()) {
            case PlayerRespawnS2CPacket respawn -> setState(false);
            case GameJoinS2CPacket join -> setState(false);
            case ClientStatusC2SPacket status when status.getMode().equals(ClientStatusC2SPacket.Mode.PERFORM_RESPAWN) ->
                    setState(false);
            default -> {
                if (e.isSend()) {
                    packets.add(e.getPacket());
                    e.cancel();
                }
            }
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (box != null) {
            Render3DUtil.drawBox(box, ColorUtil.getClientColor(), 1);
        }
    }
}

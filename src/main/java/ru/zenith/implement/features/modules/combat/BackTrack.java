package ru.zenith.implement.features.modules.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.*;
import ru.zenith.implement.events.keyboard.KeyEvent;
import ru.zenith.implement.events.player.AttackEvent;
import ru.zenith.implement.events.player.TickEvent;
import ru.zenith.implement.events.render.WorldRenderEvent;
import ru.zenith.implement.events.packet.PacketEvent;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.common.util.render.Render3DUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class BackTrack extends Module {

    // Настройки
    final BindSetting resetKey = new BindSetting("Reset", "Сбросить позицию");

    final ValueSetting range = new ValueSetting("Range", "Дистанция работы")
            .range(3.0f, 100.0f)
            .setValue(3.0f);

    final ValueSetting delayMs = new ValueSetting("Delay", "Задержка пакетов")
            .range(100.0f, 1000.0f)
            .setValue(500.0f);

    // Состояния (без final чтобы можно было изменять)
    final List<Queued> queue = new LinkedList<>();
    Entity target;
    Vec3d realPos;
    Vec3d interpRealPos;

    public BackTrack() {
        super("BackTrack", "BackTrack", ModuleCategory.COMBAT);
        setup(resetKey, range, delayMs);
    }

    @Override
    public void activate() {
        super.activate();
        if (mc.isIntegratedServerRunning()) {
            deactivate();
        } else {
            reset();
        }
    }

    @Override
    public void deactivate() {
        super.deactivate();
        if (!mc.isIntegratedServerRunning()) {
            reset();
        }
    }

    @EventHandler
    public void onKey(KeyEvent event) {
        if (event.isKeyDown(resetKey.getKey())) {
            reset();
        }
    }

    @EventHandler
    public void onAttack(AttackEvent event) {
        Entity targetEntity = event.getEntity();
        if (targetEntity != null) {
            if (targetEntity != this.target && !targetEntity.isRemoved()) {
                this.target = targetEntity;
                this.realPos = targetEntity.getPos();
                this.interpRealPos = this.realPos;
            }
        }
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (event.getType() == PacketEvent.Type.RECEIVE && !mc.isIntegratedServerRunning()) {
            if (shouldLag()) {
                Object packet = event.getPacket();

                // Игнорируем определенные пакеты
                if (!(packet instanceof EntityS2CPacket.Rotate) &&
                        !(packet instanceof EntityS2CPacket.MoveRelative)) {

                    if (!(packet instanceof PlayerPositionLookS2CPacket) && !isDisconnectPacket(packet)) {
                        if (packet instanceof EntityPositionS2CPacket) {
                            processEntityPacket((EntityPositionS2CPacket) packet);
                        }

                        event.setCancelled(true);
                        queue.add(new Queued(packet, System.currentTimeMillis()));
                    } else {
                        reset();
                    }
                }
            }
        }
    }

    private void processEntityPacket(EntityPositionS2CPacket packet) {
        try {
            int id = tryPacketInt(packet, "getId", "id");
            if (this.target != null && this.target.getId() == id) {
                double dx = tryPacketDouble(packet, "getDeltaX", "deltaX") / 4096.0;
                double dy = tryPacketDouble(packet, "getDeltaY", "deltaY") / 4096.0;
                double dz = tryPacketDouble(packet, "getDeltaZ", "deltaZ") / 4096.0;

                if (!Double.isNaN(dx) && !Double.isNaN(dy) && !Double.isNaN(dz)) {
                    if (this.realPos == null) {
                        this.realPos = this.target.getPos();
                    }
                    this.realPos = this.realPos.add(dx, dy, dz);
                } else {
                    double ax = tryPacketDouble(packet, "getX", "x");
                    double ay = tryPacketDouble(packet, "getY", "y");
                    double az = tryPacketDouble(packet, "getZ", "z");
                    if (!Double.isNaN(ax) && !Double.isNaN(ay) && !Double.isNaN(az)) {
                        this.realPos = new Vec3d(ax, ay, az);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (!mc.isIntegratedServerRunning()) {
            if (!queue.isEmpty() && realPos != null) {
                if (!shouldLag()) {
                    reset();
                } else {
                    processQueue();
                }
            }
        }
    }

    private void processQueue() {
        double dist = mc.player.getPos().distanceTo(realPos);
        double factor = dist / range.getValue();
        long now = System.currentTimeMillis();
        Iterator<Queued> it = queue.iterator();

        while (it.hasNext()) {
            Queued q = it.next();
            if (q.timestamp + (long)(delayMs.getValue() * Math.max(0.5, factor)) > now) {
                break;
            }
            it.remove();
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent event) {
        if (realPos != null && !mc.isIntegratedServerRunning() && target instanceof LivingEntity) {
            renderTargetBox(event);
        }
    }

    private void renderTargetBox(WorldRenderEvent event) {
        if (interpRealPos == null || realPos.squaredDistanceTo(interpRealPos) >= 4.0) {
            interpRealPos = realPos;
        }

        interpRealPos = MathUtil.interpolate(interpRealPos, realPos);
        double halfWidth = target.getWidth() / 2.0;
        Box box = new Box(
                interpRealPos.x - halfWidth, interpRealPos.y, interpRealPos.z - halfWidth,
                interpRealPos.x + halfWidth, interpRealPos.y + target.getHeight(), interpRealPos.z + halfWidth
        );

        int color = getTargetColor();
        Render3DUtil.drawBox(box, color, 2.0f, true, true, true);
    }

    private int getTargetColor() {
        try {
            if (((LivingEntity) target).hurtTime > 0) {
                return 0xFFFF6666; // Light red when hurt
            }
        } catch (Throwable ignored) {
        }
        return 0xFFFF6666; // Default light red
    }

    private boolean shouldLag() {
        if (target != null && target.isAlive() && !target.isRemoved()) {
            if (realPos == null) {
                return false;
            } else {
                return mc.player.getPos().distanceTo(realPos) <= range.getValue();
            }
        } else {
            return false;
        }
    }

    private void reset() {
        queue.clear();
        target = null;
        realPos = null;
        interpRealPos = null;
    }

    private boolean isDisconnectPacket(Object packet) {
        try {
            return packet.getClass().getSimpleName().toLowerCase().contains("disconnect");
        } catch (Throwable e) {
            return false;
        }
    }

    private double tryPacketDouble(Object packet, String... methodNames) {
        for (String methodName : methodNames) {
            try {
                Object value = packet.getClass().getMethod(methodName).invoke(packet);
                if (value instanceof Number number) {
                    return number.doubleValue();
                }
            } catch (Throwable ignored) {
            }
        }
        return Double.NaN;
    }

    private int tryPacketInt(Object packet, String... methodNames) {
        for (String methodName : methodNames) {
            try {
                Object value = packet.getClass().getMethod(methodName).invoke(packet);
                if (value instanceof Number number) {
                    return number.intValue();
                }
            } catch (Throwable ignored) {
            }
        }

        try {
            for (String fieldName : new String[]{"id", "entityId", "entityID"}) {
                Field field = packet.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(packet);
                if (value instanceof Number number) {
                    return number.intValue();
                }
            }
        } catch (Throwable ignored) {
        }

        return -1;
    }

    private static class Queued {
        final Object packet;
        final long timestamp;

        Queued(Object packet, long timestamp) {
            this.packet = packet;
            this.timestamp = timestamp;
        }
    }
}
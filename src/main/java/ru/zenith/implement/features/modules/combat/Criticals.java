package ru.zenith.implement.features.modules.combat;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.BooleanSetting;
import ru.zenith.api.feature.module.setting.implement.GroupSetting;
import ru.zenith.api.feature.module.setting.implement.ValueSetting;
import ru.zenith.common.util.other.Instance;
import ru.zenith.implement.events.packet.PacketEvent;

import java.util.concurrent.ThreadLocalRandom;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Criticals extends Module {
    public static Criticals getInstance() {
        return Instance.get(Criticals.class);
    }

    BooleanSetting onlyCritical = new BooleanSetting("Only Critical", "Attacks only with critical hits")
            .setValue(false);

    BooleanSetting autoDisable = new BooleanSetting("Auto Disable", "Disables after critical hit")
            .setValue(false);

    ValueSetting delay = new ValueSetting("Delay", "Delay between critical attempts")
            .setValue(100f).range(0f, 500f);

    ValueSetting packetDelay = new ValueSetting("Packet Delay", "Delay between packets")
            .setValue(10f).range(0f, 50f);

    GroupSetting advancedGroup = new GroupSetting("Advanced", "Advanced critical settings")
            .settings(onlyCritical, autoDisable, delay, packetDelay).setValue(false);

    @NonFinal
    long lastCriticalTime = 0L;

    @NonFinal
    long lastPacketTime = 0L;

    public Criticals() {
        super("Criticals", ModuleCategory.COMBAT);
        setup(advancedGroup);
    }

    @Override
    public void activate() {
        lastCriticalTime = 0L;
        lastPacketTime = 0L;
        super.activate();
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (!state || mc.player == null) return;

        // Только для пакетов атаки
        if (e.getPacket() instanceof net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket &&
                !mc.player.isOnGround()) {

            handleCriticalHit();
        }
    }

    private void handleCriticalHit() {
        if (!canPerformCritical()) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCriticalTime < delay.getValue()) return;

        // Задержка между пакетами
        if (currentTime - lastPacketTime < packetDelay.getValue()) {
            return;
        }

        performCriticalAttack();
        lastCriticalTime = currentTime;
        lastPacketTime = currentTime;

        if (autoDisable.isValue()) {
            setState(false);
        }
    }

    private boolean canPerformCritical() {
        return mc.player != null &&
                !mc.player.isSubmergedInWater() &&
                !mc.player.isClimbing() &&
                !mc.player.isRiding() &&
                mc.player.getAttackCooldownProgress(0.5f) >= 1.0f &&
                mc.world != null;
    }

    private void performCriticalAttack() {
        if (mc.getNetworkHandler() == null || mc.player == null) return;

        // Только один пакет вместо нескольких
        double offsetY = ThreadLocalRandom.current().nextDouble(0.001, 0.002);

        sendCriticalPosition(offsetY, false);
    }

    private void sendCriticalPosition(double offsetY, boolean onGround) {
        if (mc.getNetworkHandler() != null && mc.player != null) {
            PlayerMoveC2SPacket.PositionAndOnGround packet = new PlayerMoveC2SPacket.PositionAndOnGround(
                    mc.player.getX(),
                    mc.player.getY() + offsetY,
                    mc.player.getZ(),
                    onGround,
                    false
            );
            mc.getNetworkHandler().sendPacket(packet);
        }
    }

    public boolean shouldOnlyCritical() {
        return state && onlyCritical.isValue();
    }
}
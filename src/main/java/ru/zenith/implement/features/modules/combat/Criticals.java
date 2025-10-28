package ru.zenith.implement.features.modules.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.SelectSetting;
import ru.zenith.implement.events.packet.PacketEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Criticals extends Module {

    // Статическая ссылка на экземпляр
    private static Criticals instance;

    final SelectSetting mode = new SelectSetting("Mode", "Criticals mode");

    public static boolean cancelCrit;

    public Criticals() {
        super("Criticals", "Criticals", ModuleCategory.COMBAT);

        // Добавляем значения отдельно в конструкторе
        mode.value("NCP")
                .value("UpdatedNCP")
                .value("Strict")
                .value("Grim")
                .value("OldNCP");

        setup(mode);
        instance = this;
    }

    // Статический метод для доступа из других модулей
    public static Criticals getInstance() {
        return instance;
    }

    @EventHandler
    public void onPacketSend(PacketEvent event) {
        if (!event.isSend() || !(event.getPacket() instanceof PlayerInteractEntityC2SPacket)) return;

        PlayerInteractEntityC2SPacket packet = (PlayerInteractEntityC2SPacket) event.getPacket();

        if (isAttackPacket(packet)) {
            Entity ent = getEntityFromPacket(packet);
            if (ent == null || ent instanceof EndCrystalEntity || cancelCrit)
                return;
            doCrit();
        }
    }

    private boolean isAttackPacket(PlayerInteractEntityC2SPacket packet) {
        try {
            return packet.getClass().getSimpleName().contains("Attack");
        } catch (Exception e) {
            return true;
        }
    }

    private Entity getEntityFromPacket(PlayerInteractEntityC2SPacket packet) {
        try {
            var field = packet.getClass().getDeclaredField("entityId");
            field.setAccessible(true);
            int entityId = (int) field.get(packet);
            return mc.world.getEntityById(entityId);
        } catch (Exception e) {
            return null;
        }
    }

    public void doCrit() {
        if (!isState() || mc.player == null || mc.world == null)
            return;

        if ((mc.player.isOnGround() || mc.player.getAbilities().flying || mode.isSelected("Grim")) &&
                !mc.player.isInLava() && !mc.player.isSubmergedInWater()) {

            String modeValue = mode.getSelected();
            switch (modeValue) {
                case "NCP":
                    critPacket(0.0625D);
                    critPacket(0.0);
                    break;
                case "UpdatedNCP":
                    critPacket(0.000000271875);
                    critPacket(0.0);
                    break;
                case "Strict":
                    critPacket(0.062600301692775);
                    critPacket(0.07260029960661);
                    critPacket(0.0);
                    critPacket(0.0);
                    break;
                case "Grim":
                    if (!mc.player.isOnGround())
                        critPacket(-0.000001);
                    break;
                case "OldNCP":
                    critPacket(0.00001058293536);
                    critPacket(0.00000916580235);
                    critPacket(0.00000010371854);
                    break;
            }
        }
    }

    private void critPacket(double yDelta) {
        if (mc.player == null) return;

        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                mc.player.getX(),
                mc.player.getY() + yDelta,
                mc.player.getZ(),
                false,
                false
        ));
    }
}
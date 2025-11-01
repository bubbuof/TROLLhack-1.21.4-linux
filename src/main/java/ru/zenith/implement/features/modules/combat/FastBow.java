package ru.zenith.implement.features.modules.combat;

import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.ValueSetting;
import ru.zenith.implement.events.player.TickEvent;

public class FastBow extends Module {

    private final ValueSetting delay = new ValueSetting("Задержка", "Задержка перед выстрелом из лука")
            .setValue(10f).range(3f, 10f);

    public FastBow() {
        super("FastBow", "FastBow", ModuleCategory.COMBAT);
        setup(delay);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        boolean hasBowInMainHand = mc.player.getMainHandStack().getItem() == Items.BOW;
        boolean hasBowInOffHand = mc.player.getOffHandStack().getItem() == Items.BOW;

        if ((hasBowInMainHand || hasBowInOffHand) && mc.player.isUsingItem()) {
            if (mc.player.getItemUseTime() >= (int) delay.getValue()) {
                if (mc.getNetworkHandler() != null) {
                    // Отправляем пакет отпускания лука
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                            PlayerActionC2SPacket.Action.RELEASE_USE_ITEM,
                            BlockPos.ORIGIN,
                            mc.player.getHorizontalFacing()
                    ));
                }

                // Определяем руку с луком
                Hand hand = hasBowInOffHand ? Hand.OFF_HAND : Hand.MAIN_HAND;

                if (mc.getNetworkHandler() != null) {
                    // Отправляем пакет взаимодействия с предметом
                    int worldActionId = getWorldActionId();
                    mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(
                            hand,
                            worldActionId,
                            mc.player.getYaw(),
                            mc.player.getPitch()
                    ));
                }

                // Останавливаем использование предмета
                mc.player.stopUsingItem();
            }
        }
    }

    private int getWorldActionId() {
        return mc.world != null ? (int) mc.world.getTime() : 0;
    }
}
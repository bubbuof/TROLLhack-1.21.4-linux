package ru.zenith.implement.features.modules.movement;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Blocks;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.Hand;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.MultiSelectSetting;
import ru.zenith.api.feature.module.setting.implement.SelectSetting;
import ru.zenith.common.util.other.Instance;
import ru.zenith.common.util.other.StopWatch;
import ru.zenith.common.util.task.scripts.Script;
import ru.zenith.implement.events.player.TickEvent;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoSlow extends Module {
    public static NoSlow getInstance() {
        return Instance.get(NoSlow.class);
    }

    private final StopWatch notifWatch = new StopWatch();
    private final Script script = new Script();
    private boolean finish;
    private boolean wasUsingItem = false;

    public final MultiSelectSetting slowTypeSetting = new MultiSelectSetting("Target Type", "Фильтрует типы замедления")
            .value("Using Item", "Web");
    public final SelectSetting itemMode = new SelectSetting("Item Mode", "Режим обхода для предметов")
            .value("Grim New", "Grim Old")
            .visible(() -> slowTypeSetting.isSelected("Using Item"));
    public final SelectSetting webMode = new SelectSetting("Web Mode", "Режим обхода для паутины")
            .value("Grim")
            .visible(() -> slowTypeSetting.isSelected("Web"));

    public NoSlow() {
        super("NoSlow", "NoSlow", ModuleCategory.MOVEMENT);
        setup(slowTypeSetting, itemMode, webMode);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        // Обход замедления в паутине
        if (slowTypeSetting.isSelected("Web") && isPlayerInBlock(Blocks.COBWEB)) {
            double[] speed = calculateDirection(0.64);
            mc.player.addVelocity(speed[0], 0, speed[1]);
            mc.player.setVelocity(mc.player.getVelocity().x,
                    mc.options.jumpKey.isPressed() ? 1.2 : mc.options.sneakKey.isPressed() ? -2 : 0,
                    mc.player.getVelocity().z);
        }

        // Обход замедления от использования предметов
        if (slowTypeSetting.isSelected("Using Item") && mc.player.isUsingItem()) {
            handleItemNoSlow();
        }

        if (script.isFinished() && hasPlayerMovement()) {
            script.update();
        }

        wasUsingItem = mc.player.isUsingItem();
    }

    private void handleItemNoSlow() {
        Hand first = mc.player.getActiveHand();
        Hand second = first.equals(Hand.MAIN_HAND) ? Hand.OFF_HAND : Hand.MAIN_HAND;

        switch (itemMode.getSelected()) {
            case "Grim Old" -> {
                if (mc.player.getOffHandStack().getUseAction().equals(UseAction.NONE) ||
                        mc.player.getMainHandStack().getUseAction().equals(UseAction.NONE)) {
                    interactItem(first);
                    interactItem(second);
                }
            }
            case "Grim New" -> {
                if (mc.player.getItemUseTime() < 7) {
                    updateSlots();
                    closeScreen(true);
                }
            }
            case "FunTime" -> {
                if (finish) {
                    // Логика для FunTime режима
                }
            }
        }
    }

    // Вспомогательные методы
    private boolean isPlayerInBlock(net.minecraft.block.Block block) {
        return mc.world.getBlockState(mc.player.getBlockPos()).getBlock() == block;
    }

    private double[] calculateDirection(double speed) {
        float yaw = (float) Math.toRadians(mc.player.getYaw());
        return new double[]{
                -Math.sin(yaw) * speed,
                Math.cos(yaw) * speed
        };
    }

    private boolean hasPlayerMovement() {
        return mc.player.forwardSpeed != 0 || mc.player.sidewaysSpeed != 0;
    }

    private void interactItem(Hand hand) {
        // Используем стандартный метод взаимодействия
        if (mc.interactionManager != null) {
            mc.interactionManager.interactItem(mc.player, hand);
        }
    }

    private void updateSlots() {
        // Отправляем пакет обновления слотов
        if (mc.getNetworkHandler() != null) {
            // mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
        }
    }

    private void closeScreen(boolean sendPacket) {
        // Закрываем экран если открыт
        if (mc.currentScreen != null) {
            mc.player.closeHandledScreen();
        }
    }
}
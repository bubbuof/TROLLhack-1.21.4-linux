package ru.zenith.implement.features.modules.movement;

import net.minecraft.util.math.BlockPos;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.BooleanSetting;
import ru.zenith.implement.events.player.TickEvent;

public class Parkour extends Module {

    private final BooleanSetting autoSprint = new BooleanSetting("Auto Sprint", "Автоматический спринт при движении")
            .setValue(true);
    private final BooleanSetting smartJump = new BooleanSetting("Smart Jump", "Умное прыганье через пропасти")
            .setValue(true);

    public Parkour() {
        super("Parkour", "Parkour", ModuleCategory.MOVEMENT);
        setup(autoSprint, smartJump);
    }

    @Override
    public void activate() {
        // Инициализация при включении
    }

    @Override
    public void deactivate() {
        if (mc.player != null) {
            mc.player.setSprinting(false);
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        // Автоматический спринт
        if (autoSprint.isValue() && mc.player.input.movementForward > 0) {
            mc.player.setSprinting(true);
        }

        // Умное прыганье через пропасти
        if (mc.player.isOnGround() && shouldJump()) {
            mc.player.jump();
        }
    }

    private boolean shouldJump() {
        if (!smartJump.isValue()) return true;

        BlockPos posBelow = mc.player.getBlockPos().down();
        BlockPos posFront = mc.player.getBlockPos()
                .offset(mc.player.getHorizontalFacing());
        BlockPos frontBelow = posFront.down();

        // Прыгаем только если: под нами есть блок, а впереди пропасть
        return mc.world.isAir(frontBelow) && !mc.world.isAir(posBelow);
    }
}
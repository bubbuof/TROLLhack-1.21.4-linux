package ru.zenith.implement.features.modules.movement;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.common.util.other.Instance;
import ru.zenith.common.util.world.BlockFinder;

public class HighJump extends Module {

    public static HighJump getInstance() {
        return Instance.get(HighJump.class);
    }

    private boolean wasNearShulker = false;
    private boolean wasOpen = false;

    public HighJump() {
        super("HighJump", "HighJump", ModuleCategory.MOVEMENT);
    }

    @Override
    public void activate() {
        wasNearShulker = false;
        wasOpen = false;
    }

    @Override
    public void deactivate() {
        wasNearShulker = false;
        wasOpen = false;
    }

    // Используем систему ивентов Zenith
    public void onEvent(Object event) {
        // Используй правильное событие обновления из твоей системы
        // Например: if (event instanceof TickEvent)
        if (event instanceof Object) { // временная заглушка - замени на нужное событие
            if (mc.world == null || mc.player == null)
                return;

            boolean isNearShulker = false;
            boolean isOpen = false;

            // Получаем BlockEntities через BlockFinder
            for (BlockEntity tile : BlockFinder.INSTANCE.blockEntities) {
                if (tile instanceof ShulkerBoxBlockEntity shulker) {
                    double dx = mc.player.getX() - (tile.getPos().getX() + 0.5);
                    double dz = mc.player.getZ() - (tile.getPos().getZ() + 0.5);
                    double dy = Math.abs(mc.player.getY() - tile.getPos().getY());

                    if (Math.sqrt(dx * dx + dz * dz) <= 1.5 && dy <= 2.5) {
                        // Проверяем анимацию открытия шалкера
                        try {
                            float progress = shulker.getAnimationProgress(1.0f);
                            if (progress > 0.0f && progress < 1.0f) {
                                isOpen = true;
                            }
                        } catch (Exception e) {
                            // Если метод не существует, считаем что шалкер открывается
                            isOpen = true;
                        }

                        isNearShulker = true;
                        break;
                    }
                }
            }

            if (wasNearShulker && wasOpen && !isNearShulker && mc.player.isOnGround()) {
                applyLongJumpBoost();
            }

            wasNearShulker = isNearShulker;
            wasOpen = isOpen;
        }
    }

    private void applyLongJumpBoost() {
        if (mc.player == null) return;

        float yaw = (float) Math.toRadians(mc.player.getYaw());

        double motionX = -Math.sin(yaw) * 1.8;
        double motionZ = Math.cos(yaw) * 1.8;

        mc.player.setVelocity(
                motionX,
                0.8,
                motionZ
        );
    }
}
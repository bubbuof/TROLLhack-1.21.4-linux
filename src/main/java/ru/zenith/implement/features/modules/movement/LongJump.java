package ru.zenith.implement.features.modules.movement;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.SelectSetting;
import ru.zenith.implement.events.player.MoveEvent;
import ru.zenith.implement.events.player.TickEvent;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class LongJump extends Module {

    SelectSetting mode = new SelectSetting("Mode", "Select long jump mode")
            .value("Slap", "FlagBoost", "InstantLong");

    boolean placed;
    int counter;
    long slapTimer;
    long flagTimer;

    public LongJump() {
        super("LongJump", "Long Jump", ModuleCategory.MOVEMENT);
        setup(mode);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (mode.isSelected("Slap") && !mc.player.isWet()) {
            int slot = getSlotWithSlabs();
            if (slot == -1) {
                // Выводим сообщение в чат вместо print
                mc.player.sendMessage(Text.literal("§cYou don't have slabs in hotbar!"), false);
                setState(false); // Выключаем модуль вместо toggle
                return;
            }

            int old = mc.player.getInventory().selectedSlot;

            if (isMoving() && mc.crosshairTarget instanceof BlockHitResult blockResult) {
                if (mc.player.fallDistance >= 0.8 &&
                        mc.world.getBlockState(mc.player.getBlockPos()).isAir() &&
                        !mc.world.getBlockState(blockResult.getBlockPos()).isAir() &&
                        mc.world.getBlockState(blockResult.getBlockPos()).isSolid() &&
                        !(mc.world.getBlockState(blockResult.getBlockPos()).getBlock() instanceof SlabBlock) &&
                        !(mc.world.getBlockState(blockResult.getBlockPos()).getBlock() instanceof StairsBlock)) {

                    mc.player.getInventory().selectedSlot = slot;
                    placed = true;
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockResult);
                    mc.player.getInventory().selectedSlot = old;
                    mc.player.fallDistance = 0;
                }

                mc.options.jumpKey.setPressed(false);

                if ((mc.player.isOnGround() && !mc.options.jumpKey.isPressed()) && placed &&
                        System.currentTimeMillis() - slapTimer >= 750) {

                    placed = false;
                    slapTimer = System.currentTimeMillis();
                } else if ((mc.player.isOnGround() && !mc.options.jumpKey.isPressed())) {
                    mc.player.jump();
                    placed = false;
                }
            } else {
                if ((mc.player.isOnGround() && !mc.options.jumpKey.isPressed())) {
                    mc.player.jump();
                    placed = false;
                }
            }
        }

        if (mode.isSelected("FlagBoost")) {
            if (mc.player.getVelocity().y != -0.0784000015258789) {
                flagTimer = System.currentTimeMillis();
            }

            if (!isMoving()) {
                flagTimer += 50;
            }

            if (System.currentTimeMillis() - flagTimer >= 100 && isMoving()) {
                flagHop();
                mc.player.setVelocity(mc.player.getVelocity().x, 1.0, mc.player.getVelocity().z);
            }
        }

        if (mode.isSelected("InstantLong") && mc.player.hurtTime == 7) {
            setSpeed(6.603774070739746);
            mc.player.setVelocity(mc.player.getVelocity().x, 0.42, mc.player.getVelocity().z);
        }
    }

    @EventHandler
    public void onMoving(MoveEvent event) {
        // Можно добавить изменение движения здесь при необходимости
    }

    private void flagHop() {
        mc.player.setVelocity(mc.player.getVelocity().x, 0.4229, mc.player.getVelocity().z);
        setSpeed(1.953f);
    }

    // Вспомогательные методы
    private boolean isMoving() {
        return mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0;
    }

    private void setSpeed(double speed) {
        if (!isMoving()) return;

        double yaw = getDirection();
        mc.player.setVelocity(-Math.sin(yaw) * speed, mc.player.getVelocity().y, Math.cos(yaw) * speed);
    }

    private double getDirection() {
        float rotationYaw = mc.player.getYaw();
        if (mc.player.input.movementForward < 0) {
            rotationYaw += 180;
        }
        if (mc.player.input.movementForward != 0 && mc.player.input.movementSideways != 0) {
            rotationYaw += (mc.player.input.movementForward > 0 ? -45 : 45) * (mc.player.input.movementSideways > 0 ? 1 : -1);
        }
        return Math.toRadians(rotationYaw);
    }

    private int getSlotWithSlabs() {
        for (int i = 0; i < 9; i++) {
            var stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof net.minecraft.item.BlockItem blockItem) {
                if (blockItem.getBlock() instanceof SlabBlock) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public void activate() {
        super.activate();
        counter = 0;
        placed = false;
        slapTimer = System.currentTimeMillis();
        flagTimer = System.currentTimeMillis();
    }

    @Override
    public void deactivate() {
        super.deactivate();
        if (mc.player != null) {
            mc.options.jumpKey.setPressed(false);
        }
    }
}
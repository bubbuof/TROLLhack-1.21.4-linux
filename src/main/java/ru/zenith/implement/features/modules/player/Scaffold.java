package ru.zenith.implement.features.modules.player;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.*;
import ru.zenith.common.util.other.Instance;
import ru.zenith.implement.events.player.TickEvent;
import ru.zenith.implement.features.modules.player.scaffold.ScaffoldBlockItemSelection;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Scaffold extends Module {
    public static Scaffold getInstance() {
        return Instance.get(Scaffold.class);
    }

    // Основные настройки
    ValueSetting delay = new ValueSetting("Delay", "Delay between placements")
            .setValue(1).range(0, 5);

    ValueSetting minDist = new ValueSetting("MinDist", "Minimum distance for placement")
            .setValue(0.08f).range(0.0f, 0.25f);

    // Техники
    SelectSetting technique = new SelectSetting("Technique", "Scaffold technique")
            .value("Normal", "GodBridge").selected("GodBridge");

    // GodBridge настройки
    SelectSetting godBridgeSide = new SelectSetting("GBSide", "GodBridge side")
            .value("Right", "Left").selected("Right");

    ValueSetting godBridgePitch = new ValueSetting("GBPitch", "GodBridge pitch")
            .setValue(81.3f).range(78.0f, 84.0f);

    ValueSetting godBridgeAngle = new ValueSetting("GBAngle", "GodBridge angle")
            .setValue(38.5f).range(35.0f, 45.0f);

    // Автоматизация
    BooleanSetting autoBlock = new BooleanSetting("AutoBlock", "Automatically select blocks");

    // Безопасность
    BooleanSetting safeWalk = new BooleanSetting("SafeWalk", "Prevent falling");

    // Визуал
    SelectSetting swingMode = new SelectSetting("Swing", "Swing mode")
            .value("Normal", "Packet", "None").selected("Packet");

    // Состояния
    @NonFinal
    int placementY;

    Random random = new Random();

    public Scaffold() {
        super("Scaffold", ModuleCategory.PLAYER);
        setup(delay, minDist, technique, godBridgeSide, godBridgePitch, godBridgeAngle, autoBlock, safeWalk, swingMode);
    }

    @Override
    public void activate() {
        placementY = mc.player.getBlockPos().getY() - 1;
        super.activate();
    }

    @Override
    public void deactivate() {
        super.deactivate();
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player.isOnGround()) {
            placementY = mc.player.getBlockPos().getY() - 1;
        }

        if (autoBlock.isValue() && !hasBlockInHand()) {
            selectBestBlock();
        }

        handleBlockPlacement();
    }

    private void handleBlockPlacement() {
        if (!canPlaceBlock()) return;

        if ("GodBridge".equals(technique.getSelected())) {
            handleStableGodBridge();
        } else {
            handleNormalPlacement();
        }
    }

    private void handleStableGodBridge() {
        BlockPos targetPos = getTargetedPosition(mc.player.getPos());
        if (targetPos == null) return;

        Hand hand = findSuitableHand();
        if (hand == null) return;

        // Устанавливаем стабильные повороты GodBridge
        setGodBridgeRotations();

        BlockHitResult hitResult = new BlockHitResult(
                new Vec3d(targetPos.getX() + 0.5, targetPos.getY() + 1, targetPos.getZ() + 0.5),
                Direction.UP,
                targetPos,
                false
        );

        if (doPlacement(hitResult, hand)) {
            waitTicks(getDelayTicks());
        }
    }

    private void setGodBridgeRotations() {
        float baseYaw = mc.player.getYaw();
        float pitch = godBridgePitch.getValue();
        float angle = godBridgeAngle.getValue();

        // Определяем сторону
        boolean isRightSide = "Right".equals(godBridgeSide.getSelected());
        float targetYaw = baseYaw + (isRightSide ? -angle : angle);

        // Нормализуем угол
        targetYaw = normalizeYaw(targetYaw);

        // Плавно применяем повороты
        float currentYaw = mc.player.getYaw();
        float yawDiff = targetYaw - currentYaw;

        // Нормализуем разницу
        if (yawDiff > 180) yawDiff -= 360;
        if (yawDiff < -180) yawDiff += 360;

        // Мягкое применение (не более 10 градусов за тик)
        float smoothYaw = currentYaw + yawDiff * 0.3f;

        mc.player.setYaw(smoothYaw);
        mc.player.setPitch(pitch);
    }

    private float normalizeYaw(float yaw) {
        yaw = yaw % 360;
        if (yaw > 180) yaw -= 360;
        if (yaw < -180) yaw += 360;
        return yaw;
    }

    private void handleNormalPlacement() {
        BlockPos targetPos = getTargetedPosition(mc.player.getPos());
        if (targetPos == null) return;

        Hand hand = findSuitableHand();
        if (hand == null) return;

        BlockHitResult hitResult = new BlockHitResult(
                new Vec3d(targetPos.getX() + 0.5, targetPos.getY() + 1, targetPos.getZ() + 0.5),
                Direction.UP,
                targetPos,
                false
        );

        if (doPlacement(hitResult, hand)) {
            waitTicks(getDelayTicks());
        }
    }

    private boolean doPlacement(BlockHitResult hitResult, Hand hand) {
        if (mc.interactionManager == null) return false;

        var result = mc.interactionManager.interactBlock(mc.player, hand, hitResult);
        boolean success = result.isAccepted();

        if (success) {
            swingHand(hand);
        }

        return success;
    }

    private void swingHand(Hand hand) {
        switch (swingMode.getSelected()) {
            case "Normal":
                mc.player.swingHand(hand);
                break;
            case "Packet":
                // Packet swing
                break;
            case "None":
                break;
        }
    }

    private boolean canPlaceBlock() {
        return hasBlockInHand() && isBlockBelow();
    }

    public BlockPos getTargetedPosition(Vec3d playerPos) {
        BlockPos playerBlockPos = new BlockPos(
                (int) Math.floor(playerPos.x),
                (int) Math.floor(playerPos.y),
                (int) Math.floor(playerPos.z)
        );

        return playerBlockPos.down();
    }

    private boolean hasBlockInHand() {
        return isValidBlock(mc.player.getMainHandStack()) || isValidBlock(mc.player.getOffHandStack());
    }

    private boolean isValidBlock(ItemStack stack) {
        return ScaffoldBlockItemSelection.getInstance().isValidBlock(stack);
    }

    private Hand findSuitableHand() {
        if (isValidBlock(mc.player.getMainHandStack())) {
            return Hand.MAIN_HAND;
        }
        if (isValidBlock(mc.player.getOffHandStack())) {
            return Hand.OFF_HAND;
        }
        return null;
    }

    private void selectBestBlock() {
        List<ItemStack> placeableStacks = findPlaceableSlots();
        if (placeableStacks.isEmpty()) return;

        ItemStack bestStack = placeableStacks.stream()
                .max(this::compareBlocks)
                .orElse(null);

        if (bestStack != null) {
            int slot = findSlotWithStack(bestStack);
            if (slot != -1) {
                mc.player.getInventory().selectedSlot = slot;
            }
        }
    }

    private List<ItemStack> findPlaceableSlots() {
        List<ItemStack> stacks = new ArrayList<>();

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (isValidBlock(stack)) {
                stacks.add(stack);
            }
        }

        ItemStack offHand = mc.player.getOffHandStack();
        if (isValidBlock(offHand)) {
            stacks.add(offHand);
        }

        return stacks;
    }

    private int findSlotWithStack(ItemStack stack) {
        for (int i = 0; i < 9; i++) {
            if (ItemStack.areEqual(mc.player.getInventory().getStack(i), stack)) {
                return i;
            }
        }
        return -1;
    }

    private int compareBlocks(ItemStack a, ItemStack b) {
        return Integer.compare(b.getCount(), a.getCount());
    }

    public boolean isBlockBelow() {
        return mc.world.getBlockCollisions(
                mc.player,
                mc.player.getBoundingBox().expand(0.5, 0.0, 0.5).offset(0.0, -1.05, 0.0)
        ).iterator().hasNext();
    }

    private int getDelayTicks() {
        return (int) delay.getValue();
    }

    private void waitTicks(int ticks) {
        try {
            Thread.sleep(ticks * 50L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
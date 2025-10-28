package ru.zenith.implement.features.modules.movement;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.ValueSetting;
import ru.zenith.implement.events.player.TickEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.item.Items;
import net.minecraft.block.Blocks;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Vec2f;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Spider extends Module {

    // Статическая ссылка на экземпляр
    private static Spider instance;

    final ValueSetting motionY = new ValueSetting("Motion Y", "Climbing speed")
            .range(0.1f, 1.0f)
            .setValue(0.42f);

    int placeAttempts = 0;
    long lastPlaceTime = 0;

    public Spider() {
        super("Spider", "Spider", ModuleCategory.MOVEMENT);
        setup(motionY);
        instance = this;
    }

    public static Spider getInstance() {
        return instance;
    }

    @Override
    public void activate() {
        super.activate();
        placeAttempts = 0;
        lastPlaceTime = 0;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        handleGrimBypass();
    }

    private void handleGrimBypass() {
        if (!mc.player.horizontalCollision) return;

        boolean nearSlime = isNearSlimeBlock();
        double climbSpeed = nearSlime ? 0.45 : motionY.getValue();

        Vec3d motion = mc.player.getVelocity();
        mc.player.setVelocity(motion.x, climbSpeed, motion.z);

        if (System.currentTimeMillis() - lastPlaceTime > 50) {
            placeSlimeBlock();
            lastPlaceTime = System.currentTimeMillis();
        }
    }

    private boolean isNearSlimeBlock() {
        BlockPos playerPos = mc.player.getBlockPos();
        for (int x = -2; x <= 2; x++) {
            for (int y = -1; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    BlockPos checkPos = playerPos.add(x, y, z);
                    if (mc.world.getBlockState(checkPos).getBlock() == Blocks.SLIME_BLOCK) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void placeSlimeBlock() {
        int slotSlime = getSlotForSlime();
        if (slotSlime == -1) {
            return;
        }

        int lastSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = slotSlime;

        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos targetPos = getOptimalSlimePosition(playerPos);

        if (targetPos != null && mc.world.getBlockState(targetPos).isAir()) {
            Vec2f rotation = calculateRotationToBlock(targetPos);
            applyRotation(rotation);

            placeBlock(targetPos);
            placeAttempts++;
        }

        mc.player.getInventory().selectedSlot = lastSlot;
    }

    private BlockPos getOptimalSlimePosition(BlockPos playerPos) {
        Vec3d lookVec = mc.player.getRotationVec(1.0F);
        int offsetX = lookVec.x > 0.5 ? 1 : (lookVec.x < -0.5 ? -1 : 0);
        int offsetZ = lookVec.z > 0.5 ? 1 : (lookVec.z < -0.5 ? -1 : 0);

        if (offsetX == 0 && offsetZ == 0) {
            offsetX = lookVec.x > 0 ? 1 : -1;
        }

        for (int yOffset = 0; yOffset <= 2; yOffset++) {
            BlockPos checkPos = playerPos.add(offsetX, yOffset, offsetZ);
            if (mc.world.getBlockState(checkPos).isAir()) {
                BlockPos below = checkPos.down();
                if (!mc.world.getBlockState(below).isAir() || yOffset == 0) {
                    return checkPos;
                }
            }
        }

        return playerPos.add(offsetX, 1, offsetZ);
    }

    private Vec2f calculateRotationToBlock(BlockPos pos) {
        Vec3d targetPosition = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        Vec3d playerEyes = mc.player.getEyePos();
        Vec3d toTarget = targetPosition.subtract(playerEyes).normalize();

        double horizontalLength = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
        float rawYaw = (float) Math.toDegrees(Math.atan2(toTarget.z, toTarget.x)) - 90.0F;
        float rawPitch = 76.0F;

        long time = System.currentTimeMillis();
        float microTime = (time % 1000) / 1000.0f;
        double microPhase = Math.sin(microTime * Math.PI * 2) * 0.15;

        float deviationYaw = (float) (microPhase * 0.3);
        float deviationPitch = (float) (microPhase * 0.1);

        float finalYaw = rawYaw + deviationYaw;
        float finalPitch = Math.clamp(rawPitch + deviationPitch, 75.0F, 77.0F);

        return new Vec2f(finalYaw, finalPitch);
    }

    private void applyRotation(Vec2f rotation) {
        mc.player.setYaw(rotation.x);
        mc.player.setPitch(rotation.y);
    }

    private void placeBlock(BlockPos pos) {
        if (!mc.world.getBlockState(pos).isAir()) {
            return;
        }

        Direction[] priorities = {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP};

        for (Direction direction : priorities) {
            BlockPos neighbor = pos.offset(direction);
            if (!mc.world.getBlockState(neighbor).isAir()) {
                Vec3d hitVec = new Vec3d(
                        neighbor.getX() + 0.5 + direction.getOpposite().getOffsetX() * 0.5,
                        neighbor.getY() + 0.5 + direction.getOpposite().getOffsetY() * 0.5,
                        neighbor.getZ() + 0.5 + direction.getOpposite().getOffsetZ() * 0.5
                );

                BlockHitResult result = new BlockHitResult(hitVec, direction.getOpposite(), neighbor, false);

                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, result);
                mc.player.swingHand(Hand.MAIN_HAND);
                return;
            }
        }
    }

    private int getSlotForSlime() {
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.SLIME_BLOCK) {
                return i;
            }
        }
        return -1;
    }
}
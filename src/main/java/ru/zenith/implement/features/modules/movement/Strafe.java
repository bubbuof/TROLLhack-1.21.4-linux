package ru.zenith.implement.features.modules.movement;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.BooleanSetting;
import ru.zenith.api.feature.module.setting.implement.ValueSetting;
import ru.zenith.common.util.entity.MovingUtil;
import ru.zenith.implement.events.player.MoveEvent;
import ru.zenith.implement.events.player.TickEvent;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Strafe extends Module {

    final ValueSetting baseSpeedSetting = new ValueSetting("Base Speed", "Base movement speed multiplier")
            .setValue(0.36f).range(0.1f, 1.0f);
    final ValueSetting speedBoostSetting = new ValueSetting("Speed Boost", "Speed boost multiplier for Speed II effect")
            .setValue(1.155f).range(1.0f, 2.0f);
    final ValueSetting melonBoostSetting = new ValueSetting("Melon Boost", "Speed boost when holding melon slice")
            .setValue(0.41755f).range(0.1f, 1.0f);
    final ValueSetting airMultiplierSetting = new ValueSetting("Air Multiplier", "Speed multiplier when in air")
            .setValue(1.435f).range(1.0f, 3.0f);
    final ValueSetting slownessReductionSetting = new ValueSetting("Slowness Reduction", "Speed reduction when having slowness effect")
            .setValue(0.835f).range(0.1f, 1.0f);
    final ValueSetting damageSpeedSetting = new ValueSetting("Damage Speed", "Speed boost when taking damage")
            .setValue(0.5f).range(0.1f, 2.0f);
    
    final BooleanSetting autoJumpSetting = new BooleanSetting("Auto Jump", "Automatically jump for better strafe")
            .setValue(true);
    final BooleanSetting damageBoostSetting = new BooleanSetting("Damage Boost", "Enable damage boost")
            .setValue(true);

    // Strafe utility variables (non-final for modification)
    double oldSpeed = 0;
    double contextFriction = 0;
    boolean needSwap = false;
    boolean NDS = false;
    int counter = 0;
    int noSlowTicks = 0;

    public Strafe() {
        super("Strafe", "Strafe", ModuleCategory.MOVEMENT);
        setup(baseSpeedSetting, speedBoostSetting, melonBoostSetting, airMultiplierSetting, 
              slownessReductionSetting, damageSpeedSetting, autoJumpSetting, damageBoostSetting);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        ItemStack offHandItem = mc.player.getOffHandStack();
        StatusEffectInstance speedEffect = mc.player.getStatusEffect(StatusEffects.SPEED);
        StatusEffectInstance slownessEffect = mc.player.getStatusEffect(StatusEffects.SLOWNESS);
        String itemName = offHandItem.getName().getString();

        float appliedSpeed = 0;
        float baseSpeed = baseSpeedSetting.getValue();

        if (speedEffect != null) {
            if (speedEffect.getAmplifier() == 2) {
                appliedSpeed = baseSpeed * speedBoostSetting.getValue();
                if (itemName.contains("Ломтик Дыни") || itemName.contains("Melon Slice")) {
                    appliedSpeed = melonBoostSetting.getValue();
                }
            } else if (speedEffect.getAmplifier() == 1) {
                appliedSpeed = baseSpeed;
            }
        } else {
            appliedSpeed = baseSpeed * 0.68f;
        }

        if (slownessEffect != null) {
            appliedSpeed *= slownessReductionSetting.getValue();
        }

        if (!mc.player.isOnGround()) {
            appliedSpeed *= airMultiplierSetting.getValue();
        }

        // Only apply speed if player is moving
        if (MovingUtil.hasPlayerMovement()) {
            MovingUtil.setVelocity(appliedSpeed);
        }
    }

    @EventHandler
    public void onMove(MoveEvent event) {
        if (mc.player == null || mc.world == null) return;

        Vec3d movement = event.getMovement();
        double horizontal = Math.sqrt(movement.x * movement.x + movement.z * movement.z);
        
        // Calculate strafe speed using advanced algorithm
        double strafeSpeed = calcStrafeSpeed(movement, damageBoostSetting.isValue(), 
                                           true, autoJumpSetting.isValue(), damageSpeedSetting.getValue());
        
        if (MovingUtil.hasPlayerMovement() && strafeSpeed > 0) {
            double[] direction = MovingUtil.calculateDirection(strafeSpeed);
            event.setMovement(new Vec3d(direction[0], movement.y, direction[1]));
        }

        // Post-move processing
        postMove(horizontal);
    }

    private double calcStrafeSpeed(Vec3d movement, boolean damageBoost, boolean hasTime, boolean autoJump, float damageSpeed) {
        boolean isOnGround = mc.player.isOnGround();
        boolean isJumping = movement.y > 0;
        float moveSpeed = getAIMoveSpeed();
        float friction = getFrictionFactor(movement);

        float adjustedFriction = mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST) && mc.player.isUsingItem() ? 0.88f : 0.9103f;
        if (isOnGround) {
            adjustedFriction = friction;
        }

        float frictionAdjustment = 0.16277136f / (adjustedFriction * adjustedFriction * adjustedFriction);
        float calculatedSpeed;

        if (isOnGround) {
            calculatedSpeed = moveSpeed * frictionAdjustment;
            if (isJumping) {
                calculatedSpeed += 0.2f;
            }
        } else {
            calculatedSpeed = (damageBoost && hasTime && (autoJump || mc.options.jumpKey.isPressed())) ? damageSpeed : 0.0255f;
        }

        boolean isSlowed = false;
        double maxSpeed = oldSpeed + calculatedSpeed;
        double currentSpeed = 0.0;

        if (mc.player.isUsingItem() && !isJumping) {
            double adjustedSpeed = oldSpeed + calculatedSpeed * 0.25;
            double verticalMotion = movement.y;
            if (verticalMotion != 0.0 && Math.abs(verticalMotion) < 0.08) {
                adjustedSpeed += 0.055;
            }
            if (maxSpeed > (currentSpeed = Math.max(0.043, adjustedSpeed))) {
                isSlowed = true;
                ++noSlowTicks;
            } else {
                noSlowTicks = Math.max(noSlowTicks - 1, 0);
            }
        } else {
            noSlowTicks = 0;
        }

        if (noSlowTicks > 3) {
            maxSpeed = currentSpeed - (mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST) && mc.player.isUsingItem() ? 0.3 : 0.019);
        } else {
            maxSpeed = Math.max(isSlowed ? 0 : 0.249984 - (++counter % 2) * 0.0001D, maxSpeed);
        }

        contextFriction = adjustedFriction;

        if (!mc.player.isOnGround()) {
            needSwap = true;
        }
        if (!mc.player.isOnGround()) {
            NDS = !mc.player.isSprinting();
        }
        if (mc.player.isOnGround()) {
            NDS = false;
        }

        return maxSpeed;
    }

    private void postMove(double horizontal) {
        oldSpeed = horizontal * contextFriction;
    }

    private float getAIMoveSpeed() {
        boolean prevSprinting = mc.player.isSprinting();
        mc.player.setSprinting(false);
        float speed = mc.player.getMovementSpeed() * 1.3f;
        mc.player.setSprinting(prevSprinting);
        return speed;
    }

    private float getFrictionFactor(Vec3d movement) {
        BlockPos.Mutable pos = new BlockPos.Mutable();
        pos.set(mc.player.getX(), mc.player.getBoundingBox().minY - 1.0D, mc.player.getZ());
        return mc.world.getBlockState(pos).getBlock().getSlipperiness() * 0.91F;
    }

    @Override
    public void deactivate() {
        // Reset strafe variables when module is disabled
        oldSpeed = 0;
        contextFriction = 0;
        needSwap = false;
        NDS = false;
        counter = 0;
        noSlowTicks = 0;
        super.deactivate();
    }
}

package ru.zenith.implement.features.modules.misc;

import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.BooleanSetting;
import ru.zenith.api.feature.module.setting.implement.SelectSetting;
import ru.zenith.api.feature.module.setting.implement.ValueSetting;
import ru.zenith.common.util.entity.MovingUtil;
import ru.zenith.common.util.other.Instance;
import ru.zenith.core.listener.impl.EventListener;
import ru.zenith.implement.events.packet.PacketEvent;
import ru.zenith.implement.events.player.MotionEvent;
import ru.zenith.implement.events.player.PlayerVelocityStrafeEvent;
import ru.zenith.implement.events.player.TickEvent;
import ru.zenith.implement.features.modules.combat.Aura;

public class LegitTargetStrafe extends Module {
    
    // Settings
    private final BooleanSetting jump = new BooleanSetting("Jump", "Auto jump when strafing")
            .setValue(true);
    private final ValueSetting distance = new ValueSetting("Distance", "Strafe distance from target")
            .setValue(1.3f).range(0.2f, 7.0f);
    private final SelectSetting boost = new SelectSetting("Boost", "Speed boost mode")
            .value("None", "Elytra", "Damage").selected("None");
    private final ValueSetting setSpeed = new ValueSetting("Speed", "Elytra boost speed")
            .setValue(1.3f).range(0.0f, 2.0f).visible(() -> boost.isSelected("Elytra"));
    private final ValueSetting velReduction = new ValueSetting("Reduction", "Damage velocity reduction")
            .setValue(6.0f).range(0.1f, 10.0f).visible(() -> boost.isSelected("Damage"));
    private final ValueSetting maxVelocitySpeed = new ValueSetting("MaxVelocity", "Maximum velocity from damage")
            .setValue(0.8f).range(0.1f, 2.0f).visible(() -> boost.isSelected("Damage"));
    private final BooleanSetting aggressive = new BooleanSetting("Aggressive", "More aggressive target following")
            .setValue(true);
    private final ValueSetting followSpeed = new ValueSetting("FollowSpeed", "Speed multiplier when following distant targets")
            .setValue(1.5f).range(1.0f, 3.0f);
    
    // State variables
    private double oldSpeed, contextFriction;
    private boolean needSwap, needSprintState, switchDir, disabled;
    private int noSlowTicks, jumpTicks, waterTicks;
    private long disableTime;
    private static LegitTargetStrafe instance;

    public LegitTargetStrafe() {
        super("LegitTargetStrafe", "Legit Target Strafe", ModuleCategory.MISC);
        setup(jump, distance, boost, setSpeed, velReduction, maxVelocitySpeed, aggressive, followSpeed);
        instance = this;
    }

    public static LegitTargetStrafe getInstance() {
        return instance;
    }

    @Override
    public void activate() {
        super.activate();
        oldSpeed = 0;
    }

    @Override
    public void deactivate() {
        super.deactivate();
        oldSpeed = 0;
    }

    public boolean canStrafe() {
        // Simplified check - only prevent if flying
        if (mc.player == null) return false;
        return !mc.player.getAbilities().flying;
    }

    public boolean needToSwitch(double x, double z) {
        if (mc.player.horizontalCollision || ((mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed()) && jumpTicks <= 0)) {
            jumpTicks = 10;
            return true;
        }
        for (int i = (int) (mc.player.getY() + 4); i >= 0; --i) {
            BlockPos playerPos = new BlockPos((int) Math.floor(x), (int) Math.floor(i), (int) Math.floor(z));
            if (mc.world.getBlockState(playerPos).getBlock().equals(Blocks.LAVA) ||
                mc.world.getBlockState(playerPos).getBlock().equals(Blocks.FIRE)) {
                return true;
            }
            if (mc.world.isAir(playerPos)) continue;
            return false;
        }
        return false;
    }

    public double calculateSpeed(MotionEvent move) {
        jumpTicks--;
        float speedAttributes = getAIMoveSpeed();
        final float frictionFactor = mc.world.getBlockState(new BlockPos.Mutable().set(mc.player.getX(), getBoundingBox().getMin(Direction.Axis.Y) - move.getY(), mc.player.getZ())).getBlock().getSlipperiness() * 0.91F;
        float n6 = mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST) && mc.player.isUsingItem() ? 0.88f : (float) (oldSpeed > 0.32 && mc.player.isUsingItem() ? 0.88 : 0.91F);
        if (mc.player.isOnGround()) {
            contextFriction = frictionFactor;
            double max2 = getMaxSpeed();
            if (oldSpeed <= max2) oldSpeed = max2;
        }
        if (mc.player.isOnGround()) {
            contextFriction = frictionFactor * n6;
        }
        boolean noslow = false;
        if (mc.player.isUsingItem() && !mc.player.hasVehicle()) {
            noslow = true;
            noSlowTicks = 1;
        } else if (noSlowTicks > 0) {
            noslow = true;
            noSlowTicks--;
        }

        double max2 = oldSpeed - oldSpeed / 159.9999985;
        if (mc.player.isUsingItem() && move.getY() <= 0) {
            max2 = oldSpeed - oldSpeed / (noslow ? 159.9999985 : 159.9999985);
        }

        double n8 = 0.026;
        double n9 = move.getY();
        if (n9 > 0.0) {
            n8 = 0.026 + (n9 - 0.0) * 0.54;
        }

        if (mc.player.isOnGround() && n9 < 0.0) {
            n8 = 0.026;
        }
        max2 = Math.max(max2, n8);
        if (noslow && mc.player.isOnGround()) max2 = Math.max(0.026, max2);
        else max2 = Math.max(noslow ? 0 : 0.025, max2) - (mc.player.age % 2 == 0 ? 0.001 : 0.002);

        contextFriction = (float) max2;
        if (!mc.player.isOnGround()) {
            needSprintState = !mc.player.lastSprinting;
        }
        return max2;
    }

    private Box getBoundingBox() {
        return new Box(mc.player.getX() - 0.1, mc.player.getY(), mc.player.getZ() - 0.1, mc.player.getX() + 0.1, mc.player.getY() + 1, mc.player.getZ() + 0.1);
    }

    private float getAIMoveSpeed() {
        boolean prevSprinting = mc.player.isSprinting();
        mc.player.setSprinting(false);
        float speed = mc.player.getMovementSpeed() * 1.3f;
        mc.player.setSprinting(prevSprinting);
        return speed;
    }

    private int getElytraSlot() {
        for (int i = 0; i < 45; ++i) {
            if (mc.player.getInventory().getStack(i).getItem().toString().contains("elytra")) {
                return i;
            }
        }
        return -1;
    }

    private void disabler(int elytra) {
        if (mc.player.age % 3 == 0) {
            if (!disabled) {
                mc.interactionManager.clickSlot(0, elytra, 1, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(0, 6, 1, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(0, elytra, 1, SlotActionType.PICKUP, mc.player);
                disabled = true;
                disableTime = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - disableTime >= 150L) {
                mc.interactionManager.clickSlot(0, 6, 1, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(0, elytra, 1, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(0, 6, 1, SlotActionType.PICKUP, mc.player);
                disabled = false;
            }
        }
    }

    private double getMaxSpeed() {
        double n = 0.2873;
        if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            n *= 1.0 + 0.2 * (mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier() + 1);
        }
        if (mc.player.hasStatusEffect(StatusEffects.SLOWNESS)) {
            n *= 1.0 + 0.2 * (mc.player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() + 1);
        }
        return n;
    }

    public double wrapDS(double x, double z) {
        return Math.toDegrees(Math.atan2(z - mc.player.getZ(), x - mc.player.getX()));
    }

    @EventHandler
    public void onMotion(MotionEvent event) {
        int elytraSlot = getElytraSlot();

        if (boost.isSelected("Elytra") && elytraSlot != -1) {
            if (MovingUtil.hasPlayerMovement() && !mc.player.isOnGround() && mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().offset(0.0, event.getY(), 0.0f)).iterator().hasNext() && disabled) {
                oldSpeed = setSpeed.getValue();
            }
        }

        // MotionEvent now only handles speed calculation, movement is in TickEvent
        double speed = calculateSpeed(event);
        oldSpeed = speed;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        oldSpeed = Math.hypot(mc.player.getX() - mc.player.prevX, mc.player.getZ() - mc.player.prevZ) * contextFriction;

        // Main movement logic moved to TickEvent for better reliability
        if (canStrafe()) {
            Aura aura = Instance.get(Aura.class);
            if (aura != null && aura.getTarget() != null && aura.isState()) {
                LivingEntity target = aura.getTarget();
                
                // Auto jump
                if (mc.player.isOnGround() && jump.isValue()) {
                    mc.player.jump();
                }
                
                // Very simple movement towards target
                double deltaX = target.getX() - mc.player.getX();
                double deltaZ = target.getZ() - mc.player.getZ();
                double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
                
                if (distance > 0.5) {
                    // Normalize and apply movement
                    double normalizedX = deltaX / distance;
                    double normalizedZ = deltaZ / distance;
                    double moveSpeed = 0.2;
                    
                    // Direct velocity application
                    mc.player.setVelocity(
                        normalizedX * moveSpeed,
                        mc.player.getVelocity().getY(),
                        normalizedZ * moveSpeed
                    );
                    
                    // Force input
                    mc.player.input.movementForward = 1.0f;
                    mc.player.input.movementSideways = 0.0f;
                    
                    // Also try MovingUtil
                    double yaw = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0;
                    float oldYaw = mc.player.getYaw();
                    mc.player.setYaw((float) yaw);
                    MovingUtil.setVelocity(moveSpeed);
                    mc.player.setYaw(oldYaw);
                }
            }
        }

        if (mc.player.isSubmergedInWater()) {
            waterTicks = 10;
        } else {
            waterTicks--;
        }

        if ((boost.isSelected("Elytra") && getElytraSlot() != -1 && !mc.player.isOnGround() && mc.player.fallDistance > 0 && !disabled)) {
            disabler(getElytraSlot());
        }
    }

    @EventHandler
    public void onPacketReceive(PacketEvent e) {
        if (e.getPacket() instanceof PlayerPositionLookS2CPacket) {
            oldSpeed = 0;
        }
        if (e.getPacket() instanceof EntityVelocityUpdateS2CPacket velocity && velocity.getEntityId() == mc.player.getId() && boost.isSelected("Damage")) {
            if (mc.player.isOnGround()) return;

            double vX = velocity.getVelocityX();
            double vZ = velocity.getVelocityZ();

            if (vX < 0) vX *= -1;
            if (vZ < 0) vZ *= -1;

            oldSpeed = (vX + vZ) / (velReduction.getValue() * 1000f);
            oldSpeed = Math.min(oldSpeed, maxVelocitySpeed.getValue());
        }
    }

    @EventHandler
    public void onPlayerVelocityStrafe(PlayerVelocityStrafeEvent e) {
        if (needSwap) {
            needSwap = false;
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
        }
        if (needSprintState) {
            needSprintState = false;
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
        }
    }
}

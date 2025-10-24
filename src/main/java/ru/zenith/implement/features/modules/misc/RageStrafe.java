package ru.zenith.implement.features.modules.misc;

import net.minecraft.entity.LivingEntity;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.BooleanSetting;
import ru.zenith.api.feature.module.setting.implement.ValueSetting;
import ru.zenith.common.util.entity.MovingUtil;
import ru.zenith.common.util.other.Instance;
import ru.zenith.implement.events.player.TickEvent;
import ru.zenith.implement.features.modules.combat.Aura;

public class RageStrafe extends Module {
    
    // Settings
    private final BooleanSetting jump = new BooleanSetting("Jump", "Auto jump when strafing")
            .setValue(true);
    private final ValueSetting distance = new ValueSetting("Distance", "Strafe distance from target")
            .setValue(2.5f).range(0.5f, 6.0f);
    private final ValueSetting speed = new ValueSetting("Speed", "Movement speed multiplier")
            .setValue(1.8f).range(0.5f, 5.0f);
    private final BooleanSetting workWithSpeed = new BooleanSetting("WorkWithSpeed", "Work together with Speed module")
            .setValue(true);
    private final BooleanSetting aggressive = new BooleanSetting("Aggressive", "More aggressive following")
            .setValue(true);
    private final ValueSetting strafeRadius = new ValueSetting("StrafeRadius", "Radius for strafing around target")
            .setValue(1.5f).range(0.5f, 4.0f);
    
    // State variables
    private boolean switchDir = false;
    private int strafeTimer = 0;
    private static RageStrafe instance;

    public RageStrafe() {
        super("RageStrafe", "Rage Strafe", ModuleCategory.MISC);
        setup(jump, distance, speed, workWithSpeed, aggressive, strafeRadius);
        instance = this;
    }

    public static RageStrafe getInstance() {
        return instance;
    }

    @Override
    public void activate() {
        super.activate();
        switchDir = false;
        strafeTimer = 0;
    }

    @Override
    public void deactivate() {
        super.deactivate();
        // Reset player input when disabling
        if (mc.player != null && mc.player.input != null) {
            mc.player.input.movementForward = 0.0f;
            mc.player.input.movementSideways = 0.0f;
        }
    }

    private boolean canStrafe() {
        if (mc.player == null || mc.world == null) return false;
        return !mc.player.getAbilities().flying;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (!canStrafe()) return;
        
        Aura aura = Instance.get(Aura.class);
        Speed speedModule = Instance.get(Speed.class);
        
        if (aura != null && aura.getTarget() != null && aura.isState()) {
            LivingEntity target = aura.getTarget();
            
            // Auto jump
            if (mc.player.isOnGround() && jump.isValue()) {
                mc.player.jump();
            }
            
            // Calculate distance to target
            double currentDistance = Math.sqrt(mc.player.squaredDistanceTo(target));
            double moveSpeed = speed.getValue();
            
            // If Speed module is active and we want to work with it, increase speed
            if (workWithSpeed.isValue() && speedModule != null && speedModule.isState()) {
                moveSpeed *= 1.5; // Boost speed when Speed module is active
            }
            
            if (currentDistance > distance.getValue()) {
                // Direct movement towards target when far
                double deltaX = target.getX() - mc.player.getX();
                double deltaZ = target.getZ() - mc.player.getZ();
                double targetDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
                
                if (targetDistance > 0) {
                    double normalizedX = deltaX / targetDistance;
                    double normalizedZ = deltaZ / targetDistance;
                    
                    // Apply aggressive movement
                    double finalSpeed = aggressive.isValue() ? moveSpeed * 1.2 : moveSpeed;
                    
                    // Multiple movement methods for maximum effectiveness
                    mc.player.setVelocity(
                        normalizedX * finalSpeed * 0.3,
                        mc.player.getVelocity().getY(),
                        normalizedZ * finalSpeed * 0.3
                    );
                    
                    // Force input movement
                    mc.player.input.movementForward = 1.0f;
                    mc.player.input.movementSideways = 0.0f;
                    
                    // Use MovingUtil for additional speed
                    double yaw = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0;
                    float oldYaw = mc.player.getYaw();
                    mc.player.setYaw((float) yaw);
                    MovingUtil.setVelocity(finalSpeed * 0.2);
                    mc.player.setYaw(oldYaw);
                }
            } else {
                // Strafe around target when close
                strafeTimer++;
                
                // Switch direction every 40 ticks (2 seconds)
                if (strafeTimer > 40) {
                    switchDir = !switchDir;
                    strafeTimer = 0;
                }
                
                // Calculate strafe position
                double angle = Math.atan2(mc.player.getZ() - target.getZ(), mc.player.getX() - target.getX());
                angle += switchDir ? 0.15 : -0.15; // Strafe increment
                
                double strafeX = target.getX() + strafeRadius.getValue() * Math.cos(angle);
                double strafeZ = target.getZ() + strafeRadius.getValue() * Math.sin(angle);
                
                // Move towards strafe position
                double deltaX = strafeX - mc.player.getX();
                double deltaZ = strafeZ - mc.player.getZ();
                double strafeDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
                
                if (strafeDistance > 0.1) {
                    double normalizedX = deltaX / strafeDistance;
                    double normalizedZ = deltaZ / strafeDistance;
                    double strafeSpeed = moveSpeed * 0.8;
                    
                    // Apply strafe movement
                    mc.player.setVelocity(
                        normalizedX * strafeSpeed * 0.25,
                        mc.player.getVelocity().getY(),
                        normalizedZ * strafeSpeed * 0.25
                    );
                    
                    // Force strafe input
                    mc.player.input.movementForward = 0.8f;
                    mc.player.input.movementSideways = switchDir ? 0.6f : -0.6f;
                    
                    // Use MovingUtil for strafe
                    double strafeYaw = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0;
                    float oldYaw = mc.player.getYaw();
                    mc.player.setYaw((float) strafeYaw);
                    MovingUtil.setVelocity(strafeSpeed * 0.15);
                    mc.player.setYaw(oldYaw);
                }
            }
        }
    }
}

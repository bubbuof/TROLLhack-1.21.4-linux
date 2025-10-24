package ru.zenith.implement.features.modules.combat.killaura.rotation.angle;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.zenith.common.util.other.StopWatch;
import ru.zenith.core.Main;
import ru.zenith.implement.features.modules.combat.Aura;
import ru.zenith.implement.features.modules.combat.killaura.attack.AttackHandler;
import ru.zenith.implement.features.modules.combat.killaura.rotation.Angle;
import ru.zenith.implement.features.modules.combat.killaura.rotation.AngleUtil;

import java.security.SecureRandom;

public class FunTimeSmoothMode extends AngleSmoothMode {
    public FunTimeSmoothMode() {
        super("FunTime");
    }
    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
        AttackHandler attackHandler = Main.getInstance().getAttackPerpetrator().getAttackHandler();
        StopWatch attackTimer = attackHandler.getAttackTimer();
        int count = attackHandler.getCount();

        Angle angleDelta = AngleUtil.calculateDelta(currentAngle, targetAngle);
        float yawDelta = angleDelta.getYaw(), pitchDelta = angleDelta.getPitch();
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));

        if (entity != null) {
            float speed = attackHandler.canAttack(Aura.getInstance().getConfig(), 0) ? 1 : new SecureRandom().nextBoolean() ? 0.4F : 0.2F;

            float lineYaw = (Math.abs(yawDelta / rotationDifference) * 180);
            float linePitch = (Math.abs(pitchDelta / rotationDifference) * 180);

            float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
            float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

            Angle moveAngle = new Angle(currentAngle.getYaw(), currentAngle.getPitch());
            moveAngle.setYaw(MathHelper.lerp(randomLerp(speed, speed + 0.2F), currentAngle.getYaw(), currentAngle.getYaw() + moveYaw));
            moveAngle.setPitch(MathHelper.lerp(randomLerp(speed, speed + 0.2F), currentAngle.getPitch(), currentAngle.getPitch() + movePitch));

            return moveAngle;
        } else {
            int suck = count % 3;
            float speed = attackTimer.finished(400) ? new SecureRandom().nextBoolean() ? 0.4F : 0.2F : -0.2F;
            float random = attackTimer.elapsedTime() / 40F + (count % 6);

            Angle randomAngle = switch (suck) {
                case 0 -> new Angle((float) Math.cos(random), (float) Math.sin(random));
                case 1 -> new Angle((float) Math.sin(random), (float) Math.cos(random));
                case 2 -> new Angle((float) Math.sin(random), (float) -Math.cos(random));
                default -> new Angle((float) -Math.cos(random), (float) Math.sin(random));
            };

            float yaw = !attackTimer.finished(2000) ? randomLerp(12, 24) * randomAngle.getYaw() : 0;
            float pitch2 = randomLerp(0, 2) * (float) Math.cos((double) System.currentTimeMillis() / 5000);
            float pitch = !attackTimer.finished(2000) ? randomLerp(2, 6) * randomAngle.getPitch() + pitch2 : 0;

            float lineYaw = (Math.abs(yawDelta / rotationDifference) * 180);
            float linePitch = (Math.abs(pitchDelta / rotationDifference) * 180);

            float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
            float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

            Angle moveAngle = new Angle(currentAngle.getYaw(), currentAngle.getPitch());
            moveAngle.setYaw(MathHelper.lerp(Math.clamp(randomLerp(speed, speed + 0.2F), 0, 1), currentAngle.getYaw(), currentAngle.getYaw() + moveYaw) + yaw);
            moveAngle.setPitch(MathHelper.lerp(Math.clamp(randomLerp(speed, speed + 0.2F), 0, 1), currentAngle.getPitch(), currentAngle.getPitch() + movePitch) + pitch);

            return moveAngle;
        }
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0.06, 0.1, 0.06);
    }

    private float randomLerp(float min, float max) {
        return MathHelper.lerp(new SecureRandom().nextFloat(), min, max);
    }
}
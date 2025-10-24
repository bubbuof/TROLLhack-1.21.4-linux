package ru.zenith.implement.features.modules.combat.killaura.rotation.angle;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.core.Main;
import ru.zenith.implement.features.modules.combat.Aura;
import ru.zenith.implement.features.modules.combat.killaura.rotation.Angle;
import ru.zenith.implement.features.modules.combat.killaura.rotation.AngleUtil;

public class SnapSmoothMode extends AngleSmoothMode {
    public SnapSmoothMode() {
        super("Snap");
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
        Angle angleDelta = AngleUtil.calculateDelta(currentAngle, targetAngle);
        float yawDelta = angleDelta.getYaw();
        float pitchDelta = angleDelta.getPitch();
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
        float speed = entity != null ? 1 : 0.4F;

        float lineYaw = (Math.abs(yawDelta / rotationDifference) * 180);
        float linePitch = (Math.abs(pitchDelta / rotationDifference) * 180);

        float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
        float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

        Angle moveAngle = new Angle(currentAngle.getYaw(), currentAngle.getPitch());
        moveAngle.setYaw(MathHelper.lerp(MathUtil.getRandom(speed, speed + 0.2F), currentAngle.getYaw(),
                currentAngle.getYaw() + moveYaw));
        moveAngle.setPitch(MathHelper.lerp(MathUtil.getRandom(speed, speed + 0.2F), currentAngle.getPitch(),
                currentAngle.getPitch() + movePitch));

        return new Angle(moveAngle.getYaw(), moveAngle.getPitch());
    }
    @Override
    public Vec3d randomValue() {
        return new Vec3d(0.12F, 0.12F, 0.12F);
    }
}

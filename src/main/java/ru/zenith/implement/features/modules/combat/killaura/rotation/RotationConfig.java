package ru.zenith.implement.features.modules.combat.killaura.rotation;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import ru.zenith.implement.features.modules.combat.killaura.rotation.angle.AngleSmoothMode;
import ru.zenith.implement.features.modules.combat.killaura.rotation.angle.LinearSmoothMode;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RotationConfig {
    public static RotationConfig DEFAULT = new RotationConfig(new LinearSmoothMode(), true, true);
    boolean moveCorrection, freeCorrection;
    AngleSmoothMode angleSmooth;
    int resetThreshold = 3;

    public RotationConfig(boolean moveCorrection, boolean freeCorrection) {
        this(new LinearSmoothMode(), moveCorrection, freeCorrection);
    }

    public RotationConfig(boolean moveCorrection) {
        this(new LinearSmoothMode(), moveCorrection, true);
    }

    public RotationConfig(AngleSmoothMode angleSmooth, boolean moveCorrection, boolean freeCorrection) {
        this.angleSmooth = angleSmooth;
        this.moveCorrection = moveCorrection;
        this.freeCorrection = freeCorrection;
    }

    public RotationPlan createRotationPlan(Angle angle, Vec3d vec, Entity entity, int reset) {
        return new RotationPlan(angle, vec, entity, angleSmooth, reset, resetThreshold, moveCorrection, freeCorrection);
    }

    public RotationPlan createRotationPlan(Angle angle, Vec3d vec, Entity entity, boolean moveCorrection, boolean freeCorrection) {
        return new RotationPlan(angle, vec, entity, angleSmooth, 1, resetThreshold, moveCorrection, freeCorrection);
    }
}
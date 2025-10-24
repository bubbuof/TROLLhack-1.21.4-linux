package ru.zenith.implement.features.modules.combat.killaura.rotation;

import lombok.experimental.UtilityClass;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import ru.zenith.common.QuickImports;

import static java.lang.Math.hypot;
import static java.lang.Math.toDegrees;
import static net.minecraft.util.math.MathHelper.wrapDegrees;

@UtilityClass
public class AngleUtil implements QuickImports {
    public Angle fromVec2f(Vec2f vector2f) {
        return new Angle(vector2f.y, vector2f.x);
    }

    public Angle fromVec3d(Vec3d vector) {
        return new Angle((float) wrapDegrees(toDegrees(Math.atan2(vector.z, vector.x)) - 90), (float) wrapDegrees(toDegrees(-Math.atan2(vector.y, hypot(vector.x, vector.z)))));
    }

    public Angle calculateDelta(Angle start, Angle end) {
        float deltaYaw = MathHelper.wrapDegrees(end.getYaw() - start.getYaw());
        float deltaPitch = MathHelper.wrapDegrees(end.getPitch() - start.getPitch());
        return new Angle(deltaYaw, deltaPitch);
    }

    public Angle calculateAngle(Vec3d to) {
        return fromVec3d(to.subtract(mc.player.getEyePos()));
    }

    public Angle pitch(float pitch) {
        return new Angle(mc.player.getYaw(), pitch);
    }

    public Angle cameraAngle() {return new Angle(mc.player.getYaw(), mc.player.getPitch());}

}

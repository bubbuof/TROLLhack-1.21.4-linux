package ru.zenith.implement.features.modules.combat.killaura.rotation.angle;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.zenith.implement.features.modules.combat.killaura.rotation.Angle;
import ru.zenith.implement.features.modules.combat.killaura.rotation.AngleUtil;

import java.util.Random;

public class SpookyTimeSmoothMode extends AngleSmoothMode {
    private static final float RETURN_SPEED = 35.0F;
    private static final float MAX_YAW_SPEED = 55.03F;
    private static final float MIN_YAW_SPEED = 42.2F;
    private static final float MAX_PITCH_SPEED = 32.2F;
    private static final float MIN_PITCH_SPEED = 9.2F;
    private static final float RANDOM_SPEED_FACTOR = 0.3F;
    private static final float YAW_RANDOM_JITTER = 3.0F;
    private static final float PITCH_RANDOM_JITTER = 2.0F;
    private static final float YAW_PITCH_COUPLING = 1.0F;
    private static final float COOLDOWN_SLOWDOWN = 1.0F;

    private final Random random = new Random();
    private float lastYawJitter;
    private float lastPitchJitter;
    private int pointChangeCounter = 0;
    private BodyPoint currentTargetPoint = BodyPoint.HEAD;

    // Точки на теле для атаки
    private enum BodyPoint {
        HEAD(0.9, "Голова"),
        CHEST(0.55, "Грудь"),
        STOMACH(0.35, "Живот"),
        LEGS(0.15, "Ноги"),
        FEET(0.02, "Ступни");

        private final double heightMultiplier;
        private final String name;

        BodyPoint(double heightMultiplier, String name) {
            this.heightMultiplier = heightMultiplier;
            this.name = name;
        }

        public double getHeightMultiplier() { return heightMultiplier; }
        public String getName() { return name; }
    }

    public SpookyTimeSmoothMode() {
        super("SpookyTime");
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
        // Мультипойнт система - меняем точку каждые 3-6 тиков
        pointChangeCounter++;
        if (pointChangeCounter >= random.nextInt(3) + 3) { // 3-5 тиков
            currentTargetPoint = getRandomBodyPoint();
            pointChangeCounter = 0;
        }

        // Получаем позицию выбранной точки на теле
        Vec3d multiPointTarget = getBodyPointPosition(entity, currentTargetPoint);

        // Используем мультипойнт цель вместо стандартной
        Angle finalTargetAngle;
        if (multiPointTarget != null && entity != null) {
            finalTargetAngle = AngleUtil.fromVec3d(multiPointTarget.subtract(getEyePos()));
        } else {
            finalTargetAngle = targetAngle;
        }

        // Основная логика ротации
        Angle angleDelta = AngleUtil.calculateDelta(currentAngle, finalTargetAngle);
        float yawDelta = angleDelta.getYaw();
        float pitchDelta = angleDelta.getPitch();

        float yawAbs = Math.abs(yawDelta);
        float pitchAbs = Math.abs(pitchDelta);

        float yawFraction = MathHelper.clamp(yawAbs / 180.0F, 0.0F, 1.0F);
        float pitchFraction = MathHelper.clamp(pitchAbs / 90.0F, 0.0F, 1.0F);

        float yawSpeed = MathHelper.lerp(yawFraction, MIN_YAW_SPEED, MAX_YAW_SPEED);
        float pitchSpeed = MathHelper.lerp(pitchFraction, MIN_PITCH_SPEED, MAX_PITCH_SPEED);

        if (mc.player != null) {
            float cooldown = 1.0F - MathHelper.clamp(mc.player.getAttackCooldownProgress(1), 0.0F, 1.0F);
            float slowdown = MathHelper.lerp(cooldown, 1.0F, COOLDOWN_SLOWDOWN);
            yawSpeed *= slowdown;
            pitchSpeed *= slowdown;
        }

        if (entity == null) {
            yawSpeed = Math.max(yawSpeed, RETURN_SPEED);
            pitchSpeed = Math.max(pitchSpeed, RETURN_SPEED * 0.6F);
        }

        float randomScaleYaw = 1.0F + ((random.nextFloat() * 2.0F - 1.0F) * RANDOM_SPEED_FACTOR);
        float randomScalePitch = 1.0F + ((random.nextFloat() * 2.0F - 1.0F) * RANDOM_SPEED_FACTOR * YAW_PITCH_COUPLING);

        yawSpeed = MathHelper.clamp(yawSpeed * randomScaleYaw, MIN_YAW_SPEED, MAX_YAW_SPEED);
        pitchSpeed = MathHelper.clamp(pitchSpeed * randomScalePitch, MIN_PITCH_SPEED, MAX_PITCH_SPEED);

        float yawStep = MathHelper.clamp(yawDelta, -yawSpeed, yawSpeed);
        float pitchStep = MathHelper.clamp(pitchDelta, -pitchSpeed, pitchSpeed);

        lastYawJitter = randomJitter(YAW_RANDOM_JITTER, lastYawJitter);
        lastPitchJitter = randomJitter(PITCH_RANDOM_JITTER, lastPitchJitter);

        Angle moveAngle = new Angle(currentAngle.getYaw() + yawStep + lastYawJitter,
                MathHelper.clamp(currentAngle.getPitch() + pitchStep + lastPitchJitter, -89.0F, 90.0F));

        return moveAngle.adjustSensitivity();
    }

    /**
     * Получаем случайную точку на теле
     */
    private BodyPoint getRandomBodyPoint() {
        int rand = random.nextInt(100);

        if (rand < 35) return BodyPoint.HEAD;      // 35% - голова
        else if (rand < 55) return BodyPoint.CHEST; // 20% - грудь
        else if (rand < 70) return BodyPoint.STOMACH; // 15% - живот
        else if (rand < 85) return BodyPoint.LEGS;   // 15% - ноги
        else return BodyPoint.FEET;                 // 15% - ступни
    }

    /**
     * Получаем позицию точки на теле со случайными смещениями
     */
    private Vec3d getBodyPointPosition(Entity entity, BodyPoint point) {
        if (entity == null) return null;

        // Используем текущие позиции
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();

        double bodyHeight = entity.getHeight();
        double pointY = y + (bodyHeight * point.getHeightMultiplier());

        // Случайные смещения для естественности
        double offsetRange = entity.getWidth() * 0.15;
        double offsetX = (random.nextDouble() - 0.5) * offsetRange;
        double offsetZ = (random.nextDouble() - 0.5) * offsetRange;
        double offsetY = (random.nextDouble() - 0.5) * bodyHeight * 0.03;

        return new Vec3d(x + offsetX, pointY + offsetY, z + offsetZ);
    }

    /**
     * Рандомизация ударов - вызывается извне при атаке
     */
    public boolean shouldAttack() {
        // 90% шанс атаки + случайные пропуски
        if (random.nextFloat() < 0.9f) {
            // Иногда пропускаем атаку в зависимости от точки прицеливания
            if (currentTargetPoint == BodyPoint.FEET && random.nextFloat() < 0.3f) {
                return false; // 30% шанс пропустить при стрельбе в ноги
            }
            if (currentTargetPoint == BodyPoint.LEGS && random.nextFloat() < 0.15f) {
                return false; // 15% шанс пропустить при стрельбе в ноги
            }
            return true;
        }
        return false;
    }

    private Vec3d getEyePos() {
        return mc.player.getEyePos();
    }

    @Override
    public Vec3d randomValue() {
        // Случайный разброс с учетом мультипойнт
        return new Vec3d(
                0.08 + random.nextDouble() * 0.24,
                0.08 + random.nextDouble() * 0.24,
                0.08 + random.nextDouble() * 0.24
        );
    }

    private float randomJitter(float bound, float previous) {
        if (bound <= 0.0F) return 0.0F;
        float next = (random.nextFloat() * 2.0F - 1.0F) * bound;
        return MathHelper.lerp(0.35F, previous, next);
    }

    public String getCurrentTargetPoint() {
        return currentTargetPoint.getName();
    }
}
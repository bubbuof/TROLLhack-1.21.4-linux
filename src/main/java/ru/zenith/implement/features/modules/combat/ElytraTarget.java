package ru.zenith.implement.features.modules.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.BooleanSetting;
import ru.zenith.api.feature.module.setting.implement.ValueSetting;
import ru.zenith.common.util.other.Instance;
import ru.zenith.common.util.render.Render3DUtil;
import ru.zenith.implement.events.player.TickEvent;
import ru.zenith.implement.events.render.WorldRenderEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ElytraTarget extends Module {
    public static ElytraTarget getInstance() {
        return Instance.get(ElytraTarget.class);
    }

    // Основные настройки
    private final ValueSetting range = new ValueSetting("Дистанция", "Максимальная дистанция атаки").setValue(4.0f).range(1.0f, 6.0f);
    private final BooleanSetting throughWalls = new BooleanSetting("Сквозь стены", "Атаковать сквозь стены").setValue(false);

    // Настройки целей
    private final BooleanSetting targetPlayers = new BooleanSetting("Игроки", "Атаковать игроков").setValue(true);
    private final BooleanSetting targetMobs = new BooleanSetting("Мобы", "Атаковать мобов").setValue(false);

    // Настройки элитры
    private final ValueSetting elytraPredictTicks = new ValueSetting("Предсказание", "Тики предсказания для элитр").setValue(3.0f).range(0.0f, 10.0f);
    private final ValueSetting elytraRangeMultiplier = new ValueSetting("Множитель дистанции", "Множитель дистанции в полете").setValue(4.0f).range(1.0f, 10.0f);
    private final ValueSetting flightSmoothFactor = new ValueSetting("Плавность в полете", "Плавность аима в полете").setValue(1.5f).range(1.0f, 3.0f);

    // Визуальные настройки
    private final BooleanSetting renderPredict = new BooleanSetting("Показывать предсказание", "Отображать предсказание траектории").setValue(true);
    private final ValueSetting predictAlpha = new ValueSetting("Прозрачность", "Прозрачность предсказания").setValue(0.35f).range(0.0f, 1.0f);
    private final ValueSetting predictLineWidth = new ValueSetting("Толщина линий", "Толщина линий предсказания").setValue(1.5f).range(0.5f, 5.0f);

    // Настройки атаки
    private final ValueSetting attackFov = new ValueSetting("Угол атаки", "Угол для начала атаки").setValue(30.0f).range(1.0f, 90.0f);
    private final ValueSetting minCooldown = new ValueSetting("Минимальный КД", "Минимальный кд атаки").setValue(0.8f).range(0.1f, 1.0f);
    private final BooleanSetting onlyElytra = new BooleanSetting("Только в полете", "Работать только при полете на элитре").setValue(true);

    private LivingEntity target;
    private long lastAttackTime;

    public ElytraTarget() {
        super("ElytraTarget", "ElytraTarget", ModuleCategory.COMBAT);

        // Настройка видимости дополнительных опций
        predictAlpha.visible(() -> renderPredict.isValue());
        predictLineWidth.visible(() -> renderPredict.isValue());
        flightSmoothFactor.visible(() -> onlyElytra.isValue());

        setup(range, throughWalls, targetPlayers, targetMobs, onlyElytra, elytraPredictTicks, elytraRangeMultiplier,
                flightSmoothFactor, renderPredict, predictAlpha, predictLineWidth,
                attackFov, minCooldown);
    }

    @Override
    public void activate() {
        super.activate();
        target = null;
        lastAttackTime = 0L;
    }

    @Override
    public void deactivate() {
        super.deactivate();
        target = null;
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.world == null || mc.player == null) return;

        // Проверка полета на элитре
        if (onlyElytra.isValue() && !isFlyingWithElytra()) {
            target = null;
            return;
        }

        findTarget();
        if (target != null) {
            faceTargetSmooth();
            tryAttack();
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (mc.world == null || mc.player == null || target == null) return;

        if (renderPredict.isValue() && isFlyingWithElytra() && elytraPredictTicks.getValue() > 0.0f) {
            renderPrediction();
        }
    }

    private void renderPrediction() {
        Vec3d predicted = getTargetPoint();
        Box base = target.getBoundingBox();
        Vec3d shift = predicted.subtract(target.getPos());
        Box box = base.offset(shift);

        int color = (int) (predictAlpha.getValue() * 255) << 24 | 0xFFFF0000;
        Render3DUtil.drawBox(box, color, predictLineWidth.getValue());

        // Линия от игрока к предсказанной позиции
        Render3DUtil.drawLine(mc.player.getPos(), predicted, 0x80FF0000, predictLineWidth.getValue(), false);
    }

    private void findTarget() {
        List<LivingEntity> targets = new ArrayList<>();

        for (Entity entity : mc.world.getEntities()) {
            if (isValidTargetType(entity)) {
                targets.add((LivingEntity) entity);
            }
        }

        // Сортировка по расстоянию
        targets.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(mc.player)));

        if (targets.isEmpty()) {
            target = null;
        } else {
            target = targets.get(0);
        }
    }

    private boolean isValidTargetType(Entity entity) {
        if (!(entity instanceof LivingEntity)) return false;
        if (entity == mc.player) return false;

        LivingEntity living = (LivingEntity) entity;

        // Проверка здоровья
        if (living.isDead() || living.getHealth() <= 0.0f) return false;

        // Проверка типа цели
        if (entity instanceof PlayerEntity) {
            if (!targetPlayers.isValue()) return false;
            PlayerEntity player = (PlayerEntity) entity;
            if (player.isSpectator()) return false;
        } else if (entity instanceof MobEntity) {
            if (!targetMobs.isValue()) return false;
        } else {
            return false; // Неподдерживаемый тип entity
        }

        // Проверка дистанции
        if (living.squaredDistanceTo(mc.player) > getEffectiveRange() * getEffectiveRange()) return false;

        // Проверка видимости
        return throughWalls.isValue() || canSeeTarget(living);
    }

    private boolean canSeeTarget(LivingEntity entity) {
        Vec3d eyes = mc.player.getEyePos();
        Vec3d targetPoint = entity.getPos().add(0, entity.getStandingEyeHeight() * 0.5, 0);

        HitResult result = mc.world.raycast(new RaycastContext(eyes, targetPoint,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));

        return result.getType() == HitResult.Type.MISS ||
                (result.getType() == HitResult.Type.ENTITY &&
                        ((EntityHitResult) result).getEntity() == entity);
    }

    private void faceTargetSmooth() {
        if (target == null) return;

        Vec3d eyes = mc.player.getEyePos();
        Vec3d targetPoint = getTargetPoint();

        if (throughWalls.isValue() || canSeeTarget(target)) {
            float[] rot = getRotationTo(eyes, targetPoint);
            float targetYaw = rot[0];
            float targetPitch = rot[1];

            // Плавность зависит от режима полета
            float smoothFactor = isFlyingWithElytra() ? flightSmoothFactor.getValue() : 1.0f;

            float yawDiff = wrapDegrees(targetYaw - mc.player.getYaw());
            float pitchDiff = wrapDegrees(targetPitch - mc.player.getPitch());

            // Применяем плавность
            yawDiff *= smoothFactor;
            pitchDiff *= smoothFactor;

            float newYaw = mc.player.getYaw() + yawDiff;
            float newPitch = mc.player.getPitch() + pitchDiff;
            newPitch = clamp(newPitch, -89.0f, 89.0f);

            mc.player.setYaw(newYaw);
            mc.player.setPitch(newPitch);
        }
    }

    private void tryAttack() {
        if (target == null) return;

        // Проверка угла атаки
        Vec3d eyes = mc.player.getEyePos();
        Vec3d targetPoint = getTargetPoint();
        float[] need = getRotationTo(eyes, targetPoint);
        float yawDiff = Math.abs(wrapDegrees(need[0] - mc.player.getYaw()));
        float pitchDiff = Math.abs(wrapDegrees(need[1] - mc.player.getPitch()));

        if (yawDiff + pitchDiff > attackFov.getValue()) {
            return;
        }

        // Проверка дистанции
        if (mc.player.squaredDistanceTo(target.getPos()) > getEffectiveRange() * getEffectiveRange()) {
            return;
        }

        // Проверка видимости
        if (!throughWalls.isValue() && !canSeeTarget(target)) {
            return;
        }

        // Проверка кд атаки
        if (mc.player.getAttackCooldownProgress(0.0f) < minCooldown.getValue()) {
            return;
        }

        // Атака
        if (mc.interactionManager != null) {
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
            lastAttackTime = System.currentTimeMillis();
        }
    }

    private boolean isFlyingWithElytra() {
        return mc.player != null && mc.player.isGliding();
    }

    private float[] getRotationTo(Vec3d from, Vec3d to) {
        Vec3d diff = to.subtract(from);
        double distXZ = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float yaw = (float) (Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0f);
        float pitch = (float) (-Math.toDegrees(Math.atan2(diff.y, distXZ)));
        return new float[]{yaw, pitch};
    }

    private Vec3d getTargetPoint() {
        Vec3d base = target.getPos().add(0, target.getHeight() * 0.5, 0);

        if (isFlyingWithElytra() && elytraPredictTicks.getValue() > 0.0f) {
            // Предсказание траектории для элитры
            Vec3d vel = target.getVelocity();
            int ticks = (int) elytraPredictTicks.getValue();
            return base.add(vel.multiply(ticks));
        }

        return base;
    }

    private float getEffectiveRange() {
        return isFlyingWithElytra() ?
                range.getValue() * elytraRangeMultiplier.getValue() :
                range.getValue();
    }

    private float wrapDegrees(float value) {
        value = value % 360.0f;
        if (value >= 180.0f) value -= 360.0f;
        if (value < -180.0f) value += 360.0f;
        return value;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
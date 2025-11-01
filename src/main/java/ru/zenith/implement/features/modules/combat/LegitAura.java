package ru.zenith.implement.features.modules.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.BindSetting;
import ru.zenith.api.feature.module.setting.implement.BooleanSetting;
import ru.zenith.api.feature.module.setting.implement.SelectSetting;
import ru.zenith.api.feature.module.setting.implement.ValueSetting;
import ru.zenith.common.util.other.Instance;
import ru.zenith.common.util.render.Render3DUtil;
import ru.zenith.implement.events.player.TickEvent;
import ru.zenith.implement.events.render.WorldRenderEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class LegitAura extends Module {
    public static LegitAura getInstance() {
        return Instance.get(LegitAura.class);
    }

    private final ValueSetting range = new ValueSetting("Расстояние", "Максимальная дистанция атаки").setValue(4.0f).range(1.0f, 6.0f);
    private final BooleanSetting throughWalls = new BooleanSetting("Сквозь стены", "Атаковать сквозь стены").setValue(false);
    private final BooleanSetting randomize = new BooleanSetting("Рандомизация", "Добавлять случайность в аим").setValue(true);
    private final ValueSetting aimSpeed = new ValueSetting("Скорость наводки", "Скорость поворота к цели").setValue(1.0f).range(0.1f, 100.0f);
    private final ValueSetting yawSpeed = new ValueSetting("Y коэф плавности", "Плавность поворота по Y").setValue(35.0f).range(1.0f, 90.0f);
    private final ValueSetting pitchSpeed = new ValueSetting("P коэф плавности", "Плавность поворота по P").setValue(20.0f).range(0.0f, 90.0f);
    private final ValueSetting smoothFactor = new ValueSetting("Уровень плавности", "Общая плавность аима").setValue(0.2f).range(0.01f, 1.0f);
    private final BooleanSetting onlyCrits = new BooleanSetting("Только криты", "Атаковать только критами").setValue(false);
    private final BooleanSetting smartCrits = new BooleanSetting("Умные криты", "Автоматически переключать криты").setValue(false);
    private final BooleanSetting resetSprint = new BooleanSetting("Сброс спринта", "Отжимать спринт для критов").setValue(true);
    private final BooleanSetting triggerMode = new BooleanSetting("Триггер мод", "Атаковать только при наведении").setValue(false);
    private final BooleanSetting yawOnly = new BooleanSetting("Только горизонталь", "Только горизонтальный аим").setValue(false);
    private final BooleanSetting onlySword = new BooleanSetting("Меч", "Работать только с мечом").setValue(false);
    private final BooleanSetting breakShields = new BooleanSetting("Ломать щит", "Автоматически ломать щиты").setValue(false);
    private final BooleanSetting targetLock = new BooleanSetting("Задерживать игрока", "Удерживать цель после атаки").setValue(false);
    private final ValueSetting lockTimeSec = new ValueSetting("Время", "Время удержания цели").setValue(5.0f).range(1.0f, 20.0f);
    private final ValueSetting lockDistance = new ValueSetting("Расстояние", "Дистанция удержания цели").setValue(10.0f).range(1.0f, 15.0f);
    private final ValueSetting elytraPredictTicks = new ValueSetting("ElytraPredict", "Предсказание для элитр").setValue(2.0f).range(0.0f, 10.0f);
    private final ValueSetting flightSmoothFactor = new ValueSetting("FlightSmooth", "Плавность в полете").setValue(1.5f).range(1.0f, 3.0f);
    private final ValueSetting elytraRangeMultiplier = new ValueSetting("ElytraRangeMul", "Множитель дистанции в полете").setValue(4.0f).range(1.0f, 10.0f);
    private final BooleanSetting renderPredict = new BooleanSetting("RenderPredict", "Показывать предсказание").setValue(true);
    private final ValueSetting predictAlpha = new ValueSetting("PredictAlpha", "Прозрачность предсказания").setValue(0.35f).range(0.0f, 1.0f);
    private final ValueSetting predictLineWidth = new ValueSetting("PredictLine", "Толщина линий предсказания").setValue(1.5f).range(0.5f, 5.0f);
    private final BooleanSetting requireCrosshair = new BooleanSetting("RequireCrosshair", "Требовать наведение").setValue(false);
    private final ValueSetting attackFov = new ValueSetting("AttackFOV", "Угол атаки").setValue(20.0f).range(1.0f, 90.0f);
    private final ValueSetting minCooldown = new ValueSetting("MinCooldown", "Минимальный кд атаки").setValue(0.85f).range(0.1f, 1.0f);

    private final SelectSetting priority = new SelectSetting("Priority", "Приоритет выбора цели")
            .value("Distance", "Health", "Angle");

    private LivingEntity target;
    private long lastJitter;
    private float lastYaw;
    private float lastPitch;
    private PlayerEntity lockedTarget;
    private long lastSuccessfulAttackMs;
    private float yawVel;
    private float pitchVel;
    private boolean wasSprintingBeforeAttack;

    public LegitAura() {
        super("LegitAura", "LegitAura", ModuleCategory.COMBAT);

        // Настройка зависимостей
        smartCrits.visible(() -> onlyCrits.isValue());
        resetSprint.visible(() -> onlyCrits.isValue() || smartCrits.isValue());
        lockTimeSec.visible(() -> targetLock.isValue());
        lockDistance.visible(() -> targetLock.isValue());

        setup(range, throughWalls, randomize, aimSpeed, yawSpeed, pitchSpeed, smoothFactor,
                onlyCrits, smartCrits, resetSprint, triggerMode, yawOnly, onlySword, breakShields,
                targetLock, lockTimeSec, lockDistance, elytraPredictTicks, flightSmoothFactor,
                elytraRangeMultiplier, renderPredict, predictAlpha, predictLineWidth,
                requireCrosshair, attackFov, minCooldown, priority);

        lastJitter = 0L;
        lockedTarget = null;
        lastSuccessfulAttackMs = 0L;
        yawVel = 0.0f;
        pitchVel = 0.0f;
        wasSprintingBeforeAttack = false;
    }

    @Override
    public void activate() {
        super.activate();
        target = null;
        if (mc.player != null) {
            lastYaw = mc.player.getYaw();
            lastPitch = mc.player.getPitch();
        }
        lastJitter = System.currentTimeMillis();
        lockedTarget = null;
        lastSuccessfulAttackMs = 0L;
        yawVel = 0.0f;
        pitchVel = 0.0f;
        wasSprintingBeforeAttack = false;
    }

    @Override
    public void deactivate() {
        super.deactivate();
        target = null;
        if (wasSprintingBeforeAttack && mc.player != null) {
            mc.player.setSprinting(true);
            wasSprintingBeforeAttack = false;
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.world == null || mc.player == null) return;

        if (onlySword.isValue()) {
            ItemStack stack = mc.player.getMainHandStack();
            if (stack.isEmpty() || !(stack.getItem() instanceof SwordItem)) {
                return;
            }
        }

        if (triggerMode.isValue()) {
            triggerAttackTick();
        } else {
            findTarget();
            if (target != null) {
                faceTargetSmooth();
                tryAttack();
            }
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (mc.world == null || mc.player == null || target == null) return;

        if (renderPredict.isValue() && mc.player.isGliding() && elytraPredictTicks.getValue() > 0.0f) {
            Vec3d predicted = getTargetPoint();
            Box base = target.getBoundingBox();
            Vec3d shift = predicted.subtract(target.getPos());
            Box box = base.offset(shift);
            int color = (int) (predictAlpha.getValue() * 255) << 24 | 0xFFFF0000;
            Render3DUtil.drawBox(box, color, predictLineWidth.getValue());
        }
    }

    private void triggerAttackTick() {
        HitResult hit = mc.crosshairTarget;
        if (hit instanceof EntityHitResult entityHit) {
            Entity ent = entityHit.getEntity();
            if (ent instanceof LivingEntity living && living.isAlive()) {
                if (ent instanceof PlayerEntity player) {
                    // TODO: Добавить проверку друзей
                }

                if (!throughWalls.isValue() && !canSeePoint(mc.player.getEyePos(),
                        living.getPos().add(0, living.getHeight() * 0.5, 0))) {
                    return;
                }

                if (mc.player.getEyePos().squaredDistanceTo(living.getPos().add(0, living.getStandingEyeHeight() * 0.5, 0)) >
                        getEffectiveRange() * getEffectiveRange()) {
                    return;
                }

                if (mc.player.getAttackCooldownProgress(0.0f) < Math.max(0.0f, Math.min(1.0f, minCooldown.getValue()))) {
                    return;
                }

                if (smartCrits.isValue()) {
                    if (mc.options.attackKey.isPressed() && !isCriticalReady()) {
                        return;
                    }
                } else if (onlyCrits.isValue() && !isCriticalReady()) {
                    return;
                }

                if (mc.interactionManager != null) {
                    boolean requireCrit = onlyCrits.isValue() || (smartCrits.isValue() && mc.options.attackKey.isPressed());
                    handleSprintForCrit(requireCrit);

                    mc.interactionManager.attackEntity(mc.player, living);
                    mc.player.swingHand(Hand.MAIN_HAND); // ФИКС: главная рука

                    restoreSprintAfterAttack();

                    lastSuccessfulAttackMs = System.currentTimeMillis();
                    if (targetLock.isValue() && lockedTarget == null && living instanceof PlayerEntity) {
                        lockedTarget = (PlayerEntity) living;
                    }
                }
            }
        }
    }

    private void tryAttack() {
        if (target == null) return;

        if (requireCrosshair.isValue()) {
            HitResult hit = mc.crosshairTarget;
            if (!throughWalls.isValue()) {
                if (!(hit instanceof EntityHitResult)) return;
                EntityHitResult ehr = (EntityHitResult) hit;
                if (ehr.getEntity() != target) return;
            }
        } else {
            if (!throughWalls.isValue() && !canSeePoint(mc.player.getEyePos(), getTargetPoint())) {
                return;
            }

            float[] need = getRotationTo(mc.player.getEyePos(), getTargetPoint());
            float yawDiff = Math.abs(wrapDegrees(need[0] - mc.player.getYaw()));
            float pitchDiff = Math.abs(wrapDegrees(need[1] - mc.player.getPitch()));
            if (yawDiff + (yawOnly.isValue() ? 0.0f : pitchDiff) > attackFov.getValue()) {
                return;
            }
        }

        if (mc.player.getEyePos().squaredDistanceTo(getTargetPoint()) > getEffectiveRange() * getEffectiveRange()) {
            return;
        }

        if (breakShields.isValue() && target instanceof PlayerEntity) {
            PlayerEntity p = (PlayerEntity) target;
            if (p.isBlocking()) {
                if (smartCrits.isValue() && mc.options.attackKey.isPressed() && !isCriticalReady()) {
                    return;
                }

                int prevSlot = mc.player.getInventory().selectedSlot;
                int axe = findAxeInHotbar();
                if (axe != -1) {
                    if (axe != prevSlot) {
                        mc.player.getInventory().selectedSlot = axe;
                    }

                    boolean requireCrit = smartCrits.isValue() && mc.options.attackKey.isPressed();
                    handleSprintForCrit(requireCrit);

                    if (mc.interactionManager != null) {
                        mc.interactionManager.attackEntity(mc.player, target);
                        mc.player.swingHand(Hand.MAIN_HAND); // ФИКС: главная рука
                    }

                    restoreSprintAfterAttack();

                    if (axe != prevSlot) {
                        mc.player.getInventory().selectedSlot = prevSlot;
                    }
                    return;
                }
            }
        }

        if (mc.player.getAttackCooldownProgress(0.0f) < minCooldown.getValue()) {
            return;
        }

        if (smartCrits.isValue()) {
            if (mc.options.attackKey.isPressed() && !isCriticalReady()) {
                return;
            }
        } else if (onlyCrits.isValue() && !isCriticalReady()) {
            return;
        }

        if (mc.interactionManager != null) {
            boolean requireCrit = onlyCrits.isValue() || (smartCrits.isValue() && mc.options.attackKey.isPressed());
            handleSprintForCrit(requireCrit);

            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND); // ФИКС: главная рука

            restoreSprintAfterAttack();

            lastSuccessfulAttackMs = System.currentTimeMillis();
            if (targetLock.isValue() && lockedTarget == null && target instanceof PlayerEntity) {
                lockedTarget = (PlayerEntity) target;
            }
        }
    }

    private void handleSprintForCrit(boolean requireCrit) {
        if (resetSprint.isValue() && requireCrit) {
            wasSprintingBeforeAttack = mc.player.isSprinting();
            if (wasSprintingBeforeAttack) {
                mc.player.setSprinting(false);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    // Игнорируем
                }
            }
        } else {
            wasSprintingBeforeAttack = false;
        }
    }

    private void restoreSprintAfterAttack() {
        if (resetSprint.isValue() && wasSprintingBeforeAttack && mc.player != null) {
            new Thread(() -> {
                try {
                    Thread.sleep(50);
                    if (mc.player != null) {
                        mc.player.setSprinting(true);
                    }
                } catch (InterruptedException e) {
                    // Игнорируем
                }
            }).start();
            wasSprintingBeforeAttack = false;
        }
    }

    private void findTarget() {
        List<LivingEntity> targets = new ArrayList<>();

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity living) {
                if (entity != mc.player && isValidTarget(living)) {
                    targets.add(living);
                }
            }
        }

        if (targetLock.isValue() && lockedTarget != null) {
            boolean alive = lockedTarget.isAlive() && lockedTarget.getHealth() > 0.0f;
            boolean inDist = mc.player.distanceTo(lockedTarget) <= Math.max(1.0f, lockDistance.getValue());
            long timeoutMs = (long) (lockTimeSec.getValue() * 1000.0f);
            boolean withinTime = System.currentTimeMillis() - lastSuccessfulAttackMs <= timeoutMs;

            if (alive && inDist && withinTime) {
                target = lockedTarget;
                return;
            }
            lockedTarget = null;
        }

        if (targets.isEmpty()) {
            target = null;
        } else {
            LivingEntity prev = target;

            String priorityMode = priority.getSelected();
            if (priorityMode.equals("Distance")) {
                targets.sort(Comparator.comparingDouble(e -> e.distanceTo(mc.player)));
            } else if (priorityMode.equals("Health")) {
                targets.sort(Comparator.comparingDouble(LivingEntity::getHealth));
            } else if (priorityMode.equals("Angle")) {
                targets.sort(Comparator.comparingDouble(this::angleTo));
            }

            target = targets.get(0);
            if (prev != target) {
                yawVel = 0.0f;
                pitchVel = 0.0f;
            }
        }
    }

    private boolean isValidTarget(PlayerEntity e) {
        if (e.isSpectator() || e.getHealth() <= 0.0f) {
            return false;
        }

        if (e.distanceTo(mc.player) > getEffectiveRange()) {
            return false;
        }

        return throughWalls.isValue() || canSeePoint(mc.player.getEyePos(),
                e.getPos().add(0, e.getStandingEyeHeight() * 0.5, 0));
    }

    private double angleTo(LivingEntity e) {
        float[] needed = getRotationTo(mc.player.getEyePos(),
                e.getPos().add(0, e.getHeight() * 0.5, 0));
        float yawDiff = wrapDegrees(needed[0] - mc.player.getYaw());
        float pitchDiff = wrapDegrees(needed[1] - mc.player.getPitch());
        return Math.abs(yawDiff) + Math.abs(pitchDiff);
    }

    private void faceTargetSmooth() {
        if (target != null) {
            Vec3d eyes = mc.player.getEyePos();
            Vec3d targetPoint = getTargetPoint();

            if (throughWalls.isValue() || canSeePoint(eyes, targetPoint)) {
                float[] rot = getRotationTo(eyes, targetPoint);
                float targetYaw = rot[0];
                float targetPitch = rot[1];

                if (randomize.isValue()) {
                    long now = System.currentTimeMillis();
                    float t = (float) (now % 120000L) / 1000.0f;
                    float wobbleScale = 0.2f;
                    targetYaw += (float) (Math.sin(t * 1.31f) * wobbleScale);
                    targetPitch += (float) (Math.cos(t * 1.77f) * (wobbleScale * 0.7f));
                }

                if (yawOnly.isValue()) {
                    targetPitch = mc.player.getPitch();
                    pitchVel = 0.0f;
                }

                float yawDiff = wrapDegrees(targetYaw - mc.player.getYaw());
                float pitchDiff = wrapDegrees(targetPitch - mc.player.getPitch());

                float maxYawStep = yawSpeed.getValue();
                float maxPitchStep = pitchSpeed.getValue();

                float yawStep = clamp(yawDiff, -maxYawStep, maxYawStep);
                float pitchStep = clamp(pitchDiff, -maxPitchStep, maxPitchStep);

                float kBase = clamp(smoothFactor.getValue(), 0.01f, 1.0f);
                float k = (float) (1.0f - Math.pow(1.0 - kBase, 1.6));
                k *= clamp(aimSpeed.getValue(), 0.1f, 100.0f) / 10.0f;

                float newYaw = mc.player.getYaw() + yawStep * k;
                float newPitch = mc.player.getPitch() + (yawOnly.isValue() ? 0.0f : pitchStep * k);
                newPitch = clamp(newPitch, -89.0f, 89.0f);

                mc.player.setYaw(newYaw);
                mc.player.setPitch(newPitch);
                lastYaw = newYaw;
                lastPitch = newPitch;
            }
        }
    }

    private int findAxeInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof AxeItem) {
                return i;
            }
        }
        return -1;
    }

    private boolean canSeePoint(Vec3d start, Vec3d end) {
        HitResult result = mc.world.raycast(new RaycastContext(start, end,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));

        if (result.getType() == HitResult.Type.MISS) {
            return true;
        } else if (result.getType() == HitResult.Type.ENTITY) {
            return ((EntityHitResult) result).getEntity() == target;
        } else {
            return false;
        }
    }

    private float[] getRotationTo(Vec3d from, Vec3d to) {
        Vec3d diff = to.subtract(from);
        double distXZ = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float yaw = (float) (Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0f);
        float pitch = (float) (-Math.toDegrees(Math.atan2(diff.y, distXZ)));
        return new float[]{yaw, pitch};
    }

    private boolean isCriticalReady() {
        if (mc.player == null) return false;
        if (mc.player.isTouchingWater()) return false;
        if (mc.player.isGliding()) return false;
        if (mc.player.isClimbing()) return false;
        if (mc.player.hasVehicle()) return false;
        try {
            if (mc.player.isInsideWall()) return false;
        } catch (Throwable e) {}
        if (mc.player.hasStatusEffect(StatusEffects.BLINDNESS)) return false;
        if (mc.player.isSprinting()) return false;
        if (mc.player.fallDistance <= 0.0f) return false;
        return !(mc.player.getVelocity().y >= -0.06);
    }

    private Vec3d getTargetPoint() {
        Vec3d base = target.getPos().add(0, target.getHeight() * 0.5, 0);
        if (mc.player.isGliding() && elytraPredictTicks.getValue() > 0.0f) {
            Vec3d vel = target.getVelocity();
            int ticks = (int) elytraPredictTicks.getValue();
            return base.add(vel.multiply(ticks));
        } else {
            return base;
        }
    }

    private float getEffectiveRange() {
        return mc.player != null && mc.player.isGliding() ?
                range.getValue() * elytraRangeMultiplier.getValue() : range.getValue();
    }

    private float wrapDegrees(float value) {
        value = value % 360.0f;
        if (value >= 180.0f) {
            value -= 360.0f;
        }
        if (value < -180.0f) {
            value += 360.0f;
        }
        return value;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
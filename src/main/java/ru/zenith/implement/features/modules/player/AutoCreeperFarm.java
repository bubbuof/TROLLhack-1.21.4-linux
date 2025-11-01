package ru.zenith.implement.features.modules.player;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.*;
import ru.zenith.implement.events.player.TickEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.text.Text;
import java.util.Comparator;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoCreeperFarm extends Module {

    private static AutoCreeperFarm instance;

    public static AutoCreeperFarm getInstance() {
        if (instance == null) {
            instance = new AutoCreeperFarm();
        }
        return instance;
    }

    final ValueSetting searchRadius = new ValueSetting("Search Radius", "Радиус поиска криперов")
            .range(10.0f, 120.0f)
            .setValue(50.0f);

    final ValueSetting attackReach = new ValueSetting("Attack Reach", "Дистанция атаки")
            .range(2.0f, 6.0f)
            .setValue(3.0f);

    final ValueSetting maxDistance = new ValueSetting("Max Distance", "Максимальная дистанция")
            .range(3.0f, 12.0f)
            .setValue(6.0f);

    final BooleanSetting useBaritone = new BooleanSetting("Use Baritone", "Использовать Baritone для следования")
            .setValue(false);

    boolean retreating = false;
    long lastHitTime = 0L;

    public AutoCreeperFarm() {
        super("AutoCreeperFarm", "AutoCreeperFarm", ModuleCategory.PLAYER);
        setup(searchRadius, attackReach, maxDistance, useBaritone);
        instance = this;
    }

    @Override
    public void activate() {
        super.activate();
        if (useBaritone.isValue()) {
            sendChatSilent("#follow entity creeper");
            sendChatSilent("#allowBreak false");
        }
    }

    @Override
    public void deactivate() {
        super.deactivate();
        if (useBaritone.isValue()) {
            sendChatSilent("#stop");
        }
        retreating = false;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        CreeperEntity target = findCreeper(searchRadius.getValue());
        if (target == null) {
            retreating = false;
            return;
        }

        long now = System.currentTimeMillis();

        // Логика отступления после удара
        if (retreating) {
            double dx = mc.player.getX() - target.getX();
            double dz = mc.player.getZ() - target.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);

            if (dist <= maxDistance.getValue() && now - lastHitTime <= 300L) {
                if (dist > 0.0001) {
                    double vx = dx / dist * 0.3;
                    double vz = dz / dist * 0.3;
                    mc.player.setVelocity(vx, mc.player.getVelocity().y, vz);
                    return;
                }
            } else {
                retreating = false;
            }
        }

        // Проверка условий для атаки
        boolean inRange = mc.player.distanceTo(target) <= maxDistance.getValue();
        boolean reachOK = distanceToPoint(target) <= attackReach.getValue();
        boolean canSee = canSeeEntity(target);
        float cooldown = mc.player.getAttackCooldownProgress(0.0f);
        boolean grounded = (!mc.options.forwardKey.isPressed() && mc.player.isOnGround()) || mc.player.fallDistance > 0.0f;

        if (grounded && cooldown >= 1.0f && inRange && reachOK && canSee) {
            // Атакуем крипера
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
            lastHitTime = now;
            retreating = true;
        }
    }

    private CreeperEntity findCreeper(float radius) {
        double radiusSquared = radius * radius;

        // Получаем все криперы в радиусе
        Box searchBox = mc.player.getBoundingBox().expand(radius);
        List<CreeperEntity> creepers = mc.world.getEntitiesByClass(
                CreeperEntity.class,
                searchBox,
                entity -> entity != null &&
                        mc.player.squaredDistanceTo(entity) <= radiusSquared &&
                        entity.isAlive()
        );

        // Возвращаем ближайшего крипера
        return creepers.stream()
                .min(Comparator.comparingDouble(entity -> mc.player.squaredDistanceTo(entity)))
                .orElse(null);
    }

    private double distanceToPoint(Entity entity) {
        Vec3d entityEyes = entity.getLerpedPos(1.0f).add(0.0, entity.getStandingEyeHeight() * 0.5, 0.0);
        Vec3d playerEyes = mc.player.getLerpedPos(1.0f).add(0.0, mc.player.getStandingEyeHeight() * 0.5, 0.0);
        return playerEyes.distanceTo(entityEyes);
    }

    private boolean canSeeEntity(Entity entity) {
        // Альтернативные способы проверки видимости

        // Вариант 1: Через raycast
        Vec3d from = mc.player.getEyePos();
        Vec3d to = entity.getBoundingBox().getCenter();

        HitResult hitResult = mc.world.raycast(new RaycastContext(
                from, to,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));

        return hitResult == null || hitResult.getType() == HitResult.Type.MISS;

        // Вариант 2: Упрощенная проверка (если raycast не работает)
        // return mc.player.distanceTo(entity) <= attackReach.getValue() * 2;

        // Вариант 3: Через метод canSee (если доступен в вашей версии)
        // try {
        //     return mc.player.canSee(entity);
        // } catch (NoSuchMethodError e) {
        //     return true; // Если метод недоступен, предполагаем что видим
        // }
    }

    private void sendChatSilent(String message) {
        try {
            if (mc.player != null && mc.getNetworkHandler() != null) {
                mc.getNetworkHandler().sendChatMessage(message);
                return;
            }
        } catch (Throwable ignored) {
        }

        try {
            if (mc.inGameHud != null) {
                mc.inGameHud.getChatHud().addMessage(Text.literal(message));
            }
        } catch (Throwable ignored) {
        }
    }
}
package ru.zenith.implement.features.modules.combat;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.ValueSetting;
import ru.zenith.common.util.other.Instance;
import ru.zenith.implement.events.player.TickEvent;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Reach extends Module {

    public static Reach getInstance() {
        return Instance.get(Reach.class);
    }

    ValueSetting reachDistance = new ValueSetting("Distance", "Attack reach distance")
            .setValue(4.0f).range(3.0f, 6.0f);

    ValueSetting hitThroughWalls = new ValueSetting("Through Walls", "How far you can hit through walls")
            .setValue(0.5f).range(0.0f, 2.0f);

    public Reach() {
        super("Reach", ModuleCategory.COMBAT);
        setup(reachDistance, hitThroughWalls);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;
        if (!mc.options.attackKey.isPressed()) return;

        // Получаем текущую цель
        HitResult hitResult = mc.crosshairTarget;
        if (!(hitResult instanceof EntityHitResult entityHit)) return;

        Entity target = entityHit.getEntity();
        if (target == null) return;

        // Проверяем дистанцию до цели
        double distance = mc.player.getPos().distanceTo(target.getPos());
        double maxReach = reachDistance.getValue() + hitThroughWalls.getValue();

        // Если цель вне обычной досягаемости, но в пределах рейча - атакуем
        if (distance > 3.0 && distance <= maxReach) {
            // Проверяем, не мешают ли стены (если Through Walls > 0)
            if (canHitThroughWalls(target, distance)) {
                mc.interactionManager.attackEntity(mc.player, target);
                mc.player.swingHand(mc.player.getActiveHand());
            }
        }
    }

    private boolean canHitThroughWalls(Entity target, double distance) {
        if (hitThroughWalls.getValue() <= 0) return true;

        // Простая проверка на наличие препятствий
        return distance <= 3.0 + hitThroughWalls.getValue();
    }

    // Метод для проверки доступности цели из других модулей (например, KillAura)
    public boolean canReach(Entity target) {
        if (!isState()) return false;

        double distance = mc.player.getPos().distanceTo(target.getPos());
        double maxReach = reachDistance.getValue() + hitThroughWalls.getValue();

        return distance <= maxReach && canHitThroughWalls(target, distance);
    }

    // Метод для получения текущего радиуса атаки
    public float getCurrentReach() {
        return isState() ? reachDistance.getValue() : 3.0f;
    }
}
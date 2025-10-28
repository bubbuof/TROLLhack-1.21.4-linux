package ru.zenith.implement.features.modules.combat;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.BooleanSetting;
import ru.zenith.api.feature.module.setting.implement.ValueSetting;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.other.StopWatch;
import ru.zenith.common.util.render.Render3DUtil;
import ru.zenith.implement.events.packet.PacketEvent;
import ru.zenith.implement.events.player.TickEvent;
import ru.zenith.implement.events.render.WorldRenderEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BackTrack extends Module {
    // Храним историю позиций врагов
    private final Map<Integer, List<Vec3d>> enemyPositionHistory = new ConcurrentHashMap<>();
    private final Map<Integer, StopWatch> enemyTimers = new ConcurrentHashMap<>();

    // Настройки
    private final BooleanSetting enabledSetting = new BooleanSetting("Enabled", "Enable backtrack hits")
            .setValue(true);

    private final ValueSetting backTrackTime = new ValueSetting("BackTrack Time", "How far to backtrack hit position (ms)")
            .setValue(100).range(50, 500);

    private final ValueSetting historySize = new ValueSetting("History Size", "Max positions to remember per enemy")
            .setValue(20).range(10, 50);

    private final ValueSetting recordInterval = new ValueSetting("Record Interval", "Position record interval (ms)")
            .setValue(50).range(10, 100);

    private final BooleanSetting visualize = new BooleanSetting("Visualize", "Show backtrack hitbox")
            .setValue(true);

    // Фейковая позиция для отрисовки и ударов
    private Vec3d fakeHitPosition;
    private Box fakeHitBox;
    private Integer currentTargetId;

    public BackTrack() {
        super("BackTrack", "Hit enemy's past server positions", ModuleCategory.COMBAT);
        setup(enabledSetting, backTrackTime, historySize, recordInterval, visualize);
    }

    @Override
    public void activate() {
        enemyPositionHistory.clear();
        enemyTimers.clear();
        fakeHitPosition = null;
        fakeHitBox = null;
        currentTargetId = null;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (!enabledSetting.isValue() || mc.world == null) return;

        // Записываем позиции всех врагов
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof LivingEntity living && isEnemy(living)) {
                recordEnemyPosition(living);
            }
        }
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (!enabledSetting.isValue()) return;

        // Перехватываем пакеты атаки и подменяем позицию
        if (event.isSend() && event.getPacket() instanceof PlayerInteractEntityC2SPacket attackPacket) {
            // Получаем entity по ID из пакета
            int entityId = getEntityIdFromPacket(attackPacket);
            Entity target = mc.world.getEntityById(entityId);

            if (target instanceof LivingEntity living && isEnemy(living)) {
                Vec3d backTrackPos = getBackTrackPosition(living);

                if (backTrackPos != null) {
                    // Устанавливаем фейковую позицию для отрисовки
                    fakeHitPosition = backTrackPos;
                    fakeHitBox = new Box(
                            backTrackPos.x - 0.3, backTrackPos.y, backTrackPos.z - 0.3,
                            backTrackPos.x + 0.3, backTrackPos.y + 1.8, backTrackPos.z + 0.3
                    );
                    currentTargetId = living.getId();

                    // Логика подмены пакета будет здесь
                    // modifyAttackPacket(attackPacket, backTrackPos);
                }
            }
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent event) {
        if (!enabledSetting.isValue() || !visualize.isValue() || fakeHitBox == null) return;

        // Отрисовываем фейковый хитбокс для удара
        Render3DUtil.drawBox(fakeHitBox, ColorUtil.getColor(255, 0, 0), 2);

        // Простая визуализация - линия от игрока к фейковой позиции
        if (fakeHitPosition != null && mc.player != null) {
            Render3DUtil.drawLine(
                    mc.player.getPos(), fakeHitPosition,
                    ColorUtil.getColor(255, 0, 0), 2.0f, false
            );
        }
    }

    private void recordEnemyPosition(LivingEntity enemy) {
        int enemyId = enemy.getId();
        List<Vec3d> history = enemyPositionHistory.computeIfAbsent(enemyId, k -> new ArrayList<>());
        StopWatch timer = enemyTimers.computeIfAbsent(enemyId, k -> new StopWatch());

        if (timer.finished(recordInterval.getInt())) {
            // Записываем текущую позицию врага
            history.add(enemy.getPos());

            // Ограничиваем размер истории
            while (history.size() > historySize.getInt()) {
                history.remove(0);
            }

            timer.reset();
        }
    }

    private Vec3d getBackTrackPosition(LivingEntity enemy) {
        List<Vec3d> history = enemyPositionHistory.get(enemy.getId());
        if (history == null || history.isEmpty()) return null;

        int targetTime = backTrackTime.getInt();
        int targetIndex = Math.max(0, history.size() - 1 - (targetTime / recordInterval.getInt()));

        return targetIndex < history.size() ? history.get(targetIndex) : null;
    }

    private boolean isEnemy(LivingEntity entity) {
        return entity != mc.player &&
                entity.isAlive() &&
                !entity.isRemoved() &&
                entity.distanceTo(mc.player) < 10; // Только ближние враги
    }

    // Получаем ID entity из пакета атаки
    private int getEntityIdFromPacket(PlayerInteractEntityC2SPacket packet) {
        // В 1.21.4 нужно использовать рефлексию или другой метод
        // Это упрощенная версия - в реальности нужно разбирать пакет
        try {
            // Временное решение - возвращаем 0, нужно будет доработать
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public void deactivate() {
        enemyPositionHistory.clear();
        enemyTimers.clear();
        fakeHitPosition = null;
        fakeHitBox = null;
        currentTargetId = null;
    }
}
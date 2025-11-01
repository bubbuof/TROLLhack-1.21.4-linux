package ru.zenith.implement.features.modules.player;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.*;
import ru.zenith.implement.events.player.TickEvent;
import ru.zenith.implement.events.player.InputEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.registry.entry.RegistryEntry;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoDodge extends Module {

    private static AutoDodge instance;

    public static AutoDodge getInstance() {
        if (instance == null) {
            instance = new AutoDodge();
        }
        return instance;
    }

    final SelectSetting mode = new SelectSetting("Mode", "Режим уклонения")
            .value("Auto", "Dodge", "Plast")
            .selected("Auto");

    final ValueSetting reactDistance = new ValueSetting("React Distance", "Дистанция реакции")
            .range(3.0f, 12.0f)
            .setValue(6.0f);

    final Map<Entity, Vec3d> predictedHits = new HashMap<>();
    long lastTrapResetMs = 0L;

    public AutoDodge() {
        super("AutoDodge", "AutoDodge", ModuleCategory.PLAYER);
        setup(mode, reactDistance);
        instance = this;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        predictedHits.clear();
        double scanRadius = 32.0;

        // Сканируем зелья
        for (PotionEntity potion : mc.world.getEntitiesByClass(
                PotionEntity.class,
                mc.player.getBoundingBox().expand(scanRadius),
                Objects::nonNull
        )) {
            ItemStack stack = getPotionStackSafe(potion);
            if (isHarmfulOnly(stack)) {
                Vec3d hit = predictImpactPoint(potion, 150);
                if (hit != null) {
                    predictedHits.put(potion, hit);
                }
            }
        }

        // Сканируем стрелы
        for (ArrowEntity arrow : mc.world.getEntitiesByClass(
                ArrowEntity.class,
                mc.player.getBoundingBox().expand(scanRadius),
                Objects::nonNull
        )) {
            ItemStack stack = getArrowStackSafe(arrow);
            if (isHarmfulOnly(stack)) {
                Vec3d hit = predictImpactPoint(arrow, 150);
                if (hit != null) {
                    predictedHits.put(arrow, hit);
                }
            }
        }
    }

    @EventHandler
    public void onInput(InputEvent event) {
        if (mc.player == null || predictedHits.isEmpty()) return;

        double maxDistance = reactDistance.getValue();

        for (Map.Entry<Entity, Vec3d> entry : predictedHits.entrySet()) {
            Vec3d hit = entry.getValue();
            if (hit != null) {
                double distance = mc.player.getPos().distanceTo(hit);
                if (distance <= maxDistance) {
                    switch (mode.getSelected()) {
                        case "Auto" -> {
                            if (!hasKelpCooldown() && distance > 3.0) {
                                useKelp();
                            } else {
                                Vec3d away = mc.player.getPos().subtract(hit).normalize();
                                applyMove(event, away);
                            }
                        }
                        case "Dodge" -> {
                            Vec3d away = mc.player.getPos().subtract(hit).normalize();
                            applyMove(event, away);
                        }
                        case "Plast" -> {
                            if (!hasKelpCooldown() && distance > 3.0) {
                                useKelp();
                            }
                        }
                    }
                    break;
                }
            }
        }
    }

    private void applyMove(InputEvent event, Vec3d direction) {
        Vec3d xz = new Vec3d(direction.x, 0.0, direction.z);
        if (xz.lengthSquared() > 0.000001) {
            // Устанавливаем движение напрямую через игрока
            mc.player.setVelocity(xz.x * 0.3, mc.player.getVelocity().y, xz.z * 0.3);
        }
    }

    private boolean hasKelpCooldown() {
        try {
            // Создаем ItemStack для проверки кулдауна
            ItemStack kelpStack = new ItemStack(Items.DRIED_KELP);
            return mc.player.getItemCooldownManager().isCoolingDown(kelpStack);
        } catch (Throwable e) {
            return false;
        }
    }

    private void useKelp() {
        try {
            // Простая реализация использования предмета
            int kelpSlot = findItemSlot(Items.DRIED_KELP);
            if (kelpSlot != -1) {
                int originalSlot = mc.player.getInventory().selectedSlot;
                mc.player.getInventory().selectedSlot = kelpSlot;
                mc.interactionManager.interactItem(mc.player, net.minecraft.util.Hand.MAIN_HAND);
                mc.player.getInventory().selectedSlot = originalSlot;
            }
        } catch (Throwable ignored) {
        }
    }

    private int findItemSlot(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private Vec3d predictImpactPoint(Entity entity, int steps) {
        Vec3d motion = entity.getVelocity();
        Vec3d pos = entity.getPos();

        for (int i = 0; i < steps; i++) {
            Vec3d prev = pos;
            pos = pos.add(motion);
            motion = motion.multiply(0.99, 0.99, 0.99);
            motion = motion.add(0.0, -0.05, 0.0);

            HitResult hitResult = mc.world.raycast(new RaycastContext(
                    prev, pos,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    entity
            ));

            if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
                return hitResult.getPos();
            }

            if (pos.y < mc.world.getBottomY()) {
                break;
            }
        }

        return null;
    }

    private ItemStack getPotionStackSafe(PotionEntity potion) {
        try {
            ItemStack stack = potion.getStack();
            return stack == null ? ItemStack.EMPTY : stack;
        } catch (Throwable e) {
            return ItemStack.EMPTY;
        }
    }

    private ItemStack getArrowStackSafe(ArrowEntity arrow) {
        try {
            Method method = arrow.getClass().getMethod("asItemStack");
            Object result = method.invoke(arrow);
            if (result instanceof ItemStack stack) {
                return stack;
            }
        } catch (Throwable ignored) {
        }

        try {
            Method method = arrow.getClass().getMethod("getItemStack");
            Object result = method.invoke(arrow);
            if (result instanceof ItemStack stack) {
                return stack;
            }
        } catch (Throwable ignored) {
        }

        try {
            Field field = arrow.getClass().getDeclaredField("item");
            field.setAccessible(true);
            Object result = field.get(arrow);
            if (result instanceof ItemStack stack) {
                return stack;
            }
        } catch (Throwable ignored) {
        }

        return ItemStack.EMPTY;
    }

    private boolean isHarmfulOnly(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        try {
            // Упрощенная проверка по типу предмета
            if (stack.getItem() instanceof net.minecraft.item.PotionItem ||
                    stack.getItem() instanceof net.minecraft.item.TippedArrowItem) {

                // Получаем эффекты через рефлексию или альтернативные методы
                List<StatusEffectInstance> effects = getPotionEffects(stack);

                int harmful = 0;
                int beneficial = 0;

                for (StatusEffectInstance effect : effects) {
                    if (effect != null && effect.getEffectType() != null) {
                        RegistryEntry<net.minecraft.entity.effect.StatusEffect> effectType = effect.getEffectType();
                        StatusEffectCategory category = effectType.value().getCategory();

                        if (category == StatusEffectCategory.HARMFUL) {
                            harmful++;
                        } else if (category == StatusEffectCategory.BENEFICIAL) {
                            beneficial++;
                        }
                    }
                }

                return harmful > 0 && beneficial == 0;
            }
            return false;
        } catch (Throwable e) {
            // Альтернативная простая проверка по названию
            return isHarmfulByName(stack);
        }
    }

    private List<StatusEffectInstance> getPotionEffects(ItemStack stack) {
        List<StatusEffectInstance> effects = new java.util.ArrayList<>();
        try {
            // Попробуем получить эффекты через PotionUtil
            Class<?> potionUtilClass = Class.forName("net.minecraft.potion.PotionUtil");
            Method method = potionUtilClass.getMethod("getPotionEffects", ItemStack.class);
            effects = (List<StatusEffectInstance>) method.invoke(null, stack);
        } catch (Exception e) {
            try {
                // Альтернативный метод
                Class<?> potionUtilClass = Class.forName("net.minecraft.potion.PotionUtil");
                Method method = potionUtilClass.getMethod("getCustomPotionEffects", ItemStack.class);
                effects = (List<StatusEffectInstance>) method.invoke(null, stack);
            } catch (Exception e2) {
                // Если не получилось, возвращаем пустой список
            }
        }
        return effects;
    }

    private boolean isHarmfulByName(ItemStack stack) {
        // Простая проверка по названию предмета
        try {
            String itemName = stack.getItem().getName().getString().toLowerCase();
            return itemName.contains("poison") ||
                    itemName.contains("harmful") ||
                    itemName.contains("slowness") ||
                    itemName.contains("weakness") ||
                    itemName.contains("instant damage") ||
                    itemName.contains("decay") ||
                    itemName.contains("wither");
        } catch (Exception e) {
            // Альтернатива через toString
            String itemName = stack.getItem().toString().toLowerCase();
            return itemName.contains("poison") ||
                    itemName.contains("slowness") ||
                    itemName.contains("weakness") ||
                    itemName.contains("damage");
        }
    }
}
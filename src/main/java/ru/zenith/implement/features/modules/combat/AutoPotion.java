package ru.zenith.implement.features.modules.combat;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.*;
import ru.zenith.common.util.other.StopWatch;
import ru.zenith.implement.events.player.TickEvent;

import java.util.Arrays;
import java.util.List;

public class AutoPotion extends Module {

    // Настройки
    private final BooleanSetting strength = new BooleanSetting("Сила", "Автоматически кидать зелье силы")
            .setValue(true);
    private final BooleanSetting speed = new BooleanSetting("Скорость", "Автоматически кидать зелье скорости")
            .setValue(true);
    private final BooleanSetting fireResistance = new BooleanSetting("Огнезащита", "Автоматически кидать зелье огнезащиты")
            .setValue(true);
    private final BooleanSetting healing = new BooleanSetting("Исцеление", "Автоматически кидать зелье исцеления")
            .setValue(true);
    private final BooleanSetting regeneration = new BooleanSetting("Регенерация", "Автоматически кидать зелье регенерации")
            .setValue(true);

    private final ValueSetting healthThreshold = new ValueSetting("Здоровье", "Порог здоровья для исцеления")
            .setValue(8f).range(0f, 20f);
    private final ValueSetting throwDelay = new ValueSetting("Задержка", "Задержка между бросками зелий")
            .setValue(1000f).range(500f, 5000f);
    private final BooleanSetting onlyGround = new BooleanSetting("Только на земле", "Работать только когда стоишь на земле")
            .setValue(true);
    private final SelectSetting searchMode = new SelectSetting("Режим поиска", "Способ поиска зелий в инвентаре")
            .value("Эффект", "Имя").selected("Эффект");

    private final StopWatch stopWatch = new StopWatch();
    private final List<BooleanSetting> potionSettings;

    public AutoPotion() {
        super("AutoPotion", "AutoPotion", ModuleCategory.COMBAT);

        // Список настроек зелий для удобного доступа
        potionSettings = Arrays.asList(strength, speed, fireResistance, healing, regeneration);

        // Регистрация настроек
        setup(
                strength, speed, fireResistance, healing, regeneration,
                healthThreshold, throwDelay, onlyGround, searchMode
        );
    }

    @Override
    public void activate() {
        stopWatch.reset();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (onlyGround.isValue() && !mc.player.isOnGround()) return;
        if (mc.player.age <= 80) return;
        if (!stopWatch.every((long) throwDelay.getValue())) return;

        checkAndThrowPotions();
    }

    private void checkAndThrowPotions() {
        // Проверяем зелья в порядке приоритета
        if (speed.isValue() && shouldThrowSpeed()) {
            throwPotion(PotionType.SPEED);
            return;
        }
        if (strength.isValue() && shouldThrowStrength()) {
            throwPotion(PotionType.STRENGTH);
            return;
        }
        if (fireResistance.isValue() && shouldThrowFireRes()) {
            throwPotion(PotionType.FIRE_RESISTANCE);
            return;
        }
        if (healing.isValue() && shouldThrowHeal()) {
            throwPotion(PotionType.HEALING);
            return;
        }
        if (regeneration.isValue() && shouldThrowRegen()) {
            throwPotion(PotionType.REGENERATION);
        }
    }

    private boolean shouldThrowSpeed() {
        return !mc.player.hasStatusEffect(StatusEffects.SPEED) && isPotionOnHotBar(PotionType.SPEED);
    }

    private boolean shouldThrowStrength() {
        return !mc.player.hasStatusEffect(StatusEffects.STRENGTH) && isPotionOnHotBar(PotionType.STRENGTH);
    }

    private boolean shouldThrowFireRes() {
        return !mc.player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE) && isPotionOnHotBar(PotionType.FIRE_RESISTANCE);
    }

    private boolean shouldThrowHeal() {
        double currentHealth = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        return currentHealth < healthThreshold.getValue() && isPotionOnHotBar(PotionType.HEALING);
    }

    private boolean shouldThrowRegen() {
        return !mc.player.hasStatusEffect(StatusEffects.REGENERATION) && isPotionOnHotBar(PotionType.REGENERATION);
    }

    private int getPotionSlot(PotionType potion) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (isStackPotion(stack, potion)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isPotionOnHotBar(PotionType potion) {
        return getPotionSlot(potion) != -1;
    }

    private boolean isStackPotion(ItemStack stack, PotionType potion) {
        if (stack == null || stack.isEmpty() || stack.getItem() != Items.SPLASH_POTION) {
            return false;
        }

        if (searchMode.isSelected("Эффект")) {
            PotionContentsComponent potionComponent = stack.get(DataComponentTypes.POTION_CONTENTS);
            if (potionComponent == null) return false;

            for (StatusEffectInstance effect : potionComponent.getEffects()) {
                if (effect.getEffectType() == potion.getEffect()) {
                    return true;
                }
            }
            return false;
        } else {
            String stackName = stack.getName().getString().toLowerCase();
            return stackName.contains("зелье") && stackName.contains(potion.getRussianName());
        }
    }

    private void throwPotion(PotionType potion) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        int slot = getPotionSlot(potion);
        if (slot == -1) return;

        int prevSlot = mc.player.getInventory().selectedSlot;
        float prevPitch = mc.player.getPitch();
        float prevYaw = mc.player.getYaw();

        // Смотрим вниз для броска
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                prevYaw,
                90f,
                mc.player.isOnGround(),
                false
        ));

        // Меняем слот
        mc.player.getInventory().selectedSlot = slot;
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));

        // Кидаем зелье
        mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(
                Hand.MAIN_HAND,
                getWorldActionId(),
                prevYaw,
                90f
        ));

        // Возвращаем обратно
        mc.player.getInventory().selectedSlot = prevSlot;
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(prevSlot));

        // Возвращаем взгляд
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                prevYaw,
                prevPitch,
                mc.player.isOnGround(),
                false
        ));

        stopWatch.reset();
    }

    private int getWorldActionId() {
        return mc.world != null ? (int) mc.world.getTime() : 0;
    }

    private enum PotionType {
        STRENGTH(StatusEffects.STRENGTH, "сил"),
        SPEED(StatusEffects.SPEED, "скор"),
        FIRE_RESISTANCE(StatusEffects.FIRE_RESISTANCE, "огне"),
        HEALING(StatusEffects.INSTANT_HEALTH, "исц"),
        REGENERATION(StatusEffects.REGENERATION, "реген");

        private final RegistryEntry<StatusEffect> effect;
        private final String russianName;

        PotionType(RegistryEntry<StatusEffect> effect, String russianName) {
            this.effect = effect;
            this.russianName = russianName;
        }

        public RegistryEntry<StatusEffect> getEffect() {
            return effect;
        }

        public String getRussianName() {
            return russianName;
        }
    }
}
package ru.zenith.implement.features.modules.combat;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.BooleanSetting;
import ru.zenith.api.feature.module.setting.implement.ValueSetting;
import ru.zenith.implement.events.player.TickEvent;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoBuff extends Module {

    final BooleanSetting strength = new BooleanSetting("Strength", "Auto use strength potions")
            .setValue(true);

    final BooleanSetting speed = new BooleanSetting("Speed", "Auto use speed potions")
            .setValue(true);

    final BooleanSetting fireResistance = new BooleanSetting("FireResistance", "Auto use fire resistance potions")
            .setValue(true);

    final BooleanSetting instantHealing = new BooleanSetting("InstantHealing", "Auto use instant healing potions")
            .setValue(true);

    final ValueSetting healthH = new ValueSetting("Health", "Health threshold for healing")
            .setValue(8).range(0, 20);

    final BooleanSetting regeneration = new BooleanSetting("Regeneration", "Auto use regeneration potions")
            .setValue(true);

    final BooleanSetting triggerOnHealth = new BooleanSetting("TriggerOnHealth", "Trigger regeneration on health")
            .setValue(false);

    final ValueSetting healthR = new ValueSetting("HP", "Health threshold for regeneration")
            .setValue(8).range(0, 20);

    final BooleanSetting onlyOnGround = new BooleanSetting("OnlyOnGround", "Only use potions when on ground")
            .setValue(true);

    final BooleanSetting pauseAura = new BooleanSetting("PauseAura", "Pause aura when using potions")
            .setValue(false);

    long lastUseTime;
    boolean spoofed = false;

    public AutoBuff() {
        super("AutoBuff", "AutoBuff", ModuleCategory.COMBAT);
        setup(strength, speed, fireResistance, instantHealing, healthH, regeneration, triggerOnHealth, healthR, onlyOnGround, pauseAura);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (onlyOnGround.isValue() && !mc.player.isOnGround()) return;

        // Спуфим питч для броска зелья
        if (mc.player.age > 80 && shouldThrow() && !spoofed) {
            mc.player.setPitch(90);
            spoofed = true;
            return;
        }

        // Использование зелий
        if (mc.player.age > 80 && shouldThrow() && System.currentTimeMillis() - lastUseTime >= 1000 && spoofed) {
            if (!mc.player.hasStatusEffect(StatusEffects.SPEED) && isPotionOnHotBar(PotionType.SPEED) && speed.isValue())
                throwPotion(PotionType.SPEED);

            if (!mc.player.hasStatusEffect(StatusEffects.STRENGTH) && isPotionOnHotBar(PotionType.STRENGTH) && strength.isValue())
                throwPotion(PotionType.STRENGTH);

            if (!mc.player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE) && isPotionOnHotBar(PotionType.FIRE_RESISTANCE) && fireResistance.isValue())
                throwPotion(PotionType.FIRE_RESISTANCE);

            if ((mc.player.getHealth() + mc.player.getAbsorptionAmount()) < healthH.getValue() && instantHealing.isValue() && isPotionOnHotBar(PotionType.INSTANT_HEALTH))
                throwPotion(PotionType.INSTANT_HEALTH);

            if (((!mc.player.hasStatusEffect(StatusEffects.REGENERATION) && !triggerOnHealth.isValue()) ||
                    ((mc.player.getHealth() + mc.player.getAbsorptionAmount()) < healthR.getValue() && triggerOnHealth.isValue())) &&
                    regeneration.isValue() && isPotionOnHotBar(PotionType.REGENERATION))
                throwPotion(PotionType.REGENERATION);

            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
            lastUseTime = System.currentTimeMillis();
            spoofed = false;
        }
    }

    private boolean shouldThrow() {
        return (!mc.player.hasStatusEffect(StatusEffects.SPEED) && isPotionOnHotBar(PotionType.SPEED) && speed.isValue()) ||
                (!mc.player.hasStatusEffect(StatusEffects.STRENGTH) && isPotionOnHotBar(PotionType.STRENGTH) && strength.isValue()) ||
                (!mc.player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE) && isPotionOnHotBar(PotionType.FIRE_RESISTANCE) && fireResistance.isValue()) ||
                ((mc.player.getHealth() + mc.player.getAbsorptionAmount()) < healthH.getValue() && isPotionOnHotBar(PotionType.INSTANT_HEALTH) && instantHealing.isValue()) ||
                ((!mc.player.hasStatusEffect(StatusEffects.REGENERATION) && !triggerOnHealth.isValue()) ||
                        ((mc.player.getHealth() + mc.player.getAbsorptionAmount()) < healthR.getValue() && triggerOnHealth.isValue())) &&
                        isPotionOnHotBar(PotionType.REGENERATION) && regeneration.isValue();
    }

    public static int getPotionSlot(PotionType potion) {
        for (int i = 0; i < 9; ++i) {
            if (isStackPotion(mc.player.getInventory().getStack(i), potion))
                return i;
        }
        return -1;
    }

    public static boolean isPotionOnHotBar(PotionType potion) {
        return getPotionSlot(potion) != -1;
    }

    public static boolean isStackPotion(ItemStack stack, PotionType potion) {
        if (stack.isEmpty()) return false;

        if (stack.getItem() instanceof SplashPotionItem) {
            PotionContentsComponent potionContentsComponent = stack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);

            RegistryEntry<StatusEffect> effect = null;

            switch (potion) {
                case STRENGTH -> effect = StatusEffects.STRENGTH;
                case SPEED -> effect = StatusEffects.SPEED;
                case FIRE_RESISTANCE -> effect = StatusEffects.FIRE_RESISTANCE;
                case INSTANT_HEALTH -> effect = StatusEffects.INSTANT_HEALTH;
                case REGENERATION -> effect = StatusEffects.REGENERATION;
            }

            for (StatusEffectInstance effectInstance : potionContentsComponent.getEffects()) {
                if (effectInstance.getEffectType().equals(effect))
                    return true;
            }
        }
        return false;
    }

    public void throwPotion(PotionType potion) {
        // Пауза Aura если нужно
        if (pauseAura.isValue()) {
            // Здесь нужно добавить логику паузы Aura модуля
            // Например: Aura.getInstance().setPaused(true);
        }

        int potionSlot = getPotionSlot(potion);
        if (potionSlot != -1) {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(potionSlot));
            mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, mc.player.getYaw(), mc.player.getPitch()));
        }
    }

    @Getter
    public enum PotionType {
        STRENGTH,
        SPEED,
        FIRE_RESISTANCE,
        INSTANT_HEALTH,
        REGENERATION
    }

    @Override
    public void activate() {
        super.activate();
        lastUseTime = System.currentTimeMillis();
        spoofed = false;
    }

    @Override
    public void deactivate() {
        super.deactivate();
        // Сброс паузы Aura если нужно
        if (pauseAura.isValue()) {
            // Aura.getInstance().setPaused(false);
        }
    }
}
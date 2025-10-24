package ru.zenith.implement.features.modules.misc;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.ValueSetting;
import ru.zenith.common.util.entity.MovingUtil;
import ru.zenith.implement.events.player.TickEvent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Speed extends Module {

    ValueSetting baseSpeedSetting = new ValueSetting("Base Speed", "Base movement speed multiplier")
            .setValue(0.36f).range(0.1f, 1.0f);
    ValueSetting speedBoostSetting = new ValueSetting("Speed Boost", "Speed boost multiplier for Speed II effect")
            .setValue(1.155f).range(1.0f, 2.0f);
    ValueSetting melonBoostSetting = new ValueSetting("Melon Boost", "Speed boost when holding melon slice")
            .setValue(0.41755f).range(0.1f, 1.0f);
    ValueSetting airMultiplierSetting = new ValueSetting("Air Multiplier", "Speed multiplier when in air")
            .setValue(1.435f).range(1.0f, 3.0f);
    ValueSetting slownessReductionSetting = new ValueSetting("Slowness Reduction", "Speed reduction when having slowness effect")
            .setValue(0.835f).range(0.1f, 1.0f);

    public Speed() {
        super("Speed", "Speed", ModuleCategory.MISC);
        setup(baseSpeedSetting, speedBoostSetting, melonBoostSetting, airMultiplierSetting, slownessReductionSetting);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        ItemStack offHandItem = mc.player.getOffHandStack();
        StatusEffectInstance speedEffect = mc.player.getStatusEffect(StatusEffects.SPEED);
        StatusEffectInstance slownessEffect = mc.player.getStatusEffect(StatusEffects.SLOWNESS);
        String itemName = offHandItem.getName().getString();

        float appliedSpeed = 0;
        float baseSpeed = baseSpeedSetting.getValue();

        if (speedEffect != null) {
            if (speedEffect.getAmplifier() == 2) {
                appliedSpeed = baseSpeed * speedBoostSetting.getValue();
                if (itemName.contains("Ломтик Дыни") || itemName.contains("Melon Slice")) {
                    appliedSpeed = melonBoostSetting.getValue();
                }
            } else if (speedEffect.getAmplifier() == 1) {
                appliedSpeed = baseSpeed;
            }
        } else {
            appliedSpeed = baseSpeed * 0.68f;
        }

        if (slownessEffect != null) {
            appliedSpeed *= slownessReductionSetting.getValue();
        }

        if (!mc.player.isOnGround()) {
            appliedSpeed *= airMultiplierSetting.getValue();
        }

        // Only apply speed if player is moving
        if (MovingUtil.hasPlayerMovement()) {
            MovingUtil.setVelocity(appliedSpeed);
        }
    }
}

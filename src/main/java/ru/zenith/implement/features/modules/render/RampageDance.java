package ru.zenith.implement.features.modules.render;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.GroupSetting;
import ru.zenith.api.feature.module.setting.implement.SelectSetting;
import ru.zenith.api.feature.module.setting.implement.ValueSetting;
import ru.zenith.implement.events.player.TickEvent;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class RampageDance extends Module {

    private static RampageDance instance;

    public SelectSetting danceStyle = new SelectSetting("Dance Style", "Select the style of Rampage dance")
            .value("Classic", "Aggressive", "Smooth", "Crazy", "Twerk");

    ValueSetting danceSpeed = new ValueSetting("Dance Speed", "Speed of the dance animation")
            .setValue(1.0f).range(0.1f, 3.0f);

    ValueSetting armIntensity = new ValueSetting("Arm Intensity", "Intensity of arm movements")
            .setValue(1.0f).range(0.1f, 2.0f);

    ValueSetting legIntensity = new ValueSetting("Leg Intensity", "Intensity of leg movements")
            .setValue(0.8f).range(0.1f, 2.0f);

    ValueSetting bodyRotation = new ValueSetting("Body Rotation", "Body rotation intensity")
            .setValue(0.5f).range(0.0f, 1.5f);

    GroupSetting animationSettings = new GroupSetting("Animation", "Dance animation settings")
            .settings(danceStyle, danceSpeed, armIntensity, legIntensity, bodyRotation).setValue(true);

    boolean dancing = false;
    float danceTick = 0f;

    public RampageDance() {
        super("RampageDance", "Rampage Dance", ModuleCategory.RENDER);
        setup(animationSettings);
        instance = this;
    }

    public static RampageDance getInstance() {
        return instance;
    }

    @Override
    public void activate() {
        dancing = true;
        danceTick = 0f;
        
        // Play dance start sound
        if (mc.player != null && mc.world != null) {
            mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.5f));
        }
    }

    @Override
    public void deactivate() {
        dancing = false;
        danceTick = 0f;
        
        // Play dance stop sound
        if (mc.player != null && mc.world != null) {
            mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_BASS, 0.8f));
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (!dancing || mc.player == null || mc.world == null) return;

        danceTick += 0.1f * danceSpeed.getValue();
        
        // Reset tick counter to prevent overflow
        if (danceTick > 1000f) {
            danceTick = 0f;
        }
    }

    public float getRightArmPitch() {
        return 0;
    }

    public float getLeftArmPitch() {
        if (!dancing) return 0f;
        
        float baseMovement = MathHelper.sin(danceTick * 4f + (float)Math.PI) * armIntensity.getValue();
        
        return switch (danceStyle.getSelected()) {
            case "Classic" -> baseMovement * 1.2f;
            case "Aggressive" -> MathHelper.sin(danceTick * 6f + (float)Math.PI) * armIntensity.getValue() * 1.5f;
            case "Smooth" -> MathHelper.sin(danceTick * 2f + (float)Math.PI) * armIntensity.getValue() * 0.8f;
            case "Crazy" -> MathHelper.sin(danceTick * 8f + (float)Math.PI + MathHelper.cos(danceTick * 3f)) * armIntensity.getValue() * 1.8f;
            case "Twerk" -> MathHelper.sin(danceTick * 5f + (float)Math.PI) * armIntensity.getValue() * 0.6f;
            default -> baseMovement;
        };
    }

    public float getRightLegPitch() {
        if (!dancing) return 0f;
        
        float baseMovement = MathHelper.cos(danceTick * 2f) * legIntensity.getValue();
        
        return switch (danceStyle.getSelected()) {
            case "Classic" -> baseMovement * 0.8f;
            case "Aggressive" -> MathHelper.cos(danceTick * 4f) * legIntensity.getValue() * 1.2f;
            case "Smooth" -> MathHelper.cos(danceTick * 1.5f) * legIntensity.getValue() * 0.6f;
            case "Crazy" -> MathHelper.cos(danceTick * 6f + MathHelper.sin(danceTick * 2f)) * legIntensity.getValue() * 1.4f;
            case "Twerk" -> MathHelper.cos(danceTick * 8f) * legIntensity.getValue() * 1.6f;
            default -> baseMovement;
        };
    }

    public float getLeftLegPitch() {
        if (!dancing) return 0f;
        
        float baseMovement = MathHelper.cos(danceTick * 2f + (float)Math.PI) * legIntensity.getValue();
        
        return switch (danceStyle.getSelected()) {
            case "Classic" -> baseMovement * 0.8f;
            case "Aggressive" -> MathHelper.cos(danceTick * 4f + (float)Math.PI) * legIntensity.getValue() * 1.2f;
            case "Smooth" -> MathHelper.cos(danceTick * 1.5f + (float)Math.PI) * legIntensity.getValue() * 0.6f;
            case "Crazy" -> MathHelper.cos(danceTick * 6f + (float)Math.PI + MathHelper.sin(danceTick * 2f)) * legIntensity.getValue() * 1.4f;
            case "Twerk" -> MathHelper.cos(danceTick * 8f + (float)Math.PI) * legIntensity.getValue() * 1.6f;
            default -> baseMovement;
        };
    }

    public float getBodyYaw() {
        if (!dancing) return 0f;
        
        float baseRotation = MathHelper.sin(danceTick * 1.5f) * bodyRotation.getValue() * 20f;
        
        return switch (danceStyle.getSelected()) {
            case "Classic" -> baseRotation;
            case "Aggressive" -> MathHelper.sin(danceTick * 3f) * bodyRotation.getValue() * 30f;
            case "Smooth" -> MathHelper.sin(danceTick * 1f) * bodyRotation.getValue() * 15f;
            case "Crazy" -> MathHelper.sin(danceTick * 4f + MathHelper.cos(danceTick * 2f)) * bodyRotation.getValue() * 40f;
            case "Twerk" -> MathHelper.sin(danceTick * 6f) * bodyRotation.getValue() * 25f;
            default -> baseRotation;
        };
    }

    public float getHeadPitch() {
        if (!dancing) return 0f;
        
        return switch (danceStyle.getSelected()) {
            case "Classic" -> MathHelper.sin(danceTick * 3f) * 0.3f;
            case "Aggressive" -> MathHelper.sin(danceTick * 5f) * 0.5f;
            case "Smooth" -> MathHelper.sin(danceTick * 1.5f) * 0.2f;
            case "Crazy" -> MathHelper.sin(danceTick * 7f + MathHelper.cos(danceTick * 4f)) * 0.7f;
            case "Twerk" -> MathHelper.sin(danceTick * 4f) * 0.4f;
            default -> 0f;
        };
    }

    public boolean isDancing() {
        return dancing;
    }
}

package ru.zenith.implement.features.modules.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.CrossbowItem;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.GroupSetting;
import ru.zenith.api.feature.module.setting.implement.SelectSetting;
import ru.zenith.api.feature.module.setting.implement.ValueSetting;
import ru.zenith.implement.events.item.HandAnimationEvent;
import ru.zenith.implement.events.item.HandOffsetEvent;
import ru.zenith.implement.events.item.SwingDurationEvent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HandTweaks extends Module {

    SelectSetting swingType = new SelectSetting("Swing Type", "Select the type of swing")
            .value("Swipe", "Down", "Smooth", "Power", "Feast");

    ValueSetting mainHandXSetting = new ValueSetting("Main Hand X", "Main Hand X value setting")
            .setValue(0.0F).range(-1.0F, 1.0F);

    ValueSetting mainHandYSetting = new ValueSetting("Main Hand Y", "Main Hand Y value setting")
            .setValue(0.0F).range(-1.0F, 1.0F);

    ValueSetting mainHandZSetting = new ValueSetting("Main Hand Z", "Main Hand Z value setting")
            .setValue(0.0F).range(-2.5F, 2.5F);

    ValueSetting offHandXSetting = new ValueSetting("Off Hand X", "Off Hand X value setting")
            .setValue(0.0F).range(-1.0F, 1.0F);

    ValueSetting offHandYSetting = new ValueSetting("Off Hand Y", "Off Hand Y value setting")
            .setValue(0.0F).range(-1.0F, 1.0F);

    ValueSetting offHandZSetting = new ValueSetting("Off Hand Z", "Off Hand Z value setting")
            .setValue(0.0F).range(-2.5F, 2.5F);

    GroupSetting swingGroup = new GroupSetting("Animation", "Custom Swing")
            .settings(swingType).setValue(true);

    GroupSetting offsetGroup = new GroupSetting("Offsets", "Custom Hands offset")
            .settings(mainHandXSetting, mainHandYSetting, mainHandZSetting, offHandXSetting, offHandYSetting, offHandZSetting).setValue(true);

    ValueSetting swingSpeedSetting = new ValueSetting("Swing Duration",  "Duration of the hit animation")
            .setValue(1.0F).range(0.5F, 2.0F);

    public HandTweaks() {
        super("HandTweaks", "Hand Tweaks", ModuleCategory.RENDER);
        setup(swingGroup, offsetGroup, swingSpeedSetting);
    }

    @EventHandler
    public void onSwingDuration(SwingDurationEvent e) {
        e.setAnimation(swingSpeedSetting.getValue());
        e.cancel();
    }

    @EventHandler
    public void onHandAnimation(HandAnimationEvent e) {
        if (e.getHand().equals(Hand.MAIN_HAND) && swingGroup.isValue()) {
            MatrixStack matrix = e.getMatrices();
            float swingProgress = e.getSwingProgress();
            int i = mc.player.getMainArm().equals(Arm.RIGHT) ? 1 : -1;
            float sin1 = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
            float sin2 = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
            float sinSmooth = (float) (Math.sin(swingProgress * Math.PI) * 0.5F);
            switch (swingType.getSelected()) {
                case "Swipe" -> {
                    matrix.translate(0.56F * i, -0.32F, -0.72F);
                    matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(60 * i));
                    matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-60 * i));
                    matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((sin2 * sin1) * -5));
                    matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees((sin2 * sin1) * -120));
                    matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60));
                }
                case "Down" -> {
                    matrix.translate(i * 0.56F, -0.32F, -0.72F);
                    matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(76 * i));
                    matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * -5));
                    matrix.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(sin2 * -100));
                    matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -155));
                    matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100));
                }
                case "Smooth" -> {
                    matrix.translate(i * 0.56F, -0.42F, -0.72F);
                    matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * (45.0F + sin1 * -20.0F)));
                    matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) i * sin2 * -20.0F));
                    matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -80.0F));
                    matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * -45.0F));
                    matrix.translate(0, -0.1, 0);
                }
                case "Power" -> {
                    matrix.translate(i * 0.56F, -0.32F, -0.72F);
                    matrix.translate((-sinSmooth * sinSmooth * sin1) * i, 0, 0);
                    matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(61 * i));
                    matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sin2));
                    matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((sin2 * sin1) * -5));
                    matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees((sin2 * sin1) * -30));
                    matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60));
                    matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sinSmooth * -60));
                }
                case "Feast" -> {
                    matrix.translate(i * 0.56F, -0.32F, -0.72F);
                    matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30 * i));
                    matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * 75 * i));
                    matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -45));
                    matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30 * i));
                    matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80));
                    matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(35 * i));
                }
            }
            e.cancel();
        }
    }

    @EventHandler
    public void onHandOffset(HandOffsetEvent e) {
        Hand hand = e.getHand();
        if (hand.equals(Hand.MAIN_HAND) && e.getStack().getItem() instanceof CrossbowItem) return;

        if (offsetGroup.isValue()) {
            MatrixStack matrix = e.getMatrices();
            if (hand.equals(Hand.MAIN_HAND)) matrix.translate(mainHandXSetting.getValue(), mainHandYSetting.getValue(), mainHandZSetting.getValue());
            else matrix.translate(offHandXSetting.getValue(), offHandYSetting.getValue(), offHandZSetting.getValue());
        }
    }
}

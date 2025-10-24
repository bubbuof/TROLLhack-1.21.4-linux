package ru.zenith.implement.screens.menu.components.implement.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.zenith.api.feature.module.setting.implement.ValueSetting;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.common.util.other.StringUtil;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

import static ru.zenith.api.system.font.Fonts.Type.BOLD;

public class ValueComponent extends AbstractSettingComponent {
    public static final int SLIDER_WIDTH = 45;

    private final ValueSetting setting;

    private boolean dragging;
    private double animation;

    public ValueComponent(ValueSetting setting) {
        super(setting);
        this.setting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();

        String wrapped = StringUtil.wrap(setting.getDescription(), 70, 12);
        height = (int) (18 + Fonts.getSize(12).getStringHeight(wrapped) / 3);

        String value = String.valueOf(setting.getValue());

        // GameSense/Skeet.cc style value display
        Fonts.getSize(12, BOLD).drawString(matrix, value, x + width - 9 - Fonts.getSize(12).getStringWidth(value), y + 8, ColorUtil.getSkeetAccent());

        changeValue(getDifference(mouseX, matrix));

        // GameSense/Skeet.cc style text colors
        Fonts.getSize(14, BOLD).drawString(matrix, setting.getName(), x + 9, y + 6, ColorUtil.getSkeetText());
        Fonts.getSize(12).drawString(matrix, wrapped, x + 9, y + 15, ColorUtil.getSkeetTextSecondary());
    }

    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        dragging = MathUtil.isHovered(mouseX, mouseY, x + width - SLIDER_WIDTH - 9, y + 13, SLIDER_WIDTH, 4) && button == 0;
        return super.mouseClicked(mouseX, mouseY, button);
    }


    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private float getDifference(int mouseX, MatrixStack matrix) {
        float percentValue = SLIDER_WIDTH * (setting.getValue() - setting.getMin()) / (setting.getMax() - setting.getMin()),
                difference = MathHelper.clamp(mouseX - (x + width - SLIDER_WIDTH - 9), 0, SLIDER_WIDTH);

        animation = MathUtil.interpolate(animation, percentValue);

        // GameSense/Skeet.cc style slider track
        rectangle.render(ShapeProperties.create(matrix, x + width - SLIDER_WIDTH - 9, y + 15, SLIDER_WIDTH, 2)
                .round(0).color(ColorUtil.getSkeetBorder()).build());

        // GameSense/Skeet.cc style slider fill
        rectangle.render(ShapeProperties.create(matrix, x + width - SLIDER_WIDTH - 9, y + 15, (float) animation, 2)
                .round(0).color(ColorUtil.getSkeetAccent()).build());

        // GameSense/Skeet.cc style slider handle
        float v = MathHelper.clamp((float) (x + width - SLIDER_WIDTH + animation), 0, x + width - 4);
        rectangle.render(ShapeProperties.create(matrix, v - 10, y + 13F, 4, 6)
                .round(0).color(ColorUtil.getSkeetText()).build());

        return difference;
    }

    
    private void changeValue(float difference) {
        BigDecimal bd = BigDecimal.valueOf((difference / SLIDER_WIDTH) * (setting.getMax() - setting.getMin()) + setting.getMin())
                .setScale(2, RoundingMode.HALF_UP);

        if (dragging) {
            float value = difference == 0 ? setting.getMin() : bd.floatValue();
            if (setting.isInteger()) value = (int) value;
            setting.setValue(value);
        }
    }
}

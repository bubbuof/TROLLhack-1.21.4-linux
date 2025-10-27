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

        height = 28;

        String value = String.valueOf(setting.getValue());

        Fonts.getSize(14, BOLD).drawString(matrix, setting.getName(), (int) (x + 6), (int) (y + 6), 0xFFD4D6E1);
        Fonts.getSize(12, BOLD).drawString(matrix, value, (int) (x + width - 6 - Fonts.getSize(12).getStringWidth(value)), (int) (y + 7), ColorUtil.getClientColor());

        changeValue(getDifference(mouseX, matrix));
    }

    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float trackX = x + 3;
        float trackW = width - 6;
        dragging = MathUtil.isHovered(mouseX, mouseY, trackX, y + 18, trackW, 4) && button == 0;
        return super.mouseClicked(mouseX, mouseY, button);
    }


    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private float getDifference(int mouseX, MatrixStack matrix) {
        float trackX = x + 6;
        float trackW = width - 12;

        float percentValue = trackW * (setting.getValue() - setting.getMin()) / (setting.getMax() - setting.getMin());
        float difference = MathHelper.clamp(mouseX - trackX, 0, trackW);

        animation = MathUtil.interpolate(animation, percentValue);

        rectangle.render(ShapeProperties.create(matrix, trackX, y + 20, trackW, 1)
                .color(0x2D2E414D).build());

        rectangle.render(ShapeProperties.create(matrix, trackX, y + 20, (float) animation, 1)
                .color(ColorUtil.getClientColor(), ColorUtil.getClientColor(), new Color(ColorUtil.getClientColor()).darker().getRGB(), new Color(ColorUtil.getClientColor()).darker().getRGB()).build());

        float v = MathHelper.clamp(trackX + (float) animation, trackX, trackX + trackW);
        rectangle.render(ShapeProperties.create(matrix, v - 2.5F, y + 18.5F, 5, 5)
                .round(2.5F).color(ColorUtil.getMainGuiColor()).build());

        rectangle.render(ShapeProperties.create(matrix, v - 1.8F, y + 19.2F, 3.6F, 3.6F)
                .round(1.8F).color(ColorUtil.getClientColor()).build());

        return difference;
    }

    
    private void changeValue(float difference) {
        float trackW = width - 12;
        BigDecimal bd = BigDecimal.valueOf((difference / trackW) * (setting.getMax() - setting.getMin()) + setting.getMin())
                .setScale(2, RoundingMode.HALF_UP);

        if (dragging) {
            float value = difference == 0 ? setting.getMin() : bd.floatValue();
            if (setting.isInteger()) value = (int) value;
            setting.setValue(value);
        }
    }
}

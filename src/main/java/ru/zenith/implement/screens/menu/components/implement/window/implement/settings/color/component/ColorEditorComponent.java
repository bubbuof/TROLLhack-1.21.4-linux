package ru.zenith.implement.screens.menu.components.implement.window.implement.settings.color.component;

import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.zenith.api.feature.module.setting.implement.ColorSetting;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.implement.screens.menu.components.AbstractComponent;

@RequiredArgsConstructor
public class ColorEditorComponent extends AbstractComponent {
    private final ColorSetting setting;


    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();

        rectangle.render(ShapeProperties.create(matrix, x + 6, y + 90.5F, 31, 14)
                .round(1.5F).thickness(2).outlineColor(ColorUtil.getOutline(0.8F)).color(ColorUtil.getRect(1)).build());

        Fonts.getSize(13).drawString(context.getMatrices(), "HEX", x + 10, y + 96, -1);

        rectangle.render(ShapeProperties.create(matrix, x + 40, y + 90.5F, 80, 14)
                .round(1.5F).thickness(2).outlineColor(ColorUtil.getOutline(0.8F)).color(ColorUtil.getRect(1)).build());

        Fonts.getSize(13).drawString(context.getMatrices(), "#" + Integer.toHexString(setting.getColor()), x + 45, y + 96, -1);

        rectangle.render(ShapeProperties.create(matrix, x + 122, y + 90.5F, 22, 14)
                .round(1.5F).thickness(2).outlineColor(ColorUtil.getOutline(0.8F)).color(ColorUtil.getRect(1)).build());

        int displayValue = (int) (setting.getAlpha() * 100);
        Fonts.getSize(13).drawCenteredString(context.getMatrices(), displayValue + "%", x + 133, y + 96, -1);
    }

    @Compile
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (MathUtil.isHovered(mouseX, mouseY, x + 122, y + 90.5F, 22, 14)) {
            setting.setAlpha(MathHelper.clamp((float) (setting.getAlpha() - (amount * 2) / 100), 0, 1));
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button);
    }
}

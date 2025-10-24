package ru.zenith.implement.screens.menu.components.implement.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.zenith.api.feature.module.setting.implement.ColorSetting;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.common.util.other.StringUtil;
import ru.zenith.implement.screens.menu.components.implement.window.AbstractWindow;
import ru.zenith.implement.screens.menu.components.implement.window.implement.settings.color.ColorWindow;

import static ru.zenith.api.system.font.Fonts.Type.*;

public class ColorComponent extends AbstractSettingComponent {
    private final ColorSetting setting;

    public ColorComponent(ColorSetting setting) {
        super(setting);
        this.setting = setting;
    }

    @Compile
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();

        String wrapped = StringUtil.wrap(setting.getDescription(), 100, 12);
        height = (int) (18 + Fonts.getSize(12).getStringHeight(wrapped) / 3);

        Fonts.getSize(14, BOLD).drawString(matrix, setting.getName(), x + 9, y + 6, 0xFFD4D6E1);
        Fonts.getSize(12).drawString(matrix, wrapped, x + 9, y + 15, 0xFF878894);

        rectangle.render(ShapeProperties.create(matrix, x + width - 14, y + 7, 7, 7)
                .round(3.5F).color(setting.getColor()).build());

        rectangle.render(ShapeProperties.create(matrix, x + width - 14, y + 7, 7, 7)
                .round(3.5F).thickness(2).softness(1).outlineColor(ColorUtil.getText()).color(0x0FFFFFF).build());
    }

    @Compile
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (MathUtil.isHovered(mouseX, mouseY, x + width - 15, y + 6.7F, 7, 7) && button == 0) {
            AbstractWindow existingWindow = null;

            for (AbstractWindow window : windowManager.getWindows()) {
                if (window instanceof ColorWindow) {
                    existingWindow = window;
                    break;
                }
            }

            if (existingWindow != null) {
                windowManager.delete(existingWindow);
            } else {
                AbstractWindow colorWindow = new ColorWindow(setting)
                        .position((int) (mouseX + 185), (int) (mouseY - 82))
                        .size(150, 165)
                        .draggable(true);

                windowManager.add(colorWindow);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}

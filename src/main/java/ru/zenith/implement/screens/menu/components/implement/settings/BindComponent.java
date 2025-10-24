package ru.zenith.implement.screens.menu.components.implement.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ColorHelper;
import org.lwjgl.glfw.GLFW;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.zenith.api.feature.module.setting.implement.BindSetting;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.common.util.other.StringUtil;

import static ru.zenith.api.system.font.Fonts.Type.BOLD;

public class BindComponent extends AbstractSettingComponent {
    private final BindSetting setting;
    private boolean binding;

    public BindComponent(BindSetting setting) {
        super(setting);
        this.setting = setting;
    }

    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();

        String bindName = StringUtil.getBindName(setting.getKey());
        String name = binding ? "(" + bindName + ") ..." : bindName;
        float stringWidth = Fonts.getSize(13, BOLD).getStringWidth(name) - 2;
        String wrapped = StringUtil.wrap(setting.getDescription(), (int) (width - stringWidth - 28), 12);

        height = (int) (18 + Fonts.getSize(12).getStringHeight(wrapped) / 3);

        rectangle.render(ShapeProperties.create(matrix, x + width - stringWidth - 17, y + 5, stringWidth + 10, 12)
                .round(2).thickness(2).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getGuiRectColor(1F)).build());

        int bindingColor = ColorHelper.getArgb(255, 135, 136, 148);

        Fonts.getSize(13, BOLD).drawString(matrix, name, x + width - 12 - stringWidth - 1, y + 9.5, bindingColor);
        Fonts.getSize(14, BOLD).drawString(matrix, setting.getName(), x + 9, y + 6, 0xFFD4D6E1);
        Fonts.getSize(12).drawString(matrix, wrapped, x + 9, y + 15, 0xFF878894);
    }


    @Compile
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (MathUtil.isHovered(mouseX, mouseY, x, y, width, height)) {
                binding = !binding;
            } else {
                binding = false;
            }
        }

        if (binding && button > 1) {
            setting.setKey(button);
            binding = false;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int key = keyCode == GLFW.GLFW_KEY_DELETE ? -1 : keyCode;
        if (binding) {
            setting.setKey(key);
            binding = false;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}

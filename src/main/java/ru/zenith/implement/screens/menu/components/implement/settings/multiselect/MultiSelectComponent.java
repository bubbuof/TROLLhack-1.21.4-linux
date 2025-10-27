package ru.zenith.implement.screens.menu.components.implement.settings.multiselect;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.zenith.api.system.animation.Animation;
import ru.zenith.api.system.animation.Direction;
import ru.zenith.api.system.animation.implement.DecelerateAnimation;
import ru.zenith.api.system.font.FontRenderer;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.other.StringUtil;
import ru.zenith.common.util.render.ScissorManager;
import ru.zenith.api.feature.module.setting.implement.MultiSelectSetting;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.core.Main;
import ru.zenith.implement.screens.menu.components.implement.settings.AbstractSettingComponent;

import java.util.ArrayList;
import java.util.List;

import static ru.zenith.api.system.font.Fonts.Type.BOLD;

public class MultiSelectComponent extends AbstractSettingComponent {
    private final List<MultiSelectedButton> multiSelectedButtons = new ArrayList<>();

    private final MultiSelectSetting setting;
    private boolean open;

    private float dropdownListX,
            dropDownListY,
            dropDownListWidth,
            dropDownListHeight;

    private final Animation alphaAnimation = new DecelerateAnimation().setMs(300).setValue(1);

    public MultiSelectComponent(MultiSelectSetting setting) {
        super(setting);
        this.setting = setting;

        alphaAnimation.setDirection(Direction.BACKWARDS);

        for (String s : setting.getList()) {
            multiSelectedButtons.add(new MultiSelectedButton(setting, s));
        }
    }

    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrices = context.getMatrices();

        List<String> fullSettingsList = setting.getList();

        float innerX = x + 3;
        float innerW = width - 6;

        this.dropdownListX = innerX;
        this.dropDownListY = y + 10 + 14 + 2;
        this.dropDownListWidth = innerW;
        this.dropDownListHeight = fullSettingsList.size() * 12;

        height = 28 + (open ? (int) dropDownListHeight + 2 : 0);

        alphaAnimation.setDirection(open ? Direction.FORWARDS : Direction.BACKWARDS);

        renderSelected(matrices, innerX, innerW);
        if (!alphaAnimation.isFinished(Direction.BACKWARDS)) renderSelectList(context, mouseX, mouseY, delta);

        Fonts.getSize(14, BOLD).drawString(matrices, setting.getName(), (int) (x + 4), (int) (y + 3), 0xFFD4D6E1);
    }

    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 || button == 1) {
            if (MathUtil.isHovered(mouseX, mouseY, dropdownListX, y + 10, dropDownListWidth, 14)) {
                open = !open;
            } else if (open && !isHoveredList(mouseX, mouseY)) {
                open = false;
            }

            if (open) {
                multiSelectedButtons.forEach(selectedButton -> selectedButton.mouseClicked(mouseX, mouseY, button));
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }


    @Override
    public boolean isHover(double mouseX, double mouseY) {
        return open && isHoveredList(mouseX, mouseY);
    }

    
    private void renderSelected(MatrixStack matrix, float innerX, float innerW) {
        rectangle.render(ShapeProperties.create(matrix, innerX, y + 10, innerW, 14)
                .round(2).thickness(2).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getGuiRectColor(1)).build());

        String selectedName = String.join(", ", setting.getSelected());
        Fonts.getSize(12, BOLD).drawString(matrix, selectedName, (int) (innerX + 3), (int) (y + 17 - 1), ColorUtil.getText());
    }

    
    private void renderSelectList(DrawContext context, int mouseX, int mouseY, float delta) {
        float opacity = alphaAnimation.getOutput().floatValue();

        rectangle.render(ShapeProperties.create(context.getMatrices(), dropdownListX, dropDownListY, dropDownListWidth, dropDownListHeight)
                .round(4).thickness(2).outlineColor(ColorUtil.getOutline(opacity, 1)).color(ColorUtil.getGuiRectColor(opacity)).build());

        float offset = dropDownListY;
        for (MultiSelectedButton button : multiSelectedButtons) {
            button.x = dropdownListX;
            button.y = offset;
            button.width = dropDownListWidth;
            button.height = 12;

            button.setAlpha(opacity);

            button.render(context, mouseX, mouseY, delta);
            offset += 12;
        }
    }

    
    private boolean isHoveredList(double mouseX, double mouseY) {
        return MathUtil.isHovered(mouseX, mouseY, dropdownListX, dropDownListY - 16, dropDownListWidth, dropDownListHeight + 16);
    }
}


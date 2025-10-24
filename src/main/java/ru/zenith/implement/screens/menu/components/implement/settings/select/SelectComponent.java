package ru.zenith.implement.screens.menu.components.implement.settings.select;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.zenith.api.feature.module.setting.implement.SelectSetting;
import ru.zenith.api.system.animation.Animation;
import ru.zenith.api.system.animation.Direction;
import ru.zenith.api.system.animation.implement.DecelerateAnimation;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.common.util.other.StringUtil;
import ru.zenith.implement.screens.menu.components.implement.settings.AbstractSettingComponent;

import java.util.ArrayList;
import java.util.List;

import static ru.zenith.api.system.font.Fonts.Type.BOLD;

public class SelectComponent extends AbstractSettingComponent {
    private final List<SelectedButton> selectedButtons = new ArrayList<>();

    private final SelectSetting setting;
    private boolean open;

    private float dropdownListX,
            dropDownListY,
            dropDownListWidth,
            dropDownListHeight;

    private final Animation alphaAnimation = new DecelerateAnimation()
            .setMs(300).setValue(1);

    public SelectComponent(SelectSetting setting) {
        super(setting);
        this.setting = setting;

        alphaAnimation.setDirection(Direction.BACKWARDS);

        for (String s : setting.getList()) {
            selectedButtons.add(new SelectedButton(setting, s));
        }
    }

    @Compile
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrices = context.getMatrices();
        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();

        String wrapped = StringUtil.wrap(setting.getDescription(), 45, 12);
        height = (int) (18 + Fonts.getSize(12).getStringHeight(wrapped) / 3);

        List<String> fullSettingsList = setting.getList();

        this.dropdownListX = x + width - 75;
        this.dropDownListY = y + 20;
        this.dropDownListWidth = 66;
        this.dropDownListHeight = fullSettingsList.size() * 12;

        alphaAnimation.setDirection(open ? Direction.FORWARDS : Direction.BACKWARDS);

        renderSelected(matrices);
        if (!alphaAnimation.isFinished(Direction.BACKWARDS)) renderSelectList(context, mouseX, mouseY, delta);

        Fonts.getSize(14, BOLD).drawString(matrices, setting.getName(), x + 9, y + 6, 0xFFD4D6E1);
        Fonts.getSize(12).drawString(matrices, wrapped, x + 9, y + 15, 0xFF878894);
    }

    @Compile
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (MathUtil.isHovered(mouseX, mouseY, x + width - 75, y + 4, 66, 14)) {
                open = !open;
            } else if (open && !isHoveredList(mouseX, mouseY)) {
                open = false;
            }

            if (open) {
                selectedButtons.forEach(selectedButton -> selectedButton.mouseClicked(mouseX, mouseY, button));
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }


    @Override
    public boolean isHover(double mouseX, double mouseY) {
        return open && isHoveredList(mouseX, mouseY);
    }


    private void renderSelected(MatrixStack matrices) {
        rectangle.render(ShapeProperties.create(matrices, x + width - 75, y + 4, 66, 14)
                .round(2).thickness(2).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getGuiRectColor(1)).build());

        String selectedName = String.join(", ", setting.getSelected());

        Fonts.getSize(12, BOLD).drawString(matrices, selectedName, x + width - 75 + 3, y + 10, 0xFFD4D6E1);

        rectangle.render(ShapeProperties.create(matrices, x + width - 20, y + 5, 10, 12)
                .round(3).color(ColorUtil.getGuiRectColor(0), ColorUtil.getGuiRectColor(0), ColorUtil.getGuiRectColor(1), ColorUtil.getGuiRectColor(1)).build());
    }

    private void renderSelectList(DrawContext context, int mouseX, int mouseY, float delta) {
        float opacity = alphaAnimation.getOutput().floatValue();

        rectangle.render(ShapeProperties.create(context.getMatrices(), dropdownListX, dropDownListY, dropDownListWidth, dropDownListHeight)
                .round(3).thickness(2).outlineColor(ColorUtil.getOutline(opacity, 1)).color(ColorUtil.getGuiRectColor(opacity)).build());

        float offset = dropDownListY;
        for (SelectedButton button : selectedButtons) {
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
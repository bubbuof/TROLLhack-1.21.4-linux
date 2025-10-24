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

    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (MathUtil.isHovered(mouseX, mouseY, x + width - 75, y + 4, 66, 14)) {
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

    
    private void renderSelected(MatrixStack matrix) {
        FontRenderer font = Fonts.getSize(12);
        int x1 = (int) (x + width - 72);

        rectangle.render(ShapeProperties.create(matrix, x1 - 3, y + 4, 66, 14)
                .round(2).thickness(2).softness(0.5F).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getGuiRectColor(0.5F)).build());

        String selectedName = String.join(", ", setting.getSelected());

        float offset = 64;

        ScissorManager scissor = Main.getInstance().getScissorManager();
        scissor.push(matrix.peek().getPositionMatrix(), x1 - 2, (float) window.getScaledHeight() / 2 - 96, 64, 220);
        font.drawStringWithScroll(matrix, selectedName, x1, y + 10, offset, ColorUtil.getText());
        scissor.pop();

        if (font.getStringWidth(selectedName) - offset > 0) {
            rectangle.render(ShapeProperties.create(matrix, x + width - 13.75F, y + 5, 4, 12)
                    .round(3).color(ColorUtil.getGuiRectColor(0), ColorUtil.getGuiRectColor(0), ColorUtil.getGuiRectColor(1), ColorUtil.getGuiRectColor(1)).build());

            rectangle.render(ShapeProperties.create(matrix, x1 - 2.25F, y + 5, 4, 12)
                    .round(3).color(ColorUtil.getGuiRectColor(1), ColorUtil.getGuiRectColor(1), ColorUtil.getGuiRectColor(0), ColorUtil.getGuiRectColor(0)).build());
        }
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


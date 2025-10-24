package ru.zenith.implement.screens.menu.components.implement.settings.multiselect;

import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.zenith.api.feature.module.setting.implement.MultiSelectSetting;
import ru.zenith.api.system.animation.Animation;
import ru.zenith.api.system.animation.Direction;
import ru.zenith.api.system.animation.implement.DecelerateAnimation;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.implement.screens.menu.components.AbstractComponent;
import ru.zenith.implement.screens.menu.components.implement.settings.select.SelectedButton;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static ru.zenith.api.system.font.Fonts.Type.BOLD;

public class MultiSelectedButton extends AbstractComponent {
    private final MultiSelectSetting setting;
    private final String text;
    @Setter
    @Accessors(chain = true)
    private float alpha;
    private final Animation alphaAnimation = new DecelerateAnimation().setMs(300).setValue(0.5);

    public MultiSelectedButton(MultiSelectSetting setting, String text) {
        this.setting = setting;
        this.text = text;

        alphaAnimation.setDirection(Direction.BACKWARDS);
    }

    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();

        alphaAnimation.setDirection(setting.getSelected().contains(text) ? Direction.FORWARDS : Direction.BACKWARDS);

        float opacity = alphaAnimation.getOutput().floatValue();
        int selectedOpacity = ColorUtil.multAlpha(ColorUtil.getClientColor(), opacity * alpha);

        if (!alphaAnimation.isFinished(Direction.BACKWARDS)) {
            rectangle.render(ShapeProperties.create(matrix, x, y, width, height + 0.15F).round(SelectedButton.getRound(setting.getList(), text)).color(selectedOpacity).build());
        }
        Fonts.getSize(12, BOLD).drawString(matrix, text, x + 4, y + 5, ColorUtil.multAlpha(0xFFD4D6E1, alpha));
    }

    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (MathUtil.isHovered(mouseX, mouseY, x, y, width, height) && button == 0) {
            List<String> selected = new ArrayList<>(setting.getSelected());
            if (selected.contains(text)) {
                selected.remove(text);
            } else {
                selected.add(text);
                sortSelectedAccordingToList(selected, setting.getList());
            }
            setting.setSelected(selected);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    
    private void sortSelectedAccordingToList(List<String> selected, List<String> list) {
        selected.sort(Comparator.comparingInt(list::indexOf));
    }
}

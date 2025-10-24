package ru.zenith.implement.screens.menu.components.implement.other;

import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.implement.screens.menu.components.AbstractComponent;

import static ru.zenith.api.system.font.Fonts.Type.BOLD;

@Setter
@Accessors(chain = true)
public class ButtonComponent extends AbstractComponent {
    private String text;
    private Runnable runnable;
    private int color = 0xFF8187FF;

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();

        width = Fonts.getSize(12).getStringWidth(text) + 13;
        height = 12;

        rectangle.render(ShapeProperties.create(matrix, x, y, width, height)
                .round(2).color(color).build());

        Fonts.getSize(12, BOLD).drawCenteredString(matrix, text, x + (double) width / 2, y + 5, -1);
    }

    @Compile
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (MathUtil.isHovered(mouseX, mouseY, x, y, width, height) && button == 0) {
            runnable.run();
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}

package ru.zenith.implement.screens.menu.components.implement.other;

import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.zenith.api.system.animation.Animation;
import ru.zenith.api.system.animation.implement.DecelerateAnimation;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.common.util.render.ScissorManager;
import ru.zenith.core.Main;
import ru.zenith.implement.screens.menu.components.AbstractComponent;

import static ru.zenith.api.system.animation.Direction.BACKWARDS;
import static ru.zenith.api.system.animation.Direction.FORWARDS;

@Setter
@Accessors(chain = true)
public class CheckComponent extends AbstractComponent {
    private boolean state;
    private Runnable runnable;

    private final Animation alphaAnimation = new DecelerateAnimation()
            .setMs(300)
            .setValue(255);

    private final Animation stencilAnimation = new DecelerateAnimation()
            .setMs(200)
            .setValue(8);


    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();
        boolean isHovered = MathUtil.isHovered(mouseX, mouseY, x, y, 8, 8);

        alphaAnimation.setDirection(state ? FORWARDS : BACKWARDS);
        stencilAnimation.setDirection(state ? FORWARDS : BACKWARDS);

        // Enhanced GameSense/Skeet.cc style checkbox
        int stateColor = state ? ColorUtil.getSkeetAccent() : 
                        isHovered ? ColorUtil.getSkeetHover() : ColorUtil.getSkeetSecondary();
        int outlineStateColor = state ? ColorUtil.getSkeetAccent() : 
                               isHovered ? ColorUtil.getSkeetAccent(0.5f) : ColorUtil.getSkeetBorder();

        int opacity = alphaAnimation.getOutput().intValue();

        // Main checkbox with enhanced styling
        rectangle.render(ShapeProperties.create(matrix, x, y, 8, 8)
                .round(1).thickness(1).softness(0.3f).outlineColor(outlineStateColor)
                .color(MathUtil.applyOpacity(stateColor, opacity)).build());

        // Glow effect when enabled
        if (state) {
            rectangle.render(ShapeProperties.create(matrix, x - 0.5f, y - 0.5f, 9, 9)
                    .round(1.5f).thickness(0.5f).softness(1f)
                    .color(MathUtil.applyOpacity(ColorUtil.getSkeetAccent(), opacity / 4)).build());
        }

        ScissorManager scissor = Main.getInstance().getScissorManager();
        scissor.push(matrix.peek().getPositionMatrix(), x, (float) window.getScaledHeight() / 2 - 96, stencilAnimation.getOutput().intValue(), 220);

        // Enhanced checkmark
        image.setTexture("textures/check.png").render(ShapeProperties.create(matrix, x + 2, y + 2.5, 4, 3)
                .color(MathUtil.applyOpacity(ColorUtil.getSkeetText(), opacity)).build());

        scissor.pop();
    }

    @Compile
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (MathUtil.isHovered(mouseX, mouseY, x, y, 8, 8) && button == 0) {
            runnable.run();
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}

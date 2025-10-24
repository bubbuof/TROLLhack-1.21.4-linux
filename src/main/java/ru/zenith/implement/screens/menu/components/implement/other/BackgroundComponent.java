package ru.zenith.implement.screens.menu.components.implement.other;

import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.render.Render2DUtil;
import ru.zenith.implement.screens.menu.MenuScreen;
import ru.zenith.implement.screens.menu.components.AbstractComponent;

@Setter
@Accessors(chain = true)
public class BackgroundComponent extends AbstractComponent {

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();

        // Main background with enhanced GameSense/Skeet.cc style
        rectangle.render(ShapeProperties.create(matrix, x, y, width, height).round(2).softness(0.5f).thickness(1).quality(50)
                .outlineColor(ColorUtil.getSkeetBorder()).color(ColorUtil.getSkeetBackground()).build());

        // Enhanced top bar with gradient effect
        rectangle.render(ShapeProperties.create(matrix, x, y, width, 3)
                .color(ColorUtil.getSkeetAccent(), ColorUtil.getSkeetAccent(), 
                       ColorUtil.getSkeetAccent(0.8f), ColorUtil.getSkeetAccent(0.8f)).build());

        // Category sidebar with subtle gradient
        rectangle.render(ShapeProperties.create(matrix, x, y, 85, height).round(2, 0, 0, 2)
                .color(ColorUtil.getSkeetSecondary(), ColorUtil.getSkeetSecondary(),
                       ColorUtil.getSkeetBackground(), ColorUtil.getSkeetBackground()).build());

        // Enhanced vertical separator with glow effect
        rectangle.render(ShapeProperties.create(context.getMatrices(), x + 85, y, 1F, height)
                .color(ColorUtil.getSkeetBorder()).build());
        rectangle.render(ShapeProperties.create(context.getMatrices(), x + 84.5f, y, 0.5F, height)
                .color(ColorUtil.getSkeetAccent(0.3f)).build());
        
        // Enhanced horizontal separator under header
        rectangle.render(ShapeProperties.create(context.getMatrices(), x + 85, y + 28, width - 85, 1F)
                .color(ColorUtil.getSkeetBorder()).build());
        rectangle.render(ShapeProperties.create(context.getMatrices(), x + 85, y + 27.5f, width - 85, 0.5F)
                .color(ColorUtil.getSkeetAccent(0.2f)).build());

        // Category title with enhanced styling
        Fonts.getSize(16, Fonts.Type.BOLD).drawString(matrix, MenuScreen.INSTANCE.getCategory().getReadableName(), x + 95, y + 13, ColorUtil.getSkeetText());
        
        // Subtle shadow effect for title
        Fonts.getSize(16, Fonts.Type.BOLD).drawString(matrix, MenuScreen.INSTANCE.getCategory().getReadableName(), x + 95.5f, y + 13.5f, ColorUtil.getSkeetBackground());
    }
}

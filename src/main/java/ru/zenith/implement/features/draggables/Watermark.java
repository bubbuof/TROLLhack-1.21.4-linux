package ru.zenith.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import ru.zenith.api.feature.draggable.AbstractDraggable;
import ru.zenith.api.system.font.FontRenderer;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.render.Render2DUtil;


public class Watermark extends AbstractDraggable {
    // Correct path to the image in resources - now in proper assets structure
    private static final Identifier TROLL_FACE = Identifier.of("zenith", "textures/11.png");
    
    public Watermark() {
        super("Watermark", 5, 5, 150, 40, true);
    }

    @Override
    public void tick() {
        // No animations or effects - just simple display
    }

    @Override
    public void drawDraggable(DrawContext context) {
        FontRenderer font = Fonts.getSize(12);
        MatrixStack matrices = context.getMatrices();
        
        // Image size - smaller now
        int imageSize = 32;
        
        // Text
        String text = "88HACK v1.13";
        float textWidth = font.getStringWidth(text);
        float textHeight = font.getStringHeight(text);
        
        // Calculate total dimensions
        float totalWidth = imageSize + textWidth + 10; // Image + spacing + text
        float totalHeight = Math.max(imageSize, textHeight);
        
        // Draw trollface image using Zenith's Render2DUtil
        Render2DUtil.drawTexture(context, TROLL_FACE, getX(), getY(), imageSize);
        
        // Calculate text position - next to image, vertically centered
        float textX = getX() + imageSize + 5; // 5px spacing from image
        float textY = getY() + (totalHeight - textHeight) / 2;
        
        // Draw TROLLHACK text - simple green text
        font.drawString(matrices, text, textX, textY, ColorUtil.getColor(0, 119, 255, 255));
        
        // Update dimensions
        setWidth((int) totalWidth);
        setHeight((int) totalHeight);
    }
}

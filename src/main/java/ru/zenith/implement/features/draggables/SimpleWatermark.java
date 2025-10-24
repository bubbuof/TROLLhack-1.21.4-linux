package ru.zenith.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import ru.zenith.api.feature.draggable.AbstractDraggable;
import ru.zenith.api.system.font.FontRenderer;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.common.util.render.Render2DUtil;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.math.MathUtil;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SimpleWatermark extends AbstractDraggable {
    private int fpsCount = 0;

    public SimpleWatermark() {
        super("SimpleWatermark", 5, 5, 200, 20, true);
    }

    @Override
    public void tick() {
        fpsCount = (int) MathUtil.interpolate(fpsCount, mc.getCurrentFps());
    }

    @Override
    public void drawDraggable(DrawContext e) {
        FontRenderer font = Fonts.getSize(12);
        MatrixStack matrix = e.getMatrices();
        
        // Get current time (HH:mm format like in image)
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        String currentTime = timeFormat.format(new Date());
        
        // Get server info
        String serverInfo = "mc.aresmine.ru"; // Default server
        if (mc.getCurrentServerEntry() != null) {
            serverInfo = mc.getCurrentServerEntry().address;
        }
        
        // Build watermark text like in the image
        String watermarkText = String.format("NEVERCLIENT %s %s %d FPS", 
            serverInfo, currentTime, fpsCount);
        
        // Calculate text dimensions
        float textWidth = font.getStringWidth(watermarkText);
        float textHeight = font.getStringHeight(watermarkText);
        
        // Padding for clean look
        float paddingX = 8;
        float paddingY = 4;
        float backgroundWidth = textWidth + paddingX * 2;
        float backgroundHeight = textHeight + paddingY * 2;
        
        // Draw background - dark like in image
        Render2DUtil.drawQuad(getX(), getY(), backgroundWidth, backgroundHeight, 
            ColorUtil.getColor(0, 0, 0, 180));
        
        // Draw subtle border
        Render2DUtil.drawQuad(getX(), getY(), backgroundWidth, 1, 
            ColorUtil.getColor(60, 60, 60, 200));
        Render2DUtil.drawQuad(getX(), getY() + backgroundHeight - 1, backgroundWidth, 1, 
            ColorUtil.getColor(60, 60, 60, 200));
        Render2DUtil.drawQuad(getX(), getY(), 1, backgroundHeight, 
            ColorUtil.getColor(60, 60, 60, 200));
        Render2DUtil.drawQuad(getX() + backgroundWidth - 1, getY(), 1, backgroundHeight, 
            ColorUtil.getColor(60, 60, 60, 200));
        
        // Calculate text position
        float textX = getX() + paddingX;
        float textY = getY() + paddingY;
        
        // Draw text - white like in image
        font.drawString(matrix, watermarkText, textX, textY, ColorUtil.getColor(255, 255, 255, 255));
        
        // Update draggable dimensions
        setWidth((int) backgroundWidth);
        setHeight((int) backgroundHeight);
    }
}

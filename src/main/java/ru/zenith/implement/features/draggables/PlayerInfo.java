package ru.zenith.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import ru.zenith.api.system.font.FontRenderer;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.world.ServerUtil;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.common.util.entity.MovingUtil;
import ru.zenith.api.feature.draggable.AbstractDraggable;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.common.util.entity.PlayerIntersectionUtil;

import java.util.Objects;

public class PlayerInfo extends AbstractDraggable {

    public PlayerInfo() {
        super("Player Info", 5, 0, 100, 36, false);
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();
        int offset = PlayerIntersectionUtil.isChat(mc.currentScreen) ? -15 : 0;
        BlockPos blockPos = Objects.requireNonNull(mc.player).getBlockPos();
        FontRenderer font = Fonts.getSize(12);

        setY(window.getScaledHeight() + offset - 40);

        // Calculate values
        float bpsValue = (float) (MovingUtil.getSpeedSqrt(mc.player) * 20.0F);
        float tpsValue = (float) ServerUtil.TPS;
        
        // GameSense style - compact text format
        String bpsText = MathUtil.round(bpsValue, 0.1F) + " bps";
        String tpsText = MathUtil.round(tpsValue, 0.1) + " tps";
        String coordsText = blockPos.getX() + " " + blockPos.getY() + " " + blockPos.getZ();
        
        // Calculate width based on longest string
        float maxWidth = Math.max(Math.max(font.getStringWidth(bpsText), font.getStringWidth(tpsText)), font.getStringWidth(coordsText));
        setWidth((int) (maxWidth + 6));
        setHeight(36);

        // ScoreBoard style background with blur and rounded corners
        blur.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), getHeight())
                .round(4).thickness(2).softness(1).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.7F)).build());

        float yPos = getY() + 3;
        
        // Simple BPS - performance based coloring
        int bpsColor = bpsValue >= 8 ? ColorUtil.getColor(150, 255, 150) : // Light green
                      bpsValue >= 4 ? ColorUtil.getColor(255, 255, 150) : // Light yellow
                      ColorUtil.getColor(255, 150, 150); // Light red
        
        font.drawString(matrix, bpsText, getX() + 3, yPos, bpsColor);
        yPos += 11;
        
        // Simple TPS - server performance based coloring  
        int tpsColor = tpsValue >= 18 ? ColorUtil.getColor(150, 255, 150) : // Light green
                      tpsValue >= 15 ? ColorUtil.getColor(255, 255, 150) : // Light yellow
                      ColorUtil.getColor(255, 150, 150); // Light red
        
        font.drawString(matrix, tpsText, getX() + 3, yPos, tpsColor);
        yPos += 11;
        
        // Simple coordinates - clean white text
        font.drawString(matrix, coordsText, getX() + 3, yPos, ColorUtil.getColor(200, 200, 200));
    }
}

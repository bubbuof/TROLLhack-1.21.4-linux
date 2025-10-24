package ru.zenith.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.boss.BossBar;
import org.joml.Vector4f;
import ru.zenith.api.feature.draggable.AbstractDraggable;
import ru.zenith.api.system.font.FontRenderer;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.color.ColorUtil;

import java.awt.*;

public class BossBars extends AbstractDraggable {
    public BossBars() {
        super("Boss Bars", 0, 0, 0, 0,false);
    }

    @Override
    public boolean visible() {
        return !mc.inGameHud.getBossBarHud().bossBars.isEmpty();
    }

    @Override
    public void drawDraggable(DrawContext context) {
        setX(mc.getWindow().getScaledWidth() / 2);

        MatrixStack matrix = context.getMatrices();

        float y = 10;
        float width = 156;
        float height = 3.5F;
        FontRenderer font = Fonts.getSize(18);

        for (ClientBossBar bossInfo : mc.inGameHud.getBossBarHud().bossBars.values()) {
            Vector4f rounds = bossInfo.getPercent() != 1 ? new Vector4f(0,0,height / 2,height / 2) : new Vector4f(height / 2);
            int color = getColor(bossInfo.getColor());

            rectangle.render(ShapeProperties.create(matrix,getX() - width / 2,y + 10,width,height).color(ColorUtil.getRect(0.8F)).round(1.75F).build());
            rectangle.render(ShapeProperties.create(matrix,getX() - width / 2,y + 10,width,height).color(ColorUtil.multAlpha(color,0.2F)).round(1.75F).build());
            rectangle.render(ShapeProperties.create(matrix,getX() - width / 2,y + 10,width * bossInfo.getPercent(),height).color(ColorUtil.multAlpha(color,0.8F)).round(rounds).build());

            font.drawText(matrix, bossInfo.getName(), (int) (getX() - font.getStringWidth(bossInfo.getName()) / 2), y);
            y += 22;
        }
    }

    public int getColor(BossBar.Color color) {
        return switch (color) {
            case PINK -> new Color(0xFF5AB4).getRGB();
            case PURPLE -> new Color(0x813CFF).getRGB();
            case RED -> new Color(0xFF3737).getRGB();
            case BLUE -> new Color(0x00A0FF).getRGB();
            case GREEN -> new Color(0x55FF55).getRGB();
            default -> new Color(color.getTextFormat().getColorValue()).getRGB();
        };
    }

}

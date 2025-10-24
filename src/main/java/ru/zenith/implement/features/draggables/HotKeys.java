package ru.zenith.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import ru.zenith.api.feature.draggable.AbstractDraggable;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.system.font.FontRenderer;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.common.util.other.StringUtil;
import ru.zenith.common.util.entity.PlayerIntersectionUtil;
import ru.zenith.core.Main;

import java.util.ArrayList;
import java.util.List;

public class HotKeys extends AbstractDraggable {
    private List<Module> keysList = new ArrayList<>();

    public HotKeys() {
        super("Hot Keys", 300, 10, 80, 23,true);
    }

    @Override
    public boolean visible() {
        return !keysList.isEmpty() || PlayerIntersectionUtil.isChat(mc.currentScreen);
    }

    @Override
    public void tick() {
        keysList = Main.getInstance().getModuleProvider().getModules().stream().filter(module -> module.getAnimation().getOutput().floatValue() != 0 && module.getKey() != -1).toList();
    }

    @Override
    public void drawDraggable(DrawContext e) {
        MatrixStack matrix = e.getMatrices();
        float centerX = getX() + getWidth() / 2F;

        FontRenderer font = Fonts.getSize(14, Fonts.Type.DEFAULT);
        FontRenderer fontModule = Fonts.getSize(13, Fonts.Type.DEFAULT);

        // ScoreBoard style background with blur and rounded corners
        blur.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), getHeight())
                .round(4).thickness(2).softness(1).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.7F)).build());

        // ScoreBoard style title
        font.drawText(matrix, Text.of(getName().toLowerCase()), (int) (centerX - font.getStringWidth(getName().toLowerCase()) / 2), getY() + 4);

        int offset = 16;
        int maxWidth = 80;

        for (Module module : keysList) {
            String bind = "[" + StringUtil.getBindName(module.getKey()) + "]";
            float centerY = getY() + offset;
            float animation = module.getAnimation().getOutput().floatValue();
            float width = fontModule.getStringWidth(module.getName() + bind) + 12;

            MathUtil.scale(matrix, centerX, centerY, 1, animation, () -> {
                // Simple module name
                fontModule.drawString(matrix, module.getName(), getX() + 4, centerY, ColorUtil.getColor(200, 200, 200));
                // Simple bind key with client color
                fontModule.drawString(matrix, bind, getX() + getWidth() - 4 - fontModule.getStringWidth(bind), centerY, ColorUtil.getClientColor());
            });

            offset += (int) (animation * 10);
            maxWidth = (int) Math.max(width, maxWidth);
        }

        setWidth(maxWidth);
        setHeight(offset + 2);
    }
}

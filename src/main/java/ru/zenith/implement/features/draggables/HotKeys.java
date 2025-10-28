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
        super("Hot Keys", 300, 10, 80, 23, true);
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
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();

        FontRenderer font = Fonts.getSize(14, Fonts.Type.DEFAULT);
        FontRenderer fontModule = Fonts.getSize(13, Fonts.Type.DEFAULT);

        // GameSense style background - темный как у CoolDowns
        rectangle.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), getHeight())
                .color(ColorUtil.getColor(15, 15, 15, 200)).build());

        // Top accent line - цвет клиента как у CoolDowns
        rectangle.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), 1)
                .color(ColorUtil.getClientColor()).build());

        float centerX = getX() + getWidth() / 2.0F;

        // Заголовок в стиле CoolDowns
        font.drawString(matrix, getName().toLowerCase(), (int) (centerX - font.getStringWidth(getName().toLowerCase()) / 2.0F), getY() + 4, ColorUtil.WHITE);

        int offset = 16;
        int maxWidth = 80;

        for (Module module : keysList) {
            String bind = "[" + StringUtil.getBindName(module.getKey()) + "]";
            float centerY = getY() + offset;
            float animation = module.getAnimation().getOutput().floatValue();
            float width = fontModule.getStringWidth(module.getName() + bind) + 12;

            MathUtil.scale(matrix, centerX, centerY, 1, animation, () -> {
                // Module name - серый текст как у CoolDowns
                fontModule.drawString(matrix, module.getName(), getX() + 4, centerY, ColorUtil.getColor(200, 200, 200));

                // Bind key - цвет клиента как у CoolDowns
                fontModule.drawString(matrix, bind, getX() + getWidth() - 4 - fontModule.getStringWidth(bind), centerY, ColorUtil.getClientColor());
            });

            offset += (int) (animation * 10);
            maxWidth = (int) Math.max(width, maxWidth);
        }

        setWidth(maxWidth);
        setHeight(offset + 2);
    }
}
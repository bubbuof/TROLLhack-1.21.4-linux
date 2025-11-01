package ru.zenith.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import ru.zenith.api.feature.draggable.AbstractDraggable;
import ru.zenith.api.system.font.FontRenderer;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.render.Render2DUtil;

public class Watermark extends AbstractDraggable {
    private static final Identifier LOGO = Identifier.of("zenith", "textures/11.png");

    public Watermark() {
        super("Watermark", 5, 5, 180, 32, true);
    }

    @Override
    public void tick() {
        // No animations needed
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();
        // Используем стандартный шрифт вместо Fonts.get("sfprosemibold")
        FontRenderer font = Fonts.getSize(16); // или Fonts.getSize(14)

        // Получаем данные
        String playerName = mc.player != null ? mc.player.getName().getString() : "Unknown";
        String fps = "FPS: " + mc.getCurrentFps();
        String serverIp = getServerIp();

        // Основной текст
        String mainText = "gaysense beta b11";
        String infoText = playerName + " | " + fps + " | " + serverIp;

        // Рассчитываем размеры
        float mainTextWidth = font.getStringWidth(mainText);
        float infoTextWidth = Fonts.getSize(12).getStringWidth(infoText);
        float maxTextWidth = Math.max(mainTextWidth, infoTextWidth);

        // Общая ширина (лого + отступы + текст)
        int totalWidth = 28 + 8 + (int) maxTextWidth + 10;
        setWidth(totalWidth);

        // GameSense стиль - темный фон с акцентной линией
        rectangle.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), getHeight())
                .color(ColorUtil.getColor(15, 15, 15, 200)).build());

        // Верхняя акцентная линия
        rectangle.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), 1)
                .color(ColorUtil.getClientColor()).build());

        // Логотип - исправленный вызов
        Render2DUtil.drawTexture(context, LOGO, getX() + 4, getY() + 4, 20);

        // Основной текст - белый
        font.drawString(matrix, mainText, getX() + 32, getY() + 6, ColorUtil.WHITE);

        // Информационный текст - серый, меньшим шрифтом
        Fonts.getSize(12).drawString(matrix, infoText, getX() + 32, getY() + 18,
                ColorUtil.getColor(180, 180, 180));

    }

    private String getServerIp() {
        if (mc.getNetworkHandler() == null || mc.getNetworkHandler().getConnection() == null) {
            return "Singleplayer";
        }

        String address = mc.getNetworkHandler().getConnection().getAddress().toString();

        // Очищаем адрес от лишней информации
        if (address.contains("/")) {
            address = address.split("/")[1];
        }
        if (address.contains(":")) {
            address = address.split(":")[0];
        }

        return address;
    }
}
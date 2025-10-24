package ru.zenith.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import ru.zenith.api.feature.draggable.AbstractDraggable;
import ru.zenith.api.system.font.FontRenderer;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.common.util.render.Render2DUtil;
import ru.zenith.common.util.color.ColorUtil;

import java.util.Objects;
import java.util.stream.IntStream;

public class HotBar extends AbstractDraggable {
    private float selectItemX;

    public HotBar() {
        super("HotBar", 0, 50, 182, 22, false);
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();
        PlayerInventory inventory = Objects.requireNonNull(mc.player).getInventory();
        ItemStack offHand = mc.player.getOffHandStack();

        // Smooth selection animation
        selectItemX = MathUtil.interpolateSmooth(0.2f, selectItemX, inventory.selectedSlot * 20);
        setX((mc.getWindow().getScaledWidth() - getWidth()) / 2);
        setY(mc.getWindow().getScaledHeight() - 25);

        // GameSense style background - dark and clean
        rectangle.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), getHeight())
                .color(ColorUtil.getColor(15, 15, 15, 200)).build());
        
        // Top accent line
        rectangle.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), 1)
                .color(ColorUtil.getClientColor()).build());

        // Selected slot indicator - minimal outline style
        rectangle.render(ShapeProperties.create(matrix, getX() + selectItemX + 1, getY() + 1, 20, 20)
                .thickness(1)
                .outlineColor(ColorUtil.getClientColor())
                .color(ColorUtil.getColor(0, 0, 0, 0)).build()); // Transparent fill

        // Draw slot separators (GameSense style)
        for (int i = 1; i < 9; i++) {
            float separatorX = getX() + i * 20;
            rectangle.render(ShapeProperties.create(matrix, separatorX, getY() + 2, 1, getHeight() - 4)
                    .color(ColorUtil.getColor(40, 40, 40, 150)).build());
        }

        // Draw items
        IntStream.range(0, 9).forEach(i -> drawStack(context, inventory.main.get(i), getX() + i * 20 + 2, getY() + 2, false));
        
        // Off-hand rendering
        if (!offHand.isEmpty()) {
            float offHandX = getX() + (Objects.requireNonNull(mc.player).getMainArm().equals(Arm.RIGHT) ? -26 : 188);
            drawStack(context, offHand, offHandX, getY() + 2, true);
        }
        
        if (!mc.player.isSpectator() && !mc.player.isCreative()) drawExperienceBar(matrix);
        drawOverlayInfo(matrix);
    }

    public void drawExperienceBar(MatrixStack matrix) {
        Fonts.getSize(16).drawCenteredString(matrix, mc.player.experienceLevel + "", window.getScaledWidth() / 2F, getY() - 9.5F, ColorUtil.GREEN);
    }

    public void drawOverlayInfo(MatrixStack matrix) {
        float scaledWidth = (float) mc.getWindow().getScaledWidth() / 2;
        float heightStart = mc.getWindow().getScaledHeight() - 75;
        float paddingX = 4;
        float paddingY = 3;
        FontRenderer font = Fonts.getSize(14, Fonts.Type.DEFAULT);

        if (mc.inGameHud.heldItemTooltipFade > 0 && mc.inGameHud.currentStack != null) {
            float alpha = ((float) mc.inGameHud.heldItemTooltipFade * 256.0F / 10.0F) / 255;

            Text text = mc.inGameHud.currentStack.getName();
            float width = font.getStringWidth(text);
            int x = (int) (scaledWidth - width / 2);
            MathUtil.setAlpha(alpha, () -> {
                blur.render(ShapeProperties.create(matrix,x - paddingX,heightStart - paddingY,width + paddingX * 2, font.getStringHeight(text) / 2.15F + paddingY * 2)
                        .round(2.5F).color(ColorUtil.getRect(0.7F)).build());
                font.drawText(matrix, text, x, heightStart + 2.5F);
            });
        }
        if (mc.inGameHud.overlayRemaining > 0 && mc.inGameHud.overlayMessage != null && !mc.inGameHud.overlayMessage.getString().isEmpty()) {
            float alpha = ((float) mc.inGameHud.overlayRemaining * 256.0F / 10.0F) / 255;

            Text text = mc.inGameHud.overlayMessage;
            float width = font.getStringWidth(text);
            int x = (int) (scaledWidth - width / 2);
            MathUtil.setAlpha(alpha, () -> {
                blur.render(ShapeProperties.create(matrix,x - paddingX,heightStart - paddingY - 17,width + paddingX * 2, font.getStringHeight(text) / 2.15F + paddingY * 2)
                        .round(2.5F).color(ColorUtil.getRect(0.7F)).build());
                font.drawText(matrix, text, x, heightStart - 14.5F);
            });
        }
    }

    public void drawStack(DrawContext context, ItemStack stack, float x, float y, boolean offHand) {
        if (offHand) {
            // GameSense style off-hand background - minimal and clean
            rectangle.render(ShapeProperties.create(context.getMatrices(), x - 2, y - 2, 20, 20)
                    .color(ColorUtil.getColor(15, 15, 15, 200)).build());
            
            // Top accent line for off-hand
            rectangle.render(ShapeProperties.create(context.getMatrices(), x - 2, y - 2, 20, 1)
                    .color(ColorUtil.getClientColor()).build());
        }
        
        Render2DUtil.defaultDrawStack(context, stack, x, y, false, true, 1);
    }
}
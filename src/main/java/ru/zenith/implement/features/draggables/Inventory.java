package ru.zenith.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import ru.zenith.api.feature.draggable.AbstractDraggable;
import ru.zenith.api.system.font.FontRenderer;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.entity.PlayerIntersectionUtil;
import ru.zenith.common.util.render.Render2DUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class Inventory extends AbstractDraggable {
    List<ItemStack> stacks = new ArrayList<>();

    public Inventory() {
        super("Inventory", 390, 10, 123, 60,true);
    }

    @Override
    public boolean visible() {
        return !stacks.stream().filter(stack -> !stack.isEmpty()).toList().isEmpty() || PlayerIntersectionUtil.isChat(mc.currentScreen);
    }

    @Override
    public void tick() {
        stacks = IntStream.range(9, 36).mapToObj(i -> mc.player.inventory.getStack(i)).toList();
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();

        // GameSense style background
        rectangle.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), getHeight())
                .color(ColorUtil.getColor(15, 15, 15, 200)).build());
        
        // Top accent line
        rectangle.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), 1)
                .color(ColorUtil.getClientColor()).build());

        Fonts.getSize(14, Fonts.Type.DEFAULT).drawCenteredString(matrix, getName().toLowerCase(), getX() + getWidth() / 2F, getY() + 4, ColorUtil.WHITE);

        int offsetY = 16;
        int offsetX = 4;
        int itemsPerRow = 9; // 9 items per row like Minecraft inventory
        int itemSize = 12;
        
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            
            // Draw slot background for empty slots
            rectangle.render(ShapeProperties.create(matrix, getX() + offsetX - 1, getY() + offsetY - 1, itemSize, itemSize)
                    .color(ColorUtil.getColor(25, 25, 25, 150)).build());
            
            // Draw item if not empty
            if (!stack.isEmpty()) {
                Render2DUtil.defaultDrawStack(context, stack, getX() + offsetX, getY() + offsetY, false, true, 0.45F);
            }

            offsetX += itemSize + 1;
            if ((i + 1) % itemsPerRow == 0) {
                offsetY += itemSize + 1;
                offsetX = 4;
            }
        }
        
        // Update height based on rows needed
        int rows = (int) Math.ceil(stacks.size() / (double) itemsPerRow);
        setHeight(16 + rows * (itemSize + 1) + 2);
    }
}

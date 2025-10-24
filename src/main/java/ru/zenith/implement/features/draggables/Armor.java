package ru.zenith.implement.features.draggables;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import ru.zenith.api.feature.draggable.AbstractDraggable;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.render.Render2DUtil;
import ru.zenith.common.util.render.ScissorManager;
import ru.zenith.common.util.render.Stencil;
import ru.zenith.core.Main;
import ru.zenith.implement.features.modules.render.Hud;

import java.util.Objects;

public class Armor extends AbstractDraggable {
    public Armor() {
        super("Armor", 0, 0, 82, 22,false);
    }

    @Override
    public boolean visible() {
        return mc.player.getInventory().armor.stream().anyMatch(s -> !s.isEmpty());
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();
        int handX = Objects.requireNonNull(mc.player).getMainArm().equals(Arm.RIGHT) ? 100 : -187;

        if (Hud.getInstance().interfaceSettings.isSelected("HotBar")) {
            setX(context.getScaledWindowWidth() / 2 + handX);
            setY(context.getScaledWindowHeight() - 27);

            blur.render(ShapeProperties.create(matrix, getX() - 0.5F, getY() - 0.5F, getWidth() + 1,23)
                    .round(3).thickness(2).softness(1).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.7F)).build());
            drawArmorStacks(context,3,3);
        } else {
            Sprite sprite = context.guiAtlasManager.getSprite(InGameHud.HOTBAR_TEXTURE);
            setX(context.getScaledWindowWidth() / 2 + handX);
            setY(context.getScaledWindowHeight() - 19);
            ScissorManager scissor = Main.getInstance().getScissorManager();
            int width = 41;

            scissor.push(matrix.peek().getPositionMatrix(), getX() - 3, getY() - 3, width, 22);
            Render2DUtil.drawSprite(matrix, sprite, getX() - 3, getY() - 3, 182, 22);
            scissor.pop();

            scissor.push(matrix.peek().getPositionMatrix(), getX() + width - 3, getY() - 3, width, 22);
            Render2DUtil.drawSprite(matrix, sprite, getX() - 103, getY() - 3, 182, 22);
            scissor.pop();
            drawArmorStacks(context,0,0);
        }
    }

    public void drawArmorStacks(DrawContext context, int offsetX, int offsetY) {
        for (ItemStack itemStack : Objects.requireNonNull(mc.player).getArmorItems()) {
            context.drawItem(itemStack, getX() + offsetX, getY() + offsetY, 1);
            context.drawStackOverlay(mc.textRenderer, itemStack, getX() + offsetX, getY() + offsetY);
            offsetX += 20;
        }
    }
}

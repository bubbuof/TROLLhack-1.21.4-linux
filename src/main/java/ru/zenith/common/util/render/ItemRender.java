package ru.zenith.common.util.render;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.text.Text;
import net.minecraft.util.math.ColorHelper;
import ru.zenith.api.system.font.FontRenderer;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.QuickImports;
import ru.zenith.common.util.color.ColorUtil;

import java.util.HashMap;

@UtilityClass
public class ItemRender implements QuickImports {
    public final HashMap<ItemStack, CachedSprite> SPRITE_CACHE = new HashMap<>();

    public void drawItem(MatrixStack matrix, ItemStack stack, float x, float y, boolean count, boolean bar) {
        CachedSprite cachedSprite = getSpriteTexture(stack);
        if (cachedSprite != null) {
            Render2DUtil.drawSprite(matrix, cachedSprite.sprite, x, y,  16, 16);
        }
        if (count) drawCount(matrix, stack, x, y);
        if (bar) drawBar(matrix, stack, x, y);
    }

    private void drawCount(MatrixStack matrix, ItemStack stack, float x, float y) {
        int count = stack.getCount();
        String text = count > 1 ? count + "" : "";
        if (!text.isEmpty()) {
            FontRenderer font = Fonts.getSize(16);
            font.drawString(matrix, text, x + 16 - font.getStringWidth(text), y + 11, ColorUtil.getText());
        }
    }

    private void drawBar(MatrixStack matrix, ItemStack stack, float x, float y) {
        if (stack.isItemBarVisible()) {
            rectangle.render(ShapeProperties.create(matrix, x + 1.5F, y + 13, 13, 2).color(-16777216).build());
            rectangle.render(ShapeProperties.create(matrix, x + 1.5F, y + 13, stack.getItemBarStep(), 1).color(ColorHelper.fullAlpha(stack.getItemBarColor())).build());
        }
    }

    private CachedSprite getSpriteTexture(ItemStack stack) {
        return SPRITE_CACHE.computeIfAbsent(stack, key -> {
            ItemRenderState state = new ItemRenderState();
            mc.getItemModelManager().update(state, stack, ModelTransformationMode.GUI, mc.world, null, 0);

            Sprite sprite = getFirstParticleSprite(state);
            if (sprite != null) {
                int glId = mc.getTextureManager().getTexture(sprite.getAtlasId()).getGlId();
                return new CachedSprite(sprite, glId, 0xFFFFFF);
            }

            return null;
        });
    }

    private Sprite getFirstParticleSprite(ItemRenderState state) {
        BakedModel model = state.layers[0].model;
        if (state.layerCount == 0 || model == null) return null;
        return model.getParticleSprite();
    }

    public record CachedSprite(Sprite sprite, int glId, int color) {}
}

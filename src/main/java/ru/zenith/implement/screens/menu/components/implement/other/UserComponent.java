package ru.zenith.implement.screens.menu.components.implement.other;

import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.kotopushka.compiler.sdk.classes.Profile;
import ru.zenith.api.system.discord.DiscordManager;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.other.StringUtil;
import ru.zenith.common.util.render.Render2DUtil;
import ru.zenith.common.util.render.ScissorManager;
import ru.zenith.core.Main;
import ru.zenith.implement.screens.menu.components.AbstractComponent;

@Setter
@Accessors(chain = true)
public class UserComponent extends AbstractComponent {
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();
        Matrix4f positionMatrix = matrix.peek().getPositionMatrix();
        DiscordManager discord = Main.getInstance().getDiscordManager();

        rectangle.render(ShapeProperties.create(matrix, x + 5, y - 30, 75, 25)
                .round(4).thickness(2).softness(0.5F).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getGuiRectColor(0.5F)).build());

        Render2DUtil.drawTexture(context, discord.getAvatarId(), x + 11, y - 25, 15, 7.5F, 0, 15, 21, ColorUtil.getGuiRectColor(1));

        rectangle.render(ShapeProperties.create(matrix, x + 21.5F, y - 15.5F, 5, 5)
                .round(2.5F).color(ColorUtil.getGuiRectColor(1)).build());

        rectangle.render(ShapeProperties.create(matrix, x + 22.5F, y - 14.5F, 3, 3)
                .round(1.5F).color(0xFF26c68c).build());

        ScissorManager scissor = Main.getInstance().getScissorManager();
        scissor.push(positionMatrix, x + 5.5F, y - 29.5F, 74, 22);
        Fonts.getSize(12).drawString(matrix, Profile.getUsername(), x + 30, y - 21, 0xFFD4D6E1);
        Fonts.getSize(10).drawGradientString(matrix, StringUtil.getUserRole(), x + 30, y - 14.5, ColorUtil.fade(0), ColorUtil.fade(60));
        scissor.pop();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }
}

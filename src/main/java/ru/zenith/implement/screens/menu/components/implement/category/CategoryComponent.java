package ru.zenith.implement.screens.menu.components.implement.category;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.kotopushka.compiler.sdk.annotations.Initialization;
import ru.kotopushka.compiler.sdk.annotations.VMProtect;
import ru.kotopushka.compiler.sdk.enums.VMProtectType;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.system.animation.Animation;
import ru.zenith.api.system.animation.Direction;
import ru.zenith.api.system.animation.implement.DecelerateAnimation;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.common.util.render.ScissorManager;
import ru.zenith.core.Main;
import ru.zenith.implement.screens.menu.MenuScreen;
import ru.zenith.implement.screens.menu.components.AbstractComponent;
import ru.zenith.implement.screens.menu.components.implement.module.ModuleComponent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CategoryComponent extends AbstractComponent {
    private final List<ModuleComponent> moduleComponents = new ArrayList<>();
    private final ModuleCategory category;

    private final Animation alphaAnimation = new DecelerateAnimation().setMs(300).setValue(1);


    @Compile
    @Initialization
    private void initialize() {
        List<Module> modules = Main.getInstance()
                .getModuleRepository()
                .modules();

        for (Module module : modules) {
            if (module.getCategory() == category) {
                moduleComponents.add(new ModuleComponent(module));
            }
        }
    }

    public CategoryComponent(ModuleCategory category) {
        this.category = category;
        initialize();
    }


    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        Matrix4f positionMatrix = context.getMatrices().peek().getPositionMatrix();
        ScissorManager scissorManager = Main.getInstance().getScissorManager();

        int contentHeight = 0;
        for (ModuleComponent component : moduleComponents) {
            int compHeight = component.getComponentHeight();
            contentHeight += compHeight + 2;
        }
        height = 24 + Math.max(0, contentHeight);

        rectangle.render(ShapeProperties.create(context.getMatrices(), x, y, width, height - 3)
                .round(4).thickness(2).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getGuiRectColor(1)).build());

        int titleColor = ColorUtil.getText();
        String title = category.getReadableName();
        float titleWidth = Fonts.getSize(20, Fonts.Type.BOLD).getStringWidth(title);
        Fonts.getSize(20, Fonts.Type.BOLD).drawString(context.getMatrices(), title, (int) (x + (width - titleWidth) / 2f), (int) (y + 6), titleColor);

        float listX = x + 3;
        float listY = y + 20;
        float listW = width - 6;
        float listH = height - 24;

        scissorManager.push(positionMatrix, listX, listY, listW, listH);

        float offset = 0;
        for (ModuleComponent component : moduleComponents) {
            int compHeight = component.getComponentHeight();
            component.x = listX;
            component.y = listY + offset;
            component.width = (int) listW;
            component.render(context, mouseX, mouseY, delta);
            offset += compHeight + 2;
        }

        scissorManager.pop();
        scroll = 0;
        smoothedScroll = 0;
    }

    @Compile
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float listX = x + 3;
        float listY = y + 20;
        float listW = width - 6;
        float listH = height - 24;
        layoutModules(listX, listY, listW);
        moduleComponents.forEach(moduleComponent -> moduleComponent.mouseClicked(mouseX, mouseY, button));
        return super.mouseClicked(mouseX, mouseY, button);
    }


    @Override
    public boolean isHover(double mouseX, double mouseY) {
        return MathUtil.isHovered(mouseX, mouseY, x, y, width, height);
    }


    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        layoutModules(x + 3, y + 20, width - 6);
        moduleComponents.forEach(moduleComponent -> moduleComponent.mouseReleased(mouseX, mouseY, button));
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Compile
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return false;
    }


    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        float listX = x + 3;
        float listY = y + 20;
        float listW = width - 6;
        layoutModules(listX, listY, listW);
        moduleComponents.forEach(moduleComponent -> moduleComponent.mouseDragged(mouseX, mouseY, button, deltaX, deltaY));
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }


    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        layoutModules(x + 3, y + 20, width - 6);
        moduleComponents.forEach(moduleComponent -> moduleComponent.keyPressed(keyCode, scanCode, modifiers));
        return super.keyPressed(keyCode, scanCode, modifiers);
    }


    @Override
    public boolean charTyped(char chr, int modifiers) {
        layoutModules(x + 3, y + 20, width - 6);
        moduleComponents.forEach(moduleComponent -> moduleComponent.charTyped(chr, modifiers));
        return super.charTyped(chr, modifiers);
    }

    private void layoutModules(float listX, float listY, float listW) {
        float offset = 0;
        for (ModuleComponent component : moduleComponents) {
            int compHeight = component.getComponentHeight();
            component.x = listX;
            component.y = listY + offset + (float) smoothedScroll;
            component.width = (int) listW;
            offset += compHeight + 2;
        }
    }


    private void drawCategoryTab(DrawContext context, MatrixStack matrix) {
    }


    private int[] calculateOffsets() { return new int[]{0}; }

    private boolean shouldRenderComponent(ModuleComponent component) { return true; }
}

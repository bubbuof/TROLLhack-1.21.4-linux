package ru.zenith.implement.screens.menu.components.implement.module;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.setting.SettingComponentAdder;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.implement.screens.menu.components.AbstractComponent;
import ru.zenith.implement.screens.menu.components.implement.settings.AbstractSettingComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import static ru.zenith.api.system.font.Fonts.Type.BOLD;

@Getter
public class ModuleComponent extends AbstractComponent {
    private final List<AbstractSettingComponent> components = new ArrayList<>();

    private final Module module;
    private boolean expanded;

    private void initialize() {
        new SettingComponentAdder().addSettingComponent(
                module.settings(),
                components
        );
    }

    public ModuleComponent(Module module) {
        this.module = module;
        initialize();
    }

    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        rectangle.render(ShapeProperties.create(context.getMatrices(), x, y, width, 16)
                .round(2).color(ColorUtil.getGuiRectColor2(1)).build());

        rectangle.render(ShapeProperties.create(context.getMatrices(), x, y, width, height = getComponentHeight())
                .round(3).thickness(1.8F).outlineColor(ColorUtil.getOutline()).color(0x00000000).build());

        String name = module.getVisibleName();
        float textWidth = Fonts.getSize(15, BOLD).getStringWidth(name);
        int textX = (int) (x + (width - textWidth) / 2f);
        int textY = (int) (y + 6);
        int nameColor = module.isState() ? ColorUtil.getClientColor() : 0xFFD4D6E1;
        Fonts.getSize(15, BOLD).drawString(context.getMatrices(), name, textX, textY, nameColor);

        if (expanded) {
            float offsetY = y + 18;
            for (AbstractSettingComponent component : components) {
                Supplier<Boolean> visible = component.getSetting().getVisible();
                if (visible != null && !visible.get()) continue;

                component.x = x;
                component.width = width;
                component.y = offsetY;
                component.render(context, mouseX, mouseY, delta);
                offsetY += component.height + 2;
            }
        }
    }

    @Compile
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (MathUtil.isHovered(mouseX, mouseY, x, y, width, 16)) {
            if (button == 0) {
                module.switchState();
            } else if (button == 1) {
                expanded = !expanded;
            }
            return true;
        }

        if (expanded) {
            layoutSettings();
            for (AbstractSettingComponent abstractComponent : components) {
                abstractComponent.mouseClicked(mouseX, mouseY, button);
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    @Override
    public boolean isHover(double mouseX, double mouseY) {
        if (MathUtil.isHovered(mouseX, mouseY, x, y, width, 16)) return true;
        if (expanded) {
            for (AbstractSettingComponent abstractComponent : components) {
                if (abstractComponent.isHover(mouseX, mouseY)) return true;
            }
        }
        return false;
    }

    
    @Override
    public void tick() {
        components.forEach(AbstractComponent::tick);
        super.tick();
    }

    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (expanded) {
            layoutSettings();
            components.forEach(abstractComponent -> abstractComponent.mouseDragged(mouseX, mouseY, button, deltaX, deltaY));
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (expanded) {
            layoutSettings();
            components.forEach(abstractComponent -> abstractComponent.mouseReleased(mouseX, mouseY, button));
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (expanded) {
            layoutSettings();
            components.forEach(abstractComponent -> abstractComponent.mouseScrolled(mouseX, mouseY, amount));
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Compile
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (expanded) layoutSettings();
        components.forEach(abstractComponent -> abstractComponent.keyPressed(keyCode, scanCode, modifiers));
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    
    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (expanded) {
            layoutSettings();
            components.forEach(abstractComponent -> abstractComponent.charTyped(chr, modifiers));
        }
        return super.charTyped(chr, modifiers);
    }

    
    public int getComponentHeight() {
        if (!expanded) return 16;
        float offsetY = 16;
        for (AbstractSettingComponent component : components) {
            Supplier<Boolean> visible = component.getSetting().getVisible();
            if (visible != null && !visible.get()) continue;
            int h = component.height > 0 ? (int) component.height : getDefaultSettingHeight(component);
            offsetY += h + 2;
        }
        return (int) offsetY;
    }

    private void layoutSettings() {
        float offsetY = y + 18;
        for (AbstractSettingComponent component : components) {
            Supplier<Boolean> visible = component.getSetting().getVisible();
            if (visible != null && !visible.get()) continue;
            component.x = x;
            component.width = width;
            component.y = offsetY;
            int h = component.height > 0 ? (int) component.height : getDefaultSettingHeight(component);
            offsetY += h + 2;
        }
    }

    private int getDefaultSettingHeight(AbstractSettingComponent component) {
        if (component instanceof ru.zenith.implement.screens.menu.components.implement.settings.ValueComponent) return 28;
        return 20;
    }

    
    private void drawBind(DrawContext context) { }

    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModuleComponent that = (ModuleComponent) o;
        return module.equals(that.module);
    }

    
    @Override
    public int hashCode() {
        return Objects.hash(module);
    }
}

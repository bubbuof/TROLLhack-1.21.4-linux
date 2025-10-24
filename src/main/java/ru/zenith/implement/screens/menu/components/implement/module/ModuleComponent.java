package ru.zenith.implement.screens.menu.components.implement.module;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.ColorHelper;
import org.lwjgl.glfw.GLFW;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.setting.SettingComponentAdder;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.common.util.other.StringUtil;
import ru.zenith.implement.screens.menu.components.AbstractComponent;
import ru.zenith.implement.screens.menu.components.implement.other.CheckComponent;
import ru.zenith.implement.screens.menu.components.implement.settings.AbstractSettingComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import static ru.zenith.api.system.font.Fonts.Type.BOLD;

@Getter
public class ModuleComponent extends AbstractComponent {
    private final List<AbstractSettingComponent> components = new ArrayList<>();

    private final CheckComponent checkComponent = new CheckComponent();

    private final Module module;
    private boolean binding;

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
        boolean isHovered = MathUtil.isHovered(mouseX, mouseY, x, y, width, height);
        
        // Enhanced GameSense/Skeet.cc style module container
        rectangle.render(ShapeProperties.create(context.getMatrices(), x, y, width, height = getComponentHeight())
                .round(3).softness(0.5f).thickness(1).outlineColor(isHovered ? ColorUtil.getSkeetAccent(0.6f) : ColorUtil.getSkeetBorder())
                .color(ColorUtil.getSkeetSecondary()).build());

        // Module header background with gradient
        rectangle.render(ShapeProperties.create(context.getMatrices(), x, y, width, 18).round(3, 3, 0, 0)
                .color(ColorUtil.getSkeetBackground(), ColorUtil.getSkeetBackground(),
                       ColorUtil.getSkeetSecondary(), ColorUtil.getSkeetSecondary()).build());

        // Module status indicator
        if (module.isState()) {
            rectangle.render(ShapeProperties.create(context.getMatrices(), x, y, 3, 18)
                    .color(ColorUtil.getSkeetAccent()).build());
        }

        // Module name with enhanced styling
        Fonts.getSize(14, BOLD).drawString(context.getMatrices(), module.getVisibleName(), x + 10, y + 8, 
                module.isState() ? ColorUtil.getSkeetAccent() : ColorUtil.getSkeetText());

        // Enable section with better spacing
        Fonts.getSize(14, BOLD).drawString(context.getMatrices(), "Enable", x + 9, y + 27, ColorUtil.getSkeetText());
        Fonts.getSize(12).drawString(context.getMatrices(), "Enables the " + module.getVisibleName().toLowerCase() + " feature.", 
                x + 9, y + 36, ColorUtil.getSkeetTextSecondary());

        ((CheckComponent) checkComponent.position(x + width - 16, y + 28.5F)).setRunnable(module::switchState).setState(module.isState()).render(context, mouseX, mouseY, delta);

        drawBind(context);

        // Enhanced separator line between header and settings
        if (!components.isEmpty()) {
            rectangle.render(ShapeProperties.create(context.getMatrices(), x + 5, y + 42, width - 10, 1)
                    .color(ColorUtil.getSkeetBorder()).build());
            rectangle.render(ShapeProperties.create(context.getMatrices(), x + 5, y + 41.5f, width - 10, 0.5f)
                    .color(ColorUtil.getSkeetAccent(0.3f)).build());
        }

        float offset = y + 46;
        for (int i = components.size() - 1; i >= 0; i--) {
            AbstractSettingComponent component = components.get(i);
            Supplier<Boolean> visible = component.getSetting().getVisible();

            if (visible != null && !visible.get()) {
                continue;
            }

            component.x = x;
            component.y = offset + (getComponentHeight() - 50 - component.height);
            component.width = width;

            component.render(context, mouseX, mouseY, delta);

            offset -= component.height;
        }
    }

    @Compile
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean isAnyComponentHovered = components.stream().anyMatch(abstractComponent -> abstractComponent.isHover(mouseX, mouseY));

        if (isAnyComponentHovered) {
            components.forEach(abstractComponent -> {
                if (abstractComponent.isHover(mouseX, mouseY)) {
                    abstractComponent.mouseClicked(mouseX, mouseY, button);
                }
            });
            return super.mouseClicked(mouseX, mouseY, button);
        } else {
            String bindName = StringUtil.getBindName(module.getKey());
            float stringWidth = Fonts.getSize(12, BOLD).getStringWidth(bindName);
            if (MathUtil.isHovered(mouseX, mouseY, x + width - 15 - stringWidth, y + 8, stringWidth + 6, 9) && button == 0) {
                binding = !binding;
            } else if (binding) {
                module.setKey(button);
                binding = false;
            }
        }

        checkComponent.mouseClicked(mouseX, mouseY, button);
        components.forEach(abstractComponent -> abstractComponent.mouseClicked(mouseX, mouseY, button));
        return super.mouseClicked(mouseX, mouseY, button);
    }

    
    @Override
    public boolean isHover(double mouseX, double mouseY) {
        for (AbstractComponent abstractComponent : components) {
            if (abstractComponent.isHover(mouseX, mouseY)) {
                return true;
            }
        }
        return MathUtil.isHovered(mouseX, mouseY, x, y, width, height);
    }

    
    @Override
    public void tick() {
        components.forEach(AbstractComponent::tick);
        super.tick();
    }

    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        components.forEach(abstractComponent -> abstractComponent.mouseDragged(mouseX, mouseY, button, deltaX, deltaY));
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        components.forEach(abstractComponent -> abstractComponent.mouseReleased(mouseX, mouseY, button));
        return super.mouseReleased(mouseX, mouseY, button);
    }

    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        components.forEach(abstractComponent -> abstractComponent.mouseScrolled(mouseX, mouseY, amount));
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Compile
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int key = keyCode == GLFW.GLFW_KEY_DELETE ? -1 : keyCode;
        if (binding) {
            module.setKey(key);
            binding = false;
        }
        components.forEach(abstractComponent -> abstractComponent.keyPressed(keyCode, scanCode, modifiers));
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    
    @Override
    public boolean charTyped(char chr, int modifiers) {
        components.forEach(abstractComponent -> abstractComponent.charTyped(chr, modifiers));
        return super.charTyped(chr, modifiers);
    }

    
    public int getComponentHeight() {
        float offsetY = 0;
        for (AbstractSettingComponent component : components) {
            Supplier<Boolean> visible = component.getSetting().getVisible();

            if (visible != null && !visible.get()) {
                continue;
            }

            offsetY += component.height;
        }
        return (int) (offsetY + 50);
    }

    
    private void drawBind(DrawContext context) {
        String bindName = StringUtil.getBindName(module.getKey());
        String name = binding ? "(" + bindName + ") ..." : bindName;
        float stringWidth = Fonts.getSize(12, BOLD).getStringWidth(name);

        // GameSense/Skeet.cc style bind display
        rectangle.render(ShapeProperties.create(context.getMatrices(), x + width - stringWidth - 15, y + 4.5F, stringWidth + 6, 9)
                .round(0).thickness(1).outlineColor(ColorUtil.getSkeetBorder()).color(ColorUtil.getSkeetSecondary()).build());

        int bindingColor = binding ? ColorUtil.getSkeetAccent() : ColorUtil.getSkeetTextSecondary();
        Fonts.getSize(12, BOLD).drawString(context.getMatrices(), name, x + width - 12 - stringWidth, y + 8, bindingColor);
    }

    
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

package ru.zenith.implement.screens.menu.components.implement.window.implement.settings.color;

import net.minecraft.client.gui.DrawContext;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.zenith.api.feature.module.setting.implement.ColorSetting;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.implement.screens.menu.components.AbstractComponent;
import ru.zenith.implement.screens.menu.components.implement.window.AbstractWindow;
import ru.zenith.implement.screens.menu.components.implement.window.implement.settings.color.component.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ColorWindow extends AbstractWindow {
    private final List<AbstractComponent> components = new ArrayList<>();

    private final HueComponent hueComponent;
    private final SaturationComponent saturationComponent;
    private final AlphaComponent alphaComponent;
    private final ColorEditorComponent colorEditorComponent;
    private final ColorPresetComponent colorPresetComponent;

    public ColorWindow(ColorSetting setting) {

        components.addAll(
                Arrays.asList(
                        hueComponent = new HueComponent(setting),
                        saturationComponent = new SaturationComponent(setting),
                        alphaComponent = new AlphaComponent(setting),
                        colorEditorComponent = new ColorEditorComponent(setting),
                        colorPresetComponent = new ColorPresetComponent(setting)
                )
        );
    }
    
    @Override
    public void drawWindow(DrawContext context, int mouseX, int mouseY, float delta) {
        rectangle.render(ShapeProperties.create(context.getMatrices(), x, y, width, height)
                .round(6).thickness(2).softness(1).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getMainGuiColor()).build());

        alphaComponent.position(x, y);
        hueComponent.position(x, y);
        saturationComponent.position(x, y);
        colorEditorComponent.position(x, y);

        height = ((ColorPresetComponent) colorPresetComponent.position(x, y))
                .getWindowHeight() + 10;

        components.forEach(component -> component.render(context, mouseX, mouseY, delta));
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        draggable(MathUtil.isHovered(mouseX, mouseY, x, y, width, 17));
        components.forEach(component -> component.mouseClicked(mouseX, mouseY, button));
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        components.forEach(component -> component.mouseScrolled(mouseX, mouseY, amount));
        return super.mouseScrolled(mouseX, mouseY, amount);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        components.forEach(component -> component.mouseReleased(mouseX, mouseY, button));
        return super.mouseReleased(mouseX, mouseY, button);
    }
}

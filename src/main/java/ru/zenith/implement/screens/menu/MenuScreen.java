package ru.zenith.implement.screens.menu;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.system.animation.Animation;
import ru.zenith.api.system.animation.Direction;
import ru.zenith.api.system.animation.implement.DecelerateAnimation;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.api.system.sound.SoundManager;
import ru.zenith.common.QuickImports;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.implement.screens.menu.components.AbstractComponent;
import ru.zenith.implement.screens.menu.components.implement.other.*;
import ru.zenith.implement.screens.menu.components.implement.settings.TextComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ru.zenith.api.system.animation.Direction.BACKWARDS;
import static ru.zenith.api.system.animation.Direction.FORWARDS;

@Setter
@Getter
public class MenuScreen extends Screen implements QuickImports {
    public static MenuScreen INSTANCE = new MenuScreen();
    private final List<AbstractComponent> components = new ArrayList<>();
    private final CategoryContainerComponent categoryContainerComponent = new CategoryContainerComponent();
    public final Animation animation = new DecelerateAnimation().setMs(200).setValue(1);
    public ModuleCategory category = ModuleCategory.COMBAT;
    public int x, y, width, height;

    public void initialize() {
        animation.setDirection(FORWARDS);
        categoryContainerComponent.initializeCategoryComponents();
        components.addAll(Arrays.asList(categoryContainerComponent));
    }

    public MenuScreen() {
        super(Text.of("MenuScreen"));
        initialize();
    }

    @Override
    public void tick() {
        close();
        components.forEach(AbstractComponent::tick);
        super.tick();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        x = window.getScaledWidth() / 2 - 318;
        y = window.getScaledHeight() / 2 - 160;
        width = 636;
        height = 320;

        categoryContainerComponent.position(x, y);

        MathUtil.scale(context.getMatrices(), x + (float) width / 2, y + (float) height / 2, getScaleAnimation(), () -> {
            components.forEach(component -> component.render(context, mouseX, mouseY, delta));
            windowManager.render(context, mouseX, mouseY, delta);
        });
        super.render(context, mouseX, mouseY, delta);
    }

    public void openGui() {
        animation.setDirection(Direction.FORWARDS);
        mc.setScreen(this);
        SoundManager.playSound(SoundManager.OPEN_GUI);
    }

    public float getScaleAnimation() {
        return animation.getOutput().floatValue();
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double[] p = unscale(mouseX, mouseY);
        if (windowManager.mouseClicked(p[0], p[1], button)) return true;
        boolean handled = false;
        for (AbstractComponent component : components) {
            handled |= component.mouseClicked(p[0], p[1], button);
        }
        return handled || super.mouseClicked(mouseX, mouseY, button);
    }


    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        double[] p = unscale(mouseX, mouseY);
        boolean handled = false;
        for (AbstractComponent component : components) {
            handled |= component.mouseReleased(p[0], p[1], button);
        }
        handled |= windowManager.mouseReleased(p[0], p[1], button);
        return handled || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        double[] p = unscale(mouseX, mouseY);
        if (windowManager.mouseDragged(p[0], p[1], button, deltaX, deltaY)) return true;
        boolean handled = false;
        for (AbstractComponent component : components) {
            handled |= component.mouseDragged(p[0], p[1], button, deltaX, deltaY);
        }
        return handled || super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }


    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        double[] p = unscale(mouseX, mouseY);
        if (windowManager.mouseScrolled(p[0], p[1], vertical)) return true;
        boolean handled = false;
        for (AbstractComponent component : components) {
            handled |= component.mouseScrolled(p[0], p[1], vertical);
        }
        return handled || super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }


    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && shouldCloseOnEsc()) {
            SoundManager.playSound(SoundManager.CLOSE_GUI);
            animation.setDirection(BACKWARDS);
            return true;
        }

        if (windowManager.keyPressed(keyCode, scanCode, modifiers)) return true;
        boolean handled = false;
        for (AbstractComponent component : components) {
            handled |= component.keyPressed(keyCode, scanCode, modifiers);
        }
        return handled || super.keyPressed(keyCode, scanCode, modifiers);
    }


    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (windowManager.charTyped(chr, modifiers)) return true;
        boolean handled = false;
        for (AbstractComponent component : components) {
            handled |= component.charTyped(chr, modifiers);
        }
        return handled || super.charTyped(chr, modifiers);
    }


    @Override
    public boolean shouldPause() {
        return false;
    }


    @Override
    public void close() {
        if (animation.isFinished(BACKWARDS)) {
            TextComponent.typing = false;
            super.close();
        }
    }

    private double[] unscale(double mx, double my) {
        float s = getScaleAnimation();
        if (s == 0) s = 1f;
        float cx = x + (float) width / 2f;
        float cy = y + (float) height / 2f;
        double ux = (mx - cx) / s + cx;
        double uy = (my - cy) / s + cy;
        return new double[]{ux, uy};
    }
}

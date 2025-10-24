package ru.zenith.implement.screens.menu.components.implement.other;

import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.DrawContext;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.kotopushka.compiler.sdk.annotations.Initialization;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.common.util.entity.PlayerInventoryComponent;
import ru.zenith.implement.screens.menu.MenuScreen;
import ru.zenith.implement.screens.menu.components.AbstractComponent;
import ru.zenith.implement.screens.menu.components.implement.category.CategoryComponent;
import ru.zenith.implement.screens.menu.components.implement.settings.TextComponent;

import java.util.ArrayList;
import java.util.List;

@Setter
@Accessors(chain = true)
public class CategoryContainerComponent extends AbstractComponent {
    private final List<CategoryComponent> categoryComponents = new ArrayList<>();


    @Compile
    @Initialization
    public void initializeCategoryComponents() {
        categoryComponents.clear();
        for (ModuleCategory category : ModuleCategory.values()) {
            categoryComponents.add(new CategoryComponent(category));
        }
    }

    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float offset = 0;

        for (CategoryComponent component : categoryComponents) {
            component.x = x + 6;
            component.y = y + 50 + offset;
            component.width = 73;
            component.height = 17;
            component.render(context, mouseX, mouseY, delta);
            offset += component.height + 2;
        }
    }

    @Override
    public void tick() {
        if (TextComponent.typing || SearchComponent.typing) PlayerInventoryComponent.unPressMoveKeys();
        else PlayerInventoryComponent.updateMoveKeys();
        categoryComponents.forEach(AbstractComponent::tick);
        super.tick();
    }

    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        categoryComponents.forEach(categoryComponent -> categoryComponent.mouseClicked(mouseX, mouseY, button));
        return super.mouseClicked(mouseX, mouseY, button);
    }

    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        categoryComponents.forEach(categoryComponent -> categoryComponent.mouseReleased(mouseX, mouseY, button));
        return super.mouseReleased(mouseX, mouseY, button);
    }

    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        categoryComponents.forEach(categoryComponent -> categoryComponent.mouseDragged(mouseX, mouseY, button, deltaX, deltaY));
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        categoryComponents.forEach(categoryComponent -> categoryComponent.mouseScrolled(mouseX, mouseY, amount));
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        categoryComponents.forEach(categoryComponent -> categoryComponent.keyPressed(keyCode, scanCode, modifiers));
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    
    @Override
    public boolean charTyped(char chr, int modifiers) {
        categoryComponents.forEach(categoryComponent -> categoryComponent.charTyped(chr, modifiers));
        return super.charTyped(chr, modifiers);
    }
}

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
    private String searchQuery = ""; // Добавлено поле для поиска

    @Compile
    @Initialization
    public void initializeCategoryComponents() {
        categoryComponents.clear();
        for (ModuleCategory category : ModuleCategory.values()) {
            categoryComponents.add(new CategoryComponent(category));
        }
    }

    // Добавлен метод для установки поискового запроса
    public void setSearchQuery(String query) {
        this.searchQuery = query != null ? query.toLowerCase() : "";
        // Передаем поисковый запрос всем категориям
        categoryComponents.forEach(component -> component.setSearchQuery(this.searchQuery));
    }

    private double globalScroll = 0;

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float offsetX = 0;

        for (int i = 0; i < categoryComponents.size(); i++) {
            CategoryComponent component = categoryComponents.get(i);
            component.x = x + 6 + offsetX;
            component.y = (float) (y + 12 + globalScroll);
            component.width = 120;
            component.render(context, mouseX, mouseY, delta);
            offsetX += component.width + 6;
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
        globalScroll += amount * 20;
        return true;
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
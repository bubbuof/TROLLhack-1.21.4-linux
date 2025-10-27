package ru.zenith.implement.screens.menu.components.implement.settings;

import net.minecraft.client.gui.DrawContext;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.zenith.api.feature.module.setting.implement.GroupSetting;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.common.util.other.StringUtil;
import ru.zenith.implement.screens.menu.components.implement.other.CheckComponent;
import ru.zenith.implement.screens.menu.components.implement.other.SettingComponent;
import ru.zenith.implement.screens.menu.components.implement.window.AbstractWindow;
import ru.zenith.implement.screens.menu.components.implement.window.implement.settings.group.GroupWindow;

import static ru.zenith.api.system.font.Fonts.Type.BOLD;

public class GroupComponent extends AbstractSettingComponent {
    private final CheckComponent checkComponent = new CheckComponent();
    private final SettingComponent settingComponent = new SettingComponent();

    private final GroupSetting setting;

    public GroupComponent(GroupSetting setting) {
        super(setting);
        this.setting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        height = 20;

        Fonts.getSize(14, BOLD).drawString(context.getMatrices(), setting.getName(), x + 6, y +9, 0xFFD4D6E1);

        ((CheckComponent) checkComponent.position(x + width - 16, y + 6.5F))
                .setRunnable(() -> setting.setValue(!setting.isValue()))
                .setState(setting.isValue())
                .render(context, mouseX, mouseY, delta);

        ((SettingComponent) settingComponent.position(x + width - 28, y + 7))
                .setRunnable(() -> spawnWindow(mouseX, mouseY))
                .render(context, mouseX, mouseY, delta);
    }

    @Compile
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && ru.zenith.common.util.math.MathUtil.isHovered(mouseX, mouseY, x, y, width, height)) {
            spawnWindow((int) mouseX, (int) mouseY);
        }

        checkComponent.mouseClicked(mouseX, mouseY, button);
        settingComponent.mouseClicked(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }


    private void spawnWindow(int mouseX, int mouseY) {
        AbstractWindow existingWindow = null;

        for (AbstractWindow window : windowManager.getWindows()) {
            if (window instanceof GroupWindow && ((GroupWindow) window).getSetting() == setting) {
                existingWindow = window;
                break;
            }
        }

        if (existingWindow != null) {
            windowManager.delete(existingWindow);
        } else {
            AbstractWindow groupWindow = new GroupWindow(setting)
                    .position(mouseX + 5, mouseY + 5)
                    .size(137, 23)
                    .draggable(false);

            windowManager.add(groupWindow);
        }
    }
}

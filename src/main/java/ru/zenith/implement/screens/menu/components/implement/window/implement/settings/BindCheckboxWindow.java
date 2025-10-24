package ru.zenith.implement.screens.menu.components.implement.window.implement.settings;

import lombok.RequiredArgsConstructor;
import ru.zenith.api.feature.module.setting.implement.BooleanSetting;
import ru.zenith.implement.screens.menu.components.implement.window.implement.AbstractBindWindow;

@RequiredArgsConstructor
public class BindCheckboxWindow extends AbstractBindWindow {
    private final BooleanSetting setting;

    @Override
    protected int getKey() {
        return setting.getKey();
    }

    @Override
    protected void setKey(int key) {
        setting.setKey(key);
    }

    @Override
    protected int getType() {
        return setting.getType();
    }

    @Override
    protected void setType(int type) {
        setting.setType(type);
    }
}
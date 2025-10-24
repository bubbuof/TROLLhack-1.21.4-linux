package ru.zenith.implement.screens.menu.components.implement.settings;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.zenith.api.feature.module.setting.Setting;
import ru.zenith.implement.screens.menu.components.AbstractComponent;

@Getter
@RequiredArgsConstructor
public abstract class AbstractSettingComponent extends AbstractComponent {
    private final Setting setting;
}

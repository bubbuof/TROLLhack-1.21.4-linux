package ru.zenith.common.trait;

import ru.zenith.api.feature.module.setting.Setting;

public interface Setupable {
    void setup(Setting... settings);
}
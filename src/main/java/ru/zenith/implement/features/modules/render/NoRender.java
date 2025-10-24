package ru.zenith.implement.features.modules.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.MultiSelectSetting;
import ru.zenith.common.util.other.Instance;

@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class NoRender extends Module {
    public static NoRender getInstance() {
        return Instance.get(NoRender.class);
    }

    public MultiSelectSetting modeSetting = new MultiSelectSetting("Elements", "Select elements to be ignored")
            .value("Fire", "Bad Effects", "Block Overlay");

    public NoRender() {
        super("NoRender","No Render",ModuleCategory.RENDER);
        setup(modeSetting);
    }

}

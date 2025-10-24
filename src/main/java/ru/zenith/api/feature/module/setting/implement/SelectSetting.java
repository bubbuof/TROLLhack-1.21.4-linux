package ru.zenith.api.feature.module.setting.implement;

import lombok.Getter;
import lombok.Setter;
import ru.zenith.api.feature.module.setting.Setting;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;


@Getter
public class SelectSetting extends Setting {
    @Setter
    private String selected;
    private List<String> list;

    public SelectSetting(String name, String description) {
        super(name, description);
    }

    public SelectSetting value(String... values) {
        List<String> list = Arrays.asList(values);

        selected = list.getFirst();
        this.list = list;

        return this;
    }

    public SelectSetting visible(Supplier<Boolean> visible) {
        setVisible(visible);
        return this;
    }

    public void setVisible(Supplier<Boolean> visible) {
        super.setVisible(visible);
    }

    public SelectSetting selected(String string) {
        this.selected = string;
        return this;
    }

    public boolean isSelected(String name) {
        return selected.equalsIgnoreCase(name);
    }

    public String getSelected() {
        return selected;
    }
}

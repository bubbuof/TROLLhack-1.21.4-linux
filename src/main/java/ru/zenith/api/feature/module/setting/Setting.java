package ru.zenith.api.feature.module.setting;

import lombok.Getter;
import lombok.Setter;

import java.util.function.Supplier;

@Getter
public class Setting {
    private final String name;
    private String description;

    @Setter
    private Supplier<Boolean> visible;

    public Setting(String name) {
        this.name = name;
    }

    public Setting(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public boolean isVisible() {
        return visible == null || visible.get();
    }
}

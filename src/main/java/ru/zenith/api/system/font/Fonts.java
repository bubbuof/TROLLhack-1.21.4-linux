package ru.zenith.api.system.font;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import ru.zenith.core.Main;

import java.awt.*;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Fonts {

    @SneakyThrows
    public static FontRenderer create(float size, String name) {
        String path = "assets/minecraft/fonts/" + name + ".otf";

        try (InputStream inputStream = Main.class.getClassLoader().getResourceAsStream(path)) {
            if (inputStream == null || name.equals("minecraft")) {
                // Fallback to default font if minecraft font is not found or requested
                return create(size, "sfpromedium");
            }
            Font font = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(inputStream))
                    .deriveFont(Font.PLAIN, size / 2f);

            return new FontRenderer(font, size / 2f);
        }
    }

    private static final Map<FontKey, FontRenderer> fontCache = new HashMap<>();

    public static void init() {
        for (Type type : Type.values()) {
            for (int size = 4; size <= 32; size++) {
                fontCache.put(new FontKey(size, type), create(size, type.getType()));
            }
        }
    }

    public static FontRenderer getSize(int size) {
        return getSize(size, Type.BOLD);
    }

    public static FontRenderer getSize(int size, Type type) {
        return fontCache.computeIfAbsent(new FontKey(size, type), k -> create(size, type.getType()));
    }

    @Getter
    @RequiredArgsConstructor
    public enum Type {
        DEFAULT("sfpromedium"),
        BOLD("sfprosemibold"),
        MINECRAFT("minecraft");

        private final String type;
    }

    private record FontKey(int size, Type type) {
    }
}
package ru.zenith.implement.events.render;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import ru.zenith.api.event.events.Event;
import ru.zenith.common.util.color.ColorUtil;

@Setter
@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TextFactoryEvent implements Event {
    String text;

    public void replaceText(String protect, String replaced) {
        if (text == null || text.isEmpty()) return;

        if (text.contains(protect)) {
            if (text.equalsIgnoreCase(protect) || text.contains(protect + " ") || text.contains(" " + protect) || text.contains("⏏" + protect) || text.contains(protect + "§")) {
                text = text.replace(protect, replaced);
            }
        }
    }
}

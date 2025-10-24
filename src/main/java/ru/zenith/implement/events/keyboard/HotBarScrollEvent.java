package ru.zenith.implement.events.keyboard;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import ru.zenith.api.event.events.callables.EventCancellable;

@Getter
@Setter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotBarScrollEvent extends EventCancellable {
    private double horizontal, vertical;
}

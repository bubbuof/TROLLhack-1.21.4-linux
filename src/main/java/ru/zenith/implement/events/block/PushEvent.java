package ru.zenith.implement.events.block;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.zenith.api.event.events.callables.EventCancellable;

@Getter
@AllArgsConstructor
public class PushEvent extends EventCancellable {
    private Type type;

    public enum Type {
        COLLISION, BLOCK, WATER
    }
}

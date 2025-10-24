package ru.zenith.implement.events.item;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import ru.zenith.api.event.events.callables.EventCancellable;

@Getter
@Setter
@AllArgsConstructor
public class UsingItemEvent extends EventCancellable {
    byte type;
}

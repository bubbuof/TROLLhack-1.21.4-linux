package ru.zenith.implement.events.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.zenith.api.event.events.Event;

@Getter
@AllArgsConstructor
public class RotationUpdateEvent implements Event {
    byte type;
}

package ru.zenith.implement.events.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.math.Vec3d;
import ru.zenith.api.event.events.callables.EventCancellable;

@Setter
@Getter
@AllArgsConstructor
public class SwimmingEvent extends EventCancellable {
    Vec3d vector;
}

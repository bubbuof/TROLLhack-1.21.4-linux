package ru.zenith.implement.events.player;

import lombok.*;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Block;
import ru.zenith.api.event.events.callables.EventCancellable;

@Setter
@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlayerCollisionEvent extends EventCancellable {
    private Block block;

}

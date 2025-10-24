package ru.zenith.implement.events.player;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;
import ru.zenith.api.event.events.Event;

@AllArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HeldItemRendererEvent implements Event {
    public Hand hand;
    public MatrixStack matrix;
}

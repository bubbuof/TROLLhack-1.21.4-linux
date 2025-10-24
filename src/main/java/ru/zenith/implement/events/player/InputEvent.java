package ru.zenith.implement.events.player;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.PlayerInput;
import ru.zenith.api.event.events.callables.EventCancellable;

@Getter
@Setter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InputEvent extends EventCancellable {
    PlayerInput input;

    public void setJumping(boolean jump) {
        input = new PlayerInput(input.forward(), input.backward(), input.left(), input.right(), jump, input.sneak(), input.sprint());
    }

    public void setDirectional(boolean forward, boolean backward, boolean left, boolean right) {
        input = new PlayerInput(forward, backward, left, right, input.jump(), input.sneak(), input.sprint());
    }

    public void inputNone() {
        input = new PlayerInput(false, false, false, false, false, false, false);
    }

    public int forward() {
        return input.forward() ? 1 : input.backward() ? -1 : 0;
    }

    public float sideways() {
        return input.left() ? 1 : input.right() ? -1 : 0;
    }
}
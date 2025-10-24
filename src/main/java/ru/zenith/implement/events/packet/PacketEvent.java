package ru.zenith.implement.events.packet;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.network.packet.Packet;
import ru.zenith.api.event.events.callables.EventCancellable;
@Getter @Setter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PacketEvent extends EventCancellable {
    Packet<?> packet;
    Type type;

    public boolean isSend() {
        return type.equals(Type.SEND);
    }

    public enum Type {
        SEND, RECEIVE
    }
}

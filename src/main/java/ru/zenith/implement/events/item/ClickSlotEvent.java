package ru.zenith.implement.events.item;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.screen.slot.SlotActionType;
import ru.zenith.api.event.events.callables.EventCancellable;

@Getter @Setter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClickSlotEvent extends EventCancellable {
    int windowId, slotId, button;
    SlotActionType actionType;
}

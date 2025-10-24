package ru.zenith.implement.events.block;

import net.minecraft.block.entity.BlockEntity;
import ru.zenith.api.event.events.Event;

public record BlockEntityProgressEvent(BlockEntity blockEntity, Type type) implements Event {
    public enum Type {
        ADD, REMOVE
    }
}

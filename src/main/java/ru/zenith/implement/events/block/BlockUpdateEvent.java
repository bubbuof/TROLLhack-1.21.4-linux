package ru.zenith.implement.events.block;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import ru.zenith.api.event.events.Event;

public record BlockUpdateEvent(BlockState state, BlockPos pos, Type type) implements Event {
    public enum Type {
        LOAD, UNLOAD, UPDATE
    }
}

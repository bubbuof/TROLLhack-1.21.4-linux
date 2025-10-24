package ru.zenith.implement.events.block;

import net.minecraft.util.math.BlockPos;
import ru.zenith.api.event.events.Event;

public record BreakBlockEvent(BlockPos blockPos) implements Event {}

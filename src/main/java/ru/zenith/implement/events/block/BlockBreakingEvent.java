package ru.zenith.implement.events.block;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import ru.zenith.api.event.events.Event;

public record BlockBreakingEvent(BlockPos blockPos, Direction direction) implements Event {}

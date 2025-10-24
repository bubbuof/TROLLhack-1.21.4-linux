package ru.zenith.implement.events.block;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import ru.zenith.api.event.events.Event;

public record BlockCollisionEvent(BlockPos blockPos, BlockState state) implements Event {}


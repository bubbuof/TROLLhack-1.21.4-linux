package ru.zenith.api.repository.way;

import net.minecraft.util.math.BlockPos;

public record Way(String name, BlockPos pos, String server) {}

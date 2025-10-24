package ru.zenith.common.util.entity;

import net.minecraft.util.math.Vec3d;

public interface PlayerSimulation {
    Vec3d pos();

    void tick();
}
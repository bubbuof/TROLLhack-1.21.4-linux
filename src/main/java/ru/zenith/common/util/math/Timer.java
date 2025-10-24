package ru.zenith.common.util.math;

import lombok.Getter;

@Getter
public class Timer {
    private long time;

    public Timer() {
        reset();
    }

    public void reset() {
        this.time = System.nanoTime();
    }

    public long getPassedTimeMs() {
        return (System.nanoTime() - time) / 1000000L;
    }

    public boolean passedMs(long ms) {
        return getPassedTimeMs() >= ms;
    }

    public static Timer create() {
        return new Timer();
    }
}

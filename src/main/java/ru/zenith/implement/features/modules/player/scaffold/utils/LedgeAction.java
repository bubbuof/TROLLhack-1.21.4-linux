package ru.zenith.implement.features.modules.player.scaffold.utils;

public class LedgeAction {
    public static final LedgeAction NO_LEDGE = new LedgeAction(false, false, false, 0);

    private final boolean jump;
    private final boolean stopInput;
    private final boolean stepBack;
    private final int sneakTime;

    public LedgeAction(boolean jump, boolean stopInput, boolean stepBack, int sneakTime) {
        this.jump = jump;
        this.stopInput = stopInput;
        this.stepBack = stepBack;
        this.sneakTime = sneakTime;
    }

    // Getters
    public boolean shouldJump() { return jump; }
    public boolean shouldStopInput() { return stopInput; }
    public boolean shouldStepBack() { return stepBack; }
    public int getSneakTime() { return sneakTime; }
}
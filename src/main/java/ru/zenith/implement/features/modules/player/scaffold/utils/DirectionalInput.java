package ru.zenith.implement.features.modules.player.scaffold.utils;

public class DirectionalInput {
    public static final DirectionalInput NONE = new DirectionalInput(false, false, false, false);

    private final boolean forward;
    private final boolean backward;
    private final boolean left;
    private final boolean right;

    public DirectionalInput(boolean forward, boolean backward, boolean left, boolean right) {
        this.forward = forward;
        this.backward = backward;
        this.left = left;
        this.right = right;
    }

    public boolean isNone() {
        return !forward && !backward && !left && !right;
    }

    public boolean isForward() { return forward && !backward && !left && !right; }
    public boolean isBackward() { return !forward && backward && !left && !right; }
    public boolean isLeft() { return !forward && !backward && left && !right; }
    public boolean isRight() { return !forward && !backward && !left && right; }
    public boolean isForwardLeft() { return forward && !backward && left && !right; }
    public boolean isForwardRight() { return forward && !backward && !left && right; }
    public boolean isBackwardLeft() { return !forward && backward && left && !right; }
    public boolean isBackwardRight() { return !forward && backward && !left && right; }
}
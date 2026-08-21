package top.fpsmaster.prism.input;

/** One mouse-button press this frame, in toolkit units. */
public final class PointerEvent {
    public final int x;
    public final int y;
    public final int button;

    public PointerEvent(int x, int y, int button) {
        this.x = x;
        this.y = y;
        this.button = button;
    }
}

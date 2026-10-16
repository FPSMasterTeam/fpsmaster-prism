package top.fpsmaster.prism.input;

/**
 * Virtual key codes for {@link Input#consumeKey(int)}. Hosts translate LWJGL2 / GLFW into these.
 */
public final class Keys {
    public static final int BACKSPACE = 1;
    public static final int ENTER = 2;
    public static final int ESCAPE = 3;
    public static final int LEFT = 4;
    public static final int RIGHT = 5;
    public static final int DELETE = 6;
    /** Caret to start of line. */
    public static final int HOME = 7;
    /** Caret to end of line. */
    public static final int END = 8;
    /**
     * Editing shortcuts. Hosts map Ctrl+A/C/X/V (Cmd on macOS) to these instead of typing the
     * letter; text fields read and write {@link Input#clipboard()} / {@link Input#setClipboard}.
     */
    public static final int SELECT_ALL = 9;
    public static final int COPY = 10;
    public static final int CUT = 11;
    public static final int PASTE = 12;
    /**
     * Modifier, read with {@link Input#isKeyDown}: hosts keep it in sync with either Shift key
     * ({@code FrameInput.setKeyDown(Keys.SHIFT, down)}) so LEFT/RIGHT/HOME/END extend the selection.
     */
    public static final int SHIFT = 13;

    private Keys() {
    }
}

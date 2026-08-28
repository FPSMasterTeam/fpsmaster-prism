package top.fpsmaster.prism.input;

/**
 * Per-frame pointer + key state. Presses are a queue that widgets <em>consume</em> so a click
 * cannot fire two controls. Hover z-order is paint order: the last widget to {@link #markHovered}
 * this frame is hovered next frame.
 */
public interface Input {
    int mouseX();

    int mouseY();

    boolean isButtonDown(int button);

    PointerEvent consumePressInBounds(float x, float y, float w, float h, int button);

    PointerEvent consumePressOutside(float x, float y, float w, float h);

    /** Look at an unconsumed outside press without claiming it. */
    boolean hasPressOutside(float x, float y, float w, float h);

    int consumeWheelDelta(float x, float y, float w, float h);

    /**
     * 丢掉这一帧的滚轮，不看鼠标在哪。模态用的，见
     * {@link FrameInput#discardWheel()}。
     */
    void discardWheel();

    void markHovered(Object id, float x, float y, float w, float h);

    boolean wasHovered(Object id);

    boolean beginDrag(Object owner, int button, float x, float y, float w, float h);

    boolean isDragging(Object owner);

    void releaseDrag(Object owner);

    boolean isKeyDown(int keyCode);

    /** One-shot key this frame (virtual {@link Keys} codes). */
    boolean consumeKey(int keyCode);

    /** One-shot platform key code for bind editors (LWJGL2 / GLFW). */
    int consumeRawKey();

    String typedChars();

    /**
     * Same characters as {@link #typedChars()}, but taken out of the frame: whoever reads them
     * first is the only reader.
     *
     * <p>Text fields must use this. {@link #typedChars()} hands the same batch to every caller,
     * so two fields drawn in the same frame both append it — the player types into one box and
     * the other silently grows the same characters.
     */
    String consumeTypedChars();

    String clipboard();

    void setClipboard(String text);
}

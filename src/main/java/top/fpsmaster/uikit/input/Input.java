package top.fpsmaster.uikit.input;

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

    int consumeWheelDelta(float x, float y, float w, float h);

    void markHovered(Object id, float x, float y, float w, float h);

    boolean wasHovered(Object id);

    boolean beginDrag(Object owner, int button, float x, float y, float w, float h);

    boolean isDragging(Object owner);

    void releaseDrag(Object owner);

    boolean isKeyDown(int keyCode);

    String typedChars();

    String clipboard();

    void setClipboard(String text);
}

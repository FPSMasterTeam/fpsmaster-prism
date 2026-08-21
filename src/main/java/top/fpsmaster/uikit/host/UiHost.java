package top.fpsmaster.uikit.host;

import top.fpsmaster.uikit.canvas.Canvas;
import top.fpsmaster.uikit.canvas.FontHandle;
import top.fpsmaster.uikit.canvas.ImageHandle;
import top.fpsmaster.uikit.input.Input;

/**
 * Per-client glue. Toolkit code talks only to this and {@link Canvas} / {@link Input}.
 */
public interface UiHost {
    Canvas canvas();

    Input input();

    FontHandle font(int size);

    /** Logical size of the UI surface in toolkit units. */
    float width();

    float height();

    long nowNanos();

    /**
     * Optional glass blur. Default is off; {@link top.fpsmaster.uikit.theme.Theme#glass()} already
     * falls back to the solid token.
     */
    boolean blurEnabled();

    void blurBehind(float x, float y, float w, float h, float radius);

    ImageHandle image(String id);
}

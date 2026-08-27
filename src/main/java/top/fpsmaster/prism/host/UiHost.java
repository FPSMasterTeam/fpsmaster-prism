package top.fpsmaster.prism.host;

import top.fpsmaster.prism.canvas.Canvas;
import top.fpsmaster.prism.canvas.FontHandle;
import top.fpsmaster.prism.canvas.ImageHandle;
import top.fpsmaster.prism.input.Input;

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
     * Optional glass blur. Default is off; {@link top.fpsmaster.prism.theme.Theme#glass()} already
     * falls back to the solid token.
     */
    boolean blurEnabled();

    void blurBehind(float x, float y, float w, float h, float radius);

    /** When false, {@link top.fpsmaster.prism.anim.Anim#approach} jumps to the target this frame. */
    default boolean animationsEnabled() {
        return true;
    }

    ImageHandle image(String id);

    /** Resolve a tinted icon/texture for a given on-screen size (hosts pick a resolution bucket). */
    default ImageHandle image(String id, float drawSize) {
        return image(id);
    }
}

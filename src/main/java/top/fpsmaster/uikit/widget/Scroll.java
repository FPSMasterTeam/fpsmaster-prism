package top.fpsmaster.uikit.widget;

import top.fpsmaster.uikit.theme.Metrics;

/**
 * Clip + wheel offset. Call {@link #begin} before children, {@link #end} after.
 */
public final class Scroll {
    private float offset;
    private final Object id;

    public Scroll(Object id) {
        this.id = id;
    }

    public float offset() {
        return offset;
    }

    public void setOffset(float offset) {
        this.offset = offset;
    }

    /**
     * @return current offset (0 = top, negative = scrolled down)
     */
    public float begin(UiFrame ui, float x, float y, float w, float viewH, float contentH) {
        int wheel = ui.input().consumeWheelDelta(x, y, w, viewH);
        if (wheel != 0) {
            offset += wheel > 0 ? 24f : -24f;
        }
        float min = Math.min(0f, viewH - contentH);
        if (offset > 0f) {
            offset = 0f;
        }
        if (offset < min) {
            offset = min;
        }
        ui.canvas().pushClip(x, y, w, viewH);
        if (contentH > viewH) {
            float trackH = Math.max(12f, viewH * viewH / contentH);
            float maxUp = contentH - viewH;
            float t = maxUp <= 0f ? 0f : -offset / maxUp;
            float trackY = y + t * (viewH - trackH);
            ui.canvas().fillRoundRect(x + w - 2f, trackY, 1.5f, trackH, 1f, ui.theme().layerActive());
        }
        return offset;
    }

    public void end(UiFrame ui) {
        ui.canvas().popClip();
        ui.input().markHovered(id, 0, 0, 0, 0);
    }

    public static float sidebar() {
        return Metrics.SIDEBAR;
    }
}

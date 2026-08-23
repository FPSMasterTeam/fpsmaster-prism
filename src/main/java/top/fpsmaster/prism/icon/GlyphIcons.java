package top.fpsmaster.prism.icon;

import top.fpsmaster.prism.canvas.Canvas;
import top.fpsmaster.prism.canvas.ImageHandle;
import top.fpsmaster.prism.widget.UiFrame;

/**
 * Shared-screen icons. Hosts supply PNG textures via {@code host.image(name, size)};
 * vector strokes are only a headless/test fallback.
 */
public final class GlyphIcons {
    private GlyphIcons() {
    }

    public static void draw(UiFrame ui, String name, float x, float y, float size, int argb) {
        ImageHandle image = ui.host().image(name, size);
        if (image != null) {
            ui.canvas().drawImage(image, x, y, size, size, argb);
            return;
        }
        drawFallback(ui.canvas(), name, x, y, size, argb);
    }

    static void drawFallback(Canvas canvas, String name, float x, float y, float size, int argb) {
        float s = size;
        float w = 1.15f;
        if ("box".equals(name)) {
            canvas.strokeRoundRect(x + s * 0.12f, y + s * 0.18f, s * 0.76f, s * 0.64f, 1.2f, w, argb);
        } else if ("globe".equals(name)) {
            canvas.fillCircle(x + s / 2f, y + s / 2f, s * 0.38f, argb);
            canvas.fillCircle(x + s / 2f, y + s / 2f, s * 0.22f, 0xFF0E0E0E);
        } else if ("sliders".equals(name)) {
            canvas.line(x + s * 0.15f, y + s * 0.32f, x + s * 0.85f, y + s * 0.32f, w, argb);
            canvas.line(x + s * 0.15f, y + s * 0.68f, x + s * 0.85f, y + s * 0.68f, w, argb);
            canvas.fillCircle(x + s * 0.38f, y + s * 0.32f, s * 0.12f, argb);
            canvas.fillCircle(x + s * 0.62f, y + s * 0.68f, s * 0.12f, argb);
        } else if ("replay".equals(name) || "play".equals(name)) {
            canvas.line(x + s * 0.28f, y + s * 0.18f, x + s * 0.28f, y + s * 0.82f, w, argb);
            canvas.line(x + s * 0.28f, y + s * 0.18f, x + s * 0.82f, y + s * 0.5f, w, argb);
            canvas.line(x + s * 0.28f, y + s * 0.82f, x + s * 0.82f, y + s * 0.5f, w, argb);
        } else if ("power".equals(name)) {
            canvas.line(x + s * 0.5f, y + s * 0.12f, x + s * 0.5f, y + s * 0.48f, w, argb);
            canvas.strokeRoundRect(x + s * 0.18f, y + s * 0.28f, s * 0.64f, s * 0.56f, s * 0.28f, w, argb);
        } else if ("image".equals(name)) {
            canvas.strokeRoundRect(x + s * 0.12f, y + s * 0.2f, s * 0.76f, s * 0.6f, 1.2f, w, argb);
            canvas.fillCircle(x + s * 0.32f, y + s * 0.38f, s * 0.08f, argb);
        } else if ("music".equals(name)) {
            canvas.line(x + s * 0.35f, y + s * 0.18f, x + s * 0.35f, y + s * 0.72f, w, argb);
            canvas.line(x + s * 0.35f, y + s * 0.18f, x + s * 0.78f, y + s * 0.3f, w, argb);
            canvas.fillCircle(x + s * 0.32f, y + s * 0.78f, s * 0.12f, argb);
        } else if ("lyrics".equals(name)) {
            canvas.line(x + s * 0.18f, y + s * 0.28f, x + s * 0.82f, y + s * 0.28f, w, argb);
            canvas.line(x + s * 0.18f, y + s * 0.5f, x + s * 0.68f, y + s * 0.5f, w, argb);
            canvas.line(x + s * 0.18f, y + s * 0.72f, x + s * 0.76f, y + s * 0.72f, w, argb);
        } else if ("folder".equals(name)) {
            canvas.strokeRoundRect(x + s * 0.12f, y + s * 0.32f, s * 0.76f, s * 0.5f, 1.2f, w, argb);
            canvas.fillRect(x + s * 0.12f, y + s * 0.22f, s * 0.32f, s * 0.14f, argb);
        } else if ("sun".equals(name) || "sparkles".equals(name)) {
            canvas.fillCircle(x + s / 2f, y + s / 2f, s * 0.18f, argb);
        } else if ("moon".equals(name)) {
            canvas.fillCircle(x + s * 0.52f, y + s * 0.48f, s * 0.28f, argb);
            canvas.fillCircle(x + s * 0.62f, y + s * 0.4f, s * 0.2f, 0xFF0E0E0E);
        } else if ("zap".equals(name)) {
            canvas.line(x + s * 0.55f, y + s * 0.12f, x + s * 0.28f, y + s * 0.52f, w, argb);
            canvas.line(x + s * 0.28f, y + s * 0.52f, x + s * 0.62f, y + s * 0.52f, w, argb);
            canvas.line(x + s * 0.62f, y + s * 0.52f, x + s * 0.38f, y + s * 0.88f, w, argb);
        } else if ("wrench".equals(name)) {
            canvas.strokeRoundRect(x + s * 0.28f, y + s * 0.18f, s * 0.44f, s * 0.64f, 1.2f, w, argb);
        } else if ("grid".equals(name)) {
            canvas.strokeRoundRect(x + s * 0.18f, y + s * 0.18f, s * 0.28f, s * 0.28f, 1f, w, argb);
            canvas.strokeRoundRect(x + s * 0.54f, y + s * 0.18f, s * 0.28f, s * 0.28f, 1f, w, argb);
            canvas.strokeRoundRect(x + s * 0.18f, y + s * 0.54f, s * 0.28f, s * 0.28f, 1f, w, argb);
            canvas.strokeRoundRect(x + s * 0.54f, y + s * 0.54f, s * 0.28f, s * 0.28f, 1f, w, argb);
        } else if ("search".equals(name)) {
            canvas.strokeRoundRect(x + s * 0.18f, y + s * 0.18f, s * 0.46f, s * 0.46f, s * 0.23f, w, argb);
            canvas.line(x + s * 0.55f, y + s * 0.55f, x + s * 0.82f, y + s * 0.82f, w, argb);
        } else if ("chev-d".equals(name)) {
            canvas.line(x + s * 0.18f, y + s * 0.32f, x + s * 0.5f, y + s * 0.7f, w, argb);
            canvas.line(x + s * 0.5f, y + s * 0.7f, x + s * 0.82f, y + s * 0.32f, w, argb);
        } else if ("chev-r".equals(name)) {
            canvas.line(x + s * 0.32f, y + s * 0.18f, x + s * 0.7f, y + s * 0.5f, w, argb);
            canvas.line(x + s * 0.7f, y + s * 0.5f, x + s * 0.32f, y + s * 0.82f, w, argb);
        } else {
            canvas.fillRect(x + s * 0.2f, y + s * 0.2f, s * 0.6f, s * 0.6f, argb);
        }
    }
}

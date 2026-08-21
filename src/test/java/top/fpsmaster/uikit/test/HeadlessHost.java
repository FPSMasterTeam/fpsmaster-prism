package top.fpsmaster.uikit.test;

import top.fpsmaster.uikit.canvas.Canvas;
import top.fpsmaster.uikit.canvas.FontHandle;
import top.fpsmaster.uikit.canvas.ImageHandle;
import top.fpsmaster.uikit.host.UiHost;
import top.fpsmaster.uikit.input.FrameInput;
import top.fpsmaster.uikit.input.Input;

public final class HeadlessHost implements UiHost {
    public final RecordingCanvas canvas = new RecordingCanvas();
    public final FrameInput input = new FrameInput();
    private final float width;
    private final float height;

    public HeadlessHost(float width, float height) {
        this.width = width;
        this.height = height;
    }

    public Canvas canvas() {
        return canvas;
    }

    public Input input() {
        return input;
    }

    public FontHandle font(int size) {
        return new RecordingFont(size);
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public long nowNanos() {
        return 0L;
    }

    public boolean blurEnabled() {
        return false;
    }

    public void blurBehind(float x, float y, float w, float h, float radius) {
    }

    public ImageHandle image(String id) {
        return null;
    }
}

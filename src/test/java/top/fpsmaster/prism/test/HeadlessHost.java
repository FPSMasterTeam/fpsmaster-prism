package top.fpsmaster.prism.test;

import top.fpsmaster.prism.canvas.Canvas;
import top.fpsmaster.prism.canvas.FontHandle;
import top.fpsmaster.prism.canvas.ImageHandle;
import top.fpsmaster.prism.host.UiHost;
import top.fpsmaster.prism.input.FrameInput;
import top.fpsmaster.prism.input.Input;

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

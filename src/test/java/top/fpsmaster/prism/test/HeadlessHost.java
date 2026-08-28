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
    /** Frozen and settable: anything that ages by wall clock is only testable if the test drives it. */
    private long nowNanos;

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
        return nowNanos;
    }

    /** Moves the clock forward by {@code millis} milliseconds. */
    public void advanceMillis(long millis) {
        nowNanos += millis * 1_000_000L;
    }

    public void setNowNanos(long value) {
        nowNanos = value;
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

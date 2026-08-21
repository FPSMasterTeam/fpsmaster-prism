package top.fpsmaster.prism.test;

import top.fpsmaster.prism.canvas.FontHandle;

public final class RecordingFont implements FontHandle {
    private final int size;

    public RecordingFont(int size) {
        this.size = size;
    }

    public int size() {
        return size;
    }

    public float measure(String text) {
        if (text == null) {
            return 0f;
        }
        return text.length() * size * 0.55f;
    }

    public float lineHeight() {
        return size * 0.5f + 2f;
    }
}

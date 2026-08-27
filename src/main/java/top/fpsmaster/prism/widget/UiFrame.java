package top.fpsmaster.prism.widget;

import top.fpsmaster.prism.anim.Anim;
import top.fpsmaster.prism.canvas.Canvas;
import top.fpsmaster.prism.canvas.FontHandle;
import top.fpsmaster.prism.geom.Hit;
import top.fpsmaster.prism.host.UiHost;
import top.fpsmaster.prism.input.Input;
import top.fpsmaster.prism.theme.Theme;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * One paint + hit-test pass. Hosts construct this at the start of a screen/HUD frame.
 */
public final class UiFrame {
    private final UiHost host;
    private final Theme theme;

    /**
     * Logical clip stack, kept so hit-testing matches what is actually painted. Push through
     * {@link #pushClip} rather than {@code canvas().pushClip} whenever anything inside the clip is
     * hit-tested: a row scrolled out of a viewport, or a setting clipped away below a panel, must not
     * consume a press or light up on hover just because its untransformed rectangle contains the mouse.
     * Raw {@code canvas().pushClip} stays available for draw-only clipping.
     */
    private final Deque<float[]> clips = new ArrayDeque<float[]>();

    public UiFrame(UiHost host, Theme theme) {
        this.host = host;
        this.theme = theme;
        Anim.setEnabled(host.animationsEnabled());
    }

    public UiHost host() {
        return host;
    }

    public Canvas canvas() {
        return host.canvas();
    }

    public Input input() {
        return host.input();
    }

    public Theme theme() {
        return theme;
    }

    public FontHandle font(int size) {
        return host.font(size);
    }

    /** Clip drawing and hit-testing to {@code rect} intersected with the enclosing clip. */
    public void pushClip(float x, float y, float w, float h) {
        float[] parent = clips.peek();
        float[] next = parent == null
                ? new float[]{x, y, w, h}
                : Hit.intersect(parent[0], parent[1], parent[2], parent[3], x, y, w, h);
        clips.push(next);
        canvas().pushClip(next[0], next[1], next[2], next[3]);
    }

    public void popClip() {
        if (!clips.isEmpty()) {
            clips.pop();
        }
        canvas().popClip();
    }

    public boolean hovered(float x, float y, float w, float h) {
        float[] r = clipped(x, y, w, h);
        return r != null && Hit.inside(r[0], r[1], r[2], r[3], input().mouseX(), input().mouseY());
    }

    public boolean clicked(float x, float y, float w, float h) {
        float[] r = clipped(x, y, w, h);
        return r != null && input().consumePressInBounds(r[0], r[1], r[2], r[3], 0) != null;
    }

    /** {@code rect} clipped to the current clip, or {@code null} when nothing of it is visible. */
    private float[] clipped(float x, float y, float w, float h) {
        float[] clip = clips.peek();
        if (clip == null) {
            return new float[]{x, y, w, h};
        }
        float[] r = Hit.intersect(clip[0], clip[1], clip[2], clip[3], x, y, w, h);
        return r[2] <= 0f || r[3] <= 0f ? null : r;
    }
}

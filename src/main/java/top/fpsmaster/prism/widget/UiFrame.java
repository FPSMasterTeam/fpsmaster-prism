package top.fpsmaster.prism.widget;

import top.fpsmaster.prism.anim.Anim;
import top.fpsmaster.prism.canvas.Canvas;
import top.fpsmaster.prism.canvas.FontHandle;
import top.fpsmaster.prism.geom.Hit;
import top.fpsmaster.prism.host.UiHost;
import top.fpsmaster.prism.input.Input;
import top.fpsmaster.prism.theme.Theme;

/**
 * One paint + hit-test pass. Hosts construct this at the start of a screen/HUD frame.
 *
 * <p>Hit-testing follows what is painted: {@link #hovered}/{@link #clicked} map the widget rectangle
 * through the current {@link #translate}/{@link #scale} transform and clip it against the
 * {@link #pushClip} stack before comparing with the pointer, which hosts report in untransformed
 * surface units. Use these methods instead of {@code canvas().translate/scale/pushClip} whenever
 * anything inside is hit-tested.
 */
public final class UiFrame {
    private final UiHost host;
    private final Theme theme;

    /**
     * Logical clip stack in surface (untransformed) units, 4 floats per level, kept so hit-testing
     * matches what is actually painted: a row scrolled out of a viewport, or a setting clipped away
     * below a panel, must not consume a press or light up on hover just because its rectangle
     * contains the mouse. Raw {@code canvas().pushClip} stays available for draw-only clipping.
     */
    private float[] clips = new float[4 * 8];
    private int clipDepth;

    /** Current transform: {@code surface = local * scale + (tx, ty)}. Saved 3 floats per level. */
    private float tx;
    private float ty;
    private float ts = 1f;
    private float[] xforms = new float[3 * 4];
    private int xformDepth;

    private boolean inputEnabled = true;

    /** Scratch result of {@link #visible}; avoids allocating per hit test. */
    private float rx;
    private float ry;
    private float rw;
    private float rh;

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

    // ---- transform ----

    /** Save the transform on both the canvas and the hit-test side. Pair with {@link #popTransform()}. */
    public void pushTransform() {
        if (xformDepth * 3 + 3 > xforms.length) {
            float[] grown = new float[xforms.length * 2];
            System.arraycopy(xforms, 0, grown, 0, xforms.length);
            xforms = grown;
        }
        int i = xformDepth * 3;
        xforms[i] = tx;
        xforms[i + 1] = ty;
        xforms[i + 2] = ts;
        xformDepth++;
        canvas().pushTransform();
    }

    public void popTransform() {
        if (xformDepth > 0) {
            xformDepth--;
            int i = xformDepth * 3;
            tx = xforms[i];
            ty = xforms[i + 1];
            ts = xforms[i + 2];
        }
        canvas().popTransform();
    }

    public void translate(float x, float y) {
        tx += x * ts;
        ty += y * ts;
        canvas().translate(x, y);
    }

    /** Uniform scale about the current origin. */
    public void scale(float s) {
        ts *= s;
        canvas().scale(s);
    }

    /** Pointer X in the current local (transformed) coordinate space. */
    public float localMouseX() {
        return (input().mouseX() - tx) / ts;
    }

    public float localMouseY() {
        return (input().mouseY() - ty) / ts;
    }

    // ---- clip ----

    /** Clip drawing and hit-testing to {@code rect} intersected with the enclosing clip. */
    public void pushClip(float x, float y, float w, float h) {
        float sx = x * ts + tx;
        float sy = y * ts + ty;
        float sw = w * ts;
        float sh = h * ts;
        if (clipDepth > 0) {
            int p = (clipDepth - 1) * 4;
            float left = Math.max(clips[p], sx);
            float top = Math.max(clips[p + 1], sy);
            float right = Math.min(clips[p] + Math.max(0f, clips[p + 2]), sx + Math.max(0f, sw));
            float bottom = Math.min(clips[p + 1] + Math.max(0f, clips[p + 3]), sy + Math.max(0f, sh));
            sx = left;
            sy = top;
            sw = Math.max(0f, right - left);
            sh = Math.max(0f, bottom - top);
        }
        if (clipDepth * 4 + 4 > clips.length) {
            float[] grown = new float[clips.length * 2];
            System.arraycopy(clips, 0, grown, 0, clips.length);
            clips = grown;
        }
        int i = clipDepth * 4;
        clips[i] = sx;
        clips[i + 1] = sy;
        clips[i + 2] = sw;
        clips[i + 3] = sh;
        clipDepth++;
        // The canvas receives the same rectangle expressed in the current local space.
        canvas().pushClip((sx - tx) / ts, (sy - ty) / ts, sw / ts, sh / ts);
    }

    public void popClip() {
        if (clipDepth > 0) {
            clipDepth--;
        }
        canvas().popClip();
    }

    // ---- hit testing ----

    /**
     * While disabled, every hit-test helper here reports "nothing" without touching the input
     * queue, and {@link TextBox} ignores typing. Screens disable input for what is painted under
     * a modal dialog (so its buttons cannot take a press meant for the dialog) and while they
     * animate out (so a click during the fade cannot toggle anything).
     */
    public void setInputEnabled(boolean enabled) {
        this.inputEnabled = enabled;
    }

    public boolean inputEnabled() {
        return inputEnabled;
    }

    public boolean hovered(float x, float y, float w, float h) {
        return inputEnabled && visible(x, y, w, h) && Hit.inside(rx, ry, rw, rh, input().mouseX(), input().mouseY());
    }

    public boolean clicked(float x, float y, float w, float h) {
        return inputEnabled && visible(x, y, w, h) && input().consumePressInBounds(rx, ry, rw, rh, 0) != null;
    }

    /** {@link Input#hasPressOutside} for a local rectangle. */
    public boolean hasPressOutside(float x, float y, float w, float h) {
        return inputEnabled && input().hasPressOutside(x * ts + tx, y * ts + ty, w * ts, h * ts);
    }

    /** {@link Input#beginDrag} for a local rectangle (clipped like {@link #clicked}). */
    public boolean beginDrag(Object owner, float x, float y, float w, float h) {
        if (!inputEnabled) {
            return false;
        }
        if (input().isDragging(owner)) {
            return true;
        }
        return visible(x, y, w, h) && input().beginDrag(owner, 0, rx, ry, rw, rh);
    }

    /** {@link Input#consumeWheelDelta} for a local rectangle. */
    public int consumeWheel(float x, float y, float w, float h) {
        if (!inputEnabled) {
            return 0;
        }
        return input().consumeWheelDelta(x * ts + tx, y * ts + ty, w * ts, h * ts);
    }

    /** {@link Input#markHovered} for a local rectangle. */
    public void markHovered(Object id, float x, float y, float w, float h) {
        if (!inputEnabled) {
            return;
        }
        input().markHovered(id, x * ts + tx, y * ts + ty, w * ts, h * ts);
    }

    /**
     * Writes the surface-space part of {@code rect} that survives the current clip into
     * {@link #rx}..{@link #rh}; {@code false} when nothing of it is visible.
     */
    private boolean visible(float x, float y, float w, float h) {
        float sx = x * ts + tx;
        float sy = y * ts + ty;
        float sw = w * ts;
        float sh = h * ts;
        if (clipDepth == 0) {
            rx = sx;
            ry = sy;
            rw = sw;
            rh = sh;
            return true;
        }
        int p = (clipDepth - 1) * 4;
        float left = Math.max(clips[p], sx);
        float top = Math.max(clips[p + 1], sy);
        float right = Math.min(clips[p] + Math.max(0f, clips[p + 2]), sx + Math.max(0f, sw));
        float bottom = Math.min(clips[p + 1] + Math.max(0f, clips[p + 3]), sy + Math.max(0f, sh));
        rx = left;
        ry = top;
        rw = right - left;
        rh = bottom - top;
        return rw > 0f && rh > 0f;
    }
}

package top.fpsmaster.prism.test;

import top.fpsmaster.prism.canvas.Canvas;
import top.fpsmaster.prism.canvas.FontHandle;
import top.fpsmaster.prism.canvas.ImageHandle;

import java.util.ArrayList;
import java.util.List;

/** Records primitive calls so tests can assert without a GPU. */
public final class RecordingCanvas implements Canvas {
    public final List<String> ops = new ArrayList<String>();
    private int clipDepth;
    private int alphaDepth;

    public void fillRect(float x, float y, float w, float h, int argb) {
        ops.add("fillRect");
    }

    public void fillRoundRect(float x, float y, float w, float h, float radius, int argb) {
        ops.add("fillRoundRect");
    }

    public void strokeRoundRect(float x, float y, float w, float h, float radius, float strokeWidth, int argb) {
        ops.add("strokeRoundRect");
    }

    public void fillCircle(float cx, float cy, float radius, int argb) {
        ops.add("fillCircle");
    }

    public void line(float x1, float y1, float x2, float y2, float width, int argb) {
        ops.add("line");
    }

    public void fillGradientH(float x, float y, float w, float h, int argbLeft, int argbRight) {
        ops.add("fillGradientH");
    }

    public void fillGradientV(float x, float y, float w, float h, int argbTop, int argbBottom) {
        ops.add("fillGradientV");
    }

    public void drawString(FontHandle font, String text, float x, float y, int argb) {
        ops.add("drawString:" + text);
    }

    public void drawImage(ImageHandle image, float x, float y, float w, float h, int tintArgb) {
        ops.add("drawImage");
    }

    public void pushClip(float x, float y, float w, float h) {
        clipDepth++;
        ops.add("pushClip");
    }

    public void popClip() {
        if (clipDepth <= 0) {
            throw new IllegalStateException("popClip without pushClip");
        }
        clipDepth--;
        ops.add("popClip");
    }

    public void pushAlpha(float alpha) {
        alphaDepth++;
        ops.add("pushAlpha");
    }

    public void popAlpha() {
        if (alphaDepth <= 0) {
            throw new IllegalStateException("popAlpha without pushAlpha");
        }
        alphaDepth--;
        ops.add("popAlpha");
    }

    public void pushTransform() {
        ops.add("pushTransform");
    }

    public void popTransform() {
        ops.add("popTransform");
    }

    public void translate(float x, float y) {
        ops.add("translate");
    }

    public void scale(float s) {
        ops.add("scale");
    }

    public int clipDepth() {
        return clipDepth;
    }

    public boolean has(String op) {
        return ops.contains(op);
    }
}

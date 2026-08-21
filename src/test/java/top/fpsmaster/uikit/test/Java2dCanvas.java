package top.fpsmaster.uikit.test;

import top.fpsmaster.uikit.canvas.Canvas;
import top.fpsmaster.uikit.canvas.FontHandle;
import top.fpsmaster.uikit.canvas.ImageHandle;
import top.fpsmaster.uikit.theme.Argb;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;

/** Toolkit unit = 1 pixel. Used to prove the SPI can drive a real 2D backend. */
public final class Java2dCanvas implements Canvas {
    public final BufferedImage image;
    private final Graphics2D g;
    private final Deque<Float> alpha = new ArrayDeque<Float>();

    public Java2dCanvas(int width, int height) {
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setComposite(AlphaComposite.SrcOver);
        alpha.push(Float.valueOf(1f));
    }

    public void fillRect(float x, float y, float w, float h, int argb) {
        g.setColor(color(argb));
        g.fillRect(Math.round(x), Math.round(y), Math.round(w), Math.round(h));
    }

    public void fillRoundRect(float x, float y, float w, float h, float radius, int argb) {
        g.setColor(color(argb));
        float d = radius * 2f;
        g.fill(new RoundRectangle2D.Float(x, y, w, h, d, d));
    }

    public void strokeRoundRect(float x, float y, float w, float h, float radius, float strokeWidth, int argb) {
        g.setColor(color(argb));
        g.setStroke(new BasicStroke(strokeWidth));
        float d = radius * 2f;
        g.draw(new RoundRectangle2D.Float(x, y, w, h, d, d));
    }

    public void fillCircle(float cx, float cy, float radius, int argb) {
        g.setColor(color(argb));
        g.fill(new Ellipse2D.Float(cx - radius, cy - radius, radius * 2f, radius * 2f));
    }

    public void line(float x1, float y1, float x2, float y2, float width, int argb) {
        g.setColor(color(argb));
        g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Float(x1, y1, x2, y2));
    }

    public void fillGradientH(float x, float y, float w, float h, int argbLeft, int argbRight) {
        java.awt.GradientPaint paint = new java.awt.GradientPaint(
                x, y, color(argbLeft), x + w, y, color(argbRight));
        g.setPaint(paint);
        g.fillRect(Math.round(x), Math.round(y), Math.round(w), Math.round(h));
    }

    public void fillGradientV(float x, float y, float w, float h, int argbTop, int argbBottom) {
        java.awt.GradientPaint paint = new java.awt.GradientPaint(
                x, y, color(argbTop), x, y + h, color(argbBottom));
        g.setPaint(paint);
        g.fillRect(Math.round(x), Math.round(y), Math.round(w), Math.round(h));
    }

    public void drawString(FontHandle font, String text, float x, float y, int argb) {
        Font awt = new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(8, font.size()));
        g.setFont(awt);
        g.setColor(color(argb));
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, x, y + metrics.getAscent());
    }

    public void drawImage(ImageHandle imageHandle, float x, float y, float w, float h, int tintArgb) {
        if (!(imageHandle instanceof Java2dImage)) {
            return;
        }
        g.drawImage(((Java2dImage) imageHandle).buffer, Math.round(x), Math.round(y),
                Math.round(w), Math.round(h), null);
    }

    public void pushClip(float x, float y, float w, float h) {
        g.setClip(Math.round(x), Math.round(y), Math.round(w), Math.round(h));
    }

    public void popClip() {
        g.setClip(null);
    }

    public void pushAlpha(float a) {
        float next = alpha.peek().floatValue() * a;
        alpha.push(Float.valueOf(next));
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, next));
    }

    public void popAlpha() {
        if (alpha.size() > 1) {
            alpha.pop();
        }
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha.peek().floatValue()));
    }

    public void dispose() {
        g.dispose();
    }

    private Color color(int argb) {
        float factor = alpha.peek().floatValue();
        int a = Math.max(0, Math.min(255, Math.round(Argb.alpha(argb) * factor)));
        return new Color(Argb.red(argb), Argb.green(argb), Argb.blue(argb), a);
    }

    public static final class Java2dImage implements ImageHandle {
        final BufferedImage buffer;

        public Java2dImage(BufferedImage buffer) {
            this.buffer = buffer;
        }

        public int width() {
            return buffer.getWidth();
        }

        public int height() {
            return buffer.getHeight();
        }
    }

    public static final class Java2dFont implements FontHandle {
        private final int size;

        public Java2dFont(int size) {
            this.size = size;
        }

        public int size() {
            return size;
        }

        public float measure(String text) {
            if (text == null) {
                return 0f;
            }
            return text.length() * size * 0.5f;
        }

        public float lineHeight() {
            return size * 0.9f;
        }
    }
}

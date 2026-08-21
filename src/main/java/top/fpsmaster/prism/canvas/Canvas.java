package top.fpsmaster.prism.canvas;

/**
 * Immediate-mode 2D backend. Coordinates are toolkit units (Edge GUI units). Implementations
 * live in the client (GL11, {@code GuiGraphics}, Java2D for tests). The toolkit never imports
 * Minecraft.
 */
public interface Canvas {
    void fillRect(float x, float y, float w, float h, int argb);

    void fillRoundRect(float x, float y, float w, float h, float radius, int argb);

    void strokeRoundRect(float x, float y, float w, float h, float radius, float strokeWidth, int argb);

    void fillCircle(float cx, float cy, float radius, int argb);

    void line(float x1, float y1, float x2, float y2, float width, int argb);

    void fillGradientH(float x, float y, float w, float h, int argbLeft, int argbRight);

    void fillGradientV(float x, float y, float w, float h, int argbTop, int argbBottom);

    void drawString(FontHandle font, String text, float x, float y, int argb);

    void drawImage(ImageHandle image, float x, float y, float w, float h, int tintArgb);

    void pushClip(float x, float y, float w, float h);

    void popClip();

    void pushAlpha(float alpha);

    void popAlpha();

    /** Save the current 2D transform (translate/scale). Must pair with {@link #popTransform()}. */
    void pushTransform();

    void popTransform();

    void translate(float x, float y);

    /** Uniform 2D scale about the current origin. */
    void scale(float s);
}

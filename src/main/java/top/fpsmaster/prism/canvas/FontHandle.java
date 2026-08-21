package top.fpsmaster.prism.canvas;

/**
 * A sized typeface. Drawing goes through {@link Canvas#drawString}; this handle only identifies
 * the face and reports metrics.
 *
 * <p>{@code y} for {@link Canvas#drawString} is the <em>top</em> of the text box, matching Edge
 * GUI units — not a typographic baseline.
 */
public interface FontHandle {
    int size();

    float measure(String text);

    float lineHeight();
}

package top.fpsmaster.prism.canvas;

/** An uploaded 2D texture. Hosts mint these; the toolkit only passes them to {@link Canvas}. */
public interface ImageHandle {
    int width();

    int height();
}

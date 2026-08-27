package top.fpsmaster.prism.geom;

public final class Hit {
    private Hit() {
    }

    public static boolean inside(float x, float y, float w, float h, float px, float py) {
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }

    /**
     * Intersection of two rectangles as {@code {x, y, w, h}}. Width and height are {@code 0} when the
     * rectangles do not overlap, so callers can test with {@code w <= 0 || h <= 0}.
     */
    public static float[] intersect(float ax, float ay, float aw, float ah,
                                    float bx, float by, float bw, float bh) {
        float aRight = ax + Math.max(0f, aw);
        float aBottom = ay + Math.max(0f, ah);
        float bRight = bx + Math.max(0f, bw);
        float bBottom = by + Math.max(0f, bh);
        float x = Math.max(ax, bx);
        float y = Math.max(ay, by);
        return new float[]{
                x,
                y,
                Math.max(0f, Math.min(aRight, bRight) - x),
                Math.max(0f, Math.min(aBottom, bBottom) - y)
        };
    }
}

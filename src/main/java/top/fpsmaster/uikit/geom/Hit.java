package top.fpsmaster.uikit.geom;

public final class Hit {
    private Hit() {
    }

    public static boolean inside(float x, float y, float w, float h, float px, float py) {
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }
}

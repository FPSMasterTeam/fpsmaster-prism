package top.fpsmaster.uikit.theme;

public final class Argb {
    private Argb() {
    }

    public static int of(int a, int r, int g, int b) {
        return ((a & 255) << 24) | ((r & 255) << 16) | ((g & 255) << 8) | (b & 255);
    }

    public static int rgb(int r, int g, int b) {
        return of(255, r, g, b);
    }

    public static int alpha(int argb) {
        return (argb >>> 24) & 255;
    }

    public static int red(int argb) {
        return (argb >>> 16) & 255;
    }

    public static int green(int argb) {
        return (argb >>> 8) & 255;
    }

    public static int blue(int argb) {
        return argb & 255;
    }

    public static int withAlpha(int argb, int a) {
        return (argb & 0x00FFFFFF) | ((a & 255) << 24);
    }

    /** {@code t} in 0..1. */
    public static int lerp(int a, int b, float t) {
        if (t <= 0f) {
            return a;
        }
        if (t >= 1f) {
            return b;
        }
        int ia = (int) (t * 255f);
        int ib = 255 - ia;
        return of(
                (alpha(a) * ib + alpha(b) * ia) / 255,
                (red(a) * ib + red(b) * ia) / 255,
                (green(a) * ib + green(b) * ia) / 255,
                (blue(a) * ib + blue(b) * ia) / 255);
    }

    public static int mulAlpha(int argb, float factor) {
        int a = Math.max(0, Math.min(255, Math.round(alpha(argb) * factor)));
        return withAlpha(argb, a);
    }
}

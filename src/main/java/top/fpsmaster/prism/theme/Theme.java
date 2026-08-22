package top.fpsmaster.prism.theme;

/**
 * Shared semantic tokens for every native FPSMaster surface.
 */
public final class Theme {
    public static final Theme DARK = new Theme(false, false);
    public static final Theme DARK_BLUR = new Theme(false, true);
    public static final Theme LIGHT = new Theme(true, false);
    public static final Theme LIGHT_BLUR = new Theme(true, true);

    public final boolean light;
    public final boolean blur;

    public Theme(boolean light, boolean blur) {
        this.light = light;
        this.blur = blur;
    }

    public static Theme of(boolean light, boolean blur) {
        if (light) {
            return blur ? LIGHT_BLUR : LIGHT;
        }
        return blur ? DARK_BLUR : DARK;
    }

    public int textPrimary() {
        return light ? Argb.rgb(30, 30, 30) : Argb.rgb(242, 242, 242);
    }

    public int textSecondary() {
        return light ? Argb.rgb(100, 100, 100) : Argb.rgb(154, 154, 154);
    }

    public int textDisabled() {
        return light ? Argb.rgb(140, 140, 140) : Argb.rgb(92, 92, 92);
    }

    /** {@code --glass} when blur is on, {@code --glass-solid} otherwise. */
    public int glass() {
        if (light) {
            return Argb.of(235, 246, 246, 246);
        }
        return blur ? Argb.of(209, 18, 18, 18) : Argb.of(240, 14, 14, 14);
    }

    public int veil() {
        return Argb.of(148, 0, 0, 0);
    }

    public int stroke() {
        return light ? Argb.of(26, 0, 0, 0) : Argb.of(20, 255, 255, 255);
    }

    public int strokeStrong() {
        return light ? Argb.of(48, 0, 0, 0) : Argb.of(41, 255, 255, 255);
    }

    public int layer() {
        return light ? Argb.of(10, 0, 0, 0) : Argb.of(11, 255, 255, 255);
    }

    public int layerHover() {
        return light ? Argb.of(16, 0, 0, 0) : Argb.of(20, 255, 255, 255);
    }

    public int layerActive() {
        return light ? Argb.of(22, 0, 0, 0) : Argb.of(31, 255, 255, 255);
    }

    public int divider() {
        return stroke();
    }

    public int card() {
        return layer();
    }

    public int cardHover() {
        return layerHover();
    }

    public int cardExpanded() {
        return layerActive();
    }

    public int accent() {
        return Argb.rgb(46, 174, 222);
    }

    public int accentHover() {
        return Argb.rgb(68, 190, 232);
    }

    public int accentSoft() {
        return Argb.of(42, 46, 174, 222);
    }

    public int accentText() {
        return light ? Argb.rgb(12, 112, 151) : Argb.rgb(126, 218, 246);
    }

    public int accentBorder() {
        return Argb.of(105, 46, 174, 222);
    }

    public int danger() {
        return Argb.rgb(240, 80, 110);
    }

    public int dangerSoft() {
        return Argb.of(40, 240, 80, 110);
    }

    public int ok() {
        return Argb.rgb(62, 207, 142);
    }

    public int warning() {
        return Argb.rgb(232, 178, 62);
    }

    public int input() {
        return light ? Argb.rgb(250, 250, 250) : Argb.rgb(17, 20, 23);
    }

    public int inputHover() {
        return light ? Argb.rgb(244, 248, 250) : Argb.rgb(21, 26, 30);
    }

    public int grid() {
        return light ? Argb.of(22, 20, 80, 100) : Argb.of(25, 180, 220, 235);
    }

    public int toggleOff() {
        return layerActive();
    }

    public int white() {
        return Argb.rgb(255, 255, 255);
    }

    public int opaquePanelBase() {
        return light ? Argb.rgb(246, 246, 246) : Argb.rgb(14, 14, 14);
    }
}

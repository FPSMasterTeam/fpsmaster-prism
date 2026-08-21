package top.fpsmaster.prism.widget;

import top.fpsmaster.prism.canvas.Canvas;
import top.fpsmaster.prism.canvas.FontHandle;
import top.fpsmaster.prism.geom.Hit;
import top.fpsmaster.prism.host.UiHost;
import top.fpsmaster.prism.input.Input;
import top.fpsmaster.prism.theme.Theme;

/**
 * One paint + hit-test pass. Hosts construct this at the start of a screen/HUD frame.
 */
public final class UiFrame {
    private final UiHost host;
    private final Theme theme;

    public UiFrame(UiHost host, Theme theme) {
        this.host = host;
        this.theme = theme;
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

    public boolean hovered(float x, float y, float w, float h) {
        return Hit.inside(x, y, w, h, input().mouseX(), input().mouseY());
    }

    public boolean clicked(float x, float y, float w, float h) {
        return input().consumePressInBounds(x, y, w, h, 0) != null;
    }
}

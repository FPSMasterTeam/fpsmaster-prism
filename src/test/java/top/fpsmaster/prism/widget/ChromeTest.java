package top.fpsmaster.prism.widget;

import org.junit.jupiter.api.Test;
import top.fpsmaster.prism.input.PointerEvent;
import top.fpsmaster.prism.test.HeadlessHost;
import top.fpsmaster.prism.theme.Metrics;
import top.fpsmaster.prism.theme.Theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChromeTest {
    @Test
    void laterWidgetConsumesTheClick() {
        HeadlessHost host = new HeadlessHost(400, 240);
        host.input.press(0, 50, 20);
        UiFrame ui = new UiFrame(host, Theme.DARK);
        assertFalse(ui.clicked(0, 0, 10, 10));
        assertTrue(ui.clicked(40, 10, 40, 20));
        assertFalse(ui.clicked(40, 10, 40, 20));
    }

    @Test
    void buttonReturnsTrueOnPress() {
        HeadlessHost host = new HeadlessHost(400, 240);
        host.input.setMouse(24, 12);
        host.input.press(0, 24, 12);
        UiFrame ui = new UiFrame(host, Theme.DARK);
        assertTrue(Chrome.button(ui, 8, 8, 40, Metrics.BTN_H, "OK", Chrome.ButtonStyle.PRIMARY));
        assertTrue(host.canvas.has("fillRoundRect"));
        assertTrue(host.canvas.has("drawString:OK"));
    }

    @Test
    void sliderDragMapsMouseX() {
        HeadlessHost host = new HeadlessHost(400, 240);
        host.input.setMouse(10, 8);
        host.input.press(0, 10, 8);
        UiFrame ui = new UiFrame(host, Theme.DARK);
        Object id = "slider";
        float t = Chrome.slider(ui, id, 0, 0, 100, 0.1f);
        assertEquals(0.10f, t, 0.001f);
        host.input.setMouse(75, 8);
        t = Chrome.slider(ui, id, 0, 0, 100, t);
        assertEquals(0.75f, t, 0.001f);
    }

    @Test
    void toggleFlipsOnClick() {
        HeadlessHost host = new HeadlessHost(400, 240);
        host.input.press(0, 12, 6);
        UiFrame ui = new UiFrame(host, Theme.DARK);
        assertTrue(Chrome.toggle(ui, 0, 0, false));
    }

    @Test
    void clipStackBalances() {
        HeadlessHost host = new HeadlessHost(400, 240);
        host.canvas.pushClip(0, 0, 100, 80);
        Chrome.panel(new UiFrame(host, Theme.DARK), 4, 4, 80, 40);
        host.canvas.popClip();
        assertEquals(0, host.canvas.clipDepth());
        assertTrue(host.canvas.has("pushClip"));
        assertTrue(host.canvas.has("popClip"));
        assertTrue(host.canvas.has("fillRoundRect"));
    }

    @Test
    void consumePressOutsideDismisses() {
        HeadlessHost host = new HeadlessHost(400, 240);
        host.input.press(0, 200, 10);
        PointerEvent event = host.input.consumePressOutside(0, 0, 80, 40);
        assertEquals(200, event.x);
        assertEquals(null, host.input.consumePressInBounds(0, 0, 400, 240, 0));
    }
}

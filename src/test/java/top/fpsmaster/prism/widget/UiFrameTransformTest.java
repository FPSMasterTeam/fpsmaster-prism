package top.fpsmaster.prism.widget;

import org.junit.jupiter.api.Test;
import top.fpsmaster.prism.test.HeadlessHost;
import top.fpsmaster.prism.theme.Theme;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiFrameTransformTest {
    @Test
    void hitTestFollowsTranslateAndScale() {
        HeadlessHost host = new HeadlessHost(400, 300);
        UiFrame ui = new UiFrame(host, Theme.DARK);
        ui.pushTransform();
        ui.translate(100f, 50f);
        ui.scale(0.5f);
        // Local (20,20,40,20) is painted at surface (110,60,20,10).
        host.input.setMouse(30, 30);
        assertFalse(ui.hovered(20f, 20f, 40f, 20f));
        host.input.setMouse(115, 65);
        assertTrue(ui.hovered(20f, 20f, 40f, 20f));
        host.input.press(0, 125, 68);
        assertTrue(ui.clicked(20f, 20f, 40f, 20f));
        ui.popTransform();
        host.input.setMouse(30, 30);
        assertTrue(ui.hovered(20f, 20f, 40f, 20f));
    }

    @Test
    void clipIsAppliedInSurfaceSpace() {
        HeadlessHost host = new HeadlessHost(400, 300);
        UiFrame ui = new UiFrame(host, Theme.DARK);
        ui.pushTransform();
        ui.translate(0f, 100f);
        ui.pushClip(0f, 0f, 100f, 20f); // surface y 100..120
        host.input.setMouse(10, 130);
        assertFalse(ui.hovered(0f, 0f, 100f, 50f));
        host.input.setMouse(10, 110);
        assertTrue(ui.hovered(0f, 0f, 100f, 50f));
        ui.popClip();
        ui.popTransform();
    }

    @Test
    void disabledInputReportsNothingAndLeavesPressQueued() {
        HeadlessHost host = new HeadlessHost(400, 300);
        UiFrame ui = new UiFrame(host, Theme.DARK);
        host.input.press(0, 10, 10);
        ui.setInputEnabled(false);
        assertFalse(ui.hovered(0f, 0f, 50f, 50f));
        assertFalse(ui.clicked(0f, 0f, 50f, 50f));
        ui.setInputEnabled(true);
        assertTrue(ui.clicked(0f, 0f, 50f, 50f));
    }
}

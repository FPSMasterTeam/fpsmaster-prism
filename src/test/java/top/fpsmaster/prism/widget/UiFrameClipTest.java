package top.fpsmaster.prism.widget;

import org.junit.jupiter.api.Test;
import top.fpsmaster.prism.test.HeadlessHost;
import top.fpsmaster.prism.theme.Theme;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiFrameClipTest {
    @Test
    void clippedAwayRowDoesNotConsumeThePress() {
        // A settings row scrolled below the module-list viewport: painted away, so it must not eat the
        // press either — otherwise the click lands on nothing and the control under the cursor is dead.
        HeadlessHost host = new HeadlessHost(320f, 180f);
        host.input.setMouse(50, 150);
        host.input.press(0, 50, 150);
        UiFrame ui = new UiFrame(host, Theme.DARK);

        ui.pushClip(0f, 0f, 320f, 100f);
        assertFalse(ui.clicked(40f, 140f, 60f, 12f), "row below the clip must not be clickable");
        ui.popClip();

        assertTrue(ui.clicked(40f, 140f, 60f, 12f), "the press must still be available outside the clip");
    }

    @Test
    void clippedAwayRowDoesNotHover() {
        HeadlessHost host = new HeadlessHost(320f, 180f);
        host.input.setMouse(50, 150);
        UiFrame ui = new UiFrame(host, Theme.DARK);

        ui.pushClip(0f, 0f, 320f, 100f);
        assertFalse(ui.hovered(40f, 140f, 60f, 12f), "row below the clip must not highlight");
        ui.popClip();

        assertTrue(ui.hovered(40f, 140f, 60f, 12f));
    }

    @Test
    void partiallyVisibleRowStaysHitTestableWhereItShows() {
        HeadlessHost host = new HeadlessHost(320f, 180f);
        host.input.setMouse(50, 95);
        UiFrame ui = new UiFrame(host, Theme.DARK);

        ui.pushClip(0f, 0f, 320f, 100f);
        // Row spans y 90..110; only 90..100 survives the clip and the mouse is at 95.
        assertTrue(ui.hovered(40f, 90f, 60f, 20f));
        ui.popClip();
    }

    @Test
    void nestedClipsIntersectRatherThanReplace() {
        HeadlessHost host = new HeadlessHost(320f, 180f);
        host.input.setMouse(50, 150);
        UiFrame ui = new UiFrame(host, Theme.DARK);

        ui.pushClip(0f, 0f, 320f, 100f);
        // An expanded module clips its settings to a card taller than the list viewport; the inner clip
        // must not widen the outer one back out.
        ui.pushClip(0f, 0f, 320f, 900f);
        assertFalse(ui.hovered(40f, 140f, 60f, 12f), "inner clip must not escape the outer viewport");
        ui.popClip();
        ui.popClip();
    }
}

package top.fpsmaster.prism.widget;

import org.junit.jupiter.api.Test;
import top.fpsmaster.prism.input.Keys;
import top.fpsmaster.prism.test.HeadlessHost;
import top.fpsmaster.prism.theme.Theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextBoxScrollTest {
    @Test
    void typesAndBackspaces() {
        HeadlessHost host = new HeadlessHost(400, 240);
        host.input.press(0, 20, 10);
        host.input.type("ab");
        UiFrame ui = new UiFrame(host, Theme.DARK);
        TextBox box = new TextBox();
        box.draw(ui, 8, 8, 80, 17);
        assertEquals("ab", box.text());
        host.input.endFrame();
        host.input.pressKey(Keys.BACKSPACE);
        box.draw(new UiFrame(host, Theme.DARK), 8, 8, 80, 17);
        assertEquals("a", box.text());
        assertTrue(host.canvas.has("pushClip"));
        assertTrue(host.canvas.has("popClip"));
    }

    @Test
    void unfocusedDrawDoesNotEatLaterButtonClick() {
        HeadlessHost host = new HeadlessHost(400, 240);
        host.input.press(0, 50, 50);
        UiFrame ui = new UiFrame(host, Theme.DARK);
        TextBox box = new TextBox();
        box.draw(ui, 8, 8, 80, 17);
        assertTrue(Chrome.button(ui, 40, 40, 40, 18, "OK", Chrome.ButtonStyle.PRIMARY));
    }

    @Test
    void focusedOutsideClickUnfocusesWithoutEatingLaterClick() {
        HeadlessHost host = new HeadlessHost(400, 240);
        host.input.press(0, 50, 50);
        UiFrame ui = new UiFrame(host, Theme.DARK);
        TextBox box = new TextBox();
        box.setFocused(true);
        box.draw(ui, 8, 8, 80, 17);
        assertEquals(false, box.focused());
        assertTrue(ui.clicked(40, 40, 40, 18));
    }

    @Test
    void scrollClampsAndClips() {
        HeadlessHost host = new HeadlessHost(400, 240);
        host.input.setMouse(20, 20);
        host.input.addWheel(-3);
        UiFrame ui = new UiFrame(host, Theme.DARK);
        Scroll scroll = new Scroll("list");
        float off = scroll.begin(ui, 0, 0, 100, 40, 200);
        assertTrue(off < 0f);
        scroll.end(ui);
        assertEquals(0, host.canvas.clipDepth());
    }
}

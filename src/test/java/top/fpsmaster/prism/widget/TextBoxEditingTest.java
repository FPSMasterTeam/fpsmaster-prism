package top.fpsmaster.prism.widget;

import org.junit.jupiter.api.Test;
import top.fpsmaster.prism.input.Keys;
import top.fpsmaster.prism.test.HeadlessHost;
import top.fpsmaster.prism.theme.Theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextBoxEditingTest {
    private final HeadlessHost host = new HeadlessHost(400, 200);

    private TextBox focused(String text) {
        TextBox box = new TextBox(text);
        box.setFocused(true);
        return box;
    }

    private void frame(TextBox box) {
        box.draw(new UiFrame(host, Theme.DARK), 8, 8, 200, 17);
        host.input.endFrame();
    }

    private void key(TextBox box, int key) {
        host.input.pressKey(key);
        frame(box);
        host.input.setKeyDown(key, false);
    }

    @Test
    void caretMovesAndEditsInTheMiddle() {
        TextBox box = focused("helo");
        key(box, Keys.LEFT);
        host.input.type("l");
        frame(box);
        assertEquals("hello", box.text());
        assertTrue(box.changed());
        key(box, Keys.HOME);
        key(box, Keys.DELETE);
        assertEquals("ello", box.text());
        key(box, Keys.END);
        key(box, Keys.BACKSPACE);
        assertEquals("ell", box.text());
    }

    @Test
    void pasteInsertsClipboardAtCaretWithoutLineBreaks() {
        TextBox box = focused("ab");
        key(box, Keys.LEFT);
        host.input.setClipboard("X\r\nY\tZ\n");
        key(box, Keys.PASTE);
        assertEquals("aXY Zb", box.text());
    }

    @Test
    void selectAllCopyAndCut() {
        TextBox box = focused("secret-cookie");
        key(box, Keys.SELECT_ALL);
        key(box, Keys.COPY);
        assertEquals("secret-cookie", host.input.clipboard());
        key(box, Keys.CUT);
        assertEquals("", box.text());
        host.input.type("new");
        frame(box);
        assertEquals("new", box.text());
    }

    @Test
    void shiftExtendsSelectionAndTypingReplacesIt() {
        TextBox box = focused("abcd");
        host.input.setKeyDown(Keys.SHIFT, true);
        key(box, Keys.LEFT);
        key(box, Keys.LEFT);
        host.input.setKeyDown(Keys.SHIFT, false);
        assertEquals("cd", box.selectedText());
        host.input.type("Z");
        frame(box);
        assertEquals("abZ", box.text());
    }

    @Test
    void maskedFieldNeverCopiesToClipboard() {
        TextBox box = focused("hunter2");
        box.setMasked(true);
        host.input.setClipboard("before");
        key(box, Keys.SELECT_ALL);
        key(box, Keys.COPY);
        key(box, Keys.CUT);
        assertEquals("before", host.input.clipboard());
        assertEquals("hunter2", box.text());
        assertFalse(host.canvas.has("drawString:hunter2"));
    }

    @Test
    void imeCommitAndMaxLength() {
        TextBox box = focused("");
        box.setMaxLength(4);
        host.input.type("你好世界啊");
        frame(box);
        assertEquals("你好世界", box.text());
        // A surrogate pair is never split by the length cap or by BACKSPACE.
        TextBox emoji = focused("a😀");
        key(emoji, Keys.BACKSPACE);
        assertEquals("a", emoji.text());
    }

    @Test
    void externalSetTextIsNotAUserEdit() {
        TextBox box = focused("x");
        box.setText("y");
        frame(box);
        assertFalse(box.changed());
    }

    @Test
    void clickPlacesCaretNearestToPointer() {
        TextBox box = new TextBox("abcdef");
        // RecordingFont(14) is 7.7 units per char; text starts at x + padLeft = 12.
        host.input.press(0, Math.round(12f + 7.7f * 2f), 16);
        frame(box);
        host.input.type("_");
        frame(box);
        assertEquals("ab_cdef", box.text());
    }
}

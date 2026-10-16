package top.fpsmaster.prism.screen;

import org.junit.jupiter.api.Test;
import top.fpsmaster.prism.input.Keys;
import top.fpsmaster.prism.test.HeadlessHost;
import top.fpsmaster.prism.theme.Theme;
import top.fpsmaster.prism.widget.TextBox;
import top.fpsmaster.prism.widget.UiFrame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClickGuiFieldsTest {
    @Test
    void removingFirstListItemKeepsTheOtherTexts() {
        HeadlessHost host = new HeadlessHost(500, 320);
        ListBridge bridge = new ListBridge("A", "B", "C");
        SharedClickGui gui = new SharedClickGui("cat");
        expand(host, gui, bridge);

        host.canvas.ops.clear();
        frame(host, gui, bridge);
        float[] remove = host.canvas.textAt("×");
        assertNotNull(remove);
        host.input.press(0, Math.round(remove[0]) + 2, Math.round(remove[1]) + 2);
        frame(host, gui, bridge);
        for (int i = 0; i < 5; i++) {
            frame(host, gui, bridge);
        }

        assertEquals(Arrays.asList("B", "C"), bridge.texts());
        assertTrue(bridge.textWrites.isEmpty(), "no field may write a stale text back: " + bridge.textWrites);
    }

    @Test
    void textSettingAdoptsExternalChangeWhenNotEditing() {
        HeadlessHost host = new HeadlessHost(500, 320);
        ListBridge bridge = new ListBridge();
        bridge.textValue = "old";
        SharedClickGui gui = new SharedClickGui("cat");
        expand(host, gui, bridge);
        assertTrue(host.canvas.has("drawString:old"));

        // A profile load (or command) changes the value behind the open ClickGUI.
        bridge.textValue = "new";
        host.canvas.ops.clear();
        frame(host, gui, bridge);
        frame(host, gui, bridge);

        assertEquals("new", bridge.textValue);
        assertTrue(bridge.textWrites.isEmpty());
        assertTrue(host.canvas.has("drawString:new"));
    }

    @Test
    void syncWritesBackOnlyUserEdits() {
        HeadlessHost host = new HeadlessHost(200, 100);
        TextBox box = new TextBox("abc");
        box.setFocused(true);
        host.input.pressKey(Keys.BACKSPACE);
        box.draw(new UiFrame(host, Theme.DARK), 0, 0, 100, 16);
        assertTrue(SharedClickGui.syncField(box, "abc"));
        assertEquals("ab", box.text());

        host.input.endFrame();
        box.draw(new UiFrame(host, Theme.DARK), 0, 0, 100, 16);
        // Focused and untouched this frame: neither direction (the bridge may still be catching up).
        assertFalse(SharedClickGui.syncField(box, "abc"));
        assertEquals("ab", box.text());

        box.setFocused(false);
        assertFalse(SharedClickGui.syncField(box, "xyz"));
        assertEquals("xyz", box.text());
    }

    private static void expand(HeadlessHost host, SharedClickGui gui, ListBridge bridge) {
        host.input.press(0, 160, 46);
        frame(host, gui, bridge);
        for (int i = 0; i < 30; i++) {
            frame(host, gui, bridge);
        }
    }

    private static void frame(HeadlessHost host, SharedClickGui gui, ListBridge bridge) {
        host.advanceMillis(16);
        gui.draw(new UiFrame(host, Theme.DARK), bridge);
        host.input.endFrame();
    }

    private static final class ListBridge implements ClickGuiBridge {
        final List<ListItem> items = new ArrayList<ListItem>();
        final List<String> textWrites = new ArrayList<String>();
        String textValue;

        ListBridge(String... texts) {
            for (int i = 0; i < texts.length; i++) {
                items.add(new ListItem(texts[i], 30 + i, "K" + i));
            }
        }

        List<String> texts() {
            List<String> out = new ArrayList<String>();
            for (ListItem item : items) {
                out.add(item.text);
            }
            return out;
        }

        public String i18n(String key) {
            return key;
        }

        public String edition() {
            return "T";
        }

        public String version() {
            return "t";
        }

        public List<String> categories() {
            return Arrays.asList("cat");
        }

        public String categoryLabel(String id) {
            return id;
        }

        public String categoryIcon(String id) {
            return "zap";
        }

        public int moduleCount(String categoryId) {
            return 1;
        }

        public int enabledCount(String categoryId) {
            return 0;
        }

        public List<ModInfo> modules(String categoryId, String query) {
            List<SettingInfo> settings = new ArrayList<SettingInfo>();
            if (textValue != null) {
                settings.add(new SettingInfo("msg", "Message", textValue));
            } else {
                settings.add(new SettingInfo("lines", "Lines", new ArrayList<ListItem>(items), 10, true));
            }
            List<ModInfo> mods = new ArrayList<ModInfo>();
            mods.add(new ModInfo("autotext", "AutoText", true, true, settings));
            return mods;
        }

        public void toggle(String moduleId) {
        }

        public void setNumber(String moduleId, String settingId, double value) {
        }

        public void setBool(String moduleId, String settingId, boolean value) {
        }

        public void setText(String moduleId, String settingId, String value) {
            textWrites.add(value);
            textValue = value;
        }

        public void removeListItem(String moduleId, String settingId, int index) {
            items.remove(index);
        }

        public void setListItemText(String moduleId, String settingId, int index, String value) {
            textWrites.add(index + "=" + value);
            ListItem old = items.get(index);
            items.set(index, new ListItem(value, old.keyCode, old.keyName));
        }

        public boolean lightTheme() {
            return false;
        }

        public void toggleTheme() {
        }

        public void openMusic() {
        }

        public void openProfiles() {
        }
    }
}

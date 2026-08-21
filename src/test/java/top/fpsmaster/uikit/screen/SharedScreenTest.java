package top.fpsmaster.uikit.screen;

import org.junit.jupiter.api.Test;
import top.fpsmaster.uikit.test.HeadlessHost;
import top.fpsmaster.uikit.theme.Theme;
import top.fpsmaster.uikit.widget.UiFrame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedScreenTest {
    @Test
    void mainMenuDrawsWordmarkAndFiresSingleplayer() {
        HeadlessHost host = new HeadlessHost(480, 270);
        RecordingBridge bridge = new RecordingBridge();
        float heroX = 480f * 0.06f;
        float dockY = 270f - 32f - SharedMainMenu.TILE_H;
        host.input.setMouse((int) (heroX + 10), (int) (dockY + 10));
        host.input.press(0, (int) (heroX + 10), (int) (dockY + 10));
        SharedMainMenu.draw(new UiFrame(host, Theme.DARK), bridge);
        assertTrue(host.canvas.has("drawString:F"));
        assertTrue(host.canvas.has("drawString:P"));
        assertTrue(host.canvas.has("drawString:" + bridge.i18n("mainmenu.single")));
        assertEquals(Collections.singletonList("singleplayer"), bridge.actions);
    }

    @Test
    void clickGuiSelectsCategoryAndTogglesModule() {
        HeadlessHost host = new HeadlessHost(500, 320);
        ClickBridge bridge = new ClickBridge();
        SharedClickGui gui = new SharedClickGui("optimize");
        host.input.setMouse(30, 90);
        host.input.press(0, 30, 90);
        gui.draw(new UiFrame(host, Theme.DARK), bridge);
        assertEquals("render", gui.category);
        assertTrue(host.canvas.has("drawString:FPSMaster"));
        host.input.endFrame();
        // Switch sits on the right of the first module row (~448, ~46 at 500x320).
        host.input.setMouse(450, 46);
        host.input.press(0, 450, 46);
        gui.draw(new UiFrame(host, Theme.DARK), bridge);
        assertEquals(Collections.singletonList("sprint"), bridge.toggled);
    }

    @Test
    void backgroundsDrawsTitle() {
        HeadlessHost host = new HeadlessHost(500, 320);
        SharedBackgrounds gui = new SharedBackgrounds();
        gui.draw(new UiFrame(host, Theme.DARK), new BackgroundsTestBridge());
        assertTrue(host.canvas.has("drawString:backgroundselector.title"));
    }

    @Test
    void configProfilesDrawsActiveName() {
        HeadlessHost host = new HeadlessHost(500, 320);
        SharedConfigProfiles gui = new SharedConfigProfiles();
        gui.draw(new UiFrame(host, Theme.DARK), new ProfilesBridge());
        assertTrue(host.canvas.has("drawString:default"));
    }

    private static final class RecordingBridge implements MenuBridge {
        final List<String> actions = new ArrayList<String>();

        public String i18n(String key) {
            return key;
        }

        public String edition() {
            return "EDGE";
        }

        public String version() {
            return "test";
        }

        public String minecraftLabel() {
            return "Minecraft 1.8.9";
        }

        public String playerName() {
            return "Player";
        }

        public String accountTypeLabel() {
            return "Offline";
        }

        public ContinueServer continueServer() {
            return null;
        }

        public boolean showReplays() {
            return true;
        }

        public boolean showDevtools() {
            return false;
        }

        public void singleplayer() {
            actions.add("singleplayer");
        }

        public void multiplayer() {
            actions.add("multiplayer");
        }

        public void settings() {
            actions.add("settings");
        }

        public void replays() {
            actions.add("replays");
        }

        public void music() {
            actions.add("music");
        }

        public void backgrounds() {
            actions.add("backgrounds");
        }

        public void quit() {
            actions.add("quit");
        }

        public void continueConnect() {
            actions.add("continue");
        }

        public void devtools() {
            actions.add("devtools");
        }
    }

    private static final class ClickBridge implements ClickGuiBridge {
        final List<String> toggled = new ArrayList<String>();

        public String i18n(String key) {
            return key;
        }

        public String edition() {
            return "NOVA";
        }

        public String version() {
            return "test";
        }

        public List<String> categories() {
            List<String> ids = new ArrayList<String>();
            ids.add("optimize");
            ids.add("render");
            return ids;
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
            List<ModInfo> mods = new ArrayList<ModInfo>();
            mods.add(new ModInfo("sprint", "Sprint", false, true, settings));
            return mods;
        }

        public void toggle(String moduleId) {
            toggled.add(moduleId);
        }

        public void setNumber(String moduleId, String settingId, double value) {
        }

        public void setBool(String moduleId, String settingId, boolean value) {
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

    private static final class ProfilesBridge implements ConfigProfilesBridge {
        public String i18n(String key) {
            if ("configprofiles.count".equals(key)) {
                return "%s";
            }
            return key;
        }

        public String activeName() {
            return "default";
        }

        public java.util.List<Profile> profiles() {
            java.util.List<Profile> list = new java.util.ArrayList<Profile>();
            list.add(new Profile("default", System.currentTimeMillis(), 12L));
            return list;
        }

        public int enabledModules() {
            return 1;
        }

        public int hudModules() {
            return 0;
        }

        public long activeBytes() {
            return 12L;
        }

        public long activeModified() {
            return System.currentTimeMillis();
        }

        public boolean isDefault(String name) {
            return true;
        }

        public String load(String name) {
            return name;
        }

        public String delete(String name) {
            return name;
        }

        public String rename(String from, String to) {
            return to;
        }

        public String create(String name) {
            return name;
        }

        public String exportActive() {
            return "ok";
        }

        public String importFile() {
            return "ok";
        }

        public String resetAllOff() {
            return "ok";
        }
    }

    private static final class BackgroundsTestBridge implements BackgroundsBridge {
        public String i18n(String key) {
            return key;
        }

        public String selected() {
            return "panorama_1";
        }

        public void select(String id) {
        }

        public void pickCustom() {
        }

        public boolean hasCustom() {
            return false;
        }

        public void paintPreview(UiFrame ui, String id, float x, float y, float w, float h) {
            ui.canvas().fillRect(x, y, w, h, 0xFF334455);
        }

        public float classicHue() {
            return 0f;
        }

        public float classicSaturation() {
            return 0f;
        }

        public float classicBrightness() {
            return 0f;
        }

        public float classicAlpha() {
            return 1f;
        }

        public String classicMode() {
            return "STATIC";
        }

        public void setClassic(float hue, float saturation, float brightness, float alpha, String mode) {
        }
    }
}

package top.fpsmaster.prism.screen;

import org.junit.jupiter.api.Test;
import top.fpsmaster.prism.test.HeadlessHost;
import top.fpsmaster.prism.theme.Theme;
import top.fpsmaster.prism.widget.UiFrame;

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
    void clickGuiUsesSharedVisibilityAndGroupMetadata() {
        HeadlessHost host = new HeadlessHost(500, 320);
        ClickBridge bridge = new ClickBridge();
        bridge.withSettings = true;
        SharedClickGui gui = new SharedClickGui("optimize");
        host.input.setMouse(160, 46);
        host.input.press(0, 160, 46);
        gui.draw(new UiFrame(host, Theme.DARK), bridge);
        for (int i = 0; i < 20; i++) {
            host.input.endFrame();
            gui.draw(new UiFrame(host, Theme.DARK), bridge);
        }
        assertTrue(host.canvas.has("drawString:Style"));
        assertTrue(host.canvas.has("drawString:Round"));
        assertTrue(!host.canvas.has("drawString:Secret"));
    }

    @Test
    void clickGuiKeepsCategoriesAboveFooterOnShortScreens() {
        HeadlessHost host = new HeadlessHost(300, 220);
        ClickBridge bridge = new ClickBridge();
        bridge.showAllCategories = true;
        SharedClickGui gui = new SharedClickGui("optimize");
        host.input.setMouse(30, 116);
        host.input.press(0, 30, 116);

        gui.draw(new UiFrame(host, Theme.DARK), bridge);

        assertEquals("optimize", gui.category);
        assertTrue(bridge.cosmeticsOpened);
    }

    @Test
    void backgroundsDrawsTitle() {
        HeadlessHost host = new HeadlessHost(500, 320);
        SharedBackgrounds gui = new SharedBackgrounds();
        gui.draw(new UiFrame(host, Theme.DARK), new BackgroundsTestBridge());
        assertTrue(host.canvas.has("drawString:backgroundselector.title"));
    }

    @Test
    void colorPickerKeepsCompactAspectRatio() {
        assertEquals(160f, SharedClickGui.colorPickerWidth(540f, false));
        assertEquals(80f, SharedClickGui.colorPickerHeight(160f));
    }

    @Test
    void cosmeticsDrawsCatalogAndPreviewsItems() {
        HeadlessHost host = new HeadlessHost(500, 320);
        SharedCosmetics gui = new SharedCosmetics();
        CosmeticsTestBridge bridge = new CosmeticsTestBridge();
        gui.draw(new UiFrame(host, Theme.DARK), bridge);
        assertTrue(host.canvas.has("drawString:Shop"));
        assertTrue(host.canvas.has("drawString:Owned"));
        assertTrue(host.canvas.has("drawString:All"));
        assertTrue(host.canvas.has("drawString:Cape"));
        assertTrue(host.canvas.has("drawString:Back"));
        assertEquals(2, bridge.itemPreviewIds.size());
        assertEquals("wings:free", bridge.itemPreviewIds.get(0));
        assertEquals("cape:1", bridge.itemPreviewIds.get(1));
        host.input.endFrame();
        host.input.setMouse(180, 100);
        host.input.press(0, 180, 100);
        gui.draw(new UiFrame(host, Theme.DARK), bridge);
        assertEquals("cape:1", bridge.previewedId);
    }

    @Test
    void cosmeticsPreviewRotatesByDragging() {
        HeadlessHost host = new HeadlessHost(500, 320);
        SharedCosmetics gui = new SharedCosmetics();
        CosmeticsTestBridge bridge = new CosmeticsTestBridge();
        host.input.press(0, 350, 120);
        gui.draw(new UiFrame(host, Theme.DARK), bridge);
        float initialYaw = bridge.previewYaw;
        host.input.endFrame();
        host.input.setMouse(400, 120);
        gui.draw(new UiFrame(host, Theme.DARK), bridge);
        assertTrue(bridge.previewYaw > initialYaw);
        host.input.release(0);
    }

    private static final class CosmeticsTestBridge implements CosmeticsBridge {
        float previewYaw;
        String previewedId;
        final List<String> itemPreviewIds = new ArrayList<String>();
        public String i18n(String key) {
            if ("cosmetics.store".equals(key)) return "Shop";
            if ("cosmetics.owned".equals(key)) return "Owned";
            if ("cosmetics.filter.all".equals(key)) return "All";
            if ("cosmetics.filter.cape".equals(key) || "cosmetics.cape".equals(key)) return "Cape";
            if ("cosmetics.filter.back".equals(key) || "cosmetics.wings".equals(key)) return "Back";
            return key;
        }
        public String playerName() { return "Steve"; }
        public List<Item> items() {
            List<Item> items = new ArrayList<Item>();
            items.add(new Item("wings:free", "Classic Wings", "", "wings", "0", true, true, true));
            items.add(new Item("cape:1", "Test Cape", "", "cape", "998", false, false, false));
            return items;
        }
        public void previewItem(String id) { previewedId = id; }
        public void paintItemPreview(UiFrame ui, Item item, float x, float y, float w, float h) {
            itemPreviewIds.add(item.id());
        }
        public boolean capeEnabled() { return true; }
        public void setCapeEnabled(boolean enabled) { }
        public float wingScale() { return 1f; }
        public void setWingScale(float scale) { }
        public void paintPlayerPreview(UiFrame ui, float x, float y, float w, float h, float yaw) {
            previewYaw = yaw;
        }
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
        boolean withSettings;
        boolean showAllCategories;
        boolean cosmeticsOpened;

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
            if (showAllCategories) {
                ids.add("utility");
                ids.add("interface");
            }
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
            if (withSettings) {
                GroupInfo style = new GroupInfo("style", "Style");
                settings.add(new SettingInfo("round", "Round", true).presentation(true, style));
                settings.add(new SettingInfo("secret", "Secret", true).presentation(false, style));
            }
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

        public void openCosmetics() {
            cosmeticsOpened = true;
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

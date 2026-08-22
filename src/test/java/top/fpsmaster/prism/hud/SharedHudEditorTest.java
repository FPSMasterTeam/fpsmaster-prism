package top.fpsmaster.prism.hud;

import org.junit.jupiter.api.Test;
import top.fpsmaster.prism.test.HeadlessHost;
import top.fpsmaster.prism.theme.Theme;
import top.fpsmaster.prism.widget.UiFrame;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedHudEditorTest {
    @Test
    void selectsMovesAndSavesOnClose() {
        Bridge bridge = new Bridge();
        SharedHudEditor editor = new SharedHudEditor();
        HeadlessHost host = new HeadlessHost(320, 180);
        host.input.setMouse(30, 50);
        host.input.press(0, 30, 50);

        editor.draw(new UiFrame(host, Theme.DARK), bridge);
        assertEquals("fps", editor.selectedId());
        host.input.endFrame();
        host.input.setMouse(160, 90);
        editor.draw(new UiFrame(host, Theme.DARK), bridge);
        assertTrue(bridge.x > 100f);
        host.input.release(0);
        editor.close(bridge);
        assertEquals(1, bridge.saves);
        assertEquals(1, bridge.closes);
    }

    private static final class Bridge implements HudEditorBridge {
        float x = 20f;
        float y = 40f;
        float scale = 1f;
        int saves;
        int closes;

        public String i18n(String key) { return key; }
        public List<Item> items() {
            List<Item> result = new ArrayList<Item>();
            result.add(new Item("fps", "FPS", x, y, 40f, 14f, scale, 0.5f, 4f, true));
            return result;
        }
        public void paintPreview(String id, float x, float y, float scale) { }
        public void setPlacement(String id, float x, float y, float scale, float w, float h) {
            this.x = x; this.y = y; this.scale = scale;
        }
        public void disable(String id) { }
        public void save() { saves++; }
        public void close() { closes++; }
    }
}

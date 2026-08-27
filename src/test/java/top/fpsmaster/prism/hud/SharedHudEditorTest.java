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

    @Test
    void letterboxedHostClampsDragToItsOwnSurface() {
        // 40px bars either side: dragging far right must stop at the surface edge, not the screen edge.
        Bridge bridge = new Bridge();
        bridge.bounds = new float[]{40f, SharedHudEditor.CONTENT_TOP, 240f, 155f};
        SharedHudEditor editor = new SharedHudEditor();
        HeadlessHost host = new HeadlessHost(320, 180);
        host.input.setMouse(45, 50);
        host.input.press(0, 45, 50);
        editor.draw(new UiFrame(host, Theme.DARK), bridge);
        assertEquals("fps", editor.selectedId());

        host.input.endFrame();
        host.input.setMouse(319, 179);
        editor.draw(new UiFrame(host, Theme.DARK), bridge);

        assertTrue(bridge.x <= 240f, "x must stay within the surface, was " + bridge.x);
        assertTrue(bridge.y <= 166f, "y must stay within the surface, was " + bridge.y);
        assertTrue(bridge.x >= 40f, "x must stay within the surface, was " + bridge.x);
    }

    @Test
    void defaultBoundsFillTheContentArea() {
        float[] bounds = HudEditorBridge.EMPTY.contentBounds(320f, 180f);
        assertEquals(0f, bounds[0]);
        assertEquals(SharedHudEditor.CONTENT_TOP, bounds[1]);
        assertEquals(320f, bounds[2]);
        assertEquals(180f - SharedHudEditor.CONTENT_TOP, bounds[3]);
    }

    private static final class Bridge implements HudEditorBridge {
        float x = 20f;
        float y = 40f;
        float scale = 1f;
        int saves;
        int closes;
        float[] bounds;

        public String i18n(String key) { return key; }
        public float[] contentBounds(float width, float height) {
            return bounds == null ? HudEditorBridge.super.contentBounds(width, height) : bounds;
        }
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

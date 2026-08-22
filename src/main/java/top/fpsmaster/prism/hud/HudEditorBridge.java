package top.fpsmaster.prism.hud;

import java.util.Collections;
import java.util.List;

/** Platform boundary for the shared HUD editor. */
public interface HudEditorBridge {
    String i18n(String key);

    List<Item> items();

    /** Paint the platform-owned preview at the editor position and scale. */
    void paintPreview(String id, float x, float y, float scale);

    /** Store an absolute editor placement; clients may persist it as an anchored/normalized value. */
    void setPlacement(String id, float x, float y, float scale, float surfaceWidth, float surfaceHeight);

    void disable(String id);

    void save();

    void close();

    final class Item {
        public final String id;
        public final String label;
        public final float x;
        public final float y;
        public final float baseWidth;
        public final float baseHeight;
        public final float scale;
        public final float minScale;
        public final float maxScale;
        public final boolean scalable;

        public Item(String id, String label, float x, float y, float baseWidth, float baseHeight,
                    float scale, float minScale, float maxScale, boolean scalable) {
            this.id = id == null ? "" : id;
            this.label = label == null ? this.id : label;
            this.x = x;
            this.y = y;
            this.baseWidth = Math.max(1f, baseWidth);
            this.baseHeight = Math.max(1f, baseHeight);
            this.scale = scale;
            this.minScale = minScale;
            this.maxScale = Math.max(minScale, maxScale);
            this.scalable = scalable;
        }

        public float width() {
            return baseWidth * scale;
        }

        public float height() {
            return baseHeight * scale;
        }
    }

    HudEditorBridge EMPTY = new HudEditorBridge() {
        public String i18n(String key) { return key; }
        public List<Item> items() { return Collections.emptyList(); }
        public void paintPreview(String id, float x, float y, float scale) { }
        public void setPlacement(String id, float x, float y, float scale, float w, float h) { }
        public void disable(String id) { }
        public void save() { }
        public void close() { }
    };
}

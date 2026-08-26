package top.fpsmaster.prism.screen;

import java.util.Collections;
import java.util.List;
import top.fpsmaster.prism.widget.UiFrame;

/** Client-specific cosmetics catalog, equipment state and player rendering. */
public interface CosmeticsBridge {
    final class Item {
        private final String id;
        private final String name;
        private final String description;
        private final String category;
        private final String price;
        private final boolean owned;
        private final boolean equipped;
        private final boolean builtin;
        private final float scale;
        private final boolean allowResize;
        private final float minScale;
        private final float maxScale;

        public Item(String id, String name, String description, String category, String price,
                    boolean owned, boolean equipped, boolean builtin) {
            this(id, name, description, category, price, owned, equipped, builtin,
                    1f, true, 0.5f, 1.5f);
        }

        public Item(String id, String name, String description, String category, String price,
                    boolean owned, boolean equipped, boolean builtin,
                    float scale, boolean allowResize, float minScale, float maxScale) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.category = category;
            this.price = price;
            this.owned = owned;
            this.equipped = equipped;
            this.builtin = builtin;
            this.scale = scale;
            this.allowResize = allowResize;
            this.minScale = minScale;
            this.maxScale = maxScale;
        }

        public String id() { return id; }
        public String name() { return name; }
        public String description() { return description; }
        public String category() { return category; }
        public String price() { return price; }
        public boolean owned() { return owned; }
        public boolean equipped() { return equipped; }
        public boolean builtin() { return builtin; }
        public float scale() { return scale; }
        public boolean allowResize() { return allowResize; }
        public float minScale() { return minScale; }
        public float maxScale() { return maxScale; }
    }

    String i18n(String key);

    String playerName();

    default List<Item> items() {
        return Collections.emptyList();
    }

    default void previewItem(String id) {
    }

    default void equipItem(String id) {
    }

    default void purchaseItem(String id) {
    }

    default boolean signedIn() {
        return false;
    }

    default boolean purchasePending() {
        return false;
    }

    default String statusMessage() {
        return "";
    }

    /** Loadout sync state: {@code ok}, {@code unavailable} or {@code failed}. Default unavailable so hosts must opt in. */
    default String syncStatus() {
        return "unavailable";
    }

    default void openCustomFolder() {
    }

    default void paintItemPreview(UiFrame ui, Item item, float x, float y, float w, float h) {
    }

    boolean capeEnabled();

    void setCapeEnabled(boolean enabled);

    float wingScale();

    void setWingScale(float scale);

    default boolean wingScaleAdjustable() {
        return true;
    }

    void paintPlayerPreview(UiFrame ui, float x, float y, float w, float h, float yaw);
}

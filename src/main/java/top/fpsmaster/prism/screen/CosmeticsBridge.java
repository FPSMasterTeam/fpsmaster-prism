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

        public Item(String id, String name, String description, String category, String price,
                    boolean owned, boolean equipped, boolean builtin) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.category = category;
            this.price = price;
            this.owned = owned;
            this.equipped = equipped;
            this.builtin = builtin;
        }

        public String id() { return id; }
        public String name() { return name; }
        public String description() { return description; }
        public String category() { return category; }
        public String price() { return price; }
        public boolean owned() { return owned; }
        public boolean equipped() { return equipped; }
        public boolean builtin() { return builtin; }
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

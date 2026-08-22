package top.fpsmaster.prism.screen;

import java.util.List;

/** Module list + actions for {@link SharedClickGui}. */
public interface ClickGuiBridge {
    String i18n(String key);

    String edition();

    String version();

    List<String> categories();

    String categoryLabel(String id);

    String categoryIcon(String id);

    int moduleCount(String categoryId);

    int enabledCount(String categoryId);

    List<ModInfo> modules(String categoryId, String query);

    void toggle(String moduleId);

    default void setModuleKey(String moduleId, int keyCode) {
    }

    void setNumber(String moduleId, String settingId, double value);

    void setBool(String moduleId, String settingId, boolean value);

    boolean lightTheme();

    void toggleTheme();

    void openMusic();

    void openProfiles();

    default void setText(String moduleId, String settingId, String value) {
    }

    default void setChoice(String moduleId, String settingId, int index) {
    }

    default void setColor(String moduleId, String settingId, float hue, float saturation,
                          float brightness, float alpha, String mode) {
    }

    default void setKey(String moduleId, String settingId, int keyCode) {
    }

    default void addListItem(String moduleId, String settingId) {
    }

    default void removeListItem(String moduleId, String settingId, int index) {
    }

    default void setListItemText(String moduleId, String settingId, int index, String value) {
    }

    default void setListItemKey(String moduleId, String settingId, int index, int keyCode) {
    }

    default boolean webUi() {
        return false;
    }

    default boolean hasWebUiToggle() {
        return false;
    }

    default void toggleWebUi() {
    }

    final class ModInfo {
        public final String id;
        public final String name;
        public final boolean enabled;
        public final boolean canToggle;
        public final List<SettingInfo> settings;
        public final int keyCode;
        public final String keyName;

        public ModInfo(String id, String name, boolean enabled, boolean canToggle, List<SettingInfo> settings) {
            this(id, name, enabled, canToggle, settings, 0, "");
        }

        public ModInfo(String id, String name, boolean enabled, boolean canToggle, List<SettingInfo> settings,
                       int keyCode, String keyName) {
            this.id = id;
            this.name = name;
            this.enabled = enabled;
            this.canToggle = canToggle;
            this.settings = settings;
            this.keyCode = keyCode;
            this.keyName = keyName == null ? "" : keyName;
        }
    }

    final class SettingInfo {
        public static final int BOOL = 0;
        public static final int NUMBER = 1;
        public static final int TEXT = 2;
        public static final int CHOICE = 3;
        public static final int COLOR = 4;
        public static final int KEY = 5;
        public static final int LIST = 6;
        public final String id;
        public final String label;
        public final int kind;
        public final boolean boolValue;
        public final double numberValue;
        public final double min;
        public final double max;
        public final String textValue;
        public final List<String> options;
        public final int selectedIndex;
        public final float hue;
        public final float saturation;
        public final float brightness;
        public final float alpha;
        public final String colorMode;
        public final int keyCode;
        public final String keyName;
        public final List<ListItem> items;
        public final int maxItems;
        public final boolean editableItems;

        public SettingInfo(String id, String label, boolean boolValue) {
            this.id = id;
            this.label = label;
            this.kind = BOOL;
            this.boolValue = boolValue;
            this.numberValue = 0;
            this.min = 0;
            this.max = 1;
            this.textValue = "";
            this.options = java.util.Collections.emptyList();
            this.selectedIndex = 0;
            this.hue = this.saturation = this.brightness = this.alpha = 0f;
            this.colorMode = "";
            this.keyCode = 0;
            this.keyName = "";
            this.items = java.util.Collections.emptyList();
            this.maxItems = 0;
            this.editableItems = false;
        }

        public SettingInfo(String id, String label, double numberValue, double min, double max) {
            this.id = id;
            this.label = label;
            this.kind = NUMBER;
            this.boolValue = false;
            this.numberValue = numberValue;
            this.min = min;
            this.max = max;
            this.textValue = "";
            this.options = java.util.Collections.emptyList();
            this.selectedIndex = 0;
            this.hue = this.saturation = this.brightness = this.alpha = 0f;
            this.colorMode = "";
            this.keyCode = 0;
            this.keyName = "";
            this.items = java.util.Collections.emptyList();
            this.maxItems = 0;
            this.editableItems = false;
        }

        public SettingInfo(String id, String label, String textValue) {
            this.id = id;
            this.label = label;
            this.kind = TEXT;
            this.boolValue = false;
            this.numberValue = 0;
            this.min = 0;
            this.max = 1;
            this.textValue = textValue == null ? "" : textValue;
            this.options = java.util.Collections.emptyList();
            this.selectedIndex = 0;
            this.hue = this.saturation = this.brightness = this.alpha = 0f;
            this.colorMode = "";
            this.keyCode = 0;
            this.keyName = "";
            this.items = java.util.Collections.emptyList();
            this.maxItems = 0;
            this.editableItems = false;
        }

        public SettingInfo(String id, String label, List<String> options, int selectedIndex) {
            this.id = id;
            this.label = label;
            this.kind = CHOICE;
            this.boolValue = false;
            this.numberValue = this.min = this.max = 0;
            this.textValue = "";
            this.options = options == null ? java.util.Collections.<String>emptyList() : options;
            this.selectedIndex = selectedIndex;
            this.hue = this.saturation = this.brightness = this.alpha = 0f;
            this.colorMode = "";
            this.keyCode = 0;
            this.keyName = "";
            this.items = java.util.Collections.emptyList();
            this.maxItems = 0;
            this.editableItems = false;
        }

        public SettingInfo(String id, String label, float hue, float saturation, float brightness,
                           float alpha, String colorMode, List<String> modes) {
            this.id = id;
            this.label = label;
            this.kind = COLOR;
            this.boolValue = false;
            this.numberValue = this.min = this.max = 0;
            this.textValue = "";
            this.options = modes == null ? java.util.Collections.<String>emptyList() : modes;
            this.selectedIndex = 0;
            this.hue = hue;
            this.saturation = saturation;
            this.brightness = brightness;
            this.alpha = alpha;
            this.colorMode = colorMode == null ? "" : colorMode;
            this.keyCode = 0;
            this.keyName = "";
            this.items = java.util.Collections.emptyList();
            this.maxItems = 0;
            this.editableItems = false;
        }

        public SettingInfo(String id, String label, int keyCode, String keyName) {
            this.id = id;
            this.label = label;
            this.kind = KEY;
            this.boolValue = false;
            this.numberValue = this.min = this.max = 0;
            this.textValue = "";
            this.options = java.util.Collections.emptyList();
            this.selectedIndex = 0;
            this.hue = this.saturation = this.brightness = this.alpha = 0f;
            this.colorMode = "";
            this.keyCode = keyCode;
            this.keyName = keyName == null ? "" : keyName;
            this.items = java.util.Collections.emptyList();
            this.maxItems = 0;
            this.editableItems = false;
        }

        public SettingInfo(String id, String label, List<ListItem> items, int maxItems, boolean editableItems) {
            this.id = id;
            this.label = label;
            this.kind = LIST;
            this.boolValue = false;
            this.numberValue = this.min = this.max = 0;
            this.textValue = "";
            this.options = java.util.Collections.emptyList();
            this.selectedIndex = 0;
            this.hue = this.saturation = this.brightness = this.alpha = 0f;
            this.colorMode = "";
            this.keyCode = 0;
            this.keyName = "";
            this.items = items == null ? java.util.Collections.<ListItem>emptyList() : items;
            this.maxItems = maxItems;
            this.editableItems = editableItems;
        }
    }

    final class ListItem {
        public final String text;
        public final int keyCode;
        public final String keyName;

        public ListItem(String text) {
            this(text, 0, "");
        }

        public ListItem(String text, int keyCode, String keyName) {
            this.text = text == null ? "" : text;
            this.keyCode = keyCode;
            this.keyName = keyName == null ? "" : keyName;
        }
    }
}

package top.fpsmaster.uikit.screen;

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

    void setNumber(String moduleId, String settingId, double value);

    void setBool(String moduleId, String settingId, boolean value);

    boolean lightTheme();

    void toggleTheme();

    void openMusic();

    void openProfiles();

    default void setText(String moduleId, String settingId, String value) {
    }

    default boolean webUi() {
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

        public ModInfo(String id, String name, boolean enabled, boolean canToggle, List<SettingInfo> settings) {
            this.id = id;
            this.name = name;
            this.enabled = enabled;
            this.canToggle = canToggle;
            this.settings = settings;
        }
    }

    final class SettingInfo {
        public static final int BOOL = 0;
        public static final int NUMBER = 1;
        public static final int TEXT = 2;
        public final String id;
        public final String label;
        public final int kind;
        public final boolean boolValue;
        public final double numberValue;
        public final double min;
        public final double max;
        public final String textValue;

        public SettingInfo(String id, String label, boolean boolValue) {
            this.id = id;
            this.label = label;
            this.kind = BOOL;
            this.boolValue = boolValue;
            this.numberValue = 0;
            this.min = 0;
            this.max = 1;
            this.textValue = "";
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
        }
    }
}

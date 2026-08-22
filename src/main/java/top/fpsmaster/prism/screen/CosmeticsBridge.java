package top.fpsmaster.prism.screen;

import top.fpsmaster.prism.widget.UiFrame;

/** Client-specific cosmetics state and player rendering for {@link SharedCosmetics}. */
public interface CosmeticsBridge {
    String i18n(String key);

    String playerName();

    boolean capeEnabled();

    void setCapeEnabled(boolean enabled);

    boolean wingsEnabled();

    void setWingsEnabled(boolean enabled);

    float wingScale();

    void setWingScale(float scale);

    void paintPlayerPreview(UiFrame ui, float x, float y, float w, float h, float yaw);
}

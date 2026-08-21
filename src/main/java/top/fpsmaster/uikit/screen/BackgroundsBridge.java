package top.fpsmaster.uikit.screen;

import top.fpsmaster.uikit.widget.UiFrame;

/** Client-specific data for {@link SharedBackgrounds}. */
public interface BackgroundsBridge {
    String[] OPTIONS = {
            "classic", "shader", "panorama_1", "panorama_2", "panorama_3", "custom"
    };

    String i18n(String key);

    String selected();

    void select(String id);

    void pickCustom();

    boolean hasCustom();

    void paintPreview(UiFrame ui, String id, float x, float y, float w, float h);

    float classicHue();

    float classicSaturation();

    float classicBrightness();

    float classicAlpha();

    String classicMode();

    void setClassic(float hue, float saturation, float brightness, float alpha, String mode);
}

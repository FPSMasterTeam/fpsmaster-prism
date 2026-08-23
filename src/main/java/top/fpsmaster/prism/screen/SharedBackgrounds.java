package top.fpsmaster.prism.screen;

import top.fpsmaster.prism.canvas.FontHandle;
import top.fpsmaster.prism.icon.GlyphIcons;
import top.fpsmaster.prism.theme.Argb;
import top.fpsmaster.prism.theme.Metrics;
import top.fpsmaster.prism.widget.Chrome;
import top.fpsmaster.prism.widget.Scroll;
import top.fpsmaster.prism.widget.UiFrame;

/**
 * Shared main-menu background picker (Edge {@code BackgroundSelector} layout).
 */
public final class SharedBackgrounds {
    private static final float CARD_H = 48f;
    private static final float CARD_GAP = 5f;
    private static final String[] MODES = {"STATIC", "WAVE", "CHROMA", "RAINBOW"};

    private final Scroll scroll = new Scroll("backgrounds");
    private final Object hueDrag = new Object();
    private final Object satDrag = new Object();
    private final Object briDrag = new Object();
    private final Object alpDrag = new Object();

    public boolean draw(UiFrame ui, BackgroundsBridge bridge) {
        float gw = ui.host().width();
        float gh = ui.host().height();
        Chrome.veil(ui, 1f);
        float pw = Math.min(250f, gw - 24f);
        int rows = (BackgroundsBridge.OPTIONS.length + 1) / 2;
        boolean classic = isSelected(bridge, "classic");
        float editorH = classic ? 78f : 0f;
        float content = 8f + rows * (CARD_H + CARD_GAP) + 8f + editorH;
        float ph = Math.min(30f + content + 10f, Math.min(gh * 0.78f, gh - 24f));
        float px = (gw - pw) / 2f;
        float py = (gh - ph) / 2f;
        Chrome.panel(ui, px, py, pw, ph);

        float headY = py + 8f;
        if (back(ui, px + 8f, headY, bridge.i18n("configprofiles.back"))) {
            return true;
        }
        FontHandle titleFont = ui.font(16);
        FontBold.draw(ui, 16, bridge.i18n("backgroundselector.title"),
                px + 28f, Chrome.textY(headY, 15f, titleFont), ui.theme().textPrimary());
        String pick = bridge.i18n("backgroundselector.pick");
        float pickW = ui.font(13).measure(pick) + 16f;
        if (Chrome.button(ui, px + pw - 10f - pickW, headY, pickW, 15f, pick, Chrome.ButtonStyle.DEFAULT)) {
            bridge.pickCustom();
        }

        float contentX = px + 10f;
        float contentY = py + 30f;
        float contentW = pw - 20f;
        float contentH = ph - 40f;
        float cardW = (contentW - CARD_GAP) / 2f;
        float totalH = 4f + rows * (CARD_H + CARD_GAP) + 6f + editorH;
        float off = scroll.begin(ui, contentX, contentY, contentW, contentH, totalH);
        for (int i = 0; i < BackgroundsBridge.OPTIONS.length; i++) {
            String id = BackgroundsBridge.OPTIONS[i];
            float cx = contentX + (i % 2) * (cardW + CARD_GAP);
            float cy = contentY + 4f + (i / 2) * (CARD_H + CARD_GAP) + off;
            drawCard(ui, bridge, cx, cy, cardW, id);
        }
        if (classic) {
            float editorY = contentY + 4f + rows * (CARD_H + CARD_GAP) + off;
            drawClassicEditor(ui, bridge, contentX, editorY, contentW);
        }
        scroll.end(ui);

        if (ui.input().consumePressOutside(px, py, pw, ph) != null) {
            return true;
        }
        return false;
    }

    private void drawCard(UiFrame ui, BackgroundsBridge bridge, float x, float y, float w, String id) {
        boolean selected = isSelected(bridge, id);
        boolean hover = ui.hovered(x, y, w, CARD_H);
        int stroke = selected ? ui.theme().accent() : (hover ? ui.theme().strokeStrong() : ui.theme().stroke());
        ui.canvas().fillRoundRect(x, y, w, CARD_H, Metrics.CARD_RADIUS, Argb.rgb(22, 22, 25));
        bridge.paintPreview(ui, id, x + 1f, y + 1f, w - 2f, CARD_H - 2f);
        float labelH = 13f;
        ui.canvas().fillRoundRect(x + 1f, y + CARD_H - 1f - labelH, w - 2f, labelH, Metrics.CARD_RADIUS,
                Argb.of(166, 0, 0, 0));
        ui.canvas().drawString(ui.font(12), bridge.i18n("backgroundselector.option." + id + ".name"),
                x + 6f, y + CARD_H - 1f - labelH + 3.5f, ui.theme().textPrimary());
        if (selected) {
            float ckX = x + w - 14f;
            float ckY = y + CARD_H - 1f - labelH + 2.5f;
            ui.canvas().fillRoundRect(ckX, ckY, 8f, 8f, 4f, ui.theme().accent());
            GlyphIcons.draw(ui, "check", ckX + 1.5f, ckY + 1.5f, 5f, 0xFFFFFFFF);
        }
        ui.canvas().strokeRoundRect(x + 0.5f, y + 0.5f, w - 1f, CARD_H - 1f,
                Metrics.CARD_RADIUS - 0.5f, 0.75f, stroke);
        if (ui.clicked(x, y, w, CARD_H)) {
            bridge.select(id);
        }
    }

    private void drawClassicEditor(UiFrame ui, BackgroundsBridge bridge, float x, float y, float w) {
        ui.canvas().fillRoundRect(x, y, w, 74f, Metrics.CARD_RADIUS, ui.theme().layer());
        float hue = Chrome.slider(ui, hueDrag, x + 8f, y + 6f, w - 16f, bridge.classicHue());
        float sat = Chrome.slider(ui, satDrag, x + 8f, y + 20f, w - 16f, bridge.classicSaturation());
        float bri = Chrome.slider(ui, briDrag, x + 8f, y + 34f, w - 16f, bridge.classicBrightness());
        float alp = Chrome.slider(ui, alpDrag, x + 8f, y + 48f, w - 16f, bridge.classicAlpha());
        String mode = bridge.classicMode();
        float mx = x + 8f;
        float mw = (w - 16f - 9f) / 4f;
        for (int i = 0; i < MODES.length; i++) {
            boolean on = MODES[i].equalsIgnoreCase(mode);
            if (Chrome.button(ui, mx + i * (mw + 3f), y + 58f, mw, 12f,
                    bridge.i18n(modeKey(MODES[i])),
                    on ? Chrome.ButtonStyle.PRIMARY : Chrome.ButtonStyle.GHOST)) {
                mode = MODES[i];
            }
        }
        if (hue != bridge.classicHue() || sat != bridge.classicSaturation()
                || bri != bridge.classicBrightness() || alp != bridge.classicAlpha()
                || !mode.equalsIgnoreCase(bridge.classicMode())) {
            bridge.setClassic(hue, sat, bri, alp, mode);
        }
    }

    private boolean back(UiFrame ui, float x, float y, String label) {
        float w = 15f;
        float h = 15f;
        boolean hover = ui.hovered(x, y, w, h);
        Chrome.ghostButton(ui, x, y, w, h, hover);
        GlyphIcons.draw(ui, "back", x + 4f, y + 4f, 7f,
                hover ? ui.theme().textPrimary() : ui.theme().textSecondary());
        return ui.clicked(x, y, w, h);
    }

    private static String modeKey(String mode) {
        if ("WAVE".equalsIgnoreCase(mode)) {
            return "colorsetting.type.breath";
        }
        return "colorsetting.type." + mode.toLowerCase();
    }

    private static boolean isSelected(BackgroundsBridge bridge, String id) {
        String current = bridge.selected();
        if ("panorama_1".equals(id) && ("panorama".equals(current) || current == null || current.isEmpty())) {
            return true;
        }
        return id.equals(current);
    }
}

package top.fpsmaster.prism.screen;

import top.fpsmaster.prism.canvas.FontHandle;
import top.fpsmaster.prism.icon.GlyphIcons;
import top.fpsmaster.prism.theme.Argb;
import top.fpsmaster.prism.theme.Metrics;
import top.fpsmaster.prism.widget.Chrome;
import top.fpsmaster.prism.widget.UiFrame;

/** Shared cosmetics wardrobe. Minecraft entity rendering remains in the client bridge. */
public final class SharedCosmetics {
    private final Object scaleDrag = new Object();

    public boolean draw(UiFrame ui, CosmeticsBridge bridge) {
        float gw = ui.host().width();
        float gh = ui.host().height();
        Chrome.veil(ui, 1f);
        float pw = Math.min(430f, gw - 24f);
        float ph = Math.min(270f, gh - 24f);
        float px = (gw - pw) / 2f;
        float py = (gh - ph) / 2f;
        Chrome.panel(ui, px, py, pw, ph);

        if (back(ui, px + 8f, py + 8f)) return true;
        FontBold.draw(ui, 17, bridge.i18n("cosmetics.title"), px + 30f, py + 10f, ui.theme().textPrimary());
        ui.canvas().drawString(ui.font(11), bridge.i18n("cosmetics.subtitle"), px + 30f, py + 25f,
                ui.theme().textSecondary());

        float previewX = px + 10f;
        float previewY = py + 43f;
        float previewW = Math.max(150f, pw * 0.53f);
        float previewH = ph - 53f;
        ui.canvas().fillRoundRect(previewX, previewY, previewW, previewH, Metrics.CARD_RADIUS, ui.theme().layer());
        ui.canvas().fillGradientV(previewX + 1f, previewY + 1f, previewW - 2f, previewH - 2f,
                Argb.of(18, 255, 255, 255), Argb.of(0, 255, 255, 255));
        float yaw = (ui.host().nowNanos() / 1_000_000_000f * 18f) % 360f;
        bridge.paintPlayerPreview(ui, previewX, previewY, previewW, previewH, yaw);
        FontHandle name = ui.font(13);
        ui.canvas().drawString(name, bridge.playerName(), previewX + 8f, previewY + previewH - 18f,
                ui.theme().textPrimary());
        ui.canvas().drawString(ui.font(10), bridge.i18n("cosmetics.preview.rotate"), previewX + 8f,
                previewY + previewH - 8f, ui.theme().textDisabled());

        float controlsX = previewX + previewW + 8f;
        float controlsW = px + pw - 10f - controlsX;
        drawSlot(ui, bridge, controlsX, previewY, controlsW, "cape", bridge.capeEnabled());
        drawSlot(ui, bridge, controlsX, previewY + 58f, controlsW, "wings", bridge.wingsEnabled());
        float sliderY = previewY + 122f;
        ui.canvas().drawString(ui.font(11), bridge.i18n("cosmetics.wings.scale"), controlsX, sliderY,
                bridge.wingsEnabled() ? ui.theme().textSecondary() : ui.theme().textDisabled());
        float next = Chrome.slider(ui, scaleDrag, controlsX, sliderY + 13f, controlsW, bridge.wingScale());
        if (bridge.wingsEnabled() && next != bridge.wingScale()) bridge.setWingScale(next);
        String value = Math.round(bridge.wingScale() * 100f) + "%";
        ui.canvas().drawString(ui.font(10), value, controlsX + controlsW - ui.font(10).measure(value), sliderY,
                ui.theme().textDisabled());
        return false;
    }

    private void drawSlot(UiFrame ui, CosmeticsBridge bridge, float x, float y, float w,
                          String id, boolean enabled) {
        boolean hover = ui.hovered(x, y, w, 51f);
        int stroke = enabled ? ui.theme().accent() : (hover ? ui.theme().strokeStrong() : ui.theme().stroke());
        ui.canvas().fillRoundRect(x - 0.5f, y - 0.5f, w + 1f, 52f, Metrics.CARD_RADIUS + 1f, stroke);
        ui.canvas().fillRoundRect(x, y, w, 51f, Metrics.CARD_RADIUS, hover ? ui.theme().layerHover() : ui.theme().layer());
        GlyphIcons.draw(ui, "sparkles", x + 9f, y + 10f, 12f,
                enabled ? ui.theme().accentText() : ui.theme().textSecondary());
        FontBold.draw(ui, 13, bridge.i18n("cosmetics." + id), x + 28f, y + 8f, ui.theme().textPrimary());
        ui.canvas().drawString(ui.font(10), bridge.i18n("cosmetics." + id + ".desc"), x + 28f, y + 23f,
                ui.theme().textSecondary());
        Chrome.toggle(ui, x + w - 26f, y + 18f, enabled);
        if (ui.clicked(x, y, w, 51f)) {
            if ("cape".equals(id)) bridge.setCapeEnabled(!enabled);
            else bridge.setWingsEnabled(!enabled);
        }
    }

    private boolean back(UiFrame ui, float x, float y) {
        boolean hover = ui.hovered(x, y, 15f, 15f);
        Chrome.ghostButton(ui, x, y, 15f, 15f, hover);
        GlyphIcons.draw(ui, "back", x + 4f, y + 4f, 7f,
                hover ? ui.theme().textPrimary() : ui.theme().textSecondary());
        return ui.clicked(x, y, 15f, 15f);
    }
}

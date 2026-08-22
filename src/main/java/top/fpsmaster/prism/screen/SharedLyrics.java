package top.fpsmaster.prism.screen;

import top.fpsmaster.prism.anim.Anim;
import top.fpsmaster.prism.canvas.FontHandle;
import top.fpsmaster.prism.theme.Argb;
import top.fpsmaster.prism.widget.UiFrame;

import java.util.List;

/** Shared timed-lyrics layout used by the immersive player and both clients' HUDs. */
public final class SharedLyrics {
    private float scrollLine = Float.NaN;

    public void drawImmersive(UiFrame ui, MusicBridge bridge, float x, float y, float w, float h, float dt) {
        List<MusicBridge.LyricRow> rows = bridge.lyricRows();
        int current = bridge.currentLyricIndex();
        if (rows.isEmpty()) {
            centered(ui, bridge.playing() ? "歌词加载中…" : "未在播放", x, y + h * 0.5f, w,
                    16, ui.theme().textDisabled());
            return;
        }
        float target = Math.max(0, current);
        scrollLine = Float.isNaN(scrollLine) ? target : Anim.approach(scrollLine, target, 0.34f, dt);
        float centerY = y + h * 0.44f;
        for (int i = 0; i < rows.size(); i++) {
            MusicBridge.LyricRow row = rows.get(i);
            if (row.text.isEmpty()) continue;
            float distance = i - scrollLine;
            if (Math.abs(distance) > 5.5f) continue;
            float lineY = centerY + distance * 31f;
            boolean active = i == current;
            float alpha = active ? 1f : Math.max(0.12f, 0.55f - Math.abs(distance) * 0.075f);
            ui.canvas().pushAlpha(alpha);
            int size = active ? 24 : 17;
            FontHandle font = ui.font(size);
            String text = ellipsize(font, row.text, w - 36f);
            float textX = x + (w - font.measure(text)) * 0.5f;
            if (active) FontBold.draw(ui, size, text, textX, lineY, ui.theme().textPrimary());
            else ui.canvas().drawString(font, text, textX, lineY, ui.theme().textSecondary());
            if (active && !row.translation.isEmpty()) {
                centered(ui, row.translation, x, lineY + 17f, w, 12, ui.theme().textSecondary());
            }
            ui.canvas().popAlpha();
        }
    }

    public void drawHud(UiFrame ui, List<MusicBridge.LyricRow> rows, int current,
                        float x, float y, float w, HudStyle style, float dt) {
        if (rows.isEmpty() || current < 0 || current >= rows.size()) return;
        int count = Math.max(1, Math.min(5, style.lineCount));
        float lineH = style.fontSize * 0.5f + (style.showTranslation ? 10f : 5f);
        float h = count * lineH + 10f;
        if (style.background) {
            ui.canvas().fillRoundRect(x, y, w, h, 6f, style.backgroundColor);
        }
        float target = style.scroll ? current : 0f;
        scrollLine = Float.isNaN(scrollLine) ? target : Anim.approach(scrollLine, target, 0.28f, dt);
        float firstLine = style.scroll ? Math.max(0f, scrollLine - count * 0.5f) : current;
        int first = (int) Math.floor(firstLine);
        float fractional = firstLine - first;
        for (int slot = 0; slot <= count; slot++) {
            int index = first + slot;
            if (index >= rows.size()) break;
            MusicBridge.LyricRow row = rows.get(index);
            boolean active = index == current;
            float lineY = y + 5f + (slot - fractional) * lineH;
            if (lineY < y + 2f || lineY + lineH > y + h - 2f) continue;
            FontHandle font = ui.font(style.fontSize);
            String text = ellipsize(font, row.text, w - 12f);
            int color = active ? style.textColor : Argb.mulAlpha(style.textColor, 0.48f);
            ui.canvas().drawString(font, text, x + (w - font.measure(text)) * 0.5f, lineY, color);
            if (style.showTranslation && !row.translation.isEmpty()) {
                centered(ui, row.translation, x + 5f, lineY + style.fontSize * 0.58f, w - 10f,
                        Math.max(9, style.fontSize - 4), Argb.mulAlpha(style.textColor, 0.68f));
            }
        }
    }

    public static float hudHeight(HudStyle style) {
        return Math.max(1, Math.min(5, style.lineCount))
                * (style.fontSize * 0.5f + (style.showTranslation ? 10f : 5f)) + 10f;
    }

    private static void centered(UiFrame ui, String text, float x, float y, float w, int size, int color) {
        FontHandle font = ui.font(size);
        String shown = ellipsize(font, text, w);
        ui.canvas().drawString(font, shown, x + (w - font.measure(shown)) * 0.5f, y, color);
    }

    private static String ellipsize(FontHandle font, String text, float max) {
        if (text == null || text.isEmpty()) return "";
        if (font.measure(text) <= max) return text;
        for (int i = text.length() - 1; i > 0; i--) {
            String cut = text.substring(0, i) + "…";
            if (font.measure(cut) <= max) return cut;
        }
        return "…";
    }

    public static final class HudStyle {
        public final int fontSize;
        public final int lineCount;
        public final boolean showTranslation;
        public final boolean scroll;
        public final boolean background;
        public final int backgroundColor;
        public final int textColor;

        public HudStyle(int fontSize, int lineCount, boolean showTranslation, boolean scroll,
                        boolean background, int backgroundColor, int textColor) {
            this.fontSize = Math.max(10, Math.min(30, fontSize));
            this.lineCount = lineCount;
            this.showTranslation = showTranslation;
            this.scroll = scroll;
            this.background = background;
            this.backgroundColor = backgroundColor;
            this.textColor = textColor;
        }
    }
}

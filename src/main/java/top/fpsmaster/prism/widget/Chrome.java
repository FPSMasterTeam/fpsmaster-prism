package top.fpsmaster.prism.widget;

import top.fpsmaster.prism.canvas.Canvas;
import top.fpsmaster.prism.canvas.FontHandle;
import top.fpsmaster.prism.theme.Argb;
import top.fpsmaster.prism.theme.Metrics;
import top.fpsmaster.prism.theme.Theme;

/**
 * Draw-only and draw+hit chrome, twin of Edge {@code UiChrome} / {@code edge-ui.css}.
 */
public final class Chrome {
    public enum ButtonStyle {
        DEFAULT, PRIMARY, DANGER, DANGER_FILL, GHOST
    }

    private Chrome() {
    }

    public static void panel(UiFrame ui, float x, float y, float w, float h) {
        panel(ui, x, y, w, h, Metrics.PANEL_RADIUS);
    }

    public static void panel(UiFrame ui, float x, float y, float w, float h, float radius) {
        Theme theme = ui.theme();
        if (ui.host().blurEnabled()) {
            ui.host().blurBehind(x, y, w, h, radius);
        }
        strokeFill(ui.canvas(), x, y, w, h, radius, theme.stroke(), theme.glass());
    }

    public static void veil(UiFrame ui, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(Argb.alpha(ui.theme().veil()) * alpha)));
        ui.canvas().fillRect(0f, 0f, ui.host().width(), ui.host().height(), Argb.of(a, 0, 0, 0));
    }

    public static void card(UiFrame ui, float x, float y, float w, float h, boolean hover, boolean expanded) {
        Theme theme = ui.theme();
        int fill = expanded ? theme.cardExpanded() : (hover ? theme.cardHover() : theme.card());
        if (expanded) {
            strokeFill(ui.canvas(), x, y, w, h, Metrics.CARD_RADIUS, theme.stroke(), fill);
        } else {
            ui.canvas().fillRoundRect(x, y, w, h, Metrics.CARD_RADIUS, fill);
        }
    }

    public static void selectedCard(UiFrame ui, float x, float y, float w, float h) {
        selectedSurface(ui, x, y, w, h, Metrics.CARD_RADIUS);
        accentMark(ui, x, y + h * 0.22f, h * 0.56f);
    }

    public static void selectedSurface(UiFrame ui, float x, float y, float w, float h, float radius) {
        Theme theme = ui.theme();
        Canvas canvas = ui.canvas();
        canvas.fillRoundRect(x - 0.5f, y - 0.5f, w + 1f, h + 1f, radius + 1f, theme.accentBorder());
        canvas.fillRoundRect(x, y, w, h, radius, theme.opaquePanelBase());
        canvas.fillRoundRect(x, y, w, h, radius, theme.accentSoft());
    }

    public static void hairlineH(UiFrame ui, float x, float y, float w) {
        ui.canvas().fillRect(x, y, w, 0.5f, ui.theme().divider());
    }

    public static void hairlineV(UiFrame ui, float x, float y, float h) {
        ui.canvas().fillRect(x, y, 0.5f, h, ui.theme().divider());
    }

    public static void accentMark(UiFrame ui, float x, float y, float h) {
        ui.canvas().fillRoundRect(x, y, 1.5f, h, 1f, ui.theme().accent());
    }

    public static void fillButton(UiFrame ui, float x, float y, float w, float h, boolean hover, boolean danger) {
        int fill;
        if (danger) {
            fill = hover ? Argb.rgb(214, 60, 90) : ui.theme().danger();
        } else {
            fill = hover ? ui.theme().accentHover() : ui.theme().accent();
        }
        ui.canvas().fillRoundRect(x, y, w, h, Metrics.CTL_RADIUS, fill);
    }

    public static void button(UiFrame ui, float x, float y, float w, float h, boolean hover) {
        Theme theme = ui.theme();
        strokeFill(ui.canvas(), x, y, w, h, Metrics.CTL_RADIUS,
                hover ? theme.strokeStrong() : theme.stroke(),
                hover ? theme.layerHover() : theme.layer());
    }

    public static void dangerButton(UiFrame ui, float x, float y, float w, float h, boolean hover) {
        if (hover) {
            strokeFill(ui.canvas(), x, y, w, h, Metrics.CTL_RADIUS,
                    Argb.of(77, 240, 80, 110), ui.theme().dangerSoft());
        } else {
            button(ui, x, y, w, h, false);
        }
    }

    public static void ghostButton(UiFrame ui, float x, float y, float w, float h, boolean hover) {
        if (hover) {
            ui.canvas().fillRoundRect(x, y, w, h, Metrics.CTL_RADIUS, ui.theme().layerHover());
        }
    }

    public static void pillIconButton(UiFrame ui, float x, float y, float size, boolean hover) {
        pillIconButton(ui, x, y, size, hover ? 1f : 0f);
    }

    public static void pillIconButton(UiFrame ui, float x, float y, float size, float hoverT) {
        float t = hoverT < 0f ? 0f : (hoverT > 1f ? 1f : hoverT);
        float r = size / 2f;
        Theme theme = ui.theme();
        strokeFill(ui.canvas(), x, y, size, size, r,
                Argb.lerp(theme.stroke(), theme.strokeStrong(), t),
                Argb.lerp(theme.layer(), theme.layerHover(), t));
    }

    public static boolean button(UiFrame ui, float x, float y, float w, float h,
                                 String label, ButtonStyle style) {
        boolean hover = ui.hovered(x, y, w, h);
        int textColor;
        switch (style) {
            case PRIMARY:
                fillButton(ui, x, y, w, h, hover, false);
                textColor = ui.theme().white();
                break;
            case DANGER:
                dangerButton(ui, x, y, w, h, hover);
                textColor = ui.theme().danger();
                break;
            case DANGER_FILL:
                fillButton(ui, x, y, w, h, hover, true);
                textColor = ui.theme().white();
                break;
            case GHOST:
                ghostButton(ui, x, y, w, h, hover);
                textColor = hover ? ui.theme().textPrimary() : ui.theme().textSecondary();
                break;
            default:
                button(ui, x, y, w, h, hover);
                textColor = ui.theme().textPrimary();
                break;
        }
        if (label != null && !label.isEmpty()) {
            FontHandle font = ui.font(14);
            float tw = font.measure(label);
            ui.canvas().drawString(font, label, x + (w - tw) / 2f, textY(y, h, font), textColor);
        }
        return ui.clicked(x, y, w, h);
    }

    public static void drawSwitch(UiFrame ui, float x, float y, boolean on, float knobT) {
        drawSwitchSized(ui, x, y, Metrics.SWITCH_W, Metrics.SWITCH_H, on, knobT);
    }

    public static void drawSwitchSm(UiFrame ui, float x, float y, boolean on, float knobT) {
        drawSwitchSized(ui, x, y, Metrics.SWITCH_SM_W, Metrics.SWITCH_SM_H, on, knobT);
    }

    public static boolean toggle(UiFrame ui, float x, float y, boolean on) {
        drawSwitch(ui, x, y, on, on ? 1f : 0f);
        if (ui.clicked(x, y, Metrics.SWITCH_W, Metrics.SWITCH_H)) {
            return !on;
        }
        return on;
    }

    public static void slider(UiFrame ui, float x, float y, float width, float t, boolean showThumb) {
        Theme theme = ui.theme();
        float clamped = clamp01(t);
        float trackY = y + Metrics.SLIDER_H / 2f - 1.25f;
        ui.canvas().fillRoundRect(x, trackY, width, 2.5f, 1f, theme.layerActive());
        if (clamped > 0f) {
            ui.canvas().fillRoundRect(x, trackY, Math.max(2.5f, width * clamped), 2.5f, 1f, theme.accent());
        }
        if (showThumb) {
            float thumb = 7f;
            ui.canvas().fillRoundRect(
                    x + width * clamped - thumb / 2f,
                    y + Metrics.SLIDER_H / 2f - thumb / 2f,
                    thumb, thumb, thumb / 2f, theme.white());
        }
    }

    /**
     * Draws a slider and, while dragged, returns the new 0..1 value.
     */
    public static float slider(UiFrame ui, Object dragId, float x, float y, float width, float t) {
        if (ui.input().beginDrag(dragId, 0, x, y, width, Metrics.SLIDER_H) || ui.input().isDragging(dragId)) {
            t = clamp01((ui.input().mouseX() - x) / width);
            if (!ui.input().isButtonDown(0)) {
                ui.input().releaseDrag(dragId);
            }
        }
        boolean showThumb = ui.hovered(x, y, width, Metrics.SLIDER_H) || ui.input().isDragging(dragId);
        slider(ui, x, y, width, t, showThumb);
        return t;
    }

    public static void inputBox(UiFrame ui, float x, float y, float w, float h, boolean focused) {
        Theme theme = ui.theme();
        strokeFill(ui.canvas(), x, y, w, h, Metrics.CTL_RADIUS,
                focused ? theme.accent() : theme.stroke(),
                Argb.of(64, 0, 0, 0));
    }

    public static void pingBars(UiFrame ui, float x, float baselineY, int level, int litArgb) {
        int dim = ui.theme().layerActive();
        for (int i = 0; i < 4; i++) {
            float h = 2f + i;
            int c = i < level ? litArgb : dim;
            ui.canvas().fillRoundRect(x + i * 2.25f, baselineY - h, 1.5f, h, 1f, c);
        }
    }

    public static int pingLevel(long pingMs) {
        if (pingMs < 0L) {
            return 0;
        }
        if (pingMs < 80L) {
            return 4;
        }
        if (pingMs < 150L) {
            return 3;
        }
        if (pingMs < 300L) {
            return 2;
        }
        return 1;
    }

    public static int pingColor(UiFrame ui, long pingMs) {
        if (pingMs < 0L) {
            return ui.theme().layerActive();
        }
        if (pingMs < 150L) {
            return ui.theme().ok();
        }
        if (pingMs < 300L) {
            return Argb.rgb(226, 185, 61);
        }
        return ui.theme().danger();
    }

    public static void searchBox(UiFrame ui, float x, float y, float w, float h, boolean focused) {
        Theme theme = ui.theme();
        float r = h / 2f;
        strokeFill(ui.canvas(), x, y, w, h, r,
                focused ? theme.accent() : theme.stroke(),
                Argb.of(64, 0, 0, 0));
    }

    public static void navItem(UiFrame ui, float x, float y, float w, float h, boolean selected, boolean hover) {
        if (selected) {
            ui.canvas().fillRoundRect(x, y, w, h, h / 2f, ui.theme().accent());
        } else if (hover) {
            ui.canvas().fillRoundRect(x, y, w, h, h / 2f, ui.theme().layerHover());
        }
    }

    public static float badge(UiFrame ui, float x, float y, String text) {
        FontHandle font = ui.font(11);
        float tw = font.measure(text);
        float w = tw + 8f;
        float h = 10f;
        ui.canvas().fillRoundRect(x, y, w, h, h / 2f, ui.theme().accentSoft());
        ui.canvas().drawString(font, text, x + (w - tw) / 2f, textY(y, h, font),
                ui.theme().accentText());
        return w;
    }

    public static void keyChip(UiFrame ui, float x, float y, float w, float h, String text,
                               boolean active, boolean hover) {
        Theme theme = ui.theme();
        int border = active ? theme.accent() : theme.strokeStrong();
        if (hover && !active) {
            border = theme.strokeStrong();
        }
        strokeFill(ui.canvas(), x, y, w, h, 3f, border, Argb.of(77, 0, 0, 0));
        FontHandle font = ui.font(11);
        float tw = font.measure(text);
        int color = active ? theme.accentText() : theme.textSecondary();
        ui.canvas().drawString(font, text, x + (w - tw) / 2f, textY(y, h, font), color);
    }

    private static void drawSwitchSized(UiFrame ui, float x, float y, float w, float h, boolean on, float knobT) {
        float t = clamp01(knobT);
        int track = Argb.lerp(ui.theme().toggleOff(), ui.theme().accent(), t);
        ui.canvas().fillRoundRect(x, y, w, h, h / 2f, track);
        float knob = h - 3f;
        float kx = x + 1.5f + (w - knob - 3f) * t;
        int knobAlpha = 217 + (int) (38 * t);
        ui.canvas().fillRoundRect(kx, y + 1.5f, knob, knob, knob / 2f, Argb.of(knobAlpha, 255, 255, 255));
    }

    /** Top of a string whose box of {@link FontHandle#lineHeight()} is vertically centered in {@code h}. */
    public static float textY(float y, float h, FontHandle font) {
        return y + (h - font.lineHeight()) * 0.5f;
    }

    private static void strokeFill(Canvas canvas, float x, float y, float w, float h, float radius,
                                   int stroke, int fill) {
        canvas.fillRoundRect(x - 0.5f, y - 0.5f, w + 1f, h + 1f, radius + 1f, stroke);
        canvas.fillRoundRect(x, y, w, h, radius, fill);
    }

    private static float clamp01(float t) {
        if (t < 0f) {
            return 0f;
        }
        if (t > 1f) {
            return 1f;
        }
        return t;
    }
}

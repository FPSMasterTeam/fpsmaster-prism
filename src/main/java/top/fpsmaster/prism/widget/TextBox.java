package top.fpsmaster.prism.widget;

import top.fpsmaster.prism.anim.Anim;
import top.fpsmaster.prism.canvas.FontHandle;
import top.fpsmaster.prism.input.Keys;
import top.fpsmaster.prism.theme.Metrics;

/**
 * Immediate-mode single-line field. Hosts feed unicode via {@code typedChars()} and special keys
 * via {@link Keys}.
 */
public final class TextBox {
    private String text;
    private boolean focused;
    private String placeholder = "";
    private int fontSize = 14;
    private boolean paintBox = true;
    private float padLeft = 4f;
    private final Object id = new Object();
    private float focusT;
    private float hoverT;
    private long lastNanos;

    public TextBox() {
        this("");
    }

    public TextBox(String text) {
        this.text = text == null ? "" : text;
    }

    public void setPlaceholder(String value) {
        this.placeholder = value == null ? "" : value;
    }

    public void setFontSize(int size) {
        this.fontSize = size < 8 ? 8 : size;
    }

    public void setPaintBox(boolean paintBox) {
        this.paintBox = paintBox;
    }

    public void setPadLeft(float padLeft) {
        this.padLeft = padLeft;
    }

    public String text() {
        return text;
    }

    public void setText(String value) {
        this.text = value == null ? "" : value;
    }

    public boolean focused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    public void draw(UiFrame ui, float x, float y, float w, float h) {
        long now = ui.host().nowNanos();
        float dt = lastNanos == 0L ? 0.016f : Math.min(0.05f, Math.max(0f, (now - lastNanos) / 1_000_000_000f));
        lastNanos = now;
        if (ui.clicked(x, y, w, h)) {
            focused = true;
        } else if (focused && ui.input().hasPressOutside(x, y, w, h)) {
            // Drop focus but leave the press for later widgets (module rows, toggles).
            focused = false;
        }
        if (focused) {
            String typed = ui.input().typedChars();
            if (typed != null && !typed.isEmpty()) {
                text = text + typed;
            }
            if (ui.input().consumeKey(Keys.BACKSPACE) && !text.isEmpty()) {
                text = text.substring(0, text.length() - 1);
            }
        }
        focusT = Anim.approach(focusT, focused ? 1f : 0f, 0.24f, dt);
        hoverT = Anim.approach(hoverT, ui.hovered(x, y, w, h) ? 1f : 0f, 0.18f, dt);
        if (paintBox) {
            Chrome.inputBox(ui, x, y, w, h, focusT, hoverT);
        }
        FontHandle font = ui.font(fontSize);
        boolean empty = text.isEmpty();
        String shown = empty ? placeholder : text;
        int color = empty ? ui.theme().textDisabled() : ui.theme().textPrimary();
        ui.canvas().pushClip(x + 3f, y, w - 6f, h);
        ui.canvas().drawString(font, shown, x + padLeft, Chrome.textY(y, h, font), color);
        if (focused && !empty) {
            float caretX = Math.min(x + w - 4f, x + padLeft + font.measure(text) + 1f);
            float pulse = (float) ((Math.sin(now / 180_000_000d) + 1d) * 0.5d);
            ui.canvas().fillRoundRect(caretX, y + 4f, 0.75f, Math.max(3f, h - 8f), 0.4f,
                    top.fpsmaster.prism.theme.Argb.lerp(ui.theme().accentSoft(), ui.theme().accent(), pulse));
        }
        ui.canvas().popClip();
        ui.input().markHovered(id, x, y, w, h);
    }

    public static float height() {
        return Metrics.INPUT_H;
    }
}

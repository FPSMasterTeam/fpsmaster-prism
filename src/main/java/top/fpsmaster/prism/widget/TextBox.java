package top.fpsmaster.prism.widget;

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
        if (paintBox) {
            Chrome.inputBox(ui, x, y, w, h, focused);
        }
        FontHandle font = ui.font(fontSize);
        boolean empty = text.isEmpty();
        String shown = empty ? placeholder : text;
        int color = empty ? ui.theme().textDisabled() : ui.theme().textPrimary();
        ui.canvas().pushClip(x + 3f, y, w - 6f, h);
        ui.canvas().drawString(font, shown, x + padLeft, Chrome.textY(y, h, font), color);
        ui.canvas().popClip();
        ui.input().markHovered(id, x, y, w, h);
    }

    public static float height() {
        return Metrics.INPUT_H;
    }
}

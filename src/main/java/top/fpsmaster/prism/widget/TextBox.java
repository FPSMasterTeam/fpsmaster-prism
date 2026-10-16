package top.fpsmaster.prism.widget;

import top.fpsmaster.prism.anim.Anim;
import top.fpsmaster.prism.canvas.FontHandle;
import top.fpsmaster.prism.input.Input;
import top.fpsmaster.prism.input.Keys;
import top.fpsmaster.prism.theme.Argb;
import top.fpsmaster.prism.theme.Metrics;

/**
 * Immediate-mode single-line field with a caret and selection.
 *
 * <p>Hosts feed committed text (keyboard and IME) through {@code type()} and editing keys through
 * {@link Keys}: BACKSPACE, DELETE, LEFT, RIGHT, HOME, END, SELECT_ALL, COPY, CUT, PASTE, plus the
 * SHIFT modifier held via {@code setKeyDown}. Copy/paste go through {@link Input#clipboard()} and
 * {@link Input#setClipboard}, which hosts back with the system clipboard. Masked fields never put
 * their content on the clipboard.
 */
public final class TextBox {
    private String text;
    private boolean focused;
    private String placeholder = "";
    private int fontSize = 14;
    private boolean paintBox = true;
    private boolean masked;
    private float padLeft = 4f;
    private int maxLength = Integer.MAX_VALUE;
    private final Object id = new Object();
    private float focusT;
    private float hoverT;
    private long lastNanos;
    /** Caret and selection anchor as char indices into {@link #text}; selection is between them. */
    private int caret;
    private int anchor;
    private float scrollX;
    private boolean changed;
    private String masks = "";

    public TextBox() {
        this("");
    }

    public TextBox(String text) {
        this.text = text == null ? "" : text;
        this.caret = this.text.length();
        this.anchor = caret;
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

    /** Renders bullets instead of the characters. {@link #text()} still returns the real value. */
    public void setMasked(boolean masked) {
        this.masked = masked;
    }

    public void setPadLeft(float padLeft) {
        this.padLeft = padLeft;
    }

    /** Maximum length in chars; typing and pasting stop there. */
    public void setMaxLength(int maxLength) {
        this.maxLength = Math.max(0, maxLength);
        if (text.length() > this.maxLength) {
            setText(text.substring(0, this.maxLength));
        }
    }

    public String text() {
        return text;
    }

    /** Replace the content (not a user edit: {@link #changed()} stays false). Caret goes to the end. */
    public void setText(String value) {
        String next = value == null ? "" : value;
        if (next.length() > maxLength) {
            next = next.substring(0, maxLength);
        }
        if (next.equals(text)) {
            return;
        }
        this.text = next;
        caret = text.length();
        anchor = caret;
    }

    public boolean focused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        if (focused && !this.focused) {
            caret = text.length();
            anchor = caret;
        }
        this.focused = focused;
    }

    /**
     * {@code true} when the user edited the text during the last {@link #draw}. Owners mirroring an
     * external value write back only then, and otherwise adopt the external value — see
     * {@code SharedClickGui}.
     */
    public boolean changed() {
        return changed;
    }

    /** Selected text, empty when nothing is selected. */
    public String selectedText() {
        return text.substring(selStart(), selEnd());
    }

    public void draw(UiFrame ui, float x, float y, float w, float h) {
        long now = ui.host().nowNanos();
        float dt = lastNanos == 0L ? 0.016f : Math.min(0.05f, Math.max(0f, (now - lastNanos) / 1_000_000_000f));
        lastNanos = now;
        changed = false;
        FontHandle font = ui.font(fontSize);
        String visible = masked ? masks(text.length()) : text;
        if (ui.clicked(x, y, w, h)) {
            boolean extend = focused && ui.input().isKeyDown(Keys.SHIFT);
            focused = true;
            caret = indexAt(font, visible, ui.localMouseX() - (x + padLeft) + scrollX);
            if (!extend) {
                anchor = caret;
            }
        } else if (focused && ui.hasPressOutside(x, y, w, h)) {
            // Drop focus but leave the press for later widgets (module rows, toggles).
            focused = false;
            anchor = caret;
        }
        if (focused && ui.inputEnabled()) {
            edit(ui.input());
            visible = masked ? masks(text.length()) : text;
        }
        focusT = Anim.approach(focusT, focused ? 1f : 0f, 0.24f, dt);
        hoverT = Anim.approach(hoverT, ui.hovered(x, y, w, h) ? 1f : 0f, 0.18f, dt);
        if (paintBox) {
            Chrome.inputBox(ui, x, y, w, h, focusT, hoverT);
        }
        boolean empty = text.isEmpty();
        // 遮蔽只影响绘制，光标与选区也按遮蔽后的串量宽，否则密码框的光标会停在实际字宽处。
        float caretPx = TextFit.width(font, visible.substring(0, caret));
        float innerW = Math.max(1f, w - padLeft - 4f);
        float totalPx = TextFit.width(font, visible);
        if (caretPx - scrollX > innerW) {
            scrollX = caretPx - innerW;
        } else if (caretPx < scrollX) {
            scrollX = caretPx;
        }
        scrollX = Math.max(0f, Math.min(scrollX, Math.max(0f, totalPx - innerW)));
        float textX = x + padLeft - scrollX;
        ui.pushClip(x + 3f, y, w - 6f, h);
        try {
            if (focused && caret != anchor) {
                float a = TextFit.width(font, visible.substring(0, selStart()));
                float b = TextFit.width(font, visible.substring(0, selEnd()));
                ui.canvas().fillRect(textX + a, y + 3f, b - a, Math.max(3f, h - 6f), ui.theme().accentSoft());
            }
            int color = empty ? ui.theme().textDisabled() : ui.theme().textPrimary();
            ui.canvas().drawString(font, empty ? placeholder : visible, empty ? x + padLeft : textX,
                    Chrome.textY(y, h, font), color);
            if (focused) {
                float caretX = Math.min(x + w - 4f, textX + caretPx + (empty ? 0f : 1f));
                float pulse = (float) ((Math.sin(now / 180_000_000d) + 1d) * 0.5d);
                ui.canvas().fillRoundRect(caretX, y + 4f, 0.75f, Math.max(3f, h - 8f), 0.4f,
                        Argb.lerp(ui.theme().accentSoft(), ui.theme().accent(), pulse));
            }
        } finally {
            ui.popClip();
        }
        ui.markHovered(id, x, y, w, h);
    }

    private void edit(Input in) {
        boolean shift = in.isKeyDown(Keys.SHIFT);
        // 消费式：同一帧里两个输入框都拿到焦点时（宿主一帧塞进两次按下），
        // 非消费式的 typedChars() 会把同一批字符交给两个框，玩家在账号里输的字
        // 会同时长在密码框里。取走之后「同帧第二个框再吃一遍」结构上不可达。
        String typed = in.consumeTypedChars();
        if (typed != null && !typed.isEmpty()) {
            insert(sanitize(typed));
        }
        if (in.consumeKey(Keys.SELECT_ALL)) {
            anchor = 0;
            caret = text.length();
        }
        if (in.consumeKey(Keys.COPY) && !masked && caret != anchor) {
            in.setClipboard(selectedText());
        }
        if (in.consumeKey(Keys.CUT) && !masked && caret != anchor) {
            in.setClipboard(selectedText());
            insert("");
        }
        if (in.consumeKey(Keys.PASTE)) {
            String clip = in.clipboard();
            if (clip != null && !clip.isEmpty()) {
                insert(sanitize(clip));
            }
        }
        if (in.consumeKey(Keys.BACKSPACE)) {
            if (caret != anchor) {
                insert("");
            } else if (caret > 0) {
                anchor = prev(caret);
                insert("");
            }
        }
        if (in.consumeKey(Keys.DELETE)) {
            if (caret != anchor) {
                insert("");
            } else if (caret < text.length()) {
                anchor = next(caret);
                insert("");
            }
        }
        if (in.consumeKey(Keys.LEFT)) {
            caret = !shift && caret != anchor ? selStart() : prev(caret);
            if (!shift) {
                anchor = caret;
            }
        }
        if (in.consumeKey(Keys.RIGHT)) {
            caret = !shift && caret != anchor ? selEnd() : next(caret);
            if (!shift) {
                anchor = caret;
            }
        }
        if (in.consumeKey(Keys.HOME)) {
            caret = 0;
            if (!shift) {
                anchor = caret;
            }
        }
        if (in.consumeKey(Keys.END)) {
            caret = text.length();
            if (!shift) {
                anchor = caret;
            }
        }
    }

    /** Replace the selection (or insert at the caret) with {@code value}, honouring maxLength. */
    private void insert(String value) {
        int start = selStart();
        int end = selEnd();
        int room = maxLength - (text.length() - (end - start));
        if (value.length() > room) {
            int cut = Math.max(0, room);
            if (cut > 0 && Character.isHighSurrogate(value.charAt(cut - 1))) {
                cut--;
            }
            value = value.substring(0, cut);
        }
        if (start == end && value.isEmpty()) {
            return;
        }
        text = text.substring(0, start) + value + text.substring(end);
        caret = start + value.length();
        anchor = caret;
        changed = true;
    }

    /**
     * Single-line field: drop line breaks and other control characters (hosts may forward them as
     * typed chars, and pasted cookies often end with a newline); tabs become spaces.
     */
    private static String sanitize(String raw) {
        StringBuilder out = null;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            boolean control = c < 0x20 || c == 0x7F;
            if (control && out == null) {
                out = new StringBuilder(raw.length());
                out.append(raw, 0, i);
            }
            if (out != null) {
                if (c == '\t') {
                    out.append(' ');
                } else if (!control) {
                    out.append(c);
                }
            }
        }
        return out == null ? raw : out.toString();
    }

    private int selStart() {
        return Math.min(caret, anchor);
    }

    private int selEnd() {
        return Math.max(caret, anchor);
    }

    private int prev(int index) {
        if (index <= 0) {
            return 0;
        }
        int i = index - 1;
        if (i > 0 && Character.isLowSurrogate(text.charAt(i)) && Character.isHighSurrogate(text.charAt(i - 1))) {
            i--;
        }
        return i;
    }

    private int next(int index) {
        if (index >= text.length()) {
            return text.length();
        }
        int i = index + 1;
        if (i < text.length() && Character.isLowSurrogate(text.charAt(i)) && Character.isHighSurrogate(text.charAt(i - 1))) {
            i++;
        }
        return i;
    }

    /** Char boundary closest to {@code px} pixels from the text start. */
    private int indexAt(FontHandle font, String visible, float px) {
        if (px <= 0f || visible.isEmpty()) {
            return 0;
        }
        int best = 0;
        float bestDist = Math.abs(px);
        for (int i = next(0); ; i = next(i)) {
            float d = Math.abs(TextFit.width(font, visible.substring(0, i)) - px);
            if (d < bestDist) {
                best = i;
                bestDist = d;
            }
            if (i >= visible.length()) {
                break;
            }
        }
        return best;
    }

    private String masks(int count) {
        if (masks.length() != count) {
            StringBuilder sb = new StringBuilder(count);
            for (int i = 0; i < count; i++) {
                sb.append('•');
            }
            masks = sb.toString();
        }
        return masks;
    }

    public static float height() {
        return Metrics.INPUT_H;
    }
}

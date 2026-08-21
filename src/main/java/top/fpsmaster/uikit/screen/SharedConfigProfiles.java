package top.fpsmaster.uikit.screen;

import top.fpsmaster.uikit.icon.GlyphIcons;
import top.fpsmaster.uikit.theme.Metrics;
import top.fpsmaster.uikit.widget.Chrome;
import top.fpsmaster.uikit.widget.Scroll;
import top.fpsmaster.uikit.widget.TextBox;
import top.fpsmaster.uikit.widget.UiFrame;

import java.util.List;

public final class SharedConfigProfiles {
    private enum Dialog {
        NONE, LOAD, DELETE, DEFAULTS, CREATE, RENAME
    }

    private final Scroll scroll = new Scroll("config.profiles");
    private final TextBox nameBox = new TextBox();
    private Dialog dialog = Dialog.NONE;
    private String dialogName = "";
    private String status = "";
    private int statusColor;
    private long statusUntil;

    public SharedConfigProfiles() {
        nameBox.setFontSize(12);
    }

    public boolean draw(UiFrame ui, ConfigProfilesBridge bridge) {
        float gw = ui.host().width();
        float gh = ui.host().height();
        Chrome.veil(ui, 1f);
        float pw = Math.min(400f, Math.max(280f, gw - 24f));
        float ph = Math.min(250f, Math.max(180f, gh - 32f));
        float px = (gw - pw) / 2f;
        float py = (gh - ph) / 2f;
        Chrome.panel(ui, px, py, pw, ph);

        float leftW = 140f;
        ui.canvas().fillRect(px + 1, py + 1, leftW - 1, ph - 2, ui.theme().layer());
        Chrome.hairlineV(ui, px + leftW, py + 1, ph - 2);

        if (back(ui, px + 7f, py + 7f, bridge.i18n("configprofiles.back"))) {
            return true;
        }
        drawCurrent(ui, bridge, px, py, leftW, ph);
        drawAll(ui, bridge, px + leftW, py, pw - leftW, ph);
        if (dialog != Dialog.NONE) {
            drawDialog(ui, bridge, px, py, pw, ph);
        }
        return false;
    }

    private boolean back(UiFrame ui, float x, float y, String label) {
        float w = 36f;
        float h = 14f;
        boolean hover = ui.hovered(x, y, w, h);
        Chrome.ghostButton(ui, x, y, w, h, hover);
        GlyphIcons.draw(ui, "back", x + 3f, y + 3.5f, 7f, ui.theme().textSecondary());
        ui.canvas().drawString(ui.font(11), label, x + 12f, Chrome.textY(y, h, ui.font(11)),
                hover ? ui.theme().textPrimary() : ui.theme().textSecondary());
        return ui.clicked(x, y, w, h);
    }

    private void drawCurrent(UiFrame ui, ConfigProfilesBridge bridge, float px, float py, float leftW, float ph) {
        float colX = px + 13f;
        float colW = leftW - 26f;
        ui.canvas().drawString(ui.font(11), bridge.i18n("configprofiles.current.label"),
                colX, py + 28f, ui.theme().accentText());
        String name = bridge.activeName();
        String letter = name.isEmpty() ? "F" : name.substring(0, 1).toUpperCase();
        ui.canvas().fillRoundRect(colX, py + 38f, 32f, 32f, 9f, ui.theme().accent());
        float lw = ui.font(22).measure(letter);
        FontBold.draw(ui, 22, letter, colX + (32f - lw) * 0.5f, Chrome.textY(py + 38f, 32f, ui.font(22)),
                ui.theme().white());
        FontBold.draw(ui, 16, ellipsize(ui.font(16), name, colW), colX, py + 76f, ui.theme().textPrimary());
        ui.canvas().drawString(ui.font(11), relativeTime(bridge.activeModified()),
                colX, py + 88f, ui.theme().textSecondary());

        float factY = py + 104f;
        factY = fact(ui, colX, factY, colW, "box", bridge.i18n("configprofiles.modules.enabled"),
                String.valueOf(bridge.enabledModules()));
        factY = fact(ui, colX, factY, colW, "grid", bridge.i18n("configprofiles.hud"),
                String.valueOf(bridge.hudModules()));
        fact(ui, colX, factY, colW, "folder", bridge.i18n("configprofiles.size"), formatSize(bridge.activeBytes()));

        float btnH = Metrics.BTN_H;
        float resetY = py + ph - 12f - btnH;
        float exportY = resetY - 4f - btnH;
        if (dialog == Dialog.NONE && Chrome.button(ui, colX, exportY, colW, btnH,
                bridge.i18n("configprofiles.export.share"), Chrome.ButtonStyle.DEFAULT)) {
            setStatus(bridge.exportActive(), ui.theme().ok());
        }
        if (dialog == Dialog.NONE && Chrome.button(ui, colX, resetY, colW, btnH,
                bridge.i18n("configprofiles.preset.alloff"), Chrome.ButtonStyle.DANGER)) {
            dialog = Dialog.DEFAULTS;
            dialogName = "";
        }
        if (!status.isEmpty() && System.currentTimeMillis() < statusUntil) {
            ui.canvas().drawString(ui.font(10), ellipsize(ui.font(10), status, colW),
                    colX, exportY - 12f, statusColor);
        }
    }

    private float fact(UiFrame ui, float x, float y, float w, String icon, String key, String value) {
        GlyphIcons.draw(ui, icon, x, y + 4f, 7f, ui.theme().textDisabled());
        ui.canvas().drawString(ui.font(12), key, x + 11f, y + 4f, ui.theme().textSecondary());
        float vw = ui.font(12).measure(value);
        ui.canvas().drawString(ui.font(12), value, x + w - vw, y + 4f, ui.theme().textPrimary());
        Chrome.hairlineH(ui, x, y + 15f, w);
        return y + 17f;
    }

    private void drawAll(UiFrame ui, ConfigProfilesBridge bridge, float x, float y, float w, float h) {
        FontBold.draw(ui, 16, bridge.i18n("configprofiles.all"), x + 11f, y + 12f, ui.theme().textPrimary());
        String count = String.format(bridge.i18n("configprofiles.count"),
                Integer.valueOf(bridge.profiles().size()));
        ui.canvas().drawString(ui.font(12), count,
                x + 11f + ui.font(16).measure(bridge.i18n("configprofiles.all")) + 5f,
                y + 13.5f, ui.theme().textDisabled());
        float importW = Math.max(44f, ui.font(12).measure(bridge.i18n("configprofiles.importfile")) + 16f);
        if (dialog == Dialog.NONE && Chrome.button(ui, x + w - 11f - importW, y + 9f, importW, 16f,
                bridge.i18n("configprofiles.importfile"), Chrome.ButtonStyle.DEFAULT)) {
            setStatus(bridge.importFile(), ui.theme().ok());
        }
        float footH = 15f;
        Chrome.hairlineH(ui, x + 1f, y + h - footH, w - 2f);
        ui.canvas().drawString(ui.font(11), bridge.i18n("configprofiles.foot"),
                x + 11f, y + h - footH + 4.5f, ui.theme().textDisabled());

        List<ConfigProfilesBridge.Profile> profiles = bridge.profiles();
        float gridX = x + 11f;
        float gridY = y + 30f;
        float gridW = w - 22f;
        float gridH = h - 38f - footH;
        int cols = gridW > 180f ? 2 : 1;
        float gap = 5f;
        float cardH = 59f;
        float cardW = (gridW - gap * (cols - 1)) / cols;
        int countCards = profiles.size() + 1;
        float contentH = ((countCards + cols - 1) / cols) * (cardH + gap);
        float off = scroll.begin(ui, gridX, gridY, gridW, gridH, contentH);
        for (int i = 0; i < countCards; i++) {
            int col = i % cols;
            int row = i / cols;
            float cx = gridX + col * (cardW + gap);
            float cy = gridY + row * (cardH + gap) + off;
            if (i < profiles.size()) {
                drawCard(ui, bridge, profiles.get(i), cx, cy, cardW, cardH);
            } else {
                drawNew(ui, bridge, cx, cy, cardW, cardH);
            }
        }
        scroll.end(ui);
    }

    private void drawCard(UiFrame ui, ConfigProfilesBridge bridge, ConfigProfilesBridge.Profile profile,
                          float x, float y, float w, float h) {
        boolean active = profile.name.equals(bridge.activeName());
        boolean hover = dialog == Dialog.NONE && ui.hovered(x, y, w, h);
        if (active) {
            Chrome.selectedSurface(ui, x, y, w, h, Metrics.PANEL_RADIUS);
        } else {
            Chrome.card(ui, x, y, w, h, hover, false);
        }
        String letter = profile.name.isEmpty() ? "P" : profile.name.substring(0, 1).toUpperCase();
        ui.canvas().fillRoundRect(x + 8f, y + 8f, 20f, 20f, 6f,
                active ? ui.theme().accent() : ui.theme().layerActive());
        float lw = ui.font(14).measure(letter);
        FontBold.draw(ui, 14, letter, x + 8f + (20f - lw) * 0.5f, Chrome.textY(y + 8f, 20f, ui.font(14)),
                ui.theme().white());
        ui.canvas().drawString(ui.font(13), ellipsize(ui.font(13), profile.name, w - 76f),
                x + 34f, y + 9f, ui.theme().textPrimary());
        ui.canvas().drawString(ui.font(11), relativeTime(profile.modified),
                x + 34f, y + 19f, ui.theme().textDisabled());
        if (hover) {
            float opX = x + w - 20f;
            if (!bridge.isDefault(profile.name)
                    && iconBtn(ui, "delete", opX, y + 6f, true)) {
                dialog = Dialog.DELETE;
                dialogName = profile.name;
                return;
            }
            if (!bridge.isDefault(profile.name)
                    && iconBtn(ui, "rename", opX - 16f, y + 6f, false)) {
                dialog = Dialog.RENAME;
                dialogName = profile.name;
                nameBox.setText(profile.name);
                nameBox.setFocused(true);
                return;
            }
        }
        if (active) {
            GlyphIcons.draw(ui, "check", x + 8f, y + h - 17f, 6.5f, ui.theme().accentText());
            ui.canvas().drawString(ui.font(12), bridge.i18n("configprofiles.inuse"),
                    x + 17.5f, y + h - 16.5f, ui.theme().accentText());
        } else {
            float applyW = w - 16f;
            float applyH = 16f;
            float applyX = x + 8f;
            float applyY = y + h - applyH - 8f;
            if (Chrome.button(ui, applyX, applyY, applyW, applyH,
                    bridge.i18n("configprofiles.apply"), Chrome.ButtonStyle.DEFAULT)) {
                dialog = Dialog.LOAD;
                dialogName = profile.name;
            }
        }
    }

    private void drawNew(UiFrame ui, ConfigProfilesBridge bridge, float x, float y, float w, float h) {
        boolean hover = dialog == Dialog.NONE && ui.hovered(x, y, w, h);
        Chrome.card(ui, x, y, w, h, hover, false);
        int color = hover ? ui.theme().textPrimary() : ui.theme().textSecondary();
        String label = bridge.i18n("configprofiles.new");
        float labelW = ui.font(13).measure(label);
        float cx = x + (w - labelW - 11f) / 2f;
        GlyphIcons.draw(ui, "plus", cx, y + h / 2f - 3.5f, 7f, color);
        ui.canvas().drawString(ui.font(13), label, cx + 11f, y + h / 2f - 3f, color);
        if (dialog == Dialog.NONE && ui.clicked(x, y, w, h)) {
            dialog = Dialog.CREATE;
            dialogName = "";
            nameBox.setText("profile");
            nameBox.setFocused(true);
        }
    }

    private boolean iconBtn(UiFrame ui, String icon, float x, float y, boolean danger) {
        float s = 14f;
        boolean hover = ui.hovered(x, y, s, s);
        ui.canvas().fillRoundRect(x, y, s, s, 4f,
                hover ? (danger ? ui.theme().danger() : ui.theme().layerHover()) : ui.theme().layer());
        GlyphIcons.draw(ui, icon, x + 3.5f, y + 3.5f, 7f,
                hover && danger ? ui.theme().white() : ui.theme().textPrimary());
        return ui.clicked(x, y, s, s);
    }

    private void drawDialog(UiFrame ui, ConfigProfilesBridge bridge, float px, float py, float pw, float ph) {
        ui.canvas().fillRect(px, py, pw, ph, 0x99000000);
        float dw = 220f;
        float dh = dialog == Dialog.RENAME || dialog == Dialog.CREATE ? 92f : 72f;
        float dx = px + (pw - dw) / 2f;
        float dy = py + (ph - dh) / 2f;
        Chrome.panel(ui, dx, dy, dw, dh, 8f);
        String title;
        switch (dialog) {
            case LOAD:
                title = String.format(bridge.i18n("configprofiles.confirm.load"), dialogName);
                break;
            case DELETE:
                title = String.format(bridge.i18n("configprofiles.confirm.delete"), dialogName);
                break;
            case DEFAULTS:
                title = bridge.i18n("configprofiles.confirm.alloff");
                break;
            case CREATE:
                title = bridge.i18n("configprofiles.new");
                break;
            default:
                title = bridge.i18n("configprofiles.rename.title");
                break;
        }
        ui.canvas().drawString(ui.font(13), ellipsize(ui.font(13), title, dw - 20f),
                dx + 10f, dy + 10f, ui.theme().textPrimary());
        if (dialog == Dialog.RENAME || dialog == Dialog.CREATE) {
            nameBox.draw(ui, dx + 10f, dy + 28f, dw - 20f, 18f);
        }
        float bw = 56f;
        float bh = 16f;
        float by = dy + dh - 12f - bh;
        if (Chrome.button(ui, dx + dw - 10f - bw, by, bw, bh,
                bridge.i18n("configprofiles.confirm"),
                dialog == Dialog.DELETE || dialog == Dialog.DEFAULTS
                        ? Chrome.ButtonStyle.DANGER_FILL : Chrome.ButtonStyle.PRIMARY)) {
            runDialog(bridge, ui);
        }
        if (Chrome.button(ui, dx + dw - 16f - bw * 2f, by, bw, bh,
                bridge.i18n("configprofiles.cancel"), Chrome.ButtonStyle.GHOST)) {
            dialog = Dialog.NONE;
        }
    }

    private void runDialog(ConfigProfilesBridge bridge, UiFrame ui) {
        String msg;
        switch (dialog) {
            case LOAD:
                msg = bridge.load(dialogName);
                break;
            case DELETE:
                msg = bridge.delete(dialogName);
                break;
            case DEFAULTS:
                msg = bridge.resetAllOff();
                break;
            case CREATE:
                msg = bridge.create(nameBox.text());
                break;
            default:
                msg = bridge.rename(dialogName, nameBox.text());
                break;
        }
        setStatus(msg, msg.contains("失败") || msg.toLowerCase().contains("fail")
                ? ui.theme().danger() : ui.theme().ok());
        dialog = Dialog.NONE;
    }

    private void setStatus(String text, int color) {
        status = text == null ? "" : text;
        statusColor = color;
        statusUntil = System.currentTimeMillis() + 4000L;
    }

    private static String relativeTime(long ms) {
        if (ms <= 0L) {
            return "";
        }
        long ago = Math.max(0L, System.currentTimeMillis() - ms) / 1000L;
        if (ago < 60L) {
            return ago + "s";
        }
        if (ago < 3600L) {
            return (ago / 60L) + "m";
        }
        if (ago < 86400L) {
            return (ago / 3600L) + "h";
        }
        return (ago / 86400L) + "d";
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        if (bytes < 1024L * 1024L) {
            return (bytes / 1024L) + " KB";
        }
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private static String ellipsize(top.fpsmaster.uikit.canvas.FontHandle font, String text, float max) {
        if (text == null) {
            return "";
        }
        if (font.measure(text) <= max) {
            return text;
        }
        for (int i = text.length() - 1; i > 0; i--) {
            String cut = text.substring(0, i) + "...";
            if (font.measure(cut) <= max) {
                return cut;
            }
        }
        return "...";
    }
}

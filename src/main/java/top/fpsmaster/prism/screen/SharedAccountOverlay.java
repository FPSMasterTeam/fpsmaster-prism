package top.fpsmaster.prism.screen;

import top.fpsmaster.prism.anim.Anim;
import top.fpsmaster.prism.canvas.FontHandle;
import top.fpsmaster.prism.icon.GlyphIcons;
import top.fpsmaster.prism.input.Keys;
import top.fpsmaster.prism.theme.Argb;
import top.fpsmaster.prism.theme.Metrics;
import top.fpsmaster.prism.widget.Chrome;
import top.fpsmaster.prism.widget.TextBox;
import top.fpsmaster.prism.widget.UiFrame;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Account chip popover plus offline / Microsoft device-code dialogs.
 * Hosts own this object so Edge can keep its existing overlay.
 */
public final class SharedAccountOverlay {
    private enum Dialog {
        NONE, OFFLINE, MICROSOFT
    }

    private static final float HOVER_SPEED = 0.32f;

    private boolean popOpen;
    private Dialog dialog = Dialog.NONE;
    private final TextBox username = new TextBox();
    private boolean usernameInvalid;
    private final Map<String, Float> hover = new HashMap<String, Float>();
    private float popT;
    private long lastNanos;

    public SharedAccountOverlay() {
        username.setFontSize(12);
        username.setPlaceholder("");
    }

    public boolean popOpen() {
        return popOpen;
    }

    public boolean blocking() {
        return dialog != Dialog.NONE;
    }

    public void togglePop() {
        if (dialog != Dialog.NONE) {
            return;
        }
        popOpen = !popOpen;
    }

    public void close() {
        if (dialog != Dialog.NONE) {
            closeDialog();
            return;
        }
        popOpen = false;
    }

    public void openOffline() {
        dialog = Dialog.OFFLINE;
        username.setText("");
        username.setFocused(true);
        usernameInvalid = false;
        popOpen = false;
    }

    public void openMicrosoft() {
        dialog = Dialog.MICROSOFT;
        popOpen = false;
    }

    public void closeDialog() {
        dialog = Dialog.NONE;
        username.setFocused(false);
    }

    private void dismissDialog(MenuBridge bridge) {
        if (dialog == Dialog.MICROSOFT) {
            bridge.cancelMicrosoftLogin();
        }
        closeDialog();
    }

    public void draw(UiFrame ui, MenuBridge bridge) {
        if (ui.input().consumeKey(Keys.ESCAPE)) {
            if (dialog == Dialog.MICROSOFT) {
                bridge.cancelMicrosoftLogin();
            }
            close();
        }
        if (dialog == Dialog.OFFLINE) {
            drawOffline(ui, bridge);
            return;
        }
        if (dialog == Dialog.MICROSOFT) {
            drawMicrosoft(ui, bridge);
            return;
        }
        float dt = dt(ui);
        popT = Anim.approach(popT, popOpen ? 1f : 0f, HOVER_SPEED, dt);
        if (popT > 0.01f) {
            drawPop(ui, bridge, dt);
        }
    }

    private float dt(UiFrame ui) {
        long now = ui.host().nowNanos();
        float dt = lastNanos == 0L ? 0.016f : (now - lastNanos) / 1_000_000_000f;
        lastNanos = now;
        if (dt < 0.001f || dt > 0.1f) {
            dt = 0.016f;
        }
        return dt;
    }

    private float hoverT(String id, boolean on, float dt) {
        float current = hover.containsKey(id) ? hover.get(id).floatValue() : 0f;
        float next = Anim.approach(current, on ? 1f : 0f, HOVER_SPEED, dt);
        hover.put(id, Float.valueOf(next));
        return next;
    }

    private void drawPop(UiFrame ui, MenuBridge bridge, float dt) {
        List<MenuBridge.AccountRow> rows = bridge.accounts();
        float x = SharedMainMenu.CHIP_X;
        float y = SharedMainMenu.CHIP_Y + SharedMainMenu.CHIP_H + 4f + (1f - popT) * 4f;
        float w = 148f;
        float rowH = 22f;
        boolean fpsRow = bridge.showFpsAccount();
        float h = 3f + rows.size() * rowH + 4.5f + rowH * 2f + 3f
                + (fpsRow ? 4.5f + rowH : 0f);
        ui.canvas().pushAlpha(Math.max(0.05f, popT));
        Chrome.panel(ui, x, y, w, h);
        float rowY = y + 3f;
        for (int i = 0; i < rows.size(); i++) {
            MenuBridge.AccountRow row = rows.get(i);
            drawRow(ui, bridge, x + 3f, rowY, w - 6f, rowH, row, dt);
            rowY += rowH;
        }
        Chrome.hairlineH(ui, x + 7f, rowY + 2f, w - 14f);
        rowY += 4.5f;
        if (drawAdd(ui, x, rowY, w, rowH, "ms", bridge.i18n("mainmenu.account.ms.add"), dt) && popOpen) {
            openMicrosoft();
            bridge.startMicrosoftLogin();
        }
        rowY += rowH;
        if (drawAdd(ui, x, rowY, w, rowH, "offline", bridge.i18n("mainmenu.account.offline.add"), dt) && popOpen) {
            openOffline();
        }
        if (fpsRow) {
            rowY += rowH;
            Chrome.hairlineH(ui, x + 7f, rowY + 2f, w - 14f);
            rowY += 4.5f;
            if (drawFpsRow(ui, bridge, x, rowY, w, rowH, dt) && popOpen) {
                popOpen = false;
                bridge.openFpsSignIn();
            }
        }
        ui.canvas().popAlpha();
        float chipW = SharedMainMenu.chipWidth(ui, bridge);
        float zoneW = Math.max(chipW, w) + 4f;
        if (popOpen && ui.input().consumePressOutside(x - 2f, SharedMainMenu.CHIP_Y - 2f,
                zoneW, SharedMainMenu.CHIP_H + 6f + h + 4f) != null) {
            popOpen = false;
        }
    }

    private void drawRow(UiFrame ui, MenuBridge bridge, float x, float y, float w, float h,
                         MenuBridge.AccountRow row, float dt) {
        boolean hover = ui.hovered(x, y, w, h);
        float t = hoverT("row." + row.id, hover, dt);
        if (t > 0.01f) {
            ui.canvas().fillRoundRect(x, y, w, h, 5f, Argb.mulAlpha(ui.theme().layerHover(), t));
        }
        drawFace(ui, x + 5f, y + 4f, 14f, row.name, row.current, bridge);
        ui.canvas().drawString(ui.font(13), row.name, x + 24f, y + 3f, ui.theme().textPrimary());
        ui.canvas().drawString(ui.font(10), row.typeLabel, x + 24f, y + 12f, ui.theme().textDisabled());
        float rightX = x + w - 16f;
        boolean removeHover = false;
        if (hover && row.removable) {
            removeHover = ui.hovered(rightX, y + 5f, 12f, 12f);
            if (removeHover) {
                ui.canvas().fillRoundRect(rightX - 1f, y + 4f, 14f, 14f, 4f, ui.theme().dangerSoft());
            }
            GlyphIcons.draw(ui, "delete", rightX + 0.5f, y + 6.5f, 9f,
                    removeHover ? ui.theme().danger() : ui.theme().textDisabled());
        } else if (row.current) {
            GlyphIcons.draw(ui, "check", rightX, y + 7f, 8f, ui.theme().accentText());
        }
        if (row.removable && removeHover && popOpen && ui.clicked(rightX, y + 5f, 12f, 12f)) {
            bridge.removeAccount(row.id);
            return;
        }
        if (popOpen && ui.clicked(x, y, w, h)) {
            bridge.selectAccount(row.id);
            popOpen = false;
        }
    }

    private boolean drawAdd(UiFrame ui, float x, float rowY, float w, float rowH, String id, String label, float dt) {
        boolean hover = ui.hovered(x + 3f, rowY, w - 6f, rowH);
        float t = hoverT("add." + id, hover, dt);
        if (t > 0.01f) {
            ui.canvas().fillRoundRect(x + 3f, rowY, w - 6f, rowH, 5f, Argb.mulAlpha(ui.theme().layerHover(), t));
        }
        float boxX = x + 8f;
        float boxY = rowY + 4f;
        ui.canvas().fillRoundRect(boxX - 0.5f, boxY - 0.5f, 15f, 15f, 5f, ui.theme().strokeStrong());
        ui.canvas().fillRoundRect(boxX, boxY, 14f, 14f, 4f, ui.theme().glass());
        GlyphIcons.draw(ui, "plus", boxX + 3.5f, boxY + 3.5f, 7f,
                hover ? ui.theme().textPrimary() : ui.theme().textSecondary());
        ui.canvas().drawString(ui.font(13), label, x + 27f, rowY + 7f,
                hover ? ui.theme().textPrimary() : ui.theme().textSecondary());
        return ui.clicked(x + 3f, rowY, w - 6f, rowH);
    }

    /**
     * 「FPSMaster 账号」那一行：已登录显示昵称，未登录是一条去登录界面的入口。它和上面的
     * Minecraft 账号列表是两码事，所以隔了一条分隔线。
     */
    private boolean drawFpsRow(UiFrame ui, MenuBridge bridge, float x, float rowY, float w, float rowH,
                               float dt) {
        boolean hover = ui.hovered(x + 3f, rowY, w - 6f, rowH);
        float t = hoverT("fps", hover, dt);
        if (t > 0.01f) {
            ui.canvas().fillRoundRect(x + 3f, rowY, w - 6f, rowH, 5f, Argb.mulAlpha(ui.theme().layerHover(), t));
        }
        boolean signedIn = bridge.fpsSignedIn();
        String name = bridge.fpsAccountName();
        if (name == null || name.isEmpty()) {
            name = bridge.i18n("signin.account.unknown");
        }
        float boxX = x + 8f;
        float boxY = rowY + 4f;
        ui.canvas().fillRoundRect(boxX, boxY, 14f, 14f, 4f,
                signedIn ? ui.theme().accent() : ui.theme().glass());
        GlyphIcons.draw(ui, signedIn ? "check" : "plus", boxX + 3.5f, boxY + 3.5f, 7f,
                signedIn ? ui.theme().accentText() : ui.theme().textSecondary());
        ui.canvas().drawString(ui.font(13),
                signedIn ? name : bridge.i18n("mainmenu.account.fps.signin"), x + 27f, rowY + 3f,
                hover ? ui.theme().textPrimary() : ui.theme().textSecondary());
        ui.canvas().drawString(ui.font(10), bridge.i18n("mainmenu.account.fps"), x + 27f, rowY + 12f,
                ui.theme().textDisabled());
        return ui.clicked(x + 3f, rowY, w - 6f, rowH);
    }

    private void drawFace(UiFrame ui, float x, float y, float size, String name, boolean current,
                          MenuBridge bridge) {
        if (current) {
            bridge.drawAvatar(ui, x, y, size);
            return;
        }
        int hue = Math.abs(name.hashCode()) % 360;
        float hf = hue / 360f;
        int rgb = java.awt.Color.HSBtoRGB(hf, 0.45f, 0.75f);
        ui.canvas().fillRoundRect(x, y, size, size, 4f, rgb);
        String letter = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
        FontHandle font = ui.font(11);
        ui.canvas().drawString(font, letter, x + (size - font.measure(letter)) / 2f, y + 3f, 0xFFFFFFFF);
    }

    private void drawOffline(UiFrame ui, MenuBridge bridge) {
        Chrome.veil(ui, 0.9f);
        float w = 190f;
        float h = 108f;
        float x = (ui.host().width() - w) / 2f;
        float y = (ui.host().height() - h) / 2f;
        Chrome.panel(ui, x, y, w, h);
        FontBold.draw(ui, 16, bridge.i18n("mainmenu.account.offline.title"), x + 13f, y + 12f,
                ui.theme().textPrimary());
        ui.canvas().drawString(ui.font(12), bridge.i18n("mainmenu.account.offline.desc"),
                x + 13f, y + 24f, ui.theme().textSecondary());
        float fieldY = y + 40f;
        username.draw(ui, x + 13f, fieldY, w - 26f, 20f);
        if (usernameInvalid) {
            ui.canvas().drawString(ui.font(11), bridge.i18n("mainmenu.account.invalid"),
                    x + 13f, fieldY + 24f, ui.theme().danger());
        }
        float btnY = y + h - 28f;
        float addW = 62f;
        float cancelW = 40f;
        if (Chrome.button(ui, x + w - 13f - addW, btnY, addW, Metrics.BTN_H,
                bridge.i18n("mainmenu.account.add"), Chrome.ButtonStyle.PRIMARY)
                || (username.focused() && ui.input().consumeKey(Keys.ENTER))) {
            if (bridge.addOffline(username.text().trim())) {
                closeDialog();
            } else {
                usernameInvalid = true;
            }
        }
        if (Chrome.button(ui, x + w - 13f - addW - 6f - cancelW, btnY, cancelW, Metrics.BTN_H,
                bridge.i18n("configprofiles.cancel"), Chrome.ButtonStyle.GHOST)) {
            dismissDialog(bridge);
        }
        if (ui.input().consumePressOutside(x, y, w, h) != null) {
            dismissDialog(bridge);
        }
    }

    private void drawMicrosoft(UiFrame ui, MenuBridge bridge) {
        Chrome.veil(ui, 0.9f);
        float w = 220f;
        float h = 132f;
        float x = (ui.host().width() - w) / 2f;
        float y = (ui.host().height() - h) / 2f;
        Chrome.panel(ui, x, y, w, h);
        FontBold.draw(ui, 16, bridge.i18n("mainmenu.account.ms.title"), x + 13f, y + 12f,
                ui.theme().textPrimary());
        ui.canvas().drawString(ui.font(12), bridge.i18n("mainmenu.account.ms.desc"),
                x + 13f, y + 24f, ui.theme().textSecondary());
        String code = bridge.microsoftCode();
        boolean hasCode = code != null && !code.isEmpty();
        if (!hasCode) {
            if (bridge.microsoftBusy()) {
                code = "····";
            } else if (bridge.microsoftHasUrl()) {
                code = bridge.i18n("mainmenu.account.ms.browser");
            } else {
                code = "—";
            }
        }
        float codeY = y + 42f;
        boolean codeHover = ui.hovered(x + 13f, codeY, w - 26f, 22f);
        ui.canvas().fillRoundRect(x + 13f, codeY, w - 26f, 22f, 5f,
                codeHover ? ui.theme().layerHover() : ui.theme().glass());
        FontHandle codeFont = ui.font(16);
        ui.canvas().drawString(codeFont, code, x + (w - codeFont.measure(code)) / 2f, codeY + 6f,
                ui.theme().textPrimary());
        if (hasCode && ui.clicked(x + 13f, codeY, w - 26f, 22f)) {
            bridge.copyMicrosoftCode();
        }
        String status = bridge.microsoftError();
        int statusColor = ui.theme().danger();
        if (status == null || status.isEmpty()) {
            status = bridge.microsoftStatus();
            statusColor = ui.theme().textSecondary();
        }
        if (status != null && !status.isEmpty()) {
            FontHandle statusFont = ui.font(11);
            float maxW = w - 26f;
            String shown = status;
            while (shown.length() > 8 && statusFont.measure(shown) > maxW) {
                shown = shown.substring(0, shown.length() - 1);
            }
            if (!shown.equals(status)) {
                shown = shown + "…";
            }
            ui.canvas().drawString(statusFont, shown, x + 13f, codeY + 26f, statusColor);
        }
        float btnY = y + h - 28f;
        float cancelW = 40f;
        float actionW = 72f;
        if (Chrome.button(ui, x + w - 13f - cancelW, btnY, cancelW, Metrics.BTN_H,
                bridge.i18n("configprofiles.cancel"), Chrome.ButtonStyle.GHOST)) {
            dismissDialog(bridge);
        }
        if (bridge.microsoftError() != null && !bridge.microsoftError().isEmpty()) {
            if (Chrome.button(ui, x + w - 13f - cancelW - 6f - actionW, btnY, actionW, Metrics.BTN_H,
                    bridge.i18n("mainmenu.account.ms.retry"), Chrome.ButtonStyle.PRIMARY)) {
                bridge.retryMicrosoftLogin();
            }
        } else if (bridge.microsoftHasUrl()) {
            if (Chrome.button(ui, x + w - 13f - cancelW - 6f - actionW, btnY, actionW, Metrics.BTN_H,
                    bridge.i18n("mainmenu.account.ms.open"), Chrome.ButtonStyle.PRIMARY)) {
                bridge.openMicrosoftUrl();
            }
        }
        if (ui.input().consumePressOutside(x, y, w, h) != null) {
            dismissDialog(bridge);
        }
    }
}

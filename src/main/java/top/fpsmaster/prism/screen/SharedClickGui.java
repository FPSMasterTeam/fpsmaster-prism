package top.fpsmaster.prism.screen;

import top.fpsmaster.prism.anim.Anim;
import top.fpsmaster.prism.canvas.Canvas;
import top.fpsmaster.prism.canvas.FontHandle;
import top.fpsmaster.prism.icon.GlyphIcons;
import top.fpsmaster.prism.theme.Argb;
import top.fpsmaster.prism.theme.Metrics;
import top.fpsmaster.prism.widget.Chrome;
import top.fpsmaster.prism.widget.Scroll;
import top.fpsmaster.prism.widget.TextBox;
import top.fpsmaster.prism.widget.UiFrame;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared Click GUI chrome: Edge {@code MainPanel} layout (sidebar + module list), including
 * the open/close scale, switch knobs, and expand-height motion.
 */
public final class SharedClickGui {
    private static final float SETTING_ROW = 19f;
    private static final float OPEN_SEC = 0.2f;
    private static final float KNOB_SPEED = 0.25f;
    private static final float EXPAND_SPEED = 0.2f;

    public String category;
    public String expandedId;
    public final TextBox search = new TextBox();
    public final Scroll scroll = new Scroll("clickgui.mods");
    private final Map<String, TextBox> fields = new HashMap<String, TextBox>();
    private final Map<String, Float> knobs = new HashMap<String, Float>();
    private final Map<String, Float> expandH = new HashMap<String, Float>();
    private String expandedColor;
    private String captureModule;
    private String captureSetting;
    private int captureListIndex = -1;

    private long lastNanos;
    private boolean animateOpen;
    private boolean closing;
    private float openT = 1f;

    public SharedClickGui(String defaultCategory) {
        this.category = defaultCategory;
        search.setPaintBox(false);
        search.setFontSize(12);
        search.setPadLeft(14f);
    }

    /** Call from the host screen's {@code init} so open motion runs (tests skip this). */
    public void onOpen() {
        animateOpen = true;
        closing = false;
        openT = 0f;
        lastNanos = 0L;
    }

    /** Start the close scale-out. {@link #draw} returns {@code true} when it has finished. */
    public void beginClose() {
        if (animateOpen) {
            closing = true;
        }
    }

    /** Cancel an active module/setting key capture. */
    public boolean cancelKeyCapture() {
        if (captureModule == null) {
            return false;
        }
        captureModule = null;
        captureSetting = null;
        captureListIndex = -1;
        return true;
    }

    /**
     * @return {@code true} when the close animation has finished and the host should dismiss
     */
    public boolean draw(UiFrame ui, ClickGuiBridge bridge) {
        applyCapturedKey(ui, bridge);
        float dt = dt(ui);
        tickOpen(dt);

        float gw = ui.host().width();
        float gh = ui.host().height();
        float eased = Anim.cssEase(openT);
        float veil = closing ? openT : eased;
        Chrome.veil(ui, veil);

        float scale = closing ? (0.7f + 0.3f * openT) : (0.8f + 0.2f * eased);
        boolean xform = animateOpen && scale < 0.999f;
        Canvas canvas = ui.canvas();
        if (xform) {
            canvas.pushTransform();
            canvas.translate(gw * 0.5f, gh * 0.5f);
            canvas.scale(scale);
            canvas.translate(-gw * 0.5f, -gh * 0.5f);
        }
        canvas.pushAlpha(Math.max(0.05f, veil));
        drawChrome(ui, bridge, dt);
        canvas.popAlpha();
        if (xform) {
            canvas.popTransform();
        }
        return closing && openT <= 0f;
    }

    private void drawChrome(UiFrame ui, ClickGuiBridge bridge, float dt) {
        float gw = ui.host().width();
        float gh = ui.host().height();
        float width = Math.min(490f, Math.max(300f, gw - 20f));
        float height = Math.min(310f, Math.max(220f, gh - 24f));
        float x = (gw - width) / 2f;
        float y = (gh - height) / 2f;
        Chrome.panel(ui, x, y, width, height);
        ui.canvas().fillRect(x + 1, y + 1, Metrics.SIDEBAR - 1, height - 2, ui.theme().layer());
        Chrome.hairlineV(ui, x + Metrics.SIDEBAR, y + 1, height - 2);

        ui.canvas().fillRoundRect(x + 7f, y + 7f, 12f, 12f, 4f, ui.theme().accent());
        FontHandle badgeFont = ui.font(12);
        float fW = badgeFont.measure("F");
        ui.canvas().drawString(badgeFont, "F", x + 7f + (12f - fW) * 0.5f, Chrome.textY(y + 7f, 12f, badgeFont),
                ui.theme().white());
        FontHandle brandFont = ui.font(13);
        FontBold.draw(ui, 13, "FPSMaster", x + 23f, Chrome.textY(y + 6f, 10f, brandFont), ui.theme().textPrimary());
        FontHandle subFont = ui.font(10);
        ui.canvas().drawString(subFont, bridge.edition() + " · " + bridge.version(),
                x + 23f, Chrome.textY(y + 14f, 8f, subFont), ui.theme().textDisabled());

        float searchX = x + 5.5f;
        float searchY = y + 25f;
        float searchW = Metrics.SIDEBAR - 11f;
        Chrome.searchBox(ui, searchX, searchY, searchW, 16f, search.focused());
        GlyphIcons.draw(ui, "search", searchX + 5f, searchY + (16f - 6.5f) / 2f, 6.5f,
                ui.theme().textDisabled());
        search.setPlaceholder(bridge.i18n("clickgui.search.placeholder"));
        search.draw(ui, searchX, searchY, searchW, 16f);

        List<String> cats = bridge.categories();
        float navX = x + 5.5f;
        float navW = Metrics.SIDEBAR - 11f;
        float my = y + 47f;
        FontHandle navFont = ui.font(13);
        FontHandle countFont = ui.font(11);
        for (int i = 0; i < cats.size(); i++) {
            String id = cats.get(i);
            boolean selected = id.equals(category);
            boolean hover = ui.hovered(navX, my, navW, Metrics.NAV_ITEM);
            Chrome.navItem(ui, navX, my, navW, Metrics.NAV_ITEM, selected, hover);
            int color = selected ? ui.theme().white() : ui.theme().textSecondary();
            GlyphIcons.draw(ui, bridge.categoryIcon(id), navX + 6f, my + (Metrics.NAV_ITEM - 7f) / 2f, 7f, color);
            String n = String.valueOf(bridge.moduleCount(id));
            float countW = countFont.measure(n);
            String label = ellipsize(navFont, bridge.categoryLabel(id), navW - 17.5f - countW - 8f);
            ui.canvas().drawString(navFont, label, navX + 17.5f, Chrome.textY(my, Metrics.NAV_ITEM, navFont), color);
            ui.canvas().drawString(countFont, n, navX + navW - 6f - countW,
                    Chrome.textY(my, Metrics.NAV_ITEM, countFont), selected ? 0xB3FFFFFF : ui.theme().textDisabled());
            if (ui.clicked(navX, my, navW, Metrics.NAV_ITEM)) {
                category = id;
                expandedId = null;
            }
            my += Metrics.NAV_ITEM + 1f;
        }

        int footerItems = bridge.hasWebUiToggle() ? 4 : 3;
        float footerY = y + height - 7f - (Metrics.NAV_ITEM + 1f) * footerItems;
        Chrome.hairlineH(ui, x + 7f, footerY - 4f, Metrics.SIDEBAR - 14f);
        if (sideNav(ui, navX, footerY, navW, bridge.i18n("clickgui.nav.music"), "music")) {
            bridge.openMusic();
        }
        if (sideNav(ui, navX, footerY + Metrics.NAV_ITEM + 1f, navW, bridge.i18n("configprofiles.button"), "folder")) {
            bridge.openProfiles();
        }
        boolean light = bridge.lightTheme();
        String themeLabel = bridge.i18n(light ? "clickgui.nav.theme.light" : "clickgui.nav.theme.dark");
        if (sideNav(ui, navX, footerY + (Metrics.NAV_ITEM + 1f) * 2f, navW, themeLabel, light ? "sun" : "moon")) {
            bridge.toggleTheme();
        }
        if (bridge.hasWebUiToggle()) {
            boolean web = bridge.webUi();
            String uiLabel = bridge.i18n(web ? "clickgui.nav.nativeui" : "clickgui.nav.webui");
            if (sideNav(ui, navX, footerY + (Metrics.NAV_ITEM + 1f) * 3f, navW, uiLabel, web ? "grid" : "monitor")) {
                bridge.toggleWebUi();
            }
        }

        float mainX = x + Metrics.SIDEBAR;
        float listY = y + 22f;
        float listH = height - 28f;
        float containerW = width - Metrics.SIDEBAR - 12f;
        boolean searching = !search.text().trim().isEmpty();
        String title = searching ? search.text() : bridge.categoryLabel(category);
        FontHandle titleFont = ui.font(16);
        FontHandle metaFont = ui.font(12);
        float headTop = y + 6f;
        float headH = 12f;
        FontBold.draw(ui, 16, title, mainX + 9f, Chrome.textY(headTop, headH, titleFont), ui.theme().textPrimary());
        if (!searching) {
            String meta;
            try {
                meta = String.format(bridge.i18n("clickgui.category.meta"),
                        Integer.valueOf(bridge.moduleCount(category)),
                        Integer.valueOf(bridge.enabledCount(category)));
            } catch (Exception ignored) {
                meta = bridge.moduleCount(category) + " · " + bridge.enabledCount(category);
            }
            ui.canvas().drawString(metaFont, meta, mainX + 9f + titleFont.measure(title) + 5f,
                    Chrome.textY(headTop, headH, metaFont), ui.theme().textSecondary());
        }

        List<ClickGuiBridge.ModInfo> mods = bridge.modules(category, search.text());
        float contentH = 8f;
        for (int i = 0; i < mods.size(); i++) {
            ClickGuiBridge.ModInfo mod = mods.get(i);
            float extra = expandH.containsKey(mod.id) ? expandH.get(mod.id).floatValue() : 0f;
            contentH += Metrics.MODULE_ROW + 3f + extra;
        }
        float off = scroll.begin(ui, mainX + 6f, listY, containerW, listH, contentH);
        float modsY = listY + 1f + off;
        for (int i = 0; i < mods.size(); i++) {
            ClickGuiBridge.ModInfo mod = mods.get(i);
            modsY += drawMod(ui, bridge, mod, mainX + 6f, modsY, containerW - 6f, dt);
        }
        if (searching && mods.isEmpty()) {
            String empty = bridge.i18n("clickgui.search.noresults");
            FontHandle emptyFont = ui.font(14);
            ui.canvas().drawString(emptyFont, empty,
                    mainX + (containerW - emptyFont.measure(empty)) / 2f,
                    listY + listH / 2f - emptyFont.lineHeight() * 0.5f, ui.theme().textDisabled());
        }
        scroll.end(ui);
    }

    private boolean sideNav(UiFrame ui, float x, float y, float w, String label, String icon) {
        boolean hover = ui.hovered(x, y, w, Metrics.NAV_ITEM);
        Chrome.navItem(ui, x, y, w, Metrics.NAV_ITEM, false, hover);
        int color = hover ? ui.theme().textPrimary() : ui.theme().textSecondary();
        GlyphIcons.draw(ui, icon, x + 6f, y + (Metrics.NAV_ITEM - 7f) / 2f, 7f, color);
        FontHandle font = ui.font(13);
        ui.canvas().drawString(font, label, x + 17.5f, Chrome.textY(y, Metrics.NAV_ITEM, font), color);
        return ui.clicked(x, y, w, Metrics.NAV_ITEM);
    }

    private float drawMod(UiFrame ui, ClickGuiBridge bridge, ClickGuiBridge.ModInfo mod,
                          float x, float y, float w, float dt) {
        boolean open = mod.id.equals(expandedId);
        float targetExtra = 0f;
        if (open) {
            for (int i = 0; i < mod.settings.size(); i++) {
                targetExtra += settingHeight(mod.id, mod.settings.get(i));
            }
        }
        float extra = Anim.approach(expandH.containsKey(mod.id) ? expandH.get(mod.id).floatValue() : 0f,
                targetExtra, EXPAND_SPEED, dt);
        expandH.put(mod.id, Float.valueOf(extra));

        boolean hover = ui.hovered(x, y, w, Metrics.MODULE_ROW + extra);
        Chrome.card(ui, x, y, w, Metrics.MODULE_ROW + extra, hover, extra > 0.5f);
        FontHandle nameFont = ui.font(14);
        ui.canvas().drawString(nameFont, mod.name, x + 8f,
                Chrome.textY(y, Metrics.MODULE_ROW, nameFont), ui.theme().textPrimary());
        float sw = Metrics.SWITCH_W;
        float sh = Metrics.SWITCH_H;
        float sx = x + w - sw - 8f;
        float sy = y + (Metrics.MODULE_ROW - sh) / 2f;
        if (mod.keyName != null && !mod.keyName.isEmpty()) {
            boolean capturing = mod.id.equals(captureModule) && captureSetting == null;
            String keyName = capturing ? "..." : mod.keyName;
            float kw = Math.max(24f, ui.font(10).measure(keyName) + 8f);
            if (Chrome.button(ui, sx - kw - 5f, y + 5f, kw, Metrics.MODULE_ROW - 10f, keyName,
                    capturing ? Chrome.ButtonStyle.PRIMARY : Chrome.ButtonStyle.GHOST)) {
                captureModule = mod.id;
                captureSetting = null;
                captureListIndex = -1;
            }
        }
        float knob = knob(mod.id, mod.enabled, dt);
        Chrome.drawSwitch(ui, sx, sy, mod.enabled, knob);
        if (mod.canToggle && ui.clicked(sx, y, sw, Metrics.MODULE_ROW)) {
            bridge.toggle(mod.id);
        } else if (ui.clicked(x, y, w - sw - 10f, Metrics.MODULE_ROW)) {
            expandedId = open ? null : mod.id;
        }

        if (extra > 0.4f && !mod.settings.isEmpty()) {
            Chrome.hairlineH(ui, x + 6f, y + Metrics.MODULE_ROW, w - 12f);
            ui.canvas().pushClip(x, y + Metrics.MODULE_ROW, w, extra);
            float vy = y + Metrics.MODULE_ROW + (SETTING_ROW - 16f) * 0.5f;
            for (int i = 0; i < mod.settings.size(); i++) {
                ClickGuiBridge.SettingInfo s = mod.settings.get(i);
                vy += drawSetting(ui, bridge, mod, s, x, vy, w, dt);
            }
            ui.canvas().popClip();
        }
        return Metrics.MODULE_ROW + 3f + extra;
    }

    private float drawSetting(UiFrame ui, ClickGuiBridge bridge, ClickGuiBridge.ModInfo mod,
                              ClickGuiBridge.SettingInfo s, float x, float vy, float w, float dt) {
        FontHandle labelFont = ui.font(12);
        ui.canvas().drawString(labelFont, s.label, x + 10f,
                Chrome.textY(vy, SETTING_ROW, labelFont), ui.theme().textSecondary());
        if (s.kind == ClickGuiBridge.SettingInfo.BOOL) {
            float sw = Metrics.SWITCH_W;
            float sh = Metrics.SWITCH_H;
            float sx = x + w - sw - 10f;
            float sy = vy + (SETTING_ROW - sh) / 2f;
            String kid = mod.id + "." + s.id;
            Chrome.drawSwitch(ui, sx, sy, s.boolValue, knob(kid, s.boolValue, dt));
            if (ui.clicked(sx, sy, sw, sh) || ui.clicked(x, vy, w, SETTING_ROW)) {
                bridge.setBool(mod.id, s.id, !s.boolValue);
            }
        } else if (s.kind == ClickGuiBridge.SettingInfo.NUMBER) {
            double span = s.max - s.min;
            float t = span == 0 ? 0f : (float) ((s.numberValue - s.min) / span);
            float sy = vy + (SETTING_ROW - Metrics.SLIDER_H) / 2f;
            float nt = Chrome.slider(ui, mod.id + "." + s.id, x + 90f, sy, w - 110f, t);
            double mapped = s.min + nt * span;
            if (Math.abs(mapped - s.numberValue) > 1e-4) {
                bridge.setNumber(mod.id, s.id, mapped);
            }
        } else if (s.kind == ClickGuiBridge.SettingInfo.TEXT) {
            String key = mod.id + "." + s.id;
            TextBox box = fields.get(key);
            if (box == null) {
                box = new TextBox(s.textValue);
                box.setFontSize(11);
                fields.put(key, box);
            }
            float bh = 14f;
            box.draw(ui, x + 90f, vy + (SETTING_ROW - bh) / 2f, w - 110f, bh);
            if (!box.text().equals(s.textValue)) {
                bridge.setText(mod.id, s.id, box.text());
            }
        } else if (s.kind == ClickGuiBridge.SettingInfo.CHOICE) {
            String value = s.options.isEmpty() ? "" : s.options.get(Math.max(0, Math.min(s.selectedIndex, s.options.size() - 1)));
            float bw = Math.max(44f, ui.font(11).measure(value) + 14f);
            float bx = x + w - bw - 10f;
            if (Chrome.button(ui, bx, vy + 2.5f, bw, 14f, value, Chrome.ButtonStyle.DEFAULT) && !s.options.isEmpty()) {
                bridge.setChoice(mod.id, s.id, (s.selectedIndex + 1) % s.options.size());
            }
        } else if (s.kind == ClickGuiBridge.SettingInfo.COLOR) {
            return drawColor(ui, bridge, mod.id, s, x, vy, w);
        } else if (s.kind == ClickGuiBridge.SettingInfo.KEY) {
            drawKey(ui, mod.id, s.id, s.keyName, false, -1, x, vy, w);
        } else if (s.kind == ClickGuiBridge.SettingInfo.LIST) {
            return drawList(ui, bridge, mod.id, s, x, vy, w);
        }
        return SETTING_ROW;
    }

    private float drawColor(UiFrame ui, ClickGuiBridge bridge, String moduleId,
                            ClickGuiBridge.SettingInfo s, float x, float y, float w) {
        String key = moduleId + "." + s.id;
        boolean open = key.equals(expandedColor);
        int rgb = java.awt.Color.HSBtoRGB(s.hue, s.saturation, s.brightness);
        int color = Argb.of(Math.round(s.alpha * 255f), (rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255);
        float dot = 10f;
        float dx = x + w - dot - 10f;
        ui.canvas().fillCircle(dx + dot / 2f, y + SETTING_ROW / 2f, dot / 2f, color);
        String mode = s.colorMode == null ? "" : s.colorMode;
        float mw = Math.max(38f, ui.font(10).measure(mode) + 10f);
        if (Chrome.button(ui, dx - mw - 5f, y + 2.5f, mw, 14f, mode, Chrome.ButtonStyle.GHOST)) {
            expandedColor = open ? null : key;
        }
        if (!open) {
            if (ui.clicked(dx - 2f, y, dot + 4f, SETTING_ROW)) {
                expandedColor = key;
            }
            return SETTING_ROW;
        }
        float[] values = {s.hue, s.saturation, s.brightness, s.alpha};
        String[] labels = {"H", "S", "B", "A"};
        for (int i = 0; i < values.length; i++) {
            float rowY = y + SETTING_ROW * (i + 1);
            ui.canvas().drawString(ui.font(10), labels[i], x + 12f, Chrome.textY(rowY, SETTING_ROW, ui.font(10)), ui.theme().textDisabled());
            float next = Chrome.slider(ui, key + "." + labels[i], x + 28f,
                    rowY + (SETTING_ROW - Metrics.SLIDER_H) / 2f, w - 40f, values[i]);
            if (Math.abs(next - values[i]) > 1e-4f) {
                float h = i == 0 ? next : s.hue;
                float sat = i == 1 ? next : s.saturation;
                float bri = i == 2 ? next : s.brightness;
                float alpha = i == 3 ? next : s.alpha;
                bridge.setColor(moduleId, s.id, h, sat, bri, alpha, mode);
            }
        }
        if (s.options.size() > 1 && Chrome.button(ui, x + w - 64f, y + SETTING_ROW * 4f + 2.5f,
                54f, 14f, mode, Chrome.ButtonStyle.DEFAULT)) {
            int index = s.options.indexOf(mode);
            String next = s.options.get((index + 1 + s.options.size()) % s.options.size());
            bridge.setColor(moduleId, s.id, s.hue, s.saturation, s.brightness, s.alpha, next);
        }
        return SETTING_ROW * 5f;
    }

    private void drawKey(UiFrame ui, String moduleId, String settingId, String keyName,
                         boolean list, int listIndex, float x, float y, float w) {
        boolean capturing = moduleId.equals(captureModule) && settingId.equals(captureSetting)
                && captureListIndex == (list ? listIndex : -1);
        String label = capturing ? "..." : (keyName == null || keyName.isEmpty() ? "None" : keyName);
        float bw = Math.max(32f, ui.font(10).measure(label) + 10f);
        if (Chrome.button(ui, x + w - bw - 10f, y + 2.5f, bw, 14f, label,
                capturing ? Chrome.ButtonStyle.PRIMARY : Chrome.ButtonStyle.DEFAULT)) {
            captureModule = moduleId;
            captureSetting = settingId;
            captureListIndex = list ? listIndex : -1;
        }
    }

    private float drawList(UiFrame ui, ClickGuiBridge bridge, String moduleId,
                           ClickGuiBridge.SettingInfo s, float x, float y, float w) {
        float h = SETTING_ROW * (s.items.size() + 1);
        if (s.items.size() < s.maxItems && Chrome.button(ui, x + w - 28f, y + 2.5f, 18f, 14f,
                "+", Chrome.ButtonStyle.DEFAULT)) {
            bridge.addListItem(moduleId, s.id);
        }
        for (int i = 0; i < s.items.size(); i++) {
            ClickGuiBridge.ListItem item = s.items.get(i);
            float rowY = y + SETTING_ROW * (i + 1);
            float removeX = x + w - 24f;
            if (Chrome.button(ui, removeX, rowY + 2.5f, 14f, 14f, "×", Chrome.ButtonStyle.GHOST)) {
                bridge.removeListItem(moduleId, s.id, i);
                continue;
            }
            if (s.editableItems) {
                drawKey(ui, moduleId, s.id, item.keyName, true, i, x, rowY, 42f);
                String fieldKey = moduleId + "." + s.id + "." + i;
                TextBox box = fields.get(fieldKey);
                if (box == null) {
                    box = new TextBox(item.text);
                    box.setFontSize(11);
                    fields.put(fieldKey, box);
                }
                box.draw(ui, x + 48f, rowY + 2.5f, w - 78f, 14f);
                if (!box.text().equals(item.text)) {
                    bridge.setListItemText(moduleId, s.id, i, box.text());
                }
            } else {
                ui.canvas().drawString(ui.font(11), item.text, x + 12f,
                        Chrome.textY(rowY, SETTING_ROW, ui.font(11)), ui.theme().textSecondary());
            }
        }
        return h;
    }

    private float settingHeight(String moduleId, ClickGuiBridge.SettingInfo s) {
        if (s.kind == ClickGuiBridge.SettingInfo.COLOR && (moduleId + "." + s.id).equals(expandedColor)) {
            return SETTING_ROW * 5f;
        }
        if (s.kind == ClickGuiBridge.SettingInfo.LIST) {
            return SETTING_ROW * (s.items.size() + 1);
        }
        return SETTING_ROW;
    }

    private void applyCapturedKey(UiFrame ui, ClickGuiBridge bridge) {
        int raw = ui.input().consumeRawKey();
        if (raw < 0 || captureModule == null) {
            return;
        }
        // LWJGL2 ESC=1, GLFW ESC=256. Escape cancels capture on both hosts.
        if (raw == 1 || raw == 256) {
            cancelKeyCapture();
            return;
        }
        if (captureSetting == null) {
            bridge.setModuleKey(captureModule, raw);
        } else if (captureListIndex >= 0) {
            bridge.setListItemKey(captureModule, captureSetting, captureListIndex, raw);
        } else {
            bridge.setKey(captureModule, captureSetting, raw);
        }
        captureModule = null;
        captureSetting = null;
        captureListIndex = -1;
    }

    private float knob(String id, boolean on, float dt) {
        float target = on ? 1f : 0f;
        float cur = knobs.containsKey(id) ? knobs.get(id).floatValue() : target;
        float next = Anim.approach(cur, target, KNOB_SPEED, dt);
        knobs.put(id, Float.valueOf(next));
        return next;
    }

    private void tickOpen(float dt) {
        if (!animateOpen) {
            openT = closing ? 0f : 1f;
            return;
        }
        if (closing) {
            openT = Math.max(0f, openT - dt / OPEN_SEC);
        } else if (openT < 1f) {
            openT = Math.min(1f, openT + dt / OPEN_SEC);
        }
    }

    private float dt(UiFrame ui) {
        long now = ui.host().nowNanos();
        float dt = lastNanos == 0L ? 0.016f : (now - lastNanos) / 1_000_000_000f;
        lastNanos = now;
        if (dt > 0.05f) {
            dt = 0.05f;
        }
        if (dt < 0f) {
            dt = 0.016f;
        }
        return dt;
    }

    private static String ellipsize(FontHandle font, String text, float max) {
        if (text == null) {
            return "";
        }
        if (max <= 0f || font.measure(text) <= max) {
            return text;
        }
        String dots = "...";
        if (font.measure(dots) >= max) {
            return dots;
        }
        for (int i = text.length() - 1; i > 0; i--) {
            String cut = text.substring(0, i) + dots;
            if (font.measure(cut) <= max) {
                return cut;
            }
        }
        return dots;
    }
}

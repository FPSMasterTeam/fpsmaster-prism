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
    private static final float GROUP_ROW = 18f;
    private static final float OPTION_ROW = 17f;
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
    private final Map<String, Boolean> collapsedGroups = new HashMap<String, Boolean>();
    private String expandedColor;
    private String expandedChoice;
    private String captureModule;
    private String captureSetting;
    private int captureListIndex = -1;

    private long lastNanos;
    private boolean animateOpen;
    private boolean closing;
    private float openT = 1f;
    private float categoryT = 1f;

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
                if (!id.equals(category)) {
                    category = id;
                    expandedId = null;
                    expandedColor = null;
                    expandedChoice = null;
                    scroll.setOffset(0f);
                    categoryT = 0f;
                }
            }
            my += Metrics.NAV_ITEM + 1f;
        }

        int footerItems = bridge.hasWebUiToggle() ? 5 : 4;
        float footerY = y + height - 7f - (Metrics.NAV_ITEM + 1f) * footerItems;
        Chrome.hairlineH(ui, x + 7f, footerY - 4f, Metrics.SIDEBAR - 14f);
        if (sideNav(ui, navX, footerY, navW, bridge.i18n("clickgui.nav.music"), "music")) {
            bridge.openMusic();
        }
        if (sideNav(ui, navX, footerY + Metrics.NAV_ITEM + 1f, navW, bridge.i18n("configprofiles.button"), "folder")) {
            bridge.openProfiles();
        }
        if (sideNav(ui, navX, footerY + (Metrics.NAV_ITEM + 1f) * 2f, navW,
                bridge.i18n("hud.editor.title"), "sliders")) {
            bridge.openHudEditor();
        }
        boolean light = bridge.lightTheme();
        String themeLabel = bridge.i18n(light ? "clickgui.nav.theme.light" : "clickgui.nav.theme.dark");
        if (sideNav(ui, navX, footerY + (Metrics.NAV_ITEM + 1f) * 3f, navW, themeLabel, light ? "sun" : "moon")) {
            bridge.toggleTheme();
        }
        if (bridge.hasWebUiToggle()) {
            boolean web = bridge.webUi();
            String uiLabel = bridge.i18n(web ? "clickgui.nav.nativeui" : "clickgui.nav.webui");
            if (sideNav(ui, navX, footerY + (Metrics.NAV_ITEM + 1f) * 4f, navW, uiLabel, web ? "grid" : "monitor")) {
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
        String meta = "";
        if (!searching) {
            try {
                meta = String.format(bridge.i18n("clickgui.category.meta"),
                        Integer.valueOf(bridge.moduleCount(category)),
                        Integer.valueOf(bridge.enabledCount(category)));
            } catch (Exception ignored) {
                meta = bridge.moduleCount(category) + " · " + bridge.enabledCount(category);
            }
        }
        float metaW = metaFont.measure(meta);
        String shortTitle = ellipsize(titleFont, title, containerW - 18f - (meta.isEmpty() ? 0f : metaW + 8f));
        FontBold.draw(ui, 16, shortTitle, mainX + 9f, Chrome.textY(headTop, headH, titleFont), ui.theme().textPrimary());
        if (!meta.isEmpty()) {
            ui.canvas().drawString(metaFont, meta, mainX + containerW - metaW - 5f,
                    Chrome.textY(headTop, headH, metaFont), ui.theme().textSecondary());
        }

        List<ClickGuiBridge.ModInfo> mods = bridge.modules(category, search.text());
        float contentH = 8f;
        for (int i = 0; i < mods.size(); i++) {
            ClickGuiBridge.ModInfo mod = mods.get(i);
            float extra = expandH.containsKey(mod.id) ? expandH.get(mod.id).floatValue() : 0f;
            contentH += Metrics.MODULE_ROW + 3f + extra;
        }
        categoryT = Anim.approach(categoryT, 1f, 0.16f, dt);
        ui.canvas().pushAlpha(0.45f + Anim.cssEase(categoryT) * 0.55f);
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
        ui.canvas().popAlpha();
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
        float targetExtra = open ? settingsHeight(mod, w) : 0f;
        float extra = Anim.approach(expandH.containsKey(mod.id) ? expandH.get(mod.id).floatValue() : 0f,
                targetExtra, EXPAND_SPEED, dt);
        expandH.put(mod.id, Float.valueOf(extra));

        boolean hover = ui.hovered(x, y, w, Metrics.MODULE_ROW + extra);
        Chrome.card(ui, x, y, w, Metrics.MODULE_ROW + extra, hover, extra > 0.5f);
        float sw = Metrics.SWITCH_W;
        float sh = Metrics.SWITCH_H;
        float sx = x + w - sw - 8f;
        float sy = y + (Metrics.MODULE_ROW - sh) / 2f;
        float keyW = 0f;
        String keyName = mod.keyName;
        if (mod.keyName != null && !mod.keyName.isEmpty()) {
            boolean capturing = mod.id.equals(captureModule) && captureSetting == null;
            keyName = capturing ? "..." : mod.keyName;
            keyW = Math.max(24f, ui.font(10).measure(keyName) + 8f);
            if (Chrome.button(ui, sx - keyW - 5f, y + 5f, keyW, Metrics.MODULE_ROW - 10f, keyName,
                    capturing ? Chrome.ButtonStyle.PRIMARY : Chrome.ButtonStyle.GHOST)) {
                captureModule = mod.id;
                captureSetting = null;
                captureListIndex = -1;
            }
        }
        FontHandle nameFont = ui.font(14);
        float nameRight = sx - (keyW > 0f ? keyW + 8f : 5f);
        String name = ellipsize(nameFont, mod.name, nameRight - x - 8f);
        ui.canvas().drawString(nameFont, name, x + 8f,
                Chrome.textY(y, Metrics.MODULE_ROW, nameFont), ui.theme().textPrimary());
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
            float vy = y + Metrics.MODULE_ROW;
            String lastGroup = null;
            boolean collapsed = false;
            for (int i = 0; i < mod.settings.size(); i++) {
                ClickGuiBridge.SettingInfo s = mod.settings.get(i);
                if (!s.visible) {
                    continue;
                }
                String groupId = s.group == null ? null : s.group.id;
                if (groupId != null && !groupId.equals(lastGroup)) {
                    collapsed = groupCollapsed(mod.id, s.group);
                    vy += drawGroup(ui, bridge, mod.id, s.group, x, vy, w, collapsed);
                } else if (groupId == null) {
                    collapsed = false;
                }
                lastGroup = groupId;
                if (collapsed) {
                    continue;
                }
                vy += drawSetting(ui, bridge, mod, s, x, vy, w, dt);
            }
            ui.canvas().popClip();
        }
        return Metrics.MODULE_ROW + 3f + extra;
    }

    private float drawGroup(UiFrame ui, ClickGuiBridge bridge, String moduleId,
                            ClickGuiBridge.GroupInfo group, float x, float y, float w, boolean collapsed) {
        String label = group.label.isEmpty() ? bridge.settingGroupLabel(group.id) : group.label;
        FontHandle font = ui.font(11);
        GlyphIcons.draw(ui, collapsed ? "chev-r" : "chev-d", x + 10f, y + 5.5f, 6f,
                ui.theme().textDisabled());
        ui.canvas().drawString(font, ellipsize(font, label, w - 34f), x + 20f,
                Chrome.textY(y, GROUP_ROW, font), ui.theme().textSecondary());
        if (ui.clicked(x + 6f, y, w - 12f, GROUP_ROW)) {
            collapsedGroups.put(groupKey(moduleId, group.id), Boolean.valueOf(!collapsed));
        }
        return GROUP_ROW;
    }

    private float drawSetting(UiFrame ui, ClickGuiBridge bridge, ClickGuiBridge.ModInfo mod,
                              ClickGuiBridge.SettingInfo s, float x, float vy, float w, float dt) {
        FontHandle labelFont = ui.font(12);
        ui.canvas().drawString(labelFont, ellipsize(labelFont, s.label, w - 118f), x + 10f,
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
            String key = mod.id + "." + s.id;
            boolean open = key.equals(expandedChoice);
            String value = s.options.isEmpty() ? "" : s.options.get(Math.max(0, Math.min(s.selectedIndex, s.options.size() - 1)));
            float bw = Math.min(w - 116f, Math.max(44f, ui.font(11).measure(value) + 18f));
            float bx = x + w - bw - 10f;
            if (Chrome.button(ui, bx, vy + 2.5f, bw, 14f, ellipsize(ui.font(11), value, bw - 14f),
                    open ? Chrome.ButtonStyle.PRIMARY : Chrome.ButtonStyle.DEFAULT)) {
                expandedChoice = open ? null : key;
            }
            if (open) {
                return drawChoices(ui, bridge, mod.id, s, x, vy, w);
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

    private float drawChoices(UiFrame ui, ClickGuiBridge bridge, String moduleId,
                              ClickGuiBridge.SettingInfo s, float x, float y, float w) {
        int columns = w >= 260f ? 3 : 2;
        int rows = (s.options.size() + columns - 1) / columns;
        float gap = 4f;
        float cellW = (w - 20f - gap * (columns - 1)) / columns;
        for (int i = 0; i < s.options.size(); i++) {
            int col = i % columns;
            int row = i / columns;
            float bx = x + 10f + col * (cellW + gap);
            float by = y + SETTING_ROW + row * OPTION_ROW + 1f;
            if (smallOption(ui, bx, by, cellW, 14f, s.options.get(i), i == s.selectedIndex)) {
                bridge.setChoice(moduleId, s.id, i);
                expandedChoice = null;
            }
        }
        return SETTING_ROW + rows * OPTION_ROW + 2f;
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
        if (Chrome.button(ui, dx - mw - 5f, y + 2.5f, mw, 14f, ellipsize(ui.font(10), mode, mw - 8f),
                open ? Chrome.ButtonStyle.PRIMARY : Chrome.ButtonStyle.GHOST)) {
            expandedColor = open ? null : key;
        }
        if (!open) {
            if (ui.clicked(dx - 2f, y, dot + 4f, SETTING_ROW)) {
                expandedColor = key;
            }
            return SETTING_ROW;
        }
        float pickerX = x + 10f;
        float pickerY = y + SETTING_ROW + 3f;
        float pickerW = Math.max(72f, w - (s.options.isEmpty() ? 20f : 82f));
        float pickerH = Math.max(47f, s.options.size() * 16f - 2f);
        int hueColor = java.awt.Color.HSBtoRGB(s.hue, 1f, 1f) | 0xFF000000;
        ui.canvas().fillGradientH(pickerX, pickerY, pickerW, pickerH, 0xFFFFFFFF, hueColor);
        ui.canvas().fillGradientV(pickerX, pickerY, pickerW, pickerH, 0x00000000, 0xFF000000);
        ui.canvas().strokeRoundRect(pickerX, pickerY, pickerW, pickerH, 2f, 0.75f, ui.theme().strokeStrong());
        float markerX = pickerX + s.saturation * pickerW;
        float markerY = pickerY + (1f - s.brightness) * pickerH;
        ui.canvas().fillCircle(markerX, markerY, 3.25f, ui.theme().white());
        ui.canvas().fillCircle(markerX, markerY, 2f, color | 0xFF000000);
        String svDrag = key + ".sv";
        if (ui.input().beginDrag(svDrag, 0, pickerX, pickerY, pickerW, pickerH) || ui.input().isDragging(svDrag)) {
            float sat = clamp01((ui.input().mouseX() - pickerX) / pickerW);
            float bri = 1f - clamp01((ui.input().mouseY() - pickerY) / pickerH);
            bridge.setColor(moduleId, s.id, s.hue, sat, bri, s.alpha, mode);
            releaseDrag(ui, svDrag);
        }

        float modeX = pickerX + pickerW + 6f;
        float modeW = x + w - 10f - modeX;
        for (int i = 0; i < s.options.size(); i++) {
            String option = s.options.get(i);
            if (smallOption(ui, modeX, pickerY + i * 16f, modeW, 14f, option, option.equals(mode))) {
                bridge.setColor(moduleId, s.id, s.hue, s.saturation, s.brightness, s.alpha, option);
            }
        }

        float hueY = pickerY + pickerH + 5f;
        drawHue(ui, pickerX, hueY, w - 20f, 5f);
        float hue = dragValue(ui, key + ".hue", pickerX, hueY, w - 20f, 5f, s.hue);
        if (Math.abs(hue - s.hue) > 1e-4f) {
            bridge.setColor(moduleId, s.id, hue, s.saturation, s.brightness, s.alpha, mode);
        }

        float alphaY = hueY + 9f;
        drawChecker(ui, pickerX, alphaY, w - 20f, 5f);
        int opaque = color | 0xFF000000;
        ui.canvas().fillGradientH(pickerX, alphaY, w - 20f, 5f, opaque & 0x00FFFFFF, opaque);
        float alpha = dragValue(ui, key + ".alpha", pickerX, alphaY, w - 20f, 5f, s.alpha);
        if (Math.abs(alpha - s.alpha) > 1e-4f) {
            bridge.setColor(moduleId, s.id, s.hue, s.saturation, s.brightness, alpha, mode);
        }
        return SETTING_ROW + pickerH + 26f;
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

    private float settingsHeight(ClickGuiBridge.ModInfo mod, float width) {
        float height = 0f;
        String lastGroup = null;
        boolean collapsed = false;
        for (int i = 0; i < mod.settings.size(); i++) {
            ClickGuiBridge.SettingInfo s = mod.settings.get(i);
            if (!s.visible) {
                continue;
            }
            String groupId = s.group == null ? null : s.group.id;
            if (groupId != null && !groupId.equals(lastGroup)) {
                height += GROUP_ROW;
                collapsed = groupCollapsed(mod.id, s.group);
            } else if (groupId == null) {
                collapsed = false;
            }
            lastGroup = groupId;
            if (!collapsed) {
                height += settingHeight(mod.id, s, width);
            }
        }
        return height;
    }

    private float settingHeight(String moduleId, ClickGuiBridge.SettingInfo s, float width) {
        if (s.kind == ClickGuiBridge.SettingInfo.COLOR && (moduleId + "." + s.id).equals(expandedColor)) {
            return SETTING_ROW + Math.max(47f, s.options.size() * 16f - 2f) + 26f;
        }
        if (s.kind == ClickGuiBridge.SettingInfo.CHOICE && (moduleId + "." + s.id).equals(expandedChoice)) {
            int columns = width >= 260f ? 3 : 2;
            int rows = (s.options.size() + columns - 1) / columns;
            return SETTING_ROW + rows * OPTION_ROW + 2f;
        }
        if (s.kind == ClickGuiBridge.SettingInfo.LIST) {
            return SETTING_ROW * (s.items.size() + 1);
        }
        return SETTING_ROW;
    }

    private boolean groupCollapsed(String moduleId, ClickGuiBridge.GroupInfo group) {
        String key = groupKey(moduleId, group.id);
        Boolean value = collapsedGroups.get(key);
        if (value == null) {
            value = Boolean.valueOf(group.collapsedByDefault);
            collapsedGroups.put(key, value);
        }
        return value.booleanValue();
    }

    private static String groupKey(String moduleId, String groupId) {
        return moduleId + ".group." + groupId;
    }

    private boolean smallOption(UiFrame ui, float x, float y, float w, float h, String label, boolean selected) {
        boolean hover = ui.hovered(x, y, w, h);
        if (selected) {
            ui.canvas().fillRoundRect(x, y, w, h, 3f, ui.theme().accentSoft());
            ui.canvas().strokeRoundRect(x, y, w, h, 3f, 0.75f, ui.theme().accentBorder());
        } else if (hover) {
            ui.canvas().fillRoundRect(x, y, w, h, 3f, ui.theme().layerHover());
        } else {
            ui.canvas().fillRoundRect(x, y, w, h, 3f, ui.theme().layer());
        }
        FontHandle font = ui.font(10);
        String text = ellipsize(font, label, w - 8f);
        ui.canvas().drawString(font, text, x + (w - font.measure(text)) * 0.5f,
                Chrome.textY(y, h, font), selected ? ui.theme().accent() : ui.theme().textSecondary());
        return ui.clicked(x, y, w, h);
    }

    private static void drawHue(UiFrame ui, float x, float y, float w, float h) {
        int[] colors = {0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000};
        float segment = w / 6f;
        for (int i = 0; i < 6; i++) {
            ui.canvas().fillGradientH(x + segment * i, y, segment + 0.5f, h, colors[i], colors[i + 1]);
        }
    }

    private static void drawChecker(UiFrame ui, float x, float y, float w, float h) {
        float size = 2.5f;
        int columns = Math.max(1, (int) Math.ceil(w / size));
        for (int i = 0; i < columns; i++) {
            ui.canvas().fillRect(x + i * size, y, Math.min(size, w - i * size), h,
                    (i & 1) == 0 ? 0xFFB8BDC2 : 0xFF747B82);
        }
    }

    private static float dragValue(UiFrame ui, String id, float x, float y, float w, float h, float value) {
        if (ui.input().beginDrag(id, 0, x, y, w, h) || ui.input().isDragging(id)) {
            value = clamp01((ui.input().mouseX() - x) / w);
            releaseDrag(ui, id);
        }
        ui.canvas().fillCircle(x + value * w, y + h * 0.5f, 2.75f, ui.theme().white());
        return value;
    }

    private static void releaseDrag(UiFrame ui, String id) {
        if (!ui.input().isButtonDown(0)) {
            ui.input().releaseDrag(id);
        }
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

    private static float clamp01(float value) {
        return value < 0f ? 0f : (value > 1f ? 1f : value);
    }
}

package top.fpsmaster.prism.screen;

import java.util.ArrayList;
import java.util.List;
import top.fpsmaster.prism.canvas.FontHandle;
import top.fpsmaster.prism.icon.GlyphIcons;
import top.fpsmaster.prism.theme.Metrics;
import top.fpsmaster.prism.widget.Chrome;
import top.fpsmaster.prism.widget.Scroll;
import top.fpsmaster.prism.widget.UiFrame;

/** Compact in-game cosmetics shop. Minecraft entity rendering remains in the client bridge. */
public final class SharedCosmetics {
    private static final float MAX_W = 660f;
    private static final float MAX_H = 380f;
    private static final float HEADER_H = 29f;
    private static final float TOOLBAR_H = 33f;
    private static final float CARD_H = 104f;
    private static final float GAP = 7f;

    private final Object scaleDrag = new Object();
    private final Object previewDrag = new Object();
    private final Scroll itemScroll = new Scroll(new Object());
    private float previewYaw = 180f;
    private float previewDragX;
    private float previewDragYaw;
    private boolean previewDragging;
    private boolean ownedOnly;
    private String category = "all";
    private String selectedId;

    public boolean draw(UiFrame ui, CosmeticsBridge bridge) {
        float gw = ui.host().width();
        float gh = ui.host().height();
        float panelW = Math.min(MAX_W, gw - 20f);
        float panelH = Math.min(MAX_H, gh - 20f);
        float panelX = (gw - panelW) / 2f;
        float panelY = (gh - panelH) / 2f;

        Chrome.veil(ui, 1f);
        Chrome.panel(ui, panelX, panelY, panelW, panelH);
        if (drawHeader(ui, bridge, panelX, panelY, panelW)) return true;

        List<CosmeticsBridge.Item> all = bridge.items();
        ensureSelection(all, bridge);
        ensureVisibleSelection(all, bridge);

        float contentY = panelY + HEADER_H;
        float rightW = Math.max(178f, Math.min(260f, panelW * 0.39f));
        float rightX = panelX + panelW - rightW;
        Chrome.hairlineV(ui, rightX, contentY, panelH - HEADER_H);

        float catalogX = panelX + 9f;
        float catalogW = rightX - catalogX - 9f;
        drawCatalog(ui, bridge, all, catalogX, contentY, catalogW, panelH - HEADER_H - 9f);
        drawPreview(ui, bridge, selected(all), rightX, contentY, rightW, panelH - HEADER_H);
        return false;
    }

    private boolean drawHeader(UiFrame ui, CosmeticsBridge bridge, float x, float y, float w) {
        if (back(ui, x + 8f, y + 7f)) return true;
        FontBold.draw(ui, 17, bridge.i18n("cosmetics.title"), x + 32f, y + 8f,
                ui.theme().textPrimary());

        float tabsX = x + 98f;
        drawScopeTab(ui, tabsX, y, 38f, bridge.i18n("cosmetics.store"), !ownedOnly);
        if (ui.clicked(tabsX, y, 38f, HEADER_H)) {
            ownedOnly = false;
            itemScroll.setOffset(0f);
        }
        drawScopeTab(ui, tabsX + 48f, y, 54f, bridge.i18n("cosmetics.owned"), ownedOnly);
        if (ui.clicked(tabsX + 48f, y, 54f, HEADER_H)) {
            ownedOnly = true;
            itemScroll.setOffset(0f);
        }
        Chrome.hairlineH(ui, x, y + HEADER_H, w);
        return false;
    }

    private void drawScopeTab(UiFrame ui, float x, float y, float w, String label, boolean selected) {
        boolean hover = ui.hovered(x, y, w, HEADER_H);
        FontHandle font = ui.font(13);
        ui.canvas().drawString(font, fit(font, label, w), x, y + 10f,
                selected || hover ? ui.theme().textPrimary() : ui.theme().textSecondary());
        if (selected) {
            ui.canvas().fillRoundRect(x, y + HEADER_H - 1f, Math.min(w, font.measure(label)), 1.5f,
                    1f, ui.theme().accent());
        }
    }

    private void ensureSelection(List<CosmeticsBridge.Item> items, CosmeticsBridge bridge) {
        if (find(items, selectedId) != null) return;
        for (CosmeticsBridge.Item item : items) {
            if (item.equipped()) {
                select(item, bridge);
                return;
            }
        }
        if (!items.isEmpty()) select(items.get(0), bridge);
    }

    private void ensureVisibleSelection(List<CosmeticsBridge.Item> items, CosmeticsBridge bridge) {
        List<CosmeticsBridge.Item> visible = filtered(items);
        for (CosmeticsBridge.Item item : visible) {
            if (item.id().equals(selectedId)) return;
        }
        if (!visible.isEmpty()) select(visible.get(0), bridge);
    }

    private void select(CosmeticsBridge.Item item, CosmeticsBridge bridge) {
        selectedId = item.id();
        bridge.previewItem(selectedId);
    }

    private void drawCatalog(UiFrame ui, CosmeticsBridge bridge, List<CosmeticsBridge.Item> all,
                             float x, float y, float w, float h) {
        float filtersW = 132f;
        float filtersX = x;
        ui.canvas().fillRoundRect(filtersX, y + 7f, filtersW, 20f, Metrics.CTL_RADIUS,
                ui.theme().layer());
        float filterX = filtersX + 2f;
        filterX = categoryTab(ui, bridge, all, filterX, y + 9f, "all", 38f);
        filterX = categoryTab(ui, bridge, all, filterX, y + 9f, "cape", 42f);
        categoryTab(ui, bridge, all, filterX, y + 9f, "back", 44f);

        List<CosmeticsBridge.Item> visible = filtered(all);
        float listY = y + TOOLBAR_H;
        float listH = h - TOOLBAR_H;
        int columns = 2;
        float cardW = (w - GAP * (columns - 1)) / columns;
        int rows = (visible.size() + columns - 1) / columns;
        float contentH = Math.max(listH, rows * (CARD_H + GAP) - GAP);
        float scroll = itemScroll.begin(ui, x, listY, w, listH, contentH);

        if (visible.isEmpty()) {
            String empty = bridge.i18n(ownedOnly ? "cosmetics.empty.owned" : "cosmetics.empty.store");
            FontHandle font = ui.font(12);
            ui.canvas().drawString(font, empty, x + (w - font.measure(empty)) / 2f,
                    listY + listH / 2f - font.lineHeight() / 2f, ui.theme().textDisabled());
        }
        for (int i = 0; i < visible.size(); i++) {
            int row = i / columns;
            int column = i % columns;
            float cardX = x + column * (cardW + GAP);
            float cardY = listY + scroll + row * (CARD_H + GAP);
            if (cardY + CARD_H < listY || cardY > listY + listH) continue;
            CosmeticsBridge.Item item = visible.get(i);
            drawItem(ui, bridge, item, cardX, cardY, cardW, CARD_H);
            if (ui.clicked(cardX, cardY, cardW, CARD_H)) select(item, bridge);
        }
        itemScroll.end(ui);
    }

    private float categoryTab(UiFrame ui, CosmeticsBridge bridge, List<CosmeticsBridge.Item> all,
                              float x, float y, String id, float w) {
        boolean selected = id.equals(category);
        boolean hover = ui.hovered(x, y, w, 16f);
        if (selected) ui.canvas().fillRoundRect(x, y, w, 16f, 4f, ui.theme().layerActive());
        FontHandle font = ui.font(12);
        String label = bridge.i18n("cosmetics.filter." + id);
        ui.canvas().drawString(font, fit(font, label, w - 6f),
                x + (w - Math.min(font.measure(label), w - 6f)) / 2f, y + 5f,
                selected || hover ? ui.theme().textPrimary() : ui.theme().textSecondary());
        if (ui.clicked(x, y, w, 16f)) {
            category = id;
            itemScroll.setOffset(0f);
            ensureVisibleSelection(all, bridge);
        }
        return x + w + 2f;
    }

    private void drawItem(UiFrame ui, CosmeticsBridge bridge, CosmeticsBridge.Item item,
                          float x, float y, float w, float h) {
        boolean selected = item.id().equals(selectedId);
        boolean hover = ui.hovered(x, y, w, h);
        if (selected) Chrome.selectedSurface(ui, x, y, w, h, Metrics.CARD_RADIUS);
        else Chrome.card(ui, x, y, w, h, hover, false);

        float footerH = 28f;
        float visualH = h - footerH;
        ui.canvas().fillRoundRect(x + 5f, y + 4f, w - 10f, visualH - 8f, 3f,
                ui.theme().layerActive());
        bridge.paintItemPreview(ui, item, x + 5f, y + 4f, w - 10f, visualH - 8f);
        Chrome.hairlineH(ui, x + 4f, y + visualH, w - 8f);

        if (item.equipped()) {
            ui.canvas().fillCircle(x + w - 10f, y + 9f, 5f, ui.theme().accent());
            GlyphIcons.draw(ui, "check", x + w - 12.5f, y + 6.5f, 5f, ui.theme().white());
        }

        String meta = !item.owned() || item.builtin() ? price(bridge, item) : "";
        FontHandle metaFont = ui.font(10);
        float metaW = Math.min(metaFont.measure(meta), w * 0.42f);
        FontHandle nameFont = ui.font(14);
        String name = fit(nameFont, item.name(), w - metaW - (meta.isEmpty() ? 16f : 19f));
        FontBold.draw(ui, 14, name, x + 8f, y + visualH + 7f, ui.theme().textPrimary());
        if (!meta.isEmpty()) {
            ui.canvas().drawString(metaFont, fit(metaFont, meta, w * 0.42f), x + w - metaW - 8f,
                    y + visualH + 8f, item.builtin() ? ui.theme().ok() : ui.theme().textDisabled());
        }
    }

    private void drawPreview(UiFrame ui, CosmeticsBridge bridge, CosmeticsBridge.Item item,
                             float x, float y, float w, float h) {
        float pad = 9f;
        float innerX = x + pad;
        float innerW = w - pad * 2f;
        String name = item == null ? bridge.i18n("cosmetics.none") : item.name();
        FontBold.draw(ui, 17, fit(ui.font(17), name, innerW - 22f), innerX, y + 7f,
                ui.theme().textPrimary());
        if (item != null) {
            String meta = bridge.i18n(categoryKey(item.category()));
            if (!item.owned()) meta += "  ·  " + price(bridge, item);
            ui.canvas().drawString(ui.font(12), fit(ui.font(12), meta, innerW - 22f), innerX, y + 21f,
                    ui.theme().textSecondary());
        }

        boolean resetHover = ui.hovered(x + w - 24f, y + 7f, 16f, 16f);
        Chrome.ghostButton(ui, x + w - 24f, y + 7f, 16f, 16f, resetHover);
        GlyphIcons.draw(ui, "reset", x + w - 19.5f, y + 11.5f, 7f,
                resetHover ? ui.theme().textPrimary() : ui.theme().textSecondary());
        if (ui.clicked(x + w - 24f, y + 7f, 16f, 16f)) previewYaw = 180f;

        boolean back = item != null && isBack(item.category());
        float footerH = back ? 72f : 50f;
        float stageY = y + TOOLBAR_H;
        float stageH = h - TOOLBAR_H - footerH;
        ui.canvas().fillRect(x + 0.5f, stageY, w - 0.5f, stageH, ui.theme().layer());
        drawGrid(ui, x + 0.5f, stageY, w - 0.5f, stageH);
        rotatePreview(ui, x, stageY, w, stageH);
        bridge.paintPlayerPreview(ui, x, stageY, w, stageH, previewYaw);

        float footerY = stageY + stageH;
        Chrome.hairlineH(ui, x, footerY, w);
        if (back) drawBackSettings(ui, bridge, innerX, footerY + 7f, innerW);
        else drawCapeSettings(ui, bridge, innerX, footerY + 7f, innerW);

        String status = bridge.statusMessage();
        if (status != null && !status.isEmpty()) {
            ui.canvas().drawString(ui.font(10), fit(ui.font(10), status, innerW - 94f), innerX,
                    y + h - 17f, ui.theme().textDisabled());
        }
        drawAction(ui, bridge, item, x + w - 98f, y + h - 25f, 90f);
    }

    private void drawGrid(UiFrame ui, float x, float y, float w, float h) {
        float step = 14f;
        for (float lineX = x + step; lineX < x + w; lineX += step) {
            ui.canvas().fillRect(lineX, y, 0.5f, h, ui.theme().grid());
        }
        for (float lineY = y + step; lineY < y + h; lineY += step) {
            ui.canvas().fillRect(x, lineY, w, 0.5f, ui.theme().grid());
        }
        ui.canvas().fillRect(x + w * 0.14f, y + h * 0.9f, w * 0.72f, 0.5f, ui.theme().stroke());
    }

    private void rotatePreview(UiFrame ui, float x, float y, float w, float h) {
        boolean dragging = ui.input().beginDrag(previewDrag, 0, x, y, w, h)
                || ui.input().isDragging(previewDrag);
        if (dragging && ui.input().isButtonDown(0)) {
            if (!previewDragging) {
                previewDragX = ui.input().mouseX();
                previewDragYaw = previewYaw;
                previewDragging = true;
            }
            previewYaw = wrapDegrees(previewDragYaw + (ui.input().mouseX() - previewDragX) * 0.8f);
        } else {
            ui.input().releaseDrag(previewDrag);
            previewDragging = false;
        }
    }

    private void drawBackSettings(UiFrame ui, CosmeticsBridge bridge, float x, float y, float w) {
        FontHandle font = ui.font(12);
        String visible = bridge.i18n("cosmetics.wings.visible");
        ui.canvas().drawString(font, visible, x, y + 4f, ui.theme().textSecondary());
        boolean enabled = bridge.wingsEnabled();
        boolean next = Chrome.toggle(ui, x + font.measure(visible) + 7f, y, enabled);
        if (next != enabled) bridge.setWingsEnabled(next);

        boolean adjustable = bridge.wingScaleAdjustable();
        String scaleLabel = bridge.i18n("cosmetics.wings.scale");
        ui.canvas().drawString(font, scaleLabel, x, y + 25f,
                enabled && adjustable ? ui.theme().textSecondary() : ui.theme().textDisabled());
        String scaleValue = Math.round(bridge.wingScale() * 100f) + "%";
        float valueW = font.measure(scaleValue);
        ui.canvas().drawString(font, scaleValue, x + w - valueW, y + 25f,
                enabled ? ui.theme().textSecondary() : ui.theme().textDisabled());
        float sliderX = x + Math.min(74f, font.measure(scaleLabel) + 8f);
        float sliderW = Math.max(30f, w - (sliderX - x) - valueW - 7f);
        float scale = bridge.wingScale();
        if (adjustable) {
            scale = Chrome.slider(ui, scaleDrag, sliderX, y + 21f, sliderW, scale);
        } else {
            Chrome.slider(ui, sliderX, y + 21f, sliderW, scale, false);
        }
        if (enabled && adjustable && scale != bridge.wingScale()) bridge.setWingScale(scale);
    }

    private void drawCapeSettings(UiFrame ui, CosmeticsBridge bridge, float x, float y, float w) {
        FontHandle font = ui.font(12);
        String label = bridge.i18n("cosmetics.cape.animation");
        ui.canvas().drawString(font, label, x, y + 4f, ui.theme().textSecondary());
        boolean enabled = bridge.capeEnabled();
        boolean next = Chrome.toggle(ui, x + font.measure(label) + 7f, y, enabled);
        if (next != enabled) bridge.setCapeEnabled(next);
    }

    private void drawAction(UiFrame ui, CosmeticsBridge bridge, CosmeticsBridge.Item item,
                            float x, float y, float w) {
        if (item == null) return;
        if (bridge.purchasePending()) {
            Chrome.button(ui, x, y, w, 18f, bridge.i18n("cosmetics.purchasing"),
                    Chrome.ButtonStyle.DEFAULT);
            return;
        }
        if (item.equipped()) {
            Chrome.button(ui, x, y, w, 18f, bridge.i18n("cosmetics.equipped"),
                    Chrome.ButtonStyle.DEFAULT);
            return;
        }
        if (item.owned()) {
            if (Chrome.button(ui, x, y, w, 18f, bridge.i18n("cosmetics.equip"),
                    Chrome.ButtonStyle.PRIMARY)) bridge.equipItem(item.id());
            return;
        }
        String label = bridge.signedIn()
                ? bridge.i18n("cosmetics.buy") + "  ·  " + price(bridge, item)
                : bridge.i18n("cosmetics.login.required");
        if (Chrome.button(ui, x, y, w, 18f, label,
                bridge.signedIn() ? Chrome.ButtonStyle.PRIMARY : Chrome.ButtonStyle.DEFAULT)
                && bridge.signedIn()) {
            bridge.purchaseItem(item.id());
        }
    }

    private List<CosmeticsBridge.Item> filtered(List<CosmeticsBridge.Item> items) {
        List<CosmeticsBridge.Item> result = new ArrayList<CosmeticsBridge.Item>();
        for (CosmeticsBridge.Item item : items) {
            if (ownedOnly && !item.owned()) continue;
            if ("cape".equals(category) && !"cape".equals(item.category())) continue;
            if ("back".equals(category) && !isBack(item.category())) continue;
            result.add(item);
        }
        return result;
    }

    private CosmeticsBridge.Item selected(List<CosmeticsBridge.Item> items) {
        return find(items, selectedId);
    }

    private CosmeticsBridge.Item find(List<CosmeticsBridge.Item> items, String id) {
        if (id == null) return null;
        for (CosmeticsBridge.Item item : items) if (id.equals(item.id())) return item;
        return null;
    }

    private boolean isBack(String itemCategory) {
        return "wings".equals(itemCategory) || "elytra".equals(itemCategory);
    }

    private String categoryKey(String itemCategory) {
        return "cape".equals(itemCategory) ? "cosmetics.cape" : "cosmetics.wings";
    }

    private String price(CosmeticsBridge bridge, CosmeticsBridge.Item item) {
        if (item.builtin() || "0".equals(item.price()) || "0.00".equals(item.price())) {
            return bridge.i18n("cosmetics.free");
        }
        return item.price() + " " + bridge.i18n("cosmetics.coins");
    }

    private String fit(FontHandle font, String value, float maxWidth) {
        if (value == null) return "";
        if (font.measure(value) <= maxWidth) return value;
        String ellipsis = "...";
        int end = value.length();
        while (end > 0 && font.measure(value.substring(0, end) + ellipsis) > maxWidth) end--;
        return end == 0 ? ellipsis : value.substring(0, end) + ellipsis;
    }

    private float wrapDegrees(float degrees) {
        float wrapped = degrees % 360f;
        return wrapped < 0f ? wrapped + 360f : wrapped;
    }

    private boolean back(UiFrame ui, float x, float y) {
        boolean hover = ui.hovered(x, y, 16f, 16f);
        Chrome.ghostButton(ui, x, y, 16f, 16f, hover);
        GlyphIcons.draw(ui, "back", x + 4.5f, y + 4.5f, 7f,
                hover ? ui.theme().textPrimary() : ui.theme().textSecondary());
        return ui.clicked(x, y, 16f, 16f);
    }
}

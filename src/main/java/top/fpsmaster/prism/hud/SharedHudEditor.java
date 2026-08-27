package top.fpsmaster.prism.hud;

import top.fpsmaster.prism.canvas.FontHandle;
import top.fpsmaster.prism.input.Keys;
import top.fpsmaster.prism.input.PointerEvent;
import top.fpsmaster.prism.theme.Argb;
import top.fpsmaster.prism.widget.Chrome;
import top.fpsmaster.prism.widget.UiFrame;

import java.util.List;

/** Shared HUD selection, movement, scaling, snapping and editor chrome. */
public final class SharedHudEditor {
    private static final float GRID = 8f;
    private static final float SNAP = 5f;
    private static final float HANDLE = 8f;
    public static final float CONTENT_TOP = 25f;

    private String selectedId;
    private String activeId;
    private boolean resizing;
    private float dragOffsetX;
    private float dragOffsetY;
    private float activeX;
    private float activeY;
    private float activeScale;
    private float guideX = Float.NaN;
    private float guideY = Float.NaN;
    private boolean dirty;

    /** HUD surface rectangle in editor space; see HudEditorBridge.contentBounds. */
    private float contentX;
    private float contentY;
    private float contentW;
    private float contentH;

    public void draw(UiFrame ui, HudEditorBridge bridge) {
        float width = ui.host().width();
        float height = ui.host().height();
        float[] bounds = bridge.contentBounds(width, height);
        contentX = bounds[0];
        contentY = bounds[1];
        contentW = bounds[2];
        contentH = bounds[3];
        ui.canvas().fillRect(0f, 0f, width, height, Argb.of(178, 5, 8, 10));
        drawGrid(ui, width, height);

        List<HudEditorBridge.Item> items = bridge.items();
        HudEditorBridge.Item selected = find(items, selectedId);
        if (selectedId != null && selected == null) {
            selectedId = null;
        }

        handlePress(ui, bridge, items, selected, width, height);
        handleActive(ui, bridge, items, width, height);
        items = bridge.items();

        for (HudEditorBridge.Item item : items) {
            bridge.paintPreview(item.id, item.x, item.y, item.scale);
            drawBounds(ui, item, item.id.equals(selectedId));
        }
        drawGuides(ui, width, height);
        drawHeader(ui, bridge, find(items, selectedId), width);

        if (ui.input().consumeKey(Keys.ESCAPE)) {
            close(bridge);
        }
    }

    public void close(HudEditorBridge bridge) {
        if (dirty) {
            bridge.save();
            dirty = false;
        }
        bridge.close();
    }

    public String selectedId() {
        return selectedId;
    }

    private void handlePress(UiFrame ui, HudEditorBridge bridge, List<HudEditorBridge.Item> items,
                             HudEditorBridge.Item selected, float width, float height) {
        if (selected != null) {
            float closeX = selected.x + selected.width() - 6f;
            float closeY = selected.y - 7f;
            if (ui.clicked(closeX, closeY, 13f, 13f)) {
                bridge.disable(selected.id);
                selectedId = null;
                activeId = null;
                dirty = true;
                return;
            }
        }

        PointerEvent press = ui.input().consumePressInBounds(0f, CONTENT_TOP, width, height - CONTENT_TOP, 0);
        if (press == null) {
            return;
        }
        for (int i = items.size() - 1; i >= 0; i--) {
            HudEditorBridge.Item item = items.get(i);
            if (!inside(item.x, item.y, item.width(), item.height(), press.x, press.y)) {
                continue;
            }
            selectedId = item.id;
            activeId = item.id;
            activeX = item.x;
            activeY = item.y;
            activeScale = item.scale;
            resizing = item.scalable && inside(item.x + item.width() - HANDLE, item.y + item.height() - HANDLE,
                    HANDLE * 2f, HANDLE * 2f, press.x, press.y);
            dragOffsetX = press.x - item.x;
            dragOffsetY = press.y - item.y;
            return;
        }
        selectedId = null;
    }

    private void handleActive(UiFrame ui, HudEditorBridge bridge, List<HudEditorBridge.Item> items,
                              float width, float height) {
        if (activeId == null) {
            return;
        }
        HudEditorBridge.Item item = find(items, activeId);
        if (item == null || !ui.input().isButtonDown(0)) {
            activeId = null;
            guideX = Float.NaN;
            guideY = Float.NaN;
            return;
        }

        float x = item.x;
        float y = item.y;
        float scale = item.scale;
        if (resizing) {
            float sx = (ui.input().mouseX() - activeX) / item.baseWidth;
            float sy = (ui.input().mouseY() - activeY) / item.baseHeight;
            scale = clamp(Math.max(sx, sy), item.minScale, item.maxScale);
            x = Math.min(activeX, Math.max(contentX, contentX + contentW - item.baseWidth * scale));
            y = Math.min(activeY, Math.max(contentY, contentY + contentH - item.baseHeight * scale));
            guideX = Float.NaN;
            guideY = Float.NaN;
        } else {
            x = clamp(ui.input().mouseX() - dragOffsetX, contentX, Math.max(contentX, contentX + contentW - item.width()));
            y = clamp(ui.input().mouseY() - dragOffsetY, contentY, Math.max(contentY, contentY + contentH - item.height()));
            float[] snapped = snap(item, items, x, y);
            x = snapped[0];
            y = snapped[1];
        }
        bridge.setPlacement(item.id, x, y, scale, width, height);
        dirty = true;
    }

    private float[] snap(HudEditorBridge.Item item, List<HudEditorBridge.Item> items,
                         float x, float y) {
        float w = item.width();
        float h = item.height();
        float gridX = Math.round(x / GRID) * GRID;
        float gridY = Math.round(y / GRID) * GRID;
        guideX = Float.NaN;
        guideY = Float.NaN;
        if (Math.abs(gridX - x) <= 2f) x = gridX;
        if (Math.abs(gridY - y) <= 2f) y = gridY;

        float bestX = SNAP;
        float bestY = SNAP;
        float[] vertical = new float[3 + items.size() * 3];
        float[] horizontal = new float[3 + items.size() * 3];
        int vc = 0;
        int hc = 0;
        vertical[vc++] = contentX; vertical[vc++] = contentX + contentW / 2f; vertical[vc++] = contentX + contentW;
        horizontal[hc++] = contentY; horizontal[hc++] = contentY + contentH / 2f; horizontal[hc++] = contentY + contentH;
        for (HudEditorBridge.Item other : items) {
            if (other.id.equals(item.id)) continue;
            vertical[vc++] = other.x; vertical[vc++] = other.x + other.width() / 2f; vertical[vc++] = other.x + other.width();
            horizontal[hc++] = other.y; horizontal[hc++] = other.y + other.height() / 2f; horizontal[hc++] = other.y + other.height();
        }
        float[] xp = {x, x + w / 2f, x + w};
        float[] xo = {0f, w / 2f, w};
        for (int p = 0; p < xp.length; p++) for (int i = 0; i < vc; i++) {
            float d = Math.abs(xp[p] - vertical[i]);
            if (d <= bestX) { bestX = d; x = vertical[i] - xo[p]; guideX = vertical[i]; }
        }
        float[] yp = {y, y + h / 2f, y + h};
        float[] yo = {0f, h / 2f, h};
        for (int p = 0; p < yp.length; p++) for (int i = 0; i < hc; i++) {
            float d = Math.abs(yp[p] - horizontal[i]);
            if (d <= bestY) { bestY = d; y = horizontal[i] - yo[p]; guideY = horizontal[i]; }
        }
        return new float[]{clamp(x, contentX, Math.max(contentX, contentX + contentW - w)),
                clamp(y, contentY, Math.max(contentY, contentY + contentH - h))};
    }

    private void drawGrid(UiFrame ui, float width, float height) {
        for (float x = GRID; x < width; x += GRID) {
            ui.canvas().fillRect(x, CONTENT_TOP, 0.5f, height - CONTENT_TOP, ui.theme().grid());
        }
        for (float y = CONTENT_TOP + GRID; y < height; y += GRID) {
            ui.canvas().fillRect(0f, y, width, 0.5f, ui.theme().grid());
        }
    }

    private void drawBounds(UiFrame ui, HudEditorBridge.Item item, boolean selected) {
        int border = selected ? ui.theme().accent() : Argb.of(110, 220, 230, 235);
        int fill = selected ? ui.theme().accentSoft() : Argb.of(20, 255, 255, 255);
        ui.canvas().fillRect(item.x, item.y, item.width(), item.height(), fill);
        ui.canvas().strokeRoundRect(item.x + 0.5f, item.y + 0.5f, item.width() - 1f, item.height() - 1f,
                3f, 1f, border);
        if (selected) {
            ui.canvas().fillRoundRect(item.x + item.width() - HANDLE, item.y + item.height() - HANDLE,
                    HANDLE, HANDLE, 2f, ui.theme().accent());
            FontHandle label = ui.font(11);
            ui.canvas().drawString(label, item.label + "  " + oneDecimal(item.scale) + "×",
                    item.x, Math.max(CONTENT_TOP, item.y - 10f), ui.theme().textPrimary());
            float closeX = item.x + item.width() - 6f;
            float closeY = item.y - 7f;
            ui.canvas().fillRoundRect(closeX, closeY, 13f, 13f, 6.5f, ui.theme().danger());
            FontHandle close = ui.font(12);
            ui.canvas().drawString(close, "×", closeX + (13f - close.measure("×")) / 2f,
                    Chrome.textY(closeY, 13f, close), ui.theme().white());
        }
    }

    private void drawGuides(UiFrame ui, float width, float height) {
        if (!Float.isNaN(guideX)) ui.canvas().fillRect(guideX, CONTENT_TOP, 1f, height - CONTENT_TOP, ui.theme().accent());
        if (!Float.isNaN(guideY)) ui.canvas().fillRect(0f, guideY, width, 1f, ui.theme().accent());
    }

    private void drawHeader(UiFrame ui, HudEditorBridge bridge, HudEditorBridge.Item selected, float width) {
        ui.canvas().fillRect(0f, 0f, width, CONTENT_TOP, ui.theme().opaquePanelBase());
        Chrome.hairlineH(ui, 0f, CONTENT_TOP - 0.5f, width);
        FontHandle title = ui.font(15);
        ui.canvas().drawString(title, bridge.i18n("hud.editor.title"), 9f, Chrome.textY(0f, CONTENT_TOP, title), ui.theme().textPrimary());
        if (selected != null) {
            FontHandle detail = ui.font(11);
            String text = selected.label + " · " + Math.round(selected.x) + ", " + Math.round(selected.y)
                    + " · " + oneDecimal(selected.scale) + "×";
            ui.canvas().drawString(detail, text, 92f, Chrome.textY(0f, CONTENT_TOP, detail), ui.theme().textSecondary());
        }
        if (Chrome.button(ui, width - 53f, 4f, 44f, 17f, bridge.i18n("hud.editor.done"), Chrome.ButtonStyle.PRIMARY)) {
            close(bridge);
        }
    }

    private static HudEditorBridge.Item find(List<HudEditorBridge.Item> items, String id) {
        if (id == null) return null;
        for (HudEditorBridge.Item item : items) if (id.equals(item.id)) return item;
        return null;
    }

    private static boolean inside(float x, float y, float w, float h, float px, float py) {
        return px >= x && py >= y && px <= x + w && py <= y + h;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String oneDecimal(float value) {
        return String.valueOf(Math.round(value * 10f) / 10f);
    }
}

package top.fpsmaster.prism.screen;

import top.fpsmaster.prism.anim.Anim;
import top.fpsmaster.prism.canvas.Canvas;
import top.fpsmaster.prism.canvas.FontHandle;
import top.fpsmaster.prism.icon.GlyphIcons;
import top.fpsmaster.prism.theme.Argb;
import top.fpsmaster.prism.theme.Metrics;
import top.fpsmaster.prism.widget.Chrome;
import top.fpsmaster.prism.widget.UiFrame;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Edge/Nova shared main menu: hero wordmark, tile dock, account chip, top actions.
 * Layout matches {@code docs/prototypes/main-menu.html} / Edge {@code MainMenu}.
 */
public final class SharedMainMenu {
    public static final float TILE_H = 64f;
    public static final float TILE_W = 59f;
    public static final float TILE_GAP = 6f;
    public static final float CONTINUE_W = 125f;
    public static final float QUIT_W = 42f;
    public static final float CHIP_X = 12f;
    public static final float CHIP_Y = 11f;
    public static final float CHIP_H = 22f;
    private static final float HOVER_SPEED = 0.32f;
    private static final float LIFT = 1.5f;
    private static final SharedMainMenu INSTANCE = new SharedMainMenu();

    private final Map<String, Float> hover = new HashMap<String, Float>();
    private long lastNanos;

    private SharedMainMenu() {
    }

    public static void draw(UiFrame ui, MenuBridge bridge) {
        INSTANCE.paint(ui, bridge);
    }

    private void paint(UiFrame ui, MenuBridge bridge) {
        float dt = dt(ui);
        float w = ui.host().width();
        float h = ui.host().height();
        float heroX = w * 0.06f;
        drawHero(ui, bridge, heroX);
        drawDock(ui, bridge, heroX, dt);
        drawFooter(ui, bridge, heroX);
        drawTopActions(ui, bridge, dt);
        drawAccountChip(ui, bridge, dt);
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

    private static void drawHero(UiFrame ui, MenuBridge bridge, float heroX) {
        float h = ui.host().height();
        float editionBaseline = h - 109f;
        float logoY = editionBaseline - 12f - 26f;
        float greetY = logoY - 14f;
        ui.canvas().drawString(ui.font(14), greeting(bridge), heroX, greetY, ui.theme().textSecondary());
        drawTracked(ui, ui.font(52), "FPSMASTER", heroX, logoY, 2.6f, ui.theme().textPrimary(), true);
        float editionW = drawTracked(ui, ui.font(12), bridge.edition(), heroX, editionBaseline, 2.4f,
                ui.theme().accentText(), false);
        float lineX = heroX + editionW + 5f;
        for (int i = 0; i < 24; i++) {
            int a = (int) (255 * (1f - i / 24f));
            ui.canvas().fillRect(lineX + i, editionBaseline + 3f, 1f, 0.5f, Argb.of(a, 89, 101, 241));
        }
    }

    private static String greeting(MenuBridge bridge) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String key = hour < 6 || hour >= 19 ? "mainmenu.greet.evening"
                : hour < 12 ? "mainmenu.greet.morning" : "mainmenu.greet.afternoon";
        return bridge.i18n(key).replace("%s", bridge.playerName());
    }

    private static float drawTracked(UiFrame ui, FontHandle font, String text, float x, float y,
                                     float tracking, int color, boolean bold) {
        float cursor = x;
        Canvas canvas = ui.canvas();
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            canvas.drawString(font, ch, cursor, y, color);
            if (bold) {
                canvas.drawString(font, ch, cursor + 0.5f, y, color);
            }
            cursor += font.measure(ch) + tracking;
        }
        return cursor - x - tracking;
    }

    private static void drawFooter(UiFrame ui, MenuBridge bridge, float heroX) {
        float w = ui.host().width();
        float h = ui.host().height();
        FontHandle font = ui.font(11);
        int color = ui.theme().textDisabled();
        String left = "FPSMaster " + bridge.edition() + " " + bridge.version() + " · " + bridge.minecraftLabel();
        ui.canvas().drawString(font, left, heroX, h - 16f, color);
        String mojang = "Copyright Mojang AB. Do not distribute!";
        ui.canvas().drawString(font, mojang, w - heroX - font.measure(mojang), h - 16f, color);
    }

    private void drawDock(UiFrame ui, MenuBridge bridge, float heroX, float dt) {
        float w = ui.host().width();
        float h = ui.host().height();
        float dockY = h - 32f - TILE_H;
        int tileCount = 3 + (bridge.showReplays() ? 1 : 0) + (bridge.showDevtools() ? 1 : 0);
        float available = w - heroX * 2f - QUIT_W - TILE_GAP;
        MenuBridge.ContinueServer cont = bridge.continueServer();
        boolean showContinue = cont != null
                && CONTINUE_W + TILE_GAP + tileCount * (TILE_W + TILE_GAP) <= available + TILE_GAP;
        float tileW = TILE_W;
        float tilesSpan = (showContinue ? CONTINUE_W + TILE_GAP : 0f) + tileCount * (tileW + TILE_GAP) - TILE_GAP;
        if (tilesSpan > available) {
            tileW = Math.max(44f, (available - (showContinue ? CONTINUE_W + TILE_GAP : 0f)) / tileCount - TILE_GAP);
        }
        float x = heroX;
        if (showContinue) {
            drawContinue(ui, bridge, cont, x, dockY, dt);
            x += CONTINUE_W + TILE_GAP;
        }
        x += drawTile(ui, bridge, x, dockY, tileW, "box", "single", bridge.i18n("mainmenu.single"), dt, new Runnable() {
            public void run() {
                bridge.singleplayer();
            }
        });
        x += drawTile(ui, bridge, x, dockY, tileW, "globe", "multi", bridge.i18n("mainmenu.multi"), dt, new Runnable() {
            public void run() {
                bridge.multiplayer();
            }
        });
        if (bridge.showReplays()) {
            x += drawTile(ui, bridge, x, dockY, tileW, "replay", "replays", bridge.i18n("mainmenu.replays"), dt, new Runnable() {
                public void run() {
                    bridge.replays();
                }
            });
        }
        x += drawTile(ui, bridge, x, dockY, tileW, "sliders", "settings", bridge.i18n("mainmenu.settings"), dt, new Runnable() {
            public void run() {
                bridge.settings();
            }
        });
        if (bridge.showDevtools()) {
            drawTile(ui, bridge, x, dockY, tileW, "wrench", "devtools", bridge.i18n("mainmenu.devtools"), dt, new Runnable() {
                public void run() {
                    bridge.devtools();
                }
            });
        }
        drawQuit(ui, bridge, w - heroX - QUIT_W, dockY, dt);
    }

    private void tileSurface(UiFrame ui, float x, float y, float width, float t, boolean danger) {
        float ty = y - LIFT * t;
        int stroke = Argb.lerp(ui.theme().stroke(), ui.theme().strokeStrong(), t);
        int fill = Argb.lerp(ui.theme().glass(), Argb.of(230, 28, 28, 28), t);
        ui.canvas().fillRoundRect(x - 0.5f, ty - 0.5f, width + 1f, TILE_H + 1f, Metrics.PANEL_RADIUS + 1f, stroke);
        ui.canvas().fillRoundRect(x, ty, width, TILE_H, Metrics.PANEL_RADIUS, fill);
        if (t > 0.01f) {
            float inset = TILE_H * (0.24f - 0.10f * t);
            float barH = TILE_H - inset * 2f;
            int bar = Argb.mulAlpha(danger ? ui.theme().danger() : ui.theme().accent(), t);
            ui.canvas().fillRoundRect(x, ty + inset, 1.5f, barH, 1f, bar);
        }
    }

    private float drawTile(UiFrame ui, MenuBridge bridge, float x, float y, float width, String icon,
                           String id, String label, float dt, Runnable action) {
        boolean hovered = ui.hovered(x, y, width, TILE_H);
        float t = hoverT("tile." + id, hovered, dt);
        tileSurface(ui, x, y, width, t, false);
        float ty = y - LIFT * t;
        int iconColor = Argb.lerp(ui.theme().textSecondary(), ui.theme().accentText(), t);
        GlyphIcons.draw(ui, icon, x + 8f, ty + TILE_H - 31f, 11f, iconColor);
        ui.canvas().drawString(ui.font(14), label, x + 8f, ty + TILE_H - 15f, ui.theme().textPrimary());
        if (clicked(ui, bridge, x, y, width, TILE_H)) {
            action.run();
        }
        return width + TILE_GAP;
    }

    private static boolean clicked(UiFrame ui, MenuBridge bridge, float x, float y, float w, float h) {
        return bridge.interactive() && ui.clicked(x, y, w, h);
    }

    private void drawContinue(UiFrame ui, MenuBridge bridge, MenuBridge.ContinueServer server,
                              float x, float y, float dt) {
        boolean hovered = ui.hovered(x, y, CONTINUE_W, TILE_H);
        float t = hoverT("tile.continue", hovered, dt);
        tileSurface(ui, x, y, CONTINUE_W, t, false);
        float ty = y - LIFT * t;
        drawTracked(ui, ui.font(10), bridge.i18n("mainmenu.continue"), x + 8f, ty + 8f, 0.7f,
                ui.theme().accentText(), false);
        GlyphIcons.draw(ui, "play", x + CONTINUE_W - 15f, ty + 7f, 8f,
                Argb.lerp(ui.theme().textSecondary(), ui.theme().accentText(), t));
        FontHandle big = ui.font(18);
        ui.canvas().drawString(big, server.name, x + 8f, ty + TILE_H - 28f, ui.theme().textPrimary());
        ui.canvas().drawString(big, server.name, x + 8.4f, ty + TILE_H - 28f, ui.theme().textPrimary());
        FontHandle meta = ui.font(11);
        float metaY = ty + TILE_H - 13f;
        ui.canvas().drawString(meta, server.address, x + 8f, metaY, ui.theme().textSecondary());
        if (server.pingMs > 0L) {
            float ipW = meta.measure(server.address);
            Chrome.pingBars(ui, x + 8f + ipW + 5f, metaY + 5f, Chrome.pingLevel(server.pingMs),
                    Chrome.pingColor(ui, server.pingMs));
            ui.canvas().drawString(meta, server.pingMs + "ms",
                    x + 8f + ipW + 5f + 10f + 4f, metaY, ui.theme().textSecondary());
        }
        if (clicked(ui, bridge, x, y, CONTINUE_W, TILE_H)) {
            bridge.continueConnect();
        }
    }

    private void drawQuit(UiFrame ui, MenuBridge bridge, float x, float y, float dt) {
        boolean hovered = ui.hovered(x, y, QUIT_W, TILE_H);
        float t = hoverT("tile.quit", hovered, dt);
        tileSurface(ui, x, y, QUIT_W, t, true);
        float ty = y - LIFT * t;
        int iconColor = Argb.lerp(ui.theme().textDisabled(), ui.theme().danger(), t);
        GlyphIcons.draw(ui, "power", x + QUIT_W / 2f - 5.5f, ty + 18f, 11f, iconColor);
        FontHandle font = ui.font(12);
        String q = bridge.i18n("mainmenu.quit");
        ui.canvas().drawString(font, q, x + (QUIT_W - font.measure(q)) / 2f, ty + 38f, ui.theme().textSecondary());
        if (clicked(ui, bridge, x, y, QUIT_W, TILE_H)) {
            bridge.quit();
        }
    }

    private void drawTopActions(UiFrame ui, MenuBridge bridge, float dt) {
        float size = 19f;
        float y = 11f;
        float w = ui.host().width();
        float musicX = w - 12f - size;
        float bgX = musicX - size - 4f;
        actionPill(ui, bridge, "bg", "image", bgX, y, size, dt,
                ui.hovered(bgX, y, size, size), false, new Runnable() {
                    public void run() {
                        bridge.backgrounds();
                    }
                });
        actionPill(ui, bridge, "music", "music", musicX, y, size, dt,
                ui.hovered(musicX, y, size, size), false, new Runnable() {
                    public void run() {
                        bridge.music();
                    }
                });
    }

    private void actionPill(UiFrame ui, MenuBridge bridge, String id, String icon, float x, float y,
                            float size, float dt, boolean on, boolean accent, Runnable action) {
        float t = hoverT("action." + id, on, dt);
        Chrome.pillIconButton(ui, x, y, size, t);
        int from = ui.theme().textSecondary();
        int to = accent ? ui.theme().accentText() : ui.theme().textPrimary();
        GlyphIcons.draw(ui, icon, x + 5.5f, y + 5.5f, 8f, Argb.lerp(from, to, t));
        if (clicked(ui, bridge, x, y, size, size)) {
            action.run();
        }
    }

    public static float chipWidth(UiFrame ui, MenuBridge bridge) {
        FontHandle nameFont = ui.font(14);
        FontHandle subFont = ui.font(11);
        float nameW = Math.max(nameFont.measure(bridge.playerName()), subFont.measure(bridge.accountTypeLabel()));
        return 4f + 14f + 5f + nameW + 4f + 6.5f + 7f;
    }

    private void drawAccountChip(UiFrame ui, MenuBridge bridge, float dt) {
        String username = bridge.playerName();
        FontHandle nameFont = ui.font(14);
        FontHandle subFont = ui.font(11);
        float chipW = chipWidth(ui, bridge);
        float chipX = CHIP_X;
        float chipY = CHIP_Y;
        boolean hover = ui.hovered(chipX, chipY, chipW, CHIP_H) || bridge.accountOpen();
        float t = hoverT("chip", hover, dt);
        ui.canvas().fillRoundRect(chipX - 0.5f, chipY - 0.5f, chipW + 1f, CHIP_H + 1f, CHIP_H / 2f + 1f,
                Argb.lerp(ui.theme().stroke(), ui.theme().strokeStrong(), t));
        ui.canvas().fillRoundRect(chipX, chipY, chipW, CHIP_H, CHIP_H / 2f,
                Argb.lerp(ui.theme().glass(), ui.theme().layerHover(), t * 0.55f));
        bridge.drawAvatar(ui, chipX + 4f, chipY + 4f, 14f);
        ui.canvas().drawString(nameFont, username, chipX + 23f, chipY + 3.5f, ui.theme().textPrimary());
        ui.canvas().drawString(subFont, bridge.accountTypeLabel(), chipX + 23f, chipY + 12f, ui.theme().textSecondary());
        GlyphIcons.draw(ui, "chev-d", chipX + chipW - 12f, chipY + CHIP_H / 2f - 3.25f, 6.5f,
                Argb.lerp(ui.theme().textDisabled(), ui.theme().textSecondary(), t));
        if (clicked(ui, bridge, chipX, chipY, chipW, CHIP_H)) {
            bridge.account();
        }
    }
}

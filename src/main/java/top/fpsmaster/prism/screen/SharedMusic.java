package top.fpsmaster.prism.screen;

import top.fpsmaster.prism.anim.Anim;
import top.fpsmaster.prism.icon.GlyphIcons;
import top.fpsmaster.prism.theme.Argb;
import top.fpsmaster.prism.theme.Metrics;
import top.fpsmaster.prism.widget.Chrome;
import top.fpsmaster.prism.widget.Scroll;
import top.fpsmaster.prism.widget.TextBox;
import top.fpsmaster.prism.widget.UiFrame;

import java.util.Calendar;
import java.util.List;

public final class SharedMusic {
    private static final float NOW_W = 160f;
    private static final int BADGE = 0xFFD9A441;

    private enum Tab {DISCOVER, PLAYLISTS, SEARCH}

    private Tab tab = Tab.DISCOVER;
    private final TextBox search = new TextBox();
    private final TextBox qqId = new TextBox();
    private final TextBox qqKey = new TextBox();
    private final Scroll scroll = new Scroll("music.list");
    private final SharedLyrics lyrics = new SharedLyrics();
    private boolean discoverRequested;
    private boolean loginOpen;
    private boolean immersiveLyrics;
    private boolean settingsOpen;
    private float lyricsOpenT;
    private long lastNanos;

    public SharedMusic() {
        search.setPaintBox(false);
        search.setFontSize(12);
        search.setPadLeft(14f);
        search.setPlaceholder("搜索歌曲、歌手…");
        qqId.setFontSize(12);
        qqId.setPlaceholder("musicid (uin)");
        qqKey.setFontSize(12);
        qqKey.setPlaceholder("musickey (qm_keyst)");
    }

    public boolean cancelOverlay() {
        if (immersiveLyrics || lyricsOpenT > 0f) {
            immersiveLyrics = false;
            return true;
        }
        if (settingsOpen) {
            settingsOpen = false;
            return true;
        }
        if (!loginOpen) {
            return false;
        }
        loginOpen = false;
        return true;
    }

    public boolean draw(UiFrame ui, MusicBridge bridge) {
        float dt = dt(ui);
        lyricsOpenT = Anim.approach(lyricsOpenT, immersiveLyrics ? 1f : 0f, 0.22f, dt);
        if (!discoverRequested) {
            bridge.loadDiscover();
            discoverRequested = true;
        }
        if (loginOpen && bridge.loggedIn()) {
            loginOpen = false;
            bridge.stopLogin();
        }
        float gw = ui.host().width();
        float gh = ui.host().height();
        Chrome.veil(ui, 1f);
        float pw = Math.min(480f, Math.max(320f, gw - 24f));
        float ph = Math.min(290f, Math.max(200f, gh - 32f));
        float px = (gw - pw) / 2f;
        float py = (gh - ph) / 2f;
        Chrome.panel(ui, px, py, pw, ph);

        if (immersiveLyrics || lyricsOpenT > 0f) {
            drawImmersiveLyrics(ui, bridge, px, py, pw, ph, dt, lyricsOpenT);
            return false;
        }
        ui.canvas().fillRect(px + 1, py + 1, NOW_W - 1, ph - 2, Argb.of(40, 0, 0, 0));
        Chrome.hairlineV(ui, px + NOW_W, py + 1, ph - 2);

        if (loginOpen) {
            drawLogin(ui, bridge, px, py, pw, ph);
            return false;
        }

        drawNow(ui, bridge, px, py, NOW_W, ph);

        float clS = 16f;
        float clX = px + pw - 7f - clS;
        float clY = py + 7f;
        boolean clHov = ui.hovered(clX, clY, clS, clS);
        Chrome.ghostButton(ui, clX, clY, clS, clS, clHov);
        GlyphIcons.draw(ui, "close", clX + 4f, clY + 4f, 8f,
                clHov ? ui.theme().textPrimary() : ui.theme().textSecondary());
        if (ui.clicked(clX, clY, clS, clS)) {
            return true;
        }

        float settingsX = clX - 21f;
        boolean settingsHover = ui.hovered(settingsX, clY, clS, clS);
        Chrome.ghostButton(ui, settingsX, clY, clS, clS, settingsHover);
        GlyphIcons.draw(ui, "sliders", settingsX + 4f, clY + 4f, 8f,
                settingsHover || settingsOpen ? ui.theme().accent() : ui.theme().textSecondary());
        if (ui.clicked(settingsX, clY, clS, clS)) {
            settingsOpen = !settingsOpen;
        }

        float bx = px + NOW_W;
        float bw = pw - NOW_W;
        if (settingsOpen) {
            drawSettings(ui, bridge, bx, py, bw, ph);
            return false;
        }
        drawHead(ui, bridge, bx, py, settingsX);
        drawToolbar(ui, bridge, bx, py + 34f, bw);
        float contentX = bx + 14f;
        float contentW = bw - 24f;
        float contentY = py + 34f + Metrics.SEARCH_H + 5f;
        float contentH = py + ph - 8f - contentY;
        drawList(ui, bridge, contentX, contentY, contentW, contentH);
        return false;
    }

    private void drawNow(UiFrame ui, MusicBridge bridge, float x, float y, float w, float h) {
        float cover = 95f;
        float cvX = x + (w - cover) / 2f;
        float cvY = y + 16f;
        ui.canvas().fillRoundRect(cvX, cvY, cover, cover, 10f, ui.theme().layerActive());
        GlyphIcons.draw(ui, "music", cvX + cover / 2f - 12f, cvY + cover / 2f - 12f, 24f,
                ui.theme().textDisabled());
        bridge.paintCover(ui, cvX, cvY, cover);
        if (bridge.hasLyrics() && ui.clicked(cvX, cvY, cover, cover)) {
            immersiveLyrics = true;
        }
        String title = bridge.nowTitle();
        String artist = bridge.status().isEmpty() ? bridge.nowArtist() : bridge.status();
        FontBold.draw(ui, 14, ellipsize(ui.font(14), title, w - 20f),
                x + (w - ui.font(14).measure(ellipsize(ui.font(14), title, w - 20f))) * 0.5f,
                cvY + cover + 10f, ui.theme().textPrimary());
        ui.canvas().drawString(ui.font(11), ellipsize(ui.font(11), artist, w - 20f),
                x + (w - ui.font(11).measure(ellipsize(ui.font(11), artist, w - 20f))) * 0.5f,
                cvY + cover + 20f, bridge.status().isEmpty() ? ui.theme().textSecondary() : BADGE);

        float barX = x + 16f;
        float barW = w - 32f;
        float barY = y + h - 78f;
        float frac = bridge.progress();
        float nt = Chrome.slider(ui, "music.progress", barX, barY, barW, frac);
        if (Math.abs(nt - frac) > 0.004f) {
            bridge.seek(nt);
        }
        ui.canvas().drawString(ui.font(10), formatMs(bridge.positionMs()), barX, barY + 12f,
                ui.theme().textDisabled());
        String dur = formatMs(bridge.durationMs());
        ui.canvas().drawString(ui.font(10), dur, barX + barW - ui.font(10).measure(dur), barY + 12f,
                ui.theme().textDisabled());

        float ctlCy = y + h - 42f;
        float bs = 18f;
        float ps = 24f;
        iconCtl(ui, "lyrics", x + 16f, ctlCy, bs, bridge.lyricsHudEnabled());
        if (ui.clicked(x + 16f, ctlCy - bs / 2f, bs, bs)) {
            bridge.setLyricsHudEnabled(!bridge.lyricsHudEnabled());
        }
        float rowW = bs * 2f + ps + 12f;
        float ix = x + (w - rowW) / 2f;
        iconCtl(ui, "prev", ix, ctlCy, bs, false);
        if (ui.clicked(ix, ctlCy - bs / 2f, bs, bs)) {
            bridge.prev();
        }
        ix += bs + 6f;
        ui.canvas().fillRoundRect(ix, ctlCy - ps / 2f, ps, ps, ps / 2f, ui.theme().accent());
        GlyphIcons.draw(ui, bridge.playing() && !bridge.paused() ? "pause" : "play",
                ix + (ps - 10f) / 2f, ctlCy - 5f, 10f, ui.theme().white());
        if (ui.clicked(ix, ctlCy - ps / 2f, ps, ps)) {
            bridge.togglePause();
        }
        ix += ps + 6f;
        iconCtl(ui, "next", ix, ctlCy, bs, false);
        if (ui.clicked(ix, ctlCy - bs / 2f, bs, bs)) {
            bridge.next();
        }

        GlyphIcons.draw(ui, "volume", x + 16f, y + h - 22f, 7.5f, ui.theme().textSecondary());
        float vsX = x + 28.5f;
        float vol = Chrome.slider(ui, "music.volume", vsX, y + h - 24f, x + w - 16f - vsX, bridge.volume());
        if (Math.abs(vol - bridge.volume()) > 0.004f) {
            bridge.setVolume(vol);
        }
    }

    private void iconCtl(UiFrame ui, String icon, float x, float cy, float bs, boolean accent) {
        boolean hover = ui.hovered(x, cy - bs / 2f, bs, bs);
        Chrome.ghostButton(ui, x, cy - bs / 2f, bs, bs, hover);
        GlyphIcons.draw(ui, icon, x + (bs - 8f) / 2f, cy - 4f, 8f,
                accent ? ui.theme().accent() : (hover ? ui.theme().textPrimary() : ui.theme().textSecondary()));
    }

    private void drawSettings(UiFrame ui, MusicBridge bridge, float x, float y, float w, float h) {
        FontBold.draw(ui, 16, "音乐设置", x + 14f, y + 10f, ui.theme().textPrimary());
        ui.canvas().drawString(ui.font(11), "歌词显示", x + 14f, y + 22f, ui.theme().textSecondary());

        float rowX = x + 14f;
        float rowW = w - 28f;
        float rowY = y + 44f;
        settingToggle(ui, bridge, "桌面歌词", rowX, rowY, rowW, bridge.lyricsHudEnabled(), 0);
        rowY += 30f;

        settingSlider(ui, bridge, "歌词字号", Math.round(bridge.lyricFontSize()) + " px",
                rowX, rowY, rowW, (bridge.lyricFontSize() - 10f) / 20f, 0);
        rowY += 30f;
        settingSlider(ui, bridge, "显示行数", bridge.lyricLines() + " 行",
                rowX, rowY, rowW, (bridge.lyricLines() - 1f) / 4f, 1);
        rowY += 30f;

        settingToggle(ui, bridge, "显示翻译", rowX, rowY, rowW, bridge.lyricTranslation(), 1);
        rowY += 30f;
        settingToggle(ui, bridge, "平滑滚动", rowX, rowY, rowW, bridge.lyricScroll(), 2);
        rowY += 30f;
        settingToggle(ui, bridge, "歌词背景", rowX, rowY, rowW, bridge.lyricBackground(), 3);
    }

    private void settingToggle(UiFrame ui, MusicBridge bridge, String label, float x, float y, float w,
                               boolean value, int setting) {
        ui.canvas().fillRoundRect(x, y, w, 24f, 6f, ui.theme().layer());
        ui.canvas().drawString(ui.font(12), label, x + 8f, Chrome.textY(y, 24f, ui.font(12)),
                ui.theme().textPrimary());
        float sx = x + w - Metrics.SWITCH_W - 8f;
        boolean next = Chrome.toggle(ui, sx, y + (24f - Metrics.SWITCH_H) / 2f, value);
        if (next == value) return;
        if (setting == 0) bridge.setLyricsHudEnabled(next);
        else if (setting == 1) bridge.setLyricTranslation(next);
        else if (setting == 2) bridge.setLyricScroll(next);
        else bridge.setLyricBackground(next);
    }

    private void settingSlider(UiFrame ui, MusicBridge bridge, String label, String value, float x, float y,
                               float w, float position, int setting) {
        ui.canvas().fillRoundRect(x, y, w, 24f, 6f, ui.theme().layer());
        ui.canvas().drawString(ui.font(12), label, x + 8f, Chrome.textY(y, 24f, ui.font(12)),
                ui.theme().textPrimary());
        float valueW = ui.font(11).measure(value);
        ui.canvas().drawString(ui.font(11), value, x + w - 8f - valueW,
                Chrome.textY(y, 24f, ui.font(11)), ui.theme().textSecondary());
        float sliderX = x + 76f;
        float sliderW = w - 126f;
        float next = Chrome.slider(ui, setting == 0 ? "music.lyric.font" : "music.lyric.lines",
                sliderX, y + 6f, sliderW, position);
        if (Math.abs(next - position) < 0.004f) return;
        if (setting == 0) bridge.setLyricFontSize(10f + Math.round(next * 20f));
        else bridge.setLyricLines(1 + Math.round(next * 4f));
    }

    private void drawHead(UiFrame ui, MusicBridge bridge, float x, float y, float closeX) {
        FontBold.draw(ui, 16, greeting(bridge), x + 14f, y + 10f, ui.theme().textPrimary());
        ui.canvas().drawString(ui.font(11), greetingSub(bridge), x + 14f, y + 21f, ui.theme().textSecondary());

        float rowH = 15f;
        float rowY = y + 11f;
        String acc = bridge.loggedIn() ? bridge.i18n("music.loggedin") : bridge.i18n("music.notloggedin");
        float labelW = ui.font(12).measure(acc);
        float pillW = 23f + labelW;
        float pillX = closeX - 6f - pillW;
        boolean accHov = ui.hovered(pillX, rowY, pillW, rowH);
        ui.canvas().fillRoundRect(pillX, rowY, pillW, rowH, 7f,
                accHov ? ui.theme().layerHover() : ui.theme().layer());
        ui.canvas().fillRoundRect(pillX + 2f, rowY + 2f, 11f, 11f, 5f, bridge.qq() ? 0xFF2FBE77 : 0xFFE7392F);
        GlyphIcons.draw(ui, "user", pillX + 4.5f, rowY + 4.5f, 6f, ui.theme().white());
        ui.canvas().drawString(ui.font(12), acc, pillX + 15f, Chrome.textY(rowY, rowH, ui.font(12)),
                ui.theme().textPrimary());
        if (bridge.supportsLogin() && ui.clicked(pillX, rowY, pillW, rowH)) {
            if (bridge.loggedIn()) {
                bridge.logout();
            } else {
                loginOpen = true;
                bridge.startLogin();
            }
        }

        String[] labels = {"网易云", "QQ 音乐"};
        float segW = ui.font(12).measure(labels[0]) + ui.font(12).measure(labels[1]) + 32f;
        float segX = pillX - 8f - segW;
        ui.canvas().fillRoundRect(segX, rowY, segW, rowH, rowH / 2f, ui.theme().layer());
        float ox = segX + 1.5f;
        for (int i = 0; i < 2; i++) {
            float ow = ui.font(12).measure(labels[i]) + 14f;
            boolean selected = (i == 1) == bridge.qq();
            if (selected) {
                ui.canvas().fillRoundRect(ox, rowY + 1.5f, ow, rowH - 3f, (rowH - 3f) / 2f, ui.theme().accent());
            }
            int color = selected ? ui.theme().white() : ui.theme().textSecondary();
            ui.canvas().drawString(ui.font(12), labels[i], ox + 7f, Chrome.textY(rowY, rowH, ui.font(12)), color);
            if (ui.clicked(ox, rowY, ow, rowH) && !selected) {
                bridge.setQq(i == 1);
                if (tab == Tab.DISCOVER) {
                    bridge.loadDiscover();
                }
            }
            ox += ow;
        }
    }

    private void drawToolbar(UiFrame ui, MusicBridge bridge, float x, float y, float w) {
        float h = Metrics.SEARCH_H;
        Tab[] tabs = new Tab[]{Tab.DISCOVER, Tab.PLAYLISTS, Tab.SEARCH};
        String[] labels = new String[]{"发现", "我的歌单", "搜索"};
        float ox = x + 14f;
        float tw = 42f;
        ui.canvas().fillRoundRect(ox, y, tw * tabs.length, h, h / 2f, ui.theme().layer());
        for (int i = 0; i < tabs.length; i++) {
            boolean selected = tab == tabs[i];
            if (selected) {
                ui.canvas().fillRoundRect(ox + 1.5f + i * tw, y + 1.5f, tw - 1f, h - 3f, (h - 3f) / 2f,
                        ui.theme().accent());
            }
            float lw = ui.font(12).measure(labels[i]);
            ui.canvas().drawString(ui.font(12), labels[i],
                    ox + i * tw + (tw - lw) / 2f, Chrome.textY(y, h, ui.font(12)),
                    selected ? ui.theme().white() : ui.theme().textSecondary());
            if (ui.clicked(ox + i * tw, y, tw, h) && tab != tabs[i]) {
                tab = tabs[i];
                if (tab == Tab.DISCOVER) {
                    bridge.loadDiscover();
                } else if (tab == Tab.PLAYLISTS) {
                    bridge.loadPlaylists();
                }
            }
        }
        float sw = 115f;
        float sx = x + w - 14f - sw;
        Chrome.searchBox(ui, sx, y, sw, h, search.focused());
        GlyphIcons.draw(ui, "search", sx + 5f, y + (h - 7f) / 2f, 7f, ui.theme().textDisabled());
        search.draw(ui, sx, y, sw, h);
        if (search.focused() && ui.input().consumeKey(top.fpsmaster.prism.input.Keys.ENTER)) {
            tab = Tab.SEARCH;
            bridge.search(search.text());
        }
    }

    private void drawList(UiFrame ui, MusicBridge bridge, float x, float y, float w, float h) {
        if (tab == Tab.PLAYLISTS) {
            List<MusicBridge.PlaylistRow> rows = bridge.playlistRows();
            float contentH = Math.max(h, rows.size() * 18f + 8f);
            float off = scroll.begin(ui, x, y, w, h, contentH);
            if (rows.isEmpty()) {
                ui.canvas().drawString(ui.font(13), bridge.loggedIn() ? "暂无歌单" : bridge.i18n("music.notloggedin"),
                        x, y + 8f + off, ui.theme().textDisabled());
            }
            for (int i = 0; i < rows.size(); i++) {
                MusicBridge.PlaylistRow row = rows.get(i);
                float ry = y + 2f + i * 18f + off;
                boolean hover = ui.hovered(x, ry, w, 17f);
                if (hover) {
                    ui.canvas().fillRoundRect(x, ry, w, 17f, 4f, ui.theme().layerHover());
                }
                ui.canvas().drawString(ui.font(12), ellipsize(ui.font(12), row.name, w - 40f),
                        x + 6f, Chrome.textY(ry, 17f, ui.font(12)), ui.theme().textPrimary());
                ui.canvas().drawString(ui.font(11), row.count, x + w - 8f - ui.font(11).measure(row.count),
                        Chrome.textY(ry, 17f, ui.font(11)), ui.theme().textDisabled());
                if (ui.clicked(x, ry, w, 17f)) {
                    bridge.openPlaylist(i);
                    tab = Tab.SEARCH;
                }
            }
            scroll.end(ui);
            return;
        }
        List<MusicBridge.TrackRow> tracks = bridge.tracks();
        ui.canvas().drawString(ui.font(12), bridge.listTitle(), x, y - 1f, ui.theme().textDisabled());
        float listY = y + 12f;
        float listH = h - 12f;
        float contentH = Math.max(listH, tracks.size() * 16f + 4f);
        float off = scroll.begin(ui, x, listY, w, listH, contentH);
        if (tracks.isEmpty()) {
            ui.canvas().drawString(ui.font(13), tab == Tab.SEARCH ? "输入关键字并回车搜索" : "加载中…",
                    x, listY + 8f + off, ui.theme().textDisabled());
        }
        for (int i = 0; i < tracks.size(); i++) {
            MusicBridge.TrackRow row = tracks.get(i);
            float ry = listY + i * 16f + off;
            boolean hover = ui.hovered(x, ry, w, 15f);
            if (hover) {
                ui.canvas().fillRoundRect(x, ry, w, 15f, 4f, ui.theme().layerHover());
            }
            String n = (i + 1) + "  " + row.name;
            ui.canvas().drawString(ui.font(12), ellipsize(ui.font(12), n, w - 80f),
                    x + 4f, Chrome.textY(ry, 15f, ui.font(12)), ui.theme().textPrimary());
            if (row.vip) {
                ui.canvas().drawString(ui.font(10), "VIP", x + w - 52f, Chrome.textY(ry, 15f, ui.font(10)), BADGE);
            }
            ui.canvas().drawString(ui.font(10), row.duration, x + w - 6f - ui.font(10).measure(row.duration),
                    Chrome.textY(ry, 15f, ui.font(10)), ui.theme().textDisabled());
            if (ui.clicked(x, ry, w, 15f)) {
                bridge.play(i);
            }
        }
        scroll.end(ui);
    }

    private void drawImmersiveLyrics(UiFrame ui, MusicBridge bridge, float x, float y, float w, float h,
                                     float dt, float openT) {
        float eased = Anim.cssEase(openT);
        ui.canvas().pushClip(x + 1f, y + 1f, w - 2f, h - 2f);
        ui.canvas().pushTransform();
        ui.canvas().translate(0f, (1f - eased) * h);
        ui.canvas().fillRoundRect(x + 1f, y + 1f, w - 2f, h - 2f, Metrics.PANEL_RADIUS,
                Argb.lerp(ui.theme().glass(), ui.theme().accentSoft(), 0.18f));
        float backX = x + 12f;
        float backY = y + 9f;
        boolean backHover = ui.hovered(backX, backY, 17f, 17f);
        Chrome.ghostButton(ui, backX, backY, 17f, 17f, backHover);
        GlyphIcons.draw(ui, "chev-d", backX + 4.5f, backY + 4.5f, 8f,
                backHover ? ui.theme().textPrimary() : ui.theme().textSecondary());
        if (ui.clicked(backX, backY, 17f, 17f)) {
            immersiveLyrics = false;
        }
        String title = ellipsize(ui.font(16), bridge.nowTitle(), w - 64f);
        FontBold.draw(ui, 16, title, x + 40f, y + 9f, ui.theme().textPrimary());
        ui.canvas().drawString(ui.font(11), ellipsize(ui.font(11), bridge.nowArtist(), w - 64f),
                x + 40f, y + 21f, ui.theme().textSecondary());
        lyrics.drawImmersive(ui, bridge, x + 16f, y + 34f, w - 32f, h - 72f, dt);
        float barX = x + 42f;
        float barW = w - 84f;
        float barY = y + h - 27f;
        float progress = bridge.progress();
        float next = Chrome.slider(ui, "music.lyrics.progress", barX, barY, barW, progress);
        if (Math.abs(next - progress) > 0.004f) bridge.seek(next);
        if (ui.clicked(x + 14f, barY - 7f, 20f, 20f)) bridge.togglePause();
        GlyphIcons.draw(ui, bridge.playing() && !bridge.paused() ? "pause" : "play",
                x + 20f, barY - 1f, 8f, ui.theme().textPrimary());
        ui.canvas().popTransform();
        ui.canvas().popClip();
    }

    private float dt(UiFrame ui) {
        long now = ui.host().nowNanos();
        float dt = lastNanos == 0L ? 0.016f : Math.min(0.05f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;
        return dt < 0f ? 0.016f : dt;
    }

    private void drawLogin(UiFrame ui, MusicBridge bridge, float px, float py, float pw, float ph) {
        ui.canvas().fillRect(px, py, pw, ph, 0x99000000);
        float w = 190f;
        float h = bridge.qq() ? 202f : 132f;
        float x = px + (pw - w) / 2f;
        float y = py + (ph - h) / 2f;
        Chrome.panel(ui, x, y, w, h);
        FontBold.draw(ui, 16, (bridge.qq() ? "QQ 音乐" : "网易云") + " 登录",
                x + 12f, y + 10f, ui.theme().textPrimary());
        float closeX = x + w - 22f;
        if (Chrome.button(ui, closeX, y + 7f, 14f, 14f, "×", Chrome.ButtonStyle.GHOST)) {
            loginOpen = false;
            bridge.stopLogin();
            return;
        }
        float qr = 68f;
        float qx = x + (w - qr) / 2f;
        float qy = y + 28f;
        ui.canvas().fillRoundRect(qx - 3f, qy - 3f, qr + 6f, qr + 6f, 6f, 0xFFFFFFFF);
        bridge.paintLoginQr(ui, qx, qy, qr);
        String status = bridge.loginStatus();
        ui.canvas().drawString(ui.font(11), ellipsize(ui.font(11), status, w - 24f),
                x + 12f, qy + qr + 7f, ui.theme().textSecondary());
        if (Chrome.button(ui, x + (w - 64f) / 2f, qy + qr + 16f, 64f, 15f,
                "刷新二维码", Chrome.ButtonStyle.DEFAULT)) {
            bridge.startLogin();
        }
        if (bridge.qq()) {
            float fieldX = x + 12f;
            float fieldW = w - 24f;
            float firstY = qy + qr + 38f;
            qqId.draw(ui, fieldX, firstY, fieldW, 18f);
            qqKey.draw(ui, fieldX, firstY + 22f, fieldW, 18f);
            if (Chrome.button(ui, fieldX, firstY + 44f, fieldW, 16f,
                    "Cookie 登录", Chrome.ButtonStyle.PRIMARY)) {
                bridge.submitQqCookie(qqId.text(), qqKey.text());
            }
        }
    }

    private static String greeting(MusicBridge bridge) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String time = hour < 6 ? "夜深了" : hour < 12 ? "早上好" : hour < 18 ? "下午好" : "晚上好";
        return time + "，" + (bridge.qq() ? "Q音听友" : "云村居民");
    }

    private static String greetingSub(MusicBridge bridge) {
        if (bridge.qq()) {
            return "热歌榜已更新";
        }
        return bridge.loggedIn() ? "每日推荐已更新" : "登录后查看每日推荐";
    }

    private static String formatMs(long ms) {
        if (ms <= 0L) {
            return "0:00";
        }
        long total = ms / 1000L;
        return (total / 60L) + ":" + String.format("%02d", (int) (total % 60L));
    }

    private static String ellipsize(top.fpsmaster.prism.canvas.FontHandle font, String text, float max) {
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

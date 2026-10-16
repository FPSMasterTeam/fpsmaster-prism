package top.fpsmaster.prism.screen;

import top.fpsmaster.prism.anim.Anim;
import top.fpsmaster.prism.canvas.FontHandle;
import top.fpsmaster.prism.icon.GlyphIcons;
import top.fpsmaster.prism.theme.Argb;
import top.fpsmaster.prism.theme.Metrics;
import top.fpsmaster.prism.widget.Chrome;
import top.fpsmaster.prism.widget.Scroll;
import top.fpsmaster.prism.widget.TextBox;
import top.fpsmaster.prism.widget.TextFit;
import top.fpsmaster.prism.widget.UiFrame;

import java.util.Calendar;
import java.util.List;

public final class SharedMusic {
    private static final float NOW_W = 160f;
    private static final int BADGE = 0xFFD9A441;

    private static final float TRACK_ROW = 16f;
    private static final float PLAYLIST_ROW = 18f;

    private enum Tab {DISCOVER, PLAYLISTS, SEARCH, LOCAL}

    /** What filled the track list last, so "Retry" can repeat exactly that request. */
    private enum Source {DISCOVER, SEARCH, PLAYLIST}

    private Tab tab = Tab.DISCOVER;
    private Source source = Source.DISCOVER;
    private String lastQuery = "";
    private int lastPlaylist = -1;
    private boolean logoutConfirm;
    private String greeting = "";
    private boolean greetingQq;
    private long greetingCheckedAt = Long.MIN_VALUE;
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
        qqId.setFontSize(12);
        qqId.setPlaceholder("musicid (uin)");
        qqKey.setFontSize(12);
        qqKey.setPlaceholder("musickey (qm_keyst)");
    }

    public boolean cancelOverlay() {
        if (logoutConfirm) {
            logoutConfirm = false;
            return true;
        }
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
            loadDiscover(bridge);
            discoverRequested = true;
        }
        if (loginOpen && bridge.loggedIn()) {
            loginOpen = false;
            bridge.stopLogin();
        }
        if (logoutConfirm && !bridge.loggedIn()) {
            logoutConfirm = false;
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

        // 退出登录确认开着时它是模态：底下照常画，但不响应任何输入。
        boolean inputWas = ui.inputEnabled();
        if (logoutConfirm) {
            ui.setInputEnabled(false);
        }
        try {
            if (drawBody(ui, bridge, px, py, pw, ph)) {
                return true;
            }
        } finally {
            ui.setInputEnabled(inputWas);
        }
        if (logoutConfirm) {
            drawLogoutConfirm(ui, bridge, px, py, pw, ph);
        }
        return false;
    }

    /** @return {@code true} when the close button was clicked */
    private boolean drawBody(UiFrame ui, MusicBridge bridge, float px, float py, float pw, float ph) {
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
        iconCtl(ui, modeIcon(bridge.playbackMode()), x + w - 34f, ctlCy, bs, true);
        if (ui.clicked(x + w - 34f, ctlCy - bs / 2f, bs, bs)) {
            bridge.setPlaybackMode(nextMode(bridge.playbackMode()));
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
        FontBold.draw(ui, 16, tr(bridge, "music.settings.title", "音乐设置"), x + 14f, y + 10f,
                ui.theme().textPrimary());
        ui.canvas().drawString(ui.font(11), tr(bridge, "music.settings.subtitle", "播放与歌词"), x + 14f, y + 22f,
                ui.theme().textSecondary());

        float rowX = x + 14f;
        float rowW = w - 28f;
        float rowY = y + 44f;
        settingMode(ui, bridge, rowX, rowY, rowW);
        rowY += 30f;
        settingSlider(ui, bridge, tr(bridge, "music.settings.lyricsize", "歌词字号"),
                Math.round(bridge.lyricFontSize()) + " px",
                rowX, rowY, rowW, (bridge.lyricFontSize() - 10f) / 20f, 0);
        rowY += 30f;
        settingSlider(ui, bridge, tr(bridge, "music.settings.lyriclines", "显示行数"),
                Tr.format(bridge.i18n("music.settings.lines.unit"), "music.settings.lines.unit", "%d 行",
                        Integer.valueOf(bridge.lyricLines())),
                rowX, rowY, rowW, (bridge.lyricLines() - 1f) / 4f, 1);
        rowY += 30f;

        float half = (rowW - 6f) / 2f;
        settingToggle(ui, bridge, tr(bridge, "music.settings.hud", "桌面歌词"), rowX, rowY, half,
                bridge.lyricsHudEnabled(), 0);
        settingToggle(ui, bridge, tr(bridge, "music.settings.translation", "显示翻译"), rowX + half + 6f, rowY, half,
                bridge.lyricTranslation(), 1);
        rowY += 30f;
        settingToggle(ui, bridge, tr(bridge, "music.settings.smooth", "平滑滚动"), rowX, rowY, half,
                bridge.lyricScroll(), 2);
        settingToggle(ui, bridge, tr(bridge, "music.settings.background", "歌词背景"), rowX + half + 6f, rowY, half,
                bridge.lyricBackground(), 3);
    }

    private void settingMode(UiFrame ui, MusicBridge bridge, float x, float y, float w) {
        ui.canvas().fillRoundRect(x, y, w, 24f, 6f, ui.theme().layer());
        ui.canvas().drawString(ui.font(12), tr(bridge, "music.settings.mode", "播放模式"), x + 8f,
                Chrome.textY(y, 24f, ui.font(12)), ui.theme().textPrimary());
        MusicBridge.PlaybackMode[] modes = MusicBridge.PlaybackMode.values();
        String[] labels = new String[]{
                tr(bridge, "music.mode.sequential", "顺序"),
                tr(bridge, "music.mode.shuffle", "乱序"),
                tr(bridge, "music.mode.repeatone", "单曲")};
        float bw = 38f;
        float bx = x + w - 8f - bw * modes.length;
        for (int i = 0; i < modes.length; i++) {
            boolean selected = bridge.playbackMode() == modes[i];
            if (Chrome.button(ui, bx + i * bw, y + 4f, bw - 2f, 16f, labels[i],
                    selected ? Chrome.ButtonStyle.PRIMARY : Chrome.ButtonStyle.GHOST)) {
                bridge.setPlaybackMode(modes[i]);
            }
        }
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
                // 单击胶囊不能直接退出：玩家多半只是想看看账号状态，退出后得重新扫码。
                logoutConfirm = true;
            } else {
                loginOpen = true;
                bridge.startLogin();
            }
        }

        String[] labels = {tr(bridge, "music.source.netease", "网易云"), tr(bridge, "music.source.qq", "QQ 音乐")};
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
                    loadDiscover(bridge);
                } else if (tab == Tab.PLAYLISTS) {
                    bridge.loadPlaylists();
                }
            }
            ox += ow;
        }
    }

    private void drawToolbar(UiFrame ui, MusicBridge bridge, float x, float y, float w) {
        float h = Metrics.SEARCH_H;
        Tab[] tabs = new Tab[]{Tab.DISCOVER, Tab.PLAYLISTS, Tab.SEARCH, Tab.LOCAL};
        String[] labels = new String[]{
                tr(bridge, "music.tab.discover", "发现"),
                tr(bridge, "music.tab.playlists", "歌单"),
                tr(bridge, "music.tab.search", "搜索"),
                tr(bridge, "music.tab.local", "本地")};
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
                    loadDiscover(bridge);
                } else if (tab == Tab.PLAYLISTS) {
                    bridge.loadPlaylists();
                }
            }
        }
        float sw = 115f;
        float sx = x + w - 14f - sw;
        Chrome.searchBox(ui, sx, y, sw, h, search.focused());
        GlyphIcons.draw(ui, "search", sx + 5f, y + (h - 7f) / 2f, 7f, ui.theme().textDisabled());
        search.setPlaceholder(tr(bridge, "music.search.placeholder", "搜索歌曲、歌手…"));
        search.draw(ui, sx, y, sw, h);
        if (search.focused() && ui.inputEnabled()
                && ui.input().consumeKey(top.fpsmaster.prism.input.Keys.ENTER)) {
            tab = Tab.SEARCH;
            source = Source.SEARCH;
            lastQuery = search.text();
            bridge.search(lastQuery);
        }
    }

    private void drawList(UiFrame ui, MusicBridge bridge, float x, float y, float w, float h) {
        if (tab == Tab.PLAYLISTS) {
            List<MusicBridge.PlaylistRow> rows = bridge.playlistRows();
            MusicBridge.ListState state = bridge.playlistsState();
            float contentH = Math.max(h, rows.size() * PLAYLIST_ROW + 8f);
            float off = scroll.begin(ui, x, y, w, h, contentH);
            try {
                if (rows.isEmpty() || state != MusicBridge.ListState.READY) {
                    listMessage(ui, bridge, state, bridge.playlistsError(),
                            tr(bridge, "music.playlists.empty", "暂无歌单"), x, y + 8f + off, w);
                } else {
                    // 只画视口内的行：歌单可能上千条，全量画每帧要测量、截断上千个字符串。
                    int first = Math.max(0, (int) Math.floor((-off - 2f) / PLAYLIST_ROW));
                    int last = Math.min(rows.size(), first + (int) Math.ceil(h / PLAYLIST_ROW) + 2);
                    FontHandle nameFont = ui.font(12);
                    FontHandle countFont = ui.font(11);
                    for (int i = first; i < last; i++) {
                        MusicBridge.PlaylistRow row = rows.get(i);
                        float ry = y + 2f + i * PLAYLIST_ROW + off;
                        boolean hover = ui.hovered(x, ry, w, 17f);
                        if (hover) {
                            ui.canvas().fillRoundRect(x, ry, w, 17f, 4f, ui.theme().layerHover());
                        }
                        ui.canvas().drawString(nameFont, ellipsize(nameFont, row.name, w - 40f),
                                x + 6f, Chrome.textY(ry, 17f, nameFont), ui.theme().textPrimary());
                        ui.canvas().drawString(countFont, row.count, x + w - 8f - TextFit.width(countFont, row.count),
                                Chrome.textY(ry, 17f, countFont), ui.theme().textDisabled());
                        if (ui.clicked(x, ry, w, 17f)) {
                            openPlaylist(bridge, i);
                        }
                    }
                }
            } finally {
                scroll.end(ui);
            }
            return;
        }
        boolean local = tab == Tab.LOCAL;
        List<MusicBridge.TrackRow> tracks = local ? bridge.localTracks() : bridge.tracks();
        if (local) {
            if (Chrome.button(ui, x + w - 64f, y - 4f, 64f, 15f, tr(bridge, "music.local.import", "导入音乐"),
                    Chrome.ButtonStyle.DEFAULT)) {
                bridge.importLocalMusic();
            }
        }
        ui.canvas().drawString(ui.font(12), local ? tr(bridge, "music.local.title", "本地音乐") : bridge.listTitle(),
                x, y - 1f, ui.theme().textDisabled());
        float listY = y + 12f;
        float listH = h - 12f;
        float contentH = Math.max(listH, tracks.size() * TRACK_ROW + 4f);
        float off = scroll.begin(ui, x, listY, w, listH, contentH);
        try {
            MusicBridge.ListState state = local ? MusicBridge.ListState.READY : bridge.tracksState();
            if (tracks.isEmpty() || state != MusicBridge.ListState.READY) {
                String empty = local ? tr(bridge, "music.local.empty", "导入 MP3、WAV 或 AIFF 文件")
                        : tab == Tab.SEARCH && source != Source.PLAYLIST && lastQuery.isEmpty()
                        ? tr(bridge, "music.search.hint", "输入关键字并回车搜索")
                        : tr(bridge, "music.list.empty", "暂无歌曲");
                listMessage(ui, bridge, state, bridge.tracksError(), empty, x, listY + 8f + off, w);
                return;
            }
            // 虚拟列表：只绘制、命中视口内的行（歌单最多上千首）。
            int first = Math.max(0, (int) Math.floor(-off / TRACK_ROW));
            int last = Math.min(tracks.size(), first + (int) Math.ceil(listH / TRACK_ROW) + 2);
            FontHandle nameFont = ui.font(12);
            FontHandle smallFont = ui.font(10);
            for (int i = first; i < last; i++) {
                MusicBridge.TrackRow row = tracks.get(i);
                float ry = listY + i * TRACK_ROW + off;
                boolean hover = ui.hovered(x, ry, w, 15f);
                if (hover) {
                    ui.canvas().fillRoundRect(x, ry, w, 15f, 4f, ui.theme().layerHover());
                }
                String n = (i + 1) + "  " + row.name;
                ui.canvas().drawString(nameFont, ellipsize(nameFont, n, w - 80f),
                        x + 4f, Chrome.textY(ry, 15f, nameFont), ui.theme().textPrimary());
                if (row.vip) {
                    ui.canvas().drawString(smallFont, "VIP", x + w - 52f, Chrome.textY(ry, 15f, smallFont), BADGE);
                }
                ui.canvas().drawString(smallFont, row.duration, x + w - 6f - TextFit.width(smallFont, row.duration),
                        Chrome.textY(ry, 15f, smallFont), ui.theme().textDisabled());
                if (ui.clicked(x, ry, w, 15f)) {
                    if (local) bridge.playLocal(i);
                    else bridge.play(i);
                }
            }
        } finally {
            scroll.end(ui);
        }
    }

    /** Placeholder for a list that is loading, empty, failed or needs login, with its action button. */
    private void listMessage(UiFrame ui, MusicBridge bridge, MusicBridge.ListState state, String error,
                             String emptyText, float x, float y, float w) {
        FontHandle font = ui.font(13);
        String text;
        String action = null;
        int color = ui.theme().textDisabled();
        if (state == MusicBridge.ListState.LOADING) {
            text = tr(bridge, "music.list.loading", "加载中…");
        } else if (state == MusicBridge.ListState.NEED_LOGIN) {
            text = tr(bridge, "music.list.needlogin", "登录后查看");
            action = bridge.supportsLogin() ? tr(bridge, "music.login", "登录") : null;
        } else if (state == MusicBridge.ListState.ERROR) {
            String reason = error == null ? "" : error.trim();
            text = reason.isEmpty() ? tr(bridge, "music.list.error", "加载失败") : reason;
            action = tr(bridge, "music.list.retry", "重试");
            color = ui.theme().danger();
        } else {
            text = emptyText;
        }
        ui.canvas().drawString(font, ellipsize(font, text, w - 8f), x, y, color);
        if (action == null) {
            return;
        }
        float bw = Math.max(48f, TextFit.width(ui.font(12), action) + 16f);
        if (Chrome.button(ui, x, y + 14f, bw, 16f, action, Chrome.ButtonStyle.DEFAULT)) {
            if (state == MusicBridge.ListState.NEED_LOGIN) {
                loginOpen = true;
                bridge.startLogin();
            } else if (tab == Tab.PLAYLISTS) {
                bridge.loadPlaylists();
            } else {
                retryTracks(bridge);
            }
        }
    }

    private void loadDiscover(MusicBridge bridge) {
        source = Source.DISCOVER;
        bridge.loadDiscover();
    }

    private void openPlaylist(MusicBridge bridge, int index) {
        source = Source.PLAYLIST;
        lastPlaylist = index;
        bridge.openPlaylist(index);
        tab = Tab.SEARCH;
    }

    /** Repeat whatever filled the track list last. */
    private void retryTracks(MusicBridge bridge) {
        if (source == Source.SEARCH) {
            bridge.search(lastQuery);
        } else if (source == Source.PLAYLIST && lastPlaylist >= 0) {
            bridge.openPlaylist(lastPlaylist);
        } else {
            bridge.loadDiscover();
        }
    }

    private void drawLogoutConfirm(UiFrame ui, MusicBridge bridge, float px, float py, float pw, float ph) {
        ui.canvas().fillRect(px, py, pw, ph, 0x99000000);
        float w = 200f;
        float h = 64f;
        float x = px + (pw - w) / 2f;
        float y = py + (ph - h) / 2f;
        Chrome.panel(ui, x, y, w, h);
        String source = bridge.qq() ? tr(bridge, "music.source.qq", "QQ 音乐") : tr(bridge, "music.source.netease", "网易云");
        String title = Tr.format(bridge.i18n("music.logout.confirm"), "music.logout.confirm", "退出%s账号？", source);
        ui.canvas().drawString(ui.font(13), ellipsize(ui.font(13), title, w - 20f), x + 10f, y + 10f,
                ui.theme().textPrimary());
        float bw = 64f;
        float by = y + h - 26f;
        if (Chrome.button(ui, x + w - 10f - bw, by, bw, 16f, tr(bridge, "music.logout", "退出登录"),
                Chrome.ButtonStyle.DANGER_FILL)) {
            logoutConfirm = false;
            bridge.logout();
        } else if (Chrome.button(ui, x + w - 16f - bw * 2f, by, bw, 16f, tr(bridge, "music.cancel", "取消"),
                Chrome.ButtonStyle.GHOST)) {
            logoutConfirm = false;
        }
    }

    private static String tr(MusicBridge bridge, String key, String fallback) {
        return Tr.of(bridge.i18n(key), key, fallback);
    }

    private void drawImmersiveLyrics(UiFrame ui, MusicBridge bridge, float x, float y, float w, float h,
                                     float dt, float openT) {
        float eased = Anim.cssEase(openT);
        ui.pushClip(x + 1f, y + 1f, w - 2f, h - 2f);
        // UiFrame 的变换：滑入过程中按钮的命中区域跟着画面一起移动。
        ui.pushTransform();
        try {
        ui.translate(0f, (1f - eased) * h);
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
        } finally {
        ui.popTransform();
        ui.popClip();
        }
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
        String sourceName = bridge.qq() ? tr(bridge, "music.source.qq", "QQ 音乐") : tr(bridge, "music.source.netease", "网易云");
        FontBold.draw(ui, 16, Tr.format(bridge.i18n("music.login.title"), "music.login.title", "%s 登录", sourceName),
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
                tr(bridge, "music.login.refreshqr", "刷新二维码"), Chrome.ButtonStyle.DEFAULT)) {
            bridge.startLogin();
        }
        if (bridge.qq()) {
            float fieldX = x + 12f;
            float fieldW = w - 24f;
            float firstY = qy + qr + 38f;
            qqId.draw(ui, fieldX, firstY, fieldW, 18f);
            qqKey.draw(ui, fieldX, firstY + 22f, fieldW, 18f);
            if (Chrome.button(ui, fieldX, firstY + 44f, fieldW, 16f,
                    tr(bridge, "music.login.cookie", "Cookie 登录"), Chrome.ButtonStyle.PRIMARY)) {
                bridge.submitQqCookie(qqId.text(), qqKey.text());
            }
        }
    }

    /** Cached for a minute: Calendar.getInstance() and the concatenation ran every frame. */
    private String greeting(MusicBridge bridge) {
        long now = System.currentTimeMillis();
        if (bridge.qq() != greetingQq || now - greetingCheckedAt >= 60_000L || now < greetingCheckedAt) {
            greetingCheckedAt = now;
            greetingQq = bridge.qq();
            int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            String time = hour < 6 ? tr(bridge, "music.greeting.night", "夜深了")
                    : hour < 12 ? tr(bridge, "music.greeting.morning", "早上好")
                    : hour < 18 ? tr(bridge, "music.greeting.afternoon", "下午好")
                    : tr(bridge, "music.greeting.evening", "晚上好");
            String who = greetingQq ? tr(bridge, "music.greeting.qq", "Q音听友")
                    : tr(bridge, "music.greeting.netease", "云村居民");
            greeting = time + tr(bridge, "music.greeting.sep", "，") + who;
        }
        return greeting;
    }

    private static String greetingSub(MusicBridge bridge) {
        if (bridge.qq()) {
            return tr(bridge, "music.sub.qq", "热歌榜已更新");
        }
        return bridge.loggedIn() ? tr(bridge, "music.sub.daily", "每日推荐已更新")
                : tr(bridge, "music.sub.login", "登录后查看每日推荐");
    }

    private static MusicBridge.PlaybackMode nextMode(MusicBridge.PlaybackMode mode) {
        if (mode == MusicBridge.PlaybackMode.SEQUENTIAL) return MusicBridge.PlaybackMode.SHUFFLE;
        if (mode == MusicBridge.PlaybackMode.SHUFFLE) return MusicBridge.PlaybackMode.REPEAT_ONE;
        return MusicBridge.PlaybackMode.SEQUENTIAL;
    }

    private static String modeIcon(MusicBridge.PlaybackMode mode) {
        if (mode == MusicBridge.PlaybackMode.SHUFFLE) return "shuffle";
        if (mode == MusicBridge.PlaybackMode.REPEAT_ONE) return "repeat-one";
        return "repeat";
    }

    private static String formatMs(long ms) {
        if (ms <= 0L) {
            return "0:00";
        }
        long total = ms / 1000L;
        int sec = (int) (total % 60L);
        return (total / 60L) + (sec < 10 ? ":0" : ":") + sec;
    }

    private static String ellipsize(top.fpsmaster.prism.canvas.FontHandle font, String text, float max) {
        return TextFit.ellipsize(font, text, max);
    }
}

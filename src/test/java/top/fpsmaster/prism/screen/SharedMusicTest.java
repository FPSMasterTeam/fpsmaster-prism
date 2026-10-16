package top.fpsmaster.prism.screen;

import org.junit.jupiter.api.Test;
import top.fpsmaster.prism.test.HeadlessHost;
import top.fpsmaster.prism.theme.Theme;
import top.fpsmaster.prism.widget.UiFrame;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedMusicTest {
    @Test
    void coverOpensLyricsWithUpwardTransition() {
        HeadlessHost host = new HeadlessHost(500, 320);
        SharedMusic music = new SharedMusic();
        MusicTestBridge bridge = new MusicTestBridge();

        host.input.press(0, 50, 40);
        music.draw(new UiFrame(host, Theme.DARK), bridge);
        host.input.endFrame();
        music.draw(new UiFrame(host, Theme.DARK), bridge);

        assertTrue(host.canvas.has("pushTransform"));
        assertTrue(host.canvas.has("translate"));
        assertFalse(host.canvas.has("drawString:歌词"));
    }

    private static void frame(HeadlessHost host, SharedMusic music, MusicTestBridge bridge) {
        host.advanceMillis(16);
        music.draw(new UiFrame(host, Theme.DARK), bridge);
        host.input.endFrame();
    }

    private static void clickText(HeadlessHost host, String text) {
        float[] at = host.canvas.textAt(text);
        assertTrue(at != null, "not drawn: " + text);
        host.input.press(0, Math.round(at[0]) + 2, Math.round(at[1]) + 2);
    }

    @Test
    void needLoginListOffersLoginInsteadOfEndlessLoading() {
        HeadlessHost host = new HeadlessHost(500, 320);
        SharedMusic music = new SharedMusic();
        MusicTestBridge bridge = new MusicTestBridge();
        bridge.state = MusicBridge.ListState.NEED_LOGIN;
        frame(host, music, bridge);
        assertTrue(host.canvas.has("drawString:登录后查看"));
        assertFalse(host.canvas.has("drawString:加载中…"));
        clickText(host, "登录");
        frame(host, music, bridge);
        assertEquals(1, bridge.loginStarts);
    }

    @Test
    void errorListShowsReasonAndRetriesTheSameRequest() {
        HeadlessHost host = new HeadlessHost(500, 320);
        SharedMusic music = new SharedMusic();
        MusicTestBridge bridge = new MusicTestBridge();
        bridge.state = MusicBridge.ListState.ERROR;
        bridge.error = "网络超时";
        frame(host, music, bridge);
        assertTrue(host.canvas.has("drawString:网络超时"));
        int loads = bridge.discoverLoads;
        clickText(host, "重试");
        frame(host, music, bridge);
        assertEquals(loads + 1, bridge.discoverLoads);
    }

    @Test
    void loadingIsOnlyShownWhileTheHostSaysSo() {
        HeadlessHost host = new HeadlessHost(500, 320);
        SharedMusic music = new SharedMusic();
        MusicTestBridge bridge = new MusicTestBridge();
        bridge.state = MusicBridge.ListState.LOADING;
        frame(host, music, bridge);
        assertTrue(host.canvas.has("drawString:加载中…"));
        host.canvas.ops.clear();
        bridge.state = MusicBridge.ListState.READY;
        frame(host, music, bridge);
        assertFalse(host.canvas.has("drawString:加载中…"));
        assertTrue(host.canvas.has("drawString:暂无歌曲"));
    }

    @Test
    void longListDrawsOnlyVisibleRows() {
        HeadlessHost host = new HeadlessHost(500, 320);
        SharedMusic music = new SharedMusic();
        MusicTestBridge bridge = new MusicTestBridge();
        for (int i = 0; i < 1000; i++) {
            bridge.rows.add(new MusicBridge.TrackRow("Song" + i, "A", "3:00", false));
        }
        frame(host, music, bridge);
        int drawn = 0;
        for (String op : host.canvas.ops) {
            if (op.startsWith("drawString:") && op.contains("  Song")) {
                drawn++;
            }
        }
        assertTrue(drawn > 5 && drawn < 30, "drew " + drawn + " rows");
    }

    @Test
    void accountPillAsksBeforeLoggingOut() {
        HeadlessHost host = new HeadlessHost(500, 320);
        SharedMusic music = new SharedMusic();
        MusicTestBridge bridge = new MusicTestBridge();
        bridge.loggedIn = true;
        frame(host, music, bridge);
        clickText(host, "music.loggedin");
        frame(host, music, bridge);
        assertEquals(0, bridge.logouts);
        host.canvas.ops.clear();
        frame(host, music, bridge);
        clickText(host, "退出登录");
        frame(host, music, bridge);
        assertEquals(1, bridge.logouts);
    }

    @Test
    void hostWithoutStateShowsLoadingWhileTracksAreEmpty() {
        HeadlessHost host = new HeadlessHost(500, 320);
        SharedMusic music = new SharedMusic();
        MusicTestBridge bridge = new MusicTestBridge();
        bridge.defaultState = true;
        frame(host, music, bridge);
        assertTrue(host.canvas.has("drawString:加载中…"));
        assertFalse(host.canvas.has("drawString:暂无歌曲"));
    }

    @Test
    void hostWithoutStateAsksLoggedOutUserToLogInForPlaylists() {
        HeadlessHost host = new HeadlessHost(500, 320);
        SharedMusic music = new SharedMusic();
        MusicTestBridge bridge = new MusicTestBridge();
        bridge.defaultState = true;
        frame(host, music, bridge);
        clickText(host, "歌单");
        frame(host, music, bridge);
        host.canvas.ops.clear();
        frame(host, music, bridge);
        assertTrue(host.canvas.has("drawString:登录后查看"));
        assertFalse(host.canvas.has("drawString:暂无歌单"));
    }

    @Test
    void searchHintIgnoresStateLeftByDiscover() {
        HeadlessHost host = new HeadlessHost(500, 320);
        SharedMusic music = new SharedMusic();
        MusicTestBridge bridge = new MusicTestBridge();
        bridge.state = MusicBridge.ListState.ERROR;
        frame(host, music, bridge);
        clickText(host, "搜索");
        frame(host, music, bridge);
        host.canvas.ops.clear();
        frame(host, music, bridge);
        assertTrue(host.canvas.has("drawString:输入关键字并回车搜索"));
        assertFalse(host.canvas.has("drawString:重试"));
    }

    private static final class MusicTestBridge implements MusicBridge {
        ListState state = ListState.READY;
        boolean defaultState;
        String error = "";
        boolean loggedIn;
        int loginStarts;
        int discoverLoads;
        int logouts;
        final List<TrackRow> rows = new java.util.ArrayList<TrackRow>();
        public ListState tracksState() { return defaultState ? MusicBridge.super.tracksState() : state; }
        public String tracksError() { return error; }
        public boolean supportsLogin() { return true; }
        public void startLogin() { loginStarts++; }
        public void logout() { logouts++; loggedIn = false; }
        public String i18n(String key) { return key; }
        public boolean qq() { return false; }
        public void setQq(boolean qq) { }
        public boolean loggedIn() { return loggedIn; }
        public String status() { return ""; }
        public String nowTitle() { return "Track"; }
        public String nowArtist() { return "Artist"; }
        public boolean playing() { return true; }
        public boolean paused() { return false; }
        public float progress() { return 0.25f; }
        public long positionMs() { return 15_000L; }
        public long durationMs() { return 60_000L; }
        public float volume() { return 0.5f; }
        public void setVolume(float value) { }
        public void seek(float value) { }
        public void togglePause() { }
        public void next() { }
        public void prev() { }
        public void play(int index) { }
        public List<TrackRow> tracks() { return rows; }
        public String listTitle() { return ""; }
        public void search(String query) { }
        public void loadDiscover() { discoverLoads++; }
        public void loadPlaylists() { }
        public boolean playlists() { return false; }
        public void openPlaylist(int index) { }
        public List<PlaylistRow> playlistRows() { return Collections.emptyList(); }
        public boolean hasLyrics() { return true; }
        public List<LyricRow> lyricRows() {
            return Collections.singletonList(new LyricRow("Line", ""));
        }
        public int currentLyricIndex() { return 0; }
    }
}

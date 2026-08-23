package top.fpsmaster.prism.screen;

import org.junit.jupiter.api.Test;
import top.fpsmaster.prism.test.HeadlessHost;
import top.fpsmaster.prism.theme.Theme;
import top.fpsmaster.prism.widget.UiFrame;

import java.util.Collections;
import java.util.List;

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

    private static final class MusicTestBridge implements MusicBridge {
        public String i18n(String key) { return key; }
        public boolean qq() { return false; }
        public void setQq(boolean qq) { }
        public boolean loggedIn() { return false; }
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
        public List<TrackRow> tracks() { return Collections.emptyList(); }
        public String listTitle() { return ""; }
        public void search(String query) { }
        public void loadDiscover() { }
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

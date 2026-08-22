package top.fpsmaster.prism.screen;

import top.fpsmaster.prism.widget.UiFrame;

import java.util.List;

public interface MusicBridge {
    String i18n(String key);

    boolean qq();

    void setQq(boolean qq);

    boolean loggedIn();

    String status();

    String nowTitle();

    String nowArtist();

    boolean playing();

    boolean paused();

    float progress();

    long positionMs();

    long durationMs();

    float volume();

    void setVolume(float t);

    void seek(float t);

    void togglePause();

    void next();

    void prev();

    void play(int index);

    List<TrackRow> tracks();

    String listTitle();

    void search(String query);

    void loadDiscover();

    void loadPlaylists();

    boolean playlists();

    void openPlaylist(int index);

    List<PlaylistRow> playlistRows();

    default boolean supportsLogin() {
        return false;
    }

    default void startLogin() {
    }

    default void stopLogin() {
    }

    default void logout() {
    }

    default String loginStatus() {
        return "";
    }

    default void paintLoginQr(UiFrame ui, float x, float y, float size) {
    }

    default void submitQqCookie(String musicId, String musicKey) {
    }

    default boolean hasLyrics() {
        return false;
    }

    default int currentLyricIndex() {
        return -1;
    }

    default List<LyricRow> lyricRows() {
        return java.util.Collections.emptyList();
    }

    final class TrackRow {
        public final String name;
        public final String artists;
        public final String duration;
        public final boolean vip;

        public TrackRow(String name, String artists, String duration, boolean vip) {
            this.name = name;
            this.artists = artists;
            this.duration = duration;
            this.vip = vip;
        }
    }

    final class PlaylistRow {
        public final String name;
        public final String count;

        public PlaylistRow(String name, String count) {
            this.name = name;
            this.count = count;
        }
    }

    final class LyricRow {
        public final String text;
        public final String translation;

        public LyricRow(String text, String translation) {
            this.text = text == null ? "" : text;
            this.translation = translation == null ? "" : translation;
        }
    }
}

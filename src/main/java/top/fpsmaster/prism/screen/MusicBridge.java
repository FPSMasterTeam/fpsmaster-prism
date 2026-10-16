package top.fpsmaster.prism.screen;

import top.fpsmaster.prism.widget.UiFrame;

import java.util.List;

public interface MusicBridge {
    enum PlaybackMode { SEQUENTIAL, SHUFFLE, REPEAT_ONE }

    /**
     * What a list currently shows. An empty list alone cannot tell "still loading" from "nothing
     * there", "needs login" or "request failed", so hosts report the state explicitly.
     */
    enum ListState {
        /** The list holds the result of the last request (possibly empty). */
        READY,
        /** A request is in flight. */
        LOADING,
        /** The source needs a logged-in account for this list (e.g. NetEase daily picks / my playlists). */
        NEED_LOGIN,
        /** The last request failed; see {@link #tracksError()} / {@link #playlistsError()}. */
        ERROR
    }

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

    default PlaybackMode playbackMode() {
        return PlaybackMode.SEQUENTIAL;
    }

    default void setPlaybackMode(PlaybackMode mode) {
    }

    void play(int index);

    List<TrackRow> tracks();

    default List<TrackRow> localTracks() {
        return java.util.Collections.emptyList();
    }

    default void importLocalMusic() {
    }

    default void playLocal(int index) {
    }

    String listTitle();

    /**
     * State of {@link #tracks()} (discover, search results, an opened playlist). Hosts set
     * {@link ListState#LOADING} when they start a request and {@link ListState#READY} /
     * {@link ListState#ERROR} / {@link ListState#NEED_LOGIN} when it settles.
     */
    default ListState tracksState() {
        return ListState.READY;
    }

    /** User-facing reason when {@link #tracksState()} is {@link ListState#ERROR}. */
    default String tracksError() {
        return "";
    }

    /** State of {@link #playlistRows()}; same contract as {@link #tracksState()}. */
    default ListState playlistsState() {
        return ListState.READY;
    }

    /** User-facing reason when {@link #playlistsState()} is {@link ListState#ERROR}. */
    default String playlistsError() {
        return "";
    }

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

    /**
     * Log out of the current source. Screens call this only after the user confirms; it is never
     * triggered by a single click.
     */
    default void logout() {
    }

    default String loginStatus() {
        return "";
    }

    default void paintLoginQr(UiFrame ui, float x, float y, float size) {
    }

    default void paintCover(UiFrame ui, float x, float y, float size) {
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

    default boolean lyricsHudEnabled() {
        return false;
    }

    default void setLyricsHudEnabled(boolean enabled) {
    }

    default float lyricFontSize() {
        return 18f;
    }

    default void setLyricFontSize(float size) {
    }

    default int lyricLines() {
        return 2;
    }

    default void setLyricLines(int lines) {
    }

    default boolean lyricTranslation() {
        return true;
    }

    default void setLyricTranslation(boolean enabled) {
    }

    default boolean lyricScroll() {
        return true;
    }

    default void setLyricScroll(boolean enabled) {
    }

    default boolean lyricBackground() {
        return true;
    }

    default void setLyricBackground(boolean enabled) {
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

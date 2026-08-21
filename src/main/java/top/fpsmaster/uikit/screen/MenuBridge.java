package top.fpsmaster.uikit.screen;

import top.fpsmaster.uikit.widget.UiFrame;

/** Client-specific data and navigation for {@link SharedMainMenu}. */
public interface MenuBridge {
    String i18n(String key);

    String edition();

    String version();

    String minecraftLabel();

    String playerName();

    String accountTypeLabel();

    ContinueServer continueServer();

    boolean showReplays();

    boolean showDevtools();

    void singleplayer();

    void multiplayer();

    void settings();

    void replays();

    void music();

    void backgrounds();

    void quit();

    void continueConnect();

    void devtools();

    /** False while a client overlay (login dialog) should swallow chrome clicks. */
    default boolean interactive() {
        return true;
    }

    default void account() {
    }

    default boolean accountOpen() {
        return false;
    }

    default void drawAvatar(UiFrame ui, float x, float y, float size) {
        ui.canvas().fillRoundRect(x, y, size, size, 4f, ui.theme().accent());
    }

    /** Nova shows a native/web chrome toggle on the main menu; Edge does not. */
    default boolean showWebToggle() {
        return false;
    }

    default boolean webUi() {
        return false;
    }

    default void toggleWebUi() {
    }

    default java.util.List<AccountRow> accounts() {
        return java.util.Collections.emptyList();
    }

    default void selectAccount(String id) {
    }

    default void removeAccount(String id) {
    }

    default void startMicrosoftLogin() {
    }

    default boolean addOffline(String name) {
        return false;
    }

    default void openMicrosoftUrl() {
    }

    default void copyMicrosoftCode() {
    }

    default void retryMicrosoftLogin() {
    }

    default void cancelMicrosoftLogin() {
    }

    default String microsoftCode() {
        return "";
    }

    default String microsoftStatus() {
        return "";
    }

    default String microsoftError() {
        return "";
    }

    default boolean microsoftBusy() {
        return false;
    }

    default boolean microsoftHasUrl() {
        return false;
    }

    final class AccountRow {
        public final String id;
        public final String name;
        public final String typeLabel;
        public final boolean current;
        public final boolean removable;
        public final boolean microsoft;

        public AccountRow(String id, String name, String typeLabel, boolean current,
                          boolean removable, boolean microsoft) {
            this.id = id;
            this.name = name;
            this.typeLabel = typeLabel;
            this.current = current;
            this.removable = removable;
            this.microsoft = microsoft;
        }
    }

    final class ContinueServer {
        public final String name;
        public final String address;
        public final long pingMs;

        public ContinueServer(String name, String address, long pingMs) {
            this.name = name;
            this.address = address;
            this.pingMs = pingMs;
        }
    }
}

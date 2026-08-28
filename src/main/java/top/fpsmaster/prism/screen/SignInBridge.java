package top.fpsmaster.prism.screen;

/** Client-specific FPSMaster account state and actions for {@link SharedSignIn}. */
public interface SignInBridge {
    String i18n(String key);

    /** True once a usable (non-expired) FPSMaster token is held. */
    boolean signedIn();

    /** Display name for the signed-in account; may be empty while the profile is still loading. */
    String accountName();

    /** True while a login / logout request is in flight. */
    boolean busy();

    /** Localised failure text, or an empty string. */
    String error();

    void submit(String account, String password);

    void signOut();

    /** Leaves the sign-in screen (usually back to the previous screen). */
    void close();

    /** Opens the account site in the system browser. No-op hosts simply hide the link. */
    default void openWebsite() {
    }

    /** False hides the "create an account" link, for hosts that cannot open a browser. */
    default boolean canOpenWebsite() {
        return false;
    }
}

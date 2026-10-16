package top.fpsmaster.prism.screen;

import java.util.List;

public interface ConfigProfilesBridge {
    String i18n(String key);

    String activeName();

    List<Profile> profiles();

    int enabledModules();

    int hudModules();

    long activeBytes();

    long activeModified();

    boolean isDefault(String name);

    /*
     * Operations return a structured Result: the screen colours the status line from Result.ok and
     * shows Result.message as is. It used to guess success by searching the message for "失败" /
     * "fail", which broke in every other language.
     */

    Result load(String name);

    Result delete(String name);

    Result rename(String from, String to);

    Result create(String name);

    Result exportActive();

    Result importFile();

    Result resetAllOff();

    /** Outcome of a profile operation: whether it succeeded and the localized line to show. */
    final class Result {
        public final boolean ok;
        public final String message;

        private Result(boolean ok, String message) {
            this.ok = ok;
            this.message = message == null ? "" : message;
        }

        public static Result ok(String message) {
            return new Result(true, message);
        }

        public static Result failed(String message) {
            return new Result(false, message);
        }
    }

    final class Profile {
        public final String name;
        public final long modified;
        public final long bytes;

        public Profile(String name, long modified, long bytes) {
            this.name = name;
            this.modified = modified;
            this.bytes = bytes;
        }
    }
}

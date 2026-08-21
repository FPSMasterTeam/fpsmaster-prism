package top.fpsmaster.uikit.screen;

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

    String load(String name);

    String delete(String name);

    String rename(String from, String to);

    String create(String name);

    String exportActive();

    String importFile();

    String resetAllOff();

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

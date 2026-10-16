package top.fpsmaster.prism.screen;

/**
 * Translation with a built-in fallback. Bridges return the key itself (or nothing) for keys the
 * host's language files do not define yet; screens then show the Chinese default instead of a raw
 * key, so a host that has not added the new keys keeps today's text.
 */
final class Tr {
    private Tr() {
    }

    static String of(String translated, String key, String fallback) {
        if (translated == null || translated.isEmpty() || translated.equals(key)) {
            return fallback;
        }
        return translated;
    }

    /** {@link String#format} on the translated pattern; falls back to the default pattern if it is malformed. */
    static String format(String translated, String key, String fallback, Object... args) {
        String pattern = of(translated, key, fallback);
        try {
            return String.format(pattern, args);
        } catch (RuntimeException badPattern) {
            return String.format(fallback, args);
        }
    }
}

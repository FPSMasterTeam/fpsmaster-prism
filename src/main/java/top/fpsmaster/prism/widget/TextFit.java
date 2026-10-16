package top.fpsmaster.prism.widget;

import top.fpsmaster.prism.canvas.FontHandle;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cached text measurement and ellipsizing shared by every screen.
 *
 * <p>Screens redraw the same labels every frame; measuring through the host font each time (and,
 * for text that does not fit, trimming one character per measurement) was thousands of glyph walks
 * per frame on long lists and the lyrics HUD. Here an over-wide string is cut with a binary search
 * (O(log n) measurements) and both widths and cut results are kept in a small per-thread LRU.
 *
 * <p>Cache key is the font's class and {@link FontHandle#size()} plus the text: hosts hand out a
 * fresh {@code FontHandle} per call, so identity would never hit. Hosts whose metrics can change at
 * runtime (font / resource reload) must call {@link #clearCache()} afterwards.
 */
public final class TextFit {
    /** Three ASCII dots, the suffix most screens use. */
    public static final String DOTS = "...";
    /** Single-glyph ellipsis. */
    public static final String ELLIPSIS = "…";

    private static final int CAPACITY = 2048;

    private static volatile int generation;

    private static final ThreadLocal<Cache> CACHE = new ThreadLocal<Cache>() {
        @Override
        protected Cache initialValue() {
            return new Cache();
        }
    };

    private TextFit() {
    }

    /** Forget every cached width and cut, on every thread. Call after the host reloads fonts. */
    public static void clearCache() {
        generation++;
    }

    /** {@link FontHandle#measure} through the cache. */
    public static float width(FontHandle font, String text) {
        if (text == null || text.isEmpty()) {
            return 0f;
        }
        Cache cache = cache();
        Key probe = cache.probe.set(font, text, 0f, null);
        Float hit = cache.widths.get(probe);
        if (hit != null) {
            return hit.floatValue();
        }
        float w = font.measure(text);
        cache.widths.put(probe.copy(), Float.valueOf(w));
        return w;
    }

    /** {@code text} cut to fit {@code max} with {@link #DOTS}. */
    public static String ellipsize(FontHandle font, String text, float max) {
        return ellipsize(font, text, max, DOTS);
    }

    /**
     * Longest prefix of {@code text} that fits {@code max} together with {@code suffix}; the text
     * itself when it fits; {@code suffix} alone when not even one character fits. Never splits a
     * surrogate pair.
     */
    public static String ellipsize(FontHandle font, String text, float max, String suffix) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (width(font, text) <= max) {
            return text;
        }
        Cache cache = cache();
        Key probe = cache.probe.set(font, text, max, suffix);
        String hit = cache.cuts.get(probe);
        if (hit != null) {
            return hit;
        }
        String cut = cut(font, text, max, suffix);
        cache.cuts.put(probe.copy(), cut);
        return cut;
    }

    private static String cut(FontHandle font, String text, float max, String suffix) {
        if (font.measure(suffix) > max) {
            return suffix;
        }
        // Largest prefix length n in [0, len - 1] with measure(prefix(n) + suffix) <= max.
        int lo = 0;
        int hi = text.length() - 1;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (font.measure(text.substring(0, mid) + suffix) <= max) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        if (lo > 0 && Character.isHighSurrogate(text.charAt(lo - 1))) {
            lo--;
        }
        return lo == 0 ? suffix : text.substring(0, lo) + suffix;
    }

    private static Cache cache() {
        Cache cache = CACHE.get();
        if (cache.generation != generation) {
            cache.widths.clear();
            cache.cuts.clear();
            cache.generation = generation;
        }
        return cache;
    }

    private static final class Cache {
        final Map<Key, Float> widths = new Lru<Float>();
        final Map<Key, String> cuts = new Lru<String>();
        final Key probe = new Key();
        int generation = TextFit.generation;
    }

    private static final class Lru<V> extends LinkedHashMap<Key, V> {
        Lru() {
            super(256, 0.75f, true);
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<Key, V> eldest) {
            return size() > CAPACITY;
        }
    }

    private static final class Key {
        Class<?> fontClass;
        int size;
        String text;
        float max;
        String suffix;
        int hash;

        Key set(FontHandle font, String text, float max, String suffix) {
            this.fontClass = font.getClass();
            this.size = font.size();
            this.text = text;
            this.max = max;
            this.suffix = suffix;
            int h = fontClass.hashCode();
            h = 31 * h + size;
            h = 31 * h + text.hashCode();
            h = 31 * h + Float.floatToIntBits(max);
            h = 31 * h + (suffix == null ? 0 : suffix.hashCode());
            this.hash = h;
            return this;
        }

        Key copy() {
            Key k = new Key();
            k.fontClass = fontClass;
            k.size = size;
            k.text = text;
            k.max = max;
            k.suffix = suffix;
            k.hash = hash;
            return k;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Key)) {
                return false;
            }
            Key k = (Key) o;
            return hash == k.hash && size == k.size && fontClass == k.fontClass
                    && Float.floatToIntBits(max) == Float.floatToIntBits(k.max)
                    && text.equals(k.text)
                    && (suffix == null ? k.suffix == null : suffix.equals(k.suffix));
        }
    }
}

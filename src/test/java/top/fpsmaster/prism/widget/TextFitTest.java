package top.fpsmaster.prism.widget;

import org.junit.jupiter.api.Test;
import top.fpsmaster.prism.canvas.FontHandle;
import top.fpsmaster.prism.test.RecordingFont;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextFitTest {
    /** Counts measurements so the test can see the binary search and the cache work. */
    private static final class CountingFont implements FontHandle {
        final RecordingFont inner = new RecordingFont(10);
        int calls;

        public int size() {
            return 777;
        }

        public float measure(String text) {
            calls++;
            return inner.measure(text);
        }

        public float lineHeight() {
            return inner.lineHeight();
        }
    }

    @Test
    void fitsTextUntouchedAndCutsLongestFittingPrefix() {
        FontHandle font = new RecordingFont(10); // 5.5 per char
        assertEquals("abc", TextFit.ellipsize(font, "abc", 100f));
        // 30 units: "abc..." is 6 chars = 33 > 30, "ab..." = 27.5 fits.
        assertEquals("ab...", TextFit.ellipsize(font, "abcdefgh", 30f));
        assertEquals("...", TextFit.ellipsize(font, "abcdefgh", 17f));
        assertEquals("...", TextFit.ellipsize(font, "abcdefgh", 1f));
        assertEquals("", TextFit.ellipsize(font, null, 10f));
    }

    @Test
    void longTextNeedsLogarithmicMeasurementsAndIsCached() {
        TextFit.clearCache();
        CountingFont font = new CountingFont();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 2000; i++) {
            sb.append('x');
        }
        String text = sb.toString();
        String cut = TextFit.ellipsize(font, text, 100f, TextFit.ELLIPSIS);
        assertTrue(cut.endsWith(TextFit.ELLIPSIS));
        assertTrue(font.calls < 20, "measured " + font.calls + " times");
        int before = font.calls;
        assertEquals(cut, TextFit.ellipsize(font, text, 100f, TextFit.ELLIPSIS));
        assertEquals(before, font.calls);
        TextFit.clearCache();
        TextFit.ellipsize(font, text, 100f, TextFit.ELLIPSIS);
        assertTrue(font.calls > before);
    }

    @Test
    void neverSplitsSurrogatePairs() {
        FontHandle font = new RecordingFont(10);
        String text = "a😀😀😀";
        for (float max = 10f; max < 60f; max += 1f) {
            String cut = TextFit.ellipsize(font, text, max);
            String body = cut.endsWith("...") ? cut.substring(0, cut.length() - 3) : cut;
            assertTrue(body.isEmpty() || !Character.isHighSurrogate(body.charAt(body.length() - 1)), cut);
        }
    }
}

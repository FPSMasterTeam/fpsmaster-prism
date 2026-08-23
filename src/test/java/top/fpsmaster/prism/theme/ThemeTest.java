package top.fpsmaster.prism.theme;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ThemeTest {
    @Test
    void accentUsesSharedInteractionColor() {
        assertEquals(0xFF5965F1, Theme.DARK.accent());
        assertNotEquals(Theme.DARK.accent(), Theme.DARK.ok());
        assertNotEquals(Theme.DARK.accent(), Theme.DARK.warning());
        assertNotEquals(Theme.DARK.accent(), Theme.DARK.danger());
    }

    @Test
    void glassFallsBackWhenBlurOff() {
        assertEquals(Argb.of(240, 14, 14, 14), Theme.DARK.glass());
        assertEquals(Argb.of(209, 18, 18, 18), Theme.DARK_BLUR.glass());
        assertNotEquals(Theme.DARK.glass(), Theme.DARK_BLUR.glass());
    }
}

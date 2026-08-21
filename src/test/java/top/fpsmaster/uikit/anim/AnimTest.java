package top.fpsmaster.uikit.anim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimTest {
    @Test
    void cssEasePinsEnds() {
        assertEquals(0f, Anim.cssEase(0f), 1e-5f);
        assertEquals(1f, Anim.cssEase(1f), 1e-4f);
        float mid = Anim.cssEase(0.5f);
        assertTrue(mid > 0.4f && mid < 0.9f);
    }

    @Test
    void approachMovesTowardTarget() {
        float v = 0f;
        for (int i = 0; i < 20; i++) {
            v = Anim.approach(v, 1f, 0.25f, 1f / 60f);
        }
        assertTrue(v > 0.9f);
        assertEquals(1f, Anim.approach(0.999f, 1f, 0.25f, 1f / 60f), 1e-4f);
    }
}

package top.fpsmaster.prism.geom;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HitTest {
    @Test
    void insideIncludesEdges() {
        assertTrue(Hit.inside(10, 10, 20, 8, 10, 10));
        assertTrue(Hit.inside(10, 10, 20, 8, 30, 18));
        assertFalse(Hit.inside(10, 10, 20, 8, 9, 14));
        assertFalse(Hit.inside(10, 10, 20, 8, 20, 19));
    }
}

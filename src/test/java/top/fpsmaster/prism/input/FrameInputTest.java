package top.fpsmaster.prism.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FrameInputTest {
    @Test
    void rawKeyIsConsumedOnceAndClearedAtFrameEnd() {
        FrameInput input = new FrameInput();
        input.pressRawKey(42);
        assertEquals(42, input.consumeRawKey());
        assertEquals(-1, input.consumeRawKey());
        input.pressRawKey(7);
        input.endFrame();
        assertEquals(-1, input.consumeRawKey());
    }
}

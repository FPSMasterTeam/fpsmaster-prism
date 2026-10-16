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

    @Test
    void clipboardGoesThroughHostBackend() {
        FrameInput input = new FrameInput();
        final String[] system = {"from-os"};
        input.setClipboardBackend(() -> system[0], text -> system[0] = text);
        assertEquals("from-os", input.clipboard());
        input.setClipboard("copied");
        assertEquals("copied", system[0]);
    }
}

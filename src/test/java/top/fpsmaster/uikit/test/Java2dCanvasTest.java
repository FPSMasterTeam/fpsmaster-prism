package top.fpsmaster.uikit.test;

import org.junit.jupiter.api.Test;
import top.fpsmaster.uikit.host.UiHost;
import top.fpsmaster.uikit.input.FrameInput;
import top.fpsmaster.uikit.input.Input;
import top.fpsmaster.uikit.theme.Theme;
import top.fpsmaster.uikit.widget.Chrome;
import top.fpsmaster.uikit.widget.UiFrame;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Java2dCanvasTest {
    @Test
    void panelPaintsOpaquePixels() {
        final Java2dCanvas canvas = new Java2dCanvas(320, 180);
        final FrameInput input = new FrameInput();
        UiHost host = new UiHost() {
            public top.fpsmaster.uikit.canvas.Canvas canvas() {
                return canvas;
            }

            public Input input() {
                return input;
            }

            public top.fpsmaster.uikit.canvas.FontHandle font(int size) {
                return new Java2dCanvas.Java2dFont(size);
            }

            public float width() {
                return 320f;
            }

            public float height() {
                return 180f;
            }

            public long nowNanos() {
                return 0L;
            }

            public boolean blurEnabled() {
                return false;
            }

            public void blurBehind(float x, float y, float w, float h, float radius) {
            }

            public top.fpsmaster.uikit.canvas.ImageHandle image(String id) {
                return null;
            }
        };
        UiFrame ui = new UiFrame(host, Theme.DARK);
        Chrome.panel(ui, 16, 16, 200, 80);
        Chrome.button(ui, 28, 32, 64, 18, "Save", Chrome.ButtonStyle.PRIMARY);
        canvas.dispose();
        BufferedImage image = canvas.image;
        int samples = 0;
        int painted = 0;
        for (int x = 20; x < 180; x += 8) {
            for (int y = 20; y < 80; y += 8) {
                samples++;
                if ((image.getRGB(x, y) >>> 24) != 0) {
                    painted++;
                }
            }
        }
        assertTrue(painted > samples / 2, "panel should cover most of its rect");
        assertNotEquals(0, image.getRGB(40, 40));
    }
}

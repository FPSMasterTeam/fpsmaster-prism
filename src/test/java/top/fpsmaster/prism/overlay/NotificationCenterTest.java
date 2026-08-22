package top.fpsmaster.prism.overlay;

import org.junit.jupiter.api.Test;
import top.fpsmaster.prism.test.HeadlessHost;
import top.fpsmaster.prism.theme.Theme;
import top.fpsmaster.prism.widget.UiFrame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationCenterTest {
    @Test
    void paintsSharedCardAndText() {
        NotificationCenter center = new NotificationCenter();
        center.add("Enabled", "Keystrokes", NotificationCenter.Type.SUCCESS, 2f);
        HeadlessHost host = new HeadlessHost(320, 180);

        center.paint(new UiFrame(host, Theme.DARK));

        assertEquals(1, center.size());
        assertTrue(host.canvas.has("drawString:Enabled"));
        assertTrue(host.canvas.has("drawString:Keystrokes"));
        assertTrue(host.canvas.has("strokeRoundRect"));
    }
}

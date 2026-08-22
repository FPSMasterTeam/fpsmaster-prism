package top.fpsmaster.prism.screen;

import org.junit.jupiter.api.Test;
import top.fpsmaster.prism.test.HeadlessHost;
import top.fpsmaster.prism.theme.Theme;
import top.fpsmaster.prism.widget.UiFrame;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedLyricsTest {
    @Test
    void hudHeightFollowsLineAndTranslationSettings() {
        SharedLyrics.HudStyle style = new SharedLyrics.HudStyle(18, 2, true, true,
                true, 0x99000000, 0xFFFFFFFF);
        assertEquals(48f, SharedLyrics.hudHeight(style));
    }

    @Test
    void hudDrawsCurrentLyricAndTranslation() {
        HeadlessHost host = new HeadlessHost(280, 90);
        SharedLyrics lyrics = new SharedLyrics();
        SharedLyrics.HudStyle style = new SharedLyrics.HudStyle(18, 2, true, true,
                true, 0x99000000, 0xFFFFFFFF);
        lyrics.drawHud(new UiFrame(host, Theme.DARK), Arrays.asList(
                new MusicBridge.LyricRow("First", "第一句"),
                new MusicBridge.LyricRow("Second", "第二句")), 0, 0f, 0f, 260f, style, 0.016f);
        assertTrue(host.canvas.has("drawString:First"));
        assertTrue(host.canvas.has("drawString:第一句"));
    }
}

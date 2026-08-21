package top.fpsmaster.prism.screen;

import top.fpsmaster.prism.widget.UiFrame;

final class FontBold {
    static void draw(UiFrame ui, int size, String text, float x, float y, int color) {
        ui.canvas().drawString(ui.font(size), text, x, y, color);
        ui.canvas().drawString(ui.font(size), text, x + 0.4f, y, color);
    }
}

package top.fpsmaster.prism.overlay;

import top.fpsmaster.prism.anim.Anim;
import top.fpsmaster.prism.canvas.FontHandle;
import top.fpsmaster.prism.theme.Argb;
import top.fpsmaster.prism.widget.Chrome;
import top.fpsmaster.prism.widget.UiFrame;

import java.util.concurrent.CopyOnWriteArrayList;

/** Shared notification queue, layout and motion for native HUD overlays. */
public final class NotificationCenter {
    public enum Type {
        INFO, SUCCESS, WARNING, ERROR
    }

    private static final float HEIGHT = 31f;
    private static final float GAP = 6f;
    private final CopyOnWriteArrayList<Entry> entries = new CopyOnWriteArrayList<Entry>();

    public void add(String title, String description, Type type, float durationSeconds) {
        long now = System.nanoTime();
        long duration = (long) (Math.max(0.3f, durationSeconds) * 1_000_000_000L);
        entries.add(new Entry(title, description, type == null ? Type.INFO : type, now, duration));
    }

    public void paint(UiFrame ui) {
        long now = ui.host().nowNanos();
        float y = 10f;
        float maxWidth = Math.max(100f, Math.min(220f, ui.host().width() - 20f));
        for (Entry entry : entries) {
            float age = (now - entry.createdAt) / 1_000_000_000f;
            float lifetime = entry.durationNanos / 1_000_000_000f;
            float target = age < lifetime ? 1f : 0f;
            float dt = entry.lastNanos == 0L ? 0.016f
                    : Math.min(0.05f, Math.max(0f, (now - entry.lastNanos) / 1_000_000_000f));
            entry.lastNanos = now;
            entry.visibility = Anim.approach(entry.visibility, target, target > 0f ? 0.2f : 0.26f, dt);
            entry.y = Anim.approach(entry.y, y, 0.2f, dt);
            if (target == 0f && entry.visibility <= 0.002f) {
                entries.remove(entry);
                continue;
            }

            FontHandle titleFont = ui.font(13);
            FontHandle bodyFont = ui.font(11);
            float natural = Math.max(titleFont.measure(entry.title), bodyFont.measure(entry.description)) + 31f;
            float width = Math.max(126f, Math.min(maxWidth, natural));
            float x = ui.host().width() - 10f - width * entry.visibility;
            int tone = color(ui, entry.type);

            ui.canvas().pushAlpha(Anim.cssEase(entry.visibility));
            ui.canvas().fillRoundRect(x, entry.y, width, HEIGHT, 6f, ui.theme().opaquePanelBase());
            ui.canvas().strokeRoundRect(x + 0.5f, entry.y + 0.5f, width - 1f, HEIGHT - 1f,
                    5.5f, 1f, ui.theme().strokeStrong());
            ui.canvas().fillRoundRect(x + 5f, entry.y + 7f, 3f, HEIGHT - 14f, 1.5f, tone);
            ui.canvas().pushClip(x + 13f, entry.y + 2f, width - 19f, HEIGHT - 4f);
            ui.canvas().drawString(titleFont, ellipsize(titleFont, entry.title, width - 22f),
                    x + 13f, entry.y + 4f, ui.theme().textPrimary());
            ui.canvas().drawString(bodyFont, ellipsize(bodyFont, entry.description, width - 22f),
                    x + 13f, entry.y + 17f, ui.theme().textSecondary());
            ui.canvas().popClip();
            float progress = Math.max(0f, Math.min(1f, age / lifetime));
            ui.canvas().fillRoundRect(x + 5f, entry.y + HEIGHT - 2f,
                    Math.max(1f, (width - 10f) * (1f - progress)), 1f, 0.5f, Argb.withAlpha(tone, 150));
            ui.canvas().popAlpha();
            y += (HEIGHT + GAP) * entry.visibility;
        }
    }

    public int size() {
        return entries.size();
    }

    private static int color(UiFrame ui, Type type) {
        switch (type) {
            case SUCCESS:
                return ui.theme().ok();
            case WARNING:
                return ui.theme().warning();
            case ERROR:
                return ui.theme().danger();
            default:
                return ui.theme().accent();
        }
    }

    private static String ellipsize(FontHandle font, String text, float maxWidth) {
        String value = text == null ? "" : text;
        if (font.measure(value) <= maxWidth) {
            return value;
        }
        String suffix = "...";
        for (int i = value.length() - 1; i > 0; i--) {
            String candidate = value.substring(0, i) + suffix;
            if (font.measure(candidate) <= maxWidth) {
                return candidate;
            }
        }
        return suffix;
    }

    private static final class Entry {
        final String title;
        final String description;
        final Type type;
        final long createdAt;
        final long durationNanos;
        long lastNanos;
        float visibility;
        float y = 10f;

        Entry(String title, String description, Type type, long createdAt, long durationNanos) {
            this.title = title == null ? "" : title;
            this.description = description == null ? "" : description;
            this.type = type;
            this.createdAt = createdAt;
            this.durationNanos = durationNanos;
        }
    }
}

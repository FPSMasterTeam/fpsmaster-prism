package top.fpsmaster.prism.anim;

/**
 * Frame-rate-aware motion used by shared screens. {@link #approach} matches Edge
 * {@code AnimMath.base}; {@link #cssEase} is the Click GUI open curve
 * {@code cubic-bezier(0.25, 0.1, 0.25, 1)}.
 */
public final class Anim {
    private static boolean enabled = true;

    private Anim() {
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static float approach(float current, float target, float speed, float dtSec) {
        if (!enabled) {
            return target;
        }
        float fps = dtSec <= 1e-4f ? 60f : 1f / dtSec;
        if (fps < 5f) {
            fps = 60f;
        }
        float next = current + (target - current) * speed / (fps / 60f);
        if (Float.isNaN(next) || Float.isInfinite(next)) {
            return target;
        }
        if (Math.abs(next - target) < 0.002f) {
            return target;
        }
        return next;
    }

    public static float clamp01(float t) {
        if (t < 0f) {
            return 0f;
        }
        if (t > 1f) {
            return 1f;
        }
        return t;
    }

    /** CSS {@code ease} — cubic-bezier(0.25, 0.1, 0.25, 1). */
    public static float cssEase(float t) {
        return cubicBezier(clamp01(t), 0.25f, 0.1f, 0.25f, 1f);
    }

    private static float cubicBezier(float x, float x1, float y1, float x2, float y2) {
        float guess = x;
        for (int i = 0; i < 6; i++) {
            float u = 1f - guess;
            float bx = 3f * u * u * guess * x1 + 3f * u * guess * guess * x2 + guess * guess * guess;
            float dx = 3f * u * u * x1 + 6f * u * guess * (x2 - x1) + 3f * guess * guess * (1f - x2);
            if (Math.abs(dx) < 1e-6f) {
                break;
            }
            guess -= (bx - x) / dx;
            if (guess < 0f) {
                guess = 0f;
            } else if (guess > 1f) {
                guess = 1f;
            }
        }
        float u = 1f - guess;
        return 3f * u * u * guess * y1 + 3f * u * guess * guess * y2 + guess * guess * guess;
    }
}

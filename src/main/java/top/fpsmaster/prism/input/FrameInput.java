package top.fpsmaster.prism.input;

import top.fpsmaster.prism.geom.Hit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Concrete {@link Input} for hosts and tests. Feed {@link #setMouse}, {@link #press},
 * {@link #release}, {@link #addWheel}, then let widgets consume; call {@link #endFrame()}
 * after painting.
 */
public final class FrameInput implements Input {
    private static final class Press {
        final PointerEvent event;
        boolean consumed;

        Press(PointerEvent event) {
            this.event = event;
        }
    }

    private final List<Press> presses = new ArrayList<Press>();
    private final boolean[] buttonsDown = new boolean[8];
    private final Set<Integer> keysDown = new HashSet<Integer>();
    private final Set<Integer> keyPresses = new HashSet<Integer>();
    private int rawKey = -1;
    private final StringBuilder typed = new StringBuilder();

    private int mouseX;
    private int mouseY;
    private int wheel;
    private Object hoveredId;
    private Object lastHoveredId;
    private Object dragOwner;
    private int dragButton = -1;
    private String clipboard = "";

    public void setMouse(int x, int y) {
        this.mouseX = x;
        this.mouseY = y;
    }

    public void press(int button, int x, int y) {
        if (button >= 0 && button < buttonsDown.length) {
            buttonsDown[button] = true;
        }
        presses.add(new Press(new PointerEvent(x, y, button)));
        mouseX = x;
        mouseY = y;
    }

    public void release(int button) {
        if (button >= 0 && button < buttonsDown.length) {
            buttonsDown[button] = false;
        }
        if (dragButton == button) {
            dragOwner = null;
            dragButton = -1;
        }
    }

    public void addWheel(int delta) {
        wheel += delta;
    }

    public void setKeyDown(int keyCode, boolean down) {
        if (down) {
            if (keysDown.add(Integer.valueOf(keyCode))) {
                keyPresses.add(Integer.valueOf(keyCode));
            }
        } else {
            keysDown.remove(Integer.valueOf(keyCode));
        }
    }

    public void pressKey(int keyCode) {
        keyPresses.add(Integer.valueOf(keyCode));
        keysDown.add(Integer.valueOf(keyCode));
    }

    public void pressRawKey(int keyCode) {
        rawKey = keyCode;
    }

    public void type(String chars) {
        if (chars != null) {
            typed.append(chars);
        }
    }

    public void endFrame() {
        lastHoveredId = hoveredId;
        hoveredId = null;
        presses.clear();
        wheel = 0;
        typed.setLength(0);
        keyPresses.clear();
        rawKey = -1;
    }

    public int mouseX() {
        return mouseX;
    }

    public int mouseY() {
        return mouseY;
    }

    public boolean isButtonDown(int button) {
        return button >= 0 && button < buttonsDown.length && buttonsDown[button];
    }

    public PointerEvent consumePressInBounds(float x, float y, float w, float h, int button) {
        for (int i = 0; i < presses.size(); i++) {
            Press press = presses.get(i);
            if (press.consumed) {
                continue;
            }
            if (button >= 0 && press.event.button != button) {
                continue;
            }
            if (!Hit.inside(x, y, w, h, press.event.x, press.event.y)) {
                continue;
            }
            press.consumed = true;
            return press.event;
        }
        return null;
    }

    public PointerEvent consumePressOutside(float x, float y, float w, float h) {
        for (int i = 0; i < presses.size(); i++) {
            Press press = presses.get(i);
            if (press.consumed) {
                continue;
            }
            if (Hit.inside(x, y, w, h, press.event.x, press.event.y)) {
                continue;
            }
            press.consumed = true;
            return press.event;
        }
        return null;
    }

    public boolean hasPressOutside(float x, float y, float w, float h) {
        for (int i = 0; i < presses.size(); i++) {
            Press press = presses.get(i);
            if (press.consumed) {
                continue;
            }
            if (!Hit.inside(x, y, w, h, press.event.x, press.event.y)) {
                return true;
            }
        }
        return false;
    }

    public int consumeWheelDelta(float x, float y, float w, float h) {
        if (!Hit.inside(x, y, w, h, mouseX, mouseY)) {
            return 0;
        }
        int d = wheel;
        wheel = 0;
        return d;
    }

    public void markHovered(Object id, float x, float y, float w, float h) {
        if (Hit.inside(x, y, w, h, mouseX, mouseY)) {
            hoveredId = id;
        }
    }

    public boolean wasHovered(Object id) {
        return id != null && id.equals(lastHoveredId);
    }

    public boolean beginDrag(Object owner, int button, float x, float y, float w, float h) {
        if (dragOwner != null) {
            return owner.equals(dragOwner);
        }
        if (consumePressInBounds(x, y, w, h, button) == null) {
            return false;
        }
        dragOwner = owner;
        dragButton = button;
        return true;
    }

    public boolean isDragging(Object owner) {
        return owner != null && owner.equals(dragOwner);
    }

    public void releaseDrag(Object owner) {
        if (owner != null && owner.equals(dragOwner)) {
            dragOwner = null;
            dragButton = -1;
        }
    }

    public boolean isKeyDown(int keyCode) {
        return keysDown.contains(Integer.valueOf(keyCode));
    }

    public boolean consumeKey(int keyCode) {
        return keyPresses.remove(Integer.valueOf(keyCode));
    }

    public int consumeRawKey() {
        int key = rawKey;
        rawKey = -1;
        return key;
    }

    public String typedChars() {
        return typed.toString();
    }

    public String clipboard() {
        return clipboard;
    }

    public void setClipboard(String text) {
        this.clipboard = text == null ? "" : text;
    }
}

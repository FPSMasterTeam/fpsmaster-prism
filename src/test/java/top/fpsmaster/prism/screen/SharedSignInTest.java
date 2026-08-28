package top.fpsmaster.prism.screen;

import org.junit.jupiter.api.Test;
import top.fpsmaster.prism.test.HeadlessHost;
import top.fpsmaster.prism.theme.Theme;
import top.fpsmaster.prism.widget.UiFrame;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 钉住登录表单的焦点裁决。
 *
 * <p>要防的是「同一帧两个框都把玩家输的字吃进去」：{@code TextBox} 在 {@code draw} 里就
 * 读字符，两个框都拿到焦点时账号里输的字会同时长在密码框里，提交时报「密码错误」而玩家
 * 什么都没输错。现在有两道闸：{@code paintForm} 在两次 draw **之前**就把按下消费掉、
 * 把焦点定死，{@code TextBox} 读的又是消费式的 {@code consumeTypedChars()}。
 * 谁把点击裁决挪到 draw 之后、或者把读字符换回非消费式，这几条用例都会红。
 */
class SharedSignInTest {
    private static final int W = 480;
    private static final int H = 300;
    /** 卡片居中，未登录高 156；账号/密码框在 y+44 / y+70，高 20。 */
    private static final int CARD_X = (W - 232) / 2;
    private static final int CARD_Y = (H - 156) / 2;
    private static final int ACCOUNT_X = CARD_X + 100;
    private static final int ACCOUNT_Y = CARD_Y + 54;
    private static final int PASSWORD_Y = CARD_Y + 80;

    @Test
    void clickingAccountWhilePasswordFocusedDoesNotDoubleWrite() {
        HeadlessHost host = new HeadlessHost(W, H);
        SharedSignIn gui = new SharedSignIn();
        StubBridge bridge = new StubBridge();
        gui.reset();

        // 第一帧：点密码框并输入两个字符。
        host.input.setMouse(ACCOUNT_X, PASSWORD_Y);
        host.input.press(0, ACCOUNT_X, PASSWORD_Y);
        host.input.type("pw");
        gui.paint(new UiFrame(host, Theme.DARK), bridge);
        assertTrue(host.canvas.has("drawString:••"), "password should hold 2 chars");
        host.input.endFrame();

        // 第二帧：点账号框并输入三个字符。密码框必须一个字符都不再吃。
        host.canvas.ops.clear();
        host.input.setMouse(ACCOUNT_X, ACCOUNT_Y);
        host.input.press(0, ACCOUNT_X, ACCOUNT_Y);
        host.input.type("abc");
        gui.paint(new UiFrame(host, Theme.DARK), bridge);
        assertTrue(host.canvas.has("drawString:abc"), "account should have taken the characters");
        assertTrue(host.canvas.has("drawString:••"), "password should still hold exactly 2 chars");
        assertFalse(host.canvas.has("drawString:•••••"),
                "password must not append the account's characters");
    }

    @Test
    void clickingPasswordWhileAccountFocusedDoesNotDoubleWrite() {
        HeadlessHost host = new HeadlessHost(W, H);
        SharedSignIn gui = new SharedSignIn();
        StubBridge bridge = new StubBridge();
        // reset() 让账号框默认持有焦点，这就是反方向的起点。
        gui.reset();

        host.input.setMouse(ACCOUNT_X, ACCOUNT_Y);
        host.input.press(0, ACCOUNT_X, ACCOUNT_Y);
        host.input.type("ab");
        gui.paint(new UiFrame(host, Theme.DARK), bridge);
        assertTrue(host.canvas.has("drawString:ab"));
        host.input.endFrame();

        host.canvas.ops.clear();
        host.input.setMouse(ACCOUNT_X, PASSWORD_Y);
        host.input.press(0, ACCOUNT_X, PASSWORD_Y);
        host.input.type("xyz");
        gui.paint(new UiFrame(host, Theme.DARK), bridge);
        assertTrue(host.canvas.has("drawString:•••"), "password should hold 3 chars");
        assertTrue(host.canvas.has("drawString:ab"), "account must not append the password's characters");
        assertFalse(host.canvas.has("drawString:abxyz"));
    }

    /**
     * 同一帧两次按下（宿主一帧塞进两次事件、或抖了一下双击）：两个框都会 clicked 成功。
     * 裁决在两次 draw 之前跑，所以密码框这一帧连焦点都没有，画面上也不该闪出圆点——
     * 检查就放在同一帧。
     */
    @Test
    void twoPressesInOneFrameDoNotLeakCharactersIntoPassword() {
        HeadlessHost host = new HeadlessHost(W, H);
        SharedSignIn gui = new SharedSignIn();
        StubBridge bridge = new StubBridge();
        gui.reset();

        host.input.setMouse(ACCOUNT_X, ACCOUNT_Y);
        host.input.press(0, ACCOUNT_X, ACCOUNT_Y);
        host.input.press(0, ACCOUNT_X, PASSWORD_Y);
        host.input.type("abc");
        gui.paint(new UiFrame(host, Theme.DARK), bridge);
        assertTrue(host.canvas.has("drawString:abc"), "account should have taken the characters");
        assertFalse(host.canvas.has("drawString:•••"),
                "password must not take a second copy of the account's characters");

        // 下一帧状态也得是对的：提交按钮读的是这份状态，不能把多出来的字符发给后端。
        host.input.endFrame();
        host.canvas.ops.clear();
        gui.paint(new UiFrame(host, Theme.DARK), bridge);
        assertTrue(host.canvas.has("drawString:abc"));
        assertFalse(host.canvas.has("drawString:•••"));
    }

    /**
     * 点了账号框，同一帧还落了第三次按下、位置在两个框之外（拖窗口、点到卡片空白处）。
     *
     * <p>{@code TextBox.draw} 里那句「框外有未消费按下就失焦」会把裁决刚定下的焦点抹掉，
     * 于是玩家点完账号框直接打字，一个字符都进不去。{@code paintForm} 在两次 draw 之后
     * 把焦点重新钉一遍就是为了这个；删掉那几行，这条用例会红。
     */
    @Test
    void pressOutsideInTheSameFrameDoesNotStealFocusFromTheClickedBox() {
        HeadlessHost host = new HeadlessHost(W, H);
        SharedSignIn gui = new SharedSignIn();
        StubBridge bridge = new StubBridge();
        gui.reset();
        // 起点是密码框持有焦点，这样「账号框拿到焦点」只可能来自这一帧的点击。
        host.input.setMouse(ACCOUNT_X, PASSWORD_Y);
        host.input.press(0, ACCOUNT_X, PASSWORD_Y);
        gui.paint(new UiFrame(host, Theme.DARK), bridge);
        host.input.endFrame();

        host.input.setMouse(ACCOUNT_X, ACCOUNT_Y);
        host.input.press(0, ACCOUNT_X, ACCOUNT_Y);
        host.input.press(0, 10, 10);
        gui.paint(new UiFrame(host, Theme.DARK), bridge);
        host.input.endFrame();

        host.canvas.ops.clear();
        host.input.type("ab");
        gui.paint(new UiFrame(host, Theme.DARK), bridge);
        assertTrue(host.canvas.has("drawString:ab"),
                "the clicked box must keep focus and take the characters");
    }

    private static final class StubBridge implements SignInBridge {
        final List<String> actions = new ArrayList<>();

        @Override
        public String i18n(String key) {
            return key;
        }

        @Override
        public boolean signedIn() {
            return false;
        }

        @Override
        public String accountName() {
            return "";
        }

        @Override
        public boolean busy() {
            return false;
        }

        @Override
        public String error() {
            return "";
        }

        @Override
        public void submit(String account, String password) {
            actions.add("submit:" + account);
        }

        @Override
        public void signOut() {
            actions.add("signout");
        }

        @Override
        public void close() {
            actions.add("close");
        }

        @Override
        public boolean canOpenWebsite() {
            return false;
        }

        @Override
        public void openWebsite() {
            actions.add("website");
        }
    }
}

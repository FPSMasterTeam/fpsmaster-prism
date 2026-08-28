package top.fpsmaster.prism.screen;

import top.fpsmaster.prism.canvas.FontHandle;
import top.fpsmaster.prism.input.Keys;
import top.fpsmaster.prism.theme.Metrics;
import top.fpsmaster.prism.widget.Chrome;
import top.fpsmaster.prism.widget.TextBox;
import top.fpsmaster.prism.widget.UiFrame;

/**
 * FPSMaster account sign-in card, shared by Edge and Nova. Hosts own the instance so the typed
 * account survives a {@code resize} (both clients rebuild widgets on resize, not the screen).
 *
 * <p>Immediate mode: {@link #paint} both draws and dispatches, so every {@link SignInBridge} call
 * lands inside the host's render pass. Hosts that switch screens from a bridge callback must defer
 * it the same way they do for the rest of the shared screens.
 */
public final class SharedSignIn {
    private static final float CARD_W = 232f;
    private static final float SIGNED_OUT_H = 156f;
    private static final float SIGNED_IN_H = 116f;

    private final TextBox account = new TextBox();
    private final TextBox password = new TextBox();
    private boolean localError;

    public SharedSignIn() {
        account.setFontSize(12);
        password.setFontSize(12);
        password.setMasked(true);
    }

    /** Clears the fields; call when the screen is (re)opened. */
    public void reset() {
        account.setText("");
        password.setText("");
        account.setFocused(true);
        password.setFocused(false);
        localError = false;
    }

    public void paint(UiFrame ui, SignInBridge bridge) {
        // 和账号浮层的两个对话框同一套底：宿主只管把自己的背景画出来，压暗由这里统一做。
        Chrome.veil(ui, 0.9f);
        boolean signedIn = bridge.signedIn();
        float w = CARD_W;
        float h = signedIn ? SIGNED_IN_H : SIGNED_OUT_H;
        float x = (ui.host().width() - w) / 2f;
        float y = (ui.host().height() - h) / 2f;
        Chrome.panel(ui, x, y, w, h);
        FontBold.draw(ui, 16, bridge.i18n("signin.title"), x + 13f, y + 12f, ui.theme().textPrimary());
        ui.canvas().drawString(ui.font(12), bridge.i18n(signedIn ? "signin.signedin.desc" : "signin.desc"),
                x + 13f, y + 26f, ui.theme().textSecondary());

        // ESC 先于按钮处理：登录中按 ESC 也应该能退出界面，请求自己会走完。
        if (ui.input().consumeKey(Keys.ESCAPE)) {
            bridge.close();
            return;
        }

        if (signedIn) {
            paintSignedIn(ui, bridge, x, y, w, h);
        } else {
            paintForm(ui, bridge, x, y, w, h);
        }
    }

    private void paintSignedIn(UiFrame ui, SignInBridge bridge, float x, float y, float w, float h) {
        String name = bridge.accountName();
        if (name == null || name.isEmpty()) {
            name = bridge.i18n("signin.account.unknown");
        }
        float rowY = y + 46f;
        ui.canvas().fillRoundRect(x + 13f, rowY, w - 26f, 24f, 5f, ui.theme().glass());
        FontHandle font = ui.font(14);
        ui.canvas().drawString(font, name, x + 21f, Chrome.textY(rowY, 24f, font), ui.theme().textPrimary());

        float btnY = y + h - 28f;
        float closeW = 52f;
        float outW = 68f;
        if (Chrome.button(ui, x + w - 13f - closeW, btnY, closeW, Metrics.BTN_H,
                bridge.i18n("signin.close"), Chrome.ButtonStyle.GHOST)) {
            bridge.close();
            return;
        }
        String outLabel = bridge.busy() ? bridge.i18n("signin.busy") : bridge.i18n("signin.signout");
        if (Chrome.button(ui, x + w - 13f - closeW - 6f - outW, btnY, outW, Metrics.BTN_H,
                outLabel, bridge.busy() ? Chrome.ButtonStyle.DEFAULT : Chrome.ButtonStyle.DANGER)
                && !bridge.busy()) {
            bridge.signOut();
        }
    }

    private void paintForm(UiFrame ui, SignInBridge bridge, float x, float y, float w, float h) {
        float fieldW = w - 26f;
        account.setPlaceholder(bridge.i18n("signin.account.placeholder"));
        password.setPlaceholder(bridge.i18n("signin.password.placeholder"));
        float accountY = y + 44f;
        float passwordY = y + 70f;
        // 点击裁决必须在两次 draw **之前**做完。
        //
        // TextBox 在 draw 里就把 typedChars() 读走，而 typedChars() 是非消费式的：只要
        // 两个框这一帧都 clicked 成功，同一批字符就会被读两遍——玩家输账号，密码框里
        // 也悄悄多出同样几个字符，提交时报「密码错误」而他什么都没输错。而两个框同帧
        // 都 clicked 成功是真实可达的：宿主一帧塞进两次按下（FrameInput.presses 是个
        // 列表），先绘制者消费掉第一次并不妨碍后绘制者消费第二次。
        //
        // 所以这里把按下先消费掉、把焦点定死，两个框 draw 时都看不到点击。
        // 顺带的结果是这两个框的焦点从此完全由本方法和 reset() 决定，TextBox 自己
        // 再也不会给它们置焦——「同时有焦点」变成了结构上不可达，而不是靠事后裁决。
        boolean pressedAccount = ui.clicked(x + 13f, accountY, fieldW, 20f);
        boolean pressedPassword = ui.clicked(x + 13f, passwordY, fieldW, 20f);
        if (pressedAccount) {
            account.setFocused(true);
            password.setFocused(false);
        } else if (pressedPassword) {
            password.setFocused(true);
            account.setFocused(false);
        }
        account.draw(ui, x + 13f, accountY, fieldW, 20f);
        password.draw(ui, x + 13f, passwordY, fieldW, 20f);
        if (pressedAccount || pressedPassword) {
            // draw 里那句「框外有未消费按下就失焦」可能把刚定下的焦点又抹掉（同一帧还落了
            // 第三次按下时）。点了哪个框以这里的裁决为准。
            account.setFocused(pressedAccount);
            password.setFocused(!pressedAccount && pressedPassword);
        }

        // 两个框都填上之后，这条本地校验提示就不该再挡着后端返回的真实错误。
        if (localError && !account.text().trim().isEmpty() && !password.text().isEmpty()) {
            localError = false;
        }

        String message = bridge.error();
        int messageColor = ui.theme().danger();
        if (localError) {
            message = bridge.i18n("signin.empty");
        } else if (message == null || message.isEmpty()) {
            message = bridge.busy() ? bridge.i18n("signin.busy") : "";
            messageColor = ui.theme().textSecondary();
        }
        if (!message.isEmpty()) {
            ui.canvas().drawString(ui.font(11), clamp(ui.font(11), message, fieldW),
                    x + 13f, y + 95f, messageColor);
        }

        if (bridge.canOpenWebsite()) {
            String link = bridge.i18n("signin.website");
            FontHandle linkFont = ui.font(11);
            float linkW = linkFont.measure(link);
            boolean linkHover = ui.hovered(x + 13f, y + 108f, linkW, 11f);
            ui.canvas().drawString(linkFont, link, x + 13f, y + 108f,
                    linkHover ? ui.theme().accentText() : ui.theme().textDisabled());
            if (ui.clicked(x + 13f, y + 108f, linkW, 11f)) {
                bridge.openWebsite();
            }
        }

        float btnY = y + h - 28f;
        float cancelW = 52f;
        float submitW = 68f;
        if (Chrome.button(ui, x + w - 13f - cancelW, btnY, cancelW, Metrics.BTN_H,
                bridge.i18n("signin.close"), Chrome.ButtonStyle.GHOST)) {
            bridge.close();
            return;
        }
        boolean enterPressed = (account.focused() || password.focused())
                && ui.input().consumeKey(Keys.ENTER);
        boolean clicked = Chrome.button(ui, x + w - 13f - cancelW - 6f - submitW, btnY, submitW,
                Metrics.BTN_H, bridge.busy() ? bridge.i18n("signin.busy") : bridge.i18n("signin.submit"),
                bridge.busy() ? Chrome.ButtonStyle.DEFAULT : Chrome.ButtonStyle.PRIMARY);
        if ((clicked || enterPressed) && !bridge.busy()) {
            // 账号允许两侧空白（复制粘贴常带），密码不 trim：空格是合法密码字符。
            String user = account.text().trim();
            String secret = password.text();
            if (user.isEmpty() || secret.isEmpty()) {
                localError = true;
                return;
            }
            localError = false;
            bridge.submit(user, secret);
            // 提交后就把密码从控件里抹掉，失败时只需重输密码。
            password.setText("");
        }
    }

    private static String clamp(FontHandle font, String text, float maxWidth) {
        if (font.measure(text) <= maxWidth) {
            return text;
        }
        String shown = text;
        while (shown.length() > 4 && font.measure(shown + "…") > maxWidth) {
            shown = shown.substring(0, shown.length() - 1);
        }
        return shown + "…";
    }
}

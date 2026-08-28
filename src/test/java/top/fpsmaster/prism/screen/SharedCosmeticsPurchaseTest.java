package top.fpsmaster.prism.screen;

import org.junit.jupiter.api.Test;
import top.fpsmaster.prism.test.HeadlessHost;
import top.fpsmaster.prism.theme.Theme;
import top.fpsmaster.prism.widget.UiFrame;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 钉住购买这条路径：钱是一次性扣掉的，误点一下就没了。
 *
 * <p>要防三件事——「点一下购买就直接下单」、「余额不够还让下单」、以及「弹窗开着的时候
 * 底下的面板照样吃点击」（立即模式没有事件冒泡，模态得靠 draw 之前把按下吸干来实现）。
 */
class SharedCosmeticsPurchaseTest {
    private static final int W = 500;
    private static final int H = 320;

    /** 右栏底部的操作按钮：rightX(302.8) + rightW(187.2) - 98 起，宽 90 高 18，y = 285。 */
    private static final int BUY_X = 437;
    private static final int BUY_Y = 294;

    /** 弹窗居中 238×112 → x∈[131,369]、y∈[104,216]；按钮行 y = 188..206。 */
    private static final int DIALOG_BTN_Y = 197;
    /** 「确认购买」76 宽，靠右 13 内边距；也正好落在「余额不足」那一态的确定键上。 */
    private static final int CONFIRM_X = 318;
    /** 「取消」46 宽，紧挨在「确认购买」左边隔 6：x∈[228,274]。 */
    private static final int CANCEL_X = 250;
    /** 表头右上角的「自定义文件夹」按钮，弹窗之外。 */
    private static final int FOLDER_X = 450;
    private static final int FOLDER_Y = 22;

    @Test
    void buyOpensTheConfirmDialogInsteadOfOrdering() {
        HeadlessHost host = new HeadlessHost(W, H);
        SharedCosmetics gui = new SharedCosmetics();
        ShopBridge bridge = new ShopBridge();

        click(host, gui, bridge, BUY_X, BUY_Y);

        assertTrue(bridge.purchased.isEmpty(), "点一下购买不能直接下单");
        assertTrue(gui.blocking(), "购买按钮必须先开确认弹窗");
    }

    @Test
    void confirmingOrdersExactlyOnceAndClosesTheDialog() {
        HeadlessHost host = new HeadlessHost(W, H);
        SharedCosmetics gui = new SharedCosmetics();
        ShopBridge bridge = new ShopBridge();

        click(host, gui, bridge, BUY_X, BUY_Y);
        click(host, gui, bridge, CONFIRM_X, DIALOG_BTN_Y);

        assertEquals(1, bridge.purchased.size(), "确认之后正好下一单");
        assertEquals("cape:1", bridge.purchased.get(0));
        assertFalse(gui.blocking(), "下单之后弹窗必须关掉，否则等待期间还能再点一次");

        // 弹窗已经不在了，同一个位置再点一次不该变成第二单。
        click(host, gui, bridge, CONFIRM_X, DIALOG_BTN_Y);
        assertEquals(1, bridge.purchased.size());
    }

    @Test
    void insufficientBalanceBlocksTheOrder() {
        HeadlessHost host = new HeadlessHost(W, H);
        SharedCosmetics gui = new SharedCosmetics();
        ShopBridge bridge = new ShopBridge();
        bridge.balance = "10";

        click(host, gui, bridge, BUY_X, BUY_Y);
        assertTrue(gui.blocking());
        // 弹窗是在按下之后的那一帧才画出来的（confirmId 在面板 draw 的中途才置上）。
        paint(host, gui, bridge);
        assertTrue(host.canvas.has("drawString:Not enough"), "余额不足要走另一态的标题");

        // 「确认购买」在这一态根本不存在，同一个位置只剩一个「知道了」。
        click(host, gui, bridge, CONFIRM_X, DIALOG_BTN_Y);
        assertTrue(bridge.purchased.isEmpty(), "余额不足时不能下单");
        assertFalse(gui.blocking());
    }

    /**
     * 余额未知（没登录过、profile 还没回来）时不能把玩家拦下来：客户端拿的是缓存，
     * 拦错了他就买不成东西且不知道为什么。真买不起由后端挡。
     */
    @Test
    void unknownBalanceStillReachesTheConfirmPath() {
        HeadlessHost host = new HeadlessHost(W, H);
        SharedCosmetics gui = new SharedCosmetics();
        ShopBridge bridge = new ShopBridge();
        bridge.balance = "";

        click(host, gui, bridge, BUY_X, BUY_Y);
        click(host, gui, bridge, CONFIRM_X, DIALOG_BTN_Y);

        assertEquals(1, bridge.purchased.size(), "余额未知时照样能买");
    }

    /**
     * 弹窗开着的时候点到底下的面板：立即模式里底层控件是在自己的 draw 里读点击的，
     * 不在 draw 之前把窗外的按下吸干，玩家点「取消」旁边一点就会顺手把分类切了。
     */
    @Test
    void theDialogAbsorbsClicksOnThePanelBehind() {
        HeadlessHost host = new HeadlessHost(W, H);
        SharedCosmetics gui = new SharedCosmetics();
        ShopBridge bridge = new ShopBridge();

        click(host, gui, bridge, BUY_X, BUY_Y);
        click(host, gui, bridge, FOLDER_X, FOLDER_Y);

        assertFalse(bridge.customFolderOpened, "窗外的按下不能落到底下的按钮上");
        assertTrue(bridge.purchased.isEmpty());
        assertFalse(gui.blocking(), "点窗外等于取消");
    }

    @Test
    void headerShowsTheBalance() {
        HeadlessHost host = new HeadlessHost(W, H);
        SharedCosmetics gui = new SharedCosmetics();
        ShopBridge bridge = new ShopBridge();

        gui.draw(new UiFrame(host, Theme.DARK), bridge);

        assertTrue(host.canvas.has("drawString:1000 Coins"), "表头要显示余额");
    }

    /**
     * 双击「购买」：第二下落地时弹窗已经开着了，而按钮在弹窗外面——按「点窗外＝取消」
     * 处理的话，玩家双击一下等于什么都没发生，还以为界面卡了。
     */
    @Test
    void doubleClickOnBuyKeepsTheDialogOpen() {
        HeadlessHost host = new HeadlessHost(W, H);
        SharedCosmetics gui = new SharedCosmetics();
        ShopBridge bridge = new ShopBridge();

        click(host, gui, bridge, BUY_X, BUY_Y);
        click(host, gui, bridge, BUY_X, BUY_Y);

        assertTrue(gui.blocking(), "双击的第二下不能把自己开的窗关掉");
        assertTrue(bridge.purchased.isEmpty(), "更不能直接下单");
    }

    /**
     * 余额在弹窗开着的时候翻了面（刚充完值切回来）：按钮组整个换一套，这一帧的按下
     * 按的是上一帧那套按钮，不能拿它当数——否则玩家点的是「知道了」，落到的是「确认购买」。
     */
    @Test
    void aBalanceFlipDiscardsThatFramesClick() {
        HeadlessHost host = new HeadlessHost(W, H);
        SharedCosmetics gui = new SharedCosmetics();
        ShopBridge bridge = new ShopBridge();
        bridge.balance = "10";

        click(host, gui, bridge, BUY_X, BUY_Y);
        paint(host, gui, bridge);
        assertTrue(host.canvas.has("drawString:Not enough"));

        // 充值到账，同一帧手指正好落在「知道了」的位置上（那儿现在是「确认购买」）。
        bridge.balance = "5000";
        click(host, gui, bridge, CONFIRM_X, DIALOG_BTN_Y);
        assertTrue(bridge.purchased.isEmpty(), "翻面那一帧的按下必须作废");
        assertTrue(gui.blocking(), "窗还开着，等玩家看清了再点");

        paint(host, gui, bridge);
        assertTrue(host.canvas.has("drawString:Confirm"), "余额够了要自己变回确认态");
        click(host, gui, bridge, CONFIRM_X, DIALOG_BTN_Y);
        assertEquals(1, bridge.purchased.size(), "看清之后再点才算数");
    }

    /** 余额不足那一态每帧都要问宿主要不要刷余额，玩家切出去充完值切回来才能自己好。 */
    @Test
    void theInsufficientDialogKeepsAskingForAFreshBalance() {
        HeadlessHost host = new HeadlessHost(W, H);
        SharedCosmetics gui = new SharedCosmetics();
        ShopBridge bridge = new ShopBridge();
        bridge.balance = "10";

        click(host, gui, bridge, BUY_X, BUY_Y);
        int afterOpen = bridge.balanceRefreshes;
        paint(host, gui, bridge);

        assertTrue(bridge.balanceRefreshes > afterOpen, "余额不足时要催宿主刷余额");

        // 钱够了就别再催了，节流是宿主的事，但这条路本来也不该走。
        bridge.balance = "5000";
        int afterEnough = bridge.balanceRefreshes;
        paint(host, gui, bridge);
        assertEquals(afterEnough, bridge.balanceRefreshes, "余额够了不用再刷");
    }

    /**
     * 右键点在弹窗上。窗外那条（consumePressOutside）本来就不挑键，窗内要是只吸左键，
     * 右键就会穿到底下的面板去。
     */
    @Test
    void aRightClickInsideTheDialogIsAbsorbed() {
        HeadlessHost host = new HeadlessHost(W, H);
        SharedCosmetics gui = new SharedCosmetics();
        ShopBridge bridge = new ShopBridge();

        click(host, gui, bridge, BUY_X, BUY_Y);
        host.canvas.ops.clear();
        host.input.setMouse(250, 130);
        host.input.press(1, 250, 130);
        gui.draw(new UiFrame(host, Theme.DARK), bridge);

        assertFalse(anyPressLeft(host), "窗内的右键也得吸掉");
        assertTrue(gui.blocking(), "右键点窗内不算取消");
        host.input.endFrame();
    }

    /**
     * 同一帧里窗外窗内各有一次按下（鼠标事件成批喂进来）：窗外那下判成取消之后，窗内
     * 那下不能就这么留着漏给底下的面板。
     */
    @Test
    void aSameFrameOutsideAndInsidePressAreBothAbsorbed() {
        HeadlessHost host = new HeadlessHost(W, H);
        SharedCosmetics gui = new SharedCosmetics();
        ShopBridge bridge = new ShopBridge();

        click(host, gui, bridge, BUY_X, BUY_Y);
        host.canvas.ops.clear();
        host.input.setMouse(250, 130);
        host.input.press(0, FOLDER_X, FOLDER_Y);
        host.input.press(0, 250, 130);
        gui.draw(new UiFrame(host, Theme.DARK), bridge);

        assertFalse(anyPressLeft(host), "两下都得吸干");
        assertFalse(bridge.customFolderOpened);
        assertTrue(bridge.purchased.isEmpty(), "一次点窗外一次点窗内，看不出想干嘛，什么都别做");
        assertFalse(gui.blocking(), "点窗外仍然等于取消");
        host.input.endFrame();
    }

    /**
     * 余额串不是「数字加一个小数点」就当成未知。{@code 1e1} 交给 BigDecimal 会解析成 10
     * 把玩家拦下来，而这种写法我们根本不会显示；{@code 1e2147483647} 更是当场卡死渲染线程。
     */
    @Test
    void anOddlyFormattedBalanceCountsAsUnknown() {
        for (String odd : new String[] {"1e1", "+5", "-3", "1,000", "1.2.3", ""}) {
            HeadlessHost host = new HeadlessHost(W, H);
            SharedCosmetics gui = new SharedCosmetics();
            ShopBridge bridge = new ShopBridge();
            bridge.balance = odd;

            click(host, gui, bridge, BUY_X, BUY_Y);
            click(host, gui, bridge, CONFIRM_X, DIALOG_BTN_Y);

            assertEquals(1, bridge.purchased.size(), "余额串「" + odd + "」看不懂就该放行给后端判");
        }
    }

    /**
     * 弹窗开着的时候商品从目录里消失了（下架、目录刷新）。静默关窗的话，玩家明明点了
     * 「确认购买」，界面上既没有订单也没有一句提示，跟买成功了长得一模一样。
     */
    @Test
    void aDelistedItemStillSendsTheOrder() {
        HeadlessHost host = new HeadlessHost(W, H);
        SharedCosmetics gui = new SharedCosmetics();
        ShopBridge bridge = new ShopBridge();

        click(host, gui, bridge, BUY_X, BUY_Y);
        bridge.delisted = true;
        click(host, gui, bridge, CONFIRM_X, DIALOG_BTN_Y);

        assertEquals(1, bridge.purchased.size(), "点了确认就得发出去，成不成由后端说");
        assertFalse(gui.blocking(), "指向空气的弹窗要收掉");
    }

    /**
     * 同一帧「取消」和「确认购买」各中一下。窗外那条已经定了「看不出想干嘛就什么都不做」，
     * 窗内也得一样——按 if/else 链的写法，谁在链上排前面谁赢，而排前面的是花钱那个。
     */
    @Test
    void aSameFrameCancelAndConfirmBuysNothing() {
        HeadlessHost host = new HeadlessHost(W, H);
        SharedCosmetics gui = new SharedCosmetics();
        ShopBridge bridge = new ShopBridge();

        click(host, gui, bridge, BUY_X, BUY_Y);
        host.canvas.ops.clear();
        host.input.setMouse(CONFIRM_X, DIALOG_BTN_Y);
        host.input.press(0, CANCEL_X, DIALOG_BTN_Y);
        host.input.press(0, CONFIRM_X, DIALOG_BTN_Y);
        gui.draw(new UiFrame(host, Theme.DARK), bridge);

        assertTrue(bridge.purchased.isEmpty(), "两个按钮一起中，不能判成买");
        assertTrue(gui.blocking(), "窗留着，让玩家看清了再点");
        assertFalse(anyPressLeft(host), "两下都得吸干，不能漏给底下的面板");
        host.input.endFrame();

        // 看清之后单点「确认购买」照样能买，别把人锁死。
        click(host, gui, bridge, CONFIRM_X, DIALOG_BTN_Y);
        assertEquals(1, bridge.purchased.size());
    }

    /** 单点「取消」还是得关窗——上面那条不能顺手把正常的取消也判没了。 */
    @Test
    void cancelClosesTheDialogWithoutOrdering() {
        HeadlessHost host = new HeadlessHost(W, H);
        SharedCosmetics gui = new SharedCosmetics();
        ShopBridge bridge = new ShopBridge();

        click(host, gui, bridge, BUY_X, BUY_Y);
        click(host, gui, bridge, CANCEL_X, DIALOG_BTN_Y);

        assertTrue(bridge.purchased.isEmpty());
        assertFalse(gui.blocking(), "点取消要关窗");
    }

    /**
     * 弹窗开着的时候这件东西到手了（另一个端买的、拥有列表刚同步回来）。再发一单就是
     * 同一件东西付两次钱——和「下架」那条相反，这边我们确知不该发。
     */
    @Test
    void anItemOwnedWhileTheDialogIsOpenIsNotOrderedAgain() {
        HeadlessHost host = new HeadlessHost(W, H);
        SharedCosmetics gui = new SharedCosmetics();
        ShopBridge bridge = new ShopBridge();

        click(host, gui, bridge, BUY_X, BUY_Y);
        bridge.owned = true;
        click(host, gui, bridge, CONFIRM_X, DIALOG_BTN_Y);

        assertTrue(bridge.purchased.isEmpty(), "已经拥有了就不能再下一单");
        assertFalse(gui.blocking(), "窗还是要收掉");
    }

    /**
     * 视口小到弹窗盖住「购买」按钮的时候双击它。窗内那条循环先跑，不认得 opener 矩形的话
     * 第二下就落进弹窗、被判成「点了确认购买」——整个确认弹窗被绕过，钱直接扣掉。
     *
     * <p>258×132：面板和弹窗都是 238 宽，购买键 [150,240]×[97,115] 整个落在弹窗矩形里。
     */
    @Test
    void aDoubleClickOnBuySwallowedByTheDialogRectDoesNotOrder() {
        HeadlessHost host = new HeadlessHost(258, 132);
        SharedCosmetics gui = new SharedCosmetics();
        ShopBridge bridge = new ShopBridge();

        click(host, gui, bridge, 195, 106);
        assertTrue(gui.blocking(), "第一下开窗");
        click(host, gui, bridge, 195, 106);

        assertTrue(bridge.purchased.isEmpty(), "双击的第二下不能绕过确认弹窗直接下单");
        assertTrue(gui.blocking(), "也不能把自己开的窗关掉");
    }

    /**
     * 余额一帧只能问宿主一次。判定、表头胶囊、弹窗那一行原来各问一次，而 refreshBalance()
     * 正好夹在中间——宿主同步换掉缓存值的话，同一帧就会画出「余额 5000 / 价格 998」底下
     * 配一个「知道了」。
     */
    @Test
    void theBalanceIsSampledOncePerFrame() {
        HeadlessHost host = new HeadlessHost(W, H);
        SharedCosmetics gui = new SharedCosmetics();
        ShopBridge bridge = new ShopBridge();
        bridge.balance = "10";

        bridge.balanceReads = 0;
        paint(host, gui, bridge);
        assertEquals(1, bridge.balanceReads, "没有弹窗时也只问一次");

        click(host, gui, bridge, BUY_X, BUY_Y);
        bridge.balanceReads = 0;
        paint(host, gui, bridge);
        assertEquals(1, bridge.balanceReads, "余额不足弹窗开着的时候还是只问一次");
    }

    /**
     * 商品 id 是空串。拿空串当「没有弹窗」的哨兵的话，这件东西的「购买」按钮点下去永远
     * 没反应——不开窗、不下单、也没有一句提示。
     */
    @Test
    void anItemWithAnEmptyIdStillOpensTheDialog() {
        HeadlessHost host = new HeadlessHost(W, H);
        SharedCosmetics gui = new SharedCosmetics();
        ShopBridge bridge = new ShopBridge();
        bridge.id = "";

        click(host, gui, bridge, BUY_X, BUY_Y);
        assertTrue(gui.blocking(), "空 id 也得开窗");

        click(host, gui, bridge, CONFIRM_X, DIALOG_BTN_Y);
        assertEquals(1, bridge.purchased.size(), "确认之后照样下单，凭什么算数由后端说");
    }

    /** 右键点窗外：吸掉，但不算取消。MC 里右键太常用，用它关掉一个等确认的弹窗不合直觉。 */
    @Test
    void aRightClickOutsideTheDialogIsAbsorbedButDoesNotCancel() {
        HeadlessHost host = new HeadlessHost(W, H);
        SharedCosmetics gui = new SharedCosmetics();
        ShopBridge bridge = new ShopBridge();

        click(host, gui, bridge, BUY_X, BUY_Y);
        host.canvas.ops.clear();
        host.input.setMouse(FOLDER_X, FOLDER_Y);
        host.input.press(1, FOLDER_X, FOLDER_Y);
        gui.draw(new UiFrame(host, Theme.DARK), bridge);

        assertTrue(gui.blocking(), "右键点窗外不算取消");
        assertFalse(bridge.customFolderOpened, "但也不能漏给底下的按钮");
        assertFalse(anyPressLeft(host));
        host.input.endFrame();
    }

    /**
     * 翻面那一帧点「取消」。作废的只该是主按钮那个位置（上一帧写「知道了」这一帧写
     * 「确认购买」，说不清按的哪个）；「取消」两态语义一样，不能连它一起废掉。
     */
    @Test
    void cancelStillWorksOnAStateFlipFrame() {
        HeadlessHost host = new HeadlessHost(W, H);
        SharedCosmetics gui = new SharedCosmetics();
        ShopBridge bridge = new ShopBridge();
        bridge.balance = "10";

        click(host, gui, bridge, BUY_X, BUY_Y);
        bridge.balance = "5000";
        click(host, gui, bridge, CANCEL_X, DIALOG_BTN_Y);

        assertFalse(gui.blocking(), "翻面那一帧点取消照样关窗");
        assertTrue(bridge.purchased.isEmpty());
    }

    /**
     * {@code blocking()} 必须恒等于「这一帧画了弹窗」——两个宿主就是拿它决定这一帧画不画
     * 3D 预览（饰品缩略图、玩家模型）的。开窗那一帧不画就会空一帧，关窗那一帧还画就会被
     * 3D 预览糊在上面。
     */
    @Test
    void blockingMatchesWhetherTheDialogWasPaintedThisFrame() {
        HeadlessHost host = new HeadlessHost(W, H);
        SharedCosmetics gui = new SharedCosmetics();
        ShopBridge bridge = new ShopBridge();

        click(host, gui, bridge, BUY_X, BUY_Y);
        assertTrue(gui.blocking());
        assertTrue(host.canvas.has("drawString:Confirm"), "开窗那一帧就得画出来，不能空一帧");

        click(host, gui, bridge, CONFIRM_X, DIALOG_BTN_Y);
        assertFalse(gui.blocking());
        assertFalse(host.canvas.has("drawString:Confirm"), "关窗那一帧不能再画一张随后被糊住的卡片");
    }

    /**
     * 弹窗开着的时候宿主的在途标志翻成 true（上一单还没回来）。再发一单就是同一件东西
     * 付两次钱——面板那道闸这时候已经过去了，得在真花钱的那一步再查一次。
     */
    @Test
    void aPendingPurchaseBlocksTheConfirmButton() {
        HeadlessHost host = new HeadlessHost(W, H);
        SharedCosmetics gui = new SharedCosmetics();
        ShopBridge bridge = new ShopBridge();

        click(host, gui, bridge, BUY_X, BUY_Y);
        bridge.pending = true;
        click(host, gui, bridge, CONFIRM_X, DIALOG_BTN_Y);

        assertTrue(bridge.purchased.isEmpty(), "上一单还在路上就不能再发一单");
        assertFalse(gui.blocking(), "窗还是要收掉，别让人对着一个点不动的按钮干等");
    }

    /**
     * 「确认购买」画在哪，就得点在哪。裁决（confirmInput）和绘制（drawConfirm）共用
     * dialogButtonRect 是为了防「改一次宽度漏改一处」，可光有共用没有断言的话，下次谁把它
     * 拆回两份复制粘贴的坐标，测试照样全绿——按下打在哪里生效是测了，按钮画在哪里没测。
     */
    @Test
    void theConfirmButtonIsPaintedWhereTheClickIsAdjudicated() {
        HeadlessHost host = new HeadlessHost(W, H);
        SharedCosmetics gui = new SharedCosmetics();
        ShopBridge bridge = new ShopBridge();

        click(host, gui, bridge, BUY_X, BUY_Y);
        float[] label = host.canvas.textAt("BUY");
        assertTrue(label != null, "确认按钮的文字得画出来");

        // 容差原来是 ±38/±9，正好各是按钮的半宽半高——把绘制的 x 整体挪 30px、玩家看到的
        // 按钮和能点的地方差出大半个按钮，这个测试照样绿，等于没钉住任何东西。
        //
        // 现在钉实测偏移：按钮矩形 [280,356]×[188,206]，中心正是 (318,197)＝点击点；文字
        // 居中画在 (306,193)。换按钮文案会让 dx 变（文字宽度变了），那时候要连这两个数一起
        // 改——钉桩本来就该这样，不是误报。
        assertEquals(-12f, label[0] - CONFIRM_X, 3f,
                "文字横向离开了按钮中心：画在 " + label[0] + "，点的是 " + CONFIRM_X);
        assertEquals(-4f, label[1] - DIALOG_BTN_Y, 2f,
                "文字纵向离开了按钮中心：画在 " + label[1] + "，点的是 " + DIALOG_BTN_Y);
    }



    /**
     * 视口一小，弹窗矩形就把底下的「购买」按钮整个盖住。opener 跳过原本在弹窗的整个生命
     * 周期里都生效，于是确认按钮永远吸不到点击——取消却还能点（它有一段露在 opener 左边），
     * 只有「确认」这一个动作单向死掉，玩家看到的就是「点了没反应」。
     *
     * <p>网格实扫过：把宽限期的判据换回恒真，100..340 宽 × 80..140 高里有 46 组尺寸
     * 买不成，260×120 是其中一组（1280×720 配 GUI 缩放 5 就是 256×144）。
     */
    @Test
    void theConfirmButtonStaysClickableInASmallViewport() {
        HeadlessHost host = new HeadlessHost(260, 120);
        SharedCosmetics gui = new SharedCosmetics();
        ShopBridge bridge = new ShopBridge();

        click(host, gui, bridge, buyX(260), buyY(120));
        assertTrue(gui.blocking(), "小视口下也要能开窗");

        // 宽限期只是拿来盖住「玩家还没看见弹窗就发出的那下」的，过了就必须让确认按钮活过来。
        host.advanceMillis(400);
        paint(host, gui, bridge);

        click(host, gui, bridge, confirmX(260), confirmY(120));
        assertEquals(1, bridge.purchased.size(), "确认按钮被 opener 矩形吞了，点不动");
        assertFalse(gui.blocking(), "下单后弹窗该收掉");
    }

    /**
     * 弹窗宽高的两道 Math.max 下界：夹没了按钮行就排不下，「取消」会被挤到弹窗左外侧、
     * 点了不响应，弹窗关不掉。把 dialogW 的 Math.max 去掉，120×100 下这个用例会红，
     * 其余购买用例全绿——所以下界得有人钉着。
     */
    @Test
    void theDialogClampsWideEnoughToKeepCancelClickable() {
        HeadlessHost host = new HeadlessHost(120, 100);
        SharedCosmetics gui = new SharedCosmetics();
        ShopBridge bridge = new ShopBridge();

        click(host, gui, bridge, buyX(120), buyY(100));
        assertTrue(gui.blocking(), "退化视口下弹窗照样得开出来");

        // 这个尺寸下取消键也落在 opener 矩形里，先把宽限期走完（见上一个用例）。
        host.advanceMillis(400);
        paint(host, gui, bridge);

        click(host, gui, bridge, cancelXAt(120), confirmY(100));
        assertFalse(gui.blocking(), "取消被挤出弹窗，点了关不掉");
        assertTrue(bridge.purchased.isEmpty(), "取消不能反而下单");
    }

    /**
     * 弹窗开着的这段时间里这件东西到手了（另一个端买的、拥有列表刚同步回来）：留着一个
     * 写着「确认购买」的窗没有意义，底下那颗按钮也已经变成「装备」却被 opener 挡着点不动。
     */
    @Test
    void theDialogClosesItselfOnceTheItemIsAlreadyOwned() {
        HeadlessHost host = new HeadlessHost(W, H);
        SharedCosmetics gui = new SharedCosmetics();
        ShopBridge bridge = new ShopBridge();

        click(host, gui, bridge, BUY_X, BUY_Y);
        assertTrue(gui.blocking());

        bridge.owned = true;
        paint(host, gui, bridge);

        assertFalse(gui.blocking(), "已经拥有了还留着确认窗，玩家会以为要再付一次钱");
        assertTrue(bridge.purchased.isEmpty(), "收窗不能顺手下单");
    }

    /**
     * 上面那几个按尺寸算坐标的用例，靠的是复刻界面的排布算式。这里把它和 500×320 下手写
     * 死的那几个常量对一遍：算式跟着界面漂了的话，先红在这儿，而不是让小视口用例莫名其妙
     * 地绿着却什么都没点到。
     */
    @Test
    void theComputedButtonCentresMatchTheHardCodedOnes() {
        assertEquals(BUY_X, buyX(W));
        assertEquals(BUY_Y, buyY(H));
        assertEquals(CONFIRM_X, confirmX(W));
        assertEquals(DIALOG_BTN_Y, confirmY(H));
        // CANCEL_X 是取消键里随手挑的一个必中点（键宽 46，中心 251），不是中心，所以这里
        // 比的是「算出来的中心落在这个键里」。
        assertTrue(Math.abs(cancelXAt(W) - CANCEL_X) <= 23,
                "算出来的取消键中心跑出了 CANCEL_X 所在的那个键：" + cancelXAt(W));
    }

    // 界面排布的复刻：面板 min(660, gw-20) 居中，操作按钮贴面板右下角（x+w-98 起，宽 90
    // 高 18，底边留 25）；弹窗 clamp(154..238) × clamp(72..112) 居中，按钮行贴弹窗底边
    // （下边距 10、高 18），确认 76 宽靠右留 13，取消 46 宽紧挨在左边隔 6。
    /**
     * 真人双击「购买」，第二下不能变成下单。
     *
     * 宽限期原来按帧算（两帧＝60fps 下 33 毫秒），而一次双击两下之间是 150~250 毫秒：
     * 到第二下的时候宽限期早过完了。这个视口下弹窗矩形正好盖住底下的「购买」按钮，那一下
     * 就落进「确认购买」——玩家一帧弹窗都没看见，钱已经扣了。
     */
    @Test
    void aRealDoubleClickOnBuyDoesNotOrderThroughTheDialog() {
        HeadlessHost host = new HeadlessHost(260, 120);
        SharedCosmetics gui = new SharedCosmetics();
        ShopBridge bridge = new ShopBridge();

        click(host, gui, bridge, buyX(260), buyY(120));
        assertTrue(gui.blocking(), "第一下开窗");

        // 第二下：人手双击的间隔，不是同一帧。
        host.advanceMillis(180);
        click(host, gui, bridge, buyX(260), buyY(120));

        assertTrue(bridge.purchased.isEmpty(), "双击的第二下绕过弹窗下了单");
        assertTrue(gui.blocking(), "弹窗还得开着等人看清楚");
        assertFalse(anyPressLeft(host), "第二下要被吸掉，不能漏给底下的面板");
    }

    /**
     * 正常视口下双击「购买」，第二下落在弹窗外面：不能当成「点窗外＝取消」。
     *
     * 按帧算宽限期的时候，玩家双击一下看到的是弹窗闪一下就没了，而且看不出是自己关的。
     */
    @Test
    void aRealDoubleClickOnBuyDoesNotDismissTheDialogItJustOpened() {
        HeadlessHost host = new HeadlessHost(W, H);
        SharedCosmetics gui = new SharedCosmetics();
        ShopBridge bridge = new ShopBridge();

        click(host, gui, bridge, BUY_X, BUY_Y);
        assertTrue(gui.blocking(), "第一下开窗");

        host.advanceMillis(180);
        click(host, gui, bridge, BUY_X, BUY_Y);

        assertTrue(gui.blocking(), "双击的第二下把刚开出来的弹窗关掉了");
        assertTrue(bridge.purchased.isEmpty());
    }

    /** 宽限期过完之后，「购买」按钮那块矩形不能再挡着窗外的取消。 */
    @Test
    void theGraceStopsSwallowingOutsideClicksOnceItExpires() {
        HeadlessHost host = new HeadlessHost(W, H);
        SharedCosmetics gui = new SharedCosmetics();
        ShopBridge bridge = new ShopBridge();

        click(host, gui, bridge, BUY_X, BUY_Y);
        assertTrue(gui.blocking());

        host.advanceMillis(400);
        click(host, gui, bridge, BUY_X, BUY_Y);

        assertFalse(gui.blocking(), "宽限期过了，点窗外（含购买按钮那块）就该是取消");
        assertTrue(bridge.purchased.isEmpty(), "取消不能反而下单");
    }

    /** prism 自己那道余额兜底闸：一秒内不重复问宿主。 */
    @Test
    void theInsufficientDialogThrottlesItsOwnBalanceRefresh() {
        HeadlessHost host = new HeadlessHost(W, H);
        SharedCosmetics gui = new SharedCosmetics();
        ShopBridge bridge = new ShopBridge();
        bridge.balance = "10";
        host.setNowNanos(7_000_000_000L);

        click(host, gui, bridge, BUY_X, BUY_Y);
        assertTrue(gui.blocking(), "买不起也要开窗");
        // 开窗是在这一帧画面板的时候才发生的，判「买不起」要等下一帧。
        paint(host, gui, bridge);
        int afterOpen = bridge.balanceRefreshes;
        assertEquals(1, afterOpen, "弹窗第一次判成买不起时问一次");

        for (int i = 0; i < 30; i++) {
            host.advanceMillis(10);
            paint(host, gui, bridge);
        }
        assertEquals(afterOpen, bridge.balanceRefreshes, "300 毫秒里不该再问宿主");

        host.advanceMillis(1200);
        paint(host, gui, bridge);
        assertEquals(afterOpen + 1, bridge.balanceRefreshes, "过了一秒该再问一次");
    }

    private static float panelW(float gw) { return Math.min(660f, gw - 20f); }

    private static float panelH(float gh) { return Math.min(380f, gh - 20f); }

    private static int buyX(float gw) { return Math.round((gw - panelW(gw)) / 2f + panelW(gw) - 53f); }

    private static int buyY(float gh) { return Math.round((gh - panelH(gh)) / 2f + panelH(gh) - 16f); }

    private static float dialogW(float gw) { return Math.max(154f, Math.min(238f, gw - 20f)); }

    private static float dialogH(float gh) { return Math.max(72f, Math.min(112f, gh - 20f)); }

    private static int confirmX(float gw) { return Math.round((gw - dialogW(gw)) / 2f + dialogW(gw) - 51f); }

    private static int confirmY(float gh) { return Math.round((gh - dialogH(gh)) / 2f + dialogH(gh) - 19f); }

    private static int cancelXAt(float gw) { return Math.round((gw - dialogW(gw)) / 2f + dialogW(gw) - 118f); }

    /** 这一帧还剩没被消费的按下——立即模式里等于「漏给了底下的控件」。 */
    private static boolean anyPressLeft(HeadlessHost host) {
        // 零面积的矩形谁都不在里面，所以「窗外还有按下」＝「还有按下」。
        return host.input.hasPressOutside(0f, 0f, 0f, 0f);
    }

    private static void paint(HeadlessHost host, SharedCosmetics gui, ShopBridge bridge) {
        host.canvas.ops.clear();
        gui.draw(new UiFrame(host, Theme.DARK), bridge);
        host.input.endFrame();
    }

    private static void click(HeadlessHost host, SharedCosmetics gui, ShopBridge bridge,
                              int x, int y) {
        host.canvas.ops.clear();
        host.input.setMouse(x, y);
        host.input.press(0, x, y);
        gui.draw(new UiFrame(host, Theme.DARK), bridge);
        host.input.endFrame();
    }

    private static final class ShopBridge implements CosmeticsBridge {
        final List<String> purchased = new ArrayList<String>();
        boolean customFolderOpened;
        boolean delisted;
        boolean owned;
        boolean pending;
        int balanceRefreshes;
        int balanceReads;
        String id = "cape:1";
        String balance = "1000";

        @Override
        public String i18n(String key) {
            if ("cosmetics.coins".equals(key)) return "Coins";
            if ("cosmetics.insufficient.title".equals(key)) return "Not enough";
            if ("cosmetics.confirm.title".equals(key)) return "Confirm";
            // 短标签：按钮的文字是居中画的，长到溢出按钮的话「画在哪」就没法拿来钉几何了。
            if ("cosmetics.confirm.buy".equals(key)) return "BUY";
            return key;
        }

        @Override
        public String playerName() {
            return "Steve";
        }

        @Override
        public List<Item> items() {
            List<Item> items = new ArrayList<Item>();
            if (!delisted) {
                items.add(new Item(id, "Test Cape", "", "cape", "998", owned, false, false));
            }
            return items;
        }

        @Override
        public boolean signedIn() {
            return true;
        }

        @Override
        public String balance() {
            balanceReads++;
            return balance;
        }

        @Override
        public boolean purchasePending() {
            return pending;
        }

        @Override
        public void refreshBalance() {
            balanceRefreshes++;
        }

        @Override
        public void purchaseItem(String id) {
            purchased.add(id);
        }

        @Override
        public void openCustomFolder() {
            customFolderOpened = true;
        }

        @Override
        public boolean capeEnabled() {
            return true;
        }

        @Override
        public void setCapeEnabled(boolean enabled) {
        }

        @Override
        public float wingScale() {
            return 1f;
        }

        @Override
        public void setWingScale(float scale) {
        }

        @Override
        public void paintPlayerPreview(UiFrame ui, float x, float y, float w, float h, float yaw) {
        }
    }
}

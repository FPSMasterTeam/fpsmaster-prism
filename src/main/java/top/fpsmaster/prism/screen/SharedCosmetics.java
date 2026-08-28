package top.fpsmaster.prism.screen;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import top.fpsmaster.prism.canvas.FontHandle;
import top.fpsmaster.prism.geom.Hit;
import top.fpsmaster.prism.icon.GlyphIcons;
import top.fpsmaster.prism.input.PointerEvent;
import top.fpsmaster.prism.theme.Metrics;
import top.fpsmaster.prism.widget.Chrome;
import top.fpsmaster.prism.widget.Scroll;
import top.fpsmaster.prism.widget.UiFrame;

/** Compact in-game cosmetics shop. Minecraft entity rendering remains in the client bridge. */
public final class SharedCosmetics {
    private static final float MAX_W = 660f;
    private static final float MAX_H = 380f;
    private static final float HEADER_H = 29f;
    private static final float TOOLBAR_H = 33f;
    private static final float CARD_H = 104f;
    private static final float GAP = 7f;
    private static final float DEFAULT_MIN_SCALE = 0.5f;
    /** 弹窗按钮宽度：输入裁决和绘制分两趟跑，几何必须共用同一份常量。 */
    private static final float DIALOG_BUY_W = 76f;
    private static final float DIALOG_CANCEL_W = 46f;
    private static final float DIALOG_OK_W = 62f;
    /** prism 自己那道余额刷新闸的周期。宿主的闸照旧，这个只是兜底，所以放得比宿主宽。 */
    private static final long BALANCE_REFRESH_INTERVAL_NANOS = 1000L * 1000L * 1000L;
    /**
     * 开窗后「购买」按钮那块矩形还要挡多久。
     *
     * 按时间而不是按帧：原来是两帧，而两帧在 60fps 下只有 33 毫秒，真人双击两下之间是
     * 150~250 毫秒——宽限期早过了，第二下照样落在弹窗上。视口小到弹窗盖住了「购买」按钮
     * 的时候（panelW-dialogW≤170，两个按钮都靠右下角对齐），那一下正好落进「确认购买」，
     * 弹窗一帧都没看见钱就扣了；视口正常的时候它落在窗外，判成取消——玩家双击一下，看到的
     * 是弹窗自己闪一下就没了。300 毫秒盖得住一次双击，又短到不会真挡着人点确认。
     */
    private static final long OPENER_GRACE_NANOS = 300L * 1000L * 1000L;
    private static final int DIALOG_NONE = 0;
    private static final int DIALOG_BUY = 1;
    private static final int DIALOG_CLOSE = 2;
    private static final int BTN_OK = 0;
    private static final int BTN_BUY = 1;
    private static final int BTN_CANCEL = 2;
    private static final float DEFAULT_MAX_SCALE = 1.5f;

    private final Object scaleDrag = new Object();
    private final Object previewDrag = new Object();
    private final Scroll itemScroll = new Scroll(new Object());
    private float previewYaw = 180f;
    private float previewDragX;
    private float previewDragYaw;
    private boolean previewDragging;
    private boolean ownedOnly;
    private String category = "all";
    private String selectedId;

    /** 这一帧的余额，{@link #draw} 开头采一次。见那里的注释。 */
    private String frameBalance = "";

    /**
     * 购买确认弹窗盯着的商品 id。存 id 不存 Item：[CosmeticsBridge.items] 每帧重建，
     * 攥着上一帧的对象会在目录刷新之后指向一个已经不存在的东西。
     *
     * <p>开没开窗看 {@link #confirmOpen}，不看这个串是不是空的：空串是一个合法的
     * {@code Item.id()}，拿它当哨兵的话那件商品的「购买」按钮点下去永远没反应——不开窗、
     * 不下单、也没有一句提示。
     */
    private String confirmId = "";

    /** 弹窗开着没有。见 {@link #confirmId} 为什么不能用「串是不是空的」来判。 */
    private boolean confirmOpen;

    /**
     * 弹窗是「余额不足」那一态还是「确认购买」那一态。
     *
     * <p>每帧按当时的余额重算，不是开窗那一刻定死的：余额那一行本来就每帧重读，锁死状态
     * 会画出「余额 5000 / 价格 998」底下配一个「知道了」的自相矛盾窗口——玩家在网页上充完值
     * 切回来正好撞上。反过来（余额从未知变成不够）更糟：按钮还写着「确认购买」，点下去
     * 必然被后端打回。
     */
    private boolean confirmInsufficient;

    /**
     * 开这个弹窗的那个「购买」按钮的矩形，每帧跟着面板更新。
     *
     * <p>用来认出双击的第二下：两次按下差一百多毫秒，隔着好几帧，等第二下落地时弹窗已经
     * 开着了，而按钮在弹窗外面——不认它就会被当成「点窗外＝取消」，玩家双击一下等于什么
     * 都没发生。落在这个矩形里的按下照样吸掉（不能穿到底下再开一次窗），只是不取消。
     */
    private float openerX;
    private float openerY;
    private float openerW;
    private float openerH;

    /**
     * 还剩几帧要把落在 opener 矩形上的按下当成「双击的第二下」丢掉。
     *
     * <p>以前这个跳过在弹窗的整个生命周期里都生效，而弹窗尺寸是夹死的、购买按钮的位置
     * 跟着面板走：视口一小两个矩形就重叠，确认按钮整个被埋进 opener 里，**永远吸不到点击**
     * （实测死区 gw ≤ 352 且 gh ≤ 144，例如 258×132 下确认按钮只有 3px 露在外面；取消按钮
     * 有一段在 opener 左边所以还能点，于是只有「确认」这一个动作单向死掉）。
     *
     * <p>要挡的其实只是「玩家还没看见弹窗就已经发出的那下」——批量喂进来的鼠标事件、以及
     * 一次双击的第二下。等宽限期过了，玩家看得见自己按的是「确认购买」，那下就是他的本意。
     * 所以给一个很短的宽限期，而不是一直挡着。
     */
    private long openerArmedAtNanos;

    /** 宽限期有没有在计时。单独一个标志，因为宿主的时钟可以从 0 开始。 */
    private boolean openerArmed;

    /**
     * 这一帧宽限期还在不在。
     *
     * 一帧只算一次：窗内、窗外两条循环必须看到同一个值，否则一次按下可能被窗内认成
     * 「双击第二下」放过、又被窗外认成取消。
     */
    private boolean openerGraceActive;

    /** 上一次真的向宿主要过余额的时刻。 */
    private long lastBalanceRefreshNanos;

    /** 有没有要过。单独一个标志而不是拿 0 当哨兵：宿主的时钟可以从 0 开始。 */
    private boolean balanceEverRefreshed;

    /** 有没有弹窗挡着。宿主用它决定 ESC 是关弹窗还是关整个界面。 */
    public boolean blocking() {
        return confirmOpen;
    }

    public void closeDialog() {
        confirmOpen = false;
        confirmId = "";
        openerX = 0f;
        openerY = 0f;
        openerW = 0f;
        openerH = 0f;
        openerArmed = false;
        openerGraceActive = false;
    }

    public boolean draw(UiFrame ui, CosmeticsBridge bridge) {
        // 余额一帧只问宿主一次。原来判定、表头胶囊、弹窗那一行各问一次，而
        // refreshBalance() 正好夹在中间：宿主只要在刷新里同步换掉缓存值，同一帧就会画出
        // 「余额 5000 / 价格 998」底下配一个「知道了」，或者反过来按钮说能买、余额行说买不起。
        frameBalance = fetchBalance(bridge);
        float gw = ui.host().width();
        float gh = ui.host().height();
        float panelW = Math.min(MAX_W, gw - 20f);
        float panelH = Math.min(MAX_H, gh - 20f);
        float panelX = (gw - panelW) / 2f;
        float panelY = (gh - panelH) / 2f;

        // 弹窗是模态的，而立即模式没有「事件冒泡到此为止」这回事：底下的面板照样会在
        // 自己的 draw 里把点击吃掉（换分类、翻页、甚至再买一次）。所以在画面板之前就把
        // 这一帧落在弹窗之外的按下和滚轮吸干——底下的控件这一帧看不到任何输入。
        // 几何必须和真正画弹窗时用的一致，所以先算出来。
        // 目录得在裁决之前取：弹窗要按「这一帧的余额和价格」决定自己是哪一态，而那要先
        // 找到商品。取目录本身是纯读，提前不影响下面的绘制。
        List<CosmeticsBridge.Item> all = bridge.items();

        // 上下界都要：只夹上界的话，视口小到荒唐的时候（宿主还没拿到真正的窗口尺寸、
        // 或者窗口被拖成一条缝）算出来的是零甚至负的宽高，弹窗就成了一块谁都不在里面的
        // 矩形——窗内的按下一次都吸不到，模态当场失效，点哪儿都直接落到底下的面板上。
        // 宁可画出屏幕，也不能漏输入。
        // 下界不是随便挑的，是按钮行真正要的宽度：13 + 76(购买) + 6 + 46(取消) + 13 = 154。
        // 再窄下去「取消」的 x 会落到弹窗左外侧，点它走的是窗外那条循环、碰巧也判成关闭——
        // 语义对上纯属巧合，按钮宽度改一次就散架。
        float dialogW = Math.max(154f, Math.min(238f, gw - 20f));
        // 高度和宽度一样得跟着窗口收：竖着只有一百来像素的窗口（分屏、超宽比例）里，
        // 固定 112 会让按钮行掉到屏幕外面，弹窗就再也关不掉了。
        float dialogH = Math.max(72f, Math.min(112f, gh - 20f));
        float dialogX = (gw - dialogW) / 2f;
        float dialogY = (gh - dialogH) / 2f;
        boolean dialog = confirmOpen;
        int dialogAction = DIALOG_NONE;
        if (dialog) {
            // 一帧一次，两条输入循环共用。时钟回拨（nanoTime 理论上单调，虚拟化下见过倒退）
            // 按过期处理：宁可少挡一下，也不能让这块矩形永远挡着确认按钮。
            long graceAge = ui.host().nowNanos() - openerArmedAtNanos;
            openerGraceActive = openerArmed && graceAge >= 0L && graceAge < OPENER_GRACE_NANOS;
            if (!openerGraceActive) {
                openerArmed = false;
            }
            CosmeticsBridge.Item confirmItem = find(all, confirmId);
            boolean nowInsufficient = insufficient(bridge, confirmItem);
            // 状态在这一帧翻了面（刚充完值、或者余额刚拉回来）：按钮组会整个换掉，
            // 这一帧的按下按的是上一帧那套按钮，不能拿它当数。吸干、什么都不做。
            boolean flipped = nowInsufficient != confirmInsufficient;
            confirmInsufficient = nowInsufficient;
            if (nowInsufficient) {
                // 玩家很可能正切出去充值：充完切回来这个窗口自己就变回「确认购买」，
                // 不用退出界面再进。
                //
                // 这里自己压一道，不把节流全外包给宿主。refreshBalance() 是带空默认实现的
                // 接口方法，契约里没写「宿主必须节流」；现在两个宿主都传了 5 秒闸，但只要有
                // 谁手滑改成立刻刷，这就是 60~240 次请求每秒打后端——而且偏偏是在玩家买不起
                // 东西的时候，用户量最大的那条路径。宿主那道闸照旧，这道只是兜底。
                // 用宿主那个时钟，和这个界面里别的计时（宽限期、动画）读的是同一个——这样
                // 测试里换掉宿主时钟就能把这道闸整条走完，不用真等一秒。
                long now = ui.host().nowNanos();
                if (!balanceEverRefreshed
                        || now - lastBalanceRefreshNanos >= BALANCE_REFRESH_INTERVAL_NANOS
                        // 时钟回拨（nanoTime 理论上单调，但换核/虚拟化下见过倒退）按过期处理，
                        // 宁可多刷一次也不能永远不刷。
                        || now < lastBalanceRefreshNanos) {
                    lastBalanceRefreshNanos = now;
                    balanceEverRefreshed = true;
                    bridge.refreshBalance();
                }
            }

            // 窗内的按下得在这里裁决完。弹窗虽然画在最上面，可底下的面板是先绘制的，
            // 它会在自己的 draw 里把这次按下吃掉（预览台的拖拽转身就横跨整个按钮行），
            // 轮到弹窗绘制时按钮已经点不着了。所以这里先判按钮、再把窗内剩下的按下吸干。
            //
            // 无条件先跑，不管窗外那条判成什么：一帧里可以同时躺着窗内和窗外两次按下
            // （鼠标事件成批喂进来、或者两个键一起按），把它放进 else 分支的话，窗外那下
            // 一取消，窗内这下就没人消费，原样漏给底下的面板——弹窗刚被点掉，底下就跟着
            // 换了一次选中项甚至又开一次窗。
            dialogAction = confirmInput(ui, dialogX, dialogY, dialogW, dialogH, flipped);

            boolean pressedOutside = false;
            PointerEvent press;
            while ((press = ui.input().consumePressOutside(dialogX, dialogY, dialogW, dialogH)) != null) {
                // 落在「购买」按钮上的那下是双击的第二下，吸掉但不取消（见 openerArmedAtNanos）。
                if (openerGraceActive
                        && Hit.inside(openerX, openerY, openerW, openerH, press.x, press.y)) {
                    continue;
                }
                // 只有左键算取消。窗外的按下一律吸掉（不能穿到底下的面板去），但 MC 里右键
                // 是个常用键，用它关掉一个正等着确认的弹窗不合直觉——吸掉就够了。
                if (press.button == 0) {
                    pressedOutside = true;
                }
            }
            // 不按矩形吸：光标被宿主报在视口外的时候 consumeWheelDelta 会原样留着滚轮，
            // 底下的商品列表跟着滚。模态期间谁都别想拿到它。
            ui.input().discardWheel();
            if (pressedOutside) {
                // 点窗外＝取消。同一帧窗内那下即使按在「确认购买」上也一并作废：一次点在
                // 窗外一次点在窗里，看不出玩家到底想干嘛，扣钱这种事宁可什么都不做。
                dialogAction = DIALOG_CLOSE;
            }
            // 弹窗开着的这段时间里这件东西已经到手了（另一个端买的、拥有列表刚同步回来）。
            // 留着一个写着「确认购买」的窗没有意义，而且底下那颗按钮已经变成「装备」、却还被
            // opener 矩形挡着点不动——看起来能点其实是死的。当场收窗，让玩家看到真实状态。
            if (confirmItem != null && confirmItem.owned() && dialogAction == DIALOG_NONE) {
                dialogAction = DIALOG_CLOSE;
            }
            if (confirmItem == null || dialogAction != DIALOG_NONE) {
                // 当帧结算，不拖到 drawConfirm 里去。confirmItem == null 是这件商品在弹窗
                // 开着的时候从目录里没了（下架、切分类刷新），指向空气的窗留着只会让玩家
                // 点了「确认」什么都没发生。
                applyDialogAction(bridge, confirmItem, dialogAction);
                dialog = false;
            }
        }

        Chrome.veil(ui, 1f);
        Chrome.panel(ui, panelX, panelY, panelW, panelH);
        if (drawHeader(ui, bridge, panelX, panelY, panelW)) {
            // 退出界面顺手收窗。这个对象在两个宿主里都是随界面新建的，按说下次进来是干净的；
            // 但只要谁把它提成长驻字段，留着的弹窗就会在下次进店时钉在那儿，指着一件玩家早
            // 忘了、价格可能已经变过的商品，而第一下点击就落在「确认购买」上。
            closeDialog();
            return true;
        }

        ensureSelection(all, bridge);
        ensureVisibleSelection(all, bridge);

        float contentY = panelY + HEADER_H;
        float rightW = Math.max(178f, Math.min(260f, panelW * 0.39f));
        float rightX = panelX + panelW - rightW;
        Chrome.hairlineV(ui, rightX, contentY, panelH - HEADER_H);

        float catalogX = panelX + 9f;
        float catalogW = rightX - catalogX - 9f;
        drawCatalog(ui, bridge, all, catalogX, contentY, catalogW, panelH - HEADER_H - 9f);
        drawPreview(ui, bridge, all, selected(all), rightX, contentY, rightW, panelH - HEADER_H);
        // 判据是 confirmId 而不是帧头那个 dialog：购买按钮是在 drawPreview 中途才把它置上的
        // （见 drawAction），照帧头的快照画的话，开窗那一帧 blocking() 已经是 true、宿主已经
        // 停画 3D 预览，而弹窗要下一帧才出来——玩家看到的是预览区空掉一帧再冒出弹窗。
        // 反过来关窗那一帧也一样：结算已经提到面板之前做了，这里就不会再画一张随后被
        // 3D 预览糊住的卡片。这两条合起来才让 blocking() 恒等于「这一帧画了弹窗」。
        CosmeticsBridge.Item confirmed = confirmOpen ? find(all, confirmId) : null;
        // 这里不再吸一次滚轮：开窗那一帧 drawCatalog 里的 Scroll.begin 早就把它吃掉了
        // （弹窗是 drawPreview 中途才开的），放在面板之后已经晚了，加一行只是看着像修好了。
        // 要真挡住得在帧头预判「这一帧的按下会不会落在购买按钮上」，那要给 Input 契约加一个
        // 「窥视界内按下」的方法、两个宿主各实现一遍。代价对不上收益：漏掉的是遮罩后面的
        // 列表滚一格，购买按钮在预览区页脚、不在滚动列表里（openerX 不受影响），选中项也不
        // 由滚动驱动。记在这儿，别再有人以为这条路是通的。
        if (confirmed == null) {
            // 走到这儿只剩「confirmId 非空但目录里没有」这一种可能，上面那段已经收过一次，
            // 兜底是为了让 blocking() 和「画没画」永远一致——宿主的 3D 门控就照这个来。
            closeDialog();
        } else {
            if (!dialog) {
                // 这一帧刚开的窗，上面那段模态处理没赶上它，状态在这里补算。
                confirmInsufficient = insufficient(bridge, confirmed);
            }
            drawConfirm(ui, bridge, confirmed, dialogX, dialogY, dialogW, dialogH);
        }
        return false;
    }

    /**
     * 结算弹窗这一帧的动作：下单、关窗，或者两样都做。
     *
     * <p>在面板绘制之前跑，不放进 {@link #drawConfirm}。关窗会把 {@link #blocking()} 翻成
     * false，而两个宿主都是在 {@code draw()} 返回之后读它来决定这一帧画不画 3D 预览（饰品
     * 缩略图、玩家模型）。绘制中途关的话，卡片已经画出去了、门却开了，缩略图正好糊在价格
     * 和余额那两行上——就是宿主注释里说要避免的那个现象，只是只持续一帧。
     *
     * @param item 确认的那件商品；目录里已经找不到时为 null
     */
    private void applyDialogAction(CosmeticsBridge bridge, CosmeticsBridge.Item item, int action) {
        String id = confirmId;
        boolean wasInsufficient = confirmInsufficient;
        // 先关再发：purchaseItem 会把 purchasePending() 翻成 true，弹窗留着的话玩家能在
        // 等待期间再点一次「确认」，等于同一件东西下两单。
        closeDialog();
        if (action != DIALOG_BUY) {
            return;
        }
        // 面板那边开窗时查过一次，这里再查一次：查过之后玩家可能又开了一次窗（宿主的标志
        // 位晚几帧才翻面的话，面板那道闸拦不住），而这一步是真的要花钱了。
        if (bridge.purchasePending()) {
            return;
        }
        if (wasInsufficient) {
            // 余额不足那一态只有一个「知道了」，confirmInput 也只会返回 CLOSE。真走到这里
            // 说明状态和动作对不上了，宁可不发。
            return;
        }
        if (item == null) {
            // 下架了还是照发：玩家按下的时候弹窗上写着完整的商品名和价格，凭什么算数由后端
            // 说；这里静默吞掉的话，界面上既没有订单也没有一句提示，跟买成功了长得一模一样。
            bridge.purchaseItem(id);
            return;
        }
        // 弹窗开着的这段时间里这件东西已经到手了（另一个端买的、拥有列表刚同步回来）。
        // 和「下架」那条相反：那边是不知道还能不能买，交给后端判；这边我们确知再发一单
        // 就是同一件东西付两次钱。
        if (!item.owned()) {
            bridge.purchaseItem(id);
        }
    }

    /**
     * 弹窗按钮的矩形，写进 {@code out}（依次是 x、y、w、h）。
     *
     * <p>裁决（{@link #confirmInput}）和绘制（{@link #drawConfirm}）各抄一份坐标的话，
     * 改一次宽度漏改一处就成了「画在这儿、点在那儿」，而两边单看都对。共用这一份。
     */
    private static void dialogButtonRect(float[] out, int which,
                                         float x, float y, float w, float h) {
        float bw;
        float bx;
        if (which == BTN_OK) {
            bw = DIALOG_OK_W;
            bx = x + w - 13f - bw;
        } else if (which == BTN_BUY) {
            bw = DIALOG_BUY_W;
            bx = x + w - 13f - bw;
        } else {
            bw = DIALOG_CANCEL_W;
            bx = x + w - 13f - DIALOG_BUY_W - 6f - bw;
        }
        out[0] = bx;
        out[1] = y + h - 28f;
        out[2] = bw;
        out[3] = Metrics.BTN_H;
    }

    /**
     * 弹窗这一帧点到了哪个按钮，顺带把窗内其余的按下吸干。只读输入，不画任何东西——
     * 必须在面板绘制之前跑完，理由见 {@link #draw} 里的注释。
     *
     * @param stateFlipped 这一帧余额/价格的判定刚翻面，按钮组换了一套，本帧的按下作废
     */
    private int confirmInput(UiFrame ui, float x, float y, float w, float h,
                             boolean stateFlipped) {
        float[] rect = new float[4];
        int primary = 0;
        int cancel = 0;
        // -1＝任意键。写死左键的话右键点在弹窗上会穿到底下的面板去，而窗外那条
        // （consumePressOutside）本来就不挑键，两边不对称迟早出事。
        //
        // 先把窗内所有按下收完再裁决，不能写成 if/else if 链：那样链上第一个命中的赢，
        // 「同一帧既点了取消又点了确认购买」会被判成购买，剩下那下静默吞掉。窗外那条
        // 已经定了「看不出想干嘛就什么都不做」，窗内不能反着来——钱是一次性扣掉的。
        PointerEvent press;
        while ((press = ui.input().consumePressInBounds(x, y, w, h, -1)) != null) {
            // 双击「购买」的第二下：窗外那条循环认得它，可窗内这条先跑，弹窗矩形一旦盖住
            // 了那个按钮（视口小到 panelW-dialogW≤170 就会重叠，两个按钮都靠右下角对齐），
            // 第二下就落进来被判成「点了确认购买」——弹窗整个被绕过，钱直接扣掉。
            // 吸掉但什么都不算，和窗外那条一个意思。
            if (openerGraceActive
                    && Hit.inside(openerX, openerY, openerW, openerH, press.x, press.y)) {
                continue;
            }
            if (confirmInsufficient) {
                dialogButtonRect(rect, BTN_OK, x, y, w, h);
                if (Hit.inside(rect[0], rect[1], rect[2], rect[3], press.x, press.y)) {
                    primary++;
                }
                continue;
            }
            dialogButtonRect(rect, BTN_BUY, x, y, w, h);
            if (Hit.inside(rect[0], rect[1], rect[2], rect[3], press.x, press.y)) {
                primary++;
                continue;
            }
            dialogButtonRect(rect, BTN_CANCEL, x, y, w, h);
            if (Hit.inside(rect[0], rect[1], rect[2], rect[3], press.x, press.y)) {
                cancel++;
            }
            // 落在弹窗空白处：不做事，但也不能穿到底下去。
        }
        if (stateFlipped && primary > 0) {
            // 翻面作废的只有「主按钮」那一个位置：那儿上一帧写着「知道了」、这一帧写着
            // 「确认购买」（或者反过来），玩家按的是哪个说不清。「取消」两态语义一样，
            // 而且它的位置在另一态里根本没有按钮，不存在歧义，照常关窗。
            return DIALOG_NONE;
        }
        if (confirmInsufficient) {
            // 这一态只有一个「知道了」，点几下都是关窗，没有歧义可言。
            return primary > 0 ? DIALOG_CLOSE : DIALOG_NONE;
        }
        if (primary > 0 && cancel > 0) {
            // 两个按钮同一帧各中一下：意图自相矛盾，窗留着，让玩家看清了再点。
            return DIALOG_NONE;
        }
        if (primary > 0) {
            // 同一个按钮连中两下（成批喂进来的双击）不算歧义，就是想买。
            return DIALOG_BUY;
        }
        return cancel > 0 ? DIALOG_CLOSE : DIALOG_NONE;
    }

    /**
     * 购买确认 / 余额不足。两态共用一张卡片：只有标题、多出来的那行说明和按钮组不同。
     *
     * <p>没有「去充值」按钮——后端的钱包接口只开了查规则，客户端没有充值入口，画一个假的
     * 按钮不如直接告诉玩家去官网。
     */
    private void drawConfirm(UiFrame ui, CosmeticsBridge bridge, CosmeticsBridge.Item item,
                             float x, float y, float w, float h) {
        Chrome.veil(ui, 0.75f);
        Chrome.panel(ui, x, y, w, h);
        FontBold.draw(ui, 15, bridge.i18n(confirmInsufficient
                ? "cosmetics.insufficient.title" : "cosmetics.confirm.title"),
                x + 13f, y + 12f, ui.theme().textPrimary());

        FontHandle nameFont = ui.font(12);
        ui.canvas().drawString(nameFont, fit(nameFont, item.name(), w - 26f), x + 13f, y + 30f,
                ui.theme().textPrimary());

        FontHandle font = ui.font(11);
        drawConfirmRow(ui, font, bridge.i18n("cosmetics.confirm.price"), price(bridge, item),
                x + 13f, y + 46f, w - 26f, ui.theme().textSecondary());
        drawConfirmRow(ui, font, bridge.i18n("cosmetics.balance"), balanceText(bridge),
                x + 13f, y + 58f, w - 26f,
                confirmInsufficient ? ui.theme().danger() : ui.theme().textSecondary());
        if (confirmInsufficient) {
            ui.canvas().drawString(font, fit(font, bridge.i18n("cosmetics.insufficient.desc"), w - 26f),
                    x + 13f, y + 74f, ui.theme().textSecondary());
        }

        // 只画。点击在 confirmInput 里裁决、在 applyDialogAction 里结算，都已经跑完了
        // （按下也都被消费掉了，Chrome.button 的返回值恒 false，不会重复触发）。
        float[] rect = new float[4];
        if (confirmInsufficient) {
            dialogButtonRect(rect, BTN_OK, x, y, w, h);
            Chrome.button(ui, rect[0], rect[1], rect[2], rect[3],
                    bridge.i18n("cosmetics.insufficient.ok"), Chrome.ButtonStyle.DEFAULT);
            return;
        }
        dialogButtonRect(rect, BTN_BUY, x, y, w, h);
        Chrome.button(ui, rect[0], rect[1], rect[2], rect[3],
                bridge.i18n("cosmetics.confirm.buy"), Chrome.ButtonStyle.PRIMARY);
        dialogButtonRect(rect, BTN_CANCEL, x, y, w, h);
        Chrome.button(ui, rect[0], rect[1], rect[2], rect[3],
                bridge.i18n("configprofiles.cancel"), Chrome.ButtonStyle.GHOST);
    }

    /** 「价格   120 金币」：标签靠左，值靠右，中间随卡片宽度伸缩。 */
    private void drawConfirmRow(UiFrame ui, FontHandle font, String label, String value,
                                float x, float y, float w, int valueColor) {
        ui.canvas().drawString(font, label, x, y, ui.theme().textDisabled());
        String shown = fit(font, value, w - font.measure(label) - 8f);
        ui.canvas().drawString(font, shown, x + w - font.measure(shown), y, valueColor);
    }

    private String balanceText(CosmeticsBridge bridge) {
        String balance = balance(bridge);
        if (balance.isEmpty()) {
            return bridge.i18n("cosmetics.balance.unknown");
        }
        return balance + " " + bridge.i18n("cosmetics.coins");
    }

    /** 这一帧的余额。全部读取都走这里，见 {@link #draw} 开头。 */
    private String balance(CosmeticsBridge bridge) {
        return frameBalance;
    }

    private static String fetchBalance(CosmeticsBridge bridge) {
        String balance = bridge.balance();
        if (balance == null) {
            return "";
        }
        balance = balance.trim();
        // 显示路径上的 fit() 是逐字符 substring+measure 的 O(n²)，每帧跑两次（表头胶囊、
        // 弹窗那一行）。后端要是回一条几万字符的串，帧率会当场掉到个位数。金额本来就
        // 不该长过 amount() 认的 24 字符，超了直接判成「未知」，让后端去说。
        return balance.length() > 24 ? "" : balance;
    }

    /**
     * 余额够不够付这一件。
     *
     * <p>余额未知或两边有任何一个解析不出来时一律放行：客户端拿的是缓存，拦错了玩家就买不
     * 成东西且不知道为什么，而真买不起后端会用 400 挡下来——那是唯一有权威的判断。
     */
    private boolean insufficient(CosmeticsBridge bridge, CosmeticsBridge.Item item) {
        // 商品从目录里消失了（下架、切分类刷新）：没有价格可比，交给 drawConfirm 收摊。
        if (item == null || item.builtin()) {
            return false;
        }
        BigDecimal balance = amount(balance(bridge));
        BigDecimal price = amount(item.price());
        return balance != null && price != null && balance.compareTo(price) < 0;
    }

    /**
     * 把金额串解析成数，解析不出来返回 null（调用方一律按「不知道」处理，放行给后端判）。
     *
     * <p>手写循环而不是直接扔给 {@link BigDecimal}：后者认 {@code +5}、{@code 1e3} 这种
     * 我们不会显示成那样的写法，更要命的是 {@code 1e2147483647} 它也照收，随后的
     * {@code compareTo} 会当场把渲染线程卡死在内存分配上。金额只该是「数字加一个小数点」。
     */
    private static BigDecimal amount(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim();
        if (text.isEmpty() || text.length() > 24) {
            return null;
        }
        boolean dot = false;
        boolean digit = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '.') {
                if (dot) {
                    return null;
                }
                dot = true;
            } else if (c >= '0' && c <= '9') {
                digit = true;
            } else {
                return null;
            }
        }
        if (!digit) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException malformed) {
            return null;
        }
    }

    private boolean drawHeader(UiFrame ui, CosmeticsBridge bridge, float x, float y, float w) {
        if (back(ui, x + 8f, y + 7f)) return true;
        FontBold.draw(ui, 17, bridge.i18n("cosmetics.title"), x + 32f, y + 8f,
                ui.theme().textPrimary());

        float tabsX = x + 98f;
        drawScopeTab(ui, tabsX, y, 38f, bridge.i18n("cosmetics.store"), !ownedOnly);
        if (ui.clicked(tabsX, y, 38f, HEADER_H)) {
            ownedOnly = false;
            itemScroll.setOffset(0f);
        }
        drawScopeTab(ui, tabsX + 48f, y, 54f, bridge.i18n("cosmetics.owned"), ownedOnly);
        if (ui.clicked(tabsX + 48f, y, 54f, HEADER_H)) {
            ownedOnly = true;
            itemScroll.setOffset(0f);
        }

        String custom = bridge.i18n("cosmetics.custom.open");
        FontHandle font = ui.font(12);
        boolean compact = w < 350f;
        float buttonW = compact ? 20f : Math.min(116f, font.measure(custom) + 24f);
        float buttonX = x + w - buttonW - 8f;
        // 余额胶囊挂在自定义目录按钮左边。窄面板上标签会先被挤掉，这里就别再抢位置了。
        String balance = balance(bridge);
        if (!compact && !balance.isEmpty()) {
            FontHandle chipFont = ui.font(11);
            String text = fit(chipFont, balance + " " + bridge.i18n("cosmetics.coins"), 108f);
            float chipW = chipFont.measure(text) + 8f;
            float chipX = buttonX - chipW - 6f;
            // 左边就是「已拥有」标签。挤不下就不画：宁可不显示余额，也不能压在标签上——
            // 靠 compact 那道宽度阈值挡住只是巧合，余额位数一多照样会撞。
            if (chipX >= tabsX + 48f + 54f + 8f) {
                Chrome.badge(ui, chipX, y + 9.5f, text);
            }
        }
        boolean hover = ui.hovered(buttonX, y + 5f, buttonW, 19f);
        Chrome.button(ui, buttonX, y + 5f, buttonW, 19f, hover);
        GlyphIcons.draw(ui, "folder", buttonX + 6f, y + 11f, 7f,
                hover ? ui.theme().textPrimary() : ui.theme().textSecondary());
        if (!compact) {
            ui.canvas().drawString(font, fit(font, custom, buttonW - 22f), buttonX + 18f, y + 11f,
                    hover ? ui.theme().textPrimary() : ui.theme().textSecondary());
        }
        if (ui.clicked(buttonX, y + 5f, buttonW, 19f)) bridge.openCustomFolder();
        Chrome.hairlineH(ui, x, y + HEADER_H, w);
        return false;
    }

    private void drawScopeTab(UiFrame ui, float x, float y, float w, String label, boolean selected) {
        boolean hover = ui.hovered(x, y, w, HEADER_H);
        FontHandle font = ui.font(13);
        ui.canvas().drawString(font, fit(font, label, w), x, y + 10f,
                selected || hover ? ui.theme().textPrimary() : ui.theme().textSecondary());
        if (selected) {
            ui.canvas().fillRoundRect(x, y + HEADER_H - 1f, Math.min(w, font.measure(label)), 1.5f,
                    1f, ui.theme().accent());
        }
    }

    private void ensureSelection(List<CosmeticsBridge.Item> items, CosmeticsBridge bridge) {
        if (find(items, selectedId) != null) return;
        for (CosmeticsBridge.Item item : items) {
            if (item.equipped()) {
                select(item, bridge);
                return;
            }
        }
        if (!items.isEmpty()) select(items.get(0), bridge);
    }

    private void ensureVisibleSelection(List<CosmeticsBridge.Item> items, CosmeticsBridge bridge) {
        List<CosmeticsBridge.Item> visible = filtered(items);
        for (CosmeticsBridge.Item item : visible) {
            // 反着比：id 为 null 的商品目前两个宿主都发不出来（都是非空 String），但
            // find() 和 drawAction 都做了 null 防护，只有这里没做——防护不一致本身就是坑，
            // 顺手统一掉，成本是零。
            if (item.id() != null && item.id().equals(selectedId)) return;
        }
        if (!visible.isEmpty()) select(visible.get(0), bridge);
    }

    private void select(CosmeticsBridge.Item item, CosmeticsBridge bridge) {
        selectedId = item.id();
        bridge.previewItem(selectedId);
    }

    private void drawCatalog(UiFrame ui, CosmeticsBridge bridge, List<CosmeticsBridge.Item> all,
                             float x, float y, float w, float h) {
        float filtersW = 132f;
        float filtersX = x;
        ui.canvas().fillRoundRect(filtersX, y + 7f, filtersW, 20f, Metrics.CTL_RADIUS,
                ui.theme().layer());
        float filterX = filtersX + 2f;
        filterX = categoryTab(ui, bridge, all, filterX, y + 9f, "all", 38f);
        filterX = categoryTab(ui, bridge, all, filterX, y + 9f, "cape", 42f);
        categoryTab(ui, bridge, all, filterX, y + 9f, "back", 44f);

        List<CosmeticsBridge.Item> visible = filtered(all);
        float listY = y + TOOLBAR_H;
        float listH = h - TOOLBAR_H;
        int columns = 2;
        float cardW = (w - GAP * (columns - 1)) / columns;
        int rows = (visible.size() + columns - 1) / columns;
        float contentH = Math.max(listH, rows * (CARD_H + GAP) - GAP);
        float scroll = itemScroll.begin(ui, x, listY, w, listH, contentH);

        if (visible.isEmpty()) {
            String empty = bridge.i18n(ownedOnly ? "cosmetics.empty.owned" : "cosmetics.empty.store");
            FontHandle font = ui.font(12);
            ui.canvas().drawString(font, empty, x + (w - font.measure(empty)) / 2f,
                    listY + listH / 2f - font.lineHeight() / 2f, ui.theme().textDisabled());
        }
        for (int i = 0; i < visible.size(); i++) {
            int row = i / columns;
            int column = i % columns;
            float cardX = x + column * (cardW + GAP);
            float cardY = listY + scroll + row * (CARD_H + GAP);
            if (cardY + CARD_H < listY || cardY > listY + listH) continue;
            CosmeticsBridge.Item item = visible.get(i);
            drawItem(ui, bridge, item, cardX, cardY, cardW, CARD_H);
            if (ui.clicked(cardX, cardY, cardW, CARD_H)) select(item, bridge);
        }
        itemScroll.end(ui);
    }

    private float categoryTab(UiFrame ui, CosmeticsBridge bridge, List<CosmeticsBridge.Item> all,
                              float x, float y, String id, float w) {
        boolean selected = id.equals(category);
        boolean hover = ui.hovered(x, y, w, 16f);
        if (selected) ui.canvas().fillRoundRect(x, y, w, 16f, 4f, ui.theme().layerActive());
        FontHandle font = ui.font(12);
        String label = bridge.i18n("cosmetics.filter." + id);
        ui.canvas().drawString(font, fit(font, label, w - 6f),
                x + (w - Math.min(font.measure(label), w - 6f)) / 2f, y + 5f,
                selected || hover ? ui.theme().textPrimary() : ui.theme().textSecondary());
        if (ui.clicked(x, y, w, 16f)) {
            category = id;
            itemScroll.setOffset(0f);
            ensureVisibleSelection(all, bridge);
        }
        return x + w + 2f;
    }

    private void drawItem(UiFrame ui, CosmeticsBridge bridge, CosmeticsBridge.Item item,
                          float x, float y, float w, float h) {
        boolean selected = item.id().equals(selectedId);
        boolean hover = ui.hovered(x, y, w, h);
        if (selected) Chrome.selectedSurface(ui, x, y, w, h, Metrics.CARD_RADIUS);
        else Chrome.card(ui, x, y, w, h, hover, false);

        float footerH = 28f;
        float visualH = h - footerH;
        ui.canvas().fillRoundRect(x + 5f, y + 4f, w - 10f, visualH - 8f, 3f,
                ui.theme().layerActive());
        bridge.paintItemPreview(ui, item, x + 5f, y + 4f, w - 10f, visualH - 8f);
        Chrome.hairlineH(ui, x + 4f, y + visualH, w - 8f);

        if (item.equipped()) {
            ui.canvas().fillCircle(x + w - 10f, y + 9f, 5f, ui.theme().accent());
            GlyphIcons.draw(ui, "check", x + w - 12.5f, y + 6.5f, 5f, ui.theme().white());
        }

        String meta = !item.owned() || item.builtin() ? price(bridge, item) : "";
        FontHandle metaFont = ui.font(10);
        float metaW = Math.min(metaFont.measure(meta), w * 0.42f);
        FontHandle nameFont = ui.font(14);
        String name = fit(nameFont, item.name(), w - metaW - (meta.isEmpty() ? 16f : 19f));
        // 页脚是一条 footerH 高的独立区域，名字和价签要在它里面垂直居中。原来写死的
        // +7 / +8 是按「行盒顶」量的，名字的 7px 行盒落在 [+7, +14]，而页脚中心在 +14,
        // 整块字比中线高了 3.5px，卡片看上去就是字贴着分隔线。
        float footerTop = y + visualH;
        FontBold.draw(ui, 14, name, x + 8f, Chrome.textY(footerTop, footerH, nameFont),
                ui.theme().textPrimary());
        if (!meta.isEmpty()) {
            ui.canvas().drawString(metaFont, fit(metaFont, meta, w * 0.42f), x + w - metaW - 8f,
                    Chrome.textY(footerTop, footerH, metaFont),
                    item.builtin() ? ui.theme().ok() : ui.theme().textDisabled());
        }
    }

    private void drawPreview(UiFrame ui, CosmeticsBridge bridge, List<CosmeticsBridge.Item> all,
                             CosmeticsBridge.Item item, float x, float y, float w, float h) {
        float pad = 9f;
        float innerX = x + pad;
        float innerW = w - pad * 2f;
        String name = item == null ? bridge.i18n("cosmetics.none") : item.name();
        FontBold.draw(ui, 17, fit(ui.font(17), name, innerW - 22f), innerX, y + 7f,
                ui.theme().textPrimary());
        if (item != null) {
            String meta = bridge.i18n(categoryKey(item.category()));
            if (!item.owned()) meta += "  ·  " + price(bridge, item);
            ui.canvas().drawString(ui.font(12), fit(ui.font(12), meta, innerW - 22f), innerX, y + 21f,
                    ui.theme().textSecondary());
        }

        boolean resetHover = ui.hovered(x + w - 24f, y + 7f, 16f, 16f);
        Chrome.ghostButton(ui, x + w - 24f, y + 7f, 16f, 16f, resetHover);
        GlyphIcons.draw(ui, "reset", x + w - 19.5f, y + 11.5f, 7f,
                resetHover ? ui.theme().textPrimary() : ui.theme().textSecondary());
        if (ui.clicked(x + w - 24f, y + 7f, 16f, 16f)) previewYaw = 180f;

        boolean back = item != null && isBack(item.category());
        float footerH = 50f;
        float stageY = y + TOOLBAR_H;
        float stageH = h - TOOLBAR_H - footerH;
        ui.canvas().fillRect(x + 0.5f, stageY, w - 0.5f, stageH, ui.theme().layer());
        drawGrid(ui, x + 0.5f, stageY, w - 0.5f, stageH);
        rotatePreview(ui, x, stageY, w, stageH);
        bridge.paintPlayerPreview(ui, x, stageY, w, stageH, previewYaw);

        float footerY = stageY + stageH;
        Chrome.hairlineH(ui, x, footerY, w);
        if (back) drawBackSettings(ui, bridge, all, item, innerX, footerY + 7f, innerW);
        else drawCapeSettings(ui, bridge, innerX, footerY + 7f, innerW);

        String status = bridge.statusMessage();
        if (status == null || status.isEmpty()) status = syncStatusText(bridge);
        if (status != null && !status.isEmpty()) {
            ui.canvas().drawString(ui.font(10), fit(ui.font(10), status, innerW - 94f), innerX,
                    y + h - 17f, ui.theme().textDisabled());
        }
        drawAction(ui, bridge, item, x + w - 98f, y + h - 25f, 90f);
    }

    private void drawGrid(UiFrame ui, float x, float y, float w, float h) {
        float step = 14f;
        for (float lineX = x + step; lineX < x + w; lineX += step) {
            ui.canvas().fillRect(lineX, y, 0.5f, h, ui.theme().grid());
        }
        for (float lineY = y + step; lineY < y + h; lineY += step) {
            ui.canvas().fillRect(x, lineY, w, 0.5f, ui.theme().grid());
        }
        ui.canvas().fillRect(x + w * 0.14f, y + h * 0.9f, w * 0.72f, 0.5f, ui.theme().stroke());
    }

    private void rotatePreview(UiFrame ui, float x, float y, float w, float h) {
        boolean dragging = ui.input().beginDrag(previewDrag, 0, x, y, w, h)
                || ui.input().isDragging(previewDrag);
        if (dragging && ui.input().isButtonDown(0)) {
            if (!previewDragging) {
                previewDragX = ui.input().mouseX();
                previewDragYaw = previewYaw;
                previewDragging = true;
            }
            previewYaw = wrapDegrees(previewDragYaw + (ui.input().mouseX() - previewDragX) * 0.8f);
        } else {
            ui.input().releaseDrag(previewDrag);
            previewDragging = false;
        }
    }

    private void drawBackSettings(UiFrame ui, CosmeticsBridge bridge, List<CosmeticsBridge.Item> all,
                                  CosmeticsBridge.Item shown, float x, float y, float w) {
        FontHandle font = ui.font(12);
        CosmeticsBridge.Item policy = equippedBack(all);
        if (policy == null) policy = shown;
        float min = policy == null ? DEFAULT_MIN_SCALE : policy.minScale();
        float max = policy == null ? DEFAULT_MAX_SCALE : policy.maxScale();
        boolean adjustable = bridge.wingScaleAdjustable()
                && (policy == null || policy.allowResize())
                && max - min > 1e-4f;
        float scale = Math.max(min, Math.min(max, bridge.wingScale()));
        String scaleLabel = bridge.i18n("cosmetics.wings.scale");
        ui.canvas().drawString(font, scaleLabel, x, y + 4f,
                adjustable ? ui.theme().textSecondary() : ui.theme().textDisabled());
        String scaleValue = Math.round(scale * 100f) + "%";
        float valueW = font.measure(scaleValue);
        ui.canvas().drawString(font, scaleValue, x + w - valueW, y + 4f,
                adjustable ? ui.theme().textSecondary() : ui.theme().textDisabled());
        float sliderX = x + Math.min(74f, font.measure(scaleLabel) + 8f);
        float sliderW = Math.max(30f, w - (sliderX - x) - valueW - 7f);
        float t = track(scale, min, max);
        if (adjustable) {
            t = Chrome.slider(ui, scaleDrag, sliderX, y, sliderW, t);
            scale = min + t * (max - min);
        } else {
            Chrome.slider(ui, sliderX, y, sliderW, t, false);
        }
        if (Math.abs(scale - bridge.wingScale()) > 1e-4f) bridge.setWingScale(scale);
    }

    private CosmeticsBridge.Item equippedBack(List<CosmeticsBridge.Item> items) {
        for (CosmeticsBridge.Item item : items) {
            if (item.equipped() && isBack(item.category())) return item;
        }
        return null;
    }

    /** Maps a scale onto the 0..1 slider track, falling back to the default band when locked. */
    private float track(float scale, float min, float max) {
        if (max - min <= 1e-4f) {
            min = DEFAULT_MIN_SCALE;
            max = DEFAULT_MAX_SCALE;
        }
        return (scale - min) / (max - min);
    }

    /** Only reports non-ok loadout sync states; success is never announced. */
    private String syncStatusText(CosmeticsBridge bridge) {
        String status = bridge.syncStatus();
        if (status == null || status.isEmpty() || "ok".equals(status)) return "";
        return bridge.i18n("cosmetics.sync." + status);
    }

    private void drawCapeSettings(UiFrame ui, CosmeticsBridge bridge, float x, float y, float w) {
        FontHandle font = ui.font(12);
        String label = bridge.i18n("cosmetics.cape.animation");
        ui.canvas().drawString(font, label, x, y + 4f, ui.theme().textSecondary());
        boolean enabled = bridge.capeEnabled();
        boolean next = Chrome.toggle(ui, x + font.measure(label) + 7f, y, enabled);
        if (next != enabled) bridge.setCapeEnabled(next);
    }

    private void drawAction(UiFrame ui, CosmeticsBridge bridge, CosmeticsBridge.Item item,
                            float x, float y, float w) {
        if (item == null) return;
        // 弹窗开着的时候按钮还在底下画，位置也可能跟着滚动变：每帧记一遍，双击的第二下才
        // 认得出来（见 openerArmedAtNanos）。只记弹窗盯着的那件商品，免得换了选中项还拿旧矩形挡人。
        //
        // 记在三个 early-return 之前：这件东西在弹窗开着的时候变成 pending / owned / equipped
        // 的话，按钮会换一副样子但位置不变，从下面记就会把矩形停在上一次的值。
        if (item.id() != null && item.id().equals(confirmId)) {
            openerX = x;
            openerY = y;
            openerW = w;
            openerH = 18f;
        }
        // 「有单在飞」是进程级的一个标志，不分商品：放在最前面判的话，等某件东西的订单回来
        // 的这一两秒里，整个列表——包括已经买下的、正穿在身上的——全变成一颗写着「购买中」
        // 的死按钮，装备也点不动。已拥有的状态跟这单没关系，先画它们。
        if (item.equipped()) {
            Chrome.button(ui, x, y, w, 18f, bridge.i18n("cosmetics.equipped"),
                    Chrome.ButtonStyle.DEFAULT);
            return;
        }
        if (item.owned()) {
            if (Chrome.button(ui, x, y, w, 18f, bridge.i18n("cosmetics.equip"),
                    Chrome.ButtonStyle.PRIMARY)) bridge.equipItem(item.id());
            return;
        }
        if (bridge.purchasePending()) {
            Chrome.button(ui, x, y, w, 18f, bridge.i18n("cosmetics.purchasing"),
                    Chrome.ButtonStyle.DEFAULT);
            return;
        }
        boolean signedIn = bridge.signedIn();
        String label = signedIn
                ? bridge.i18n("cosmetics.buy") + "  ·  " + price(bridge, item)
                : bridge.i18n("cosmetics.login.required");
        // 未登录时这个按钮以前是死的（点了没反应），现在直接把人送去登录界面。
        if (Chrome.button(ui, x, y, w, 18f, label,
                signedIn ? Chrome.ButtonStyle.PRIMARY : Chrome.ButtonStyle.DEFAULT)) {
            if (signedIn && item.id() != null) {
                // 不再直接下单：钱是一次性扣掉的，误点一下就没了。
                //
                // id 为 null 的不开窗：confirmId 折成空串后 find() 用 equals 比较永远匹配不上，
                // 同一帧末尾就被兜底的 closeDialog() 收掉——弹窗一帧都没画出来，玩家看到的是
                // 「按钮点了没反应」。这种商品两个宿主都下不了单（都要把 id 解析成 long），
                // 与其弹一个假窗不如什么都不做。
                confirmOpen = true;
                confirmId = item.id();
                confirmInsufficient = insufficient(bridge, item);
                openerX = x;
                openerY = y;
                openerW = w;
                openerH = 18f;
                openerArmedAtNanos = ui.host().nowNanos();
                openerArmed = true;
            } else if (!signedIn) {
                bridge.openSignIn();
            }
        }
    }

    private List<CosmeticsBridge.Item> filtered(List<CosmeticsBridge.Item> items) {
        List<CosmeticsBridge.Item> result = new ArrayList<CosmeticsBridge.Item>();
        for (CosmeticsBridge.Item item : items) {
            if (ownedOnly && !item.owned()) continue;
            if ("cape".equals(category) && !"cape".equals(item.category())) continue;
            if ("back".equals(category) && !isBack(item.category())) continue;
            result.add(item);
        }
        return result;
    }

    private CosmeticsBridge.Item selected(List<CosmeticsBridge.Item> items) {
        return find(items, selectedId);
    }

    private CosmeticsBridge.Item find(List<CosmeticsBridge.Item> items, String id) {
        if (id == null) return null;
        for (CosmeticsBridge.Item item : items) if (id.equals(item.id())) return item;
        return null;
    }

    private boolean isBack(String itemCategory) {
        return "wings".equals(itemCategory) || "elytra".equals(itemCategory);
    }

    private String categoryKey(String itemCategory) {
        return "cape".equals(itemCategory) ? "cosmetics.cape" : "cosmetics.wings";
    }

    private String price(CosmeticsBridge bridge, CosmeticsBridge.Item item) {
        if (item.builtin() || "0".equals(item.price()) || "0.00".equals(item.price())) {
            return bridge.i18n("cosmetics.free");
        }
        return item.price() + " " + bridge.i18n("cosmetics.coins");
    }

    private String fit(FontHandle font, String value, float maxWidth) {
        if (value == null) return "";
        if (font.measure(value) <= maxWidth) return value;
        String ellipsis = "...";
        int end = value.length();
        while (end > 0 && font.measure(value.substring(0, end) + ellipsis) > maxWidth) end--;
        return end == 0 ? ellipsis : value.substring(0, end) + ellipsis;
    }

    private float wrapDegrees(float degrees) {
        float wrapped = degrees % 360f;
        return wrapped < 0f ? wrapped + 360f : wrapped;
    }

    private boolean back(UiFrame ui, float x, float y) {
        boolean hover = ui.hovered(x, y, 16f, 16f);
        Chrome.ghostButton(ui, x, y, 16f, 16f, hover);
        GlyphIcons.draw(ui, "back", x + 4.5f, y + 4.5f, 7f,
                hover ? ui.theme().textPrimary() : ui.theme().textSecondary());
        return ui.clicked(x, y, 16f, 16f);
    }
}

# Prism

> Minecraft-free immediate-mode GUI scaffold for the FPSMaster family.

Prism 是 FPSMaster 自研的轻量 immediate-mode UI 脚手架 —— 零 Minecraft / LWJGL / Fabric / Forge 依赖，以 `UiHost + Canvas` 抽象同时支撑 **跨版本** 与 **跨加载器**。

它被 `FPSMaster Edge (1.8.9)` 与 `FPSMaster Nova (1.19.2–1.21.11, Stonecutter)` 共用：同一套 `Shared*` 屏幕与 `Chrome` 组件，在两个客户端各自实现 `UiHost` 即可渲染。

```
[Shared Screens / Widgets]          Prism (this repo)
        │                           top.fpsmaster.prism:prism
        │  UiFrame + Theme/Metrics  ── 纯 Java 8，无 MC 依赖
        ▼
     UiHost / Canvas / Input         Edge 实现 ↔ Nova 实现
        │                           GL11 / GuiGraphics / PoseStack / submit-node
        ▼
   Minecraft / LWJGL / Platform
```

---

## 特性

* **Minecraft-free** — `group = top.fpsmaster`, `artifactId = prism`，只依赖 JDK 8，不触碰任何游戏类。可被任意渲染后端驱动，也可在单测里用 `Java2D` 直跑。
* **Immediate scaffold** — 无 retained 组件树。每帧 `new UiFrame(host, theme)` → 调 `Shared*` / `Chrome` 即画即测，命中测试由调用方通过 `Input.consumePressInBounds` 消费。
* **跨版本** — Prism 坐标系与渲染语义固定，版本差异收敛在 `UiHost/Canvas` 实现层。Nova 侧以 Stonecutter 单源码树覆盖 `1.19.2 / 1.20.1 / 1.21.1 / 1.21.8 / 1.21.11`，Edge 侧以 `ScaledGuiScreen` 适配 `1.8.9`。
* **跨加载器** — 不绑定 Fabric / Forge / NeoForge。宿主只要实现 `UiHost`（画布、输入、字体、可选的毛玻璃与 WebView 占位）即可接入。
* **可主题化** — `Theme` / `Metrics` / `Argb` 集中管理色板、圆角、间距；`Theme.glass()` 在宿主未启用 blur 时自动回落到实色。
* **工程化** — Java 8 bytecode（`release = 8`），UTF-8 编译，`withSourcesJar()`，JitPack 发布，与 `Cadence (music-api)` 同一套分发模型。

## 包结构

```
top.fpsmaster.prism.canvas   Canvas, FontHandle, ImageHandle    // 渲染后端抽象
top.fpsmaster.prism.host     UiHost                             // 宿主胶水层
top.fpsmaster.prism.input    Input, PointerEvent, FrameInput, Keys
top.fpsmaster.prism.theme    Theme, Metrics, Argb
top.fpsmaster.prism.widget   UiFrame, Chrome, TextBox, Scroll    // 原子组件
top.fpsmaster.prism.screen   SharedMainMenu, SharedClickGui,     // 跨端共享屏幕
                             SharedBackgrounds, SharedMusic,
                             SharedConfigProfiles, … + *Bridge
top.fpsmaster.prism.geom     Hit
top.fpsmaster.prism.anim     Anim
top.fpsmaster.prism.icon     GlyphIcons
```

`widget` 只负责绘制，不持有状态；`screen/Shared*` 承载布局与交互编排；`*Bridge` 由宿主注入导航与业务数据。

## 坐标系

> **1 toolkit unit = 1 Edge GUI unit = prototype CSS px / 2**

原型见 `FPSMaster-Edge/docs/prototypes/edge-ui.css`。所有 `Canvas` 坐标、`*Bridge` 布局、点击命中均使用该单位，宿主负责在 `UiHost.width()/height()` 与实际帧缓冲之间做缩放。

## 快速开始

### 1. 引入

```kotlin
// JitPack
repositories { maven("https://jitpack.io") }

dependencies {
    implementation("com.github.FPSMasterTeam:fpsmaster-prism:0.1.0")
    // 或本地 Maven：top.fpsmaster:prism:0.1.0
}
```

```gradle
// Gradle (Edge/Nova 已内置)
implementation("top.fpsmaster:prism:0.1.0")
```

### 2. 实现 UiHost

```java
class MyHost implements UiHost {
    private final MyCanvas canvas = new MyCanvas();
    private final FrameInput input = new FrameInput();
    public Canvas canvas() { return canvas; }
    public Input input() { return input; }
    public FontHandle font(int size) { return cache.get(size); }
    public float width() { return scaledWidth; }
    public float height() { return scaledHeight; }
    public boolean blurEnabled() { return true; }
    public void blurBehind(float x, float y, float w, float h, float r) { /* platform blur */ }
    public ImageHandle image(String id) { return atlas.get(id); }
}
```

### 3. 每帧驱动

```java
void render(float mouseX, float mouseY) {
    host.input().beginFrame(mouseX, mouseY);
    UiFrame ui = new UiFrame(host, Theme.DARK);
    SharedMainMenu.render(ui, bridge); // 或直接用 Chrome.button / Chrome.card
    if (ui.clicked(x, y, w, h)) { /* ... */ }
}
```

参考实现：`FPSMaster-Edge/src/main/java/top/fpsmaster/ui/kit/EdgeHost.java` · `FPSMaster-Nova/src/main/kotlin/top/fpsmaster/ui/kit/NovaHost.kt`；单测对照 `fpsmaster-prism/src/test/java/top/fpsmaster/prism/test/HeadlessHost.java`。

## 共享屏幕

| 屏幕 | 共享实现 | 宿主 Bridge |
|---|---|---|
| 主菜单 | `SharedMainMenu` | `MenuBridge` |
| ClickGUI | `SharedClickGui` | `ClickGuiBridge` |
| 背景/封面 | `SharedBackgrounds` | `BackgroundsBridge` |
| 音乐 | `SharedMusic` | `MusicBridge` |
| 配置档案 | `SharedConfigProfiles` | `ConfigProfilesBridge` |
| 账号 | `SharedAccountOverlay` | — |

新增共享屏幕：Prism 内新增 `SharedXxx + XxxBridge`，两端各自实现 `XxxBridge` 并用同一个 `UiFrame` 渲染。

## 构建与发布

```bash
./gradlew publishToMavenLocal -x test   # 本地验证
./gradlew test                          # 单测（含 Java2D 黄金对照）
```

* `java { sourceCompatibility = 1.8 }`, `options.release = 8`
* JitPack：`top.fpsmaster:prism:<tag>`，`jitpack.yml` 已配置 `openjdk21` 构建

## 从 fpsmaster-ui 迁移

`fpsmaster-ui` 已重命名为 **Prism**：

| 旧 | 新 |
|---|---|
| `FPSMasterTeam/fpsmaster-ui` | `FPSMasterTeam/fpsmaster-prism` |
| `top.fpsmaster:ui:0.1.0` | `top.fpsmaster:prism:0.1.0` |
| `top.fpsmaster.uikit.*` | `top.fpsmaster.prism.*` |
| `rootProject.name = "fpsmaster-ui"` | `rootProject.name = "fpsmaster-prism"` |

批量替换：`s/top.fpsmaster.uikit/top.fpsmaster.prism/g` + `s/top.fpsmaster:ui/top.fpsmaster:prism/g`。

## 许可

MIT © FPSMaster Team — 见 [LICENSE](LICENSE)。

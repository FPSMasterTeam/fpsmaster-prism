# fpsmaster-ui

Minecraft-free immediate-mode UI toolkit for FPSMaster Edge and Nova.

**Java 8 bytecode**, zero Minecraft / LWJGL / Fabric / Forge dependencies. Each client
implements `UiHost` (canvas, input, fonts, optional blur and WebView). Widgets and
theme live here.

Sizing: **1 toolkit unit = Edge GUI unit = prototype CSS px / 2**
(`FPSMaster-Edge/docs/prototypes/edge-ui.css`).

```
top.fpsmaster.uikit.canvas   Canvas, FontHandle, ImageHandle
top.fpsmaster.uikit.host     UiHost
top.fpsmaster.uikit.input    Input, PointerEvent, FrameInput
top.fpsmaster.uikit.theme    Theme, Metrics
top.fpsmaster.uikit.widget   UiFrame, Chrome
```

Maven: `top.fpsmaster:ui` (JitPack, same pattern as Cadence / `music-api`).

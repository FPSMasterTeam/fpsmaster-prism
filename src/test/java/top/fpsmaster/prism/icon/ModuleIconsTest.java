package top.fpsmaster.prism.icon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModuleIconsTest {
    @Test
    void normalizesNovaAndEdgeModuleIdentities() {
        assertEquals("modules/fps-display", ModuleIcons.resource("fps-display"));
        assertEquals("modules/fps-display", ModuleIcons.resource("FPSDisplay"));
        assertEquals("modules/hit-boxes", ModuleIcons.resource("HitBoxes"));
        assertEquals("modules/performance", ModuleIcons.resource("Performance"));
        assertEquals("modules/nametags", ModuleIcons.resource("Nametags"));
        assertEquals("modules/custom-fov", ModuleIcons.resource("CustomFov"));
    }
}

package gg.maeve.mod

import gg.maeve.mod.config.Config
import gg.maeve.mod.module.ModuleManager
import gg.maeve.mod.module.hud.CoordsModule
import gg.maeve.mod.module.hud.FpsModule
import gg.maeve.mod.module.hud.KeystrokesModule
import gg.maeve.mod.render.HudRenderController
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HudAndConfigTest {

    private fun manager() = ModuleManager(Config(Files.createTempDirectory("maeve-test"))).apply {
        register(FpsModule()); register(CoordsModule()); register(KeystrokesModule())
    }

    @Test
    fun `enabled hud modules draw, disabled ones do not`() {
        val canvas = FakeHudCanvas()
        HudRenderController(manager()).draw(canvas, gameCtx())
        val texts = canvas.draws.map { it.text }
        assertTrue(texts.any { it == "60 FPS" }, "FPS should render: $texts")
        assertTrue(texts.any { it.startsWith("XYZ:") }, "Coords should render: $texts")
        assertTrue(texts.none { it.contains("[W]") }, "Keystrokes disabled -> nothing: $texts")
    }

    @Test
    fun `coords hidden when not in world`() {
        val canvas = FakeHudCanvas()
        HudRenderController(manager()).draw(canvas, gameCtx(inWorld = false))
        assertTrue(canvas.draws.none { it.text.startsWith("XYZ:") })
    }

    @Test
    fun `module toggle persists across reload`() {
        val dir = Files.createTempDirectory("maeve-persist")
        val m1 = ModuleManager(Config(dir).apply { load() }).apply { register(FpsModule()) }
        m1.toggle("fps")
        val reloaded = FpsModule()
        ModuleManager(Config(dir).apply { load() }).register(reloaded)
        assertEquals(false, reloaded.enabled, "disabled state should persist across reload")
    }
}

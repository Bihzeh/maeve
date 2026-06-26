package gg.maeve.mod.config

import gg.maeve.mod.module.hud.CpsModule
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfigOptionsTest {
    @Test fun `module options round-trip through config`() {
        val dir = Files.createTempDirectory("cfg")
        val cps = CpsModule().also { it.setOption("right", false) }
        Config(dir).apply { snapshot(listOf(cps)); save() }
        val restored = CpsModule()
        assertTrue(restored.option("right"), "default before load")
        Config(dir).apply { load(); applyTo(restored) }
        assertFalse(restored.option("right"), "persisted option restored")
    }
}

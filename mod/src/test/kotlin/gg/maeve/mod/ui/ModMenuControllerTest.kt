package gg.maeve.mod.ui

import gg.maeve.mod.config.Config
import gg.maeve.mod.module.FontModule
import gg.maeve.mod.module.ModuleManager
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class ModMenuControllerTest {
    @Test fun `onToggle fires the after-toggle callback with the new state`() {
        val mgr = ModuleManager(Config(Files.createTempDirectory("c"))).apply { register(FontModule()) }
        val seen = mutableListOf<Pair<String, Boolean>>()
        val controller = ModMenuController(mgr) { id, enabled -> seen.add(id to enabled) }
        controller.onToggle("font") // FontModule defaults enabled=true -> becomes false
        assertEquals(listOf("font" to false), seen)
    }
}

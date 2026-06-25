package gg.maeve.mod.editor

import gg.maeve.mod.config.Config
import gg.maeve.mod.module.HudAnchor
import gg.maeve.mod.module.ModuleManager
import gg.maeve.mod.module.hud.FpsModule
import gg.maeve.mod.platform.GameContext
import gg.maeve.shared.MaevePalette
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private object Measure : TextMeasurer {
    override fun width(text: String) = text.length * 6
    override val lineHeight = 10
}

private fun ctx() = GameContext(60, true, 1.0, 64.0, -2.0, false, false, false, false)

class EditorStateTest {
    private fun setup(): Pair<ModuleManager, List<ElementBox>> {
        val mgr = ModuleManager(Config(Files.createTempDirectory("editor"))).apply { register(FpsModule()) }
        val boxes = ElementLayout.boxesFor(mgr.hudModules(), ctx(), Measure, 800, 600)
        return mgr to boxes
    }

    private fun control(id: String) =
        PanelLayout.controls(800 - PanelLayout.WIDTH, PanelLayout.TOP).first { it.id == id }.rect

    @Test fun `press on element selects it`() {
        val (mgr, boxes) = setup()
        val s = EditorState()
        val b = boxes.first { it.id == "fps" }.rect
        assertTrue(s.onPress(b.left + b.width / 2, b.top + b.height / 2, 800, 600, boxes, mgr))
        assertEquals("fps", s.selectedId)
    }

    @Test fun `press on empty space deselects`() {
        val (mgr, boxes) = setup()
        val s = EditorState()
        assertFalse(s.onPress(400, 300, 800, 600, boxes, mgr))
        assertNull(s.selectedId)
    }

    @Test fun `drag re-anchors the element so it stays put`() {
        val (mgr, boxes) = setup()
        val s = EditorState()
        val b = boxes.first { it.id == "fps" }.rect
        s.onPress(b.left + b.width / 2, b.top + b.height / 2, 800, 600, boxes, mgr)
        s.onDrag(b.left + b.width / 2 + 700, b.top + b.height / 2 + 500, 800, 600, boxes, mgr)
        s.onRelease()
        assertEquals(HudAnchor.BOTTOM_RIGHT, mgr.hudById("fps")!!.anchor)
        assertTrue(s.dirty)
    }

    @Test fun `panel toggle flips bold on the selected element`() {
        val (mgr, boxes) = setup()
        val s = EditorState()
        val b = boxes.first { it.id == "fps" }.rect
        s.onPress(b.left + b.width / 2, b.top + b.height / 2, 800, 600, boxes, mgr)
        val bold = control("bold")
        s.onPress(bold.left + bold.width / 2, bold.top + bold.height / 2, 800, 600, boxes, mgr)
        assertTrue(mgr.hudById("fps")!!.style.bold)
    }

    @Test fun `swatch recolors the selected element keeping its alpha`() {
        val (mgr, boxes) = setup()
        val s = EditorState()
        val b = boxes.first { it.id == "fps" }.rect
        s.onPress(b.left + b.width / 2, b.top + b.height / 2, 800, 600, boxes, mgr)
        val swatch = control("swatch:2") // index 2 = primary
        s.onPress(swatch.left + swatch.width / 2, swatch.top + swatch.height / 2, 800, 600, boxes, mgr)
        assertEquals(MaeveColor.rgbOf(MaevePalette.primary), MaeveColor.rgbOf(mgr.hudById("fps")!!.style.color))
    }

    @Test fun `scale buttons clamp to range`() {
        val (mgr, boxes) = setup()
        val s = EditorState()
        val b = boxes.first { it.id == "fps" }.rect
        s.onPress(b.left + b.width / 2, b.top + b.height / 2, 800, 600, boxes, mgr)
        repeat(20) { val c = control("scale+"); s.onPress(c.left + 1, c.top + 1, 800, 600, boxes, mgr) }
        assertEquals(3.0f, mgr.hudById("fps")!!.style.scale)
    }
}

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
    private fun setup(screenW: Int = 800, screenH: Int = 600): Triple<ModuleManager, List<ElementBox>, EditorState> {
        val mgr = ModuleManager(Config(Files.createTempDirectory("editor"))).apply { register(FpsModule()) }
        val boxes = ElementLayout.boxesFor(mgr.hudModules(), ctx(), Measure, screenW, screenH)
        return Triple(mgr, boxes, EditorState())
    }

    private fun control(id: String) =
        PanelLayout.controls(800 - PanelLayout.WIDTH, PanelLayout.TOP).first { it.id == id }.rect

    private fun selectFps(s: EditorState, boxes: List<ElementBox>, mgr: ModuleManager) {
        val b = boxes.first { it.id == "fps" }.rect
        s.onPress(b.left + b.width / 2, b.top + b.height / 2, 800, 600, boxes, mgr)
    }

    @Test fun `press on element selects it`() {
        val (mgr, boxes, s) = setup()
        selectFps(s, boxes, mgr)
        assertEquals("fps", s.selectedId)
    }

    @Test fun `press on empty space deselects`() {
        val (mgr, boxes, s) = setup()
        assertFalse(s.onPress(400, 300, 800, 600, boxes, mgr))
        assertNull(s.selectedId)
    }

    @Test fun `drag re-anchors the element so it stays put`() {
        val (mgr, boxes, s) = setup()
        val b = boxes.first { it.id == "fps" }.rect
        s.onPress(b.left + b.width / 2, b.top + b.height / 2, 800, 600, boxes, mgr)
        s.onDrag(b.left + b.width / 2 + 700, b.top + b.height / 2 + 500, 800, 600, mgr)
        s.onRelease()
        assertEquals(HudAnchor.BOTTOM_RIGHT, mgr.hudById("fps")!!.anchor)
        assertTrue(s.dirty)
    }

    @Test fun `drag does not crash when the element is larger than the screen`() {
        val (mgr, boxes, s) = setup(screenW = 20, screenH = 600) // box width 40 > 20
        val b = boxes.first { it.id == "fps" }.rect
        s.onPress(b.left + 1, b.top + 1, 20, 600, boxes, mgr)
        s.onDrag(b.left + 5, b.top + 5, 20, 600, mgr) // would throw on inverted coerceIn before the fix
        s.onRelease()
        assertTrue(s.dirty)
    }

    @Test fun `prune deselects when the selected element has no box`() {
        val (mgr, boxes, s) = setup()
        selectFps(s, boxes, mgr)
        assertEquals("fps", s.selectedId)
        s.pruneSelection(emptyList())
        assertNull(s.selectedId)
    }

    @Test fun `panel toggle flips bold on the selected element`() {
        val (mgr, boxes, s) = setup()
        selectFps(s, boxes, mgr)
        val bold = control("bold")
        s.onPress(bold.left + bold.width / 2, bold.top + bold.height / 2, 800, 600, boxes, mgr)
        assertTrue(mgr.hudById("fps")!!.style.bold)
    }

    @Test fun `swatch recolors the selected element keeping its alpha`() {
        val (mgr, boxes, s) = setup()
        selectFps(s, boxes, mgr)
        val swatch = control("swatch:2") // index 2 = primary
        s.onPress(swatch.left + swatch.width / 2, swatch.top + swatch.height / 2, 800, 600, boxes, mgr)
        assertEquals(MaeveColor.rgbOf(MaevePalette.primary), MaeveColor.rgbOf(mgr.hudById("fps")!!.style.color))
    }

    @Test fun `scale buttons clamp to range`() {
        val (mgr, boxes, s) = setup()
        selectFps(s, boxes, mgr)
        repeat(20) { val c = control("scale+"); s.onPress(c.left + 1, c.top + 1, 800, 600, boxes, mgr) }
        assertEquals(3.0f, mgr.hudById("fps")!!.style.scale)
    }
}

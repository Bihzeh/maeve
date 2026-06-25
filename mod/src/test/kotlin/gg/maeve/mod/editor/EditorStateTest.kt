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

    private fun fpsColor(mgr: ModuleManager) = mgr.hudById("fps")!!.style.color

    @Test fun `press on element selects it`() {
        val (mgr, boxes, s) = setup(); selectFps(s, boxes, mgr); assertEquals("fps", s.selectedId)
    }

    @Test fun `press on empty space deselects`() {
        val (mgr, boxes, s) = setup()
        assertFalse(s.onPress(400, 300, 800, 600, boxes, mgr)); assertNull(s.selectedId)
    }

    @Test fun `drag re-anchors the element so it stays put`() {
        val (mgr, boxes, s) = setup()
        val b = boxes.first { it.id == "fps" }.rect
        s.onPress(b.left + b.width / 2, b.top + b.height / 2, 800, 600, boxes, mgr)
        s.onDrag(b.left + b.width / 2 + 700, b.top + b.height / 2 + 500, 800, 600, mgr)
        s.onRelease()
        assertEquals(HudAnchor.BOTTOM_RIGHT, mgr.hudById("fps")!!.anchor); assertTrue(s.dirty)
    }

    @Test fun `drag does not crash when the element is larger than the screen`() {
        val (mgr, boxes, s) = setup(screenW = 20, screenH = 600)
        val b = boxes.first { it.id == "fps" }.rect
        s.onPress(b.left + 1, b.top + 1, 20, 600, boxes, mgr)
        s.onDrag(b.left + 5, b.top + 5, 20, 600, mgr); s.onRelease(); assertTrue(s.dirty)
    }

    @Test fun `prune deselects when the selected element has no box`() {
        val (mgr, boxes, s) = setup(); selectFps(s, boxes, mgr); s.pruneSelection(emptyList()); assertNull(s.selectedId)
    }

    @Test fun `panel toggle flips bold on the selected element`() {
        val (mgr, boxes, s) = setup(); selectFps(s, boxes, mgr)
        val bold = control("bold"); s.onPress(bold.left + bold.width / 2, bold.top + bold.height / 2, 800, 600, boxes, mgr)
        assertTrue(mgr.hudById("fps")!!.style.bold)
    }

    @Test fun `swatch recolors the selected element keeping its alpha`() {
        val (mgr, boxes, s) = setup(); selectFps(s, boxes, mgr)
        val sw = control("swatch:2"); s.onPress(sw.left + sw.width / 2, sw.top + sw.height / 2, 800, 600, boxes, mgr)
        assertEquals(MaeveColor.rgbOf(MaevePalette.primary), MaeveColor.rgbOf(fpsColor(mgr)))
    }

    @Test fun `scale buttons clamp to range`() {
        val (mgr, boxes, s) = setup(); selectFps(s, boxes, mgr)
        repeat(20) { val c = control("scale+"); s.onPress(c.left + 1, c.top + 1, 800, 600, boxes, mgr) }
        assertEquals(3.0f, mgr.hudById("fps")!!.style.scale)
    }

    @Test fun `SV square sets full saturation and value at top-right`() {
        val (mgr, boxes, s) = setup(); selectFps(s, boxes, mgr)
        val r = control("sv"); s.onPress(r.left + r.width - 1, r.top, 800, 600, boxes, mgr)
        val (_, sat, value) = MaeveColor.rgbToHsv(MaeveColor.rgbOf(fpsColor(mgr)))
        assertTrue(sat > 0.95f, "sat=$sat"); assertTrue(value > 0.95f, "value=$value")
    }

    @Test fun `hue bar sets the hue from vertical position`() {
        val (mgr, boxes, s) = setup(); selectFps(s, boxes, mgr)
        val r = control("hue"); s.onPress(r.left + 1, r.top + r.height / 2, 800, 600, boxes, mgr) // mid -> ~180
        val (hue, _, _) = MaeveColor.rgbToHsv(MaeveColor.rgbOf(fpsColor(mgr)))
        assertTrue(hue in 170f..190f, "hue=$hue")
    }

    @Test fun `alpha bar bottom makes the color transparent`() {
        val (mgr, boxes, s) = setup(); selectFps(s, boxes, mgr)
        val r = control("alpha"); s.onPress(r.left + 1, r.top + r.height - 1, 800, 600, boxes, mgr)
        assertTrue(MaeveColor.alphaOf(fpsColor(mgr)) < 10, "alpha=${MaeveColor.alphaOf(fpsColor(mgr))}")
    }

    @Test fun `picker drag updates continuously`() {
        val (mgr, boxes, s) = setup(); selectFps(s, boxes, mgr)
        val r = control("sv")
        s.onPress(r.left + 1, r.top + r.height - 1, 800, 600, boxes, mgr) // s~0, v~0 -> near black
        s.onDrag(r.left + r.width - 1, r.top, 800, 600, mgr)             // drag to s~1, v~1
        s.onRelease()
        val (_, sat, value) = MaeveColor.rgbToHsv(MaeveColor.rgbOf(fpsColor(mgr)))
        assertTrue(sat > 0.95f && value > 0.95f, "sat=$sat value=$value")
    }

    @Test fun `hex field applies a full 6-digit code`() {
        val (mgr, boxes, s) = setup(); selectFps(s, boxes, mgr)
        val hex = control("hex"); s.onPress(hex.left + 2, hex.top + 2, 800, 600, boxes, mgr)
        assertTrue(s.isHexFocused)
        "00FF00".forEach { s.onCharTyped(it, mgr) }
        assertEquals(0xFF00FF00.toInt(), fpsColor(mgr))
    }

    @Test fun `6-digit hex keeps the element's current alpha`() {
        val (mgr, boxes, s) = setup(); selectFps(s, boxes, mgr)
        val a = control("alpha"); s.onPress(a.left + 1, a.top + a.height - 1, 800, 600, boxes, mgr) // alpha ~ 0
        assertTrue(MaeveColor.alphaOf(fpsColor(mgr)) < 10)
        val hex = control("hex"); s.onPress(hex.left + 2, hex.top + 2, 800, 600, boxes, mgr)
        "112233".forEach { s.onCharTyped(it, mgr) }
        assertEquals(0x112233, MaeveColor.rgbOf(fpsColor(mgr)))
        assertTrue(MaeveColor.alphaOf(fpsColor(mgr)) < 10, "alpha preserved on 6-digit hex")
    }

    @Test fun `hex field ignores an incomplete code`() {
        val (mgr, boxes, s) = setup(); selectFps(s, boxes, mgr)
        val before = fpsColor(mgr)
        val hex = control("hex"); s.onPress(hex.left + 2, hex.top + 2, 800, 600, boxes, mgr)
        s.onCharTyped('A', mgr); s.onCharTyped('B', mgr) // only 2 chars -> no apply
        assertEquals(before, fpsColor(mgr))
    }
}

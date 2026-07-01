package gg.snell.mod.menu

import gg.snell.mod.platform.EditorCanvas
import gg.snell.mod.platform.TextRun
import gg.snell.mod.ui.node.Layout
import gg.snell.mod.ui.node.asMetrics
import gg.snell.mod.ui.node.find
import gg.snell.mod.ui.node.render
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Paint-level regression for the title screen: renders the node tree through a recording canvas
 * and asserts on the recorded draw calls (what text is drawn where), complementing the pure-layout
 * checks in [MenuLayoutTest].
 */
class TitleRenderTest {
    private class Recorded(val kind: String, val x: Int, val y: Int, val text: String)

    /** Records text/texture draw calls (in absolute design-space coords); everything else is a no-op. */
    private class RecordingCanvas : EditorCanvas {
        val calls = mutableListOf<Recorded>()
        val textures = mutableListOf<String>()
        private var dx = 0f
        private var dy = 0f
        private var scale = 1f

        override fun drawText(x: Int, y: Int, text: String, color: Int) = record("text", x, y, text)
        override fun drawStyledText(x: Int, y: Int, text: String, run: TextRun) = record("text", x, y, text)
        override fun drawMono(x: Int, y: Int, text: String, color: Int) = record("mono", x, y, text)
        override fun drawDisplay(x: Int, y: Int, text: String, color: Int) = record("display", x, y, text)
        private fun record(kind: String, x: Int, y: Int, text: String) {
            calls += Recorded(kind, (dx + x * scale).toInt(), (dy + y * scale).toInt(), text)
        }

        override fun withScale(scale: Float, pivotX: Int, pivotY: Int, body: () -> Unit) {
            val (sdx, sdy, ss) = Triple(dx, dy, this.scale)
            dx += pivotX * ss; dy += pivotY * ss; this.scale = ss * scale
            try { body() } finally { dx = sdx; dy = sdy; this.scale = ss }
        }

        override fun drawTexture(id: String, x: Int, y: Int, w: Int, h: Int) { textures += id }
        override fun fill(x: Int, y: Int, w: Int, h: Int, color: Int) {}
        override fun border(x: Int, y: Int, w: Int, h: Int, color: Int) {}
        override fun gradientV(x: Int, y: Int, w: Int, h: Int, top: Int, bottom: Int) {}
        override fun overlayStratum() {}
        override fun drawIcon(glyph: Char, x: Int, y: Int, color: Int) {}
        override fun iconWidth(glyph: Char) = 8
        override fun sprite(id: String, x: Int, y: Int, w: Int, h: Int, tint: Int) {}
        override fun textWidth(text: String) = text.length * 6
        override fun monoWidth(text: String) = text.length * 6
        override fun displayWidth(text: String) = text.length * 40
        override val lineHeight = 9
        override val screenWidth = 1440
        override val screenHeight = 810
    }

    private val w = 1440
    private val h = 810

    private fun rendered(d: TitleData = TitleData(username = "Bihzeh")): Pair<gg.snell.mod.ui.node.Node, RecordingCanvas> {
        val c = RecordingCanvas()
        val t = TitleView.build(d).also { Layout.layout(it, w, h, c.asMetrics()) }
        t.render(c, -1, -1)
        return t to c
    }

    @Test fun `wordmark is the logo alone - no display lettering`() {
        val (_, c) = rendered()
        assertTrue(c.calls.none { it.kind == "display" }, "no wordmark lettering, logo only")
        assertTrue(c.textures.any { it.contains("snell_mark") }, "slipstream mark drawn")
    }

    @Test fun `account chip drops the status line and centres the username`() {
        val (t, c) = rendered()
        val chip = assertNotNull(t.find("account"), "account chip node carries an id").rect
        assertTrue(c.calls.none { it.text == "Online" }, "no status text in the chip")
        val name = assertNotNull(c.calls.find { it.text == "Bihzeh" }, "username drawn")
        val centred = chip.top + (chip.height - c.lineHeight) / 2
        assertTrue(name.y in centred..centred + 2, "username vertically centred (y=${name.y}, chip=$chip)")
    }

    @Test fun `nav rows separate header and info lines by a wider gap`() {
        val (_, c) = rendered()
        val head = assertNotNull(c.calls.find { it.text == "Singleplayer" })
        val info = assertNotNull(c.calls.find { it.text == "Create or load a world" })
        assertEquals(c.lineHeight + 4, info.y - head.y, "header→info gap")
    }
}

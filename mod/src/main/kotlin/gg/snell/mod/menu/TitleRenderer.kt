package gg.snell.mod.menu

import gg.snell.mod.editor.Rect
import gg.snell.mod.platform.EditorCanvas
import gg.snell.mod.ui.SnellBtn
import gg.snell.mod.ui.SnellUi
import gg.snell.shared.SnellPalette

/** Pure renderer for the bespoke title screen — backdrop, brand lockup, button column, footer. */
object TitleRenderer {
    private val labels = mapOf(
        "singleplayer" to "Singleplayer",
        "multiplayer" to "Multiplayer",
        "options" to "Options",
        "quit" to "Quit Game",
    )

    fun render(canvas: EditorCanvas, w: Int, h: Int, mouseX: Int, mouseY: Int, version: String = "26.2") {
        SnellUi.backdrop(canvas, w, h)
        lockup(canvas, TitleLayout.logoRect(w, h))
        for (c in TitleLayout.buttons(w, h)) {
            val style = when (c.id) {
                "singleplayer" -> SnellBtn.Primary
                "quit" -> SnellBtn.Ghost
                else -> SnellBtn.Secondary
            }
            SnellUi.button(canvas, c.rect, labels[c.id] ?: c.id, style, hover = c.rect.contains(mouseX, mouseY))
        }
        val foot = "Snell Client $version"
        canvas.drawText((w - canvas.textWidth(foot)) / 2, h - 14, foot, SnellPalette.text3)
    }

    /** The Snell "slipstream" mark (three cyan bars) above the SNELL wordmark. */
    private fun lockup(canvas: EditorCanvas, r: Rect) {
        val cx = r.left + r.width / 2
        val barH = 9
        val gap = 7
        val top = r.top + 4
        // (width, cyan shade) per bar — the slipstream silhouette.
        val bars = listOf(84 to 0x1AA0D9, 108 to 0x00D9FF, 64 to 0x0E6FA8)
        bars.forEachIndexed { i, (bw, rgb) ->
            val bar = Rect(cx - bw / 2, top + i * (barH + gap), bw, barH)
            canvas.fill(bar.left, bar.top, bar.width, bar.height, 0xFF000000.toInt() or rgb)
            SnellUi.round(canvas, bar, SnellPalette.bg2)
        }
        val word = "SNELL"
        val s = 2.6f
        val tw = (canvas.textWidth(word) * s).toInt()
        SnellUi.heading(canvas, cx - tw / 2, top + 3 * (barH + gap) + 10, word, s)
    }
}

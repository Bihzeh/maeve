package gg.snell.mod.menu

import gg.snell.mod.platform.EditorCanvas
import gg.snell.mod.ui.SnellBtn
import gg.snell.mod.ui.SnellUi
import gg.snell.shared.SnellPalette

/** Pure renderer for the bespoke pause menu — a scrim over the world + a centred button column. */
object PauseRenderer {
    private val labels = mapOf(
        "resume" to "Back to Game",
        "options" to "Options",
        "advancements" to "Advancements",
        "statistics" to "Statistics",
        "disconnect" to "Save & Quit to Title",
    )

    fun render(canvas: EditorCanvas, w: Int, h: Int, mouseX: Int, mouseY: Int) {
        SnellUi.scrim(canvas, w, h)
        // Heading: small accent eyebrow + big "Paused".
        val eyebrow = "SNELL"
        canvas.drawText((w - canvas.textWidth(eyebrow)) / 2, PauseLayout.titleY(h), eyebrow, SnellPalette.accent)
        val title = "Paused"
        val s = 2.0f
        val tw = (canvas.textWidth(title) * s).toInt()
        SnellUi.heading(canvas, (w - tw) / 2, PauseLayout.titleY(h) + canvas.lineHeight + 4, title, s)

        for (c in PauseLayout.buttons(w, h)) {
            val style = when (c.id) {
                "resume" -> SnellBtn.Primary
                "disconnect" -> SnellBtn.Ghost
                else -> SnellBtn.Secondary
            }
            SnellUi.button(canvas, c.rect, labels[c.id] ?: c.id, style, hover = c.rect.contains(mouseX, mouseY))
        }
    }
}

package gg.snell.mod.menu

import gg.snell.mod.editor.Control
import gg.snell.mod.editor.Rect

/**
 * Pure layout for the bespoke pause menu: a centred button column over a scrimmed world.
 * No Minecraft types — unit-testable and headlessly renderable.
 */
object PauseLayout {
    const val BTN_W = 220
    const val BTN_H = 24
    const val GAP = 9

    /** Button ids in draw order; the screen maps these to the vanilla pause actions. */
    val IDS = listOf("resume", "options", "advancements", "statistics", "disconnect")

    fun titleY(h: Int): Int = (h * 0.16f).toInt()

    fun buttons(w: Int, h: Int): List<Control> {
        val x = (w - BTN_W) / 2
        // Anchored below the heading (not screen-centred) so the title never collides with row 1.
        var y = (h * 0.34f).toInt()
        val out = ArrayList<Control>(IDS.size)
        for (id in IDS) {
            out += Control(id, Rect(x, y, BTN_W, BTN_H))
            y += BTN_H + GAP
        }
        return out
    }

    fun hit(w: Int, h: Int, mouseX: Int, mouseY: Int): String? =
        buttons(w, h).firstOrNull { it.rect.contains(mouseX, mouseY) }?.id
}

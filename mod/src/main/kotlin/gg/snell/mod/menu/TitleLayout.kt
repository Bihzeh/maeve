package gg.snell.mod.menu

import gg.snell.mod.editor.Control
import gg.snell.mod.editor.Rect

/**
 * Pure layout for the bespoke title screen: a centred button column under the brand lockup.
 * No Minecraft types, so it can be unit-tested and headlessly rendered.
 */
object TitleLayout {
    const val BTN_W = 240
    const val BTN_H = 26
    const val GAP = 10

    /** Button ids in draw order; the screen maps these to vanilla actions. */
    val IDS = listOf("singleplayer", "multiplayer", "options", "quit")

    /** The brand lockup region (slipstream bars + SNELL wordmark), top-centred. */
    fun logoRect(w: Int, h: Int): Rect {
        val lw = 240
        val lh = 96
        return Rect((w - lw) / 2, (h * 0.13f).toInt(), lw, lh)
    }

    fun buttons(w: Int, h: Int): List<Control> {
        val x = (w - BTN_W) / 2
        var y = (h * 0.46f).toInt()
        val out = ArrayList<Control>(IDS.size)
        for (id in IDS) {
            out += Control(id, Rect(x, y, BTN_W, BTN_H))
            y += BTN_H + GAP
        }
        return out
    }

    /** Id of the button under the cursor, or null. */
    fun hit(w: Int, h: Int, mouseX: Int, mouseY: Int): String? =
        buttons(w, h).firstOrNull { it.rect.contains(mouseX, mouseY) }?.id
}

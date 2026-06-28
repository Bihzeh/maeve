package gg.snell.mod.menu

import gg.snell.mod.editor.Control
import gg.snell.mod.editor.Rect

/**
 * Pure layout for the bespoke Options screen: a centred panel with a header, a 2-column grid of
 * option rows, a footer row of category buttons and a Done button. No Minecraft types, so it can
 * be unit-tested and headlessly rendered.
 *
 * Coordinates are all derived from [panelRect], so every region scales with the screen.
 */
object OptionsLayout {
    /** Title-bar height inside the panel (matches [gg.snell.mod.ui.SnellUi.header]). */
    const val HEADER_H = 22

    /** Inner padding between the panel edge and its content. */
    const val PAD = 12

    /** A single option row. */
    const val ROW_H = 22
    const val ROW_GAP = 6

    /** The grid is always two columns wide. */
    const val ITEM_COLUMNS = 2
    const val COL_GAP = 10

    /** Footer button height + the gap between the category row and the Done row. */
    const val FOOTER_H = 22
    const val FOOTER_GAP = 6

    /** Width of the right-hand control area inside each row (toggle / cycle / slider). */
    const val CONTROL_W = 96

    /** Category footer-button ids, left to right. */
    val CATEGORIES = listOf("video", "sound", "controls", "chat", "accessibility")
    const val CAT_GAP = 6

    /** Done button width, right-aligned on its own row. */
    const val DONE_W = 100

    // ---- panel ---------------------------------------------------------------------------------

    /** Centred card. Capped so it leaves margins on big screens and never overflows small ones. */
    fun panelRect(w: Int, h: Int): Rect {
        val pw = minOf(w - 32, 460)
        val ph = minOf(h - 32, 320)
        return Rect((w - pw) / 2, (h - ph) / 2, pw, ph)
    }

    // ---- grid ----------------------------------------------------------------------------------

    private fun gridLeft(p: Rect) = p.left + PAD
    private fun gridTop(p: Rect) = p.top + HEADER_H + PAD
    private fun gridWidth(p: Rect) = p.width - 2 * PAD
    private fun colWidth(p: Rect) = (gridWidth(p) - COL_GAP) / ITEM_COLUMNS

    /** Full row rect for option [index] (left column = even indices, right column = odd). */
    fun itemRect(index: Int, w: Int, h: Int): Rect {
        val p = panelRect(w, h)
        val cw = colWidth(p)
        val col = index % ITEM_COLUMNS
        val row = index / ITEM_COLUMNS
        val x = gridLeft(p) + col * (cw + COL_GAP)
        val y = gridTop(p) + row * (ROW_H + ROW_GAP)
        return Rect(x, y, cw, ROW_H)
    }

    /** The right-hand control area within an item row — hit-test the toggle/cycle/slider here. */
    fun controlRect(index: Int, w: Int, h: Int): Rect {
        val row = itemRect(index, w, h)
        return Rect(row.right - CONTROL_W, row.top, CONTROL_W, row.height)
    }

    // ---- footer --------------------------------------------------------------------------------

    private fun doneTop(p: Rect) = p.bottom - PAD - FOOTER_H
    private fun catTop(p: Rect) = doneTop(p) - FOOTER_GAP - FOOTER_H

    /** Category buttons, equally dividing the grid width on the row above [doneButton]. */
    fun categoryButtons(w: Int, h: Int): List<Control> {
        val p = panelRect(w, h)
        val n = CATEGORIES.size
        val total = gridWidth(p)
        val cw = (total - (n - 1) * CAT_GAP) / n
        val y = catTop(p)
        val out = ArrayList<Control>(n)
        var x = gridLeft(p)
        for (id in CATEGORIES) {
            out += Control(id, Rect(x, y, cw, FOOTER_H))
            x += cw + CAT_GAP
        }
        return out
    }

    /** The primary Done button, right-aligned on the bottom footer row. */
    fun doneButton(w: Int, h: Int): Control {
        val p = panelRect(w, h)
        return Control("done", Rect(p.right - PAD - DONE_W, doneTop(p), DONE_W, FOOTER_H))
    }

    /** Index of the option row under the cursor, or -1. */
    fun hitItem(w: Int, h: Int, mouseX: Int, mouseY: Int, count: Int): Int {
        for (i in 0 until count) if (itemRect(i, w, h).contains(mouseX, mouseY)) return i
        return -1
    }

    /** Id of the category/Done button under the cursor, or null. */
    fun hitButton(w: Int, h: Int, mouseX: Int, mouseY: Int): String? {
        val done = doneButton(w, h)
        if (done.rect.contains(mouseX, mouseY)) return done.id
        return categoryButtons(w, h).firstOrNull { it.rect.contains(mouseX, mouseY) }?.id
    }
}

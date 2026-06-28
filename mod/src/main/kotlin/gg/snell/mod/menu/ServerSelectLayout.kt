package gg.snell.mod.menu

import gg.snell.mod.editor.Control
import gg.snell.mod.editor.Rect

/**
 * Pure layout for the bespoke multiplayer server picker — a centred card with a "Multiplayer"
 * header, a scrollable server list, and a footer action bar. Mirrors the world-select shape: all
 * geometry is plain integer maths over the scaled-GUI size, so it is unit-testable without a canvas.
 *
 * The canvas has no clipping, so the renderer asks this layout which rows are on-screen
 * ([visibleRange]) and where each sits ([rowRect]); rows outside the viewport are never drawn.
 */
object ServerSelectLayout {
    // Card chrome.
    const val HEADER_H = 26
    const val FOOTER_H = 36
    const val FOOTER_BTN_H = 22
    const val FOOTER_PAD = 8
    const val FOOTER_GAP = 6

    // List metrics — rowHeight fits a 28px icon + name + motd + a status pill.
    const val ROW_H = 40
    const val ROW_GAP = 4
    const val LIST_PAD = 8
    const val SCROLLBAR_GUTTER = 8

    /** Footer button ids, left → right; the screen maps these to vanilla multiplayer actions. */
    val FOOTER_IDS = listOf("join", "direct", "add", "edit", "delete", "refresh", "cancel")

    // Relative footer widths; the bar fills the card width responsively across GUI sizes.
    private val WEIGHTS = mapOf(
        "join" to 5, "direct" to 8, "add" to 4, "edit" to 4,
        "delete" to 5, "refresh" to 6, "cancel" to 5,
    )

    /** The centred card. ~480x270 leaves comfortable margins; clamps so 520x360 stays inset too. */
    fun panelRect(w: Int, h: Int): Rect {
        val pw = (w - 48).coerceIn(220, 460)
        val ph = (h - 40).coerceIn(160, 320)
        return Rect((w - pw) / 2, (h - ph) / 2, pw, ph)
    }

    /** The list viewport inside the card, between the header and the footer bar (inset by padding). */
    fun listViewport(w: Int, h: Int): Rect {
        val p = panelRect(w, h)
        val top = p.top + HEADER_H + LIST_PAD
        val bottom = p.bottom - FOOTER_H - LIST_PAD
        return Rect(p.left + LIST_PAD, top, p.width - LIST_PAD * 2, (bottom - top).coerceAtLeast(0))
    }

    private const val STEP = ROW_H + ROW_GAP

    /** Bounds of row [index] at the given [scrollY] (may sit partly/fully outside the viewport). */
    fun rowRect(index: Int, scrollY: Int, w: Int, h: Int): Rect {
        val vp = listViewport(w, h)
        val y = vp.top + index * STEP - scrollY
        return Rect(vp.left, y, vp.width - SCROLLBAR_GUTTER, ROW_H)
    }

    /** Total stacked height of [count] rows (no trailing gap), for scrollbar + clamp maths. */
    fun contentHeight(count: Int): Int = if (count <= 0) 0 else count * ROW_H + (count - 1) * ROW_GAP

    /** Largest valid [scrollY]; the screen clamps wheel input to 0..this. */
    fun maxScroll(count: Int, w: Int, h: Int): Int =
        (contentHeight(count) - listViewport(w, h).height).coerceAtLeast(0)

    /** Inclusive index range of rows intersecting the viewport at [scrollY]; EMPTY if none. */
    fun visibleRange(count: Int, scrollY: Int, w: Int, h: Int): IntRange {
        if (count <= 0) return IntRange.EMPTY
        val vp = listViewport(w, h)
        val first = (scrollY / STEP).coerceAtLeast(0)
        val last = ((scrollY + vp.height) / STEP).coerceAtMost(count - 1)
        return if (first > last) IntRange.EMPTY else first..last
    }

    /** Index of the row under the cursor (constrained to the viewport), or -1. */
    fun rowAt(count: Int, scrollY: Int, mouseX: Int, mouseY: Int, w: Int, h: Int): Int {
        if (!listViewport(w, h).contains(mouseX, mouseY)) return -1
        for (i in visibleRange(count, scrollY, w, h)) {
            if (rowRect(i, scrollY, w, h).contains(mouseX, mouseY)) return i
        }
        return -1
    }

    /** Footer action buttons, sized by [WEIGHTS] to fill the card width (last takes the remainder). */
    fun buttons(w: Int, h: Int): List<Control> {
        val p = panelRect(w, h)
        val gapTotal = (FOOTER_IDS.size - 1) * FOOTER_GAP
        val avail = (p.width - 2 * FOOTER_PAD - gapTotal).coerceAtLeast(FOOTER_IDS.size)
        val totalWeight = FOOTER_IDS.sumOf { WEIGHTS.getValue(it) }
        val y = p.bottom - FOOTER_H + (FOOTER_H - FOOTER_BTN_H) / 2
        var x = p.left + FOOTER_PAD
        var used = 0
        val out = ArrayList<Control>(FOOTER_IDS.size)
        FOOTER_IDS.forEachIndexed { idx, id ->
            val bw = if (idx == FOOTER_IDS.lastIndex) avail - used
            else avail * WEIGHTS.getValue(id) / totalWeight
            out += Control(id, Rect(x, y, bw, FOOTER_BTN_H))
            x += bw + FOOTER_GAP
            used += bw
        }
        return out
    }

    /** Id of the footer button under the cursor, or null. */
    fun hit(w: Int, h: Int, mouseX: Int, mouseY: Int): String? =
        buttons(w, h).firstOrNull { it.rect.contains(mouseX, mouseY) }?.id
}

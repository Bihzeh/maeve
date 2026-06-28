package gg.snell.mod.menu

import gg.snell.mod.editor.Control
import gg.snell.mod.editor.Rect

/**
 * Pure layout for the singleplayer world picker: a centred card filling most of the screen, a
 * titled header, a search field, a scrollable world list and a footer button bar. No Minecraft
 * types, so it's unit-testable and headlessly renderable; the runtime screen wires input/scroll.
 *
 * The canvas has no clipping, so the list region is exposed explicitly — [visibleRange] yields only
 * the indices that intersect the viewport for a given [scrollY], and [rowRect] places each one — and
 * the renderer skips everything else.
 */
object WorldSelectLayout {
    private const val MARGIN = 20      // screen inset around the card
    const val HEADER_H = 26            // titled header strip
    private const val PAD = 12         // inner body padding
    private const val SEARCH_H = 18    // search field height
    private const val LIST_GAP = 8     // gap between search field and the list
    const val rowHeight = 34
    const val rowGap = 6
    private const val FOOTER_BTN_H = 22
    private const val FOOTER_GAP = 8
    private const val SCROLL_GUTTER = 8 // reserved at the list's right edge for the scrollbar

    /** Footer button ids, left→right. The screen maps these to the vanilla world actions. */
    val IDS = listOf("play", "create", "edit", "delete", "cancel")

    private fun stride() = rowHeight + rowGap

    /** The card: centred, leaving a uniform [MARGIN] around it (most of the screen). */
    fun panelRect(w: Int, h: Int): Rect = Rect(MARGIN, MARGIN, w - 2 * MARGIN, h - 2 * MARGIN)

    /** Y of the footer button row's top (a fixed strip pinned to the bottom of the card). */
    private fun footerTop(panel: Rect): Int = panel.bottom - PAD - FOOTER_BTN_H

    /** The content area between the header and the footer bar, inset by [PAD]. */
    fun bodyRect(w: Int, h: Int): Rect {
        val p = panelRect(w, h)
        val top = p.top + HEADER_H + PAD
        val bottom = footerTop(p) - PAD
        return Rect(p.left + PAD, top, p.width - 2 * PAD, bottom - top)
    }

    /** Search field, pinned to the top of the body. */
    fun searchRect(w: Int, h: Int): Rect {
        val b = bodyRect(w, h)
        return Rect(b.left, b.top, b.width, SEARCH_H)
    }

    /** Scrollable list viewport, below the search field. The scrollbar lives in its right gutter. */
    fun listRect(w: Int, h: Int): Rect {
        val b = bodyRect(w, h)
        val s = searchRect(w, h)
        val top = s.bottom + LIST_GAP
        return Rect(b.left, top, b.width, b.bottom - top)
    }

    /** Total height of all rows stacked (content extent, for scroll maths + the scrollbar thumb). */
    fun contentHeight(count: Int): Int =
        if (count <= 0) 0 else count * rowHeight + (count - 1) * rowGap

    /** The furthest [scrollY] can travel so the last row rests at the list bottom. */
    fun maxScroll(count: Int, w: Int, h: Int): Int =
        (contentHeight(count) - listRect(w, h).height).coerceAtLeast(0)

    /** Bounds of row [index] for a given [scrollY] (width excludes the scrollbar gutter). */
    fun rowRect(index: Int, scrollY: Int, w: Int, h: Int): Rect {
        val list = listRect(w, h)
        val y = list.top + index * stride() - scrollY
        return Rect(list.left, y, list.width - SCROLL_GUTTER, rowHeight)
    }

    /** X of the scrollbar track (in the list's right gutter). */
    fun scrollbarX(w: Int, h: Int): Int = listRect(w, h).right - 3

    /** Inclusive index range whose rows intersect the list viewport at [scrollY]; empty if none. */
    fun visibleRange(count: Int, scrollY: Int, w: Int, h: Int): IntRange {
        if (count <= 0) return IntRange.EMPTY
        val list = listRect(w, h)
        val s = stride()
        val first = (scrollY / s).coerceAtLeast(0)
        val last = ((scrollY + list.height) / s).coerceAtMost(count - 1)
        return if (last < first) IntRange.EMPTY else first..last
    }

    /** Footer button bar: five evenly-split controls across the card's inner width. */
    fun buttons(w: Int, h: Int): List<Control> {
        val p = panelRect(w, h)
        val barLeft = p.left + PAD
        val barW = p.width - 2 * PAD
        val y = footerTop(p)
        val n = IDS.size
        val btnW = (barW - (n - 1) * FOOTER_GAP) / n
        val out = ArrayList<Control>(n)
        var x = barLeft
        for ((i, id) in IDS.withIndex()) {
            // The last button absorbs integer-division remainder so the bar ends flush with the card.
            val cw = if (i == n - 1) barLeft + barW - x else btnW
            out += Control(id, Rect(x, y, cw, FOOTER_BTN_H))
            x += btnW + FOOTER_GAP
        }
        return out
    }

    /** Id of the footer button under the cursor, or null. */
    fun footerHit(w: Int, h: Int, mouseX: Int, mouseY: Int): String? =
        buttons(w, h).firstOrNull { it.rect.contains(mouseX, mouseY) }?.id

    /** Index of the list row under the cursor (only visible rows, inside the viewport), or null. */
    fun rowAt(w: Int, h: Int, scrollY: Int, count: Int, mx: Int, my: Int): Int? {
        val list = listRect(w, h)
        if (!list.contains(mx, my)) return null
        for (i in visibleRange(count, scrollY, w, h)) {
            val r = rowRect(i, scrollY, w, h)
            if (r.contains(mx, my)) return i
        }
        return null
    }
}

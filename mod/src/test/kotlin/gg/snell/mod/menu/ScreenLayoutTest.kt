package gg.snell.mod.menu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Pure-layout regression for the scrollable pickers + the options grid (no rendering). */
class ScreenLayoutTest {
    private val w = 520
    private val h = 360

    @Test fun `world footer carries the action ids and scroll clamps`() {
        assertEquals(WorldSelectLayout.IDS, WorldSelectLayout.buttons(w, h).map { it.id })
        assertEquals(0, WorldSelectLayout.maxScroll(0, w, h))
        assertTrue(WorldSelectLayout.maxScroll(100, w, h) > 0)
    }

    @Test fun `world visible range stays within the list and is empty when no rows`() {
        assertTrue(WorldSelectLayout.visibleRange(0, 0, w, h).isEmpty())
        val r = WorldSelectLayout.visibleRange(100, 0, w, h)
        assertEquals(0, r.first)
        assertTrue(r.last in 0..99 && r.last < 100)
    }

    @Test fun `server footer is populated and scroll clamps`() {
        assertTrue(ServerSelectLayout.buttons(w, h).isNotEmpty())
        assertEquals(0, ServerSelectLayout.maxScroll(0, w, h))
        assertTrue(ServerSelectLayout.maxScroll(80, w, h) > 0)
        assertTrue(ServerSelectLayout.visibleRange(80, 0, w, h).first == 0)
    }

    @Test fun `options items flow two columns then down`() {
        val a = OptionsLayout.itemRect(0, w, h) // col 0, row 0
        val b = OptionsLayout.itemRect(1, w, h) // col 1, row 0
        val c = OptionsLayout.itemRect(2, w, h) // col 0, row 1
        assertTrue(b.left > a.left, "second item is in the right column")
        assertEquals(a.top, b.top, "first row shares a top")
        assertTrue(c.top > a.top, "third item wraps to the next row")
        assertEquals(a.left, c.left, "third item returns to the left column")
    }
}

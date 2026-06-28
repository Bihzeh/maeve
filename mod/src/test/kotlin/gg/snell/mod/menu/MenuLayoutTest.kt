package gg.snell.mod.menu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MenuLayoutTest {
    private val w = 480
    private val h = 270

    @Test fun `title buttons are centred, ordered and non-overlapping`() {
        val btns = TitleLayout.buttons(w, h)
        assertEquals(TitleLayout.IDS, btns.map { it.id })
        for (b in btns) assertEquals((w - b.rect.width) / 2, b.rect.left, "centred: ${b.id}")
        for (i in 1 until btns.size) assertTrue(btns[i].rect.top >= btns[i - 1].rect.bottom, "stacked: ${btns[i].id}")
    }

    @Test fun `title hit-testing maps a point to its button and misses empty space`() {
        val sp = TitleLayout.buttons(w, h).first().rect
        assertEquals("singleplayer", TitleLayout.hit(w, h, sp.left + 1, sp.top + 1))
        assertNull(TitleLayout.hit(w, h, 2, 2))
    }

    @Test fun `pause buttons are centred, below the title and on-screen`() {
        val btns = PauseLayout.buttons(w, h)
        assertEquals(PauseLayout.IDS, btns.map { it.id })
        for (b in btns) assertEquals((w - b.rect.width) / 2, b.rect.left)
        assertTrue(btns.first().rect.top > PauseLayout.titleY(h) + 30, "column starts below the title")
        assertTrue(btns.last().rect.bottom < h, "column fits on screen")
    }

    @Test fun `pause hit-testing resolves the disconnect button`() {
        val d = PauseLayout.buttons(w, h).last().rect
        assertEquals("disconnect", PauseLayout.hit(w, h, d.left + d.width / 2, d.top + d.height / 2))
    }
}

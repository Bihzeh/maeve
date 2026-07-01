package gg.snell.mod.menu

import gg.snell.mod.ui.node.Layout
import gg.snell.mod.ui.node.Metrics
import gg.snell.mod.ui.node.Node
import gg.snell.mod.ui.node.find
import gg.snell.mod.ui.node.hit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pure-layout regression for the ported World / Server / Options node trees (like [MenuLayoutTest]
 * covers Title + Pause): ids present, rows virtualized, scroll clamped, controls inside their rows.
 */
class ScreenLayoutTest {
    private val w = 1440
    private val h = 810

    private object FixedMetrics : Metrics {
        override fun textWidth(s: String) = s.length * 6
        override fun monoWidth(s: String) = s.length * 6
        override fun displayWidth(s: String) = s.length * 40
        override val lineHeight = 9
    }

    private fun laid(n: Node): Node { Layout.layout(n, w, h, FixedMetrics); return n }

    private fun worlds(n: Int) = List(n) { WorldRow("World $it", "w$it", "Survival", "1.21 · $it days ago", "w$it · 12 MB") }
    private fun servers(n: Int) = List(n) { ServerRow("Server $it", "s$it.example.net", "motd $it", "3/20", 40, ServerStatus.Online) }

    private fun worldTree(n: Int, scrollY: Int = 0, selected: Int = -1) =
        laid(WorldView.build(WorldState(worlds(n), selected, scrollY)))

    private fun serverTree(n: Int, scrollY: Int = 0) =
        laid(ServerView.build(ServerState(servers(n), -1, scrollY)))

    private fun optionEntries() = listOf(
        OptionEntry.Section("Rendering"),
        OptionEntry.Item(OptionItem("renderDistance", "Render Distance", OptionKind.Slider, "12 chunks", fraction = 0.4f)),
        OptionEntry.Item(OptionItem("graphics", "Graphics", OptionKind.Cycle, "Fancy")),
        OptionEntry.Item(OptionItem("vsync", "VSync", OptionKind.Toggle, "On", on = true)),
    )

    private fun optionsTree(scrollY: Int = 0) =
        laid(OptionsView.build(OptionsState(optionEntries(), "video", scrollY)))

    // ---- world ----------------------------------------------------------------------------------

    @Test fun `world footer carries the action ids in order and the list scroll clamps`() {
        val t = worldTree(5)
        val rects = WorldView.FOOTER_IDS.map { assertNotNull(t.find(it), "footer id $it").rect }
        for (i in 1 until rects.size) assertTrue(rects[i].left > rects[i - 1].left, "footer flows left→right")
        assertEquals(0, WorldView.maxScroll(t, 0))
        assertEquals(0, WorldView.maxScroll(t, 1))
        assertTrue(WorldView.maxScroll(t, 100) > 0, "long lists scroll")
    }

    @Test fun `world rows virtualize to the viewport and hit-test`() {
        val t = worldTree(100)
        val r0 = assertNotNull(t.find("row:0"), "first row built").rect
        assertNull(t.find("row:99"), "off-screen rows are not built")
        assertEquals("row:0", t.hit(r0.left + r0.width / 2, r0.top + r0.height / 2))
        val t2 = worldTree(100, scrollY = WorldView.maxScroll(t, 100))
        assertNotNull(t2.find("row:99"), "scrolled to the end builds the last row")
        assertNull(t2.find("row:0"), "start rows dropped once scrolled away")
    }

    @Test fun `world header carries back and rows sit below the header band`() {
        val t = worldTree(3)
        val back = assertNotNull(t.find("back")).rect
        val row = assertNotNull(t.find("row:0")).rect
        assertTrue(row.top >= back.bottom, "list starts under the header")
    }

    // ---- server ---------------------------------------------------------------------------------

    @Test fun `server footer + header ids are present and rows virtualize`() {
        val t = serverTree(50)
        (ServerView.FOOTER_IDS + listOf("back", "refresh")).forEach { assertNotNull(t.find(it), "id $it") }
        assertNotNull(t.find("row:0"))
        assertNull(t.find("row:49"))
        assertTrue(ServerView.maxScroll(t, 50) > 0)
    }

    // ---- options --------------------------------------------------------------------------------

    @Test fun `options rail carries the categories and content rows stack`() {
        val t = optionsTree()
        OptionsView.CATEGORIES.forEach { assertNotNull(t.find(it), "category $it") }
        val r1 = assertNotNull(t.find("row:1")).rect
        val r2 = assertNotNull(t.find("row:2")).rect
        assertTrue(r2.top > r1.top, "rows flow downward")
    }

    @Test fun `options controls sit inside their rows and hit before the row`() {
        val t = optionsTree()
        val row = assertNotNull(t.find("row:1")).rect
        val ctrl = assertNotNull(t.find("ctrl:renderDistance"), "slider control node").rect
        assertTrue(ctrl.left >= row.left && ctrl.right <= row.right && ctrl.top >= row.top && ctrl.bottom <= row.bottom, "control inside row")
        assertEquals("ctrl:renderDistance", t.hit(ctrl.left + ctrl.width / 2, ctrl.top + ctrl.height / 2))
        val done = assertNotNull(t.find("done")).rect
        assertEquals("done", t.hit(done.left + done.width / 2, done.top + done.height / 2))
    }
}

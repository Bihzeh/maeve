package gg.maeve.mod.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModuleBrowserLayoutTest {
    @Test fun `buttons are present and disjoint`() {
        val modules = ModuleBrowserLayout.modulesButton(800, 600)
        val done = ModuleBrowserLayout.doneButton(800, 600)
        assertTrue(modules.width > 0 && done.width > 0)
        assertTrue(done.left > modules.right, "Done sits right of Modules")
    }

    @Test fun `rows are one per id and inside the panel`() {
        val ids = listOf("a", "b", "c")
        val rows = ModuleBrowserLayout.rows(800, ids)
        assertEquals(ids, rows.map { it.first })
        val panel = ModuleBrowserLayout.panelRect(800, ids.size)
        for ((_, r) in rows) {
            assertTrue(r.left >= panel.left && r.right <= panel.right)
            assertTrue(r.top >= panel.top && r.bottom <= panel.bottom)
        }
    }
}

package gg.maeve.mod.module

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModuleOptionsTest {
    private val defs = listOf(ModuleToggle("a", "Alpha", true), ModuleToggle("b", "Beta", false))

    @Test fun `returns the declared default until set`() {
        val o = ModuleOptions(defs)
        assertTrue(o.get("a")); assertFalse(o.get("b"))
    }

    @Test fun `set overrides the default`() {
        val o = ModuleOptions(defs)
        o.set("a", false); o.set("b", true)
        assertFalse(o.get("a")); assertTrue(o.get("b"))
    }

    @Test fun `unknown key is false`() {
        assertFalse(ModuleOptions(defs).get("nope"))
    }

    @Test fun `exposes its toggle definitions`() {
        assertEquals(defs, ModuleOptions(defs).toggles)
    }
}

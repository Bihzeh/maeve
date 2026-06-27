package gg.maeve.launcher.ui

import gg.maeve.launcher.ui.components.MaeveIcons
import kotlin.test.Test
import kotlin.test.assertTrue

class MaeveIconsTest {
    /** Every Material Symbols glyph the launcher UI references by name must exist in the
     *  subsetted font's codepoint map — guards against typos and an out-of-date subset. */
    private val usedByUi = listOf(
        "stadia_controller", "extension", "auto_awesome", "group", "settings",
        "remove", "crop_square", "close", "play_arrow", "person", "lock", "check",
        "open_in_new", "content_copy", "error", "refresh", "verified", "speed",
        "dashboard", "tune", "download", "bolt", "folder_open", "code", "chevron_right",
    )

    @Test
    fun allReferencedGlyphsArePresent() {
        val missing = usedByUi.filterNot { it in MaeveIcons.codepoints }
        assertTrue(missing.isEmpty(), "Missing Material Symbols glyphs in subset: $missing")
    }

    @Test
    fun noBlankCodepoints() {
        val blank = MaeveIcons.codepoints.filterValues { it.code <= 0x20 }.keys
        assertTrue(blank.isEmpty(), "Blank/control codepoints mapped for: $blank")
    }
}

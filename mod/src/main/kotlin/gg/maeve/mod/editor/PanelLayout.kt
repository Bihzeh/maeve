package gg.maeve.mod.editor

import gg.maeve.shared.MaevePalette

/** A clickable region in the editor side panel. */
data class Control(val id: String, val rect: Rect)

/**
 * Lays out the editor's selection panel (right edge). Both the renderer (draw) and the state
 * machine (hit-test) call [controls] so they never drift. Pure. Includes a full HSV picker
 * (SV square + hue/alpha bars + hex field + preview) plus preset swatches and style toggles.
 */
object PanelLayout {
    const val WIDTH = 150
    const val TOP = 26 // below the editor's top button bar
    private const val PAD = 8
    const val SV = 80   // SV square side
    const val BAR = 10  // hue / alpha bar width

    /** Preset colors: launcher palette plus a few vivid Minecraft-friendly tones. */
    val SWATCHES = intArrayOf(
        MaevePalette.text, MaevePalette.gold, MaevePalette.primary, MaevePalette.success, MaevePalette.error,
        0xFFFFFFFF.toInt(), 0xFFFF5555.toInt(), 0xFF55FF55.toInt(), 0xFF55FFFF.toInt(), 0xFFFFFF55.toInt(),
    )

    val TOGGLES = listOf("visible", "bold", "italic", "underline", "strike", "shadow", "background")

    fun controls(panelLeft: Int, panelTop: Int): List<Control> {
        val l = panelLeft + PAD
        val w = WIDTH - PAD * 2
        val out = mutableListOf<Control>()
        var y = panelTop + 8
        out += Control("preview", Rect(l + w - 30, y, 30, 12))
        y += 18
        out += Control("sv", Rect(l, y, SV, SV))
        out += Control("hue", Rect(l + SV + 4, y, BAR, SV))
        out += Control("alpha", Rect(l + SV + 4 + BAR + 2, y, BAR, SV))
        y += SV + 6
        out += Control("hex", Rect(l, y, w, 12))
        y += 16
        val sw = 13; val gap = 4; val perRow = 5
        SWATCHES.forEachIndexed { i, _ ->
            val col = i % perRow; val r = i / perRow
            out += Control("swatch:$i", Rect(l + col * (sw + gap), y + r * (sw + gap), sw, sw))
        }
        y += ((SWATCHES.size + perRow - 1) / perRow) * (sw + gap) + 4
        for (id in TOGGLES) { out += Control(id, Rect(l, y, w, 13)); y += 16 }
        out += Control("scale-", Rect(l, y, 22, 13))
        out += Control("scale+", Rect(l + w - 22, y, 22, 13))
        y += 16
        out += Control("reset", Rect(l, y, w, 13))
        return out
    }

    fun panelRect(screenW: Int, screenH: Int): Rect = Rect(screenW - WIDTH, TOP, WIDTH, screenH - TOP)
}

package gg.maeve.mod.editor

import gg.maeve.shared.MaevePalette

/** A clickable region in the editor side panel. */
data class Control(val id: String, val rect: Rect)

/**
 * Lays out the editor's selection panel (right edge). Both the renderer (draw) and the
 * state machine (hit-test) call [controls] so they never drift. Pure. The swatch row gives
 * one-click recoloring; the full HSV picker is a later polish pass.
 */
object PanelLayout {
    const val WIDTH = 150
    const val TOP = 0
    private const val PAD = 8

    /** Preset colors: launcher palette plus a few vivid Minecraft-friendly tones. */
    val SWATCHES = intArrayOf(
        MaevePalette.text, MaevePalette.gold, MaevePalette.primary, MaevePalette.success, MaevePalette.error,
        0xFFFFFFFF.toInt(), 0xFFFF5555.toInt(), 0xFF55FF55.toInt(), 0xFF55FFFF.toInt(), 0xFFFFFF55.toInt(),
    )

    val TOGGLES = listOf("visible", "bold", "italic", "underline", "strike", "shadow", "background")

    fun controls(panelLeft: Int, panelTop: Int): List<Control> {
        val l = panelLeft + PAD
        val w = WIDTH - PAD * 2
        var y = panelTop + 22
        val out = mutableListOf<Control>()
        for (id in TOGGLES) { out += Control(id, Rect(l, y, w, 14)); y += 18 }
        out += Control("scale-", Rect(l, y, 22, 14))
        out += Control("scale+", Rect(l + w - 22, y, 22, 14))
        y += 18
        val sw = 14; val gap = 4; val perRow = 5
        SWATCHES.forEachIndexed { i, _ ->
            val col = i % perRow; val r = i / perRow
            out += Control("swatch:$i", Rect(l + col * (sw + gap), y + r * (sw + gap), sw, sw))
        }
        y += ((SWATCHES.size + perRow - 1) / perRow) * (sw + gap) + 6
        out += Control("reset", Rect(l, y, w, 14))
        return out
    }

    /** The full panel rectangle, for drawing the background and bounding clicks. */
    fun panelRect(screenW: Int, screenH: Int): Rect = Rect(screenW - WIDTH, TOP, WIDTH, screenH)
}

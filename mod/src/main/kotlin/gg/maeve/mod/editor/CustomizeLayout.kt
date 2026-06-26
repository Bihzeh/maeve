package gg.maeve.mod.editor

import gg.maeve.shared.MaevePalette

/**
 * Layout for the editor's customization popup. ONE HSV picker (left) edits a SELECTED colour target;
 * the right column stacks, top-to-bottom: colour-target chips, the module's option toggles, the
 * generic style toggles (visible/bold/italic), scale and reset — all uniform rows so geometry and
 * behaviour never drift between EditorState (hit-test) and EditorRenderer (draw). Swatches sit under
 * the picker. Non-HUD modules get a single enable toggle. Pure.
 */
object CustomizeLayout {
    const val W = 300
    const val NONHUD_W = 184
    private const val PAD = 10
    private const val TITLE_H = 20
    private const val SV = 100 // SV square side
    private const val BAR = 10 // hue / alpha bar width
    private const val GAP = 12 // gap between the columns
    private const val ROW_H = 15
    private const val ROW_GAP = 3
    private const val LEFT_W = SV + 4 + BAR + 2 + BAR // picker column width (126)

    /** Preset colours: launcher palette plus a few vivid Minecraft-friendly tones. */
    val SWATCHES = intArrayOf(
        MaevePalette.text, MaevePalette.gold, MaevePalette.primary, MaevePalette.success, MaevePalette.error,
        0xFFFFFFFF.toInt(), 0xFFFF5555.toInt(), 0xFF55FF55.toInt(), 0xFF55FFFF.toInt(), 0xFFFFFF55.toInt(),
    )

    /** Generic style toggles shown in the popup (the enable row uses the "visible" id). */
    val TOGGLES = listOf("bold", "italic")

    private fun rightRowCount(targetCount: Int, optionCount: Int) =
        targetCount + optionCount + 1 + TOGGLES.size + 1 + 1 // chips + options + visible + toggles + scale + reset

    private fun hudHeight(targetCount: Int, optionCount: Int): Int {
        val swatchRows = (SWATCHES.size + 4) / 5
        val leftH = 12 + 4 + SV + 6 + 12 + 4 + swatchRows * (13 + 4) // preview + sv + hex + swatches
        val rightH = rightRowCount(targetCount, optionCount) * (ROW_H + ROW_GAP)
        return TITLE_H + maxOf(leftH, rightH) + PAD
    }

    private fun nonHudHeight() = TITLE_H + ROW_H + PAD * 2

    fun popupRect(screenW: Int, screenH: Int, isHud: Boolean, targetCount: Int = 0, optionCount: Int = 0): Rect {
        val w = if (isHud) W else NONHUD_W
        val h = if (isHud) hudHeight(targetCount, optionCount) else nonHudHeight()
        return Rect((screenW - w) / 2, ((screenH - h) / 2).coerceAtLeast(0), w, h)
    }

    fun closeButton(popup: Rect): Rect = Rect(popup.right - PAD - 12, popup.top + 4, 12, 12)
    fun enableToggle(popup: Rect): Rect = Rect(popup.left + PAD, popup.top + TITLE_H, popup.width - 2 * PAD, ROW_H)

    private fun rightX(popup: Rect) = popup.left + PAD + LEFT_W + GAP
    private fun rightW(popup: Rect) = popup.right - PAD - rightX(popup)
    private fun rightRow(popup: Rect, i: Int): Rect =
        Rect(rightX(popup), popup.top + TITLE_H + i * (ROW_H + ROW_GAP), rightW(popup), ROW_H)

    /** Colour-target chips, stacked at the top of the right column. */
    fun targetChips(popup: Rect, count: Int): List<Rect> = (0 until count).map { rightRow(popup, it) }

    /** Module option switch rows, below the chips. */
    fun optionRows(popup: Rect, targetCount: Int, count: Int): List<Rect> =
        (0 until count).map { rightRow(popup, targetCount + it) }

    /** Picker + swatches (left) and the fixed right-column controls (after chips + options). */
    fun controls(popup: Rect, targetCount: Int, optionCount: Int): List<Control> {
        val out = mutableListOf<Control>()
        val l = popup.left + PAD
        val top = popup.top + TITLE_H
        out += Control("preview", Rect(l + LEFT_W - 30, top, 30, 12))
        val svTop = top + 16
        out += Control("sv", Rect(l, svTop, SV, SV))
        out += Control("hue", Rect(l + SV + 4, svTop, BAR, SV))
        out += Control("alpha", Rect(l + SV + 4 + BAR + 2, svTop, BAR, SV))
        out += Control("hex", Rect(l, svTop + SV + 6, LEFT_W, 12))
        val sw = 13; val gap = 4; val perRow = 5
        val swTop = svTop + SV + 6 + 12 + 4
        SWATCHES.forEachIndexed { i, _ ->
            val col = i % perRow; val r = i / perRow
            out += Control("swatch:$i", Rect(l + col * (sw + gap), swTop + r * (sw + gap), sw, sw))
        }
        var i = targetCount + optionCount
        out += Control("visible", rightRow(popup, i)); i++
        for (id in TOGGLES) { out += Control(id, rightRow(popup, i)); i++ }
        val sr = rightRow(popup, i); i++
        out += Control("scale-", Rect(sr.left, sr.top, 22, sr.height))
        out += Control("scale+", Rect(sr.right - 22, sr.top, 22, sr.height))
        out += Control("reset", rightRow(popup, i))
        return out
    }

    fun controlRect(popup: Rect, id: String, targetCount: Int, optionCount: Int): Rect? =
        controls(popup, targetCount, optionCount).firstOrNull { it.id == id }?.rect
}

package gg.snell.mod.menu

import gg.snell.mod.editor.Rect
import gg.snell.mod.platform.EditorCanvas
import gg.snell.mod.ui.SnellBtn
import gg.snell.mod.ui.SnellUi
import gg.snell.shared.SnellPalette

/**
 * Pure renderer for the bespoke Options screen — backdrop, panel, header, a 2-column grid of
 * option rows (each an inset list-row with a label + right-hand control) and a footer of category
 * buttons + a primary Done button. Launcher look: cyan accents over neutral-dark surfaces.
 */
object OptionsRenderer {
    private val categoryLabels = mapOf(
        "video" to "Video",
        "sound" to "Sound",
        "controls" to "Controls",
        "chat" to "Chat",
        "accessibility" to "Accessibility",
    )

    fun render(
        canvas: EditorCanvas,
        w: Int,
        h: Int,
        mouseX: Int,
        mouseY: Int,
        items: List<OptionItem>,
        hoveredIndex: Int,
    ) {
        SnellUi.backdrop(canvas, w, h)

        val panel = OptionsLayout.panelRect(w, h)
        SnellUi.panel(canvas, panel)
        SnellUi.header(canvas, panel, OptionsLayout.HEADER_H, "Options")

        for (i in items.indices) {
            row(canvas, w, h, items[i], hover = i == hoveredIndex, index = i)
        }

        for (c in OptionsLayout.categoryButtons(w, h)) {
            SnellUi.button(
                canvas, c.rect, categoryLabels[c.id] ?: c.id,
                SnellBtn.Secondary, hover = c.rect.contains(mouseX, mouseY),
            )
        }
        val done = OptionsLayout.doneButton(w, h)
        SnellUi.button(canvas, done.rect, "Done", SnellBtn.Primary, hover = done.rect.contains(mouseX, mouseY))
    }

    // ---- one option row ------------------------------------------------------------------------

    private fun row(canvas: EditorCanvas, w: Int, h: Int, item: OptionItem, hover: Boolean, index: Int) {
        val rect = OptionsLayout.itemRect(index, w, h)
        SnellUi.listRow(canvas, rect, selected = false, hover = hover)

        val ctrl = OptionsLayout.controlRect(index, w, h)
        val midY = rect.top + (rect.height - canvas.lineHeight) / 2 + 1

        // Label on the left, ellipsized so it never collides with the control.
        val labelX = rect.left + 8
        val labelW = (ctrl.left - 6) - labelX
        canvas.drawText(labelX, midY, SnellUi.ellipsize(canvas, item.label, labelW), SnellPalette.text)

        when (item.kind) {
            OptionKind.Toggle -> toggle(canvas, ctrl, item.on)
            OptionKind.Cycle -> cycle(canvas, ctrl, item.valueText, hover)
            OptionKind.Slider -> slider(canvas, ctrl, item.fraction, item.valueText)
        }
    }

    /** Right-aligned switch within the control area. */
    private fun toggle(canvas: EditorCanvas, ctrl: Rect, on: Boolean) {
        val sw = 24
        val sh = 14
        val x = ctrl.right - sw
        val y = ctrl.top + (ctrl.height - sh) / 2
        SnellUi.switch(canvas, Rect(x, y, sw, sh), on)
    }

    /** A compact Secondary button showing the current value. */
    private fun cycle(canvas: EditorCanvas, ctrl: Rect, valueText: String, hover: Boolean) {
        val bh = 16
        val y = ctrl.top + (ctrl.height - bh) / 2
        val r = Rect(ctrl.left, y, ctrl.width, bh)
        SnellUi.button(canvas, r, SnellUi.ellipsize(canvas, valueText, ctrl.width - 8), SnellBtn.Secondary, hover = hover)
    }

    /** A neutral track + cyan fill to [fraction] + a 6px knob, with [valueText] right-aligned. */
    private fun slider(canvas: EditorCanvas, ctrl: Rect, fraction: Float, valueText: String) {
        val f = fraction.coerceIn(0f, 1f)
        val vtw = canvas.textWidth(valueText)

        val trackLeft = ctrl.left
        val trackRight = ctrl.right - vtw - 6
        val trackW = (trackRight - trackLeft).coerceAtLeast(8)
        val trackH = 4
        val trackY = ctrl.top + (ctrl.height - trackH) / 2

        // Track groove.
        canvas.fill(trackLeft, trackY, trackW, trackH, SnellPalette.s2)
        canvas.border(trackLeft, trackY, trackW, trackH, SnellPalette.border)
        // Cyan fill.
        val fillW = (trackW * f).toInt()
        if (fillW > 0) canvas.fill(trackLeft, trackY, fillW, trackH, SnellPalette.accent)

        // 6px knob centred on the fill edge.
        val knob = 6
        val kx = (trackLeft + fillW - knob / 2).coerceIn(trackLeft, trackRight - knob)
        val ky = ctrl.top + (ctrl.height - knob) / 2
        canvas.fill(kx, ky, knob, knob, SnellUi.WHITE)
        canvas.border(kx, ky, knob, knob, SnellPalette.accentHi)

        // Value, right-aligned.
        val ty = ctrl.top + (ctrl.height - canvas.lineHeight) / 2 + 1
        canvas.drawText(ctrl.right - vtw, ty, valueText, SnellPalette.text2)
    }
}

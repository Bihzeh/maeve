package gg.maeve.mod.editor

import gg.maeve.mod.config.HexColor
import gg.maeve.mod.module.ModuleManager
import gg.maeve.mod.platform.EditorCanvas
import gg.maeve.mod.platform.GameContext
import gg.maeve.mod.render.HudModuleRender
import gg.maeve.shared.MaevePalette

/** Draws the HUD editor: live preview of all elements, hover/selection outlines, and the
 *  selection panel with a full HSV colour picker. Pure orchestration over [EditorCanvas]. */
class EditorRenderer {
    private val white = 0xFFFFFFFF.toInt()
    private val black = 0xFF000000.toInt()

    fun render(
        canvas: EditorCanvas, screenW: Int, screenH: Int, mouseX: Int, mouseY: Int,
        ctx: GameContext, modules: ModuleManager, state: EditorState,
    ) {
        val measurer = object : TextMeasurer {
            override fun width(text: String) = canvas.textWidth(text)
            override val lineHeight get() = canvas.lineHeight
        }
        val all = modules.hudModules()
        for (m in all) HudModuleRender.draw(canvas, m, m.render(ctx))
        canvas.overlayStratum()

        val boxes = ElementLayout.boxesFor(all, ctx, measurer, screenW, screenH)
        state.pruneSelection(boxes)
        val hover = hitTest(boxes, mouseX, mouseY)
        for (b in boxes) {
            val enabled = modules.hudById(b.id)?.enabled ?: true
            val color = when {
                b.id == state.selectedId -> MaevePalette.gold
                b.id == hover -> white
                !enabled -> MaevePalette.error
                else -> MaevePalette.outline
            }
            canvas.border(b.rect.left - 1, b.rect.top - 1, b.rect.width + 2, b.rect.height + 2, color)
        }

        canvas.drawText(6, 6, "Drag to move · click to select · Esc/Done to save", MaevePalette.text)
        drawButtons(canvas, screenW, state)
        state.selectedId?.let { drawPanel(canvas, it, screenW, screenH, modules, state) }
        if (state.browserOpen) drawBrowser(canvas, screenW, modules)
    }

    private fun drawPanel(canvas: EditorCanvas, sel: String, screenW: Int, screenH: Int, modules: ModuleManager, state: EditorState) {
        val module = modules.hudById(sel) ?: return
        val st = module.style
        val pr = PanelLayout.panelRect(screenW, screenH)
        canvas.fill(pr.left, pr.top, pr.width, pr.height, MaevePalette.surface)
        canvas.border(pr.left, pr.top, pr.width, pr.height, MaevePalette.outline)
        canvas.drawText(pr.left + 8, pr.top + 8, module.displayName, MaevePalette.gold)

        val controls = PanelLayout.controls(pr.left, PanelLayout.TOP).associateBy { it.id }
        controls["preview"]?.rect?.let { r ->
            checker(canvas, r.left, r.top, r.width, r.height)
            canvas.fill(r.left, r.top, r.width, r.height, st.color)
            canvas.border(r.left, r.top, r.width, r.height, MaevePalette.outline)
        }
        controls["sv"]?.rect?.let { r -> drawSvSquare(canvas, r, state) }
        controls["hue"]?.rect?.let { r -> drawHueBar(canvas, r, state) }
        controls["alpha"]?.rect?.let { r -> drawAlphaBar(canvas, r, state) }
        controls["hex"]?.rect?.let { r ->
            canvas.fill(r.left, r.top, r.width, r.height, MaevePalette.elevated)
            canvas.border(r.left, r.top, r.width, r.height, if (state.isHexFocused) MaevePalette.gold else MaevePalette.outline)
            val text = if (state.isHexFocused) "#" + state.hexText + "_" else HexColor.encode(st.color)
            canvas.drawText(r.left + 3, r.top + 2, text, MaevePalette.text)
        }

        for ((id, c) in controls) {
            val r = c.rect
            when {
                id in PanelLayout.TOGGLES -> {
                    val on = when (id) {
                        "visible" -> module.enabled; "bold" -> st.bold; "italic" -> st.italic
                        "underline" -> st.underline; "strike" -> st.strikethrough; "shadow" -> st.shadow
                        "background" -> st.background; else -> false
                    }
                    canvas.fill(r.left, r.top, r.width, r.height, if (on) MaevePalette.primary else MaevePalette.elevated)
                    canvas.border(r.left, r.top, r.width, r.height, MaevePalette.outline)
                    canvas.drawText(r.left + 4, r.top + 3, label(id), white)
                }
                id == "scale-" || id == "scale+" -> {
                    canvas.fill(r.left, r.top, r.width, r.height, MaevePalette.elevated)
                    canvas.border(r.left, r.top, r.width, r.height, MaevePalette.outline)
                    canvas.drawText(r.left + 7, r.top + 3, if (id == "scale-") "-" else "+", white)
                    if (id == "scale+") canvas.drawText(pr.left + 34, r.top + 3, "x%.2f".format(st.scale), MaevePalette.text2)
                }
                id == "reset" -> {
                    canvas.fill(r.left, r.top, r.width, r.height, MaevePalette.elevated)
                    canvas.border(r.left, r.top, r.width, r.height, MaevePalette.outline)
                    canvas.drawText(r.left + 4, r.top + 3, "Reset style", white)
                }
                id.startsWith("swatch:") -> {
                    val idx = id.removePrefix("swatch:").toInt()
                    canvas.fill(r.left, r.top, r.width, r.height, black or MaeveColor.rgbOf(PanelLayout.SWATCHES[idx]))
                    canvas.border(r.left, r.top, r.width, r.height, MaevePalette.outline)
                }
            }
        }
    }

    private fun drawSvSquare(canvas: EditorCanvas, r: Rect, state: EditorState) {
        // exact: for fixed (h,s), rgb(h,s,v) = v * rgb(h,s,1), so each column is a gradient to black
        val denom = (r.width - 1).coerceAtLeast(1)
        for (x in 0 until r.width) {
            val s = x.toFloat() / denom
            val top = black or MaeveColor.hsvToRgb(state.colorH, s, 1f)
            canvas.gradientV(r.left + x, r.top, 1, r.height, top, black)
        }
        val mx = (r.left + (state.colorS * r.width).toInt()).coerceIn(r.left, r.right - 1)
        val my = (r.top + ((1f - state.colorV) * r.height).toInt()).coerceIn(r.top, r.bottom - 1)
        canvas.border(mx - 2, my - 2, 4, 4, white)
        canvas.border(mx - 3, my - 3, 6, 6, black)
    }

    private fun drawHueBar(canvas: EditorCanvas, r: Rect, state: EditorState) {
        for (i in 0 until 6) {
            val y0 = r.top + i * r.height / 6        // boundary-based so the remainder never leaves a gap
            val y1 = r.top + (i + 1) * r.height / 6
            val top = black or MaeveColor.hsvToRgb(i * 60f, 1f, 1f)
            val bottom = black or MaeveColor.hsvToRgb((i + 1) * 60f, 1f, 1f)
            canvas.gradientV(r.left, y0, r.width, y1 - y0, top, bottom)
        }
        val my = (r.top + (state.colorH / 360f * r.height).toInt()).coerceIn(r.top, r.bottom - 1)
        canvas.border(r.left - 1, my - 1, r.width + 2, 3, white)
    }

    private fun drawAlphaBar(canvas: EditorCanvas, r: Rect, state: EditorState) {
        checker(canvas, r.left, r.top, r.width, r.height)
        val rgb = MaeveColor.hsvToRgb(state.colorH, state.colorS, state.colorV)
        canvas.gradientV(r.left, r.top, r.width, r.height, black or rgb, rgb) // bottom alpha 0
        canvas.border(r.left, r.top, r.width, r.height, MaevePalette.outline)
        val my = (r.top + ((1f - state.colorA / 255f) * r.height).toInt()).coerceIn(r.top, r.bottom - 1)
        canvas.border(r.left - 1, my - 1, r.width + 2, 3, white)
    }

    private fun checker(canvas: EditorCanvas, x: Int, y: Int, w: Int, h: Int) {
        canvas.fill(x, y, w, h, 0xFFBBBBBB.toInt())
        val s = 4
        var yy = 0
        while (yy < h) {
            var xx = 0
            while (xx < w) {
                if (((xx / s) + (yy / s)) % 2 == 0) {
                    canvas.fill(x + xx, y + yy, minOf(s, w - xx), minOf(s, h - yy), 0xFF777777.toInt())
                }
                xx += s
            }
            yy += s
        }
    }

    private fun drawButtons(canvas: EditorCanvas, screenW: Int, state: EditorState) {
        button(canvas, ModuleBrowserLayout.modulesButton(screenW), "Modules", state.browserOpen)
        button(canvas, ModuleBrowserLayout.doneButton(screenW), "Done", false)
    }

    private fun button(canvas: EditorCanvas, r: Rect, text: String, active: Boolean) {
        canvas.fill(r.left, r.top, r.width, r.height, if (active) MaevePalette.primary else MaevePalette.elevated)
        canvas.border(r.left, r.top, r.width, r.height, MaevePalette.outline)
        canvas.drawText(r.left + 6, r.top + 4, text, white)
    }

    private fun drawBrowser(canvas: EditorCanvas, screenW: Int, modules: ModuleManager) {
        val mods = modules.all().toList()
        val panel = ModuleBrowserLayout.panelRect(screenW, mods.size)
        canvas.fill(panel.left, panel.top, panel.width, panel.height, MaevePalette.surface)
        canvas.border(panel.left, panel.top, panel.width, panel.height, MaevePalette.gold)
        canvas.drawText(panel.left + 8, panel.top + 5, "Modules", MaevePalette.gold)
        val rows = ModuleBrowserLayout.rows(screenW, mods.map { it.id })
        for (i in mods.indices) {
            val m = mods[i]; val r = rows[i].second
            canvas.fill(r.left, r.top, r.width, r.height, MaevePalette.elevated)
            canvas.border(r.left, r.top, r.width, r.height, MaevePalette.outline)
            canvas.drawText(r.left + 6, r.top + 4, m.displayName, white)
            val pillW = 34
            val px = r.right - pillW - 4
            canvas.fill(px, r.top + 2, pillW, r.height - 4, if (m.enabled) MaevePalette.primary else MaevePalette.surface)
            canvas.border(px, r.top + 2, pillW, r.height - 4, MaevePalette.outline)
            canvas.drawText(px + 6, r.top + 4, if (m.enabled) "ON" else "OFF", white)
        }
    }

    private fun label(id: String) = when (id) {
        "visible" -> "Visible"; "bold" -> "Bold"; "italic" -> "Italic"; "underline" -> "Underline"
        "strike" -> "Strikethrough"; "shadow" -> "Shadow"; "background" -> "Background"; else -> id
    }
}

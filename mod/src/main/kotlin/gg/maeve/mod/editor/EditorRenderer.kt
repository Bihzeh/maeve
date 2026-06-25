package gg.maeve.mod.editor

import gg.maeve.mod.module.ModuleManager
import gg.maeve.mod.platform.EditorCanvas
import gg.maeve.mod.platform.GameContext
import gg.maeve.mod.render.HudModuleRender
import gg.maeve.shared.MaevePalette

/** Draws the HUD editor: live preview of all elements, hover/selection outlines, and the
 *  selection panel. Pure orchestration over [EditorCanvas]; no Minecraft types. */
class EditorRenderer {
    private val white = 0xFFFFFFFF.toInt()

    fun render(
        canvas: EditorCanvas, screenW: Int, screenH: Int, mouseX: Int, mouseY: Int,
        ctx: GameContext, modules: ModuleManager, state: EditorState,
    ) {
        val measurer = object : TextMeasurer {
            override fun width(text: String) = canvas.textWidth(text)
            override val lineHeight get() = canvas.lineHeight
        }
        val all = modules.hudModules()

        // 1) preview every module (disabled ones too, so they can be positioned)
        for (m in all) HudModuleRender.draw(canvas, m, m.render(ctx))
        canvas.overlayStratum()

        // 2) outlines: selected = gold, hover = white, disabled = red, else faint
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

        canvas.drawText(6, 6, "HUD Editor  -  drag to move, click to select, Esc to save", MaevePalette.text)
        state.selectedId?.let { drawPanel(canvas, it, screenW, screenH, modules) }
    }

    private fun drawPanel(canvas: EditorCanvas, sel: String, screenW: Int, screenH: Int, modules: ModuleManager) {
        val module = modules.hudById(sel) ?: return
        val st = module.style
        val pr = PanelLayout.panelRect(screenW, screenH)
        canvas.fill(pr.left, pr.top, pr.width, pr.height, MaevePalette.surface)
        canvas.border(pr.left, pr.top, pr.width, pr.height, MaevePalette.outline)
        canvas.drawText(pr.left + 8, pr.top + 8, module.displayName, MaevePalette.gold)

        for (c in PanelLayout.controls(pr.left, PanelLayout.TOP)) {
            val r = c.rect
            when {
                c.id in PanelLayout.TOGGLES -> {
                    val on = when (c.id) {
                        "visible" -> module.enabled; "bold" -> st.bold; "italic" -> st.italic
                        "underline" -> st.underline; "strike" -> st.strikethrough; "shadow" -> st.shadow
                        "background" -> st.background; else -> false
                    }
                    canvas.fill(r.left, r.top, r.width, r.height, if (on) MaevePalette.primary else MaevePalette.elevated)
                    canvas.border(r.left, r.top, r.width, r.height, MaevePalette.outline)
                    canvas.drawText(r.left + 4, r.top + 3, label(c.id), white)
                }
                c.id == "scale-" || c.id == "scale+" -> {
                    canvas.fill(r.left, r.top, r.width, r.height, MaevePalette.elevated)
                    canvas.border(r.left, r.top, r.width, r.height, MaevePalette.outline)
                    canvas.drawText(r.left + 7, r.top + 3, if (c.id == "scale-") "-" else "+", white)
                    if (c.id == "scale+") canvas.drawText(pr.left + 34, r.top + 3, "x%.2f".format(st.scale), MaevePalette.text2)
                }
                c.id == "reset" -> {
                    canvas.fill(r.left, r.top, r.width, r.height, MaevePalette.elevated)
                    canvas.border(r.left, r.top, r.width, r.height, MaevePalette.outline)
                    canvas.drawText(r.left + 4, r.top + 3, "Reset style", white)
                }
                c.id.startsWith("swatch:") -> {
                    val idx = c.id.removePrefix("swatch:").toInt()
                    canvas.fill(r.left, r.top, r.width, r.height, 0xFF000000.toInt() or MaeveColor.rgbOf(PanelLayout.SWATCHES[idx]))
                    canvas.border(r.left, r.top, r.width, r.height, MaevePalette.outline)
                }
            }
        }
    }

    private fun label(id: String) = when (id) {
        "visible" -> "Visible"; "bold" -> "Bold"; "italic" -> "Italic"; "underline" -> "Underline"
        "strike" -> "Strikethrough"; "shadow" -> "Shadow"; "background" -> "Background"; else -> id
    }
}

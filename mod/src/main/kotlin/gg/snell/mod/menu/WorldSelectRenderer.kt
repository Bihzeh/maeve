package gg.snell.mod.menu

import gg.snell.mod.editor.Rect
import gg.snell.mod.platform.EditorCanvas
import gg.snell.mod.ui.SnellBtn
import gg.snell.mod.ui.SnellUi
import gg.snell.shared.SnellPalette

/**
 * Pure renderer for the singleplayer world picker — scrim + card + header, a search field, the
 * visible slice of the world list (icon placeholder, name, subtitle, selection/hover), a scrollbar
 * and the footer action bar. All geometry comes from [WorldSelectLayout] so draw + hit-test never
 * drift. No Minecraft types; every pixel goes through [EditorCanvas].
 */
object WorldSelectRenderer {
    private const val ICON = 24
    private val labels = mapOf(
        "play" to "Play",
        "create" to "Create New",
        "edit" to "Edit",
        "delete" to "Delete",
        "cancel" to "Cancel",
    )

    fun render(
        canvas: EditorCanvas,
        w: Int,
        h: Int,
        mouseX: Int,
        mouseY: Int,
        rows: List<WorldRow>,
        selected: Int,
        scrollY: Int,
        search: String,
        searchFocused: Boolean,
    ) {
        SnellUi.scrim(canvas, w, h)

        val panel = WorldSelectLayout.panelRect(w, h)
        SnellUi.panel(canvas, panel)
        SnellUi.header(canvas, panel, WorldSelectLayout.HEADER_H, "Singleplayer")

        // Right-aligned world count in the header, like the launcher's section meta.
        val count = "${rows.size} ${if (rows.size == 1) "world" else "worlds"}"
        canvas.drawText(
            panel.right - 12 - canvas.textWidth(count),
            panel.top + (WorldSelectLayout.HEADER_H - canvas.lineHeight) / 2,
            count,
            SnellPalette.text3,
        )

        SnellUi.textField(canvas, WorldSelectLayout.searchRect(w, h), search, searchFocused, "Search worlds…")

        val list = WorldSelectLayout.listRect(w, h)
        if (rows.isEmpty()) {
            emptyState(canvas, list, search.isNotEmpty())
        } else {
            for (i in WorldSelectLayout.visibleRange(rows.size, scrollY, w, h)) {
                val r = WorldSelectLayout.rowRect(i, scrollY, w, h)
                // No clipping: skip any row that doesn't actually fall inside the viewport.
                if (r.bottom <= list.top || r.top >= list.bottom) continue
                row(canvas, r, rows[i], selected = i == selected, hover = r.contains(mouseX, mouseY))
            }
            SnellUi.scrollbar(
                canvas,
                WorldSelectLayout.scrollbarX(w, h),
                list.top,
                list.height,
                WorldSelectLayout.contentHeight(rows.size),
                scrollY,
            )
        }

        val hasSelection = selected in rows.indices
        for (c in WorldSelectLayout.buttons(w, h)) {
            val style = when (c.id) {
                "play" -> SnellBtn.Primary
                "delete" -> SnellBtn.Danger
                "cancel" -> SnellBtn.Ghost
                else -> SnellBtn.Secondary
            }
            // Play/Edit/Delete need a selected world; Create New / Cancel are always live.
            val enabled = when (c.id) {
                "play", "edit", "delete" -> hasSelection
                else -> true
            }
            SnellUi.button(
                canvas,
                c.rect,
                labels[c.id] ?: c.id,
                style,
                hover = enabled && c.rect.contains(mouseX, mouseY),
                enabled = enabled,
            )
        }
    }

    /** A single world row: rounded icon placeholder, name (primary) over subtitle (muted). */
    private fun row(canvas: EditorCanvas, r: Rect, world: WorldRow, selected: Boolean, hover: Boolean) {
        SnellUi.listRow(canvas, r, selected, hover)

        val iconY = r.top + (r.height - ICON) / 2
        val icon = Rect(r.left + 5, iconY, ICON, ICON)
        canvas.fill(icon.left, icon.top, icon.width, icon.height, SnellPalette.accentSubtle)
        canvas.border(icon.left, icon.top, icon.width, icon.height, SnellPalette.withAlpha(SnellPalette.accent, 0x55))
        SnellUi.round(canvas, icon, SnellPalette.s1)

        val textX = icon.right + 8
        val textW = r.right - 8 - textX
        val block = canvas.lineHeight * 2 + 3
        val nameY = r.top + (r.height - block) / 2
        val subY = nameY + canvas.lineHeight + 3
        canvas.drawText(textX, nameY, SnellUi.ellipsize(canvas, world.name, textW), SnellPalette.text)
        canvas.drawText(textX, subY, SnellUi.ellipsize(canvas, world.subtitle, textW), SnellPalette.text2)
    }

    /** Centred placeholder when there are no worlds (or none match the search). */
    private fun emptyState(canvas: EditorCanvas, list: Rect, filtered: Boolean) {
        val title = if (filtered) "No worlds match your search" else "No worlds yet"
        val hint = if (filtered) "Try a different name." else "Create a new world to get started."
        val cy = list.top + list.height / 2
        canvas.drawText(list.left + (list.width - canvas.textWidth(title)) / 2, cy - canvas.lineHeight, title, SnellPalette.text2)
        canvas.drawText(list.left + (list.width - canvas.textWidth(hint)) / 2, cy + 2, hint, SnellPalette.text3)
    }
}

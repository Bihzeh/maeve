package gg.snell.mod.menu

import gg.snell.mod.editor.Rect
import gg.snell.mod.platform.EditorCanvas
import gg.snell.mod.ui.PillRole
import gg.snell.mod.ui.SnellBtn
import gg.snell.mod.ui.SnellUi
import gg.snell.shared.SnellPalette

/**
 * Pure renderer for the bespoke multiplayer server picker — backdrop, card, "Multiplayer" header,
 * scrollable server list, and a footer action bar. Matches the launcher chrome via [SnellUi].
 *
 * The canvas has no clipping, so rows are drawn first (only those in the visible range) and may
 * bleed a few pixels past the viewport at the top/bottom edges; the header and footer bands are
 * then repainted on top, masking any overflow cleanly.
 */
object ServerSelectRenderer {
    private val LABELS = mapOf(
        "join" to "Join",
        "direct" to "Direct Connect",
        "add" to "Add Server",
        "edit" to "Edit",
        "delete" to "Delete",
        "refresh" to "Refresh",
        "cancel" to "Cancel",
    )

    fun render(
        canvas: EditorCanvas,
        w: Int,
        h: Int,
        mouseX: Int,
        mouseY: Int,
        rows: List<ServerRow>,
        selected: Int,
        scrollY: Int,
    ) {
        SnellUi.backdrop(canvas, w, h)
        val p = ServerSelectLayout.panelRect(w, h)
        SnellUi.panel(canvas, p)

        val vp = ServerSelectLayout.listViewport(w, h)
        if (rows.isEmpty()) {
            emptyState(canvas, vp)
        } else {
            for (i in ServerSelectLayout.visibleRange(rows.size, scrollY, w, h)) {
                drawRow(canvas, ServerSelectLayout.rowRect(i, scrollY, w, h), rows[i], i == selected, mouseX, mouseY, vp)
            }
            SnellUi.scrollbar(canvas, vp.right - 4, vp.top, vp.height, ServerSelectLayout.contentHeight(rows.size), scrollY)
        }

        // Mask any row overflow above the viewport, then lay the header chrome on top.
        canvas.fill(p.left + 1, p.top + 1, p.width - 2, (vp.top - (p.top + 1)).coerceAtLeast(0), SnellPalette.s1)
        SnellUi.header(canvas, p, ServerSelectLayout.HEADER_H, "Multiplayer")
        if (rows.isNotEmpty()) {
            val chip = if (rows.size == 1) "1 server" else "${rows.size} servers"
            canvas.drawText(
                p.right - 12 - canvas.textWidth(chip),
                p.top + (ServerSelectLayout.HEADER_H - canvas.lineHeight) / 2,
                chip,
                SnellPalette.text3,
            )
        }

        // Mask overflow below the viewport, then draw the footer divider + action bar.
        val footTop = p.bottom - ServerSelectLayout.FOOTER_H
        canvas.fill(p.left + 1, vp.bottom, p.width - 2, (p.bottom - 1 - vp.bottom).coerceAtLeast(0), SnellPalette.s1)
        canvas.fill(p.left + 1, footTop, p.width - 2, 1, SnellPalette.border)

        val hasSel = selected in rows.indices
        for (c in ServerSelectLayout.buttons(w, h)) {
            val enabled = when (c.id) {
                "join", "edit", "delete" -> hasSel  // need a selected server
                else -> true                         // direct / add / refresh / cancel always live
            }
            SnellUi.button(
                canvas, c.rect, LABELS[c.id] ?: c.id, styleFor(c.id),
                hover = enabled && c.rect.contains(mouseX, mouseY), enabled = enabled,
            )
        }
    }

    private fun styleFor(id: String): SnellBtn = when (id) {
        "join" -> SnellBtn.Primary
        "delete" -> SnellBtn.Danger
        "cancel" -> SnellBtn.Ghost
        else -> SnellBtn.Secondary
    }

    private fun drawRow(canvas: EditorCanvas, r: Rect, row: ServerRow, selected: Boolean, mouseX: Int, mouseY: Int, vp: Rect) {
        val hover = vp.contains(mouseX, mouseY) && r.contains(mouseX, mouseY)
        SnellUi.listRow(canvas, r, selected, hover, under = SnellPalette.s1)

        // Rounded icon-placeholder square with the server's initial in accent.
        val pad = (r.height - 28) / 2
        val ix = r.left + pad
        val iy = r.top + pad
        canvas.fill(ix, iy, 28, 28, SnellPalette.s2)
        canvas.border(ix, iy, 28, 28, SnellPalette.border)
        SnellUi.round(canvas, Rect(ix, iy, 28, 28), SnellPalette.s1)
        val glyph = row.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        canvas.drawText(ix + (28 - canvas.textWidth(glyph)) / 2, iy + (28 - canvas.lineHeight) / 2 + 1, glyph, SnellPalette.accent)

        // Right-aligned status pill (its width matches SnellUi.pill's internal sizing).
        val (pillText, pillRole) = pillFor(row)
        val pillW = canvas.textWidth(pillText) + 16
        val pillH = canvas.lineHeight + 6
        val px = r.right - 8 - pillW
        SnellUi.pill(canvas, px, r.top + (r.height - pillH) / 2, pillText, pillRole)

        // Name + motd, ellipsized to the gap before the pill.
        val tx = ix + 28 + 8
        val textMaxW = (px - 8 - tx).coerceAtLeast(8)
        val blockTop = r.top + (r.height - (canvas.lineHeight * 2 + 3)) / 2
        canvas.drawText(tx, blockTop, SnellUi.ellipsize(canvas, row.name, textMaxW), SnellPalette.text)
        val sub = row.motd.ifEmpty { row.address }
        canvas.drawText(tx, blockTop + canvas.lineHeight + 3, SnellUi.ellipsize(canvas, sub, textMaxW), SnellPalette.text2)
    }

    /** Pill copy + role for a row's reachability. */
    private fun pillFor(row: ServerRow): Pair<String, PillRole> = when (row.status) {
        ServerStatus.Online -> {
            val text = when {
                row.ping < 0 -> row.players.ifEmpty { "Online" }
                row.players.isEmpty() -> "${row.ping}ms"
                else -> "${row.players}  ${row.ping}ms"
            }
            text to PillRole.Online
        }
        ServerStatus.Offline -> "Offline" to PillRole.Offline
        ServerStatus.Pinging -> "Pinging…" to PillRole.Neutral
    }

    private fun emptyState(canvas: EditorCanvas, vp: Rect) {
        val title = "No servers yet"
        val s = 1.4f
        val tw = (canvas.textWidth(title) * s).toInt()
        SnellUi.heading(canvas, vp.left + (vp.width - tw) / 2, vp.top + vp.height / 2 - canvas.lineHeight, title, s, SnellPalette.text2)
        val sub = "Use \"Add Server\" to add one."
        canvas.drawText(vp.left + (vp.width - canvas.textWidth(sub)) / 2, vp.top + vp.height / 2 + canvas.lineHeight, sub, SnellPalette.text3)
    }
}

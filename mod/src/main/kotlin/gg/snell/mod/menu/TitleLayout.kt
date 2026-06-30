package gg.snell.mod.menu

import gg.snell.mod.editor.Control
import gg.snell.mod.editor.Rect

/**
 * Pure layout for the bespoke title screen (design: "Snell In-Game Menus"): a left command column
 * (brand lockup, big Discord/Singleplayer/Multiplayer nav rows, an Options/Quit pair) plus top-right
 * quick actions (wallet/cosmetics/friends) and an account chip, and a bottom-right "What's new" card.
 * No Minecraft types — unit-testable and headlessly renderable.
 */
object TitleLayout {
    /** Big nav rows, top→bottom. `discord` is a placeholder; the rest delegate to vanilla. */
    val NAV_IDS = listOf("discord", "singleplayer", "multiplayer")

    /** The Options/Quit pair below the nav rows. */
    val FOOT_IDS = listOf("options", "quit")

    /** Top-right quick actions (all placeholders for now). */
    val ACTION_IDS = listOf("wallet", "cosmetics", "friends")

    /** Every activatable id (nav + foot), for the screen's delegation map + tests. */
    val IDS = NAV_IDS + FOOT_IDS

    private const val MARGIN = 22
    private const val NAV_H = 30
    private const val FEAT_H = 46 // the featured Discord card is taller (badge + Link CTA)
    private const val NAV_GAP = 7
    private const val FOOT_H = 22

    private fun colW(w: Int) = (w * 0.46f).toInt().coerceIn(200, 280)

    /** Brand lockup region (mark + SNELL wordmark), top-left of the command column. */
    fun logoRect(w: Int, h: Int): Rect = Rect(MARGIN, MARGIN, colW(w), 30)

    /** The Options/Quit row, pinned to the bottom of the command column. */
    fun footRow(w: Int, h: Int): List<Control> {
        val x = MARGIN; val cw = colW(w)
        val y = h - MARGIN - FOOT_H
        val bw = (cw - 8) / 2
        return listOf(
            Control("options", Rect(x, y, bw, FOOT_H)),
            Control("quit", Rect(x + bw + 8, y, cw - bw - 8, FOOT_H)),
        )
    }

    /** The featured Discord card + two big nav rows, stacked just above [footRow]. */
    fun navButtons(w: Int, h: Int): List<Control> {
        val x = MARGIN; val cw = colW(w)
        val totalH = FEAT_H + 2 * NAV_H + 2 * NAV_GAP
        var y = (h - MARGIN - FOOT_H - 12) - totalH
        val out = ArrayList<Control>(3)
        out += Control("discord", Rect(x, y, cw, FEAT_H)); y += FEAT_H + NAV_GAP
        out += Control("singleplayer", Rect(x, y, cw, NAV_H)); y += NAV_H + NAV_GAP
        out += Control("multiplayer", Rect(x, y, cw, NAV_H))
        return out
    }

    /** The Link CTA inside the featured Discord card (drawn + hit-tests inside the `discord` rect). */
    fun linkButton(w: Int, h: Int): Rect {
        val d = navButtons(w, h).first { it.id == "discord" }.rect
        val bw = 44; val bh = 18
        return Rect(d.right - 8 - bw, d.top + (d.height - bh) / 2, bw, bh)
    }

    /** Account chip (non-interactive) at the far top-right. */
    fun accountChip(w: Int, h: Int): Rect {
        val cw = 92; val ch = 26
        return Rect(w - MARGIN - cw, MARGIN - 4, cw, ch)
    }

    /** Top-right quick-action squares (wallet is wider), right-aligned before the account chip. */
    fun topActions(w: Int, h: Int): List<Control> {
        val sq = 18; val gap = 6
        var right = accountChip(w, h).left - gap
        val friends = Rect(right - sq, MARGIN, sq, sq); right = friends.left - gap
        val cosmetics = Rect(right - sq, MARGIN, sq, sq); right = cosmetics.left - gap
        val walletW = 52
        val wallet = Rect(right - walletW, MARGIN, walletW, sq)
        return listOf(Control("wallet", wallet), Control("cosmetics", cosmetics), Control("friends", friends))
    }

    /** Bottom-right "What's new" card (non-interactive). */
    fun whatsNewRect(w: Int, h: Int): Rect {
        val cw = (w * 0.30f).toInt().coerceIn(180, 300); val ch = 52
        return Rect(w - MARGIN - cw, h - MARGIN - ch, cw, ch)
    }

    /** Id of the activatable control under the cursor, or null. */
    fun hit(w: Int, h: Int, mx: Int, my: Int): String? =
        (navButtons(w, h) + footRow(w, h) + topActions(w, h)).firstOrNull { it.rect.contains(mx, my) }?.id
}

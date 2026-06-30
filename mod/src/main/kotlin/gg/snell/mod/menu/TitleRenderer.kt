package gg.snell.mod.menu

import gg.snell.mod.editor.Rect
import gg.snell.mod.platform.EditorCanvas
import gg.snell.mod.ui.SnellBtn
import gg.snell.mod.ui.SnellUi
import gg.snell.shared.SnellPalette

/**
 * Pure renderer for the bespoke title screen (design: "Snell In-Game Menus") — dusk backdrop, the
 * Snell slipstream brand mark + wordmark, a left command column (Discord/Singleplayer/Multiplayer
 * nav rows, Options/Quit), top-right quick actions + account chip, and a bottom-right "What's new"
 * card. Icons are real Tabler glyphs; the logo is the slipstream texture.
 */
object TitleRenderer {
    private const val DEFAULT_WHATS_NEW = "Sodium 0.6 rebuilt for 26.2, a new keystroke HUD, and faster cold-start."

    fun render(
        canvas: EditorCanvas, w: Int, h: Int, mouseX: Int, mouseY: Int,
        modVersion: String = "0.0.0", mcVersion: String = "26.2",
        username: String = "Player", statusLabel: String = "Online", crowns: String = "0",
        singleplayerSub: String = "Create or load a world", multiplayerSub: String = "Join a server",
        whatsNewBody: String = DEFAULT_WHATS_NEW,
    ) {
        // No backdrop() here — SnellMenuScreen.extractBackground paints the live panorama + scrims behind us.
        lockup(canvas, TitleLayout.logoRect(w, h))

        val link = TitleLayout.linkButton(w, h)
        for (c in TitleLayout.navButtons(w, h)) {
            val hover = c.rect.contains(mouseX, mouseY)
            when (c.id) {
                "discord" -> {
                    val hoverLink = link.contains(mouseX, mouseY)
                    val tile = SnellUi.featuredNavButton(canvas, c.rect, "Link your Discord", "Free cosmetics, role perks & party sync", "REWARDS", link, "Link", hover, hoverLink)
                    SnellUi.icon(canvas, "discord", tile.left + tile.width / 2, tile.top + tile.height / 2, tile.width - 6, SnellUi.WHITE)
                }
                "singleplayer" -> {
                    val tile = SnellUi.navButton(canvas, c.rect, SnellPalette.accent, "Singleplayer", singleplayerSub, hover)
                    SnellUi.icon(canvas, "singleplayer", tile.left + tile.width / 2, tile.top + tile.height / 2, tile.width - 5, SnellPalette.accent)
                }
                else -> {
                    val tile = SnellUi.navButton(canvas, c.rect, SnellPalette.accent, "Multiplayer", multiplayerSub, hover)
                    SnellUi.icon(canvas, "multiplayer", tile.left + tile.width / 2, tile.top + tile.height / 2, tile.width - 5, SnellPalette.accent)
                }
            }
        }

        for (c in TitleLayout.footRow(w, h)) {
            val hover = c.rect.contains(mouseX, mouseY)
            val label = if (c.id == "options") "Options" else "Quit Game"
            val ic = if (c.id == "options") "options" else "quit"
            SnellUi.button(canvas, c.rect, label, if (c.id == "quit") SnellBtn.Danger else SnellBtn.Secondary, hover, iconName = ic)
        }

        for (c in TitleLayout.topActions(w, h)) {
            val hover = c.rect.contains(mouseX, mouseY)
            val r = c.rect
            if (c.id == "wallet") {
                SnellUi.walletPill(canvas, r, crowns, hover)
            } else {
                SnellUi.squareButton(canvas, r, hover)
                SnellUi.icon(canvas, c.id, r.left + r.width / 2, r.top + r.height / 2, 12, SnellPalette.text2)
            }
        }
        accountChip(canvas, TitleLayout.accountChip(w, h), username, statusLabel)
        whatsNew(canvas, TitleLayout.whatsNewRect(w, h), modVersion, whatsNewBody)
        footer(canvas, w, h, modVersion, mcVersion)
    }

    /** The Snell slipstream mark beside the crisp downscaled SNELL wordmark, inline left. */
    private fun lockup(canvas: EditorCanvas, r: Rect) {
        val markSize = r.height
        SnellUi.slipstream(canvas, r.left, r.top, markSize)
        val px = (markSize * 0.78f).toInt()
        SnellUi.heading(canvas, r.left + markSize + 12, r.top + (markSize - px) / 2, "SNELL", pixelHeight = px)
    }

    private fun accountChip(canvas: EditorCanvas, r: Rect, username: String, status: String) {
        canvas.fill(r.left, r.top, r.width, r.height, SnellUi.rowFill)
        canvas.border(r.left, r.top, r.width, r.height, SnellUi.rowBorder)
        SnellUi.round(canvas, r, SnellPalette.menuBase)
        val sk = r.height - 6
        val avatar = Rect(r.left + 3, r.top + 3, sk, sk)
        canvas.fill(avatar.left, avatar.top, avatar.width, avatar.height, SnellPalette.menuInset)
        canvas.border(avatar.left, avatar.top, avatar.width, avatar.height, SnellPalette.outline)
        SnellUi.round(canvas, avatar, SnellPalette.menuPanel)
        val initial = username.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        canvas.drawText(avatar.left + (avatar.width - canvas.textWidth(initial)) / 2, avatar.top + (avatar.height - canvas.lineHeight) / 2 + 1, initial, SnellPalette.accent)
        // status badge on the avatar's bottom-right corner (panel ring + status colour)
        val sc = SnellUi.statusColor(status)
        SnellUi.dot(canvas, avatar.right - 2, avatar.bottom - 2, 6, SnellPalette.menuPanel)
        SnellUi.dot(canvas, avatar.right - 2, avatar.bottom - 2, 4, sc)
        val tx = avatar.right + 6
        val maxW = r.right - 6 - tx
        canvas.drawText(tx, r.top + 4, SnellUi.ellipsize(canvas, username, maxW), SnellPalette.text)
        canvas.drawText(tx, r.top + 4 + canvas.lineHeight, SnellUi.ellipsize(canvas, status, maxW), sc)
    }

    private fun whatsNew(canvas: EditorCanvas, r: Rect, version: String, body: String) {
        canvas.fill(r.left, r.top, r.width, r.height, SnellPalette.withAlpha(SnellPalette.gold, 0x12))
        canvas.border(r.left, r.top, r.width, r.height, SnellPalette.withAlpha(SnellPalette.gold, 0x48))
        SnellUi.round(canvas, r, SnellPalette.menuBase)
        SnellUi.dot(canvas, r.left + 10, r.top + 9, 7, SnellPalette.gold)
        canvas.drawText(r.left + 17, r.top + 5, "WHAT'S NEW · $version".uppercase(), SnellPalette.gold)
        val (l1, l2) = wrap2(canvas, body, r.width - 18)
        canvas.drawText(r.left + 9, r.top + 5 + canvas.lineHeight + 4, l1, SnellPalette.text)
        if (l2.isNotEmpty()) canvas.drawText(r.left + 9, r.top + 5 + 2 * canvas.lineHeight + 5, SnellUi.ellipsize(canvas, l2, r.width - 18), SnellPalette.text)
    }

    /** Two-tone version footer, bottom-left: "SNELL {mod}" · "Minecraft {mc} · Fabric" in Geist Mono. */
    private fun footer(canvas: EditorCanvas, w: Int, h: Int, modVersion: String, mcVersion: String) {
        val y = h - 15
        var x = 22
        val head = "SNELL $modVersion"
        canvas.drawMono(x, y, head, SnellPalette.text2)
        x += canvas.monoWidth(head) + 7
        SnellUi.dot(canvas, x, y + canvas.lineHeight / 2, 3, SnellPalette.gold)
        x += 7
        canvas.drawMono(x, y, "Minecraft $mcVersion · Fabric", SnellPalette.menuText3)
    }

    /** Greedy word-wrap into (first line, remainder) for the given pixel width. */
    private fun wrap2(canvas: EditorCanvas, text: String, maxW: Int): Pair<String, String> {
        if (canvas.textWidth(text) <= maxW) return text to ""
        val words = text.split(' '); val sb = StringBuilder(); var i = 0
        while (i < words.size && canvas.textWidth(("$sb ${words[i]}").trim()) <= maxW) {
            if (sb.isNotEmpty()) sb.append(' '); sb.append(words[i]); i++
        }
        return sb.toString() to words.drop(i).joinToString(" ")
    }
}

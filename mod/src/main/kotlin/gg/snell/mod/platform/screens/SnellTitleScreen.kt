package gg.snell.mod.platform.screens

import gg.snell.mod.menu.TitleLayout
import gg.snell.mod.menu.TitleRenderer
import gg.snell.mod.platform.EditorCanvas
import gg.snell.mod.platform.SnellMenuScreen
import gg.snell.mod.platform.SnellMenus
import net.minecraft.client.multiplayer.ServerList
import net.minecraft.network.chat.Component

/**
 * Bespoke main menu. Renders the Snell title (command column + quick actions) and opens the bespoke
 * sub-screens directly (so they return here on back). The Discord / wallet / cosmetics / friends
 * quick actions are styled placeholders for now (no-op until those surfaces are built).
 */
class SnellTitleScreen : SnellMenuScreen(Component.literal("Snell")) {

    private var singleplayerSub = "Create or load a world"
    private var multiplayerSub = "Join a server"
    private var loaded = false // MC re-runs init() on every resize; load the counts only once

    override fun init() {
        super.init()
        if (loaded) return
        loaded = true
        // server count off the render thread (servers.dat is disk I/O + NBT parse), posted back on the client thread
        Thread({
            val n = runCatching { ServerList(mc).apply { load() }.size() }.getOrDefault(0)
            mc.execute { multiplayerSub = if (n == 0) "No servers yet" else "$n ${if (n == 1) "server" else "servers"}" }
        }, "snell-serverlist").apply { isDaemon = true; start() }
        WorldAdapter.summary(mc) { n, ago ->
            singleplayerSub = when {
                n == 0 -> "No worlds yet"
                ago.isEmpty() -> "$n ${if (n == 1) "world" else "worlds"}"
                else -> "$n ${if (n == 1) "world" else "worlds"} · last played $ago"
            }
        }
    }

    override fun draw(canvas: EditorCanvas, mouseX: Int, mouseY: Int) =
        TitleRenderer.render(
            canvas, width, height, mouseX, mouseY,
            modVersion = SnellMenus.modVersion, mcVersion = SnellMenus.mcVersion,
            username = mc.user.name, statusLabel = "Online", crowns = "0",
            singleplayerSub = singleplayerSub, multiplayerSub = multiplayerSub,
        )

    override fun hitId(mouseX: Int, mouseY: Int): String? = TitleLayout.hit(width, height, mouseX, mouseY)

    override fun onActivate(id: String) {
        when (id) {
            "singleplayer" -> mc.setScreenAndShow(SnellWorldSelectScreen(this))
            "multiplayer" -> mc.setScreenAndShow(SnellServerSelectScreen(this))
            "options" -> mc.setScreenAndShow(SnellOptionsScreen(this))
            "quit" -> mc.stop()
            // discord / wallet / cosmetics / friends — placeholders (no-op for now)
        }
    }

    override fun isPauseScreen(): Boolean = false
}

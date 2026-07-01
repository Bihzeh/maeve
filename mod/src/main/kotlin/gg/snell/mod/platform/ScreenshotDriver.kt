package gg.snell.mod.platform

import gg.snell.mod.menu.ServerRow
import gg.snell.mod.menu.ServerStatus
import gg.snell.mod.menu.WorldRow
import gg.snell.mod.platform.screens.SnellOptionsScreen
import gg.snell.mod.platform.screens.SnellPauseScreen
import gg.snell.mod.platform.screens.SnellServerSelectScreen
import gg.snell.mod.platform.screens.SnellTitleScreen
import gg.snell.mod.platform.screens.SnellWorldSelectScreen
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.Screenshot
import net.minecraft.client.gui.screens.Screen
import java.nio.file.Files
import java.nio.file.Path

/**
 * Headless screenshot harness (enabled by `-Dsnell.shotmode`). Once the client has finished loading it
 * opens each bespoke Snell menu in turn, lets it render a few frames, grabs the real framebuffer to a
 * PNG under `-Dsnell.shotdir`, then quits the client. Run via `./gradlew :mod:runScreenshots` (locally,
 * or under `xvfb` in CI) to get REAL in-game renders — the authoritative verification the menus lacked,
 * which is why earlier fixes passed the divergent AWT preview yet failed in-game.
 *
 * All MC symbols verified against the 26.2 jar: `Screenshot.takeScreenshot(RenderTarget, Consumer)`,
 * `GameRenderer.mainRenderTarget()`, `Minecraft.isGameLoadFinished()`, `NativeImage.writeToFile(Path)`.
 */
object ScreenshotDriver {

    // Title/World/Server/Options render fine with no world loaded (adapters load async, draw empty then
    // fill; in shot mode the pickers show [ShotSeed] demo rows so the lists are captured populated).
    // Pause renders standalone here (painted dusk backdrop, not the blurred world) — a faithful
    // world-backed pause shot is a later phase.
    private val shots: List<Pair<String, () -> Screen>> = listOf(
        "title" to { SnellTitleScreen() },
        "world" to { SnellWorldSelectScreen(null) },
        "server" to { SnellServerSelectScreen(null) },
        "options" to { SnellOptionsScreen(null) },
        "options-controls" to { SnellOptionsScreen(null, initialCategory = "controls") },
        "options-audio" to { SnellOptionsScreen(null, initialCategory = "audio") },
        "options-mods" to { SnellOptionsScreen(null, initialCategory = "mods") },
        "pause" to { SnellPauseScreen() },
    )

    fun install(outDir: Path) {
        Files.createDirectories(outDir)
        SnellMenus.shotSeed = true
        var warmup = 60        // ~3s after load: let the always-on Geist pack + atlases settle
        var idx = -1
        var waitFrames = 0
        var draining = 0
        ClientTickEvents.END_CLIENT_TICK.register(
            ClientTickEvents.EndTick { mc ->
                if (!mc.isGameLoadFinished) return@EndTick
                if (warmup > 0) { warmup--; return@EndTick }
                SnellMenus.enabled = true
                if (waitFrames > 0) {
                    waitFrames--
                    if (waitFrames == 0) capture(mc, outDir, shots[idx].first)
                    return@EndTick
                }
                if (idx >= 0 && draining < 10) { draining++; return@EndTick } // flush the async PNG encode
                draining = 0
                idx++
                if (idx >= shots.size) { mc.execute { mc.stop() }; return@EndTick }
                mc.setScreenAndShow(shots[idx].second())
                waitFrames = 8 // render several frames before grabbing
            },
        )
    }

    /** Demo rows the pickers show under [SnellMenus.shotSeed], covering selected/hover-less states,
     *  every mode pill colour, and every server status (online w/ ping tiers, offline, pinging). */
    object ShotSeed {
        val worlds = listOf(
            WorldRow("New World", "new-world", "Survival", "26.2 · 2 minutes ago", "2.1 GB"),
            WorldRow("Hardcore Attempt 4", "hardcore-4", "Hardcore", "26.2 · yesterday", "880 MB"),
            WorldRow("Creative Flat", "flat-1", "Creative", "26.2 · 3 days ago", "120 MB"),
            WorldRow("SkyBlock with an Extremely Long World Name That Ellipsizes", "sb", "Survival", "25.4 · last week", "640 MB"),
            WorldRow("Old Base", "old", "Spectator", "24.6 · 2 months ago", "1.4 GB"),
        )
        val servers = listOf(
            ServerRow("Hypixel", "mc.hypixel.net", "Bedwars · SkyBlock · 30+ minigames", "84231/100000", 23, ServerStatus.Online),
            ServerRow("CubeCraft", "play.cubecraft.net", "Lucky Islands · EggWars", "12044/40000", 95, ServerStatus.Online),
            ServerRow("My SMP", "smp.example.net", "Private survival realm", "3/20", 210, ServerStatus.Online),
            ServerRow("Old Server", "dead.example.net", "Can't connect to server", "", -1, ServerStatus.Offline),
            ServerRow("Resolving", "new.example.net", "", "", -1, ServerStatus.Pinging),
        )
    }

    private fun capture(mc: Minecraft, dir: Path, name: String) {
        // END_CLIENT_TICK runs on the client (= render) thread, so the GPU readback is valid here.
        runCatching {
            Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget()) { img ->
                runCatching { img.writeToFile(dir.resolve("snell-$name.png")) }
                img.close()
            }
        }
    }
}

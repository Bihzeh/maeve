package gg.snell.mod.platform.screens

import gg.snell.mod.menu.WorldRow
import gg.snell.mod.menu.WorldState
import gg.snell.mod.menu.WorldView
import gg.snell.mod.platform.EditorCanvas
import gg.snell.mod.platform.ScreenshotDriver
import gg.snell.mod.platform.SnellMenuScreen
import gg.snell.mod.platform.SnellMenus
import gg.snell.mod.ui.node.Layout
import gg.snell.mod.ui.node.Node
import gg.snell.mod.ui.node.asMetrics
import gg.snell.mod.ui.node.hit
import gg.snell.mod.ui.node.render
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.network.chat.Component

/**
 * Bespoke singleplayer world picker. Builds the [WorldView] node tree per frame (810-tall design
 * space) and delegates every action to vanilla via [WorldAdapter] (load summaries / play / create /
 * edit / delete). Single-click selects a row, double-click or "Play" loads it.
 */
class SnellWorldSelectScreen(private val parent: Screen?) : SnellMenuScreen(Component.literal("Singleplayer")) {
    private var rows: List<WorldRow> = emptyList()
    private var selected = -1
    private var scrollY = 0
    private var loaded = false
    private var laid: Node? = null

    override val designH: Int get() = 810

    override fun init() {
        super.init()
        if (!loaded) {
            loaded = true
            if (SnellMenus.shotSeed) { // screenshot harness: capture a populated, selected list
                rows = ScreenshotDriver.ShotSeed.worlds
                selected = 0
            } else {
                WorldAdapter.load(mc) { rows = it; clampScroll() }
            }
        }
    }

    override fun draw(canvas: EditorCanvas, mouseX: Int, mouseY: Int) {
        val t = WorldView.build(WorldState(rows, selected, scrollY))
        Layout.layout(t, designW, designH, canvas.asMetrics())
        t.render(canvas, mouseX, mouseY)
        laid = t
    }

    // Rows go through onPress (select vs play); everything else is a plain activate id.
    override fun hitId(mouseX: Int, mouseY: Int): String? =
        laid?.hit(mouseX, mouseY)?.takeUnless { it.startsWith("row:") || it == "list" || it == "search" }

    override fun onPress(mouseX: Int, mouseY: Int, doubled: Boolean): Boolean {
        val id = laid?.hit(mouseX, mouseY) ?: return false
        if (!id.startsWith("row:")) return false
        selected = id.removePrefix("row:").toIntOrNull() ?: return false
        if (doubled) playSelected()
        return true
    }

    override fun onActivate(id: String) {
        when (id) {
            "back", "cancel" -> onClose()
            "play" -> playSelected()
            "create" -> WorldAdapter.create(mc, this)
            "edit" -> rows.getOrNull(selected)?.let { WorldAdapter.edit(mc, it.folder, this) }
            "delete" -> rows.getOrNull(selected)?.let { WorldAdapter.delete(mc, it.folder, it.name, this) }
        }
    }

    override fun onScroll(amount: Double) {
        scrollY -= (amount * 24).toInt()
        clampScroll()
    }

    private fun clampScroll() {
        scrollY = scrollY.coerceIn(0, laid?.let { WorldView.maxScroll(it, rows.size) } ?: 0)
    }

    private fun playSelected() {
        rows.getOrNull(selected)?.let { WorldAdapter.play(mc, it.folder) { mc.setScreenAndShow(this) } }
    }

    override fun onClose() {
        mc.setScreenAndShow(parent ?: TitleScreen())
    }

    override fun isPauseScreen(): Boolean = false
}

package gg.snell.mod.platform.screens

import gg.snell.mod.menu.ServerRow
import gg.snell.mod.menu.ServerState
import gg.snell.mod.menu.ServerView
import gg.snell.mod.platform.EditorCanvas
import gg.snell.mod.platform.SnellMenuScreen
import gg.snell.mod.platform.SnellMenus
import gg.snell.mod.ui.node.Layout
import gg.snell.mod.ui.node.Node
import gg.snell.mod.ui.node.asMetrics
import gg.snell.mod.ui.node.hit
import gg.snell.mod.ui.node.render
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen
import net.minecraft.network.chat.Component

/**
 * Bespoke multiplayer server picker. Builds the [ServerView] node tree per frame (810-tall design
 * space) and delegates to vanilla via [ServerAdapter] (list + live ping + join). Add / Edit / Direct
 * Connect hand off to vanilla's JoinMultiplayerScreen via a one-shot swap bypass.
 */
class SnellServerSelectScreen(private val parent: Screen?) : SnellMenuScreen(Component.literal("Multiplayer")) {
    private val adapter = ServerAdapter(mc)
    private var rows: List<ServerRow> = emptyList()
    private var selected = -1
    private var scrollY = 0
    private var started = false
    private var laid: Node? = null

    override val designH: Int get() = 810

    override fun init() {
        super.init()
        if (!started) { started = true; adapter.pingAll() }
        rows = adapter.rows()
    }

    override fun tick() {
        adapter.tick()
        rows = adapter.rows()
    }

    override fun draw(canvas: EditorCanvas, mouseX: Int, mouseY: Int) {
        val t = ServerView.build(ServerState(rows, selected, scrollY))
        Layout.layout(t, designW, designH, canvas.asMetrics())
        t.render(canvas, mouseX, mouseY)
        laid = t
    }

    override fun hitId(mouseX: Int, mouseY: Int): String? =
        laid?.hit(mouseX, mouseY)?.takeUnless { it.startsWith("row:") || it == "list" }

    override fun onPress(mouseX: Int, mouseY: Int, doubled: Boolean): Boolean {
        val id = laid?.hit(mouseX, mouseY) ?: return false
        if (!id.startsWith("row:")) return false
        selected = id.removePrefix("row:").toIntOrNull() ?: return false
        if (doubled) joinSelected()
        return true
    }

    override fun onActivate(id: String) {
        when (id) {
            "back", "cancel" -> onClose()
            "refresh" -> { adapter.pingAll(); rows = adapter.rows() }
            "join" -> joinSelected()
            "add", "direct" -> openVanillaList()
        }
    }

    override fun onScroll(amount: Double) {
        scrollY = (scrollY - (amount * 24).toInt())
            .coerceIn(0, laid?.let { ServerView.maxScroll(it, rows.size) } ?: 0)
    }

    private fun joinSelected() {
        if (selected in rows.indices) {
            adapter.dispose()
            adapter.join(this, selected)
        }
    }

    /** Hand Add / Edit / Direct Connect to vanilla's list (no bespoke input dialogs in 26.1.2). */
    private fun openVanillaList() {
        SnellMenus.bypassNext = true
        mc.setScreenAndShow(JoinMultiplayerScreen(parent ?: this))
    }

    override fun onClose() {
        adapter.dispose()
        mc.setScreenAndShow(parent ?: TitleScreen())
    }

    override fun isPauseScreen(): Boolean = false
}

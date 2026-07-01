package gg.snell.mod.platform.screens

import gg.snell.mod.editor.Rect
import gg.snell.mod.menu.OptionEntry
import gg.snell.mod.menu.OptionItem
import gg.snell.mod.menu.OptionKind
import gg.snell.mod.menu.OptionsState
import gg.snell.mod.menu.OptionsView
import gg.snell.mod.module.ModuleManager
import gg.snell.mod.platform.EditorCanvas
import gg.snell.mod.platform.SnellMenuScreen
import gg.snell.mod.platform.SnellMenus
import gg.snell.mod.ui.node.Layout
import gg.snell.mod.ui.node.Node
import gg.snell.mod.ui.node.asMetrics
import gg.snell.mod.ui.node.find
import gg.snell.mod.ui.node.hit
import gg.snell.mod.ui.node.render
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.network.chat.Component

/**
 * Bespoke Options screen — full native: every control reads/writes the live game
 * [net.minecraft.client.Options] through [OptionsAdapter] (Video / Controls / Audio) and the Mods tab
 * toggles real modules via [ModuleManager]. Builds the [OptionsView] node tree per frame (810-tall
 * design space); entries are rebuilt only on category switch + after a mutation (not per frame).
 */
class SnellOptionsScreen(
    private val parent: Screen?,
    private val modules: ModuleManager? = SnellMenus.modules,
    initialCategory: String = "video",
) : SnellMenuScreen(Component.literal("Options")) {

    private var category = initialCategory
    private var entries: List<OptionEntry> = emptyList()
    private var scrollY = 0
    private var sliderDrag: String? = null
    private var laid: Node? = null

    override val designH: Int get() = 810

    override fun init() {
        super.init()
        rebuild()
    }

    private fun rebuild() {
        entries = OptionsAdapter.entries(mc.options, modules, category)
        clampScroll()
    }

    private fun clampScroll() {
        scrollY = scrollY.coerceIn(0, laid?.let { OptionsView.maxScroll(it, entries.size) } ?: 0)
    }

    override fun draw(canvas: EditorCanvas, mouseX: Int, mouseY: Int) {
        val t = OptionsView.build(OptionsState(entries, category, scrollY))
        Layout.layout(t, designW, designH, canvas.asMetrics())
        t.render(canvas, mouseX, mouseY)
        laid = t
    }

    // Rows/controls go through onPress; back / done / categories are plain activate ids.
    override fun hitId(mouseX: Int, mouseY: Int): String? =
        laid?.hit(mouseX, mouseY)?.takeUnless { it.startsWith("row:") || it.startsWith("ctrl:") || it == "list" }

    override fun onActivate(id: String) {
        when {
            id == "back" || id == "done" -> onClose()
            id in OptionsView.CATEGORIES -> { category = id; scrollY = 0; rebuild() }
        }
    }

    override fun onPress(mouseX: Int, mouseY: Int, doubled: Boolean): Boolean {
        val id = laid?.hit(mouseX, mouseY) ?: return false
        val item = itemFor(id) ?: return false
        when (item.kind) {
            OptionKind.Toggle -> { OptionsAdapter.toggle(mc.options, modules, item.id); rebuild() }
            OptionKind.Cycle -> { OptionsAdapter.cycle(mc.options, item.id); rebuild() }
            OptionKind.Slider -> {
                sliderDrag = item.id
                ctrlRect(item.id)?.let { applySlider(item.id, mouseX, it) }
                rebuild()
            }
        }
        return true
    }

    override fun onDragTo(mouseX: Int, mouseY: Int) {
        val id = sliderDrag ?: return
        applySlider(id, mouseX, ctrlRect(id) ?: return)
        rebuild()
    }

    override fun onReleaseDrag() { sliderDrag = null }

    override fun onScroll(amount: Double) {
        scrollY -= (amount * 24).toInt()
        clampScroll()
    }

    /** Resolve a hit id (`row:<i>` or `ctrl:<optionId>`) to its option item. */
    private fun itemFor(id: String): OptionItem? = when {
        id.startsWith("ctrl:") -> {
            val oid = id.removePrefix("ctrl:")
            entries.firstNotNullOfOrNull { (it as? OptionEntry.Item)?.item?.takeIf { i -> i.id == oid } }
        }
        id.startsWith("row:") -> {
            val i = id.removePrefix("row:").toIntOrNull()
            (i?.let { entries.getOrNull(it) } as? OptionEntry.Item)?.item
        }
        else -> null
    }

    private fun ctrlRect(id: String): Rect? = laid?.find("ctrl:$id")?.rect

    private fun applySlider(id: String, mouseX: Int, ctrl: Rect) {
        val f = ((mouseX - ctrl.left).toFloat() / ctrl.width.coerceAtLeast(1)).coerceIn(0f, 1f)
        OptionsAdapter.setSlider(mc.options, id, f)
    }

    override fun onClose() {
        mc.options.save()
        mc.setScreenAndShow(parent ?: TitleScreen())
    }

    override fun isPauseScreen(): Boolean = false
}

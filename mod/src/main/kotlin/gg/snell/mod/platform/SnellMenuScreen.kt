package gg.snell.mod.platform

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

/**
 * Base for bespoke Snell menu screens. Adapts Minecraft 26.x input/render to the mod's pure
 * [EditorCanvas] renderers (same seam as [SnellHudEditorScreen]): [draw] paints the whole screen
 * through the launcher-matched SnellUi kit, [hitId] maps a cursor point to a control id via the
 * screen's pure `*Layout`, and [onActivate] performs the (vanilla) action for a clicked id.
 *
 * The screens add no vanilla widgets and paint an opaque/scrim background over the default one,
 * so the bespoke chrome fully covers it.
 */
abstract class SnellMenuScreen(title: Component) : Screen(title) {

    protected val mc: Minecraft get() = Minecraft.getInstance()

    protected abstract fun draw(canvas: EditorCanvas, mouseX: Int, mouseY: Int)
    protected abstract fun hitId(mouseX: Int, mouseY: Int): String?
    protected abstract fun onActivate(id: String)

    override fun extractRenderState(extractor: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        super.extractRenderState(extractor, mouseX, mouseY, deltaTicks)
        draw(EditorExtractorCanvas(extractor, mc.font), mouseX, mouseY)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        val id = hitId(event.x().toInt(), event.y().toInt())
        if (id != null) {
            onActivate(id)
            return true
        }
        return super.mouseClicked(event, doubled)
    }
}

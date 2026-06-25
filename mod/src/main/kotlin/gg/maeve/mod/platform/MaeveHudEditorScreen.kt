package gg.maeve.mod.platform

import gg.maeve.mod.editor.EditorRenderer
import gg.maeve.mod.editor.EditorState
import gg.maeve.mod.editor.ElementBox
import gg.maeve.mod.editor.ElementLayout
import gg.maeve.mod.editor.TextMeasurer
import gg.maeve.mod.module.ModuleManager
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

/**
 * Drag-and-customize HUD editor. Renders a live preview of every HUD element (so even
 * hidden/out-of-world ones are positionable), lets the user drag elements to re-anchor them,
 * and shows a per-element style panel. All interaction logic is in the pure [EditorState];
 * this screen only adapts Minecraft 26.2 input/render to it. Persists once on close.
 */
class MaeveHudEditorScreen(
    private val modules: ModuleManager,
    private val sampleCtx: () -> GameContext,
) : Screen(Component.literal("Maeve HUD Editor")) {

    private val state = EditorState()
    private val renderer = EditorRenderer()

    private fun boxes(): List<ElementBox> {
        val font = Minecraft.getInstance().font
        val measurer = object : TextMeasurer {
            override fun width(text: String) = font.width(text)
            override val lineHeight = font.lineHeight
        }
        return ElementLayout.boxesFor(modules.hudModules(), sampleCtx(), measurer, width, height)
    }

    override fun extractRenderState(extractor: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        super.extractRenderState(extractor, mouseX, mouseY, deltaTicks)
        val canvas = EditorExtractorCanvas(extractor, Minecraft.getInstance().font)
        renderer.render(canvas, width, height, mouseX, mouseY, sampleCtx(), modules, state)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        val handled = state.onPress(event.x().toInt(), event.y().toInt(), width, height, boxes(), modules)
        if (state.closeRequested) { onClose(); return true }
        return handled || super.mouseClicked(event, doubled)
    }

    override fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean =
        state.onDrag(event.x().toInt(), event.y().toInt(), width, height, modules) || super.mouseDragged(event, dragX, dragY)

    override fun mouseReleased(event: MouseButtonEvent): Boolean =
        state.onRelease() || super.mouseReleased(event)

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true }
        if (event.key() == GLFW.GLFW_KEY_BACKSPACE && state.onBackspace(modules)) return true
        return super.keyPressed(event)
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        if (event.codepoint() > 0xFFFF) return super.charTyped(event) // hex digits are all BMP
        return state.onCharTyped(event.codepoint().toChar(), modules) || super.charTyped(event)
    }

    override fun onClose() {
        modules.saveAll()
        super.onClose()
    }

    override fun isPauseScreen(): Boolean = false
}

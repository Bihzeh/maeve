package gg.maeve.mod.platform

import gg.maeve.mod.ui.ModMenuController
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

/**
 * In-game mod menu: keyboard-driven (Up/Down select, Enter toggle/open, Esc close). The last
 * row opens the HUD editor. Rendered with the 26.2 retained-mode extractor.
 */
class MaeveMenuScreen(
    private val controller: ModMenuController,
    private val onOpenEditor: () -> Unit,
) : Screen(Component.literal("Maeve")) {

    private var selected = 0

    override fun extractRenderState(extractor: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        super.extractRenderState(extractor, mouseX, mouseY, deltaTicks)
        val font = Minecraft.getInstance().font
        val rows = controller.rows()
        val total = rows.size + 1
        if (selected >= total) selected = total - 1

        extractor.text(font, "Maeve", 20, 16, WHITE, true)
        rows.forEachIndexed { i, row ->
            val cursor = if (i == selected) "> " else "  "
            val state = if (row.enabled) "ON" else "OFF"
            val color = if (row.enabled) GREEN else GREY
            extractor.text(font, "$cursor${row.name}: $state", 20, 40 + i * 12, color, true)
        }
        val editorRow = rows.size
        val cursor = if (selected == editorRow) "> " else "  "
        extractor.text(font, "${cursor}Edit HUD...", 20, 40 + editorRow * 12, GOLD, true)
        extractor.text(font, "Up/Down select  -  Enter  -  Esc close", 20, 40 + total * 12 + 10, DARK_GREY, true)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        val rows = controller.rows()
        val total = rows.size + 1
        when (event.key()) {
            GLFW.GLFW_KEY_UP -> { selected = (selected - 1 + total) % total; return true }
            GLFW.GLFW_KEY_DOWN -> { selected = (selected + 1) % total; return true }
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (selected < rows.size) rows.getOrNull(selected)?.let { controller.onToggle(it.id) } else onOpenEditor()
                return true
            }
        }
        return super.keyPressed(event)
    }

    override fun isPauseScreen(): Boolean = false

    private companion object {
        const val WHITE = 0xFFFFFFFF.toInt()
        const val GREEN = 0xFF55FF55.toInt()
        const val GREY = 0xFFAAAAAA.toInt()
        const val GOLD = 0xFFE2B45C.toInt()
        const val DARK_GREY = 0xFF808080.toInt()
    }
}

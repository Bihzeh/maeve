package gg.snell.mod.platform.screens

import gg.snell.mod.menu.PauseLayout
import gg.snell.mod.menu.PauseRenderer
import gg.snell.mod.platform.EditorCanvas
import gg.snell.mod.platform.SnellMenuScreen
import net.minecraft.client.gui.screens.achievement.StatsScreen
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen
import net.minecraft.client.gui.screens.options.OptionsScreen
import net.minecraft.network.chat.Component

/**
 * Bespoke pause menu. Renders the Snell pause column over a scrim and delegates each action to the
 * same vanilla call the original PauseScreen makes (resume / options / advancements / statistics /
 * save & quit), so no in-game flow is reimplemented. Esc resumes.
 */
class SnellPauseScreen : SnellMenuScreen(Component.literal("Paused")) {

    override fun draw(canvas: EditorCanvas, mouseX: Int, mouseY: Int) =
        PauseRenderer.render(canvas, width, height, mouseX, mouseY)

    override fun hitId(mouseX: Int, mouseY: Int): String? = PauseLayout.hit(width, height, mouseX, mouseY)

    override fun onActivate(id: String) {
        when (id) {
            "resume" -> onClose()
            "options" -> mc.setScreenAndShow(OptionsScreen(this, mc.options, false))
            "advancements" -> mc.connection?.let { mc.setScreenAndShow(AdvancementsScreen(it.advancements, this)) }
            "statistics" -> mc.player?.let { mc.setScreenAndShow(StatsScreen(this, it.stats)) }
            "disconnect" -> if (mc.hasSingleplayerServer()) mc.disconnectWithSavingScreen() else mc.disconnectWithProgressScreen()
        }
    }

    override fun onClose() {
        super.onClose() // returns to the world (setScreen(null))
        mc.mouseHandler.grabMouse()
    }

    override fun isPauseScreen(): Boolean = true
}

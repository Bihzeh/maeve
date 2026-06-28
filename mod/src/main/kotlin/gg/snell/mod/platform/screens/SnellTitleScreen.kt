package gg.snell.mod.platform.screens

import gg.snell.mod.menu.TitleLayout
import gg.snell.mod.menu.TitleRenderer
import gg.snell.mod.platform.EditorCanvas
import gg.snell.mod.platform.SnellMenuScreen
import gg.snell.mod.platform.SnellMenus
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen
import net.minecraft.client.gui.screens.options.OptionsScreen
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen
import net.minecraft.network.chat.Component

/**
 * Bespoke main menu. Renders the Snell title (slipstream lockup + button column) and delegates each
 * action to the same vanilla call the original TitleScreen makes — the sub-screens it opens are in
 * turn re-skinned by the screen-swap mixin once those bespoke screens land.
 */
class SnellTitleScreen : SnellMenuScreen(Component.literal("Snell")) {

    override fun draw(canvas: EditorCanvas, mouseX: Int, mouseY: Int) =
        TitleRenderer.render(canvas, width, height, mouseX, mouseY, SnellMenus.VERSION)

    override fun hitId(mouseX: Int, mouseY: Int): String? = TitleLayout.hit(width, height, mouseX, mouseY)

    override fun onActivate(id: String) {
        when (id) {
            "singleplayer" -> mc.setScreenAndShow(SelectWorldScreen(this))
            "multiplayer" -> mc.setScreenAndShow(JoinMultiplayerScreen(this))
            "options" -> mc.setScreenAndShow(OptionsScreen(this, mc.options, false))
            "quit" -> mc.stop()
        }
    }

    override fun isPauseScreen(): Boolean = false
}

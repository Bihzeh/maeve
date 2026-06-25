package gg.maeve.mod.module.hud

import gg.maeve.mod.module.HudAnchor
import gg.maeve.mod.module.HudLine
import gg.maeve.mod.module.HudModule
import gg.maeve.mod.module.HudStyle
import gg.maeve.mod.platform.GameContext

/** Simple WASD keystroke display. Pressed keys show as [W]; the color follows the module
 *  style, so it themes and recolors with the rest of the HUD. A graphical key grid is later. */
class KeystrokesModule : HudModule {
    override val id = "keystrokes"
    override val displayName = "Keystrokes"
    override var enabled = false   // off by default; opt-in
    override var anchor = HudAnchor.TOP_LEFT
    override var offsetX = 4
    override var offsetY = 40
    override var style = HudStyle()

    override fun render(ctx: GameContext): List<HudLine> {
        fun mark(down: Boolean, c: String) = if (down) "[$c]" else " $c "
        return listOf(
            HudLine("  ${mark(ctx.keyForward, "W")}  "),
            HudLine("${mark(ctx.keyLeft, "A")}${mark(ctx.keyBack, "S")}${mark(ctx.keyRight, "D")}"),
        )
    }
}

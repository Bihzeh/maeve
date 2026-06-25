package gg.maeve.mod.module.hud

import gg.maeve.mod.module.HudAnchor
import gg.maeve.mod.module.HudLine
import gg.maeve.mod.module.HudModule
import gg.maeve.mod.module.HudStyle
import gg.maeve.mod.platform.GameContext

/** Simple WASD keystroke display. A graphical key grid lands in a later phase. */
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
        val on = 0xFFFFFFFF.toInt() // explicit per-line color overrides the module style color
        return listOf(
            HudLine("  ${mark(ctx.keyForward, "W")}  ", on),
            HudLine("${mark(ctx.keyLeft, "A")}${mark(ctx.keyBack, "S")}${mark(ctx.keyRight, "D")}", on),
        )
    }
}

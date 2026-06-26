package gg.maeve.mod.module.hud

import gg.maeve.mod.module.HudAnchor
import gg.maeve.mod.module.HudLine
import gg.maeve.mod.module.HudModule
import gg.maeve.mod.module.ModuleOptions
import gg.maeve.mod.module.ModuleToggle
import gg.maeve.mod.module.HudStyle
import gg.maeve.mod.module.TextAlign
import gg.maeve.mod.platform.GameContext

/** WASD keystroke display. Centre-aligned so the W sits over the S regardless of font widths
 *  (the renderer centres each line within the block). Colour follows the module style. */
class KeystrokesModule : HudModule {
    override val id = "keystrokes"
    override val displayName = "Keystrokes"
    override var enabled = false
    override var anchor = HudAnchor.TOP_LEFT
    override var offsetX = 4
    override var offsetY = 40
    override val defaultStyle = HudStyle(align = TextAlign.CENTER)
    override var style = HudStyle(align = TextAlign.CENTER)

    private val opts = ModuleOptions(listOf(ModuleToggle("space", "Spacebar", true)))
    override val toggles get() = opts.toggles
    override fun option(key: String) = opts.get(key)
    override fun setOption(key: String, value: Boolean) = opts.set(key, value)

    override fun render(ctx: GameContext): List<HudLine> {
        fun mark(down: Boolean, c: String) = if (down) "[$c]" else " $c "
        val lines = mutableListOf(
            HudLine(mark(ctx.keyForward, "W")),
            HudLine("${mark(ctx.keyLeft, "A")}${mark(ctx.keyBack, "S")}${mark(ctx.keyRight, "D")}"),
        )
        if (opts.get("space")) lines += HudLine(if (ctx.keyJump) "[_______]" else " _______ ") // spacebar under A-D
        return lines
    }
}

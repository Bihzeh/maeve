package gg.maeve.mod.module.hud

import gg.maeve.mod.module.HudAnchor
import gg.maeve.mod.module.HudLine
import gg.maeve.mod.module.HudModule
import gg.maeve.mod.module.ModuleOptions
import gg.maeve.mod.module.ModuleToggle
import gg.maeve.mod.module.HudStyle
import gg.maeve.mod.platform.GameContext

/** Clicks per second (left | right), measured from the player's own input — server-legal. */
class CpsModule : HudModule {
    override val id = "cps"
    override val displayName = "CPS"
    override var enabled = false
    override var anchor = HudAnchor.TOP_RIGHT
    override var offsetX = 4
    override var offsetY = 52
    override var style = HudStyle()

    private val opts = ModuleOptions(listOf(ModuleToggle("right", "Show right click", true)))
    override val toggles get() = opts.toggles
    override fun option(key: String) = opts.get(key)
    override fun setOption(key: String, value: Boolean) = opts.set(key, value)

    override fun render(ctx: GameContext): List<HudLine> =
        listOf(HudLine("CPS: ${ctx.leftCps}" + if (opts.get("right")) " | ${ctx.rightCps}" else ""))
}

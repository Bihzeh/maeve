package gg.maeve.mod.module.hud

import gg.maeve.mod.module.HudAnchor
import gg.maeve.mod.module.HudLine
import gg.maeve.mod.module.HudModule
import gg.maeve.mod.module.HudStyle
import gg.maeve.mod.platform.GameContext
import gg.maeve.shared.MaevePalette

class FpsModule : HudModule {
    override val id = "fps"
    override val displayName = "FPS"
    override var enabled = true
    override var anchor = HudAnchor.TOP_LEFT
    override var offsetX = 4
    override var offsetY = 4
    // Inlined (not `= defaultStyle`) so it doesn't rely on property declaration order / virtual dispatch.
    override val defaultStyle = HudStyle(color = MaevePalette.gold)
    override var style = HudStyle(color = MaevePalette.gold)

    override fun render(ctx: GameContext): List<HudLine> =
        listOf(HudLine("${ctx.fps} FPS"))
}

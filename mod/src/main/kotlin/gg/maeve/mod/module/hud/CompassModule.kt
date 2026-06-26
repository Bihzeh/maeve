package gg.maeve.mod.module.hud

import gg.maeve.mod.module.HudAnchor
import gg.maeve.mod.module.HudLine
import gg.maeve.mod.module.HudModule
import gg.maeve.mod.module.HudStyle
import gg.maeve.mod.module.TextAlign
import gg.maeve.mod.platform.GameContext

/** A direction tape (compass) centred on the player's facing, server-legal (own yaw only).
 *  Sits at the top-centre by default. Centre-aligned so the caret stays under the tape centre. */
class CompassModule : HudModule {
    override val id = "compass"
    override val displayName = "Compass"
    override var enabled = true // on by default, at the top of the screen
    override var anchor = HudAnchor.TOP_CENTER
    override var offsetX = 0
    override var offsetY = 4
    override val defaultStyle = HudStyle(align = TextAlign.CENTER)
    override var style = HudStyle(align = TextAlign.CENTER)

    override fun render(ctx: GameContext): List<HudLine> =
        if (!ctx.inWorld) emptyList() else HudFormat.compass(ctx.yaw).map { HudLine(it) }
}

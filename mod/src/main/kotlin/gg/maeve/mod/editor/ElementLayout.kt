package gg.maeve.mod.editor

import gg.maeve.mod.module.HudModule
import gg.maeve.mod.platform.GameContext
import gg.maeve.mod.render.HudLayout
import kotlin.math.ceil

/** Text metrics the editor needs to size elements (HudCanvas already satisfies this). */
interface TextMeasurer {
    fun width(text: String): Int
    val lineHeight: Int
}

/** Computes on-screen bounds for HUD elements, matching the renderer's footprint math,
 *  so editor drag handles line up exactly with drawn pixels. Pure. */
object ElementLayout {
    fun boxesFor(
        modules: List<HudModule>, ctx: GameContext, m: TextMeasurer, screenW: Int, screenH: Int,
    ): List<ElementBox> = modules.mapNotNull { module ->
        val lines = module.render(ctx)
        if (lines.isEmpty()) return@mapNotNull null
        val st = module.style
        val textW = lines.maxOf { m.width(it.text) }
        val textH = lines.size * m.lineHeight
        val footW = ceil((textW + st.padding * 2) * st.scale).toInt()
        val footH = ceil((textH + st.padding * 2) * st.scale).toInt()
        val (left, top) = HudLayout.resolveTopLeft(module.anchor, module.offsetX, module.offsetY, footW, footH, screenW, screenH)
        ElementBox(module.id, Rect(left, top, footW, footH))
    }
}

package gg.maeve.mod.render

import gg.maeve.mod.module.HudLine
import gg.maeve.mod.module.HudModule
import gg.maeve.mod.platform.HudCanvas
import gg.maeve.mod.platform.TextRun
import kotlin.math.ceil

/** Draws a single HUD module's lines with its anchor/scale/style/background. Pure; shared
 *  by the in-game renderer and the editor preview so they position elements identically. */
object HudModuleRender {
    fun draw(canvas: HudCanvas, module: HudModule, lines: List<HudLine>) {
        if (lines.isEmpty()) return
        val style = module.style
        val pad = style.padding
        val textW = lines.maxOf { canvas.textWidth(it.text) }
        val textH = lines.size * canvas.lineHeight
        val localW = textW + pad * 2
        val localH = textH + pad * 2
        val footW = ceil(localW * style.scale).toInt()
        val footH = ceil(localH * style.scale).toInt()
        val (left, top) = HudLayout.resolveTopLeft(
            module.anchor, module.offsetX, module.offsetY, footW, footH, canvas.screenWidth, canvas.screenHeight,
        )
        canvas.withScale(style.scale, left, top) {
            if (style.background) canvas.fill(0, 0, localW, localH, style.backgroundColor)
            var lineY = pad
            for (line in lines) {
                val lineW = canvas.textWidth(line.text)
                val lineX = pad + HudLayout.lineX(0, textW, lineW, style.align)
                val color = line.color ?: style.color
                canvas.drawStyledText(
                    lineX, lineY, line.text,
                    TextRun(color, style.bold, style.italic, style.underline, style.strikethrough, style.shadow),
                )
                lineY += canvas.lineHeight
            }
        }
    }
}

package gg.maeve.mod.render

import gg.maeve.mod.module.ModuleManager
import gg.maeve.mod.platform.GameContext
import gg.maeve.mod.platform.HudCanvas
import gg.maeve.mod.platform.TextRun
import kotlin.math.ceil

/**
 * Draws all enabled HUD modules. Pure orchestration over the HudCanvas abstraction,
 * so it is fully unit-testable without Minecraft. Each module is positioned by its
 * anchor + offset (resolved against the current screen size), drawn inside one scale
 * transform, with an optional background panel and per-line color inheritance.
 */
class HudRenderController(private val modules: ModuleManager) {
    fun draw(canvas: HudCanvas, ctx: GameContext) {
        for (module in modules.hudModules()) {
            if (!module.enabled) continue
            val lines = module.render(ctx)
            if (lines.isEmpty()) continue

            val style = module.style
            val pad = style.padding
            val textW = lines.maxOf { canvas.textWidth(it.text) }
            val textH = lines.size * canvas.lineHeight
            val localW = textW + pad * 2
            val localH = textH + pad * 2
            // ceil so the reserved footprint always covers the actual scaled size — otherwise
            // a fractional scale could shift a right/center-anchored element off by a sub-pixel.
            val footW = ceil(localW * style.scale).toInt()
            val footH = ceil(localH * style.scale).toInt()
            val (left, top) = HudLayout.resolveTopLeft(
                module.anchor, module.offsetX, module.offsetY, footW, footH, canvas.screenWidth, canvas.screenHeight,
            )

            canvas.withScale(style.scale, left, top) {
                // element-local origin (0,0) maps to (left, top)
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
}

package gg.maeve.mod.module.hud

import gg.maeve.mod.module.HudAnchor
import gg.maeve.mod.module.HudLine
import gg.maeve.mod.module.HudModule
import gg.maeve.mod.module.HudSize
import gg.maeve.mod.module.HudStyle
import gg.maeve.mod.module.ModuleOptions
import gg.maeve.mod.module.ModuleToggle
import gg.maeve.mod.platform.GameContext
import gg.maeve.mod.platform.HudCanvas
import gg.maeve.mod.platform.TextRun

/**
 * Boxed WASD keystroke display (a custom-drawn module). Each key is a rounded slot whose
 * background colour is [HudStyle.color] (user-changeable) and which DARKENS while held — no
 * brackets, no spacebar raise. W sits exactly over S; gaps are uniform. The optional spacebar is
 * a full-width slot beneath A-S-D.
 */
class KeystrokesModule : HudModule {
    override val id = "keystrokes"
    override val displayName = "Keystrokes"
    override var enabled = false
    override var anchor = HudAnchor.TOP_LEFT
    override var offsetX = 4
    override var offsetY = 40
    override val defaultStyle = HudStyle(color = KEY_BG)
    override var style = HudStyle(color = KEY_BG)

    private val opts = ModuleOptions(listOf(ModuleToggle("space", "Spacebar", true)))
    override val toggles get() = opts.toggles
    override fun option(key: String) = opts.get(key)
    override fun setOption(key: String, value: Boolean) = opts.set(key, value)

    override fun render(ctx: GameContext): List<HudLine> = emptyList() // custom-drawn

    override fun footprint(ctx: GameContext): HudSize {
        val w = 3 * KEY + 2 * GAP
        var h = KEY + GAP + KEY
        if (opts.get("space")) h += GAP + SPACE_H
        return HudSize(w, h)
    }

    override fun drawCustom(canvas: HudCanvas, ctx: GameContext) {
        val rowW = 3 * KEY + 2 * GAP
        key(canvas, KEY + GAP, 0, KEY, KEY, ctx.keyForward, "W") // W column == S column
        val y2 = KEY + GAP
        key(canvas, 0, y2, KEY, KEY, ctx.keyLeft, "A")
        key(canvas, KEY + GAP, y2, KEY, KEY, ctx.keyBack, "S")
        key(canvas, 2 * (KEY + GAP), y2, KEY, KEY, ctx.keyRight, "D")
        if (opts.get("space")) key(canvas, 0, y2 + KEY + GAP, rowW, SPACE_H, ctx.keyJump, "")
    }

    private fun key(canvas: HudCanvas, x: Int, y: Int, w: Int, h: Int, pressed: Boolean, label: String) {
        val fill = if (pressed) darken(style.color, 0.5f) else style.color
        canvas.fill(x, y, w, h, fill)
        if (label.isEmpty()) return
        val tw = canvas.textWidth(label)
        canvas.drawStyledText(
            x + (w - tw) / 2, y + (h - canvas.lineHeight) / 2 + 1, label,
            TextRun(letterColor(fill), bold = style.bold, italic = style.italic, shadow = false), // contrast vs the actual fill
        )
    }

    /** Darken rgb toward black by [f], preserving alpha. */
    private fun darken(argb: Int, f: Float): Int {
        val a = argb.toLong() and 0xFF000000L
        val r = (((argb ushr 16) and 0xFF) * (1 - f)).toInt().coerceIn(0, 255)
        val g = (((argb ushr 8) and 0xFF) * (1 - f)).toInt().coerceIn(0, 255)
        val b = ((argb and 0xFF) * (1 - f)).toInt().coerceIn(0, 255)
        return (a.toInt()) or (r shl 16) or (g shl 8) or b
    }

    /** Readable letter colour for a given key background (auto black/white by luminance). */
    private fun letterColor(bg: Int): Int {
        val r = (bg ushr 16) and 0xFF; val g = (bg ushr 8) and 0xFF; val b = bg and 0xFF
        val lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
        return if (lum > 0.6) 0xFF101018.toInt() else 0xFFFFFFFF.toInt()
    }

    private companion object {
        const val KEY = 18
        const val GAP = 4
        const val SPACE_H = 11
        val KEY_BG = 0xD03B3658.toInt() // mid translucent key slot (changeable); darkens on press
    }
}

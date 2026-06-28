package gg.snell.mod.ui

import gg.snell.mod.editor.Rect
import gg.snell.mod.platform.EditorCanvas
import gg.snell.shared.SnellPalette

/** Button visual styles, mirroring the launcher's SnellButton variants. */
enum class SnellBtn { Primary, Secondary, Ghost, Danger }

/** Status-pill colour roles, mirroring the launcher's PillKind. */
enum class PillRole { Online, Offline, Info, Neutral, Warning }

/**
 * Launcher-matched menu chrome, drawn only from the canvas primitives Minecraft gives us
 * (fill / outline / vertical gradient / text / scale) so it stays version-independent and unit-
 * renderable off a real game. Cyan accent over neutral surfaces, soft shadows, bevels, knocked
 * corners, real toggles — the same recipes the HUD editor uses, generalized for full screens.
 *
 * All methods are pure draws (side effects on [EditorCanvas]); layout/hit-testing lives in the
 * per-screen `*Layout` so it can be tested without a canvas.
 */
object SnellUi {
    const val WHITE: Int = 0xFFFFFFFF.toInt()
    private const val BLACK: Int = 0xFF000000.toInt()
    private const val SHADOW: Int = 0x55000000

    // ---- colour maths (copied from the editor's proven recipe) --------------------------------
    fun blend(a: Int, b: Int, f: Float): Int {
        val ar = (a ushr 16) and 0xFF; val ag = (a ushr 8) and 0xFF; val ab = a and 0xFF
        val br = (b ushr 16) and 0xFF; val bg = (b ushr 8) and 0xFF; val bb = b and 0xFF
        val rr = (ar + (br - ar) * f).toInt().coerceIn(0, 255)
        val gg = (ag + (bg - ag) * f).toInt().coerceIn(0, 255)
        val bl = (ab + (bb - ab) * f).toInt().coerceIn(0, 255)
        return (a.toLong() and 0xFF000000L).toInt() or (rr shl 16) or (gg shl 8) or bl
    }
    fun lighten(c: Int, f: Float) = blend(c, WHITE, f)
    fun darken(c: Int, f: Float) = blend(c, BLACK, f)

    fun ellipsize(canvas: EditorCanvas, text: String, maxW: Int): String {
        if (canvas.textWidth(text) <= maxW) return text
        var t = text
        while (t.isNotEmpty() && canvas.textWidth("$t…") > maxW) t = t.dropLast(1)
        return t.trimEnd() + "…"
    }

    /** Knock ~2px corners to [bg] so a panel/row/button reads as rounded over that surface. */
    fun round(canvas: EditorCanvas, r: Rect, bg: Int) {
        canvas.fill(r.left, r.top, 2, 1, bg); canvas.fill(r.left, r.top, 1, 2, bg)
        canvas.fill(r.right - 2, r.top, 2, 1, bg); canvas.fill(r.right - 1, r.top, 1, 2, bg)
        canvas.fill(r.left, r.bottom - 1, 2, 1, bg); canvas.fill(r.left, r.bottom - 2, 1, 2, bg)
        canvas.fill(r.right - 2, r.bottom - 1, 2, 1, bg); canvas.fill(r.right - 1, r.bottom - 2, 1, 2, bg)
    }

    // ---- surfaces -----------------------------------------------------------------------------

    /** Opaque full-screen backdrop (the dark launcher field) with a faint top sheen. */
    fun backdrop(canvas: EditorCanvas, w: Int, h: Int) {
        canvas.fill(0, 0, w, h, SnellPalette.bg2)
        canvas.gradientV(0, 0, w, h / 2, SnellPalette.withAlpha(SnellPalette.s1, 0x4D), SnellPalette.withAlpha(SnellPalette.s1, 0))
    }

    /** Translucent dim over the live world/HUD, for menus opened in-game (pause, popups). */
    fun scrim(canvas: EditorCanvas, w: Int, h: Int) =
        canvas.fill(0, 0, w, h, SnellPalette.withAlpha(SnellPalette.bg2, 0xD0))

    /** Card/panel surface: soft drop shadow, s1 fill, neutral border, knocked corners over [under]. */
    fun panel(canvas: EditorCanvas, r: Rect, under: Int = SnellPalette.bg2) {
        canvas.fill(r.left + 3, r.bottom, r.width, 3, SHADOW)
        canvas.fill(r.right, r.top + 3, 3, r.height, SHADOW)
        canvas.gradientV(r.left, r.top, r.width, r.height, lighten(SnellPalette.s1, 0.04f), SnellPalette.s1)
        canvas.border(r.left, r.top, r.width, r.height, SnellPalette.border)
        round(canvas, r, under)
    }

    /** Header strip at the top of a [panel]: gradient bar, accent tab, title, divider. */
    fun header(canvas: EditorCanvas, panel: Rect, headerH: Int, title: String) {
        canvas.gradientV(panel.left + 1, panel.top + 1, panel.width - 2, headerH - 1, lighten(SnellPalette.s2, 0.06f), SnellPalette.s2)
        canvas.fill(panel.left + 1, panel.top + headerH, panel.width - 2, 1, SnellPalette.border)
        canvas.fill(panel.left + 1, panel.top + (headerH - 9) / 2, 3, 9, SnellPalette.accent) // accent tab
        canvas.drawText(panel.left + 12, panel.top + (headerH - canvas.lineHeight) / 2, title, SnellPalette.text)
    }

    /** Muted uppercase section label with an underline rule, like the launcher's SectionLabel. */
    fun sectionLabel(canvas: EditorCanvas, x: Int, y: Int, w: Int, text: String) {
        canvas.drawText(x, y, text.uppercase(), SnellPalette.text3)
        canvas.fill(x, y + canvas.lineHeight + 1, w, 1, SnellPalette.border)
    }

    // ---- controls -----------------------------------------------------------------------------

    /** A button in one of four [SnellBtn] styles, with hover + disabled states. */
    fun button(canvas: EditorCanvas, r: Rect, text: String, style: SnellBtn = SnellBtn.Secondary, hover: Boolean = false, enabled: Boolean = true) {
        val fg: Int
        when (style) {
            SnellBtn.Primary -> {
                val base = if (enabled && hover) SnellPalette.accentHi else SnellPalette.accent
                if (enabled) canvas.fill(r.left + 1, r.bottom, r.width, 2, SHADOW)
                val a = if (enabled) base else darken(SnellPalette.accent, 0.5f)
                canvas.gradientV(r.left, r.top, r.width, r.height, lighten(a, 0.18f), a)
                canvas.border(r.left, r.top, r.width, r.height, lighten(a, 0.15f))
                fg = if (enabled) SnellPalette.onAccent else SnellPalette.textDisabled
            }
            SnellBtn.Secondary -> {
                val base = if (enabled && hover) lighten(SnellPalette.s2, 0.14f) else SnellPalette.s2
                canvas.fill(r.left, r.top, r.width, r.height, base)
                canvas.border(r.left, r.top, r.width, r.height, SnellPalette.border)
                fg = if (enabled) SnellPalette.text else SnellPalette.textDisabled
            }
            SnellBtn.Ghost -> {
                if (enabled && hover) canvas.fill(r.left, r.top, r.width, r.height, SnellPalette.accentSubtle)
                fg = if (enabled) SnellPalette.accentHi else SnellPalette.textDisabled
            }
            SnellBtn.Danger -> {
                if (enabled && hover) canvas.fill(r.left, r.top, r.width, r.height, SnellPalette.withAlpha(SnellPalette.danger, 0x33))
                else canvas.fill(r.left, r.top, r.width, r.height, SnellPalette.withAlpha(SnellPalette.danger, 0x1F))
                canvas.border(r.left, r.top, r.width, r.height, SnellPalette.withAlpha(SnellPalette.danger, 0x66))
                fg = if (enabled) SnellPalette.danger else SnellPalette.textDisabled
            }
        }
        if (style != SnellBtn.Ghost) round(canvas, r, SnellPalette.bg2)
        val tw = canvas.textWidth(text)
        canvas.drawText(r.left + (r.width - tw) / 2, r.top + (r.height - canvas.lineHeight) / 2 + 1, text, fg)
    }

    /** A 2-state pill toggle: cyan track + white knob when on, neutral when off. */
    fun switch(canvas: EditorCanvas, r: Rect, on: Boolean) {
        val track = if (on) SnellPalette.accent else darken(SnellPalette.s2, 0.18f)
        canvas.gradientV(r.left, r.top, r.width, r.height, lighten(track, 0.18f), track)
        canvas.border(r.left, r.top, r.width, r.height, if (on) lighten(SnellPalette.accent, 0.2f) else SnellPalette.border)
        round(canvas, r, SnellPalette.s1)
        val k = r.height - 4
        val kx = if (on) r.right - k - 2 else r.left + 2
        canvas.fill(kx, r.top + 2, k, k, WHITE)
    }

    /**
     * A selection-list row: hover lighten, selected gets an accent left-strip + subtle cyan wash.
     * Caller draws the row content (icon/title/subtitle) on top.
     */
    fun listRow(canvas: EditorCanvas, r: Rect, selected: Boolean, hover: Boolean, under: Int = SnellPalette.s1) {
        val bg = when {
            selected -> SnellPalette.accentSubtle
            hover -> lighten(under, 0.06f)
            else -> under
        }
        canvas.fill(r.left, r.top, r.width, r.height, bg)
        canvas.border(r.left, r.top, r.width, r.height, if (selected) SnellPalette.withAlpha(SnellPalette.accent, 0x66) else SnellPalette.border)
        if (selected) canvas.fill(r.left, r.top, 3, r.height, SnellPalette.accent)
        round(canvas, r, under)
    }

    /** A status pill (rounded, tinted background + coloured label), like the launcher's StatusPill. */
    fun pill(canvas: EditorCanvas, x: Int, y: Int, text: String, role: PillRole) {
        val c = when (role) {
            PillRole.Online -> SnellPalette.accent
            PillRole.Offline -> SnellPalette.danger
            PillRole.Info -> SnellPalette.info
            PillRole.Warning -> SnellPalette.ember
            PillRole.Neutral -> SnellPalette.text3
        }
        val w = canvas.textWidth(text) + 16
        val h = canvas.lineHeight + 6
        val r = Rect(x, y, w, h)
        canvas.fill(r.left, r.top, r.width, r.height, SnellPalette.withAlpha(c, 0x22))
        canvas.border(r.left, r.top, r.width, r.height, SnellPalette.withAlpha(c, 0x55))
        round(canvas, r, SnellPalette.s1)
        canvas.fill(r.left + 6, r.top + (h - 4) / 2, 4, 4, c) // status dot
        canvas.drawText(r.left + 13, r.top + (h - canvas.lineHeight) / 2, text, c)
    }

    /** Text input chrome: inset s2 field, accent border when focused, placeholder/caret. */
    fun textField(canvas: EditorCanvas, r: Rect, text: String, focused: Boolean, placeholder: String = "") {
        canvas.fill(r.left, r.top, r.width, r.height, darken(SnellPalette.s2, 0.12f))
        canvas.border(r.left, r.top, r.width, r.height, if (focused) SnellPalette.accent else SnellPalette.border)
        round(canvas, r, SnellPalette.s1)
        val ty = r.top + (r.height - canvas.lineHeight) / 2 + 1
        if (text.isEmpty() && !focused) {
            canvas.drawText(r.left + 7, ty, ellipsize(canvas, placeholder, r.width - 14), SnellPalette.text3)
        } else {
            val shown = ellipsize(canvas, text, r.width - 16)
            canvas.drawText(r.left + 7, ty, shown, SnellPalette.text)
            if (focused) canvas.fill(r.left + 7 + canvas.textWidth(shown) + 1, ty - 1, 1, canvas.lineHeight + 1, SnellPalette.accent)
        }
    }

    /** A vertical scrollbar for a list: faint track + accent-tinted thumb sized to the viewport. */
    fun scrollbar(canvas: EditorCanvas, x: Int, top: Int, trackH: Int, contentH: Int, scrollY: Int) {
        if (contentH <= trackH) return
        canvas.fill(x, top, 3, trackH, SnellPalette.withAlpha(SnellPalette.border, 0x80))
        val thumbH = (trackH.toLong() * trackH / contentH).toInt().coerceAtLeast(16)
        val maxScroll = contentH - trackH
        val thumbY = top + if (maxScroll <= 0) 0 else ((trackH - thumbH).toLong() * scrollY / maxScroll).toInt()
        canvas.fill(x, thumbY, 3, thumbH, SnellPalette.withAlpha(SnellPalette.accent, 0xAA))
    }

    /**
     * A larger screen heading, scaled up from the fixed game font. ([x],[y]) is the top-left of
     * the scaled text; `withScale` makes the pivot the local origin, so the body draws at (0,0).
     * The unscaled width is `textWidth(text)`; multiply by [scale] to centre.
     */
    fun heading(canvas: EditorCanvas, x: Int, y: Int, text: String, scale: Float = 1.6f, color: Int = SnellPalette.text) {
        canvas.withScale(scale, x, y) { canvas.drawText(0, 0, text, color) }
    }
}

package gg.snell.shared

/**
 * The launcher "Twilight Royal" palette as plain ARGB Ints, shared by the launcher
 * (Compose `Color(Int)`) and the mod (GuiGraphics ARGB) so the in-game HUD's base theme
 * matches the launcher from a single source of truth. (`const` is not usable here because
 * the ARGB values exceed Int range as positive literals and need `.toInt()`.)
 */
object SnellPalette {
    val primary: Int = 0xFF8B6DFF.toInt()
    val gold: Int = 0xFFE2B45C.toInt()
    val background: Int = 0xFF0B0A12.toInt()
    val surface: Int = 0xFF14121E.toInt()
    val elevated: Int = 0xFF1C1A2A.toInt()
    val text: Int = 0xFFECEAF5.toInt()
    val text2: Int = 0xFFA7A3C0.toInt()
    val outline: Int = 0xFF2A2740.toInt()
    val success: Int = 0xFF48D597.toInt()
    val error: Int = 0xFFFF6B7A.toInt()

    // --- Launcher-aligned ramp (cyan accent + neutral surfaces) --------------------------------
    // The HUD's base theme above is "Twilight Royal" (purple). The launcher chrome — and now the
    // in-game menus — use a cyan accent over neutral-grey surfaces. These mirror the launcher's
    // SnellColors exactly so menus match it from one source. `s1 < s2`, `bg2` is the darkest rail.
    val accent: Int = 0xFF00D9FF.toInt()        // primary action / selection (cyan)
    val accentHi: Int = 0xFF5CE6FF.toInt()      // hover
    val accentLo: Int = 0xFF00A6C2.toInt()      // pressed
    val accentSubtle: Int = 0x2900D9FF          // ~16% accent fill (active rows, chips)
    val onAccent: Int = 0xFF04222B.toInt()      // text/icons on accent (dark teal)
    val bg2: Int = 0xFF0E0E0E.toInt()           // darkest surface (screen backdrop, rail)
    val s1: Int = 0xFF141414.toInt()            // cards / panels
    val s2: Int = 0xFF1E1E1E.toInt()            // inset controls / elevated
    val border: Int = 0xFF262626.toInt()        // neutral border (menus; cf. purple [outline])
    val text3: Int = 0xFF6E6E6E.toInt()         // muted text
    val textDisabled: Int = 0xFF4A4A4A.toInt()
    val ember: Int = 0xFFFF4D9D.toInt()         // warning / promoted (pink)
    val danger: Int = 0xFFF06B6B.toInt()        // error / offline
    val info: Int = 0xFF6FA8FF.toInt()          // info / downloading (blue)

    /** [surface] at the given [alpha] (0..255), for translucent HUD background panels. */
    fun surfaceAlpha(alpha: Int): Int = (surface and 0x00FFFFFF) or ((alpha and 0xFF) shl 24)

    /** Any ARGB [color] re-alpha'd to [alpha] (0..255) — for translucent menu scrims/panels. */
    fun withAlpha(color: Int, alpha: Int): Int = (color and 0x00FFFFFF) or ((alpha and 0xFF) shl 24)
}

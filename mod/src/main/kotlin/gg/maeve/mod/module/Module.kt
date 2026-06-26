package gg.maeve.mod.module

/**
 * A self-contained client feature. The mod menu and config layer are generic
 * over Module, so adding a feature never requires touching menu or config code.
 */
interface Module {
    /** Stable identifier used as the config key. Lowercase, no spaces. */
    val id: String

    /** Human-readable name shown in the mod menu. */
    val displayName: String

    /** Whether the module is currently active. Persisted by the config layer. */
    var enabled: Boolean

    /** Called once when the module is registered. */
    fun onRegister() {}
}

/** A module that draws to the in-game HUD. */
interface HudModule : Module {
    /** Corner/edge/center this element is pinned to. Persisted. */
    var anchor: HudAnchor

    /** Offset from [anchor], in scaled GUI pixels: an inward gap for corner/edge anchors,
     *  and a fine nudge from the centered position for the centered anchors. Persisted. */
    var offsetX: Int
    var offsetY: Int

    /** Current visual style (starts at [defaultStyle], user-overridable). Persisted. */
    var style: HudStyle

    /** The module's base/theme style — the reset target. */
    val defaultStyle: HudStyle get() = HudStyle()

    /** Module-specific boolean options surfaced in the editor (beyond generic [style]). */
    val toggles: List<ModuleToggle> get() = emptyList()
    fun option(key: String): Boolean = false
    fun setOption(key: String, value: Boolean) {}

    /** Produce the lines to draw this frame. Empty = nothing to draw. */
    fun render(ctx: gg.maeve.mod.platform.GameContext): List<HudLine>
}

/**
 * One line of HUD text. A null [color] means "use the module's style color"; a non-null
 * value is an explicit per-line override (e.g. keystroke pressed vs released).
 */
data class HudLine(val text: String, val color: Int? = null)

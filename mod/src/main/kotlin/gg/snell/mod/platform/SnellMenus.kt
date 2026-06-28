package gg.snell.mod.platform

/**
 * Runtime switch + constants for the bespoke in-game menu overhaul, read by the screen-swap mixin
 * (which can't easily reach the [gg.snell.mod.config.Config] instance). Set from config on init.
 */
object SnellMenus {
    /** When true, the mixin replaces vanilla menus with the Snell screens. Mirrors the config flag. */
    @Volatile
    var enabled: Boolean = true

    /** Client version shown in menu footers. */
    const val VERSION: String = "26.2"
}

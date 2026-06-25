package gg.maeve.mod.ui

import gg.maeve.mod.module.ModuleManager

/**
 * Drives the in-game mod menu: lists modules and toggles them. [onAfterToggle] fires after a
 * toggle so the bridge can apply MC-side side effects (e.g. enabling the font resource pack)
 * without the pure menu/module layer depending on Minecraft. Default no-op keeps it testable.
 */
class ModMenuController(
    val modules: ModuleManager,
    private val onAfterToggle: (id: String, enabled: Boolean) -> Unit = { _, _ -> },
) {
    data class Row(val id: String, val name: String, val enabled: Boolean)

    fun rows(): List<Row> = modules.all().map { Row(it.id, it.displayName, it.enabled) }

    fun onToggle(id: String) {
        modules.toggle(id)
        onAfterToggle(id, modules.byId(id)?.enabled ?: false)
    }
}

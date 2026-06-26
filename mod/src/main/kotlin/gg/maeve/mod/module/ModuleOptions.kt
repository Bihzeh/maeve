package gg.maeve.mod.module

/** A user-toggleable, module-specific boolean option (beyond generic style), shown in the editor. */
data class ModuleToggle(val key: String, val label: String, val default: Boolean)

/** Reusable backing store for a module's [ModuleToggle]s. Pure (no Minecraft types). */
class ModuleOptions(val toggles: List<ModuleToggle>) {
    private val state = mutableMapOf<String, Boolean>()
    fun get(key: String): Boolean = state[key] ?: toggles.firstOrNull { it.key == key }?.default ?: false
    fun set(key: String, value: Boolean) { state[key] = value }
}

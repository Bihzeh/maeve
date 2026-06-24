package gg.maeve.mod.module

import gg.maeve.mod.config.Config

/**
 * Owns the registry of modules and bridges them to the config layer.
 * Registration order is the HUD draw order.
 *
 * Threading: all access happens on the Minecraft client main thread (module
 * registration at init, toggles from input/tick, HUD reads from the GUI extract
 * phase — all the same thread), so no synchronization is required.
 */
class ModuleManager(private val config: Config) {
    private val modules = LinkedHashMap<String, Module>()
    private val hudList = ArrayList<HudModule>() // cached so the render hot path allocates nothing

    fun register(module: Module) {
        require(!modules.containsKey(module.id)) { "Duplicate module id: ${module.id}" }
        modules[module.id] = module
        if (module is HudModule) hudList.add(module)
        config.applyTo(module)   // restore persisted state
        module.onRegister()
    }

    fun all(): Collection<Module> = modules.values

    /** Returns the cached HUD list directly (read-only use): no per-frame allocation. */
    fun hudModules(): List<HudModule> = hudList

    fun toggle(id: String) {
        modules[id]?.let {
            it.enabled = !it.enabled
            config.snapshot(modules.values)
            config.save()
        }
    }

    fun saveAll() {
        config.snapshot(modules.values)
        config.save()
    }
}

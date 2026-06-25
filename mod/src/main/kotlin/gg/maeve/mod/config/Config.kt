package gg.maeve.mod.config

import gg.maeve.mod.module.HudAnchor
import gg.maeve.mod.module.HudModule
import gg.maeve.mod.module.Module
import gg.maeve.mod.module.TextAlign
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

@Serializable
private data class ModuleState(
    val enabled: Boolean,
    // legacy v1 position (still read for migration; mirrored on write)
    val x: Int = 0,
    val y: Int = 0,
    // v2 position
    val anchor: String? = null,
    val offsetX: Int? = null,
    val offsetY: Int? = null,
    // v2 style (all optional; null => keep the module's default for that field)
    val color: String? = null,
    val bold: Boolean? = null,
    val italic: Boolean? = null,
    val underline: Boolean? = null,
    val strikethrough: Boolean? = null,
    val shadow: Boolean? = null,
    val scale: Float? = null,
    val align: String? = null,
    val background: Boolean? = null,
    val backgroundColor: String? = null,
    val padding: Int? = null,
)

@Serializable
private data class ConfigData(
    val schema: Int = 2,
    val modules: MutableMap<String, ModuleState> = mutableMapOf(),
)

/**
 * Schema-versioned per-module config persisted as JSON under the given dir
 * (at runtime: .minecraft/config/maeve/config.json).
 *
 * Saves are synchronous: the file is small and only written on a user action (a toggle,
 * or the editor closing), never per frame. I/O failures are logged, never thrown.
 * v1 files (schema 1, enabled/x/y only) migrate lazily on load: missing v2 fields fall
 * back to the module's default style, and legacy x/y become a TOP_LEFT anchor + offset.
 */
class Config(private val dir: Path) {
    private val file: Path = dir.resolve("config.json")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private var data: ConfigData = ConfigData()

    fun load() {
        if (!file.exists()) return
        runCatching { data = json.decodeFromString<ConfigData>(file.readText()) }
            .onFailure { LOG.warn("Maeve: config parse failed, using defaults", it) }
    }

    /** Restore persisted state onto a freshly registered module. */
    fun applyTo(module: Module) {
        val s = data.modules[module.id] ?: return
        module.enabled = s.enabled
        if (module is HudModule) {
            module.anchor = s.anchor?.let { name ->
                runCatching { HudAnchor.valueOf(name) }
                    .onFailure { LOG.warn("Maeve: unknown HUD anchor '$name', using TOP_LEFT") }
                    .getOrNull()
            } ?: HudAnchor.TOP_LEFT
            module.offsetX = s.offsetX ?: s.x
            module.offsetY = s.offsetY ?: s.y
            val d = module.defaultStyle
            module.style = d.copy(
                color = s.color?.let(HexColor::decode) ?: d.color,
                bold = s.bold ?: d.bold,
                italic = s.italic ?: d.italic,
                underline = s.underline ?: d.underline,
                strikethrough = s.strikethrough ?: d.strikethrough,
                shadow = s.shadow ?: d.shadow,
                scale = (s.scale ?: d.scale).coerceIn(0.5f, 3.0f),
                align = s.align?.let { name ->
                    runCatching { TextAlign.valueOf(name) }
                        .onFailure { LOG.warn("Maeve: unknown HUD text align '$name', using ${d.align}") }
                        .getOrNull()
                } ?: d.align,
                background = s.background ?: d.background,
                backgroundColor = s.backgroundColor?.let(HexColor::decode) ?: d.backgroundColor,
                padding = s.padding ?: d.padding,
            )
        }
    }

    /** Capture the current state of all modules into memory (v2 form). */
    fun snapshot(modules: Collection<Module>) {
        modules.forEach { m ->
            data.modules[m.id] = if (m is HudModule) {
                val st = m.style
                ModuleState(
                    enabled = m.enabled,
                    x = m.offsetX, y = m.offsetY, // mirror so any v1 reader still gets a position
                    anchor = m.anchor.name,
                    offsetX = m.offsetX, offsetY = m.offsetY,
                    color = HexColor.encode(st.color),
                    bold = st.bold, italic = st.italic,
                    underline = st.underline, strikethrough = st.strikethrough,
                    shadow = st.shadow, scale = st.scale, align = st.align.name,
                    background = st.background, backgroundColor = HexColor.encode(st.backgroundColor),
                    padding = st.padding,
                )
            } else {
                ModuleState(m.enabled)
            }
        }
    }

    fun save() {
        runCatching {
            Files.createDirectories(dir)
            file.writeText(json.encodeToString(data))
        }.onFailure { LOG.error("Maeve: failed to save config", it) }
    }

    private companion object {
        private val LOG = LoggerFactory.getLogger("Maeve")
    }
}

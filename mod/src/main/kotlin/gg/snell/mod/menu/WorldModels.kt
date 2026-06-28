package gg.snell.mod.menu

/**
 * One row in the singleplayer world picker — a plain data model so the picker can be unit-tested
 * and headlessly rendered without Minecraft's `LevelSummary`. The runtime screen maps these to/from
 * the real save list; the layout/renderer only ever see this shape.
 *
 * @param name      display name of the world (the level name shown to the player)
 * @param folder    the save-directory id (stable key for selection / play / delete)
 * @param subtitle  one-line meta, e.g. "Survival · 1.21 · 3 days ago"
 */
data class WorldRow(
    val name: String,
    val folder: String,
    val subtitle: String,
)

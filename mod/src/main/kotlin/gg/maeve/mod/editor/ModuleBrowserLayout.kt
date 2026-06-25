package gg.maeve.mod.editor

/**
 * Pure layout for the editor's top button bar and the module browser overlay. Both the renderer
 * (draw) and EditorState (hit-test) call these so they never drift (mirrors PanelLayout).
 */
object ModuleBrowserLayout {
    private const val BAR_H = 16
    private const val MODULES_W = 96
    private const val DONE_W = 60
    private const val PANEL_W = 240
    private const val PANEL_TOP = 28
    private const val ROW_H = 16
    private const val ROW_GAP = 2

    fun modulesButton(screenW: Int, screenH: Int): Rect = Rect((screenW - MODULES_W) / 2, screenH - BAR_H - 6, MODULES_W, BAR_H)
    fun doneButton(screenW: Int, screenH: Int): Rect = Rect(screenW - DONE_W - 6, screenH - BAR_H - 6, DONE_W, BAR_H)

    fun panelRect(screenW: Int, count: Int): Rect =
        Rect((screenW - PANEL_W) / 2, PANEL_TOP, PANEL_W, 24 + count * (ROW_H + ROW_GAP))

    /** One clickable row per module id, inside the panel. */
    fun rows(screenW: Int, ids: List<String>): List<Pair<String, Rect>> {
        val left = (screenW - PANEL_W) / 2 + 8
        return ids.mapIndexed { i, id ->
            id to Rect(left, PANEL_TOP + 18 + i * (ROW_H + ROW_GAP), PANEL_W - 16, ROW_H)
        }
    }
}

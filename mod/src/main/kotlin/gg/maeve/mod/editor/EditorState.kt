package gg.maeve.mod.editor

import gg.maeve.mod.module.ModuleManager

/**
 * Pure, immediate-mode editor interaction state. Drives the editor screen from raw mouse
 * events without any Minecraft types, so it is fully unit-testable. The screen supplies the
 * current element boxes + screen size each event; control mutations go through ModuleManager
 * setters (live preview); the screen persists once on close.
 */
class EditorState {
    var selectedId: String? = null
        private set
    var dirty: Boolean = false
        private set

    private var dragId: String? = null
    private var startMouseX = 0
    private var startMouseY = 0
    private var startLeft = 0
    private var startTop = 0

    /** Returns true if the press was consumed. Order: panel controls (if selected), else element. */
    fun onPress(mouseX: Int, mouseY: Int, screenW: Int, screenH: Int, boxes: List<ElementBox>, modules: ModuleManager): Boolean {
        if (selectedId != null && mouseX >= screenW - PanelLayout.WIDTH) {
            val ctrl = PanelLayout.controls(screenW - PanelLayout.WIDTH, PanelLayout.TOP)
                .firstOrNull { it.rect.contains(mouseX, mouseY) }
            if (ctrl != null) return applyControl(ctrl.id, modules)
            return true // inside the panel but not on a control: consume, keep selection
        }
        val id = hitTest(boxes, mouseX, mouseY)
        selectedId = id
        dragId = id
        if (id == null) return false
        val box = boxes.first { it.id == id }.rect
        startMouseX = mouseX; startMouseY = mouseY; startLeft = box.left; startTop = box.top
        return true
    }

    /** Drag the selected element; re-anchors live so it stays put on release. Returns true if dragging. */
    fun onDrag(mouseX: Int, mouseY: Int, screenW: Int, screenH: Int, boxes: List<ElementBox>, modules: ModuleManager): Boolean {
        val id = dragId ?: return false
        val box = boxes.firstOrNull { it.id == id }?.rect ?: return false
        val left = (startLeft + (mouseX - startMouseX)).coerceIn(0, screenW - box.width)
        val top = (startTop + (mouseY - startMouseY)).coerceIn(0, screenH - box.height)
        val moved = Rect(left, top, box.width, box.height)
        val anchor = EditorAnchor.anchorFromPosition(moved, screenW, screenH)
        val (ox, oy) = EditorAnchor.offsetForAnchor(anchor, moved, screenW, screenH)
        modules.setAnchorOffset(id, anchor, ox, oy)
        dirty = true
        return true
    }

    fun onRelease(): Boolean {
        val was = dragId != null
        dragId = null
        return was
    }

    private fun applyControl(id: String, modules: ModuleManager): Boolean {
        val sel = selectedId ?: return false
        when {
            id == "visible" -> modules.hudById(sel)?.let { modules.setEnabled(sel, !it.enabled) } ?: return false
            id == "bold" -> modules.updateStyle(sel) { it.copy(bold = !it.bold) }
            id == "italic" -> modules.updateStyle(sel) { it.copy(italic = !it.italic) }
            id == "underline" -> modules.updateStyle(sel) { it.copy(underline = !it.underline) }
            id == "strike" -> modules.updateStyle(sel) { it.copy(strikethrough = !it.strikethrough) }
            id == "shadow" -> modules.updateStyle(sel) { it.copy(shadow = !it.shadow) }
            id == "background" -> modules.updateStyle(sel) { it.copy(background = !it.background) }
            id == "scale-" -> modules.updateStyle(sel) { it.copy(scale = (it.scale - 0.25f).coerceIn(0.5f, 3.0f)) }
            id == "scale+" -> modules.updateStyle(sel) { it.copy(scale = (it.scale + 0.25f).coerceIn(0.5f, 3.0f)) }
            id == "reset" -> modules.resetStyle(sel)
            id.startsWith("swatch:") -> {
                val idx = id.removePrefix("swatch:").toIntOrNull() ?: return false
                val rgb = MaeveColor.rgbOf(PanelLayout.SWATCHES.getOrNull(idx) ?: return false)
                modules.updateStyle(sel) { it.copy(color = MaeveColor.argb(MaeveColor.alphaOf(it.color), rgb)) }
            }
            else -> return false
        }
        dirty = true
        return true
    }
}

package gg.maeve.mod.editor

import gg.maeve.mod.module.ModuleManager

/**
 * Pure, immediate-mode editor interaction state. Drives the editor screen from raw mouse
 * events without any Minecraft types, so it is fully unit-testable. The drag footprint is
 * captured on press so a mid-drag text-size change (e.g. coords updating) can't make the
 * clamp range invert or the element jump. Control mutations go through ModuleManager
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
    private var dragW = 0
    private var dragH = 0

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
        if (id == null) { dragId = null; return false }
        val box = boxes.first { it.id == id }.rect
        dragId = id
        startMouseX = mouseX; startMouseY = mouseY
        startLeft = box.left; startTop = box.top
        dragW = box.width; dragH = box.height
        return true
    }

    /** Drag the selected element; re-anchors live so it stays put on release. Returns true if dragging. */
    fun onDrag(mouseX: Int, mouseY: Int, screenW: Int, screenH: Int, modules: ModuleManager): Boolean {
        val id = dragId ?: return false
        // coerceAtLeast(0) so an element larger than the viewport pins to (0,0) instead of
        // inverting the clamp range (which would throw IllegalArgumentException).
        val maxLeft = (screenW - dragW).coerceAtLeast(0)
        val maxTop = (screenH - dragH).coerceAtLeast(0)
        val left = (startLeft + (mouseX - startMouseX)).coerceIn(0, maxLeft)
        val top = (startTop + (mouseY - startMouseY)).coerceIn(0, maxTop)
        val moved = Rect(left, top, dragW, dragH)
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

    /** Clears the selection (and any drag) if the selected element no longer has a box — e.g.
     *  its module stopped rendering — so the panel never shows for an absent element. */
    fun pruneSelection(boxes: List<ElementBox>) {
        val sel = selectedId ?: return
        if (boxes.none { it.id == sel }) { selectedId = null; dragId = null }
    }

    private fun applyControl(id: String, modules: ModuleManager): Boolean {
        val sel = selectedId ?: return false
        val module = modules.hudById(sel) ?: return false
        when {
            id == "visible" -> modules.setEnabled(sel, !module.enabled)
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

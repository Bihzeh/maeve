package gg.maeve.mod.editor

import gg.maeve.mod.config.HexColor
import gg.maeve.mod.module.HudModule
import gg.maeve.mod.module.ModuleManager
import kotlin.math.roundToInt

/**
 * Pure, immediate-mode editor interaction state. Drives the editor screen from raw mouse/char
 * events without any Minecraft types, so it is fully unit-testable. Holds the live HSVA being
 * edited for the selected element's color; the SV square / hue / alpha bars / hex field all
 * read and write it. Control mutations go through ModuleManager setters (live preview); the
 * screen persists once on close.
 */
class EditorState {
    var selectedId: String? = null
        private set
    var dirty: Boolean = false
        private set
    var browserOpen: Boolean = false
        private set
    var closeRequested: Boolean = false
        private set

    private var dragId: String? = null
    private var startMouseX = 0
    private var startMouseY = 0
    private var startLeft = 0
    private var startTop = 0
    private var dragW = 0
    private var dragH = 0

    private var editH = 0f
    private var editS = 0f
    private var editV = 0f
    private var editA = 255
    private var activeColor: String? = null // "sv" | "hue" | "alpha" while dragging a picker
    private var hexFocused = false
    private var hexBuffer = ""

    val colorH get() = editH
    val colorS get() = editS
    val colorV get() = editV
    val colorA get() = editA
    val isHexFocused get() = hexFocused
    val hexText get() = hexBuffer

    fun onPress(mouseX: Int, mouseY: Int, screenW: Int, screenH: Int, boxes: List<ElementBox>, modules: ModuleManager): Boolean {
        if (ModuleBrowserLayout.doneButton(screenW, screenH).contains(mouseX, mouseY)) { closeRequested = true; return true }
        if (ModuleBrowserLayout.modulesButton(screenW, screenH).contains(mouseX, mouseY)) { browserOpen = !browserOpen; return true }
        if (browserOpen) return onBrowserPress(mouseX, mouseY, screenW, modules)
        if (selectedId != null && mouseX >= screenW - PanelLayout.WIDTH) {
            val ctrl = PanelLayout.controls(screenW - PanelLayout.WIDTH, PanelLayout.TOP)
                .firstOrNull { it.rect.contains(mouseX, mouseY) }
            if (ctrl == null) { hexFocused = false; activeColor = null; return true }
            when {
                ctrl.id in PICKERS -> {
                    activeColor = ctrl.id; hexFocused = false
                    setPickerValue(ctrl.id, ctrl.rect, mouseX, mouseY); applyEditColor(modules)
                }
                ctrl.id == "hex" -> { activeColor = null; hexFocused = true; hexBuffer = "" }
                else -> { activeColor = null; hexFocused = false; applyControl(ctrl.id, modules); loadColor(modules) }
            }
            return true
        }
        val id = hitTest(boxes, mouseX, mouseY)
        val previous = selectedId
        selectedId = id
        if (id == null) { dragId = null; return false }
        val box = boxes.first { it.id == id }.rect
        dragId = id
        startMouseX = mouseX; startMouseY = mouseY
        startLeft = box.left; startTop = box.top
        dragW = box.width; dragH = box.height
        if (id != previous) loadColor(modules)
        return true
    }

    fun onDrag(mouseX: Int, mouseY: Int, screenW: Int, screenH: Int, modules: ModuleManager): Boolean {
        activeColor?.let { ac ->
            val rect = PanelLayout.controls(screenW - PanelLayout.WIDTH, PanelLayout.TOP).first { it.id == ac }.rect
            setPickerValue(ac, rect, mouseX, mouseY); applyEditColor(modules)
            return true
        }
        val id = dragId ?: return false
        val maxLeft = (screenW - dragW).coerceAtLeast(0)
        val maxTop = (screenH - 24 - dragH).coerceAtLeast(0) // keep elements above the bottom button bar
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
        val was = dragId != null || activeColor != null
        dragId = null
        activeColor = null
        return was
    }

    fun onCharTyped(ch: Char, modules: ModuleManager): Boolean {
        if (!hexFocused) return false
        if (hexBuffer.length < 8 && (ch in '0'..'9' || ch in 'a'..'f' || ch in 'A'..'F')) {
            hexBuffer += ch.uppercaseChar(); tryApplyHex(modules)
        }
        return true
    }

    fun onBackspace(modules: ModuleManager): Boolean {
        if (!hexFocused) return false
        if (hexBuffer.isNotEmpty()) { hexBuffer = hexBuffer.dropLast(1); tryApplyHex(modules) }
        return true
    }

    fun pruneSelection(boxes: List<ElementBox>) {
        val sel = selectedId ?: return
        if (boxes.none { it.id == sel }) { selectedId = null; dragId = null; activeColor = null }
    }

    private fun onBrowserPress(mouseX: Int, mouseY: Int, screenW: Int, modules: ModuleManager): Boolean {
        val ids = modules.all().map { it.id }
        val row = ModuleBrowserLayout.rows(screenW, ids).firstOrNull { it.second.contains(mouseX, mouseY) }
        if (row != null) {
            val module = modules.byId(row.first) ?: return true
            modules.setEnabled(row.first, !module.enabled)
            dirty = true
            if (module is HudModule) { selectedId = row.first; loadColor(modules); browserOpen = false }
            return true
        }
        if (ModuleBrowserLayout.panelRect(screenW, ids.size).contains(mouseX, mouseY)) return true // inside panel, not on a row
        browserOpen = false
        return false // click-away: close the browser and let the click select/drag the element under it
    }

    private fun setPickerValue(id: String, r: Rect, mx: Int, my: Int) {
        val fx = ((mx - r.left).toFloat() / r.width).coerceIn(0f, 1f)
        val fy = ((my - r.top).toFloat() / r.height).coerceIn(0f, 1f)
        when (id) {
            "sv" -> { editS = fx; editV = 1f - fy }
            "hue" -> editH = fy * 360f
            "alpha" -> editA = ((1f - fy) * 255f).roundToInt()
        }
    }

    private fun applyEditColor(modules: ModuleManager) {
        val sel = selectedId ?: return
        val color = MaeveColor.argb(editA, MaeveColor.hsvToRgb(editH, editS, editV))
        modules.updateStyle(sel) { it.copy(color = color) }
        dirty = true
    }

    private fun loadColor(modules: ModuleManager) {
        val c = selectedId?.let { modules.hudById(it)?.style?.color } ?: return
        val (h, s, v) = MaeveColor.rgbToHsv(MaeveColor.rgbOf(c))
        editH = h; editS = s; editV = v; editA = MaeveColor.alphaOf(c)
        hexFocused = false; hexBuffer = ""
    }

    private fun tryApplyHex(modules: ModuleManager) {
        val parsed = HexColor.decode(hexBuffer) ?: return
        val sel = selectedId ?: return
        // 6-digit codes keep the element's current alpha; 8-digit codes set alpha explicitly.
        val argb = if (hexBuffer.length == 6) MaeveColor.argb(editA, MaeveColor.rgbOf(parsed)) else parsed
        modules.updateStyle(sel) { it.copy(color = argb) }
        dirty = true
        val (h, s, v) = MaeveColor.rgbToHsv(MaeveColor.rgbOf(argb))
        editH = h; editS = s; editV = v; editA = MaeveColor.alphaOf(argb)
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

    private companion object {
        val PICKERS = setOf("sv", "hue", "alpha")
    }
}

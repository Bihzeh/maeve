package gg.maeve.mod.editor

import gg.maeve.mod.config.HexColor
import gg.maeve.mod.module.ModuleManager
import kotlin.math.roundToInt

/**
 * Pure, immediate-mode editor interaction state. Drives the editor screen from raw mouse/char
 * events without any Minecraft types, so it is fully unit-testable. Holds the live HSVA being
 * edited for the selected element's color (the SV square / hue / alpha bars / hex field all
 * read and write it); control mutations go through ModuleManager setters (live preview), and
 * the screen persists once on close.
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

    // color editor state
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
        if (selectedId != null && mouseX >= screenW - PanelLayout.WIDTH) {
            val ctrl = PanelLayout.controls(screenW - PanelLayout.WIDTH, PanelLayout.TOP)
                .firstOrNull { it.rect.contains(mouseX, mouseY) }
            if (ctrl == null) { hexFocused = false; return true }
            when {
                ctrl.id == "sv" || ctrl.id == "hue" || ctrl.id == "alpha" -> {
                    activeColor = ctrl.id; hexFocused = false
                    setPickerValue(ctrl.id, ctrl.rect, mouseX, mouseY); applyEditColor(modules)
                }
                ctrl.id == "hex" -> { hexFocused = true; hexBuffer = "" }
                else -> { hexFocused = false; applyControl(ctrl.id, modules); loadColor(modules) }
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
        val was = dragId != null || activeColor != null
        dragId = null
        activeColor = null
        return was
    }

    /** Hex field input. Returns true if the editor consumed the char. */
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
        val argb = HexColor.decode(hexBuffer) ?: return
        val sel = selectedId ?: return
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
}

package gg.maeve.mod.platform

import gg.maeve.mod.module.ModuleManager
import gg.maeve.mod.ui.ModMenuController
import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.resources.Identifier
import org.lwjgl.glfw.GLFW
import java.nio.file.Path

/**
 * The single file that touches Minecraft 26.2 + Fabric API. All symbol names here
 * were verified against the unobfuscated 26.2 jar (Mojang mappings). Rendering uses
 * the retained-mode GuiGraphicsExtractor (Blaze3D / Vulkan-safe), never raw GL.
 */
class FabricMinecraftBridge : MinecraftBridge {

    override fun configDir(): Path =
        FabricLoader.getInstance().configDir.resolve("maeve")

    override fun installHud(render: (HudCanvas, GameContext) -> Unit) {
        HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath("maeve", "hud"),
            HudElement { extractor, _ ->
                val mc = Minecraft.getInstance()
                render(ExtractorCanvas(extractor, mc.font), capture(mc))
            },
        )
    }

    override fun installMenuKeybind(onOpen: () -> Unit) {
        val key = KeyMappingHelper.registerKeyMapping(
            KeyMapping(
                "key.maeve.menu",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyMapping.Category.MISC,
            ),
        )
        ClientTickEvents.END_CLIENT_TICK.register(
            ClientTickEvents.EndTick { _ ->
                while (key.consumeClick()) onOpen()
            },
        )
    }

    override fun openModMenu(controller: ModMenuController) {
        Minecraft.getInstance().setScreenAndShow(MaeveMenuScreen(controller) { openHudEditor(controller.modules) })
    }

    override fun openHudEditor(modules: ModuleManager) {
        Minecraft.getInstance().setScreenAndShow(MaeveHudEditorScreen(modules, ::sampleContext))
    }

    private fun capture(mc: Minecraft): GameContext {
        val player = mc.player
        val pos = player?.position()
        val opts = mc.options
        return GameContext(
            fps = mc.fps,
            inWorld = player != null && mc.level != null,
            playerX = pos?.x ?: 0.0,
            playerY = pos?.y ?: 0.0,
            playerZ = pos?.z ?: 0.0,
            keyForward = opts.keyUp.isDown,
            keyBack = opts.keyDown.isDown,
            keyLeft = opts.keyLeft.isDown,
            keyRight = opts.keyRight.isDown,
        )
    }

    /** A context where every module renders (in-world coords substituted), so the editor can
     *  position elements even when not in a world. */
    private fun sampleContext(): GameContext {
        val real = capture(Minecraft.getInstance())
        return if (real.inWorld) real else real.copy(inWorld = true, playerX = 100.5, playerY = 64.0, playerZ = -200.5)
    }
}

/** HudCanvas backed by the 26.2 retained-mode extractor. Open so the editor canvas extends it. */
internal open class ExtractorCanvas(
    protected val extractor: GuiGraphicsExtractor,
    protected val font: Font,
) : HudCanvas {
    override fun drawText(x: Int, y: Int, text: String, color: Int) {
        extractor.text(font, text, x, y, color, true) // dropShadow = true
    }

    override fun drawStyledText(x: Int, y: Int, text: String, run: TextRun) {
        val style = Style.EMPTY
            .withColor(run.color and 0xFFFFFF)
            .withBold(run.bold)
            .withItalic(run.italic)
            .withUnderlined(run.underline)
            .withStrikethrough(run.strikethrough)
        // RGB on the Style + full color arg as fallback. MC's text color is RGB-only, so
        // text alpha is not honored here (background panels carry translucency instead).
        extractor.text(font, Component.literal(text).setStyle(style), x, y, run.color, run.shadow)
    }

    override fun fill(x: Int, y: Int, w: Int, h: Int, color: Int) {
        extractor.fill(x, y, x + w, y + h, color)
    }

    override fun withScale(scale: Float, pivotX: Int, pivotY: Int, body: () -> Unit) {
        val pose = extractor.pose()
        pose.pushMatrix()
        pose.translate(pivotX.toFloat(), pivotY.toFloat())
        if (scale != 1.0f) pose.scale(scale, scale)
        try {
            body()
        } finally {
            pose.popMatrix()
        }
    }

    override fun textWidth(text: String): Int = font.width(text)
    override val lineHeight: Int get() = font.lineHeight
    override val screenWidth: Int get() = extractor.guiWidth()
    override val screenHeight: Int get() = extractor.guiHeight()
}

/** Adds the editor overlay primitives to [ExtractorCanvas]. */
internal class EditorExtractorCanvas(
    extractor: GuiGraphicsExtractor,
    font: Font,
) : ExtractorCanvas(extractor, font), EditorCanvas {
    override fun border(x: Int, y: Int, w: Int, h: Int, color: Int) {
        extractor.outline(x, y, w, h, color)
    }

    override fun gradientV(x: Int, y: Int, w: Int, h: Int, top: Int, bottom: Int) {
        extractor.fillGradient(x, y, x + w, y + h, top, bottom)
    }

    override fun overlayStratum() {
        extractor.nextStratum()
    }
}

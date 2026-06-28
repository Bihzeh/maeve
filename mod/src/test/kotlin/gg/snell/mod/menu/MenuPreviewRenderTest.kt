package gg.snell.mod.menu

import gg.snell.mod.platform.EditorCanvas
import gg.snell.mod.platform.TextRun
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Font
import java.awt.GradientPaint
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Visual smoke render (not a behavioural assertion): rasterizes each bespoke menu via a Java2D
 * [EditorCanvas] (headless, no GL) so the launcher-matched chrome can be eyeballed without
 * launching the game. Writes PNGs under build/menu-preview/.
 */
class MenuPreviewRenderTest {
    private class AwtCanvas(val img: BufferedImage, override val screenWidth: Int, override val screenHeight: Int) : EditorCanvas {
        val g = img.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            font = Font("SansSerif", Font.PLAIN, 9)
        }
        private fun col(argb: Int) = Color(argb, true)
        override fun drawText(x: Int, y: Int, text: String, color: Int) {
            val fm = g.fontMetrics
            g.color = Color(0, 0, 0, 140); g.drawString(text, x + 1f, y + fm.ascent + 1f)
            g.color = col(color); g.drawString(text, x.toFloat(), y + fm.ascent.toFloat())
        }
        override fun drawStyledText(x: Int, y: Int, text: String, run: TextRun) = drawText(x, y, text, run.color)
        override fun fill(x: Int, y: Int, w: Int, h: Int, color: Int) {
            g.composite = AlphaComposite.SrcOver; g.color = col(color); g.fillRect(x, y, w, h)
        }
        override fun border(x: Int, y: Int, w: Int, h: Int, color: Int) { g.color = col(color); g.drawRect(x, y, w - 1, h - 1) }
        override fun gradientV(x: Int, y: Int, w: Int, h: Int, top: Int, bottom: Int) {
            g.paint = GradientPaint(x.toFloat(), y.toFloat(), col(top), x.toFloat(), (y + h).toFloat(), col(bottom))
            g.fillRect(x, y, w, h); g.paint = null
        }
        override fun withScale(scale: Float, pivotX: Int, pivotY: Int, body: () -> Unit) {
            val saved = g.transform
            val t = AffineTransform(saved); t.translate(pivotX.toDouble(), pivotY.toDouble()); t.scale(scale.toDouble(), scale.toDouble())
            g.transform = t; try { body() } finally { g.transform = saved }
        }
        override fun textWidth(text: String) = g.fontMetrics.stringWidth(text)
        override val lineHeight: Int get() = g.fontMetrics.height - 2
        override fun overlayStratum() {}
    }

    private fun frame(w: Int, h: Int): Pair<BufferedImage, AwtCanvas> {
        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        return img to AwtCanvas(img, w, h)
    }

    private fun write(img: BufferedImage, name: String): File {
        val out = File("build/menu-preview").apply { mkdirs() }
        val up = BufferedImage(img.width * 2, img.height * 2, BufferedImage.TYPE_INT_ARGB)
        up.createGraphics().apply { drawImage(img, AffineTransform.getScaleInstance(2.0, 2.0), null); dispose() }
        val f = File(out, name); ImageIO.write(up, "png", f); return f
    }

    @Test fun `render title screen`() {
        val w = 480; val h = 270
        val (img, canvas) = frame(w, h)
        // hover the Singleplayer (first) button to show the hover state
        val sp = TitleLayout.buttons(w, h).first().rect
        TitleRenderer.render(canvas, w, h, sp.left + sp.width / 2, sp.top + sp.height / 2)
        assertTrue(write(img, "01-title.png").length() > 0)
    }

    @Test fun `render pause menu`() {
        val w = 480; val h = 270
        val (img, canvas) = frame(w, h)
        canvas.gradientV(0, 0, w, h, 0xFF3A4A2A.toInt(), 0xFF12180C.toInt()) // fake world behind the scrim
        val opt = PauseLayout.buttons(w, h)[1].rect
        PauseRenderer.render(canvas, w, h, opt.left + opt.width / 2, opt.top + opt.height / 2)
        assertTrue(write(img, "02-pause.png").length() > 0)
    }
}

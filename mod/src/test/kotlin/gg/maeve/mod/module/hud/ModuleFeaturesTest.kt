package gg.maeve.mod.module.hud

import gg.maeve.mod.platform.GameContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun ctx(left: Int = 0, right: Int = 0, jump: Boolean = false, yaw: Float = 0f, inWorld: Boolean = true) =
    GameContext(60, inWorld, 0.0, 0.0, 0.0, false, false, false, false, yaw = yaw, leftCps = left, rightCps = right, keyJump = jump)

class ModuleFeaturesTest {
    @Test fun `keystrokes is a boxed module whose footprint grows with the spacebar`() {
        val m = KeystrokesModule()
        assertTrue(m.render(ctx()).isEmpty(), "boxed module has no text lines")
        m.setOption("space", false); val h0 = m.footprint(ctx())!!.h
        m.setOption("space", true); val h1 = m.footprint(ctx())!!.h
        assertTrue(h1 > h0, "spacebar adds a row")
    }

    @Test fun `keystrokes draws a slot per key`() {
        val m = KeystrokesModule().apply { setOption("space", false) }
        val canvas = gg.maeve.mod.FakeHudCanvas()
        gg.maeve.mod.render.HudModuleRender.draw(canvas, m, ctx())
        assertEquals(4, canvas.fills.size, "W A S D slots")
        assertEquals(setOf("W", "A", "S", "D"), canvas.draws.map { it.text }.toSet())
    }

    @Test fun `cps shows both buttons by default and left-only when right disabled`() {
        val m = CpsModule()
        assertEquals("CPS: 3 | 1", m.render(ctx(left = 3, right = 1))[0].text)
        m.setOption("right", false)
        assertEquals("CPS: 3", m.render(ctx(left = 3, right = 1))[0].text)
    }

    @Test fun `compass module renders the tape in-world and nothing otherwise`() {
        val m = CompassModule()
        assertTrue(m.enabled, "compass is on by default")
        assertEquals(2, m.render(ctx(yaw = 180f)).size, "tape + caret")
        assertTrue(m.render(ctx(inWorld = false)).isEmpty())
    }
}

class CompassFormatTest {
    @Test fun `tape and caret are equal width with a centred caret`() {
        val (tape, caret) = HudFormat.compass(180f).let { it[0] to it[1] }
        assertEquals(tape.length, caret.length)
        assertEquals('^', caret[caret.length / 2])
    }

    @Test fun `facing north puts N at the centre`() {
        // MC yaw 180 = facing north
        val tape = HudFormat.compass(180f)[0]
        assertEquals('N', tape[tape.length / 2])
    }

    @Test fun `facing east puts E at the centre`() {
        // MC yaw -90 = facing east
        val tape = HudFormat.compass(-90f)[0]
        assertEquals('E', tape[tape.length / 2])
    }
}

package gg.snell.mod.ui.node

import gg.snell.mod.editor.Point
import gg.snell.mod.editor.Rect
import gg.snell.mod.editor.Size
import gg.snell.mod.platform.EditorCanvas

/**
 * A declarative UI node tree for the bespoke Snell menus. ONE tree per screen is laid out once
 * ([Layout.layout]) and then drives drawing ([render]), hit-testing ([hit]) and scroll/drag geometry —
 * replacing the old `*Layout` (geometry) + `*Renderer` (draw) + hand-written `hit()` triplication where
 * the three copies drifted apart. Builders are pure (no Minecraft types) so screens unit-test + preview.
 */

/** Text metrics the layout pass needs for content (`Auto`) sizing. The canvas supplies these; tests stub them. */
interface Metrics {
    fun textWidth(s: String): Int
    fun monoWidth(s: String): Int
    fun displayWidth(s: String): Int
    val lineHeight: Int
}

/** Per-axis sizing rule. */
sealed interface Len {
    data class Fixed(val px: Int) : Len                                          // exact px
    data object Auto : Len                                                       // intrinsic (content / children)
    data class Frac(val f: Float, val min: Int = 0, val max: Int = Int.MAX_VALUE) : Len   // fraction of parent inner, clamped
    data class Flex(val weight: Int = 1, val min: Int = 0, val max: Int = Int.MAX_VALUE) : Len // share leftover / fill, clamped
}

enum class Dir { Row, Column, Stack }                 // Stack = overlap / anchored children
enum class Cross { Start, Center, End, Stretch }      // cross-axis alignment in a Row/Column
enum class Main { Start, Center, End }                // main-axis alignment (use Flex spacers for "between")
enum class Anchor { TopLeft, TopCenter, TopRight, CenterLeft, Center, CenterRight, BottomLeft, BottomCenter, BottomRight }

data class Edge(val l: Int = 0, val t: Int = 0, val r: Int = 0, val b: Int = 0) {
    companion object {
        fun all(v: Int) = Edge(v, v, v, v)
        fun xy(x: Int, y: Int) = Edge(x, y, x, y)
    }
}

/** Paint chrome into the measured [rect]; [mx]/[my] are the cursor (hover only — never geometry). */
fun interface Paint { fun draw(c: EditorCanvas, rect: Rect, mx: Int, my: Int) }

/** A virtualized list: only the rows intersecting the viewport are built/laid out during arrange. */
data class Lazy(
    val count: Int,
    val itemH: Int,
    val gap: Int,
    val scrollY: Int,
    val item: (index: Int) -> Node,
)

class Node(
    val id: String? = null,
    val dir: Dir = Dir.Stack,
    val width: Len = Len.Auto,
    val height: Len = Len.Auto,
    val padding: Edge = Edge(),
    val gap: Int = 0,
    val main: Main = Main.Start,
    val cross: Cross = Cross.Start,
    val anchor: Anchor = Anchor.TopLeft,   // honoured when the PARENT is a Stack
    val offset: Point = Point(0, 0),       // fine nudge for an anchored child
    val clip: Boolean = false,             // logical bounds: children outside don't draw/hit (lists, masked bands)
    val children: List<Node> = emptyList(),
    val paint: Paint? = null,
    val measure: ((Metrics) -> Size)? = null, // intrinsic size for Auto leaves (text/chip)
    val lazy: Lazy? = null,
) {
    var rect: Rect = Rect(0, 0, 0, 0)      // assigned by Layout.arrange()
    var lazyKids: List<Node> = emptyList() // rows built during arrange() when lazy != null
    internal var iw = 0
    internal var ih = 0
    fun kids(): List<Node> = if (lazy != null) lazyKids else children
}

/** Convenience: a flex spacer (absorbs leftover space in a Row/Column). */
fun spacer(weight: Int = 1): Node = Node(width = Len.Flex(weight), height = Len.Flex(weight))

/** A pure text-leaf size used by [measure] for `Auto` text nodes. */
fun textSize(m: Metrics, s: String): Size = Size(m.textWidth(s), m.lineHeight)

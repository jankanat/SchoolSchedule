package kz.alnazar.schedule.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Иконки из прототипа (контурные, 24×24, толщина 1.9). */
object Ic {
    private fun rr(x: Float, y: Float, w: Float, h: Float, r: Float) =
        "M${x + r} ${y}h${w - 2 * r}a$r $r 0 0 1 $r ${r}v${h - 2 * r}a$r $r 0 0 1 -$r ${r}h-${w - 2 * r}a$r $r 0 0 1 -$r -${r}v-${h - 2 * r}a$r $r 0 0 1 $r -${r}z"

    private fun circ(cx: Float, cy: Float, r: Float) = "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 -${2 * r} 0"

    private fun icon(name: String, vararg paths: String): ImageVector {
        val b = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        paths.forEach { d ->
            b.addPath(
                pathData = addPathNodes(d), fill = null, stroke = SolidColor(Color.White),
                strokeLineWidth = 1.9f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
            )
        }
        return b.build()
    }

    val home = icon("home", "M3.5 11.2 12 4l8.5 7.2", "M5.5 9.8V20h13V9.8", "M10 20v-5.5h4V20")
    val cal = icon("cal", rr(3.5f, 5f, 17f, 15.5f, 3.5f), "M3.5 10h17M8 3v4M16 3v4")
    val wid = icon("wid", rr(3.5f, 3.5f, 7f, 7f, 2f), rr(13.5f, 3.5f, 7f, 7f, 2f), rr(3.5f, 13.5f, 7f, 7f, 2f), rr(13.5f, 13.5f, 7f, 7f, 2f))
    val hw = icon("hw", "M5 4.5h11a3 3 0 0 1 3 3V20H8a3 3 0 0 1-3-3z", "M9 9h6M9 13h4")
    val set = icon("set", "M4 7h9M17 7h3M4 17h3M11 17h9", circ(15f, 7f, 2.2f), circ(9f, 17f, 2.2f))
    val plus = icon("plus", "M12 5v14M5 12h14")
    val cam = icon("cam", "M4 8h3l1.5-2h7L17 8h3v11H4z", circ(12f, 13f, 3.5f))
    val trash = icon("trash", "M5 7h14M9 7V4.5h6V7M7 7l1 13h8l1-13")
    val chevL = icon("chevL", "M15 6l-6 6 6 6")
    val chevR = icon("chevR", "M9 6l6 6-6 6")
    val bell = icon("bell", "M6 16V11a6 6 0 0 1 12 0v5l1.5 2h-15z", "M10 20.5a2 2 0 0 0 4 0")
    val more = icon("more", circ(12f, 5.5f, 1.1f), circ(12f, 12f, 1.1f), circ(12f, 18.5f, 1.1f))
    val gear = icon("gear", circ(12f, 12f, 3f),
        "M12.2 2h-.4a2 2 0 0 0-2 2v.2a2 2 0 0 1-1 1.7l-.4.3a2 2 0 0 1-2 0l-.2-.1a2 2 0 0 0-2.7.7l-.2.4a2 2 0 0 0 .7 2.7l.2.1a2 2 0 0 1 1 1.7v.5a2 2 0 0 1-1 1.7l-.2.1a2 2 0 0 0-.7 2.7l.2.4a2 2 0 0 0 2.7.7l.2-.1a2 2 0 0 1 2 0l.4.3a2 2 0 0 1 1 1.7v.2a2 2 0 0 0 2 2h.4a2 2 0 0 0 2-2v-.2a2 2 0 0 1 1-1.7l.4-.3a2 2 0 0 1 2 0l.2.1a2 2 0 0 0 2.7-.7l.2-.4a2 2 0 0 0-.7-2.7l-.2-.1a2 2 0 0 1-1-1.7v-.5a2 2 0 0 1 1-1.7l.2-.1a2 2 0 0 0 .7-2.7l-.2-.4a2 2 0 0 0-2.7-.7l-.2.1a2 2 0 0 1-2 0l-.4-.3a2 2 0 0 1-1-1.7V4a2 2 0 0 0-2-2z")
    val edit = icon("edit", "M4 20h4L19 9l-4-4L4 16z", "M13.5 6.5l4 4")
    val restore = icon("restore", "M4 12a8 8 0 1 0 2.3-5.6", "M4 4v4h4")
    val clock = icon("clock", circ(12f, 12f, 8f), "M12 8v4l3 2")
    val check = icon("check", "M5 12.5l4.5 4.5L19 7.5")
    val close = icon("close", "M6 6l12 12M18 6L6 18")
    val image = icon("image", rr(3.5f, 4.5f, 17f, 15f, 3.5f), circ(9f, 9.5f, 1.6f), "M4 17l5-4.5 4 3.5 3-2.5 4.5 4")
    val save = icon("save", "M12 4v11", "M7.5 10.5 12 15l4.5-4.5", "M5 19.5h14")
    val folder = icon("folder", "M3.5 7.5a2 2 0 0 1 2-2h4l2 2h7a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2h-13a2 2 0 0 1-2-2z")
    val clip = icon("clip", rr(6f, 4.5f, 12f, 16f, 2.5f), "M9.5 4.5V3.5h5v1", "M9 10h6M9 14h4")
}

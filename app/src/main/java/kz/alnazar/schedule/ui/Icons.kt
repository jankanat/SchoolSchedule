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
}

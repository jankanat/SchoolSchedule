package kz.alnazar.schedule.ui

import android.graphics.BlurMaskFilter
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb

/**
 * «Обои» стилей, которые рисуются кодом: OriginOS 7 (тёплые дуги) и One UI 9.5 (мягкий сиреневый изгиб).
 * Рисуются в координатах экрана (w×h); boost — ярче (для стеклянных цифр, сквозь которые видно фон).
 */
fun DrawScope.drawStyleWall(id: String, w: Float, h: Float, boost: Float = 1f, sharp: Boolean = true) {
    val sx = w / 412f; val sy = h / 915f
    fun p(x: Float, y: Float) = Offset(x * sx, y * sy)
    when (id) {
        "origin" -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF1B1009), Color(0xFF120B07), Color(0xFF0D0906)), 0f, h))
            drawRect(Brush.radialGradient(listOf(Color(0xB35A2E12), Color.Transparent), p(206f, 350f), 230f * sx))
            // верхняя дуга опускается по краям примерно до середины экрана, нижняя чаша — заметно ниже: между ними зазор
            val top = Path().apply { moveTo(p(-40f, 420f).x, p(-40f, 420f).y); cubicTo(p(0f, 90f).x, p(0f, 90f).y, p(412f, 90f).x, p(412f, 90f).y, p(452f, 420f).x, p(452f, 420f).y) }
            val bot = Path().apply { moveTo(p(-40f, 600f).x, p(-40f, 600f).y); cubicTo(p(10f, 960f).x, p(10f, 960f).y, p(402f, 960f).x, p(402f, 960f).y, p(452f, 600f).x, p(452f, 600f).y) }
            glowStroke(top, Color(0xFFFF9A4A), 36f * sx, 28f * sx, .45f * boost)
            if (sharp) glowStroke(top, Color(0xFFFFC58A), 3f * sx, 6f * sx, .8f * boost)
            glowStroke(bot, Color(0xFFFF8A3A), 30f * sx, 28f * sx, .35f * boost)
            if (sharp) glowStroke(bot, Color(0xFFFFB27A), 2.5f * sx, 6f * sx, .6f * boost)
        }
        "oneui" -> {
            drawRect(Brush.linearGradient(listOf(Color(0xFF1D1838), Color(0xFF272056), Color(0xFF1A1735)), Offset(0f, 0f), Offset(w * .3f, h)))
            val s = Path().apply {
                moveTo(p(120f, -40f).x, p(120f, -40f).y)
                cubicTo(p(360f, 180f).x, p(360f, 180f).y, p(-60f, 420f).x, p(-60f, 420f).y, p(230f, 620f).x, p(230f, 620f).y)
                cubicTo(p(400f, 740f).x, p(400f, 740f).y, p(420f, 900f).x, p(420f, 900f).y, p(520f, 960f).x, p(520f, 960f).y)
            }
            glowStroke(s, Color(0xFF8F7DFF), 140f * sx, 60f * sx, .35f * boost)
            val s2 = Path().apply { moveTo(p(420f, 120f).x, p(420f, 120f).y); cubicTo(p(260f, 260f).x, p(260f, 260f).y, p(420f, 420f).x, p(420f, 420f).y, p(300f, 560f).x, p(300f, 560f).y) }
            glowStroke(s2, Color(0xFFC7B6FF), 90f * sx, 60f * sx, .18f * boost)
        }
    }
}

private fun DrawScope.glowStroke(path: Path, color: Color, width: Float, blur: Float, alpha: Float) {
    drawIntoCanvas { c ->
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = width
            strokeCap = android.graphics.Paint.Cap.ROUND
            this.color = color.copy(alpha = alpha.coerceIn(0f, 1f)).toArgb()
            if (blur > 0f) maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL)
        }
        c.nativeCanvas.drawPath(path.asAndroidPath(), paint)
    }
}

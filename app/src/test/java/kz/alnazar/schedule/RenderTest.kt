package kz.alnazar.schedule

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.test.core.app.ApplicationProvider
import kz.alnazar.schedule.data.AppState
import kz.alnazar.schedule.data.Lesson
import kz.alnazar.schedule.logic.Now
import kz.alnazar.schedule.logic.Phase
import kz.alnazar.schedule.logic.Slot
import kz.alnazar.schedule.widget.WRender
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

/** Рисует все виджеты в картинки, чтобы сверить с макетами (запускается в CI). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
class RenderTest {
    private val lessons = listOf(
        Triple("hist_kz", "215", 480 to 525), Triple("rus", "121", 530 to 575), Triple("algebra", "304", 580 to 625),
        Triple("eng", "210", 635 to 680), Triple("pe", "спортзал", 690 to 735),
    )
    private fun slots(d: LocalDate) = lessons.mapIndexed { i, (sj, r, t) -> Slot(i, Lesson(sj, r), t.first, t.second, false, 3, d) }

    private fun nowOf(phase: Phase): Now {
        val d = LocalDate.of(2026, 11, 19)
        val sl = slots(d)
        return when (phase) {
            Phase.LESSON -> Now(d, 8 * 60 + 12, Phase.LESSON, sl, cur = 0, next = 1, prev = -1, minutesLeft = 33)
            Phase.BREAK -> Now(d, 8 * 60 + 47, Phase.BREAK, sl, prev = 0, next = 1, minutesLeft = 3)
            else -> Now(d, 14 * 60, Phase.AFTER, sl, nextDay = d.plusDays(1), nextDayLessons = slots(d.plusDays(1)))
        }
    }

    private val sizes = mapOf("2x2" to (183f to 183f), "2x1" to (183f to 90f), "1x2" to (85f to 196f), "2x4" to (183f to 408f), "4x2" to (380f to 196f))

    @Test
    fun render() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val out = File("build/render"); out.mkdirs()
        val styles = listOf("color", "ios26", "pixel", "origin", "oneui")
        val walls = mapOf("color" to (0xFF0F5132.toInt() to 0xFF000000.toInt()), "ios26" to (0xFF4B3A8F.toInt() to 0xFF8F3A6E.toInt()),
            "pixel" to (0xFF1D2B3A.toInt() to 0xFF11141B.toInt()), "origin" to (0xFF3A2414.toInt() to 0xFF0D0906.toInt()), "oneui" to (0xFF3A2A6B.toInt() to 0xFF1A1236.toInt()))
        val d = ctx.resources.displayMetrics.density
        for (style in styles) for (ph in listOf(Phase.LESSON, Phase.BREAK, Phase.AFTER)) {
            val s = AppState(style = style, iconSet = "fluent")
            val n = nowOf(ph)
            val board = Bitmap.createBitmap((412 * d).toInt(), (760 * d).toInt(), Bitmap.Config.ARGB_8888)
            val c = Canvas(board)
            val wp = Paint().apply { shader = LinearGradient(0f, 0f, 0f, board.height.toFloat(), walls.getValue(style).first, walls.getValue(style).second, Shader.TileMode.CLAMP) }
            c.drawRect(0f, 0f, board.width.toFloat(), board.height.toFloat(), wp)
            val pos = mapOf("4x2" to (16f to 16f), "2x4" to (16f to 228f), "2x2" to (214f to 228f), "1x2" to (214f to 426f), "2x1" to (16f to 652f))
            for ((sz, wh) in sizes) {
                val b = WRender.render(ctx, s, n, sz, wh.first, wh.second)
                val (x, y) = pos.getValue(sz)
                c.drawBitmap(b, x * d, y * d, null)
            }
            FileOutputStream(File(out, "board_${style}_${ph.name.lowercase()}.png")).use { board.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}

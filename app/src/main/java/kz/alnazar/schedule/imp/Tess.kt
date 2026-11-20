package kz.alnazar.schedule.imp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import com.googlecode.tesseract.android.TessBaseAPI
import kz.alnazar.schedule.data.Bell
import java.io.File

/** Распознавание на телефоне без интернета (Tesseract, русский + казахский + английский). */
object Tess {
    private class W(val t: String, val r: Rect) { val cx get() = (r.left + r.right) / 2f; val cy get() = (r.top + r.bottom) / 2f }

    private fun dataDir(ctx: Context): File {
        val dir = File(ctx.filesDir, "tess/tessdata").apply { mkdirs() }
        for (l in listOf("rus", "kaz", "eng")) {
            val f = File(dir, "$l.traineddata")
            if (!f.exists() || f.length() < 100_000) runCatching {
                ctx.assets.open("tessdata/$l.traineddata").use { i -> f.outputStream().use { o -> i.copyTo(o) } }
            }
        }
        return dir.parentFile!!
    }

    private val DAYS = listOf(
        listOf("пн", "пон", "дс", "дүй", "дуй", "mon"), listOf("вт", "втор", "сс", "сей", "tue"), listOf("ср", "сре", "сәр", "сар", "wed"),
        listOf("чт", "чет", "бс", "бей", "thu"), listOf("пт", "пят", "жм", "жұм", "жум", "fri"), listOf("сб", "суб", "сн", "сен", "sat"),
    )
    private fun dayOf(t: String): Int? {
        val n = t.lowercase().filter { it.isLetter() }
        if (n.length < 2) return null
        for ((i, keys) in DAYS.withIndex()) if (keys.any { n == it || (n.length >= 3 && n.startsWith(it)) }) return i
        return null
    }

    fun recognize(ctx: Context, bmp: Bitmap): ImpResult {
        val api = TessBaseAPI()
        val words = ArrayList<W>()
        try {
            if (!api.init(dataDir(ctx).absolutePath, "rus+kaz+eng")) throw IllegalStateException("tess init")
            api.pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO
            api.setImage(bmp)
            api.utF8Text
            val it = api.resultIterator
            it.begin()
            do {
                val t = it.getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_WORD)?.trim().orEmpty()
                val r = it.getBoundingRect(TessBaseAPI.PageIteratorLevel.RIL_WORD)
                if (t.isNotEmpty() && it.confidence(TessBaseAPI.PageIteratorLevel.RIL_WORD) > 25f) words += W(t, r)
            } while (it.next(TessBaseAPI.PageIteratorLevel.RIL_WORD))
            it.delete()
        } finally { api.recycle() }
        return parse(words)
    }

    private fun parse(words: List<W>): ImpResult {
        // 1) строка с названиями дней: берём ту горизонталь, где найдено больше всего разных дней
        val heads = words.mapNotNull { w -> dayOf(w.t)?.let { w to it } }
        if (heads.size < 2) return ImpResult(emptyList(), emptyList())
        val medH = words.map { it.r.height() }.sorted()[words.size / 2].coerceAtLeast(8)
        val rows = heads.groupBy { (it.first.cy / (medH * 2)).toInt() }
        val hRow = rows.values.maxByOrNull { g -> g.map { it.second }.distinct().size } ?: return ImpResult(emptyList(), emptyList())
        val cols = hRow.distinctBy { it.second }.sortedBy { it.first.cx }
        if (cols.size < 2) return ImpResult(emptyList(), emptyList())
        val headBottom = hRow.maxOf { it.first.r.bottom }
        val centers = cols.map { it.first.cx }
        val colW = centers.zipWithNext { a, b -> b - a }.sorted().let { it[it.size / 2] }
        val bounds = centers.mapIndexed { i, c -> (if (i == 0) c - colW / 2 else (centers[i - 1] + c) / 2) to (if (i == centers.size - 1) c + colW / 2 else (c + centers[i + 1]) / 2) }
        val dayIdx = cols.map { it.second }

        // 2) слова тела таблицы по колонкам, в каждой колонке — склейка строк в ячейки по вертикальным промежуткам
        val body = words.filter { it.cy > headBottom + medH / 2 }
        val days = Array(6) { ArrayList<ImpCell>() }
        for ((ci, di) in dayIdx.withIndex()) {
            val (l, r) = bounds[ci]
            val col = body.filter { it.cx in l..r }.sortedBy { it.cy }
            if (col.isEmpty()) continue
            val lines = ArrayList<ArrayList<W>>()
            for (w in col) {
                val last = lines.lastOrNull()
                if (last != null && kotlin.math.abs(w.cy - last.map { it.cy }.average()) < medH * .7) last += w else lines += arrayListOf(w)
            }
            val cells = ArrayList<ArrayList<ArrayList<W>>>()
            for (ln in lines) {
                val y = ln.map { it.cy }.average()
                val prev = cells.lastOrNull()?.lastOrNull()?.map { it.cy }?.average()
                if (prev != null && y - prev < medH * 1.6) cells.last() += ln else cells += arrayListOf(ln)
            }
            for (c in cells) {
                val txt = c.joinToString(" ") { ln -> ln.sortedBy { it.cx }.joinToString(" ") { it.t } }
                days[di] += splitRoom(txt)
            }
        }
        val used = days.indexOfLast { it.isNotEmpty() } + 1
        // 3) звонки: время слева от первой колонки («08:00 – 08:45»)
        val left = body.filter { it.cx < bounds.first().first }.sortedBy { it.cy }
        val times = Regex("""\d{1,2}[:.]\d{2}""").findAll(left.joinToString(" ") { it.t }).mapNotNull { Gemini.hm(it.value) }.toList()
        val bells = if (times.size >= 2 && times.size % 2 == 0) times.chunked(2).mapNotNull { (a, b) -> if (b > a) Bell(a, b) else null } else emptyList()
        return ImpResult(days.take(maxOf(used, 5)).map { it.toList() }, bells)
    }

    private fun splitRoom(t: String): ImpCell {
        val parts = t.trim().split(Regex("\\s+")).toMutableList()
        var room = ""
        val rx = Regex("""^(каб\.?|№)?\d{1,3}[а-яa-z]?$""", RegexOption.IGNORE_CASE)
        while (parts.size > 1 && (rx.matches(parts.last()) || parts.last().equals("спортзал", true) || parts.last().equals("спортзал.", true))) {
            room = parts.removeLast().removePrefix("каб.").removePrefix("№") + if (room.isEmpty()) "" else " $room"
        }
        return ImpCell(parts.joinToString(" ").trim(' ', '.', ',', '|', '-'), room.trim())
    }
}

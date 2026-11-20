package kz.alnazar.schedule.imp

import kz.alnazar.schedule.data.*

/** Один распознанный урок: название как на фото и кабинет. Правится в предпросмотре. */
data class ImpCell(val name: String, val room: String = "")

/** Результат распознавания: уроки по дням (пн..сб) и, если нашлись, звонки. */
data class ImpResult(val days: List<List<ImpCell>>, val bells: List<Bell>)

object ImpMatch {
    private val subs get() = Catalog.subjects

    fun norm(s: String): String = s.lowercase().replace('ё', 'е').filter { it.isLetterOrDigit() }

    private fun lev(a: String, b: String): Int {
        if (a == b) return 0
        val d = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prev = d[0]; d[0] = i
            for (j in 1..b.length) {
                val t = d[j]
                d[j] = minOf(d[j] + 1, d[j - 1] + 1, prev + if (a[i - 1] == b[j - 1]) 0 else 1)
                prev = t
            }
        }
        return d[b.length]
    }

    /** id предмета каталога или своего предмета, подходящий под название на фото; null — не нашли. */
    fun find(raw: String, custom: List<CustomSubject>): String? {
        val n = norm(raw)
        if (n.length < 2) return null
        custom.firstOrNull { norm(it.name) == n }?.let { return it.id }
        var best: String? = null; var bestScore = 0.0
        for (s in subs) {
            for (nm in listOf(s.ru, s.kk, s.en)) {
                val m = norm(nm)
                if (m == n) return s.id
                val score = 1.0 - lev(n, m).toDouble() / maxOf(n.length, m.length)
                if (score > bestScore) { bestScore = score; best = s.id }
            }
        }
        // короткие сокращения вроде «физ-ра», «инф», «лит-ра»
        if (bestScore < .72) {
            val pre = subs.firstOrNull { s -> listOf(s.ru, s.kk).any { norm(it).startsWith(n) } && n.length >= 3 }
            if (pre != null) return pre.id
            val aliases = mapOf("физра" to "pe", "физкультура" to "pe", "физо" to "pe", "лит" to "literature", "литра" to "literature", "инф" to "inf", "хим" to "chem", "био" to "bio", "геогр" to "geo", "мат" to "math", "рус" to "rus", "англ" to "eng", "каз" to "kaz", "обж" to "obzh", "нвп" to "nvp", "изо" to "art", "кл.час" to "cl", "клчас" to "cl")
            aliases[n]?.let { return it }
        }
        return if (bestScore >= .72) best else null
    }
}

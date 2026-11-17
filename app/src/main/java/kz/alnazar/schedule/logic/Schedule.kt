package kz.alnazar.schedule.logic

import kz.alnazar.schedule.data.AppState
import kz.alnazar.schedule.data.Bell
import kz.alnazar.schedule.data.Catalog
import kz.alnazar.schedule.data.Lesson
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

/** Урок конкретного дня: с учётом замен недели и звонков текущей смены. */
data class Slot(
    val idx: Int,
    val lesson: Lesson,
    val start: Int,
    val end: Int,
    val replaced: Boolean,
    val dayIdx: Int = 0,
)

/** Отображаемые данные предмета: название, эмодзи, цвет. */
data class SubjView(val name: String, val emoji: String, val color: Long)

enum class Phase { LESSON, BREAK, BEFORE, AFTER, DAYOFF, EMPTY }

/** Всё, что нужно и главному экрану, и виджетам, — одна модель на всех. */
data class Now(
    val date: LocalDate,
    val minute: Int,
    val phase: Phase,
    val today: List<Slot>,
    val cur: Int = -1,
    val next: Int = -1,
    val prev: Int = -1,
    val minutesLeft: Int = 0,
    val nextDay: LocalDate? = null,
    val nextDayLessons: List<Slot> = emptyList(),
)

object Sched {
    fun weekKey(d: LocalDate): String = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()

    fun subj(st: AppState, id: String, lang: String): SubjView {
        Catalog.find(id)?.let { return SubjView(it.name(lang), it.emoji, it.color) }
        st.custom.firstOrNull { it.id == id }?.let { return SubjView(it.name, it.emoji, it.color) }
        return SubjView(id, "📘", 0xFF8E8E93)
    }

    /** Уроки на дату: постоянное расписание дня + замены этой недели. */
    fun dayLessons(st: AppState, d: LocalDate): List<Slot> {
        val dow = d.dayOfWeek.value - 1
        if (dow >= st.schoolDays) return emptyList()
        val base = st.week.getOrNull(dow) ?: emptyList()
        val wk = weekKey(d)
        val ov = st.overrides.filter { it.week == wk && it.day == dow }
        val n = maxOf(base.size, (ov.maxOfOrNull { it.idx } ?: -1) + 1)
        val bells: List<Bell> = st.bells
        val out = ArrayList<Slot>()
        for (i in 0 until n) {
            val o = ov.firstOrNull { it.idx == i }
            val lesson: Lesson? = if (o != null) o.lesson else base.getOrNull(i)
            if (lesson == null) continue
            val b = bells.getOrNull(i) ?: continue
            out += Slot(i, lesson, b.start, b.end, o != null, dow)
        }
        return out
    }

    /**
     * «в том же кабинете» — если предыдущий урок дня был в этом же кабинете.
     * Числовой кабинет показываем как «каб. 204», текстовый («Спортзал») — как есть.
     */
    fun roomLabel(list: List<Slot>, pos: Int, lang: String): String {
        val r = list.getOrNull(pos)?.lesson?.room?.trim().orEmpty()
        if (r.isEmpty()) return ""
        if (pos > 0 && list[pos - 1].lesson.room.trim() == r) return Tr.t(lang, "sameRoom")
        return if (r.first().isDigit()) Tr.t(lang, "room", r) else r
    }

    /**
     * Ближайший учебный день после сегодняшнего:
     * в пятницу/субботу после уроков — понедельник, в воскресенье — завтра.
     */
    fun nextSchoolDay(st: AppState, from: LocalDate): LocalDate? {
        var d = from.plusDays(1)
        repeat(21) {
            if (dayLessons(st, d).isNotEmpty()) return d
            d = d.plusDays(1)
        }
        return null
    }

    fun now(st: AppState, t: LocalDateTime = LocalDateTime.now()): Now {
        val d = t.toLocalDate()
        val m = t.hour * 60 + t.minute
        val list = dayLessons(st, d)
        fun idle(p: Phase): Now {
            val nd = nextSchoolDay(st, d)
            return Now(d, m, p, list, nextDay = nd, nextDayLessons = nd?.let { dayLessons(st, it) } ?: emptyList())
        }
        if (d.dayOfWeek.value - 1 >= st.schoolDays) return idle(Phase.DAYOFF)
        if (list.isEmpty()) return idle(Phase.EMPTY)
        list.forEachIndexed { i, s ->
            if (m >= s.start && m < s.end)
                return Now(d, m, Phase.LESSON, list, cur = i, next = if (i + 1 < list.size) i + 1 else -1, prev = i - 1, minutesLeft = s.end - m)
        }
        if (m < list.first().start) return Now(d, m, Phase.BEFORE, list, next = 0, minutesLeft = list.first().start - m)
        for (i in 0 until list.size - 1) {
            if (m >= list[i].end && m < list[i + 1].start)
                return Now(d, m, Phase.BREAK, list, prev = i, next = i + 1, minutesLeft = list[i + 1].start - m)
        }
        return idle(Phase.AFTER)
    }

    /** Ближайший момент, когда картинка виджета должна смениться (начало/конец урока или полночь). */
    fun nextBoundary(st: AppState, t: LocalDateTime = LocalDateTime.now()): LocalDateTime {
        val d = t.toLocalDate()
        val m = t.hour * 60 + t.minute
        val marks = dayLessons(st, d).flatMap { listOf(it.start, it.end) }.filter { it > m }.sorted()
        return if (marks.isNotEmpty()) d.atStartOfDay().plusMinutes(marks.first().toLong())
        else d.plusDays(1).atStartOfDay().plusMinutes(1)
    }

    /** Подпись дня без уроков: эмодзи, заголовок и ближайший учебный день. */
    data class IdleInfo(val em: String, val title: String, val day: String)

    fun idle(n: Now, lang: String): IdleInfo {
        val (em, key) = when (n.phase) {
            Phase.AFTER -> "✅" to "doneToday"
            Phase.DAYOFF -> "😴" to "weekend"
            else -> "💤" to "noLessons"
        }
        return IdleInfo(em, Tr.t(lang, key), n.nextDay?.let { dayLabel(lang, n.date, it) } ?: "")
    }

    /** «08:30–09:15 · каб. 204» с учётом настроек показа времени и кабинета. */
    fun meta(list: List<Slot>, pos: Int, lang: String, time: Boolean = true, room: Boolean = true, startOnly: Boolean = false): String {
        val s = list[pos]
        val p = ArrayList<String>()
        if (time) p += if (startOnly) Tr.hm(s.start) else Tr.hm(s.start) + "–" + Tr.hm(s.end)
        if (room) roomLabel(list, pos, lang).takeIf { it.isNotEmpty() }?.let { p += it }
        return p.joinToString(" · ")
    }

    fun dayLabel(lang: String, today: LocalDate, d: LocalDate): String =
        if (d == today.plusDays(1)) Tr.t(lang, "tomorrow")
        else Tr.dayLong(lang, d.dayOfWeek.value - 1)
}

package kz.alnazar.schedule.logic

import kz.alnazar.schedule.data.AppState
import kz.alnazar.schedule.data.Bell
import kz.alnazar.schedule.data.Catalog
import kz.alnazar.schedule.data.Lesson
import kz.alnazar.schedule.data.Holiday
import kz.alnazar.schedule.data.Hw
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
    val date: LocalDate? = null,
)

/** Урок дня для экрана «Расписание»: с отменёнными уроками и тем, что стояло до замены. */
data class DaySlot(val slot: Slot, val cancelled: Boolean, val wasSubj: String?)

/** Сводка по дню недели для полосы дней. */
data class DayInfo(val date: LocalDate, val count: Int, val hw: Int, val changed: Boolean, val holiday: Boolean)

/** Отображаемые данные предмета: название, эмодзи, цвет. */
data class SubjView(val name: String, val emoji: String, val color: Long, val img: String? = null)

enum class Phase { LESSON, BREAK, BEFORE, AFTER, DAYOFF, EMPTY, HOLIDAY }

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
    val holiday: Holiday? = null,
)

object Sched {
    fun weekKey(d: LocalDate): String = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()

    fun subj(st: AppState, id: String, lang: String): SubjView {
        Catalog.find(id)?.let { return SubjView(it.name(lang), it.emoji, it.color) }
        st.custom.firstOrNull { it.id == id }?.let { return SubjView(it.name, if (it.img != null) "img:" + it.img else it.emoji, it.color, it.img) }
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
            out += Slot(i, lesson, b.start, b.end, o != null, dow, d)
        }
        return out
    }

    /** Все уроки дня, включая отменённые на эту неделю (для экрана «Расписание»). */
    fun dayAll(st: AppState, d: LocalDate): List<DaySlot> {
        val dow = d.dayOfWeek.value - 1
        if (dow >= st.schoolDays) return emptyList()
        val base = st.week.getOrNull(dow) ?: emptyList()
        val wk = weekKey(d)
        val ov = st.overrides.filter { it.week == wk && it.day == dow }
        val n = maxOf(base.size, (ov.maxOfOrNull { it.idx } ?: -1) + 1)
        val out = ArrayList<DaySlot>()
        for (i in 0 until n) {
            val o = ov.firstOrNull { it.idx == i }
            val b = base.getOrNull(i)
            val bell = st.bells.getOrNull(i) ?: continue
            when {
                o == null && b != null -> out += DaySlot(Slot(i, b, bell.start, bell.end, false, dow, d), false, null)
                o != null && o.lesson != null -> out += DaySlot(Slot(i, o.lesson, bell.start, bell.end, true, dow, d), false, b?.subj?.takeIf { it != o.lesson.subj })
                o != null && b != null -> out += DaySlot(Slot(i, b, bell.start, bell.end, true, dow, d), true, null)
            }
        }
        return out
    }

    /** Понедельник недели, в которую входит дата. */
    fun monday(d: LocalDate): LocalDate = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /** Учебные дни недели с числом уроков, заданий и отметкой изменений. */
    fun week(st: AppState, anyDay: LocalDate): List<DayInfo> {
        val mon = monday(anyDay)
        val wk = weekKey(mon)
        return (0 until st.schoolDays).map { i ->
            val d = mon.plusDays(i.toLong())
            val hol = holiday(st, d) != null
            val k = d.toString()
            DayInfo(
                d, if (hol) 0 else dayLessons(st, d).size,
                st.hw.count { it.kind == "hw" && !it.done && it.due == k },
                st.overrides.any { it.week == wk && it.day == i }, hol,
            )
        }
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
    fun holiday(st: AppState, d: LocalDate): Holiday? {
        val k = d.toString()
        return st.holidays.firstOrNull { k >= it.from && k <= it.to }
    }

    fun nextSchoolDay(st: AppState, from: LocalDate): LocalDate? {
        var d = from.plusDays(1)
        repeat(120) {
            if (holiday(st, d) == null && dayLessons(st, d).isNotEmpty()) return d
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
        holiday(st, d)?.let { h -> return idle(Phase.HOLIDAY).copy(today = emptyList(), holiday = h) }
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
        val marks = (if (holiday(st, d) != null) emptyList() else dayLessons(st, d)).flatMap { listOf(it.start, it.end) }.filter { it > m }.sorted()
        return if (marks.isNotEmpty()) d.atStartOfDay().plusMinutes(marks.first().toLong())
        else d.plusDays(1).atStartOfDay().plusMinutes(1)
    }

    /** Подпись дня без уроков: эмодзи, заголовок и ближайший учебный день. */
    data class IdleInfo(val em: String, val title: String, val day: String, val sub: String = "")

    fun idle(n: Now, lang: String): IdleInfo {
        val (em, key) = when (n.phase) {
            Phase.AFTER -> "✅" to "doneToday"
            Phase.DAYOFF -> "😴" to "weekend"
            Phase.HOLIDAY -> "🏖️" to "holiday"
            else -> "💤" to "noLessons"
        }
        val sub = n.holiday?.let { Tr.t(lang, "holUntil", Tr.dateShort(lang, LocalDate.parse(it.to))) } ?: ""
        return IdleInfo(em, Tr.t(lang, key), n.nextDay?.let { dayLabel(lang, n.date, it) } ?: "", sub)
    }

    /* ===== Домашка ===== */

    /** Есть ли невыполненная домашка по предмету к этой дате. */
    fun hasHw(st: AppState, subj: String, d: LocalDate): Boolean {
        val k = d.toString()
        return st.hw.any { it.kind == "hw" && !it.done && it.subj == subj && it.due == k }
    }

    /** Отметка 📝 для урока (если включена в настройках виджета). */
    fun hwMark(st: AppState, s: Slot): String =
        if (st.widget.hw && s.date != null && hasHw(st, s.lesson.subj, s.date)) " 📝" else ""

    /** Срок по умолчанию — ближайший следующий урок этого предмета (не сегодня). */
    fun nextLessonDate(st: AppState, subj: String, from: LocalDate = LocalDate.now()): LocalDate? {
        var d = from.plusDays(1)
        repeat(120) {
            if (holiday(st, d) == null && dayLessons(st, d).any { it.lesson.subj == subj }) return d
            d = d.plusDays(1)
        }
        return null
    }

    /** Активные задания, отсортированные по сроку. */
    fun todo(st: AppState): List<Hw> = st.hw.filter { it.kind == "hw" && !it.done }.sortedBy { it.due ?: "9999" }

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
        else if (d.toEpochDay() - today.toEpochDay() > 6) Tr.dayLong(lang, d.dayOfWeek.value - 1) + ", " + Tr.dateShort(lang, d)
        else Tr.dayLong(lang, d.dayOfWeek.value - 1)

    /** «Сегодня», «Завтра», «пт, 21 нояб.» — подпись срока сдачи. */
    fun dueLabel(lang: String, today: LocalDate, d: LocalDate): String = when (d.toEpochDay() - today.toEpochDay()) {
        0L -> Tr.t(lang, "today"); 1L -> Tr.t(lang, "tomorrow")
        else -> Tr.dayShort(lang, d.dayOfWeek.value - 1) + ", " + Tr.dateShort(lang, d)
    }
}

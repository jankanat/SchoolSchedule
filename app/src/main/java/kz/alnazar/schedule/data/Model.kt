package kz.alnazar.schedule.data

import kotlinx.serialization.Serializable

/** Урок в постоянном расписании недели. subj — id предмета из каталога или своего предмета. */
@Serializable
data class Lesson(val subj: String, val room: String = "")

/** Звонок: начало и конец урока в минутах от полуночи. */
@Serializable
data class Bell(val start: Int, val end: Int)

/**
 * Замена одного урока на конкретную неделю.
 * week — дата понедельника этой недели (ISO, «2026-11-16»), day — 0..5 (пн..сб), idx — номер урока.
 * lesson = null — урок в эту неделю отменён.
 */
@Serializable
data class Override(val week: String, val day: Int, val idx: Int, val lesson: Lesson?)

/** Свой предмет школы. */
@Serializable
data class CustomSubject(val id: String, val name: String, val emoji: String, val color: Long)

/** Что показывать на маленьких виджетах (2×1 и 1×2) и в день без уроков. */
@Serializable
data class WidgetCfg(
    val onLesson: String = "current",   // current | next
    val onBreak: String = "next",       // next | prev
    val idleNoLessons: Boolean = true,  // надпись «Уроков нет»
    val idleNextDay: Boolean = true,    // таблица ближайшего учебного дня
    val time: Boolean = true,           // показывать время урока
    val room: Boolean = true,           // показывать кабинет
)

/** Своя тема: акцентный и основной цвет (тон по кругу, насыщенность, яркость Ч/Б) и прозрачность поверхностей. */
@Serializable
data class CustomTheme(
    val ah: Float = 268f, val asat: Float = .85f, val al: Float = .66f,   // акцент
    val bh: Float = 255f, val bs: Float = .35f, val bl: Float = .09f,     // основной (фон)
    val alpha: Float = .72f,                                             // прозрачность карточек и панелей
)

@Serializable
data class AppState(
    val grade: Int = 7,
    val program: String = "kz",         // kz | ru
    val shift: Int = 1,                 // 1 | 2
    val sixDays: Boolean = false,
    val bells1: List<Bell> = Catalog.defaultBells(1),
    val bells2: List<Bell> = Catalog.defaultBells(2),
    /** 6 дней (пн..сб), в каждом — список уроков по порядку */
    val week: List<List<Lesson>> = Catalog.genTimetable("kz", 7, false),
    val overrides: List<Override> = emptyList(),
    val appLang: String = "ru",         // ru | kk | en
    val widgetLang: String = "ru",
    val custom: List<CustomSubject> = emptyList(),
    val widget: WidgetCfg = WidgetCfg(),
    val iconSet: String = "android",    // android | fluent | twemoji | mono
    val iconTint: String = "subject",   // для mono: subject | accent | bw
    val style: String = "ios26",        // ios26 | pixel | ios18 | color | origin | oneui
    val theme: String = "dark",         // dark | custom
    val ct: CustomTheme = CustomTheme(),
) {
    val bells: List<Bell> get() = if (shift == 2) bells2 else bells1
    val schoolDays: Int get() = if (sixDays) 6 else 5
}

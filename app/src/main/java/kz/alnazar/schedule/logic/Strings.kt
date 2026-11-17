package kz.alnazar.schedule.logic

import java.time.LocalDate

/** Тексты на трёх языках. Язык приложения и язык виджетов выбираются отдельно. */
object Tr {
    private val ru = mapOf(
        "now" to "Сейчас", "next" to "Далее", "break" to "Перемена", "before" to "До уроков",
        "prev" to "Был", "noLessons" to "Уроков нет", "doneToday" to "Уроки закончились",
        "weekend" to "Выходной", "sameRoom" to "в том же кабинете", "room" to "каб. %s",
        "tomorrow" to "Завтра", "today" to "Сегодня", "left" to "ещё %d мин", "until" to "через %d мин",
        "tab_home" to "Главная", "tab_schedule" to "Расписание", "tab_settings" to "Настройки",
        "lessonsToday" to "Уроки на сегодня", "grade" to "Класс", "program" to "Программа",
        "prog_kz" to "РК", "prog_ru" to "РФ", "shift" to "Смена", "shift1" to "1 смена", "shift2" to "2 смена",
        "sixDays" to "Шестидневка", "bells" to "Звонки", "appLang" to "Язык приложения",
        "widgetLang" to "Язык виджетов", "widgets" to "Виджеты", "onLesson" to "На уроке показывать (2×1, 1×2)",
        "onBreak" to "На перемене показывать (2×1, 1×2)", "opt_current" to "Текущий", "opt_next" to "Следующий",
        "opt_prev" to "Предыдущий", "idle" to "Когда уроков нет", "idleNo" to "Надпись «Уроков нет»",
        "idleNext" to "Таблица ближайшего дня", "regen" to "Заполнить расписание по программе класса",
        "regenAsk" to "Заменить всё расписание недели черновиком по программе %d класса?",
        "yes" to "Заменить", "cancel" to "Отмена", "save" to "Сохранить", "delete" to "Удалить",
        "addLesson" to "Добавить урок", "permanent" to "Постоянное", "thisWeek" to "Только эта неделя",
        "replaced" to "замена", "cancelled" to "отменён", "resetWeek" to "Вернуть как обычно",
        "subject" to "Предмет", "roomField" to "Кабинет", "cancelLesson" to "Отменить урок на этой неделе",
        "exactAlarm" to "Точное время виджетов", "exactAlarmOn" to "Включено", "exactAlarmOff" to "Разрешить",
        "exactAlarmHint" to "Чтобы виджет переключался ровно по звонку", "start" to "Начало", "end" to "Конец",
        "lessonN" to "%d урок", "emptyDay" to "В этот день уроков нет", "nextDay" to "Ближайший учебный день",
    )
    private val kk = mapOf(
        "now" to "Қазір", "next" to "Келесі", "break" to "Үзіліс", "before" to "Сабаққа дейін",
        "prev" to "Болған", "noLessons" to "Сабақ жоқ", "doneToday" to "Сабақ бітті",
        "weekend" to "Демалыс", "sameRoom" to "сол кабинетте", "room" to "%s каб.",
        "tomorrow" to "Ертең", "today" to "Бүгін", "left" to "тағы %d мин", "until" to "%d минуттан кейін",
        "tab_home" to "Басты", "tab_schedule" to "Кесте", "tab_settings" to "Баптаулар",
        "lessonsToday" to "Бүгінгі сабақтар", "grade" to "Сынып", "program" to "Бағдарлама",
        "prog_kz" to "ҚР", "prog_ru" to "РФ", "shift" to "Ауысым", "shift1" to "1 ауысым", "shift2" to "2 ауысым",
        "sixDays" to "Алты күндік", "bells" to "Қоңыраулар", "appLang" to "Қосымша тілі",
        "widgetLang" to "Виджет тілі", "widgets" to "Виджеттер", "onLesson" to "Сабақта көрсету (2×1, 1×2)",
        "onBreak" to "Үзілісте көрсету (2×1, 1×2)", "opt_current" to "Ағымдағы", "opt_next" to "Келесі",
        "opt_prev" to "Алдыңғы", "idle" to "Сабақ жоқ кезде", "idleNo" to "«Сабақ жоқ» жазуы",
        "idleNext" to "Жақын күннің кестесі", "regen" to "Кестені сынып бағдарламасы бойынша толтыру",
        "regenAsk" to "Апталық кестені %d-сынып бағдарламасы бойынша жобамен ауыстыру керек пе?",
        "yes" to "Ауыстыру", "cancel" to "Болдырмау", "save" to "Сақтау", "delete" to "Жою",
        "addLesson" to "Сабақ қосу", "permanent" to "Тұрақты", "thisWeek" to "Тек осы апта",
        "replaced" to "ауыстыру", "cancelled" to "болмайды", "resetWeek" to "Әдеттегідей қайтару",
        "subject" to "Пән", "roomField" to "Кабинет", "cancelLesson" to "Осы аптада сабақты болдырмау",
        "exactAlarm" to "Виджеттің дәл уақыты", "exactAlarmOn" to "Қосулы", "exactAlarmOff" to "Рұқсат беру",
        "exactAlarmHint" to "Виджет қоңырау бойынша дәл ауысуы үшін", "start" to "Басы", "end" to "Соңы",
        "lessonN" to "%d-сабақ", "emptyDay" to "Бұл күні сабақ жоқ", "nextDay" to "Жақын оқу күні",
    )
    private val en = mapOf(
        "now" to "Now", "next" to "Next", "break" to "Break", "before" to "Before school",
        "prev" to "Was", "noLessons" to "No lessons", "doneToday" to "School's over",
        "weekend" to "Day off", "sameRoom" to "same room", "room" to "room %s",
        "tomorrow" to "Tomorrow", "today" to "Today", "left" to "%d min left", "until" to "in %d min",
        "tab_home" to "Home", "tab_schedule" to "Schedule", "tab_settings" to "Settings",
        "lessonsToday" to "Today's lessons", "grade" to "Grade", "program" to "Curriculum",
        "prog_kz" to "KZ", "prog_ru" to "RU", "shift" to "Shift", "shift1" to "1st shift", "shift2" to "2nd shift",
        "sixDays" to "Six-day week", "bells" to "Bell times", "appLang" to "App language",
        "widgetLang" to "Widget language", "widgets" to "Widgets", "onLesson" to "During a lesson show (2×1, 1×2)",
        "onBreak" to "During a break show (2×1, 1×2)", "opt_current" to "Current", "opt_next" to "Next",
        "opt_prev" to "Previous", "idle" to "When there are no lessons", "idleNo" to "“No lessons” label",
        "idleNext" to "Next school day's list", "regen" to "Fill the timetable from the grade curriculum",
        "regenAsk" to "Replace the whole weekly timetable with a draft for grade %d?",
        "yes" to "Replace", "cancel" to "Cancel", "save" to "Save", "delete" to "Delete",
        "addLesson" to "Add lesson", "permanent" to "Permanent", "thisWeek" to "This week only",
        "replaced" to "replaced", "cancelled" to "cancelled", "resetWeek" to "Back to normal",
        "subject" to "Subject", "roomField" to "Room", "cancelLesson" to "Cancel this lesson this week",
        "exactAlarm" to "Exact widget timing", "exactAlarmOn" to "On", "exactAlarmOff" to "Allow",
        "exactAlarmHint" to "So the widget switches exactly on the bell", "start" to "Start", "end" to "End",
        "lessonN" to "Lesson %d", "emptyDay" to "No lessons on this day", "nextDay" to "Next school day",
    )
    private val all = mapOf("ru" to ru, "kk" to kk, "en" to en)

    fun t(lang: String, key: String, vararg a: Any): String {
        val s = all[lang]?.get(key) ?: ru[key] ?: key
        return if (a.isEmpty()) s else String.format(s, *a)
    }

    private val dShort = mapOf(
        "ru" to listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"),
        "kk" to listOf("Дс", "Сс", "Ср", "Бс", "Жм", "Сн", "Жс"),
        "en" to listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
    )
    private val dLong = mapOf(
        "ru" to listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье"),
        "kk" to listOf("Дүйсенбі", "Сейсенбі", "Сәрсенбі", "Бейсенбі", "Жұма", "Сенбі", "Жексенбі"),
        "en" to listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"),
    )
    private val months = mapOf(
        "ru" to listOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря"),
        "kk" to listOf("қаңтар", "ақпан", "наурыз", "сәуір", "мамыр", "маусым", "шілде", "тамыз", "қыркүйек", "қазан", "қараша", "желтоқсан"),
        "en" to listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    )

    fun dayShort(lang: String, dow0: Int) = (dShort[lang] ?: dShort.getValue("ru"))[dow0]
    fun dayLong(lang: String, dow0: Int) = (dLong[lang] ?: dLong.getValue("ru"))[dow0]

    /** «Вторник, 17 ноября» / «17 қараша, сейсенбі» / «Tuesday, November 17» */
    fun dateTitle(lang: String, d: LocalDate): String {
        val dow = dayLong(lang, d.dayOfWeek.value - 1)
        val m = (months[lang] ?: months.getValue("ru"))[d.monthValue - 1]
        return when (lang) {
            "kk" -> "${d.dayOfMonth} $m, ${dow.lowercase()}"
            "en" -> "$dow, $m ${d.dayOfMonth}"
            else -> "$dow, ${d.dayOfMonth} $m"
        }
    }

    fun hm(min: Int) = "%02d:%02d".format(min / 60, min % 60)
}

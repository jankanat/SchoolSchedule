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
        "lessonsCnt" to "уроков: %d", "from" to "с %s", "lastLesson" to "Последний урок", "till" to "до %s", "nearest" to "Ближайший",
        "soon" to "Скоро", "startsIn" to "через %d мин", "w_time" to "Время урока", "w_room" to "Кабинет", "hello_sub" to "%d класс · %d смена",
        "week" to "Неделя", "swaps" to "замен: %d", "view_days" to "По дням", "view_table" to "Таблица", "perm" to "Постоянно",
        "apply_to" to "Применить", "remove_swap" to "Убрать замену", "lesson_title" to "%d урок · %s", "own_subject" to "Свой предмет",
        "all_subjects" to "Все предметы", "room_ph" to "Например, 204 или Спортзал", "tab_widgets" to "Виджеты", "tab_hw" to "Задания",
        "w_sub" to "Так они выглядят на рабочем столе", "w_settings" to "Настройки виджетов", "w_add" to "Как добавить виджет",
        "w_add_hint" to "Зажми пустое место на рабочем столе → Виджеты → SchoolSchedule", "hw_title" to "Задания", "hw_tab_hw" to "Домашка",
        "hw_tab_notes" to "Заметки", "stage4" to "Домашка и заметки появятся на этапе 4", "set_title" to "Настройки",
        "set_look" to "Оформление", "appearance" to "Стиль и тема", "language" to "Язык", "set_school" to "Школа",
        "class_prog" to "Класс и программа", "holidays" to "Каникулы", "subjects" to "Предметы", "set_data" to "Данные",
        "import_title" to "Импорт расписания", "backup" to "Резервная копия", "set_system" to "Система", "later" to "скоро",
        "styles_soon" to "Остальные стили и темы — следующий шаг",
        "icons" to "Иконки предметов", "ic_android" to "Android", "ic_fluent" to "Fluent 3D", "ic_tw" to "Twemoji", "ic_mono" to "Монохромные", "ic_tint" to "Цвет монохромных иконок", "tint_subject" to "Цвет предмета", "tint_accent" to "Акцентный", "tint_bw" to "Ч/Б", "credits" to "Fluent Emoji © Microsoft (MIT). Twemoji © Twitter/X и авторы, CC BY 4.0.", "style" to "Стиль", "theme" to "Тема", "th_dark" to "Тёмная",
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
        "lessonsCnt" to "сабақ: %d", "from" to "%s бастап", "lastLesson" to "Соңғы сабақ", "till" to "%s дейін", "nearest" to "Жақын",
        "soon" to "Жақында", "startsIn" to "%d минуттан кейін", "w_time" to "Сабақ уақыты", "w_room" to "Кабинет", "hello_sub" to "%d-сынып · %d ауысым",
        "week" to "Апта", "swaps" to "ауыстыру: %d", "view_days" to "Күндер", "view_table" to "Кесте", "perm" to "Тұрақты",
        "apply_to" to "Қолдану", "remove_swap" to "Ауыстыруды алу", "lesson_title" to "%d-сабақ · %s", "own_subject" to "Өз пәнің",
        "all_subjects" to "Барлық пәндер", "room_ph" to "Мысалы, 204 немесе Спортзал", "tab_widgets" to "Виджеттер", "tab_hw" to "Тапсырмалар",
        "w_sub" to "Жұмыс үстелінде осылай көрінеді", "w_settings" to "Виджет баптаулары", "w_add" to "Виджетті қалай қосу",
        "w_add_hint" to "Жұмыс үстелінің бос жерін басып тұр → Виджеттер → SchoolSchedule", "hw_title" to "Тапсырмалар", "hw_tab_hw" to "Үй жұмысы",
        "hw_tab_notes" to "Жазбалар", "stage4" to "Үй жұмысы мен жазбалар 4-кезеңде пайда болады", "set_title" to "Баптаулар",
        "set_look" to "Безендіру", "appearance" to "Стиль мен тақырып", "language" to "Тіл", "set_school" to "Мектеп",
        "class_prog" to "Сынып және бағдарлама", "holidays" to "Демалыс", "subjects" to "Пәндер", "set_data" to "Деректер",
        "import_title" to "Кестені импорттау", "backup" to "Сақтық көшірме", "set_system" to "Жүйе", "later" to "жақында",
        "styles_soon" to "Басқа стильдер мен тақырыптар — келесі қадам",
        "icons" to "Пән белгішелері", "ic_android" to "Android", "ic_fluent" to "Fluent 3D", "ic_tw" to "Twemoji", "ic_mono" to "Монохромды", "ic_tint" to "Монохромды белгішелер түсі", "tint_subject" to "Пән түсі", "tint_accent" to "Акцент", "tint_bw" to "Ақ/қара", "credits" to "Fluent Emoji © Microsoft (MIT). Twemoji © Twitter/X және авторлар, CC BY 4.0.", "style" to "Стиль", "theme" to "Тақырып", "th_dark" to "Қараңғы",
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
        "lessonsCnt" to "%d lessons", "from" to "from %s", "lastLesson" to "Last lesson", "till" to "until %s", "nearest" to "Next",
        "soon" to "Soon", "startsIn" to "in %d min", "w_time" to "Lesson time", "w_room" to "Room", "hello_sub" to "Grade %d · shift %d",
        "week" to "Week", "swaps" to "%d swaps", "view_days" to "Days", "view_table" to "Table", "perm" to "Permanent",
        "apply_to" to "Apply to", "remove_swap" to "Remove swap", "lesson_title" to "Lesson %d · %s", "own_subject" to "Own subject",
        "all_subjects" to "All subjects", "room_ph" to "e.g. 204 or Gym", "tab_widgets" to "Widgets", "tab_hw" to "Tasks",
        "w_sub" to "How they look on the home screen", "w_settings" to "Widget settings", "w_add" to "How to add a widget",
        "w_add_hint" to "Long-press the home screen → Widgets → SchoolSchedule", "hw_title" to "Tasks", "hw_tab_hw" to "Homework",
        "hw_tab_notes" to "Notes", "stage4" to "Homework and notes arrive in stage 4", "set_title" to "Settings",
        "set_look" to "Look", "appearance" to "Style & theme", "language" to "Language", "set_school" to "School",
        "class_prog" to "Grade & curriculum", "holidays" to "Holidays", "subjects" to "Subjects", "set_data" to "Data",
        "import_title" to "Import timetable", "backup" to "Backup", "set_system" to "System", "later" to "soon",
        "styles_soon" to "Other styles and themes — next step",
        "icons" to "Subject icons", "ic_android" to "Android", "ic_fluent" to "Fluent 3D", "ic_tw" to "Twemoji", "ic_mono" to "Monochrome", "ic_tint" to "Monochrome icon colour", "tint_subject" to "Subject colour", "tint_accent" to "Accent", "tint_bw" to "B/W", "credits" to "Fluent Emoji © Microsoft (MIT). Twemoji © Twitter/X and contributors, CC BY 4.0.", "style" to "Style", "theme" to "Theme", "th_dark" to "Dark",
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

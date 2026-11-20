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
        "icons" to "Иконки предметов", "ic_android" to "Android", "ic_fluent" to "Fluent 3D", "ic_tw" to "Twemoji", "ic_mono" to "Монохромные", "ic_tint" to "Цвет монохромных иконок", "tint_subject" to "Цвет предмета", "tint_accent" to "Акцентный", "tint_bw" to "Ч/Б", "credits" to "Fluent Emoji © Microsoft (MIT). Twemoji © Twitter/X и авторы, CC BY 4.0.", "style" to "Стиль", "theme" to "Тема", "th_dark" to "Тёмная", "th_custom" to "Своя", "ct_acc" to "Акцент", "ct_base" to "Основной", "ct_sat" to "Насыщенность", "ct_light" to "Ч/Б (яркость)", "ct_alpha" to "Прозрачность", "ct_presets" to "Готовые варианты",
        "holiday" to "Каникулы", "holUntil" to "до %s", "hw_text" to "Что задали", "hw_note_text" to "Текст заметки", "hw_due" to "Срок", "hw_new" to "Новая запись", "hw_edit" to "Запись", "hw_empty" to "Пока пусто", "hw_done_sec" to "Выполнено", "hw_overdue" to "Просрочено", "hw_soon" to "Скоро сдавать", "hw_later" to "Позже", "hw_nodue" to "Без срока", "no_hw" to "Заданий нет", "type_hw" to "Домашка", "type_note" to "Заметка", "hw_photo" to "Фото", "hw_camera" to "Камера", "hw_gallery" to "Галерея", "hw_no_subj" to "Без предмета", "hw_next_lesson" to "К следующему уроку", "hw_pick_date" to "Другая дата", "hw_all" to "Все", "hw_clear_done" to "Очистить выполненные", "hw_swipe" to "Смахни запись влево, чтобы удалить", "w_hw" to "Отметка о домашке 📝", "lm_hw" to "Домашка к этому уроку", "lm_swap" to "Заменить на этой неделе", "lm_cancel" to "Отменить на этой неделе", "lm_restore" to "Вернуть как было", "lm_edit" to "Изменить урок", "lm_hint" to "Зажми урок — откроется меню", "hol_tpl" to "Загрузить шаблон", "hol_tpl_kz" to "Шаблон РК", "hol_tpl_ru" to "Шаблон РФ", "hol_tpl_ask" to "Заменить список каникул шаблоном?", "hol_note" to "Даты на 2026–2027 учебный год: РК — приказ Минпросвещения, РФ — рекомендации Минпросвещения. Сверьте с вашей школой.", "hol_name" to "Название", "hol_from" to "С", "hol_to" to "По", "hol_add" to "Добавить каникулы", "hol_new" to "Новые каникулы", "hol_days" to "%d дн.", "hol_now" to "сейчас", "hol_bad" to "Дата «по» раньше даты «с»", "subj_new" to "Новый предмет", "subj_edit" to "Предмет", "subj_name" to "Название", "subj_color" to "Цвет", "subj_icon" to "Иконка", "subj_emoji" to "Эмодзи", "subj_img" to "Картинка", "subj_emoji_hint" to "Выбери ниже или вставь любое эмодзи с клавиатуры", "subj_dl" to "Для наборов Fluent 3D и Twemoji иконка скачается из интернета", "subj_pick_img" to "Выбрать из галереи", "subj_add" to "Добавить предмет", "subj_mine" to "Мои предметы", "subj_mine_empty" to "Своих предметов пока нет. Они появятся в выборе урока.", "subj_room" to "Кабинет по умолчанию", "subj_in_use" to "Предмет стоит в расписании — уроки с ним тоже удалятся", "exp" to "Экспорт", "exp_d" to "Расписание, звонки, каникулы, предметы, задания и оформление одним файлом. Фото к домашке в файл не входят.", "exp_file" to "Сохранить файл", "copy" to "Копировать", "copied" to "Скопировано", "saved" to "Сохранено", "imp_json" to "Импорт", "imp_d" to "Заменит все текущие данные данными из файла.", "imp_file" to "Открыть файл", "imp_paste" to "Вставить из буфера", "imp_bad" to "Не удалось прочитать данные", "imp_ok" to "Данные загружены", "imp_ask" to "Заменить все текущие данные?", "autobak" to "Автобэкап Google", "autobak_d" to "Android сам сохраняет данные приложения в Google-аккаунт (включается в настройках телефона → Google → Резервное копирование) и восстанавливает их на новом телефоне. Фото к домашке не входят.", "notif" to "Напоминания", "n_evening" to "Вечером накануне", "n_evening_sub" to "Список домашки на следующий учебный день", "n_at" to "Время", "n_before" to "Перед уроком", "n_before_sub" to "Напомнить про домашку перед уроком, к которому задано", "n_min" to "за %d мин", "n_h" to "за %d ч", "n_perm" to "Разреши уведомления в настройках телефона", "n_t_evening" to "Домашка на %s", "n_t_before" to "%s через %d мин", "n_ch" to "Домашка", "n_test" to "Проверить уведомление", "corners" to "Скругление углов", "glareW" to "Блики виджетов", "glareUi" to "Блики интерфейса", "g_on" to "Вкл", "g_off" to "Выкл", "g_note" to "Кромка и блик по краю. Только ColorOS 17, iOS 26 и OriginOS 7", "font" to "Шрифт", "f_style" to "Как в стиле", "f_system" to "Системный", "f_rounded" to "Скруглённый", "f_serif" to "С засечками", "f_mono" to "Моноширинный", "f_hand" to "Рукописный", "f_widget_note" to "На виджетах используется ближайший системный шрифт", "edit" to "Изменить", "add" to "Добавить", "done" to "Готово",
        "now_n" to "Сейчас · %d-й урок", "min_end" to "мин\nдо конца", "min_bell" to "мин\nдо звонка", "min_start" to "мин\nдо начала", "next_brk" to "Далее · перемена %d мин", "after_brk" to "После перемены", "first_on" to "%s первым", "lessons" to "Уроки", "lessons_today" to "Уроки сегодня", "passed" to "%d из %d прошло", "left_time" to "%s осталось", "tasks" to "Задания", "all_n" to "Все %d", "due_today" to "Сдать сегодня", "due_tomorrow" to "Сдать завтра", "more_week" to "ещё %d на неделе", "day" to "День", "changes_n" to "изменений: %d", "cancel_week" to "отменён на неделю", "lesson_of" to "%d-й урок из %d", "until_end" to "до конца урока", "to_bell" to "до звонка %d мин", "g_lesson" to "урок", "g_day" to "день", "g_hand" to "сдать", "g_end" to "конец", "ui_scale" to "Размер интерфейса", "time_side" to "Время в списке уроков", "ts_auto" to "Как в стиле", "ts_left" to "Слева", "ts_right" to "Справа", "week_n" to "НЕДЕЛЯ %d", "lessons_n1" to "урок", "lessons_n2" to "урока", "lessons_n5" to "уроков", "min_n1" to "минута", "min_n2" to "минуты", "min_n5" to "минут", "to_end_of" to "до конца урока", "to_start_of" to "до начала урока", "tomorrow_n" to "Завтра · %s", "all_today" to "всё на сегодня", "tasks_tomorrow" to "заданий на завтра: %d",
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
        "icons" to "Пән белгішелері", "ic_android" to "Android", "ic_fluent" to "Fluent 3D", "ic_tw" to "Twemoji", "ic_mono" to "Монохромды", "ic_tint" to "Монохромды белгішелер түсі", "tint_subject" to "Пән түсі", "tint_accent" to "Акцент", "tint_bw" to "Ақ/қара", "credits" to "Fluent Emoji © Microsoft (MIT). Twemoji © Twitter/X және авторлар, CC BY 4.0.", "style" to "Стиль", "theme" to "Тақырып", "th_dark" to "Қараңғы", "th_custom" to "Өзімдікі", "ct_acc" to "Акцент", "ct_base" to "Негізгі", "ct_sat" to "Қанықтық", "ct_light" to "Ақ/қара (жарықтық)", "ct_alpha" to "Мөлдірлік", "ct_presets" to "Дайын нұсқалар",
        "holiday" to "Каникул", "holUntil" to "%s дейін", "hw_text" to "Не берілді", "hw_note_text" to "Жазба мәтіні", "hw_due" to "Мерзімі", "hw_new" to "Жаңа жазба", "hw_edit" to "Жазба", "hw_empty" to "Әзірге бос", "hw_done_sec" to "Орындалды", "hw_overdue" to "Мерзімі өтті", "hw_soon" to "Жақында тапсыру", "hw_later" to "Кейін", "hw_nodue" to "Мерзімсіз", "no_hw" to "Тапсырма жоқ", "type_hw" to "Үй жұмысы", "type_note" to "Жазба", "hw_photo" to "Фото", "hw_camera" to "Камера", "hw_gallery" to "Галерея", "hw_no_subj" to "Пәнсіз", "hw_next_lesson" to "Келесі сабаққа", "hw_pick_date" to "Басқа күн", "hw_all" to "Барлығы", "hw_clear_done" to "Орындалғандарды тазалау", "hw_swipe" to "Жою үшін жазбаны солға сырғыт", "w_hw" to "Үй жұмысы белгісі 📝", "lm_hw" to "Осы сабаққа үй жұмысы", "lm_swap" to "Осы аптада ауыстыру", "lm_cancel" to "Осы аптада болдырмау", "lm_restore" to "Бұрынғыдай қайтару", "lm_edit" to "Сабақты өзгерту", "lm_hint" to "Мәзір үшін сабақты басып тұр", "hol_tpl" to "Үлгіні жүктеу", "hol_tpl_kz" to "ҚР үлгісі", "hol_tpl_ru" to "РФ үлгісі", "hol_tpl_ask" to "Каникул тізімін үлгімен ауыстыру керек пе?", "hol_note" to "2026–2027 оқу жылының күндері: ҚР — Оқу-ағарту министрлігінің бұйрығы, РФ — ұсыныстар. Мектебіңізбен салыстырыңыз.", "hol_name" to "Атауы", "hol_from" to "Бастап", "hol_to" to "Дейін", "hol_add" to "Каникул қосу", "hol_new" to "Жаңа каникул", "hol_days" to "%d күн", "hol_now" to "қазір", "hol_bad" to "«Дейін» күні «бастап» күнінен бұрын", "subj_new" to "Жаңа пән", "subj_edit" to "Пән", "subj_name" to "Атауы", "subj_color" to "Түсі", "subj_icon" to "Белгіше", "subj_emoji" to "Эмодзи", "subj_img" to "Сурет", "subj_emoji_hint" to "Төменнен таңда немесе пернетақтадан кез келген эмодзи қой", "subj_dl" to "Fluent 3D және Twemoji үшін белгіше интернеттен жүктеледі", "subj_pick_img" to "Галереядан таңдау", "subj_add" to "Пән қосу", "subj_mine" to "Менің пәндерім", "subj_mine_empty" to "Өз пәндеріңіз әзірге жоқ. Олар сабақ таңдауда шығады.", "subj_room" to "Әдепкі кабинет", "subj_in_use" to "Пән кестеде бар — оның сабақтары да жойылады", "exp" to "Экспорт", "exp_d" to "Кесте, қоңыраулар, каникул, пәндер, тапсырмалар және безендіру бір файлда. Үй жұмысының фотолары кірмейді.", "exp_file" to "Файлды сақтау", "copy" to "Көшіру", "copied" to "Көшірілді", "saved" to "Сақталды", "imp_json" to "Импорт", "imp_d" to "Барлық ағымдағы деректерді файлдағымен ауыстырады.", "imp_file" to "Файлды ашу", "imp_paste" to "Буферден қою", "imp_bad" to "Деректерді оқу мүмкін болмады", "imp_ok" to "Деректер жүктелді", "imp_ask" to "Барлық деректерді ауыстыру керек пе?", "autobak" to "Google автосақтық", "autobak_d" to "Android деректерді Google аккаунтына өзі сақтайды (телефон баптаулары → Google → Сақтық көшірме) және жаңа телефонда қалпына келтіреді.", "notif" to "Еске салғыштар", "n_evening" to "Алдыңғы кеші", "n_evening_sub" to "Келесі оқу күнінің үй жұмысы", "n_at" to "Уақыты", "n_before" to "Сабақ алдында", "n_before_sub" to "Тапсырма берілген сабақ алдында еске салу", "n_min" to "%d мин бұрын", "n_h" to "%d сағ бұрын", "n_perm" to "Телефон баптауларында хабарландыруларға рұқсат бер", "n_t_evening" to "%s үй жұмысы", "n_t_before" to "%s — %d минуттан кейін", "n_ch" to "Үй жұмысы", "n_test" to "Хабарландыруды тексеру", "corners" to "Бұрыштарды дөңгелектеу", "glareW" to "Виджет жарқылы", "glareUi" to "Интерфейс жарқылы", "g_on" to "Қосу", "g_off" to "Өшіру", "g_note" to "Жиек пен жарқыл. Тек ColorOS 17, iOS 26 және OriginOS 7", "font" to "Қаріп", "f_style" to "Стильдегідей", "f_system" to "Жүйелік", "f_rounded" to "Дөңгелек", "f_serif" to "Серифті", "f_mono" to "Моно", "f_hand" to "Қолжазба", "f_widget_note" to "Виджеттерде ең жақын жүйелік қаріп қолданылады", "edit" to "Өзгерту", "add" to "Қосу", "done" to "Дайын",
        "now_n" to "Қазір · %d-сабақ", "min_end" to "мин\nаяқталуға", "min_bell" to "мин\nқоңырауға", "min_start" to "мин\nбасталуға", "next_brk" to "Келесі · үзіліс %d мин", "after_brk" to "Үзілістен кейін", "first_on" to "%s бірінші", "lessons" to "Сабақтар", "lessons_today" to "Бүгінгі сабақтар", "passed" to "%d/%d өтті", "left_time" to "%s қалды", "tasks" to "Тапсырмалар", "all_n" to "Барлығы %d", "due_today" to "Бүгін тапсыру", "due_tomorrow" to "Ертең тапсыру", "more_week" to "аптада тағы %d", "day" to "Күн", "changes_n" to "өзгеріс: %d", "cancel_week" to "аптаға болдырылмады", "lesson_of" to "%d/%d сабақ", "until_end" to "сабақ аяқталуға", "to_bell" to "қоңырауға %d мин", "g_lesson" to "сабақ", "g_day" to "күн", "g_hand" to "тапсыру", "g_end" to "соңы", "ui_scale" to "Интерфейс өлшемі", "time_side" to "Сабақ тізіміндегі уақыт", "ts_auto" to "Стильдегідей", "ts_left" to "Сол жақта", "ts_right" to "Оң жақта", "week_n" to "%d-АПТА", "lessons_n1" to "сабақ", "lessons_n2" to "сабақ", "lessons_n5" to "сабақ", "min_n1" to "минут", "min_n2" to "минут", "min_n5" to "минут", "to_end_of" to "сабақтың соңына дейін", "to_start_of" to "сабақтың басталуына", "tomorrow_n" to "Ертең · %s", "all_today" to "бүгінге бәрі", "tasks_tomorrow" to "ертеңге тапсырма: %d",
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
        "icons" to "Subject icons", "ic_android" to "Android", "ic_fluent" to "Fluent 3D", "ic_tw" to "Twemoji", "ic_mono" to "Monochrome", "ic_tint" to "Monochrome icon colour", "tint_subject" to "Subject colour", "tint_accent" to "Accent", "tint_bw" to "B/W", "credits" to "Fluent Emoji © Microsoft (MIT). Twemoji © Twitter/X and contributors, CC BY 4.0.", "style" to "Style", "theme" to "Theme", "th_dark" to "Dark", "th_custom" to "Custom", "ct_acc" to "Accent", "ct_base" to "Base", "ct_sat" to "Saturation", "ct_light" to "B/W (lightness)", "ct_alpha" to "Transparency", "ct_presets" to "Presets",
        "holiday" to "Holidays", "holUntil" to "until %s", "hw_text" to "What was assigned", "hw_note_text" to "Note text", "hw_due" to "Due", "hw_new" to "New entry", "hw_edit" to "Entry", "hw_empty" to "Nothing here yet", "hw_done_sec" to "Done", "hw_overdue" to "Overdue", "hw_soon" to "Due soon", "hw_later" to "Later", "hw_nodue" to "No due date", "no_hw" to "No tasks", "type_hw" to "Homework", "type_note" to "Note", "hw_photo" to "Photo", "hw_camera" to "Camera", "hw_gallery" to "Gallery", "hw_no_subj" to "No subject", "hw_next_lesson" to "Next lesson", "hw_pick_date" to "Other date", "hw_all" to "All", "hw_clear_done" to "Clear done", "hw_swipe" to "Swipe left to delete", "w_hw" to "Homework mark 📝", "lm_hw" to "Homework for this lesson", "lm_swap" to "Replace this week", "lm_cancel" to "Cancel this week", "lm_restore" to "Restore", "lm_edit" to "Edit lesson", "lm_hint" to "Long-press a lesson for more", "hol_tpl" to "Load template", "hol_tpl_kz" to "Kazakhstan", "hol_tpl_ru" to "Russia", "hol_tpl_ask" to "Replace the list with the template?", "hol_note" to "2026–27 school year dates: Kazakhstan — ministry order; Russia — ministry recommendations. Check with your school.", "hol_name" to "Name", "hol_from" to "From", "hol_to" to "To", "hol_add" to "Add holidays", "hol_new" to "New holidays", "hol_days" to "%d days", "hol_now" to "now", "hol_bad" to "End date is before start", "subj_new" to "New subject", "subj_edit" to "Subject", "subj_name" to "Name", "subj_color" to "Colour", "subj_icon" to "Icon", "subj_emoji" to "Emoji", "subj_img" to "Picture", "subj_emoji_hint" to "Pick below or type any emoji", "subj_dl" to "For Fluent 3D and Twemoji the icon is downloaded", "subj_pick_img" to "Choose from gallery", "subj_add" to "Add subject", "subj_mine" to "My subjects", "subj_mine_empty" to "No custom subjects yet. They appear in the lesson picker.", "subj_room" to "Default room", "subj_in_use" to "This subject is in the timetable — its lessons will be removed too", "exp" to "Export", "exp_d" to "Timetable, bells, holidays, subjects, tasks and appearance in one file. Homework photos are not included.", "exp_file" to "Save file", "copy" to "Copy", "copied" to "Copied", "saved" to "Saved", "imp_json" to "Import", "imp_d" to "Replaces all current data with the file’s data.", "imp_file" to "Open file", "imp_paste" to "Paste from clipboard", "imp_bad" to "Couldn’t read the data", "imp_ok" to "Data imported", "imp_ask" to "Replace all current data?", "autobak" to "Google auto-backup", "autobak_d" to "Android backs the app data up to your Google account (phone Settings → Google → Backup) and restores it on a new phone.", "notif" to "Reminders", "n_evening" to "The evening before", "n_evening_sub" to "Homework for the next school day", "n_at" to "Time", "n_before" to "Before the lesson", "n_before_sub" to "Remind before the lesson it’s due", "n_min" to "%d min before", "n_h" to "%d h before", "n_perm" to "Allow notifications in phone settings", "n_t_evening" to "Homework for %s", "n_t_before" to "%s in %d min", "n_ch" to "Homework", "n_test" to "Test notification", "corners" to "Corner radius", "glareW" to "Widget glare", "glareUi" to "Interface glare", "g_on" to "On", "g_off" to "Off", "g_note" to "Rim and edge glint. ColorOS 17, iOS 26 and OriginOS 7 only", "font" to "Font", "f_style" to "Style default", "f_system" to "System", "f_rounded" to "Rounded", "f_serif" to "Serif", "f_mono" to "Monospace", "f_hand" to "Handwritten", "f_widget_note" to "Widgets use the closest system font", "edit" to "Edit", "add" to "Add", "done" to "Done",
        "now_n" to "Now · lesson %d", "min_end" to "min\nleft", "min_bell" to "min\nto bell", "min_start" to "min\nto start", "next_brk" to "Next · %d min break", "after_brk" to "After the break", "first_on" to "First %s", "lessons" to "Lessons", "lessons_today" to "Today’s lessons", "passed" to "%d of %d done", "left_time" to "%s left", "tasks" to "Tasks", "all_n" to "All %d", "due_today" to "Due today", "due_tomorrow" to "Due tomorrow", "more_week" to "%d more this week", "day" to "Day", "changes_n" to "changes: %d", "cancel_week" to "cancelled this week", "lesson_of" to "lesson %d of %d", "until_end" to "until the bell", "to_bell" to "%d min to bell", "g_lesson" to "lesson", "g_day" to "day", "g_hand" to "due", "g_end" to "ends", "ui_scale" to "Interface size", "time_side" to "Time in the lesson list", "ts_auto" to "Style default", "ts_left" to "Left", "ts_right" to "Right", "week_n" to "WEEK %d", "lessons_n1" to "lesson", "lessons_n2" to "lessons", "lessons_n5" to "lessons", "min_n1" to "minute", "min_n2" to "minutes", "min_n5" to "minutes", "to_end_of" to "until the lesson ends", "to_start_of" to "until the lesson starts", "tomorrow_n" to "Tomorrow · %s", "all_today" to "all done today", "tasks_tomorrow" to "%d tasks for tomorrow",
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

    /** «18 ноября» / «18 қараша» / «November 18» */
    fun dateDM(lang: String, d: LocalDate): String {
        val m = (months[lang] ?: months.getValue("ru"))[d.monthValue - 1]
        return if (lang == "en") "$m ${d.dayOfMonth}" else "${d.dayOfMonth} $m"
    }

    /** «21 ноя» / «21 қар» / «Nov 21» */
    fun dateShort(lang: String, d: LocalDate): String {
        val m = (months[lang] ?: months.getValue("ru"))[d.monthValue - 1]
        return if (lang == "en") "${m.take(3)} ${d.dayOfMonth}" else "${d.dayOfMonth} ${m.take(3)}"
    }

    /** Число со словом: 1 урок, 2 урока, 5 уроков (base — «lessons» или «min»). */
    fun plural(lang: String, base: String, n: Int): String {
        val m10 = n % 10; val m100 = n % 100
        val f = when { m10 == 1 && m100 != 11 -> "1"; m10 in 2..4 && m100 !in 12..14 -> "2"; else -> "5" }
        return "$n " + t(lang, "${base}_n$f")
    }

    /** «2 ч 05 мин» / «45 мин» */
    fun dur(lang: String, min: Int): String {
        val h = min / 60; val m = min % 60
        val hu = when (lang) { "kk" -> "сағ"; "en" -> "h"; else -> "ч" }
        val mu = when (lang) { "en" -> "min"; else -> "мин" }
        return if (h > 0) "$h $hu %02d $mu".format(m) else "$m $mu"
    }

    fun hm(min: Int) = "%02d:%02d".format(min / 60, min % 60)
}

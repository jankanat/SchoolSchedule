package kz.alnazar.schedule.data

/** Предмет из каталога: названия на трёх языках, эмодзи, цвет плитки, кабинет по умолчанию. */
data class Subject(
    val id: String,
    val ru: String,
    val kk: String,
    val en: String,
    val emoji: String,
    val color: Long,
    val room: String,
) {
    fun name(lang: String) = when (lang) { "kk" -> kk; "en" -> en; else -> ru }
}

object Catalog {
    private fun s(id: String, ru: String, kk: String, en: String, e: String, c: Long, r: String) =
        Subject(id, ru, kk, en, e, c, r)

    val subjects: List<Subject> = listOf(
        s("math", "Математика", "Математика", "Mathematics", "🧮", 0xFFFF9F0A, "313"),
        s("algebra", "Алгебра", "Алгебра", "Algebra", "➗", 0xFFFF9F0A, "313"),
        s("geometry", "Геометрия", "Геометрия", "Geometry", "📐", 0xFFFF7A45, "313"),
        s("rus", "Русский язык", "Орыс тілі", "Russian", "✏️", 0xFFFF453A, "205"),
        s("ruslit", "Русская литература", "Орыс әдебиеті", "Russian literature", "📖", 0xFFFF375F, "205"),
        s("literature", "Литература", "Әдебиет", "Literature", "📖", 0xFFFF375F, "205"),
        s("kaz", "Казахский язык", "Қазақ тілі", "Kazakh", "🖊️", 0xFF30B0C7, "204"),
        s("kazlit", "Казахская литература", "Қазақ әдебиеті", "Kazakh literature", "📚", 0xFF32ADE6, "204"),
        s("eng", "Английский язык", "Ағылшын тілі", "English", "🔤", 0xFF0A84FF, "112"),
        s("inf", "Информатика", "Информатика", "Computer science", "💻", 0xFF5E5CE6, "301"),
        s("hist_kz", "История Казахстана", "Қазақстан тарихы", "History of Kazakhstan", "🏹", 0xFFBF5AF2, "215"),
        s("hist_world", "Всемирная история", "Дүниежүзі тарихы", "World history", "🏛️", 0xFFA2845E, "215"),
        s("hist", "История", "Тарих", "History", "📜", 0xFFA2845E, "215"),
        s("geo", "География", "География", "Geography", "🌍", 0xFF34C759, "218"),
        s("bio", "Биология", "Биология", "Biology", "🧬", 0xFF30D158, "220"),
        s("phys", "Физика", "Физика", "Physics", "⚛️", 0xFF64D2FF, "308"),
        s("chem", "Химия", "Химия", "Chemistry", "🧪", 0xFFBF5AF2, "310"),
        s("art", "Изо", "Бейнелеу өнері", "Art", "🎨", 0xFFFF6482, "120"),
        s("music", "Музыка", "Музыка", "Music", "🎵", 0xFFFF2D92, "121"),
        s("pe", "Физкультура", "Дене шынықтыру", "Physical education", "⚽", 0xFF32D74B, "Спортзал"),
        s("tech", "Технология", "Технология", "Technology", "🛠️", 0xFF8E8E93, "101"),
        s("nvp", "НВП", "Алғашқы әскери дайындық", "Military training", "🎖️", 0xFF6C7A4F, "105"),
        s("natural", "Естествознание", "Жаратылыстану", "Natural science", "🌱", 0xFF30D158, "313"),
        s("law", "Основы права", "Құқық негіздері", "Law basics", "⚖️", 0xFF8E6FD8, "215"),
        s("self", "Самопознание", "Өзін-өзі тану", "Self-knowledge", "🌟", 0xFFFFD60A, "313"),
        s("cl", "Классный час", "Сынып сағаты", "Class hour", "🏫", 0xFF8E8E93, "313"),
        s("social", "Обществознание", "Қоғамтану", "Social studies", "👥", 0xFFC69C6D, "215"),
        s("obzh", "ОБЖ", "Өмір қауіпсіздігі", "Life safety", "🦺", 0xFFFF9500, "105"),
        s("prob", "Вероятность и статистика", "Ықтималдық және статистика", "Probability & statistics", "📊", 0xFFFFB340, "313"),
        s("odnknr", "ОДНКНР", "Рухани-адамгершілік мәдениет", "Spiritual & moral culture", "🕊️", 0xFF98989D, "313"),
    )
    private val byId = subjects.associateBy { it.id }
    fun find(id: String): Subject? = byId[id]

    /** Предметы по классам: программа РК и РФ */
    val programs: Map<String, Map<Int, List<String>>> = mapOf(
        "kz" to mapOf(
            5 to listOf("math", "kaz", "hist_kz", "eng", "natural", "rus", "pe", "kazlit", "inf", "ruslit", "art", "hist_world", "music", "tech", "self", "cl"),
            6 to listOf("math", "kaz", "geo", "eng", "natural", "rus", "pe", "hist_kz", "kazlit", "inf", "ruslit", "art", "hist_world", "music", "tech", "self", "cl"),
            7 to listOf("algebra", "kaz", "hist_kz", "eng", "phys", "rus", "geo", "geometry", "bio", "kazlit", "pe", "chem", "ruslit", "inf", "hist_world", "art", "tech", "music", "self", "cl"),
            8 to listOf("algebra", "kaz", "hist_kz", "eng", "phys", "rus", "geo", "geometry", "bio", "kazlit", "pe", "chem", "ruslit", "inf", "hist_world", "art", "tech", "music", "self", "cl"),
            9 to listOf("algebra", "kaz", "hist_kz", "eng", "phys", "rus", "geo", "geometry", "bio", "kazlit", "pe", "chem", "ruslit", "inf", "hist_world", "law", "tech", "self", "cl"),
            10 to listOf("algebra", "kaz", "hist_kz", "eng", "phys", "rus", "geo", "geometry", "bio", "kazlit", "pe", "chem", "ruslit", "inf", "hist_world", "nvp", "self", "cl"),
            11 to listOf("algebra", "kaz", "hist_kz", "eng", "phys", "rus", "geo", "geometry", "bio", "kazlit", "pe", "chem", "ruslit", "inf", "hist_world", "nvp", "self", "cl"),
        ),
        "ru" to mapOf(
            5 to listOf("math", "rus", "hist", "eng", "bio", "literature", "geo", "pe", "music", "art", "tech", "odnknr", "cl"),
            6 to listOf("math", "rus", "hist", "eng", "bio", "literature", "geo", "pe", "music", "art", "tech", "odnknr", "cl"),
            7 to listOf("algebra", "rus", "hist", "eng", "phys", "literature", "geo", "geometry", "bio", "pe", "inf", "social", "prob", "music", "art", "tech", "cl"),
            8 to listOf("algebra", "rus", "hist", "eng", "phys", "literature", "geo", "geometry", "bio", "chem", "pe", "inf", "social", "prob", "obzh", "music", "tech", "cl"),
            9 to listOf("algebra", "rus", "hist", "eng", "phys", "literature", "geo", "geometry", "bio", "chem", "pe", "inf", "social", "prob", "obzh", "tech", "cl"),
            10 to listOf("algebra", "rus", "hist", "eng", "phys", "literature", "geo", "geometry", "bio", "chem", "pe", "inf", "social", "obzh", "cl"),
            11 to listOf("algebra", "rus", "hist", "eng", "phys", "literature", "geo", "geometry", "bio", "chem", "pe", "inf", "social", "obzh", "cl"),
        ),
    )

    fun programSubjects(program: String, grade: Int): List<String> =
        programs[program]?.get(grade) ?: programs.getValue("kz").getValue(7)

    private fun hm(s: String): Int { val p = s.split(":"); return p[0].toInt() * 60 + p[1].toInt() }

    /**
     * Каникулы 2026–2027 учебного года.
     * РК — приказ Минпросвещения РК; РФ — рекомендации Минпросвещения РФ (школы по четвертям), школа может менять даты.
     */
    fun holidayTemplate(program: String): List<Holiday> = if (program == "ru") listOf(
        Holiday(1, "Осенние", "2026-10-26", "2026-11-03"),
        Holiday(2, "Зимние", "2026-12-31", "2027-01-10"),
        Holiday(3, "Весенние", "2027-03-27", "2027-04-04"),
        Holiday(4, "Летние", "2027-05-27", "2027-08-31"),
    ) else listOf(
        Holiday(1, "Осенние", "2026-10-26", "2026-11-01"),
        Holiday(2, "Зимние", "2026-12-28", "2027-01-10"),
        Holiday(3, "Весенние", "2027-03-22", "2027-03-28"),
        Holiday(4, "Летние", "2027-05-26", "2027-08-31"),
    )

    fun defaultBells(shift: Int): List<Bell> {
        val list = if (shift == 2) listOf(
            "13:30" to "14:15", "14:25" to "15:10", "15:20" to "16:05", "16:15" to "17:00",
            "17:10" to "17:55", "18:05" to "18:50", "19:00" to "19:45", "19:55" to "20:40",
        ) else listOf(
            "08:30" to "09:15", "09:25" to "10:10", "10:20" to "11:05", "11:15" to "12:00",
            "12:10" to "12:55", "13:05" to "13:50", "14:00" to "14:45", "14:55" to "15:40",
        )
        return list.map { Bell(hm(it.first), hm(it.second)) }
    }

    /** Черновик расписания по программе класса: дальше пользователь правит его сам. */
    fun genTimetable(program: String, grade: Int, six: Boolean): List<List<Lesson>> {
        val ids = programSubjects(program, grade)
        val hasCl = "cl" in ids
        val pool = ids.filter { it != "cl" }
        val counts = intArrayOf(6, 7, 7, 6, 7, if (six) 4 else 0)
        var k = 0
        return (0 until 6).map { d ->
            val arr = mutableListOf<Lesson>()
            if (d == 0 && hasCl && counts[d] > 0) arr += Lesson("cl", roomOf("cl"))
            var guard = 0
            while (arr.size < counts[d] && guard++ < 80) {
                val s = pool[(k++ + d * 3) % pool.size]
                if (arr.any { it.subj == s }) continue
                arr += Lesson(s, roomOf(s))
            }
            arr
        }
    }

    fun roomOf(id: String) = find(id)?.room ?: ""
}

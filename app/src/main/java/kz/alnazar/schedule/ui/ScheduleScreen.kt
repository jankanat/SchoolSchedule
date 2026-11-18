package kz.alnazar.schedule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.togetherWith
import kz.alnazar.schedule.data.AppState
import kz.alnazar.schedule.data.Catalog
import kz.alnazar.schedule.data.Lesson
import kz.alnazar.schedule.data.Override
import kz.alnazar.schedule.data.Store
import kz.alnazar.schedule.logic.Sched
import kz.alnazar.schedule.logic.Tr
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

private val MS = mapOf(
    "ru" to listOf("янв", "фев", "мар", "апр", "мая", "июн", "июл", "авг", "сен", "окт", "ноя", "дек"),
    "kk" to listOf("қаң", "ақп", "нау", "сәу", "мам", "мау", "шіл", "там", "қыр", "қаз", "қар", "жел"),
    "en" to listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
)

@Composable
fun ScheduleScreen(st: AppState) {
    val ui = LocalUi.current
    val ctx = LocalContext.current
    val lang = st.appLang
    val t = T
    val today = LocalDate.now()
    val nd = st.schoolDays
    // какой день показать: из главной (нажали день недели) или сегодня
    var sel by rememberSaveable { mutableStateOf(today.toString()) }
    var view by rememberSaveable { mutableStateOf("days") }
    val req = SchedBus.day
    androidx.compose.runtime.LaunchedEffect(req) { if (req != null) { sel = req.toString(); view = "days"; SchedBus.day = null } }
    var date = LocalDate.parse(sel)
    if (date.dayOfWeek.value - 1 >= nd) date = Sched.monday(date).plusDays((nd - 1).toLong())
    val mon = Sched.monday(date)
    val wk = mon.toString()
    val changes = st.overrides.count { it.week == wk }
    val end = mon.plusDays((nd - 1).toLong())
    val ms = MS[lang] ?: MS.getValue("ru")
    val range = "${mon.dayOfMonth} – ${end.dayOfMonth} ${ms[end.monthValue - 1]}".let { if (mon.monthValue != end.monthValue) "${mon.dayOfMonth} ${ms[mon.monthValue - 1]} – ${end.dayOfMonth} ${ms[end.monthValue - 1]}" else it }
    val week = remember(st, wk) { Sched.week(st, mon) }
    val all = remember(st, date) { Sched.dayAll(st, date) }
    val n = remember(st) { Sched.now(st) }
    val items = remember(st, date, n) { feedItems(st, date, lang, if (date == today) n else null, true) }

    fun edit(dayIdx: Int, i: Int) { ui.open { LessonSheet(dayIdx, i, wk) } }
    fun shift(weeks: Long) { sel = date.plusWeeks(weeks).toString() }
    val actions = listOf(Ic.chevL to { shift(-1) }, Ic.chevR to { shift(1) })
    val dow = date.dayOfWeek.value - 1

    Page {
        item {
            if (t.pixel) ScreenHead(Tr.t(lang, "week_n", mon.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR)),
                range + if (changes > 0) " · " + Tr.t(lang, "changes_n", changes) else "", actions, kicker = Tr.t(lang, "tab_schedule"))
            else ScreenHead(Tr.t(lang, "tab_schedule"), range, actions)
        }
        if (!t.pixel) item {
            Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp).appear(1)) {
                HeadSeg(listOf("days" to Tr.t(lang, "day"), "table" to Tr.t(lang, "week")), view, wide = true) { view = it }
            }
        }
        item {
            Box(Modifier.padding(top = if (t.pixel) 18.dp else 12.dp).appear(2)) {
                DayStrip(lang, week, date, today) { d -> sel = d.toString(); view = "days" }
            }
        }
        if (view == "days") {
            item {
                SecHead(Tr.dayLong(lang, dow) + if (t.pixel || t.id == "origin" || t.id == "oneui") " · " + Tr.plural(lang, "lessons", all.count { !it.cancelled }) else "") {
                    if (t.pixel) HeadSeg(listOf("days" to Tr.t(lang, "day"), "table" to Tr.t(lang, "week")), view) { view = it }
                    else if (changes > 0) Tag(Tr.t(lang, "changes_n", changes), Color(0xFFFF9F0A))
                }
            }
            item {
                androidx.compose.animation.AnimatedContent(date, transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.spring(.85f, 380f)) { it / 5 * dir } + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(200))) togetherWith
                        (androidx.compose.animation.slideOutHorizontally(androidx.compose.animation.core.tween(160)) { -it / 6 * dir } + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(120)))
                }, label = "day") { d ->
                    val its = if (d == date) items else feedItems(st, d, lang, null, true)
                    val dAll = if (d == date) all else Sched.dayAll(st, d)
                    if (its.isEmpty()) Empty(Tr.t(lang, "emptyDay"))
                    else LessonFeed(
                        its, lang, timeLeft(st.timeSide), Tr.t(lang, "replaced"), Tr.t(lang, "cancel_week"),
                        onClick = { i -> dAll.getOrNull(i)?.let { edit(it.slot.dayIdx, it.slot.idx) } },
                        onLong = { i -> dAll.getOrNull(i)?.let { x -> ui.open { LessonMenu(d, x.slot) } } },
                        onRestore = { i -> dAll.getOrNull(i)?.let { x -> Store.update(ctx) { s -> s.copy(overrides = s.overrides.filterNot { o -> o.week == wk && o.day == x.slot.dayIdx && o.idx == x.slot.idx }) } } },
                    )
                }
            }
            item {
                AddButton(Tr.t(lang, "addLesson")) {
                    val nn = maxOf(st.week.getOrNull(dow)?.size ?: 0, (all.maxOfOrNull { it.slot.idx } ?: -1) + 1)
                    edit(dow, nn)
                }
            }
        } else {
            item { Box(Modifier.padding(horizontal = 16.dp, vertical = 14.dp).appear(3)) { WeekTable(st, mon, nd, lang) { d, i -> edit(d, i) } } }
        }
    }
}

/** Таблица недели (.tbl): дни по столбцам, номера уроков по строкам. */
@Composable
private fun WeekTable(st: AppState, mon: LocalDate, nd: Int, lang: String, onEdit: (Int, Int) -> Unit) {
    val lists = (0 until nd).map { Sched.dayLessons(st, mon.plusDays(it.toLong())) }
    val rows = minOf(8, (lists.maxOfOrNull { l -> (l.maxOfOrNull { it.idx } ?: -1) + 1 } ?: 0) + 1)
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Spacer(Modifier.width(22.dp))
            (0 until nd).forEach { k ->
                Text(Tr.dayShort(lang, k), color = T.tx2, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(vertical = 4.dp))
            }
        }
        for (i in 0 until rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${i + 1}", color = T.tx2, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(22.dp))
                (0 until nd).forEach { k ->
                    val x = lists[k].firstOrNull { it.idx == i }
                    val last = (lists[k].maxOfOrNull { it.idx } ?: -1) + 1
                    val shape = RoundedCornerShape(12.dp)
                    when {
                        x != null -> {
                            val v = Sched.subj(st, x.lesson.subj, lang)
                            Box(
                                Modifier.weight(1f).heightIn(min = 52.dp).glass(shape).tap { onEdit(k, i) }.padding(vertical = 6.dp, horizontal = 2.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Tile(v.emoji, v.color, 24.dp)
                                    Text(v.name.take(6), color = T.tx, fontSize = 10.5.sp, maxLines = 1)
                                }
                                if (x.replaced) Box(Modifier.align(Alignment.TopEnd).size(6.dp).background(Color(0xFFFF9F0A), CircleShape))
                            }
                        }
                        i == last -> Box(
                            Modifier.weight(1f).height(52.dp).clip(shape).border(1.5.dp, T.line, shape).tap { onEdit(k, i) },
                            contentAlignment = Alignment.Center,
                        ) { Text("+", color = T.tx2, fontSize = 18.sp) }
                        else -> Spacer(Modifier.weight(1f).height(52.dp))
                    }
                }
            }
        }
    }
}

/** Шторка урока: выбор предмета, кабинет, «постоянно / только эта неделя». */
@Composable
fun ColumnScope.LessonSheet(dayIdx: Int, i: Int, wk: String, scope0: String? = null) {
    val ctx = LocalContext.current
    val ui = LocalUi.current
    val st = remember { Store.state.value }
    val lang = st.appLang
    val base = st.week.getOrNull(dayIdx)?.getOrNull(i)
    val ov = st.overrides.firstOrNull { it.week == wk && it.day == dayIdx && it.idx == i }
    val cur: Lesson? = if (ov != null) ov.lesson else base
    val isNew = base == null && ov == null
    var subj by remember { mutableStateOf(cur?.subj ?: "") }
    var room by remember { mutableStateOf(cur?.room ?: "") }
    var all by remember { mutableStateOf(false) }
    var scope by remember { mutableStateOf(scope0 ?: if (ov != null) "week" else "perm") }

    val prog = Catalog.programSubjects(st.program, st.grade)
    var ids = if (all) Catalog.subjects.map { it.id } + st.custom.map { it.id } else prog + st.custom.map { it.id }
    if (subj.isNotEmpty() && subj !in ids) ids = listOf(subj) + ids

    SheetTitle(Tr.t(lang, "lesson_title", i + 1, Tr.dayLong(lang, dayIdx)))
    Lab(Tr.t(lang, "subject"))
    LazyVerticalGrid(
        GridCells.Fixed(3), Modifier.fillMaxWidth().heightIn(max = 210.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(ids) { id ->
            val v = Sched.subj(st, id, lang)
            val on = id == subj
            val shape = RoundedCornerShape(14.dp)
            Column(
                Modifier.clip(shape).background(if (on) T.accSoft else T.s2).border(2.dp, if (on) T.acc else Color.Transparent, shape)
                    .tap { subj = id; if (room.isBlank()) room = st.custom.firstOrNull { it.id == id }?.room ?: Catalog.roomOf(id) }.padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Tile(v.emoji, v.color, 32.dp)
                Text(v.name, color = T.tx, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    Row(Modifier.padding(top = 8.dp)) { Chip(Tr.t(lang, "all_subjects"), all) { all = !all } }
    Lab(Tr.t(lang, "roomField"))
    Field(room, { room = it.take(20) }, Tr.t(lang, "room_ph"))
    if (!isNew) {
        Lab(Tr.t(lang, "apply_to"))
        Seg(listOf("perm" to Tr.t(lang, "perm"), "week" to Tr.t(lang, "thisWeek")), scope) { scope = it }
    }
    Btns {
        Btn(Tr.t(lang, "save"), Modifier.weight(1f), enabled = subj.isNotEmpty()) {
            val l = Lesson(subj, room.trim())
            Store.update(ctx) { s ->
                if (scope == "week" && !isNew) {
                    s.copy(overrides = s.overrides.filterNot { it.week == wk && it.day == dayIdx && it.idx == i } + Override(wk, dayIdx, i, l))
                } else {
                    val days = s.week.map { it.toMutableList() }.toMutableList()
                    while (days.size < 6) days += mutableListOf<Lesson>()
                    val d = days[dayIdx]
                    if (i < d.size) d[i] = l else d += l
                    s.copy(week = days)
                }
            }
            ui.close()
        }
        if (ov != null) Btn(Tr.t(lang, "remove_swap"), Modifier.weight(1f), BtnKind.Sec) {
            Store.update(ctx) { s -> s.copy(overrides = s.overrides.filterNot { it.week == wk && it.day == dayIdx && it.idx == i }) }
            ui.close()
        }
        if (!isNew) Btn("", kind = BtnKind.Danger, icon = Ic.trash) {
            Store.update(ctx) { s ->
                if (scope == "week") {
                    s.copy(overrides = s.overrides.filterNot { it.week == wk && it.day == dayIdx && it.idx == i } + Override(wk, dayIdx, i, null))
                } else {
                    val days = s.week.map { it.toMutableList() }.toMutableList()
                    if (i < days[dayIdx].size) days[dayIdx].removeAt(i)
                    s.copy(week = days)
                }
            }
            ui.close()
        }
    }
}

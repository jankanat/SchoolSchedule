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
    val lang = st.appLang
    val today = LocalDate.now()
    val mon = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val wk = mon.toString()
    val nd = st.schoolDays
    var sel by rememberSaveable { mutableStateOf((today.dayOfWeek.value - 1).coerceAtMost(nd - 1)) }
    if (sel >= nd) sel = 0
    var view by rememberSaveable { mutableStateOf("days") }
    val swaps = st.overrides.count { it.week == wk }
    val end = mon.plusDays((nd - 1).toLong())
    val ms = MS[lang] ?: MS.getValue("ru")
    val range = "${mon.dayOfMonth} ${ms[mon.monthValue - 1]} – ${end.dayOfMonth} ${ms[end.monthValue - 1]}"

    fun edit(dayIdx: Int, i: Int) { ui.open { LessonSheet(dayIdx, i, wk) } }

    Page {
        item { Hdr(Tr.t(lang, "tab_schedule"), Tr.t(lang, "week") + " " + range + if (swaps > 0) " · " + Tr.t(lang, "swaps", swaps) else "") }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Seg(listOf("days" to Tr.t(lang, "view_days"), "table" to Tr.t(lang, "view_table")), view) { view = it }
                Spacer(Modifier.height(12.dp))
            }
        }
        if (view == "days") {
            item {
                Box(Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)) {
                    EqualPills(
                        (0 until nd).map { it to Tr.dayShort(lang, it) }, sel,
                        dots = st.overrides.filter { it.week == wk }.map { it.day }.toSet(),
                    ) { sel = it }
                }
            }
            val date = mon.plusDays(sel.toLong())
            val L = Sched.dayLessons(st, date)
            if (L.isEmpty()) item { Empty(Tr.t(lang, "emptyDay")) }
            itemsIndexed(L) { i, s ->
                Box(Modifier.padding(horizontal = 16.dp)) { SlotRow(st, L, i, LsState.Normal, lang) { edit(sel, s.idx) } }
            }
            item {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    Btn("+ " + Tr.t(lang, "addLesson"), Modifier.fillMaxWidth(), BtnKind.Sec) {
                        val n = maxOf(st.week.getOrNull(sel)?.size ?: 0, (L.maxOfOrNull { it.idx } ?: -1) + 1)
                        edit(sel, n)
                    }
                }
            }
        } else {
            item { Box(Modifier.padding(horizontal = 16.dp)) { WeekTable(st, mon, nd, lang) { d, i -> edit(d, i) } } }
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
private fun ColumnScope.LessonSheet(dayIdx: Int, i: Int, wk: String) {
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
    var scope by remember { mutableStateOf(if (ov != null) "week" else "perm") }

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
                    .tap { subj = id; if (room.isBlank()) room = Catalog.roomOf(id) }.padding(horizontal = 4.dp, vertical = 8.dp),
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
        if (!isNew) Btn("🗑", kind = BtnKind.Danger) {
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

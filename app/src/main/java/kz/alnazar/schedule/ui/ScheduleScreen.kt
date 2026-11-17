package kz.alnazar.schedule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

/** Строка редактора: номер урока, урок (или пусто), признак замены. */
private data class EdRow(val idx: Int, val lesson: Lesson?, val replaced: Boolean)

/** Что редактируем в диалоге. */
private data class Edit(val idx: Int, val lesson: Lesson?, val isNew: Boolean)

@Composable
fun ScheduleScreen(st: AppState) {
    val ctx = LocalContext.current
    val lang = st.appLang
    val today = LocalDate.now()
    var day by rememberSaveable { mutableStateOf((today.dayOfWeek.value - 1).coerceAtMost(st.schoolDays - 1)) }
    if (day >= st.schoolDays) day = 0
    var weekMode by rememberSaveable { mutableStateOf(false) }
    var edit by remember { mutableStateOf<Edit?>(null) }

    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val wk = monday.toString()
    val base = st.week.getOrNull(day) ?: emptyList()
    val ov = st.overrides.filter { it.week == wk && it.day == day }
    val rows: List<EdRow> = if (!weekMode) base.mapIndexed { i, l -> EdRow(i, l, false) } else {
        val n = maxOf(base.size, (ov.maxOfOrNull { it.idx } ?: -1) + 1)
        (0 until n).map { i ->
            val o = ov.firstOrNull { it.idx == i }
            if (o != null) EdRow(i, o.lesson, true) else EdRow(i, base.getOrNull(i), false)
        }
    }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Text(Tr.t(lang, "tab_schedule"), color = Pal.tx, fontSize = 30.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp))
        }
        item {
            ChipRow(
                listOf(false to Tr.t(lang, "permanent"), true to Tr.t(lang, "thisWeek")),
                weekMode,
            ) { weekMode = it }
        }
        item {
            ChipRow(
                (0 until st.schoolDays).map { it to Tr.dayShort(lang, it) },
                day,
            ) { day = it }
        }
        if (weekMode) item {
            val date = monday.plusDays(day.toLong())
            Text(
                Tr.dateTitle(lang, date), color = Pal.tx2, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }
        if (rows.isEmpty()) item {
            Text(Tr.t(lang, "emptyDay"), color = Pal.tx2, fontSize = 15.sp, modifier = Modifier.padding(20.dp))
        }
        items(rows) { r ->
            val bell = st.bells.getOrNull(r.idx)
            val sameRoom = r.idx > 0 && r.lesson != null && rows.getOrNull(r.idx - 1)?.lesson?.room?.trim() == r.lesson.room.trim() && r.lesson.room.isNotBlank()
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 4.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp)).background(Pal.card)
                    .clickable { edit = Edit(r.idx, r.lesson, false) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${r.idx + 1}", color = Pal.tx2, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(18.dp))
                if (r.lesson != null) {
                    val v = Sched.subj(st, r.lesson.subj, lang)
                    EmojiTile(v.emoji, v.color, 38.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(v.name, color = Pal.tx, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val room = when {
                            r.lesson.room.isBlank() -> ""
                            sameRoom -> Tr.t(lang, "sameRoom")
                            r.lesson.room.first().isDigit() -> Tr.t(lang, "room", r.lesson.room)
                            else -> r.lesson.room
                        }
                        val tag = if (r.replaced) " · " + Tr.t(lang, "replaced") else ""
                        if (room.isNotEmpty() || tag.isNotEmpty())
                            Text(room + tag, color = if (r.replaced) Pal.warn else Pal.tx2, fontSize = 13.sp, maxLines = 1)
                    }
                } else {
                    Text("—  " + Tr.t(lang, "cancelled"), color = Pal.red, fontSize = 15.sp, modifier = Modifier.weight(1f).padding(start = 10.dp))
                }
                if (bell != null) Column(horizontalAlignment = Alignment.End) {
                    Text(Tr.hm(bell.start), color = Pal.tx, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(Tr.hm(bell.end), color = Pal.tx2, fontSize = 13.sp)
                }
            }
        }
        item {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (rows.size < 10) TextButton(onClick = { edit = Edit(rows.size, null, true) }) { Text("+ " + Tr.t(lang, "addLesson"), color = Pal.acc) }
                if (weekMode && ov.isNotEmpty()) TextButton(onClick = {
                    Store.update(ctx) { s -> s.copy(overrides = s.overrides.filterNot { it.week == wk && it.day == day }) }
                }) { Text(Tr.t(lang, "resetWeek"), color = Pal.warn) }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    edit?.let { e ->
        LessonDialog(
            st = st, e = e, weekMode = weekMode,
            onDismiss = { edit = null },
            onSave = { l ->
                Store.update(ctx) { s ->
                    if (!weekMode) {
                        val days = s.week.map { it.toMutableList() }.toMutableList()
                        while (days.size < 6) days += mutableListOf<Lesson>()
                        val d = days[day]
                        if (e.idx < d.size) d[e.idx] = l else d += l
                        s.copy(week = days)
                    } else {
                        val rest = s.overrides.filterNot { it.week == wk && it.day == day && it.idx == e.idx }
                        s.copy(overrides = rest + Override(wk, day, e.idx, l))
                    }
                }
                edit = null
            },
            onRemove = {
                Store.update(ctx) { s ->
                    if (!weekMode) {
                        val days = s.week.map { it.toMutableList() }.toMutableList()
                        if (e.idx < days[day].size) days[day].removeAt(e.idx)
                        s.copy(week = days)
                    } else {
                        // отменить урок только на эту неделю
                        val rest = s.overrides.filterNot { it.week == wk && it.day == day && it.idx == e.idx }
                        s.copy(overrides = rest + Override(wk, day, e.idx, null))
                    }
                }
                edit = null
            },
        )
    }
}

@Composable
private fun LessonDialog(
    st: AppState, e: Edit, weekMode: Boolean,
    onDismiss: () -> Unit, onSave: (Lesson) -> Unit, onRemove: () -> Unit,
) {
    val lang = st.appLang
    var subj by remember { mutableStateOf(e.lesson?.subj ?: "") }
    var room by remember { mutableStateOf(e.lesson?.room ?: "") }
    // предметы программы класса — первыми, остальные каталога — следом, затем свои
    val prog = Catalog.programSubjects(st.program, st.grade)
    val ids = prog + Catalog.subjects.map { it.id }.filterNot { it in prog } + st.custom.map { it.id }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Pal.card,
        title = { Text(Tr.t(lang, "lessonN", e.idx + 1), color = Pal.tx) },
        text = {
            Column {
                Text(Tr.t(lang, "subject"), color = Pal.tx2, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.heightIn(max = 300.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(ids) { id ->
                        val v = Sched.subj(st, id, lang)
                        val sel = id == subj
                        Column(
                            Modifier.clip(RoundedCornerShape(14.dp))
                                .background(if (sel) Pal.acc.copy(alpha = .35f) else Pal.card2)
                                .clickable {
                                    subj = id
                                    if (room.isBlank()) room = Catalog.roomOf(id)
                                }
                                .padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(v.emoji, fontSize = 20.sp)
                            Text(v.name, color = Pal.tx, fontSize = 11.sp, maxLines = 2, textAlign = TextAlign.Center, lineHeight = 13.sp)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = room, onValueChange = { room = it.take(20) },
                    label = { Text(Tr.t(lang, "roomField")) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = subj.isNotEmpty(), onClick = { onSave(Lesson(subj, room.trim())) }) {
                Text(Tr.t(lang, "save"), color = if (subj.isNotEmpty()) Pal.acc else Pal.tx3)
            }
        },
        dismissButton = {
            Row {
                if (!e.isNew) TextButton(onClick = onRemove) {
                    Text(Tr.t(lang, if (weekMode) "cancelLesson" else "delete"), color = Pal.red)
                }
                TextButton(onClick = onDismiss) { Text(Tr.t(lang, "cancel"), color = Pal.tx2) }
            }
        },
    )
}

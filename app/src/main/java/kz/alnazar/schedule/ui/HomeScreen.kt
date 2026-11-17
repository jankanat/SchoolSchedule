package kz.alnazar.schedule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.alnazar.schedule.data.AppState
import kz.alnazar.schedule.logic.Now
import kz.alnazar.schedule.logic.Phase
import kz.alnazar.schedule.logic.Sched
import kz.alnazar.schedule.logic.Slot
import kz.alnazar.schedule.logic.Tr

/** Строка урока из модели дня: подпись кабинета («в том же кабинете»), замена с зачёркнутым исходным предметом. */
@Composable
fun SlotRow(st: AppState, list: List<Slot>, i: Int, state: LsState, lang: String, onClick: (() -> Unit)? = null) {
    val s = list[i]
    val v = Sched.subj(st, s.lesson.subj, lang)
    val orig = if (s.replaced) st.week.getOrNull(s.dayIdx)?.getOrNull(s.idx)?.let { Sched.subj(st, it.subj, lang).name } else null
    LessonRow(
        num = s.idx + 1, emoji = v.emoji, color = v.color, name = v.name,
        sub = Sched.roomLabel(list, i, lang), start = Tr.hm(s.start), end = Tr.hm(s.end), state = state,
        tag = if (s.replaced) Tr.t(lang, "replaced") else null,
        was = orig?.takeIf { it != v.name }, onClick = onClick,
    )
}

@Composable
fun HomeScreen(st: AppState, clock: Long) {
    val n = remember(st, clock) { Sched.now(st) }
    val lang = st.appLang
    Page {
        item { Hdr(Tr.dateTitle(lang, n.date), Tr.t(lang, "hello_sub", st.grade, st.shift)) }
        item { Box(Modifier.padding(horizontal = 16.dp)) { Hero(st, n) } }
        if (n.today.isNotEmpty()) {
            item { Box(Modifier.padding(horizontal = 16.dp)) { Sec(Tr.t(lang, "today")) } }
            val past = when (n.phase) { Phase.LESSON -> n.cur - 1; Phase.BREAK -> n.prev; Phase.AFTER -> n.today.size - 1; else -> -1 }
            itemsIndexed(n.today) { i, _ ->
                Box(Modifier.padding(horizontal = 16.dp)) {
                    SlotRow(st, n.today, i, if (n.phase == Phase.LESSON && i == n.cur) LsState.Cur else if (i <= past) LsState.Past else LsState.Normal, lang)
                }
            }
        } else if (n.nextDay != null) {
            val nd = n.nextDay
            item { Box(Modifier.padding(horizontal = 16.dp)) { Sec(Tr.t(lang, "nearest") + " · " + Sched.dayLabel(lang, n.date, nd)) } }
            itemsIndexed(n.nextDayLessons) { i, _ ->
                Box(Modifier.padding(horizontal = 16.dp)) { SlotRow(st, n.nextDayLessons, i, LsState.Normal, lang) }
            }
        }
    }
}

/** Большая карточка «Сейчас» (.hero): градиент акцентного цвета. */
@Composable
private fun Hero(st: AppState, n: Now) {
    val lang = st.appLang
    val fg = T.accTx
    Column(
        Modifier.padding(bottom = 6.dp).fillMaxWidth().clip(RoundedCornerShape(T.rc * 1.1f)).background(T.heroBrush).padding(16.dp),
    ) {
        @Composable
        fun hl(a: String, b: String) = Row(Modifier.fillMaxWidth()) {
            Text(a.uppercase(), color = fg.copy(alpha = .92f), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, letterSpacing = .5.sp, modifier = Modifier.weight(1f))
            Text(b, color = fg.copy(alpha = .85f), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        }
        val L = n.today
        when (n.phase) {
            Phase.LESSON -> {
                val c = L[n.cur]; val v = Sched.subj(st, c.lesson.subj, lang)
                hl(Tr.t(lang, "now"), Tr.t(lang, "left", n.minutesLeft))
                Row(Modifier.padding(top = 12.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Tile(v.emoji, v.color, 54.dp, onAccent = true)
                    Column {
                        Text(v.name, color = fg, fontSize = 23.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp)
                        Text(Sched.meta(L, n.cur, lang), color = fg.copy(alpha = .88f), fontSize = 13.5.sp, modifier = Modifier.padding(top = 3.dp))
                    }
                }
                val pr = (1f - n.minutesLeft.toFloat() / (c.end - c.start).coerceAtLeast(1)).coerceIn(0f, 1f)
                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(9.dp)).background(Color.White.copy(alpha = .28f))) {
                    Box(Modifier.fillMaxWidth(pr).height(6.dp).clip(RoundedCornerShape(9.dp)).background(fg.copy(alpha = .9f)))
                }
                NextStrip(st, n, lang)
            }
            Phase.BREAK, Phase.BEFORE -> {
                val x = L[n.next]; val v = Sched.subj(st, x.lesson.subj, lang)
                hl(Tr.t(lang, if (n.phase == Phase.BEFORE) "soon" else "break"), Tr.t(lang, "startsIn", n.minutesLeft))
                Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Tile(v.emoji, v.color, 54.dp, onAccent = true)
                    Column {
                        Text(v.name, color = fg, fontSize = 23.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp)
                        val r = Sched.roomLabel(L, n.next, lang)
                        Text(Tr.t(lang, "next") + " · " + Tr.hm(x.start) + if (r.isNotEmpty()) " · $r" else "",
                            color = fg.copy(alpha = .88f), fontSize = 13.5.sp, modifier = Modifier.padding(top = 3.dp))
                    }
                }
            }
            else -> {
                val I = Sched.idle(n, lang)
                Row(Modifier.padding(top = 6.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(I.em, fontSize = 38.sp)
                    Column {
                        Text(I.title, color = fg, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        if (I.day.isNotEmpty()) Text(Tr.t(lang, "nearest") + ": " + I.day, color = fg.copy(alpha = .85f), fontSize = 13.5.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun NextStrip(st: AppState, n: Now, lang: String) {
    val fg = T.accTx
    Row(
        Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = .16f)).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (n.next < 0) { Text(Tr.t(lang, "lastLesson"), color = fg.copy(alpha = .85f), fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold); return@Row }
        val x = n.today[n.next]; val v = Sched.subj(st, x.lesson.subj, lang)
        Text(Tr.t(lang, "next"), color = fg.copy(alpha = .85f), fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
        Tile(v.emoji, v.color, 22.dp, onAccent = true)
        Text(v.name, color = fg, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(Tr.hm(x.start), color = fg.copy(alpha = .85f), fontSize = 13.5.sp)
    }
}

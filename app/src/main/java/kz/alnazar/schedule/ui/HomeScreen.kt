package kz.alnazar.schedule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@Composable
fun HomeScreen(st: AppState, clock: Long) {
    val n = remember(st, clock) { Sched.now(st) }
    val lang = st.appLang
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 6.dp)) {
                Text(Tr.dateTitle(lang, n.date), color = Pal.tx, fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp)
                Text(
                    "${st.grade} ${Tr.t(lang, "grade").lowercase()} · ${Tr.t(lang, if (st.shift == 2) "shift2" else "shift1")}",
                    color = Pal.tx2, fontSize = 13.sp,
                )
            }
        }
        item { Hero(st, n) }
        if (n.today.isNotEmpty()) {
            item { SectionTitle(Tr.t(lang, "today")) }
            itemsIndexed(n.today) { i, slot -> LessonRow(st, n.today, i, slot, n) }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun Hero(st: AppState, n: Now) {
    val lang = st.appLang
    val p = when (n.phase) { Phase.LESSON -> n.cur; Phase.BREAK, Phase.BEFORE -> n.next; else -> -1 }
    Column(
        Modifier.padding(horizontal = 14.dp, vertical = 8.dp).fillMaxWidth()
            .clip(RoundedCornerShape(26.dp)).background(Pal.card).padding(16.dp),
    ) {
        if (p < 0) {
            val title = when (n.phase) {
                Phase.AFTER -> "✅ " + Tr.t(lang, "doneToday")
                Phase.DAYOFF -> "😴 " + Tr.t(lang, "weekend")
                else -> "💤 " + Tr.t(lang, "noLessons")
            }
            Text(title, color = Pal.tx, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            val nd = n.nextDay
            if (nd != null && n.nextDayLessons.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(Sched.dayLabel(lang, n.date, nd), color = Pal.acc, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                n.nextDayLessons.forEach { s ->
                    val v = Sched.subj(st, s.lesson.subj, lang)
                    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(Tr.hm(s.start), color = Pal.tx2, fontSize = 13.sp, modifier = Modifier.width(48.dp))
                        Text(v.emoji + "  " + v.name, color = Pal.tx, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            return@Column
        }
        val slot = n.today[p]
        val v = Sched.subj(st, slot.lesson.subj, lang)
        val label = when (n.phase) {
            Phase.LESSON -> Tr.t(lang, "now")
            Phase.BREAK -> Tr.t(lang, "break") + " · " + Tr.t(lang, "next").lowercase()
            else -> Tr.t(lang, "before") + " · " + Tr.t(lang, "next").lowercase()
        }
        val left = if (n.phase == Phase.LESSON) Tr.t(lang, "left", n.minutesLeft) else Tr.t(lang, "until", n.minutesLeft)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label.uppercase(), color = Pal.acc, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(left, color = Pal.tx2, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmojiTile(v.emoji, v.color, 52.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(v.name, color = Pal.tx, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp)
                val room = Sched.roomLabel(n.today, p, lang)
                Text(
                    Tr.hm(slot.start) + "–" + Tr.hm(slot.end) + if (room.isNotEmpty()) " · $room" else "",
                    color = Pal.tx2, fontSize = 14.sp,
                )
            }
        }
        if (n.phase == Phase.LESSON) {
            Spacer(Modifier.height(14.dp))
            val total = (slot.end - slot.start).coerceAtLeast(1)
            LinearProgressIndicator(
                progress = { 1f - n.minutesLeft.toFloat() / total },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = Pal.tx, trackColor = Pal.card2,
            )
        }
        val after = p + 1
        if (after < n.today.size) {
            val ns = n.today[after]
            val nv = Sched.subj(st, ns.lesson.subj, lang)
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Pal.card2).padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(Tr.t(lang, "next"), color = Pal.tx2, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(nv.emoji + " " + nv.name, color = Pal.tx, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(Tr.hm(ns.start), color = Pal.tx2, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun LessonRow(st: AppState, list: List<Slot>, i: Int, slot: Slot, n: Now) {
    val lang = st.appLang
    val v = Sched.subj(st, slot.lesson.subj, lang)
    val active = (n.phase == Phase.LESSON && i == n.cur)
    val past = when (n.phase) {
        Phase.LESSON -> i < n.cur
        Phase.BREAK -> i <= n.prev
        Phase.AFTER -> true
        else -> false
    }
    val a = if (past) 0.45f else 1f
    Row(
        Modifier.padding(horizontal = 14.dp, vertical = 4.dp).fillMaxWidth()
            .clip(RoundedCornerShape(18.dp)).background(if (active) Pal.card2 else Pal.card)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${slot.idx + 1}", color = Pal.tx2.copy(alpha = a), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(18.dp))
        EmojiTile(v.emoji, v.color, 38.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(v.name, color = Pal.tx.copy(alpha = a), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val room = Sched.roomLabel(list, i, lang)
            val extra = if (slot.replaced) " · " + Tr.t(lang, "replaced") else ""
            if (room.isNotEmpty() || extra.isNotEmpty())
                Text(room + extra, color = (if (slot.replaced) Pal.warn else Pal.tx2).copy(alpha = a), fontSize = 13.sp, maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(Tr.hm(slot.start), color = Pal.tx.copy(alpha = a), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(Tr.hm(slot.end), color = Pal.tx2.copy(alpha = a), fontSize = 13.sp)
        }
    }
}

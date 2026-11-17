package kz.alnazar.schedule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.alnazar.schedule.data.AppState
import kz.alnazar.schedule.logic.Now
import kz.alnazar.schedule.logic.Phase
import kz.alnazar.schedule.logic.Sched
import kz.alnazar.schedule.logic.Slot
import kz.alnazar.schedule.logic.Tr
import kz.alnazar.schedule.widget.pickSmall

/* Превью виджетов внутри приложения — разметка 1:1 из прототипа (04-widgets.js + CSS .wg). */

private val WBG = Color(0x80343442)     // rgba(52,52,66,.5)
private val WBD = Color(0x29FFFFFF)     // rgba(255,255,255,.16)

@Composable
fun WidgetFrame(w: Int, h: Int, pad: androidx.compose.foundation.layout.PaddingValues, content: @Composable ColumnScope.() -> Unit) {
    // подложка виджета под стиль (значения --wbg / --wbd / --rw из прототипа)
    val (bgc, bd, r) = when (T.id) {
        "pixel" -> Triple(T.s2, Color.Transparent, 28)
        "ios18" -> Triple(Color(0xFF1C1C1E), Color.Transparent, 24)
        "color" -> Triple(Color(0xE6242424), Color(0x47FFFFFF), 22)
        "origin" -> Triple(Color(0xB8222531), Color(0x14FFFFFF), 28)
        "oneui" -> Triple(Color(0xFF262629), Color.Transparent, 30)
        else -> Triple(Color.Unspecified, Color.Unspecified, 34)
    }
    val fill = T.wfill
    if (T.id != "ios26" || fill != null) {
        val sh = RoundedCornerShape(r.dp)
        var m = Modifier.size(w.dp, h.dp).clip(sh).background(fill ?: bgc)
        if (bd != Color.Transparent) m = m.border(if (T.glow) 1.2.dp else 1.dp, Brush.verticalGradient(listOf(bd, bd.copy(alpha = bd.alpha * .25f))), sh)
        Column(m.padding(pad), content = content)
        return
    }
    val shape = RoundedCornerShape(34.dp)
    Column(
        Modifier.size(w.dp, h.dp).clip(shape).background(WBG).border(1.dp, WBD, shape)
            .drawWithContent {
                drawContent()
                drawRect(Brush.linearGradient(0f to Color(0x52FFFFFF), .3f to Color.Transparent, .7f to Color.Transparent, 1f to Color(0x1AFFFFFF)))
            }
            .padding(pad),
        content = content,
    )
}

@Composable
private fun Lb(a: String, b: String = "", size: TextUnit = 10.sp, center: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (center) Arrangement.Center else Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        Text(if (T.secUpper) a.uppercase() else a, color = T.acc, fontSize = if (T.secUpper) size else size * 1.1f, fontWeight = FontWeight.Bold, letterSpacing = if (T.secUpper) .5.sp else 0.sp, maxLines = 2,
            textAlign = if (center) TextAlign.Center else TextAlign.Start, lineHeight = (size.value * 1.2f).sp)
        if (b.isNotEmpty()) Text(b, color = T.tx2, fontSize = size, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun Mt(text: String, size: TextUnit = 10.5.sp, center: Boolean = false, lines: Int = 1) {
    if (text.isNotEmpty()) Text(text, color = T.tx2, fontSize = size, maxLines = lines, overflow = TextOverflow.Ellipsis, textAlign = if (center) TextAlign.Center else TextAlign.Start)
}

@Composable
private fun Nm(text: String, size: TextUnit, lines: Int = 1, center: Boolean = false, weight: FontWeight = FontWeight.Bold) {
    Text(text, color = T.tx, fontSize = size, fontWeight = weight, maxLines = lines, overflow = TextOverflow.Ellipsis,
        lineHeight = (size.value * 1.15f).sp, textAlign = if (center) TextAlign.Center else TextAlign.Start)
}

private fun minutes(n: Int, lang: String) = Tr.t(lang, "left", n)

@Composable
fun WidgetPreview(size: String, st: AppState, n: Now, lang: String) {
    val cfg = st.widget
    val live = n.phase == Phase.LESSON || n.phase == Phase.BREAK || n.phase == Phase.BEFORE
    val L = n.today
    fun sv(s: Slot) = Sched.subj(st, s.lesson.subj, lang)
    val idle = Sched.idle(n, lang)
    val nd = n.nextDayLessons
    when (size) {
        /* ---------- 2×2 ---------- */
        "2x2" -> WidgetFrame(160, 160, androidx.compose.foundation.layout.PaddingValues(12.dp)) {
            if (!live) {
                Lb(if (cfg.idleNoLessons) idle.title else idle.day, if (cfg.idleNoLessons) idle.day else "")
                if (cfg.idleNextDay && nd.isNotEmpty()) {
                    Spacer(Modifier.height(3.dp))
                    nd.take(4).forEach { s -> RowMini(s, sv(s).emoji, sv(s).color, sv(s).name) }
                    if (nd.size > 4) Mt("+${nd.size - 4}")
                } else BigIdle(idle.em, idle.title)
                return@WidgetFrame
            }
            if (n.phase == Phase.LESSON) {
                val c = L[n.cur]; val v = sv(c)
                Lb(Tr.t(lang, "now"), minutes(n.minutesLeft, lang))
                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Tile(v.emoji, v.color, 28.dp)
                    Column { Nm(v.name, 15.sp, 2); Mt(Sched.meta(L, n.cur, lang, cfg.time, cfg.room)) }
                }
            } else {
                Lb(Tr.t(lang, if (n.phase == Phase.BEFORE) "soon" else "break"), Tr.t(lang, "startsIn", n.minutesLeft))
                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Tile("☕", 0xFFFF9F0A, 32.dp)
                    Column { Nm(Tr.t(lang, "break"), 15.sp); Mt(Tr.t(lang, "next") + ": " + Tr.hm(L[n.next].start)) }
                }
            }
            Spacer(Modifier.weight(.5f))
            Box(Modifier.fillMaxWidth().padding(top = 1.dp, bottom = 2.dp).height(1.dp).background(T.line))
            if (n.next >= 0) {
                val x = L[n.next]; val v = sv(x)
                Lb(Tr.t(lang, "next"))
                Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Tile(v.emoji, v.color, 16.dp); Nm(v.name, 12.5.sp)
                }
                Mt(Sched.meta(L, n.next, lang, cfg.time, cfg.room))
            } else Mt(Tr.t(lang, "lastLesson"))
        }
        /* ---------- 2×1 ---------- */
        "2x1" -> WidgetFrame(160, 76, androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 10.dp)) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!live) {
                    if (cfg.idleNextDay && nd.isNotEmpty()) {
                        val f = nd.first(); val v = sv(f)
                        Tile(v.emoji, v.color, 30.dp)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Lb(if (cfg.idleNoLessons) idle.title else idle.day, if (cfg.idleNoLessons) idle.day else "")
                            Nm(v.name, 14.5.sp)
                            Mt(Tr.t(lang, "lessonsCnt", nd.size) + " · " + Tr.t(lang, "from", Tr.hm(f.start)))
                        }
                    } else {
                        Tile(idle.em, 0xFF8E8E93, 36.dp)
                        Column { Lb(idle.day); Nm(idle.title, 14.5.sp) }
                    }
                    return@Row
                }
                val p = pickSmall(st, n, lang) ?: return@Row
                val x = L[p.pos]; val v = sv(x)
                Tile(v.emoji, v.color, 30.dp)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Lb(p.label); Nm(v.name, 14.5.sp); Mt(Sched.meta(L, p.pos, lang, cfg.time, cfg.room))
                }
            }
        }
        /* ---------- 1×2 ---------- */
        "1x2" -> WidgetFrame(76, 160, androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 9.dp)) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                if (!live) {
                    if (cfg.idleNextDay && nd.isNotEmpty()) {
                        val f = nd.first(); val v = sv(f)
                        Lb(if (cfg.idleNoLessons) idle.title else idle.day, size = 8.5.sp, center = true)
                        Tile(v.emoji, v.color, 34.dp)
                        Nm(v.name, 10.5.sp, 3, center = true)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Mt(idle.day, 9.sp, true); Mt(Tr.hm(f.start), 9.sp, true) }
                    } else {
                        Lb(idle.day, size = 8.5.sp, center = true)
                        Tile(idle.em, 0xFF8E8E93, 34.dp)
                        Nm(idle.title, 10.5.sp, 3, center = true)
                        Spacer(Modifier.height(1.dp))
                    }
                    return@Column
                }
                val p = pickSmall(st, n, lang) ?: return@Column
                val x = L[p.pos]; val v = sv(x)
                Lb(p.label.replace(" · ", "\n"), size = 8.5.sp, center = true)
                Tile(v.emoji, v.color, 34.dp)
                Nm(v.name, 10.5.sp, 3, center = true)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (cfg.time) Mt(Tr.hm(x.start) + "–" + Tr.hm(x.end), 9.sp, true)
                    if (cfg.room) Mt(Sched.roomLabel(L, p.pos, lang), 9.sp, true, 2)
                }
            }
        }
        /* ---------- 2×4 ---------- */
        "2x4" -> WidgetFrame(160, 328, androidx.compose.foundation.layout.PaddingValues(12.dp)) {
            if (!live) {
                Hd(if (cfg.idleNoLessons) idle.title else idle.day, if (cfg.idleNoLessons) idle.day else "")
                if (cfg.idleNextDay && nd.isNotEmpty()) Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    nd.take(8).forEachIndexed { i, s -> Lr(st, nd, i, lang, LsState.Normal) }
                } else BigIdle(idle.em, idle.title)
                return@WidgetFrame
            }
            val stx = if (n.phase == Phase.LESSON) minutes(n.minutesLeft, lang) else "☕ " + Tr.t(lang, "startsIn", n.minutesLeft)
            Hd(Tr.dayLong(lang, n.date.dayOfWeek.value - 1), stx)
            val past = when (n.phase) { Phase.LESSON -> n.cur - 1; Phase.BREAK -> n.prev; else -> -1 }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                L.take(8).forEachIndexed { i, _ ->
                    Lr(st, L, i, lang, if (n.phase == Phase.LESSON && i == n.cur) LsState.Cur else if (i <= past) LsState.Past else LsState.Normal)
                }
            }
        }
        /* ---------- 4×2 ---------- */
        else -> WidgetFrame(328, 160, androidx.compose.foundation.layout.PaddingValues(12.dp)) {
            if (!live) {
                Hd(if (cfg.idleNoLessons) idle.title else idle.day, if (cfg.idleNoLessons) idle.day else "")
                if (cfg.idleNextDay && nd.isNotEmpty()) Cols(st, nd, 0, -1, lang) else BigIdle(idle.em, idle.title)
                return@WidgetFrame
            }
            val start = if (n.phase == Phase.LESSON) n.cur else n.next
            val stx = if (n.phase == Phase.LESSON) Tr.t(lang, "now") + " · " + minutes(n.minutesLeft, lang)
                      else Tr.t(lang, "break") + " · " + Tr.t(lang, "startsIn", n.minutesLeft)
            Hd(Tr.dayLong(lang, n.date.dayOfWeek.value - 1), stx)
            Cols(st, L, start, if (n.phase == Phase.LESSON) n.cur else -1, lang)
        }
    }
}

@Composable
private fun Hd(b: String, s: String) {
    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        Text(b, color = T.tx, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        if (s.isNotEmpty()) Text(s, color = T.tx2, fontSize = 10.5.sp, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun ColumnScope.BigIdle(em: String, title: String) {
    Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(em, fontSize = 30.sp)
        Text(title, color = T.tx, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun RowMini(s: Slot, emoji: String, color: Long, name: String) {
    Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("${s.idx + 1}", color = T.tx2, fontSize = 9.5.sp, modifier = Modifier.width(9.dp), textAlign = TextAlign.Center)
        Tile(emoji, color, 18.dp)
        Text(name, color = T.tx, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(Tr.hm(s.start), color = T.tx2, fontSize = 9.5.sp)
    }
}

@Composable
private fun ColumnScope.Lr(st: AppState, L: List<Slot>, i: Int, lang: String, state: LsState) {
    val s = L[i]; val v = Sched.subj(st, s.lesson.subj, lang)
    val a = if (state == LsState.Past) .42f else 1f
    Row(
        Modifier.weight(1f, fill = false).heightIn(max = 36.dp).fillMaxWidth().clip(RoundedCornerShape(11.dp))
            .background(if (state == LsState.Cur) T.accSoft else Color.Transparent)
            .padding(start = 4.dp, end = 6.dp, top = 2.dp, bottom = 2.dp).graphicsAlpha(a),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text("${s.idx + 1}", color = T.tx2, fontSize = 10.sp, modifier = Modifier.width(10.dp), textAlign = TextAlign.Center)
        Tile(v.emoji, v.color, 24.dp)
        Column(Modifier.weight(1f)) {
            Text(v.name, color = T.tx, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Mt(Sched.meta(L, i, lang, st.widget.time, st.widget.room, startOnly = true), 9.5.sp)
        }
    }
}

@Composable
private fun ColumnScope.Cols(st: AppState, L: List<Slot>, start: Int, cur: Int, lang: String) {
    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        val items = L.drop(start).take(4)
        items.forEachIndexed { k, s ->
            val i = start + k; val v = Sched.subj(st, s.lesson.subj, lang)
            val isCur = i == cur
            val shape = RoundedCornerShape(14.dp)
            var m = Modifier.weight(1f).fillMaxHeight().clip(shape).background(if (isCur) T.accSoft else T.tx.copy(alpha = .07f))
            if (isCur) m = m.border(1.2.dp, T.acc, shape)
            Column(m.padding(horizontal = 6.dp, vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Tile(v.emoji, v.color, 26.dp)
                Text(v.name, color = T.tx, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 12.sp, modifier = Modifier.padding(top = 2.dp))
                if (st.widget.time) Mt(Tr.hm(s.start), 9.5.sp)
                if (st.widget.room) Mt(Sched.roomLabel(L, i, lang), 9.5.sp)
            }
        }
        repeat(4 - items.size) { Spacer(Modifier.weight(1f)) }
    }
}

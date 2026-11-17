package kz.alnazar.schedule.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kz.alnazar.schedule.R
import kz.alnazar.schedule.data.AppState
import kz.alnazar.schedule.data.Store
import kz.alnazar.schedule.logic.Now
import kz.alnazar.schedule.logic.Phase
import kz.alnazar.schedule.logic.Sched
import kz.alnazar.schedule.logic.Slot
import kz.alnazar.schedule.logic.Tr
import kz.alnazar.schedule.ui.MainActivity

/* ===== Цвета стиля iOS 26 (тёмная тема). Остальные стили и темы — на следующем этапе. ===== */
private val TX = ColorProvider(Color(0xFFFFFFFF))
private val TX2 = ColorProvider(Color(0x99FFFFFF))
private val TX3 = ColorProvider(Color(0x59FFFFFF))
private val ACC = ColorProvider(Color(0xFF0A84FF))

private fun st(size: TextUnit, color: ColorProvider = TX, bold: Boolean = false) =
    TextStyle(color = color, fontSize = size, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)

/** Общая основа всех виджетов: стекло, клик открывает приложение, пересчёт «сейчас» при каждом тике. */
abstract class LessonWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Single

    @Composable
    abstract fun Body(s: AppState, n: Now, lang: String)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Store.ensure(context)
        provideContent {
            val s by Store.state.collectAsState()
            val tick by Store.tick.collectAsState()
            val n = remember(s, tick) { Sched.now(s) }
            Box(
                GlanceModifier.fillMaxSize()
                    .background(ImageProvider(R.drawable.glass_dark))
                    .cornerRadius(26.dp)
                    .clickable(actionStartActivity<MainActivity>())
                    .padding(12.dp),
            ) { Body(s, n, s.widgetLang) }
        }
    }
}

/* ===== Общие кусочки ===== */

@Composable
private fun Tile(s: AppState, slot: Slot, lang: String, size: Dp, font: TextUnit) {
    val v = Sched.subj(s, slot.lesson.subj, lang)
    Box(
        GlanceModifier.size(size).background(Color(v.color).copy(alpha = .30f)).cornerRadius(size / 3.5f),
        contentAlignment = Alignment.Center,
    ) { Text(v.emoji, style = TextStyle(fontSize = font)) }
}

private fun name(s: AppState, slot: Slot, lang: String) = Sched.subj(s, slot.lesson.subj, lang).name
private fun time(slot: Slot) = Tr.hm(slot.start) + "–" + Tr.hm(slot.end)
private fun meta(n: Now, pos: Int, lang: String, withTime: Boolean = true): String {
    val slot = n.today[pos]
    val room = Sched.roomLabel(n.today, pos, lang)
    val t = if (withTime) time(slot) else Tr.hm(slot.start)
    return if (room.isEmpty()) t else "$t · $room"
}

/** Какой урок главный для большого блока и как его подписать. */
private fun mainPos(n: Now): Int = when (n.phase) {
    Phase.LESSON -> n.cur
    Phase.BREAK, Phase.BEFORE -> n.next
    else -> -1
}

private fun mainLabel(n: Now, lang: String) = when (n.phase) {
    Phase.LESSON -> Tr.t(lang, "now")
    Phase.BREAK -> Tr.t(lang, "break") + " · " + Tr.t(lang, "next").lowercase()
    Phase.BEFORE -> Tr.t(lang, "before") + " · " + Tr.t(lang, "next").lowercase()
    else -> ""
}

/** День без уроков: надпись «Уроков нет» и/или таблица ближайшего учебного дня (переключатели в настройках). */
@Composable
private fun Idle(s: AppState, n: Now, lang: String, maxRows: Int, compact: Boolean = false) {
    val title = when (n.phase) {
        Phase.AFTER -> "✅ " + Tr.t(lang, "doneToday")
        Phase.DAYOFF -> "😴 " + Tr.t(lang, "weekend")
        else -> "💤 " + Tr.t(lang, "noLessons")
    }
    Column(GlanceModifier.fillMaxSize()) {
        if (s.widget.idleNoLessons) Text(title, style = st(if (compact) 12.sp else 14.sp, bold = true), maxLines = 1)
        val nd = n.nextDay
        if (s.widget.idleNextDay && nd != null && n.nextDayLessons.isNotEmpty() && maxRows > 0) {
            if (s.widget.idleNoLessons) Spacer(GlanceModifier.height(6.dp))
            Text(Sched.dayLabel(lang, n.date, nd), style = st(11.sp, ACC, bold = true), maxLines = 1)
            Spacer(GlanceModifier.height(3.dp))
            n.nextDayLessons.take(maxRows).forEach { slot ->
                Row(GlanceModifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(Tr.hm(slot.start), style = st(10.sp, TX2), maxLines = 1)
                    Spacer(GlanceModifier.width(6.dp))
                    Text(Sched.subj(s, slot.lesson.subj, lang).emoji + " " + name(s, slot, lang), style = st(12.sp), maxLines = 1)
                }
            }
        }
    }
}

/** Большой блок «сейчас»: плитка, название, время и кабинет. */
@Composable
private fun MainBlock(s: AppState, n: Now, lang: String) {
    val p = mainPos(n)
    val slot = n.today[p]
    Text(mainLabel(n, lang), style = st(11.sp, ACC, bold = true), maxLines = 1)
    Spacer(GlanceModifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Tile(s, slot, lang, 28.dp, 15.sp)
        Spacer(GlanceModifier.width(8.dp))
        Column {
            Text(name(s, slot, lang), style = st(15.sp, bold = true), maxLines = 2)
        }
    }
    Spacer(GlanceModifier.height(3.dp))
    Text(meta(n, p, lang), style = st(11.sp, TX2), maxLines = 2)
}

/* ===== 2×2: текущий и следующий урок ===== */
class W22 : LessonWidget() {
    @Composable
    override fun Body(s: AppState, n: Now, lang: String) {
        val p = mainPos(n)
        if (p < 0) { Idle(s, n, lang, 3); return }
        Column(GlanceModifier.fillMaxSize()) {
            MainBlock(s, n, lang)
            Spacer(GlanceModifier.defaultWeight())
            val after = p + 1
            if (after < n.today.size) {
                val slot = n.today[after]
                Text(Tr.t(lang, "next"), style = st(10.sp, TX3, bold = true), maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(Sched.subj(s, slot.lesson.subj, lang).emoji + " " + name(s, slot, lang), style = st(12.sp, bold = true), maxLines = 1,
                        modifier = GlanceModifier.defaultWeight())
                    Text(" " + Tr.hm(slot.start), style = st(11.sp, TX2), maxLines = 1)
                }
            }
        }
    }
}

/* ===== 2×1 и 1×2: один урок, что именно — по настройке ===== */
private data class Pick(val pos: Int, val label: String)

private fun pickSmall(s: AppState, n: Now, lang: String): Pick? = when (n.phase) {
    Phase.LESSON -> if (s.widget.onLesson == "next" && n.next >= 0) Pick(n.next, Tr.t(lang, "next"))
                    else Pick(n.cur, Tr.t(lang, "now"))
    Phase.BREAK -> if (s.widget.onBreak == "prev" && n.prev >= 0) Pick(n.prev, Tr.t(lang, "break") + " · " + Tr.t(lang, "prev").lowercase())
                   else Pick(n.next, Tr.t(lang, "break") + " · " + Tr.t(lang, "next").lowercase())
    Phase.BEFORE -> Pick(n.next, Tr.t(lang, "next"))
    else -> null
}

class W21 : LessonWidget() {
    @Composable
    override fun Body(s: AppState, n: Now, lang: String) {
        val pk = pickSmall(s, n, lang)
        if (pk == null) { Idle(s, n, lang, 1, compact = true); return }
        val slot = n.today[pk.pos]
        Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Tile(s, slot, lang, 30.dp, 16.sp)
            Spacer(GlanceModifier.width(8.dp))
            Column(GlanceModifier.defaultWeight()) {
                Text(pk.label, style = st(10.sp, ACC, bold = true), maxLines = 1)
                Text(name(s, slot, lang), style = st(13.sp, bold = true), maxLines = 1)
                Text(meta(n, pk.pos, lang), style = st(10.sp, TX2), maxLines = 1)
            }
        }
    }
}

class W12 : LessonWidget() {
    @Composable
    override fun Body(s: AppState, n: Now, lang: String) {
        val pk = pickSmall(s, n, lang)
        if (pk == null) { Idle(s, n, lang, 3, compact = true); return }
        val slot = n.today[pk.pos]
        Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(pk.label, style = st(10.sp, ACC, bold = true), maxLines = 2)
            Spacer(GlanceModifier.height(6.dp))
            Tile(s, slot, lang, 34.dp, 18.sp)
            Spacer(GlanceModifier.height(6.dp))
            Text(name(s, slot, lang), style = st(13.sp, bold = true), maxLines = 2)
            Spacer(GlanceModifier.defaultWeight())
            Text(Tr.hm(slot.start), style = st(11.sp, TX2), maxLines = 1)
            val room = Sched.roomLabel(n.today, pk.pos, lang)
            if (room.isNotEmpty()) Text(room, style = st(10.sp, TX2), maxLines = 2)
        }
    }
}

/* ===== 2×4: все уроки дня списком, текущий подсвечен ===== */
class W24 : LessonWidget() {
    @Composable
    override fun Body(s: AppState, n: Now, lang: String) {
        if (mainPos(n) < 0) { Idle(s, n, lang, 8); return }
        Column(GlanceModifier.fillMaxSize()) {
            Text(Tr.dayLong(lang, n.date.dayOfWeek.value - 1), style = st(14.sp, bold = true), maxLines = 1)
            Text(mainLabel(n, lang), style = st(10.sp, ACC, bold = true), maxLines = 1)
            Spacer(GlanceModifier.height(6.dp))
            n.today.forEachIndexed { i, slot ->
                val active = i == mainPos(n)
                val past = n.phase != Phase.BEFORE && i < (if (n.phase == Phase.LESSON) n.cur else n.next)
                var m = GlanceModifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp)
                if (active) m = m.background(Color(0x400A84FF)).cornerRadius(10.dp)
                Row(m, verticalAlignment = Alignment.CenterVertically) {
                    Text(Sched.subj(s, slot.lesson.subj, lang).emoji, style = TextStyle(fontSize = 13.sp))
                    Spacer(GlanceModifier.width(6.dp))
                    Text(name(s, slot, lang), style = st(12.sp, if (past) TX3 else TX, bold = active), maxLines = 1,
                        modifier = GlanceModifier.defaultWeight())
                    Text(Tr.hm(slot.start), style = st(10.sp, if (past) TX3 else TX2), maxLines = 1)
                }
            }
        }
    }
}

/* ===== 4×2: слева «сейчас», справа следующие уроки ===== */
class W42 : LessonWidget() {
    @Composable
    override fun Body(s: AppState, n: Now, lang: String) {
        val p = mainPos(n)
        if (p < 0) { Idle(s, n, lang, 4); return }
        Row(GlanceModifier.fillMaxSize()) {
            Column(GlanceModifier.defaultWeight().fillMaxHeight()) { MainBlock(s, n, lang) }
            Spacer(GlanceModifier.width(10.dp))
            Column(GlanceModifier.defaultWeight().fillMaxHeight()) {
                Text(Tr.t(lang, "next"), style = st(11.sp, TX3, bold = true), maxLines = 1)
                Spacer(GlanceModifier.height(4.dp))
                n.today.drop(p + 1).take(4).forEach { slot ->
                    Row(GlanceModifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(Sched.subj(s, slot.lesson.subj, lang).emoji, style = TextStyle(fontSize = 12.sp))
                        Spacer(GlanceModifier.width(5.dp))
                        Text(name(s, slot, lang), style = st(12.sp), maxLines = 1, modifier = GlanceModifier.defaultWeight())
                        Text(Tr.hm(slot.start), style = st(10.sp, TX2), maxLines = 1)
                    }
                }
            }
        }
    }
}

/* ===== Приёмники виджетов (по одному на размер) ===== */
class W22Receiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = W22() }
class W21Receiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = W21() }
class W12Receiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = W12() }
class W24Receiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = W24() }
class W42Receiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = W42() }

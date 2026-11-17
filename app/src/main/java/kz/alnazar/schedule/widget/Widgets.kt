package kz.alnazar.schedule.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.Image
import kz.alnazar.schedule.ui.IconRes
import kz.alnazar.schedule.ui.withCustom
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
/** Палитра виджета для текущего стиля приложения. */
internal data class WPal(
    val upper: Boolean, val tileR: Float, val tx: Color, val tx2: Color, val tx3: Color, val acc: Color, val soft: Color,
    val colBg: Color, val line: Color, val tileBw: Color, val bg: Int, val radius: Dp,
    val fill: Color? = null,   // своя тема: сплошная заливка вместо картинки-фона
)

/** Своя тема для виджета: форма от стиля, цвета — из настроек пользователя. */
private fun customPal(base: WPal, s: kz.alnazar.schedule.data.AppState): WPal {
    val t = kz.alnazar.schedule.ui.tokFor(s.style).withCustom(s.ct)
    val c = s.ct
    val dark = c.bl < .55f
    val fill = kz.alnazar.schedule.ui.hsl(c.bh, c.bs, c.bl + (if (dark) .07f else -.03f), (c.alpha + .2f).coerceAtMost(1f))
    return base.copy(
        tx = t.tx, tx2 = t.tx.copy(alpha = .62f), tx3 = t.tx.copy(alpha = .36f), acc = t.acc,
        soft = t.acc.copy(alpha = .2f), colBg = t.tx.copy(alpha = .07f), line = t.tx.copy(alpha = .12f),
        tileBw = t.tx.copy(alpha = .16f), fill = fill,
    )
}
private fun pal(upper: Boolean, tileR: Float, acc: Color, soft: Color, bg: Int, r: Int, tx: Color = Color.White, tx2: Color = Color(0xFF9B9BA3)) =
    WPal(upper, tileR, tx, tx2, tx2.copy(alpha = .55f), acc, soft, Color(0x12FFFFFF), Color(0x1FFFFFFF), Color(0xFF2C2C2E), bg, r.dp)
private val IOS = WPal(true, .3f, Color(0xFFFFFFFF), Color(0x99FFFFFF), Color(0x59FFFFFF), Color(0xFF0A84FF), Color(0x2B0A84FF),
    Color(0x12FFFFFF), Color(0x1FFFFFFF), Color(0x42828296), R.drawable.glass_dark, 34.dp)
private val PIXEL = WPal(false, .5f, Color(0xFFE6E0E9), Color(0xFFCAC4D0), Color(0x80CAC4D0), Color(0xFFD0BCFF), Color(0xFF4A4458),
    Color(0xFF36343B), Color(0x29CAC4D0), Color(0xFF36343B), R.drawable.pixel_dark, 28.dp)
private val IOS18 = pal(true, .24f, Color(0xFF0A84FF), Color(0x2B0A84FF), R.drawable.ios18_dark, 24)
private val COLOR = pal(false, .5f, Color(0xFF35D07F), Color(0x2B35D07F), R.drawable.color_dark, 22)
private val ORIGIN = pal(false, .34f, Color(0xFF7C93FF), Color(0x2B7C93FF), R.drawable.origin_dark, 28)
private val ONEUI = pal(false, .36f, Color(0xFF4DA3FF), Color(0x2B4DA3FF), R.drawable.oneui_dark, 30)
private fun palFor(style: String) = when (style) {
    "pixel" -> PIXEL; "ios18" -> IOS18; "color" -> COLOR; "origin" -> ORIGIN; "oneui" -> ONEUI; else -> IOS
}
internal val LocalWP = staticCompositionLocalOf { IOS }

private val TX: ColorProvider @Composable get() = ColorProvider(LocalWP.current.tx)
private val TX2: ColorProvider @Composable get() = ColorProvider(LocalWP.current.tx2)
private val TX3: ColorProvider @Composable get() = ColorProvider(LocalWP.current.tx3)
private val ACC: ColorProvider @Composable get() = ColorProvider(LocalWP.current.acc)

@Composable
private fun st(size: TextUnit, color: ColorProvider = TX, bold: Boolean = false) =
    TextStyle(color = color, fontSize = size, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)

/** Общая основа всех виджетов: стекло, клик открывает приложение, пересчёт «сейчас» при каждом тике. */
abstract class LessonWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Single

    @Composable
    abstract fun Body(s: AppState, n: Now, lang: String)
    open val padH: Dp = 12.dp
    open val padV: Dp = 12.dp

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Store.ensure(context)
        provideContent {
            val s by Store.state.collectAsState()
            val tick by Store.tick.collectAsState()
            val n = remember(s, tick) { Sched.now(s) }
            val pal = palFor(s.style).let { if (s.theme == "custom") customPal(it, s) else it }
            CompositionLocalProvider(LocalWP provides pal) {
                Box(
                    GlanceModifier.fillMaxSize()
                        .let { m -> pal.fill?.let { m.background(it) } ?: m.background(ImageProvider(pal.bg)) }
                        .cornerRadius(pal.radius)
                        .clickable(actionStartActivity<MainActivity>())
                        .padding(horizontal = padH, vertical = padV),
                ) { Body(s, n, s.widgetLang) }
            }
        }
    }
}


/* ===== Общие кусочки (разметка 1:1 с прототипом: .lb, .nm, .mt, .ic) ===== */

private val LINE: ColorProvider @Composable get() = ColorProvider(LocalWP.current.line)
private val SOFT: Color @Composable get() = LocalWP.current.soft
private val COLBG: Color @Composable get() = LocalWP.current.colBg

@Composable
private fun TileE(s: AppState, emoji: String, color: Long, size: Dp) {
    val key = IconRes.key(emoji)
    val mono = s.iconSet == "mono" && key != null
    val bg = when {
        mono && s.iconTint == "accent" -> SOFT
        mono && s.iconTint == "bw" -> LocalWP.current.tileBw
        else -> Color(color).copy(alpha = .24f)
    }
    val res = when {
        key == null -> null
        s.iconSet == "fluent" -> IconRes.fluent[key]
        s.iconSet == "twemoji" -> IconRes.twemoji[key]
        s.iconSet == "mono" -> IconRes.mono[key]
        else -> null
    }
    Box(GlanceModifier.size(size).background(bg).cornerRadius(size * LocalWP.current.tileR), contentAlignment = Alignment.Center) {
        when {
            res != null && mono -> Image(
                ImageProvider(res), contentDescription = null, modifier = GlanceModifier.size(size * .6f),
                colorFilter = ColorFilter.tint(ColorProvider(when (s.iconTint) { "accent" -> LocalWP.current.acc; "bw" -> LocalWP.current.tx; else -> Color(color) })),
            )
            res != null -> Image(ImageProvider(res), contentDescription = null, modifier = GlanceModifier.size(size * if (s.iconSet == "fluent") .7f else .6f))
            else -> Text(emoji, style = TextStyle(fontSize = (size.value * .55f).sp))
        }
    }
}

@Composable
private fun TileS(s: AppState, slot: Slot, lang: String, size: Dp) {
    val v = Sched.subj(s, slot.lesson.subj, lang); TileE(s, v.emoji, v.color, size)
}

private fun name(s: AppState, slot: Slot, lang: String) = Sched.subj(s, slot.lesson.subj, lang).name

/** Подпись-метка: слева акцентом капсом, справа серым. */
@Composable
private fun Lb(a: String, b: String = "", size: TextUnit = 10.sp, center: Boolean = false) {
    Row(GlanceModifier.fillMaxWidth(), horizontalAlignment = if (center) Alignment.CenterHorizontally else Alignment.Start, verticalAlignment = Alignment.Bottom) {
        Text(if (LocalWP.current.upper) a.uppercase() else a, style = st(size, ACC, bold = true), maxLines = 2, modifier = if (b.isNotEmpty()) GlanceModifier.defaultWeight() else GlanceModifier)
        if (b.isNotEmpty()) Text(b, style = st(size, TX2), maxLines = 1)
    }
}

@Composable
private fun Mt(text: String, size: TextUnit = 10.5.sp, lines: Int = 1) {
    if (text.isNotEmpty()) Text(text, style = st(size, TX2), maxLines = lines)
}

@Composable
private fun Big(em: String, title: String) {
    Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
        Text(em, style = TextStyle(fontSize = 30.sp))
        Text(title, style = st(15.sp, bold = true), maxLines = 2)
    }
}

@Composable
private fun Hd(b: String, s: String) {
    Row(GlanceModifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.Bottom) {
        Text(b, style = st(15.sp, bold = true), maxLines = 1, modifier = GlanceModifier.defaultWeight())
        if (s.isNotEmpty()) Text(s, style = st(10.5.sp, TX2), maxLines = 1)
    }
}

/** до какого времени идёт урок / когда начнётся следующий (вместо обратного отсчёта — виджет обновляется по звонкам) */
private fun tillCur(n: Now, lang: String) = Tr.t(lang, "till", Tr.hm(n.today[n.cur].end))
private fun atNext(n: Now) = Tr.hm(n.today[n.next].start)

private fun live(n: Now) = n.phase == Phase.LESSON || n.phase == Phase.BREAK || n.phase == Phase.BEFORE

/* ===== 2×2: сейчас + далее ===== */
class W22 : LessonWidget() {
    @Composable
    override fun Body(s: AppState, n: Now, lang: String) {
        val cfg = s.widget; val L = n.today
        Column(GlanceModifier.fillMaxSize()) {
            if (!live(n)) {
                val I = Sched.idle(n, lang); val nd = n.nextDayLessons
                Lb(if (cfg.idleNoLessons) I.title else I.day, if (cfg.idleNoLessons) I.day else "")
                if (cfg.idleNextDay && nd.isNotEmpty()) {
                    nd.take(4).forEach { x -> MiniRow(s, x, lang) }
                    if (nd.size > 4) Mt("+${nd.size - 4}")
                } else Big(I.em, I.title)
                return@Column
            }
            if (n.phase == Phase.LESSON) {
                Lb(Tr.t(lang, "now"), tillCur(n, lang))
                Row(GlanceModifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    TileS(s, L[n.cur], lang, 28.dp)
                    Spacer(GlanceModifier.width(9.dp))
                    Column {
                        Text(name(s, L[n.cur], lang), style = st(15.sp, bold = true), maxLines = 2)
                        Mt(Sched.meta(L, n.cur, lang, cfg.time, cfg.room))
                    }
                }
            } else {
                Lb(Tr.t(lang, if (n.phase == Phase.BEFORE) "soon" else "break"), atNext(n))
                Row(GlanceModifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    TileE(s, "☕", 0xFFFF9F0A, 32.dp)
                    Spacer(GlanceModifier.width(9.dp))
                    Column {
                        Text(Tr.t(lang, "break"), style = st(15.sp, bold = true), maxLines = 1)
                        Mt(Tr.t(lang, "next") + ": " + atNext(n))
                    }
                }
            }
            Spacer(GlanceModifier.defaultWeight())
            Box(GlanceModifier.fillMaxWidth().height(1.dp).background(LINE)) {}
            Spacer(GlanceModifier.height(2.dp))
            if (n.next >= 0) {
                val x = L[n.next]
                Lb(Tr.t(lang, "next"))
                Row(GlanceModifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    TileS(s, x, lang, 16.dp)
                    Spacer(GlanceModifier.width(6.dp))
                    Text(name(s, x, lang), style = st(12.5.sp, bold = true), maxLines = 1)
                }
                Mt(Sched.meta(L, n.next, lang, cfg.time, cfg.room))
            } else Mt(Tr.t(lang, "lastLesson"))
        }
    }
}

@Composable
private fun MiniRow(s: AppState, x: Slot, lang: String) {
    Row(GlanceModifier.fillMaxWidth().padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("${x.idx + 1}", style = st(9.5.sp, TX2))
        Spacer(GlanceModifier.width(6.dp))
        TileS(s, x, lang, 18.dp)
        Spacer(GlanceModifier.width(6.dp))
        Text(name(s, x, lang), style = st(11.5.sp, bold = true), maxLines = 1, modifier = GlanceModifier.defaultWeight())
        Text(Tr.hm(x.start), style = st(9.5.sp, TX2), maxLines = 1)
    }
}

/* ===== 2×1 и 1×2: один урок, что именно — по настройке ===== */
internal data class Pick(val pos: Int, val label: String)

internal fun pickSmall(s: AppState, n: Now, lang: String): Pick? = when (n.phase) {
    Phase.LESSON -> if (s.widget.onLesson == "next" && n.next >= 0) Pick(n.next, Tr.t(lang, "next"))
                    else Pick(n.cur, Tr.t(lang, "now"))
    Phase.BREAK -> if (s.widget.onBreak == "prev" && n.prev >= 0) Pick(n.prev, Tr.t(lang, "break") + " · " + Tr.t(lang, "prev").lowercase())
                   else Pick(n.next, Tr.t(lang, "break") + " · " + Tr.t(lang, "next").lowercase())
    Phase.BEFORE -> Pick(n.next, Tr.t(lang, "soon") + " · " + Tr.t(lang, "next").lowercase())
    else -> null
}

internal fun mainPos(n: Now): Int = when (n.phase) {
    Phase.LESSON -> n.cur
    Phase.BREAK, Phase.BEFORE -> n.next
    else -> -1
}

class W21 : LessonWidget() {
    override val padV: Dp = 10.dp
    @Composable
    override fun Body(s: AppState, n: Now, lang: String) {
        val cfg = s.widget
        Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            if (!live(n)) {
                val I = Sched.idle(n, lang); val nd = n.nextDayLessons
                if (cfg.idleNextDay && nd.isNotEmpty()) {
                    val f = nd.first()
                    TileS(s, f, lang, 30.dp)
                    Spacer(GlanceModifier.width(10.dp))
                    Column(GlanceModifier.defaultWeight()) {
                        Lb(if (cfg.idleNoLessons) I.title else I.day, if (cfg.idleNoLessons) I.day else "")
                        Text(name(s, f, lang), style = st(14.5.sp, bold = true), maxLines = 1)
                        Mt(Tr.t(lang, "lessonsCnt", nd.size) + " · " + Tr.t(lang, "from", Tr.hm(f.start)))
                    }
                } else {
                    TileE(s, I.em, 0xFF8E8E93, 36.dp)
                    Spacer(GlanceModifier.width(10.dp))
                    Column(GlanceModifier.defaultWeight()) { Lb(I.day); Text(I.title, style = st(14.5.sp, bold = true), maxLines = 1) }
                }
                return@Row
            }
            val p = pickSmall(s, n, lang) ?: return@Row
            val x = n.today[p.pos]
            TileS(s, x, lang, 30.dp)
            Spacer(GlanceModifier.width(10.dp))
            Column(GlanceModifier.defaultWeight()) {
                Lb(p.label)
                Text(name(s, x, lang), style = st(14.5.sp, bold = true), maxLines = 1)
                Mt(Sched.meta(n.today, p.pos, lang, cfg.time, cfg.room))
            }
        }
    }
}

class W12 : LessonWidget() {
    override val padH: Dp = 6.dp
    override val padV: Dp = 9.dp
    @Composable
    override fun Body(s: AppState, n: Now, lang: String) {
        val cfg = s.widget
        Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (!live(n)) {
                val I = Sched.idle(n, lang); val nd = n.nextDayLessons
                if (cfg.idleNextDay && nd.isNotEmpty()) {
                    val f = nd.first()
                    Lb(if (cfg.idleNoLessons) I.title else I.day, size = 8.5.sp, center = true)
                    Spacer(GlanceModifier.defaultWeight())
                    TileS(s, f, lang, 34.dp)
                    Text(name(s, f, lang), style = st(10.5.sp, bold = true), maxLines = 3)
                    Spacer(GlanceModifier.defaultWeight())
                    Mt(I.day, 9.sp); Mt(Tr.hm(f.start), 9.sp)
                } else {
                    Lb(I.day, size = 8.5.sp, center = true)
                    Spacer(GlanceModifier.defaultWeight())
                    TileE(s, I.em, 0xFF8E8E93, 34.dp)
                    Text(I.title, style = st(10.5.sp, bold = true), maxLines = 3)
                    Spacer(GlanceModifier.defaultWeight())
                }
                return@Column
            }
            val p = pickSmall(s, n, lang) ?: return@Column
            val x = n.today[p.pos]
            Lb(p.label.replace(" · ", "\n"), size = 8.5.sp, center = true)
            Spacer(GlanceModifier.defaultWeight())
            TileS(s, x, lang, 34.dp)
            Spacer(GlanceModifier.height(4.dp))
            Text(name(s, x, lang), style = st(10.5.sp, bold = true), maxLines = 3)
            Spacer(GlanceModifier.defaultWeight())
            if (cfg.time) Mt(Tr.hm(x.start) + "–" + Tr.hm(x.end), 9.sp)
            if (cfg.room) Mt(Sched.roomLabel(n.today, p.pos, lang), 9.sp, 2)
        }
    }
}

/* ===== 2×4: уроки дня списком ===== */
class W24 : LessonWidget() {
    @Composable
    override fun Body(s: AppState, n: Now, lang: String) {
        val cfg = s.widget
        Column(GlanceModifier.fillMaxSize()) {
            if (!live(n)) {
                val I = Sched.idle(n, lang); val nd = n.nextDayLessons
                Hd(if (cfg.idleNoLessons) I.title else I.day, if (cfg.idleNoLessons) I.day else "")
                if (cfg.idleNextDay && nd.isNotEmpty()) nd.take(8).forEachIndexed { i, _ -> Lr(s, nd, i, lang, 0) }
                else Big(I.em, I.title)
                return@Column
            }
            val stx = if (n.phase == Phase.LESSON) tillCur(n, lang) else "☕ " + atNext(n)
            Hd(Tr.dayLong(lang, n.date.dayOfWeek.value - 1), stx)
            val past = when (n.phase) { Phase.LESSON -> n.cur - 1; Phase.BREAK -> n.prev; else -> -1 }
            n.today.take(8).forEachIndexed { i, _ ->
                Lr(s, n.today, i, lang, if (n.phase == Phase.LESSON && i == n.cur) 1 else if (i <= past) 2 else 0)
            }
        }
    }
}

/** Строка списка: 0 — обычная, 1 — текущая (подсветка), 2 — прошедшая (бледная). */
@Composable
private fun Lr(s: AppState, L: List<Slot>, i: Int, lang: String, state: Int) {
    val x = L[i]
    val tx = if (state == 2) TX3 else TX
    val tx2 = if (state == 2) TX3 else TX2
    var m = GlanceModifier.fillMaxWidth().padding(start = 4.dp, end = 6.dp, top = 2.dp, bottom = 2.dp)
    if (state == 1) m = m.background(SOFT).cornerRadius(11.dp)
    Row(m, verticalAlignment = Alignment.CenterVertically) {
        Text("${x.idx + 1}", style = st(10.sp, tx2))
        Spacer(GlanceModifier.width(7.dp))
        TileS(s, x, lang, 24.dp)
        Spacer(GlanceModifier.width(7.dp))
        Column(GlanceModifier.defaultWeight()) {
            Text(name(s, x, lang), style = st(12.5.sp, tx, bold = true), maxLines = 1)
            val meta = Sched.meta(L, i, lang, s.widget.time, s.widget.room, startOnly = true)
            if (meta.isNotEmpty()) Text(meta, style = st(9.5.sp, tx2), maxLines = 1)
        }
    }
}

/* ===== 4×2: четыре урока колонками ===== */
class W42 : LessonWidget() {
    @Composable
    override fun Body(s: AppState, n: Now, lang: String) {
        val cfg = s.widget
        Column(GlanceModifier.fillMaxSize()) {
            if (!live(n)) {
                val I = Sched.idle(n, lang); val nd = n.nextDayLessons
                Hd(if (cfg.idleNoLessons) I.title else I.day, if (cfg.idleNoLessons) I.day else "")
                if (cfg.idleNextDay && nd.isNotEmpty()) Cols(s, nd, 0, -1, lang) else Big(I.em, I.title)
                return@Column
            }
            val start = if (n.phase == Phase.LESSON) n.cur else n.next
            val stx = if (n.phase == Phase.LESSON) Tr.t(lang, "now") + " · " + tillCur(n, lang)
                      else Tr.t(lang, "break") + " · " + atNext(n)
            Hd(Tr.dayLong(lang, n.date.dayOfWeek.value - 1), stx)
            Cols(s, n.today, start, if (n.phase == Phase.LESSON) n.cur else -1, lang)
        }
    }
}

@Composable
private fun Cols(s: AppState, L: List<Slot>, start: Int, cur: Int, lang: String) {
    Row(GlanceModifier.fillMaxSize()) {
        val items = L.drop(start).take(4)
        for (k in 0 until 4) {
            if (k > 0) Spacer(GlanceModifier.width(7.dp))
            val x = items.getOrNull(k)
            if (x == null) { Spacer(GlanceModifier.defaultWeight()); continue }
            val i = start + k
            Column(
                GlanceModifier.defaultWeight().fillMaxHeight().background(if (i == cur) SOFT else COLBG).cornerRadius(14.dp)
                    .padding(horizontal = 6.dp, vertical = 7.dp),
            ) {
                TileS(s, x, lang, 26.dp)
                Spacer(GlanceModifier.height(4.dp))
                Text(name(s, x, lang), style = st(11.sp, bold = true), maxLines = 2)
                if (s.widget.time) Mt(Tr.hm(x.start), 9.5.sp)
                if (s.widget.room) Mt(Sched.roomLabel(L, i, lang), 9.5.sp)
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

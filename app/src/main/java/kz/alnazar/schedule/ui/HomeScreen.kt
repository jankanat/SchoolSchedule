package kz.alnazar.schedule.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.alnazar.schedule.data.AppState
import kz.alnazar.schedule.data.Hw
import kz.alnazar.schedule.data.Store
import kz.alnazar.schedule.logic.Now
import kz.alnazar.schedule.logic.Phase
import kz.alnazar.schedule.logic.Sched
import kz.alnazar.schedule.logic.Slot
import kz.alnazar.schedule.logic.Tr
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/* ===================== Данные главного экрана ===================== */

/** Что показывать в большой карточке. */
private data class HeroInfo(
    val kind: Phase,                 // LESSON / BREAK / BEFORE, иначе — «свободно»
    val slot: Slot?,                 // текущий (на уроке) или следующий урок
    val num: Int,                    // номер урока по порядку
    val big: String,                 // крупная цифра
    val unit: String,                // подпись к цифре
    val label: String,               // «Сейчас · 3-й урок»
    val meta: String,                // «каб. 305 · 10:20 – 11:05»
    val progress: Float,
    val next: Slot?,                 // урок после текущего
    val nextLabel: String,
)

private fun heroOf(st: AppState, n: Now, lang: String): HeroInfo? {
    val L = n.today
    fun meta(i: Int): String {
        val s = L[i]
        val r = Sched.roomLabel(L, i, lang)
        return listOfNotNull(r.takeIf { it.isNotEmpty() }, Tr.hm(s.start) + " – " + Tr.hm(s.end)).joinToString(" · ")
    }
    return when (n.phase) {
        Phase.LESSON -> {
            val s = L[n.cur]
            val nx = L.getOrNull(n.cur + 1)
            HeroInfo(Phase.LESSON, s, n.cur + 1, "${n.minutesLeft}", Tr.t(lang, "min_end"),
                Tr.t(lang, "now_n", n.cur + 1), meta(n.cur),
                ((n.minute - s.start).toFloat() / (s.end - s.start).coerceAtLeast(1)).coerceIn(0f, 1f),
                nx, nx?.let { Tr.t(lang, "next_brk", (it.start - s.end).coerceAtLeast(0)) } ?: "")
        }
        Phase.BREAK, Phase.BEFORE -> {
            val s = L[n.next]
            val prevEnd = if (n.phase == Phase.BREAK) L[n.prev].end else s.start - 60
            val big = if (n.minutesLeft <= 99) "${n.minutesLeft}" else Tr.hm(s.start)
            HeroInfo(n.phase, s, n.next + 1, big, if (n.minutesLeft <= 99) Tr.t(lang, if (n.phase == Phase.BREAK) "min_bell" else "min_start") else "",
                if (n.phase == Phase.BREAK) Tr.t(lang, "break") + " · " + Tr.t(lang, "next").lowercase() else Tr.t(lang, "before"),
                meta(n.next),
                ((n.minute - prevEnd).toFloat() / (s.start - prevEnd).coerceAtLeast(1)).coerceIn(0f, 1f),
                L.getOrNull(n.next + 1), "")
        }
        else -> null
    }
}

/** Строки ленты для даты: с заменами; на сегодня — прошедшие, текущий и прогресс. */
fun feedItems(st: AppState, date: LocalDate, lang: String, n: Now?, withCancelled: Boolean): List<LItem> {
    val all = Sched.dayAll(st, date).filter { withCancelled || !it.cancelled }
    val slots = all.map { it.slot }
    val isToday = n != null && n.date == date
    return all.mapIndexed { i, ds ->
        val s = ds.slot
        val v = Sched.subj(st, s.lesson.subj, lang)
        val state = when {
            !isToday -> RowState.Normal
            n!!.phase == Phase.LESSON && n.minute >= s.start && n.minute < s.end -> RowState.Cur
            n.minute >= s.end || n.phase == Phase.AFTER -> RowState.Past
            else -> RowState.Normal
        }
        LItem(
            key = "${date}_${s.idx}", start = s.start, end = s.end, name = v.name, emoji = v.emoji, color = v.color,
            sub = Sched.roomLabel(slots, i, lang), state = state,
            hw = Sched.hasHw(st, s.lesson.subj, date),
            was = ds.wasSubj?.let { Sched.subj(st, it, lang).name },
            cancelled = ds.cancelled,
            progress = if (state == RowState.Cur) ((n!!.minute - s.start).toFloat() / (s.end - s.start).coerceAtLeast(1)) else 0f,
        )
    }
}

/* ===================== Главная ===================== */

@Composable
fun HomeScreen(st: AppState, clock: Long) {
    val n = remember(st, clock) { Sched.now(st) }
    val lang = st.appLang
    val ui = LocalUi.current
    val goTab = LocalGoTab.current
    val t = T
    val hero = remember(n) { heroOf(st, n, lang) }
    val week = remember(st, n.date) { Sched.week(st, n.date) }
    val idleDay = n.phase !in listOf(Phase.LESSON, Phase.BREAK, Phase.BEFORE)
    // Ближайший учебный день в Now заполнен только в простое, а переключатель «Завтра» нужен и во время уроков
    val nextD = remember(st, n.date) { n.nextDay ?: Sched.nextSchoolDay(st, n.date) }
    var showNext by rememberSaveable(n.date, idleDay) { mutableStateOf(idleDay && nextD != null) }
    val listDate = if (showNext) nextD ?: n.date else n.date
    val items = remember(st, n, listDate) { feedItems(st, listDate, lang, if (listDate == n.date) n else null, false) }
    val todo = remember(st) { Sched.todo(st) }
    val today = n.date
    val title = Tr.dayLong(lang, today.dayOfWeek.value - 1)
    val sub = Tr.dateDM(lang, today) + " · " + Tr.plural(lang, "lessons", n.today.size)
    val actions = listOf(Ic.bell to { ui.go("notif") }, (if (t.id.startsWith("ios")) Ic.gear else Ic.more) to { ui.go("settings") })
    val onLong: (Int) -> Unit = { i ->
        val ds = Sched.dayAll(st, listDate).filter { !it.cancelled }.getOrNull(i)
        if (ds != null) ui.open { LessonMenu(listDate, ds.slot) }
    }

    Page {
        // ---------- шапка ----------
        when (t.id) {
            "ios26", "ios18" -> item { ScreenHead(Tr.t(lang, "today"), Tr.dateTitle(lang, today), actions) }
            "pixel" -> item { ScreenHead(title, sub, actions, kicker = Tr.t(lang, "tab_schedule")) }
            "origin" -> item { OriginTop(lang, today, n, actions) }
            else -> item { ScreenHead(title, sub, actions) }
        }

        // ---------- дни недели (iOS, Pixel — над карточкой) ----------
        if (t.id.startsWith("ios") || t.pixel) item {
            Box(Modifier.padding(top = 14.dp).appear(1)) {
                DayStrip(lang, week, today, today) { d -> SchedBus.day = d; goTab(1) }
            }
        }

        // ---------- большая карточка ----------
        item {
            Box(Modifier.padding(top = if (t.id == "oneui") 0.dp else 18.dp).appear(2)) {
                when (t.id) {
                    "ios26", "ios18" -> HeroIos(hero, n, st, lang)
                    "pixel" -> HeroPixel(hero, n, st, lang)
                    "origin" -> HeroOrigin(hero, n, st, lang)
                    "oneui" -> HeroOneUi(hero, n, st, lang)
                    else -> HeroColor(hero, n, st, lang)
                }
            }
        }

        // ---------- вторая строка: неделя / плитки / кольца ----------
        item {
            Box(Modifier.padding(top = 12.dp).appear(3)) {
                when (t.id) {
                    "color" -> DayStrip(lang, week, today, today) { d -> SchedBus.day = d; goTab(1) }
                    "oneui" -> Gauges(hero, n, todo, lang)
                    else -> Tiles(hero, n, st, todo, lang) { goTab(2) }
                }
            }
        }
        if (t.id == "oneui") item { Box(Modifier.padding(top = 12.dp).appear(4)) { Tiles(hero, n, st, todo, lang) { goTab(2) } } }

        // ---------- уроки ----------
        item { Column {
            val passed = items.count { it.state == RowState.Past }
            SecHead(if (t.id == "origin" || t.id == "oneui") Tr.t(lang, "lessons_today") else Tr.t(lang, "lessons")) {
                if (t.id == "color" || t.pixel) {
                    HeadSeg(listOf(false to Tr.t(lang, "today"), true to Tr.t(lang, "tomorrow")), showNext) { showNext = it }
                } else if (items.isNotEmpty() && !showNext) {
                    Text(Tr.t(lang, "passed", passed, items.size), color = T.tx2, fontSize = 15.sp)
                }
            }
            if (t.id == "origin" && items.isNotEmpty()) DaySegments(items)
        } }
        item {
            AnimatedContent(listDate, transitionSpec = { (fadeIn(tween(220)) + scaleIn(initialScale = .98f)) togetherWith fadeOut(tween(120)) }, label = "feed") { _ ->
                if (items.isEmpty()) Empty(Tr.t(lang, "emptyDay"))
                else LessonFeed(items, lang, timeLeft(st.timeSide), Tr.t(lang, "replaced"), Tr.t(lang, "cancel_week"), onClick = onLong, onLong = onLong)
            }
        }

        // ---------- задания ----------
        item {
            SecHead(Tr.t(lang, "tasks")) { if (todo.isNotEmpty()) SecLink(Tr.t(lang, "all_n", todo.size)) { goTab(2) } }
        }
        item { HomeTasks(st, todo.take(if (t.id == "color") 8 else 3), today, lang) }
    }
}

/* ===================== Большие карточки ===================== */

@Composable
private fun HeroColor(h: HeroInfo?, n: Now, st: AppState, lang: String) {
    val shape = RoundedCornerShape(32.dp)
    if (h == null) { IdleHero(n, st, lang, shape, Brush.linearGradient(listOf(Color(0xFF14432A), Color(0xFF0D2A1B), Color(0xFF081A11)))); return }
    val v = Sched.subj(st, h.slot!!.lesson.subj, lang)
    val c = Color(v.color)
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(shape)
        .background(Brush.linearGradient(listOf(lerp(Color(0xFF0B1328), c, .35f), lerp(Color(0xFF0B1328), c, .12f), Color(0xFF0B1328))))
        .press(shape) {}.padding(22.dp)) {
        Column {
            Box(Modifier.fillMaxWidth().heightIn(min = 84.dp)) {
                Box(Modifier.align(Alignment.TopEnd)) { BigEmoji(v.emoji, 76.dp) }
                Column(Modifier.padding(end = 88.dp)) {
                    NowLabel(h.label, Color.White.copy(alpha = .75f))
                    Text(v.name, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                }
            }
            BigNumber(h.big, h.unit, 96.sp, FontWeight.Normal)
            ProgressLine(h.progress, Color.White, Color.White.copy(alpha = .14f), 8.dp, Modifier.padding(top = 18.dp))
            MetaRow(h.meta.substringBefore(" · ").takeIf { h.meta.contains(" · ") } ?: "", h.meta.substringAfterLast(" · "), Color.White.copy(alpha = .72f))
            h.next?.let { NextPill(st, it, h.nextLabel, lang, Color.White.copy(alpha = .09f)) }
        }
    }
}

@Composable
private fun HeroIos(h: HeroInfo?, n: Now, st: AppState, lang: String) {
    val shape = RoundedCornerShape(34.dp)
    if (h == null) { IdleHero(n, st, lang, shape, null); return }
    val v = Sched.subj(st, h.slot!!.lesson.subj, lang)
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).liquid(shape, T.acc.copy(alpha = .22f)).press(shape) {}.padding(18.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        Box(Modifier.size(124.dp), contentAlignment = Alignment.Center) {
            Ring(h.progress, T.acc, Color.White.copy(alpha = .14f), 9.dp, Modifier.size(124.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                // время вида 08:00 — одной строкой строго по центру кольца, не касаясь дуги
                Text(h.big, color = T.tx, fontSize = if (h.big.length > 3) 24.sp else 38.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false,
                    style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum", platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)))
                val u = h.unit.substringBefore("\n")
                if (u.isNotBlank()) Text(u, color = T.tx2, fontSize = 13.sp, maxLines = 1)
            }
        }
        Column(Modifier.weight(1f)) {
            NowLabel(h.label, T.tx2)
            Text(v.name, color = T.tx, fontSize = 28.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            Text(h.meta.replace(" · ", "\n"), color = T.tx2, fontSize = 15.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun HeroPixel(h: HeroInfo?, n: Now, st: AppState, lang: String) {
    val shape = RoundedCornerShape(36.dp)
    val pc = Color(0xFF4F378B); val onPc = Color(0xFFEADDFF)
    if (h == null) { IdleHero(n, st, lang, shape, Brush.linearGradient(listOf(pc, pc))); return }
    val v = Sched.subj(st, h.slot!!.lesson.subj, lang)
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(shape).background(pc).press(shape) {}.padding(20.dp)) {
        // волнистое кольцо прогресса вокруг «печеньки» с предметом
        Box(Modifier.align(Alignment.TopEnd).size(132.dp), contentAlignment = Alignment.Center) {
            WavyRing(h.progress, onPc, onPc.copy(alpha = .18f), Modifier.size(132.dp))
            Box(Modifier.size(96.dp).clip(FormShape(M3Form.Cookie)).background(Color(0xFF381E72)), contentAlignment = Alignment.Center) { BigEmoji(v.emoji, 42.dp) }
        }
        Column {
            Row(Modifier.clip(RoundedCornerShape(99.dp)).background(Color(0x38000000)).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF7CFFB2)))
                Text(h.label, color = onPc, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
            }
            Text(v.name, color = onPc, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(h.big, color = onPc, fontSize = 118.sp, lineHeight = 100.sp, fontFamily = CondensedFont, fontWeight = FontWeight.ExtraBold)
                if (h.unit.isNotEmpty()) Text(Tr.t(lang, "min_n5").take(3).uppercase(), color = onPc, fontSize = 30.sp, fontFamily = CondensedFont, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(start = 8.dp, bottom = 12.dp))
            }
            MetaRow(h.meta.substringBefore(" · ").takeIf { h.meta.contains(" · ") } ?: "", h.meta.substringAfterLast(" · "), onPc.copy(alpha = .85f))
        }
    }
}

@Composable
private fun HeroOrigin(h: HeroInfo?, n: Now, st: AppState, lang: String) {
    val shape = RoundedCornerShape(30.dp)
    if (h == null) { IdleHero(n, st, lang, shape, null); return }
    val v = Sched.subj(st, h.slot!!.lesson.subj, lang)
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).vivoGlass(shape).press(shape) {}.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Tile(v.emoji, v.color, 52.dp)
        Column(Modifier.weight(1f)) {
            Text(h.label, color = Color(0xFFFF9A3C), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(v.name, color = T.tx, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
            Text(h.meta, color = T.tx2, fontSize = 14.sp)
            ProgressLine(h.progress, Color(0xFFFF8A3A), Color.White.copy(alpha = .14f), 6.dp, Modifier.padding(top = 10.dp), glow = true)
        }
    }
}

@Composable
private fun HeroOneUi(h: HeroInfo?, n: Now, st: AppState, lang: String) {
    val shape = RoundedCornerShape(28.dp)
    if (h == null) { IdleHero(n, st, lang, shape, null); return }
    val v = Sched.subj(st, h.slot!!.lesson.subj, lang)
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).frost(shape, Color(0x30A0AAFF)).press(shape) {}.padding(18.dp)) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = .2f)), contentAlignment = Alignment.Center) { BigEmoji(v.emoji, 30.dp) }
        Text(v.name + if (h.kind == Phase.LESSON) " · " + Tr.t(lang, "left", h.big.toIntOrNull() ?: 0) else "", color = T.tx, fontSize = 22.sp, fontWeight = FontWeight.Bold,
            maxLines = 2, modifier = Modifier.padding(top = 12.dp))
        Text(h.meta, color = T.tx2, fontSize = 14.5.sp, modifier = Modifier.padding(top = 4.dp))
        // толстый ползунок, как яркость в шторке
        val p by animateFloatAsState(h.progress, tween(600), label = "ouprog")
        Box(Modifier.padding(top = 16.dp).fillMaxWidth().height(44.dp).clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = .14f))) {
            Box(Modifier.fillMaxWidth(p.coerceIn(.18f, 1f)).height(44.dp).clip(RoundedCornerShape(22.dp)).background(Color.White), contentAlignment = Alignment.CenterEnd) {
                Text("${(p * 100).toInt()}%", color = Color(0xFF2A2356), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 14.dp))
            }
            if (h.big.toIntOrNull() != null) Text(Tr.t(lang, "to_bell", h.big.toInt()), color = T.tx2, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp))
        }
    }
}

/** «Свободно»: уроки закончились, выходной, каникулы — крупная 3D-иконка и первый урок следующего дня. */
@Composable
private fun IdleHero(n: Now, st: AppState, lang: String, shape: RoundedCornerShape, bg: Brush?) {
    val I = Sched.idle(n, lang)
    val t = T
    val m = when {
        bg != null -> Modifier.clip(shape).background(bg)
        t.id.startsWith("ios") -> Modifier.liquid(shape, Color(0x3330D158))
        t.id == "origin" -> Modifier.vivoGlass(shape)
        t.id == "oneui" -> Modifier.frost(shape)
        else -> Modifier.clip(shape).background(t.s1)
    }
    val fg = if (t.pixel) Color(0xFFEADDFF) else Color.White
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).then(m).press(shape) {}.padding(22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                NowLabel(listOf(Tr.hm(n.minute), I.sub.ifEmpty { Tr.t(lang, "all_today") }).joinToString(" · "), fg.copy(alpha = .8f))
                Text(I.title, color = fg, fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
            }
            BigEmoji(I.em, 110.dp)
        }
        val nd = n.nextDay
        val first = n.nextDayLessons.firstOrNull()
        if (nd != null && first != null) {
            val label = Tr.t(lang, "first_on", Sched.dayLabel(lang, n.date, nd))
            NextPill(st, first, label, lang, fg.copy(alpha = .1f))
        }
    }
}

/* ===================== Детали карточек ===================== */

@Composable
private fun NowLabel(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // мягко пульсирующая точка «сейчас»
        // пульс — два вдоха раз в 20 секунд, а не вечный: вечная анимация перерисовывает весь экран 120 раз в секунду и сажает батарею
        val pulse = remember { androidx.compose.animation.core.Animatable(1f) }
        LaunchedEffect(Unit) {
            while (true) {
                repeat(2) { pulse.animateTo(.55f, tween(550)); pulse.animateTo(1f, tween(550)) }
                kotlinx.coroutines.delay(20_000)
            }
        }
        Box(Modifier.size(8.dp).graphicsLayer { alpha = pulse.value }.clip(CircleShape).background(Color(0xFF4ADE80)))
        Text(text, color = color, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp), maxLines = 1)
    }
}

@Composable
private fun BigNumber(big: String, unit: String, size: androidx.compose.ui.unit.TextUnit, w: FontWeight) {
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 8.dp)) {
        AnimatedContent(big, transitionSpec = { (fadeIn(tween(250)) + scaleIn(initialScale = .9f)) togetherWith fadeOut(tween(150)) }, label = "bn") { b ->
            Text(b, color = Color.White, fontSize = size, lineHeight = size, fontWeight = w, letterSpacing = (-4).sp)
        }
        if (unit.isNotEmpty()) Text(unit, color = Color.White.copy(alpha = .8f), fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 10.dp, bottom = 12.dp))
    }
}

@Composable
private fun MetaRow(a: String, b: String, c: Color) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Text(a, color = c, fontSize = 15.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(b, color = c, fontSize = 15.sp)
    }
}

@Composable
private fun NextPill(st: AppState, s: Slot, label: String, lang: String, bg: Color) {
    val v = Sched.subj(st, s.lesson.subj, lang)
    val shape = RoundedCornerShape(22.dp)
    Row(Modifier.padding(top = 18.dp).fillMaxWidth().clip(shape).background(bg).press(shape) {}.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Tile(v.emoji, v.color, 44.dp)
        Column(Modifier.weight(1f)) {
            Text(label, color = Color.White.copy(alpha = .62f), fontSize = 13.sp, maxLines = 1)
            Text(v.name, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(Tr.hm(s.start), color = Color.White.copy(alpha = .85f), fontSize = 15.sp)
            if (s.lesson.room.isNotBlank()) Text(Tr.t(lang, "room", s.lesson.room), color = Color.White.copy(alpha = .7f), fontSize = 14.sp)
        }
    }
}

/** Крупная 3D-иконка (в наборе Fluent) или эмодзи. */
@Composable
private fun BigEmoji(emoji: String, size: androidx.compose.ui.unit.Dp) {
    val key = IconRes.key(emoji)
    val res = key?.let { IconRes.fluent[it] }
    if (res != null) androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(res), null, Modifier.size(size))
    else Text(emoji, fontSize = (size.value * .72f).sp,
        style = androidx.compose.ui.text.TextStyle(
            platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center, androidx.compose.ui.text.style.LineHeightStyle.Trim.Both)))
}

/** Кольцо прогресса (iOS). */
@Composable
private fun Ring(p: Float, fg: Color, track: Color, w: androidx.compose.ui.unit.Dp, modifier: Modifier) {
    val a by animateFloatAsState(p.coerceIn(0f, 1f), tween(700), label = "ring")
    Canvas(modifier) {
        val sw = w.toPx(); val inset = sw / 2
        val sz = Size(size.width - sw, size.height - sw)
        drawArc(track, 0f, 360f, false, Offset(inset, inset), sz, style = Stroke(sw))
        drawArc(fg.copy(alpha = .35f), -90f, 360f * a, false, Offset(inset, inset), sz, style = Stroke(sw * 1.8f, cap = StrokeCap.Round))
        drawArc(fg, -90f, 360f * a, false, Offset(inset, inset), sz, style = Stroke(sw, cap = StrokeCap.Round))
    }
}


/** Фаза волны: бежит несколько секунд после появления/смены прогресса и замирает — без вечной анимации. */
@Composable
private fun restingPhase(key: Any?, periodMs: Int = 2400, activeMs: Int = 6000): androidx.compose.runtime.State<Float> {
    val t = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(key) {
        t.snapTo(0f)
        t.animateTo(activeMs / periodMs.toFloat(), tween(activeMs, easing = androidx.compose.animation.core.LinearEasing))
    }
    return remember { androidx.compose.runtime.derivedStateOf { t.value % 1f } }
}

/** Волнистое кольцо прогресса (Material 3 Expressive), волна бежит по кругу. */
@Composable
private fun WavyRing(p: Float, fg: Color, track: Color, modifier: Modifier) {
    val a by animateFloatAsState(p.coerceIn(0f, 1f), tween(700), label = "wr")
    val ph by restingPhase((p * 100).toInt())
    Canvas(modifier) {
        val c = Offset(size.width / 2, size.height / 2)
        val R = size.minDimension / 2 - 8.dp.toPx()
        drawCircle(track, R, c, style = Stroke(6.dp.toPx()))
        val path = Path()
        val end = (360f * a).toInt()
        for (i in 0..end) {
            val th = (i - 90f) * PI.toFloat() / 180f
            val r = R + 3.dp.toPx() * sin(12 * th + ph * 2 * PI.toFloat())
            val x = c.x + r * cos(th); val y = c.y + r * sin(th)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        if (end > 0) drawPath(path, fg, style = Stroke(7.dp.toPx(), cap = StrokeCap.Round))
    }
}

/* ===================== Плитки и кольца ===================== */

@Composable
private fun Tiles(h: HeroInfo?, n: Now, st: AppState, todo: List<Hw>, lang: String, onTasks: () -> Unit) {
    val t = T
    val nextSlot = if (h?.kind == Phase.LESSON) h.next else h?.slot ?: n.nextDayLessons.firstOrNull()
    val dueToday = todo.filter { it.due == n.date.toString() }
    val dueTomorrow = todo.filter { it.due == n.date.plusDays(1).toString() }
    val dueList = if (h == null) dueTomorrow else dueToday
    val shape = RoundedCornerShape(if (t.id == "oneui") 36.dp else if (t.pixel) 32.dp else 30.dp)
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).then(if (t.id == "oneui") Modifier else Modifier.height(IntrinsicSize.Max)),
        horizontalArrangement = Arrangement.spacedBy(if (t.pixel) 8.dp else 12.dp)) {
        // «Далее»
        val bgA = when (t.id) {
            "pixel" -> Modifier.clip(shape).background(Color(0xFFFF8A80))
            "oneui" -> Modifier.frost(shape)
            "origin" -> Modifier.vivoGlass(shape)
            else -> Modifier.liquid(shape)
        }
        val fgA = if (t.pixel) Color(0xFF4A0E0B) else t.tx
        val tileH = if (t.id == "oneui") 72.dp else if (t.pixel) 168.dp else 150.dp
        if (t.id == "oneui") {
            Row(Modifier.weight(1f).height(tileH).then(bgA).press(shape) {}.padding(start = 16.dp, end = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(Tr.t(lang, "next") + (nextSlot?.let { " · " + Tr.hm(it.start) } ?: ""), color = t.tx2, fontSize = 12.sp, maxLines = 1)
                    Text(nextSlot?.let { Sched.subj(st, it.lesson.subj, lang).name } ?: "—", color = t.tx, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                nextSlot?.let { val v = Sched.subj(st, it.lesson.subj, lang); Tile(v.emoji, v.color, 52.dp) }
            }
            Row(Modifier.weight(1f).height(tileH).frost(shape, Color(0x3D3DDC84)).press(shape, onClick = onTasks).padding(start = 16.dp, end = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(Tr.t(lang, if (h == null) "due_tomorrow" else "due_today"), color = t.tx2, fontSize = 12.sp, maxLines = 1)
                    Text("${dueList.size}", color = t.tx, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                BigEmoji("📝", 40.dp)
            }
            return@Row
        }
        Column(Modifier.weight(1f).fillMaxHeight().heightIn(min = tileH).then(bgA).press(shape) {}.padding(16.dp)) {
            Text(Tr.t(lang, "next") + (nextSlot?.let { " · " + Tr.hm(it.start) } ?: ""), color = fgA.copy(alpha = .8f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            nextSlot?.let { val v = Sched.subj(st, it.lesson.subj, lang); Box(Modifier.padding(top = 10.dp)) { Tile(v.emoji, v.color, 46.dp) } }
            Spacer(Modifier.weight(1f))
            Text(nextSlot?.let { Sched.subj(st, it.lesson.subj, lang).name } ?: "—", color = fgA, fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
            nextSlot?.lesson?.room?.takeIf { it.isNotBlank() }?.let { Text(Tr.t(lang, "room", it), color = fgA.copy(alpha = .8f), fontSize = 14.sp) }
        }
        // «Сдать сегодня»
        val bgB = when (t.id) {
            "pixel" -> Modifier.clip(shape).background(Color(0xFF4A4458))
            "origin" -> Modifier.vivoGlass(shape)
            else -> Modifier.liquid(shape, Color(0x47FF9F0A))
        }
        val fgB = if (t.pixel) Color(0xFFE8DEF8) else t.tx
        Column(Modifier.weight(1f).fillMaxHeight().heightIn(min = tileH).then(bgB).press(shape, onClick = onTasks).padding(16.dp)) {
            Text(Tr.t(lang, if (h == null) "due_tomorrow" else "due_today"), color = fgB.copy(alpha = .8f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.Top) {
                Text("${dueList.size}", color = fgB, fontSize = if (t.pixel) 64.sp else 46.sp, lineHeight = if (t.pixel) 60.sp else 48.sp,
                    fontFamily = if (t.pixel) CondensedFont else null, fontWeight = if (t.pixel) FontWeight.ExtraBold else FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (!t.pixel) BigEmoji("📝", 40.dp)
            }
            Spacer(Modifier.weight(1f))
            Text(dueList.mapNotNull { it.subj }.distinct().joinToString(", ") { Sched.subj(st, it, lang).name }.ifEmpty { Tr.t(lang, "no_hw") },
                color = fgB, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
            val more = todo.size - dueList.size
            if (more > 0) Text(Tr.t(lang, "more_week", more), color = fgB.copy(alpha = .8f), fontSize = 13.sp)
        }
    }
}

/** One UI: кольца как в виджете батарей. */
@Composable
private fun Gauges(h: HeroInfo?, n: Now, todo: List<Hw>, lang: String) {
    val total = n.today.size
    val done = n.today.count { n.minute >= it.end }
    val end = n.today.lastOrNull()?.end
    val due = todo.count { it.due == n.date.toString() }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).frost(RoundedCornerShape(40.dp)).padding(vertical = 12.dp, horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround) {
        Gauge(if (h?.kind == Phase.LESSON) h.progress else 0f, Color.White, if (h?.kind == Phase.LESSON) "${(h.progress * 100).toInt()}%" else "—", Tr.t(lang, "g_lesson"), Ic.clock)
        Gauge(if (total > 0) done.toFloat() / total else 0f, Color(0xFF3DDC84), "$done/$total", Tr.t(lang, "g_day"), Ic.cal)
        Gauge((due / 5f).coerceAtMost(1f), Color(0xFFFFB86B), "$due", Tr.t(lang, "g_hand"), Ic.hw)
        Gauge(if (end != null) (n.minute.toFloat() / end).coerceIn(0f, 1f) else 0f, Color(0xFFA8B4FF), end?.let { Tr.hm(it) } ?: "—", Tr.t(lang, "g_end"), Ic.clock)
    }
}

@Composable
private fun Gauge(p: Float, c: Color, value: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val a by animateFloatAsState(p, tween(800), label = "g")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(58.dp)) {
                val sw = 6.dp.toPx(); val inset = sw / 2 + 2.dp.toPx()
                val sz = Size(size.width - inset * 2, size.height - inset * 2)
                drawArc(Color.White.copy(alpha = .18f), 135f, 270f, false, Offset(inset, inset), sz, style = Stroke(sw, cap = StrokeCap.Round))
                if (a > 0f) drawArc(c, 135f, 270f * a, false, Offset(inset, inset), sz, style = Stroke(sw, cap = StrokeCap.Round))
            }
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(label, color = T.tx2, fontSize = 11.sp)
    }
}

/** OriginOS: полоса дня из отрезков — сколько уроков прошло и сколько идёт текущий. */
@Composable
private fun DaySegments(items: List<LItem>) {
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp).height(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { x ->
            val p = when (x.state) { RowState.Past -> 1f; RowState.Cur -> x.progress; else -> 0f }
            val a by animateFloatAsState(p, tween(700), label = "dseg")
            Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = .14f))) {
                Box(Modifier.fillMaxWidth(a).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Brush.horizontalGradient(listOf(Color(0xFFFFB067), Color(0xFFFF7A2F)))))
            }
        }
    }
}

/* ===================== OriginOS: верх с «жидкими» цифрами ===================== */

@Composable
private fun OriginTop(lang: String, today: LocalDate, n: Now, actions: List<Pair<androidx.compose.ui.graphics.vector.ImageVector, () -> Unit>>) {
    val h = heroOf(Store.state.value, n, lang)
    Column {
        Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 16.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(Tr.dateTitle(lang, today), color = Color.White.copy(alpha = .88f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                val sub = when {
                    h != null -> Tr.t(lang, "lesson_of", h.num, n.today.size)
                    else -> Sched.idle(n, lang).title
                }
                Text(sub, color = T.tx2, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
            }
            actions.lastOrNull()?.let { (ic, f) -> RoundBtn(ic, 46.dp, f) }
        }
        if (h != null) {
            GlassDigits(h.big, Modifier.fillMaxWidth().height(170.dp).padding(top = 6.dp))
            Text(
                if (h.big.length <= 2) Tr.plural(lang, "min", h.big.toInt()).substringAfter(" ") + " " + Tr.t(lang, if (h.kind == Phase.LESSON) "to_end_of" else "to_start_of") else Tr.t(lang, "to_start_of"),
                color = Color.White.copy(alpha = .85f), fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

/**
 * Стеклянные цифры как часы на экране блокировки OriginOS 7: сквозь них видно обои — ярче и чуть увеличенные,
 * будто через линзу; по внешнему краю — светлая кромка, блики сверху-слева и снизу-справа.
 */
@Composable
private fun GlassDigits(text: String, modifier: Modifier) {
    val ctx = LocalContext.current
    val d = LocalDensity.current
    val cfg = LocalConfiguration.current
    var pos by remember { mutableStateOf(Offset.Zero) }
    val screenW = with(d) { cfg.screenWidthDp.dp.toPx() }
    val screenH = with(d) { cfg.screenHeightDp.dp.toPx() }
    // Настоящие контуры букв (getPathForRange у Compose даёт только прямоугольник выделения). Шрифт — узкий жирный Roboto.
    val tf = remember {
        runCatching {
            android.graphics.Typeface.Builder(ctx.assets, "f_flex.ttf").setFontVariationSettings("'wght' 800, 'wdth' 75").build()
        }.getOrNull() ?: android.graphics.Typeface.DEFAULT_BOLD
    }
    val glyphs = remember(text, tf, d.density) {
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            typeface = tf; textSize = 168f * d.density; letterSpacing = -.025f
        }
        val raw = android.graphics.Path()
        paint.getTextPath(text, 0, text.length, 0f, 0f, raw)
        // убираем наложения контуров шрифта, чтобы кромка шла только по внешнему краю
        val merged = android.graphics.Path()
        val ok = runCatching { merged.op(raw, android.graphics.Path(), android.graphics.Path.Op.UNION) }.getOrDefault(false)
        val p = if (ok && !merged.isEmpty) merged else raw
        val rb = android.graphics.RectF(); p.computeBounds(rb, true)
        p.offset(-rb.left, -rb.top)
        Triple(p.asComposePath(), rb.width(), rb.height())
    }
    val (path, gw, gh) = glyphs
    Canvas(modifier.onGloballyPositioned { pos = it.positionInRoot() }) {
        val dx = (size.width - gw) / 2f
        val dy = (size.height - gh) / 2f
        translate(dx, dy) {
            // тень
            translate(0f, 8.dp.toPx()) { drawPath(path, Color.Black.copy(alpha = .28f)) }
            clipPath(path) {
                // обои сквозь стекло: увеличены относительно центра цифр и ярче
                scale(1.08f, pivot = Offset(gw / 2f, gh / 2f)) {
                    translate(-pos.x - dx, -pos.y - dy) { drawStyleWall("origin", screenW, screenH, boost = 1.6f, sharp = false) }
                }
                drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = .16f), Color.White.copy(alpha = .04f))), size = Size(gw, gh))
            }
            // кромка: блики на 45°
            drawPath(path, Brush.linearGradient(
                0f to Color.White.copy(alpha = .9f), .3f to Color.White.copy(alpha = .15f), .7f to Color.White.copy(alpha = .1f), 1f to Color.White.copy(alpha = .7f),
                start = Offset.Zero, end = Offset(gw, gh),
            ), style = Stroke(1.6.dp.toPx()))
        }
    }
}

/* ===================== Задания на главной ===================== */

@Composable
private fun HomeTasks(st: AppState, list: List<Hw>, today: LocalDate, lang: String) {
    val t = T
    val ui = LocalUi.current
    // Только что отмеченное задание не пропадает мгновенно: остаётся на своём месте ~1 с (видно, как рисуется галочка), потом плавно уезжает.
    val linger = remember { androidx.compose.runtime.mutableStateMapOf<Long, Linger>() }
    val jobs = remember { mutableMapOf<Long, kotlinx.coroutines.Job>() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val onToggle: (Hw) -> Unit = { h ->
        val ex = linger[h.id]
        if (ex != null) {
            jobs.remove(h.id)?.cancel(); linger.remove(h.id); toggleDone(h)
        } else {
            val idx = list.indexOfFirst { it.id == h.id }.coerceAtLeast(0)
            linger[h.id] = Linger(idx, h.copy(done = true), false)
            toggleDone(h)
            jobs[h.id] = scope.launch {
                kotlinx.coroutines.delay(1000)
                linger[h.id]?.let { linger[h.id] = it.copy(leaving = true) }
                kotlinx.coroutines.delay(500)
                linger.remove(h.id); jobs.remove(h.id)
            }
        }
    }
    val shown: List<Pair<Hw, Boolean>> = run {
        val out = list.filter { l -> linger[l.id] == null }.map { it to false }.toMutableList()
        linger.values.sortedBy { it.idx }.forEach { l -> out.add(l.idx.coerceIn(0, out.size), l.hw to l.leaving) }
        out
    }
    if (shown.isEmpty()) { Text(Tr.t(lang, "no_hw"), color = t.tx2, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp)); return }
    if (t.id == "color") {
        // ColorOS: цветные карточки с прокруткой вбок, как «Другие альбомы»
        LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(shown.filter { !it.second }, key = { it.first.id }) { (h, _) ->
                val v = h.subj?.let { Sched.subj(st, it, lang) }
                val c = Color(v?.color ?: 0xFF8E8E93)
                val shape = RoundedCornerShape(28.dp)
                val dim by animateFloatAsState(if (h.done) .55f else 1f, tween(300), label = "dim")
                Column(Modifier.animateItem(fadeInSpec = null, placementSpec = spring(.8f, 380f), fadeOutSpec = tween(260)).width(168.dp).height(184.dp).clip(shape)
                    .background(Brush.linearGradient(listOf(lerp(Color(0xFF101010), c, .45f), lerp(Color(0xFF101010), c, .22f))))
                    .press(shape) { ui.open { HwSheet(h.id, h.kind) } }.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        if (v != null) Box(Modifier.graphicsLayer { alpha = dim }) { BigEmoji(v.emoji, 40.dp) }
                        Spacer(Modifier.weight(1f))
                        CheckCircle(h.done, Color.White) { onToggle(h) }
                    }
                    Column(Modifier.graphicsLayer { alpha = dim }) {
                        Text(v?.name ?: Tr.t(lang, "type_note"), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.padding(top = 12.dp),
                            textDecoration = if (h.done) androidx.compose.ui.text.style.TextDecoration.LineThrough else null)
                        Text(h.text, color = Color.White.copy(alpha = .75f), fontSize = 15.sp, lineHeight = 19.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    h.due?.let { Text(Sched.dueLabel(lang, today, LocalDate.parse(it)), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.graphicsLayer { alpha = dim }.clip(RoundedCornerShape(99.dp)).background(Color(0x47000000)).padding(horizontal = 10.dp, vertical = 5.dp)) }
                }
            }
        }
        return
    }
    val sep = t.pixel || t.id == "origin"
    val gap = if (t.pixel) 3.dp else 8.dp
    val rows: @Composable () -> Unit = {
        shown.forEachIndexed { i, (h, leaving) ->
            androidx.compose.runtime.key(h.id) {
            androidx.compose.animation.AnimatedVisibility(
                visible = !leaving,
                exit = androidx.compose.animation.shrinkVertically(spring(.85f, 420f)) + androidx.compose.animation.fadeOut(tween(220)) +
                    androidx.compose.animation.slideOutHorizontally(tween(320)) { it / 3 },
            ) {
            Column {
            val v = h.subj?.let { Sched.subj(st, it, lang) }
            val first = i == 0; val last = i == shown.lastIndex
            val shape = when {
                t.pixel -> RoundedCornerShape(topStart = if (first) 28.dp else 8.dp, topEnd = if (first) 28.dp else 8.dp, bottomStart = if (last) 28.dp else 8.dp, bottomEnd = if (last) 28.dp else 8.dp)
                else -> RoundedCornerShape(24.dp)
            }
            val bg = when { t.pixel -> Modifier.clip(shape).background(t.s1); t.id == "origin" -> Modifier.vivoGlass(shape); else -> Modifier }
            val dueC = if (h.due == today.toString() || (h.due != null && h.due < today.toString())) (if (t.pixel) Color(0xFFFFB1A8) else Color(0xFFFF9F0A)) else t.tx2
            val dim by animateFloatAsState(if (h.done) .5f else 1f, tween(300), label = "dim")
            Row(Modifier.fillMaxWidth().appear(i).then(bg).press(shape) { ui.open { HwSheet(h.id, h.kind) } }.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (t.pixel) CheckSquare(h.done) { onToggle(h) } else CheckCircle(h.done, t.tx2) { onToggle(h) }
                if (v != null && !t.id.startsWith("ios")) Box(Modifier.graphicsLayer { alpha = dim }) { Tile(v.emoji, v.color, 38.dp) }
                Column(Modifier.weight(1f).graphicsLayer { alpha = dim }) {
                    Text(v?.name ?: Tr.t(lang, "type_note"), color = t.tx, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                        textDecoration = if (h.done) androidx.compose.ui.text.style.TextDecoration.LineThrough else null)
                    if (h.text.isNotBlank()) Text(h.text, color = t.tx2, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                h.due?.let { Text(Sched.dueLabel(lang, today, LocalDate.parse(it)), color = dueC, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.graphicsLayer { alpha = dim }) }
            }
            if (!sep && !last) Box(Modifier.padding(start = 56.dp).fillMaxWidth().height(1.dp).background(t.line))
            if (sep && !last) Spacer(Modifier.height(gap))
            }
            }
            }
        }
    }
    if (sep) Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) { rows() }
    else Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).styleCard(RoundedCornerShape(28.dp)).padding(vertical = 4.dp)) { rows() }
}

private data class Linger(val idx: Int, val hw: Hw, val leaving: Boolean)

private fun toggleDone(h: Hw) {
    val ctx = kz.alnazar.schedule.App.ctx
    Store.update(ctx) { s -> s.copy(hw = s.hw.map { if (it.id == h.id) it.copy(done = !it.done) else it }) }
}

/** Кружок-галочка с пружинкой. */
@Composable
fun CheckCircle(on: Boolean, ring: Color, onClick: () -> Unit) = AnimCheck(on, 26.dp, false, ring, onClick)

/** Квадратная галочка Material. */
@Composable
fun CheckSquare(on: Boolean, onClick: () -> Unit) = AnimCheck(on, 22.dp, true, T.tx2, onClick)

/** Волнистая полоса прогресса Material 3 Expressive (используется в Pixel). */
@Composable
fun WavyProgress(progress: Float, active: Color, track: Color) {
    val phase = restingPhase((progress * 100).toInt(), 1600, 6000)
    Canvas(Modifier.fillMaxWidth().height(12.dp)) {
        val w = size.width; val cy = size.height / 2; val sw = 4.dp.toPx(); val amp = 2.6.dp.toPx(); val wl = 22.dp.toPx()
        val px = (w * progress).coerceIn(0f, w)
        val gap = 6.dp.toPx()
        if (px + gap < w) drawLine(track, Offset(px + gap, cy), Offset(w, cy), sw, StrokeCap.Round)
        if (px > 1f) {
            val path = Path()
            var x = 0f
            while (x <= px) {
                val y = cy + amp * sin(((x / wl) - phase.value) * 2f * PI.toFloat())
                if (x == 0f) path.moveTo(x, y) else path.lineTo(x, y)
                x += 2f
            }
            drawPath(path, active, style = Stroke(sw, cap = StrokeCap.Round))
        }
        drawCircle(active, sw / 2, Offset(w - sw / 2, cy))
    }
}

@Suppress("unused") private val keepOffset = Modifier.offset(0.dp)

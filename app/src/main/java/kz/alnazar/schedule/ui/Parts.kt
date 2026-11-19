package kz.alnazar.schedule.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.alnazar.schedule.logic.DayInfo
import kz.alnazar.schedule.logic.Tr
import java.time.LocalDate

/* ===================== Поверхности по стилям ===================== */

/** ColorOS 17: объёмная кнопка — светлее сверху, тёмная тень, блик по верхней кромке. */
fun Modifier.vol3d(shape: Shape): Modifier = this
    .shadow(10.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
    .clip(shape)
    .background(Brush.verticalGradient(listOf(Color(0xFF3A3A3D), Color(0xFF262628))))
    .drawBehind {
        val o = shape.createOutline(size, layoutDirection, this)
        drawOutline(o, Brush.verticalGradient(0f to Color(0x29FFFFFF), .18f to Color.Transparent), style = Stroke(1.dp.toPx()))
    }

/** iOS 26: жидкое стекло — полупрозрачно, блики на 45° (сверху-слева и снизу-справа). */
fun Modifier.liquid(shape: Shape, tint: Color = Color.White.copy(alpha = .10f)): Modifier = this
    .shadow(14.dp, shape, ambientColor = Color(0x47000000), spotColor = Color(0x47000000))
    .clip(shape)
    .background(Brush.linearGradient(listOf(tint.copy(alpha = (tint.alpha + .05f).coerceAtMost(1f)), tint.copy(alpha = tint.alpha * .5f), tint.copy(alpha = tint.alpha * .8f))))
    .drawBehind {
        val o = shape.createOutline(size, layoutDirection, this)
        drawOutline(o, Brush.linearGradient(
            0f to Color(0xD9FFFFFF), .22f to Color(0x1FFFFFFF), .5f to Color.Transparent, .78f to Color(0x1FFFFFFF), 1f to Color(0x99FFFFFF),
            start = Offset.Zero, end = Offset(size.width, size.height),
        ), style = Stroke(1.3.dp.toPx()))
    }

/** OriginOS 7: толстое стекло vivo — фаска сверху, затемнение снизу. */
fun Modifier.vivoGlass(shape: Shape, tint: Brush? = null): Modifier = this
    .shadow(16.dp, shape, ambientColor = Color(0x59000000), spotColor = Color(0x59000000))
    .clip(shape)
    .background(tint ?: Brush.verticalGradient(listOf(Color(0x29FFFFFF), Color(0x0FFFFFFF))))
    .drawBehind {
        drawRect(Brush.verticalGradient(.55f to Color.Transparent, 1f to Color(0x2E000000)))
        val o = shape.createOutline(size, layoutDirection, this)
        drawOutline(o, Brush.verticalGradient(0f to Color(0x80FFFFFF), .15f to Color(0x14FFFFFF), 1f to Color(0x1FFFFFFF)), style = Stroke(1.4.dp.toPx()))
    }

/** One UI 9.5: матовая заливка без бликов. */
fun Modifier.frost(shape: Shape, tint: Color = Color(0x1FFFFFFF)): Modifier = this
    .shadow(12.dp, shape, ambientColor = Color(0x40080428), spotColor = Color(0x40080428))
    .clip(shape).background(tint)

/** Карточка в стиле текущей темы. */
@Composable
fun Modifier.styleCard(shape: Shape = RoundedCornerShape(T.rc)): Modifier = when (T.id) {
    "color" -> this.clip(shape).background(T.s1)
    "ios26" -> this.liquid(shape)
    "origin" -> this.vivoGlass(shape)
    "oneui" -> this.frost(shape)
    "pixel" -> this.clip(shape).background(T.s1)
    else -> this.glass(shape)
}

/** Круглая кнопка в шапке (колокольчик, «⋯», стрелки). */
@Composable
fun RoundBtn(icon: ImageVector, size: Dp = 48.dp, onClick: () -> Unit) {
    val t = T
    val shape = CircleShape
    val m = when (t.id) {
        "color" -> Modifier.vol3d(shape)
        "ios26" -> Modifier.liquid(shape)
        "origin" -> Modifier.vivoGlass(shape)
        "oneui" -> Modifier.frost(shape)
        else -> Modifier.clip(shape)
    }
    Box(Modifier.size(size).then(m).press(shape, scaleTo = .9f, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = if (t.pixel) t.tx2 else t.tx, modifier = Modifier.size(size * .46f))
    }
}

/* ===================== Шапка экрана ===================== */

/** Свёрнутая шапка ColorOS: экран сообщает заголовок и кнопки, корень рисует её поверх. */
object HeaderBus {
    var title by mutableStateOf("")
    var frac by mutableFloatStateOf(0f)
    var actions by mutableStateOf<List<Pair<ImageVector, () -> Unit>>>(emptyList())
    var owner by mutableIntStateOf(-1)
}

/** Номер страницы в пейджере и признак «сейчас на экране» — для шапки ColorOS. */
val LocalPageIdx = staticCompositionLocalOf { -1 }
val LocalPageOn = staticCompositionLocalOf { true }
val LocalListState = staticCompositionLocalOf<LazyListState?> { null }

/**
 * Большой заголовок экрана в стиле темы.
 * ColorOS: при прокрутке заголовок с кнопками сжимается в одну строку под матовой плашкой.
 */
@Composable
fun ScreenHead(title: String, sub: String?, actions: List<Pair<ImageVector, () -> Unit>> = emptyList(), kicker: String? = null) {
    val t = T
    val d = LocalDensity.current
    if (t.id == "color") {
        val ls = LocalListState.current
        val idx = LocalPageIdx.current
        val on = LocalPageOn.current
        if (ls != null && on) {
            val fr = if (ls.firstVisibleItemIndex > 0) 1f else with(d) { (ls.firstVisibleItemScrollOffset / 70.dp.toPx()).coerceIn(0f, 1f) }
            SideEffect { HeaderBus.title = title; HeaderBus.actions = actions; HeaderBus.frac = fr; HeaderBus.owner = idx }
        }
    }
    when (t.id) {
        "pixel" -> Column {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(kicker ?: "", color = t.tx, fontSize = 22.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                actions.forEach { (ic, f) -> RoundBtn(ic, 48.dp, f) }
            }
            Text(title.uppercase(), color = t.tx, fontSize = 52.sp, lineHeight = 52.sp, fontFamily = CondensedFont, fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp))
            if (sub != null) Text(sub, color = t.tx2, fontSize = 16.sp, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp))
        }
        "ios26", "ios18" -> Column {
            if (actions.isNotEmpty()) Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RoundBtn(actions.first().first, 48.dp, actions.first().second)
                Spacer(Modifier.weight(1f))
                actions.drop(1).forEach { (ic, f) -> RoundBtn(ic, 48.dp, f) }
            }
            Text(title, color = t.tx, fontSize = 36.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.6).sp,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp))
            if (sub != null) Text(sub, color = t.tx2, fontSize = 17.sp, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 2.dp))
        }
        "oneui" -> Box(Modifier.fillMaxWidth().height(200.dp)) {
            Row(Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                actions.forEach { (ic, f) -> RoundBtn(ic, 44.dp, f) }
            }
            Column(Modifier.align(Alignment.BottomStart).padding(start = 24.dp, end = 24.dp, bottom = 20.dp)) {
                Text(title, color = t.tx, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                if (sub != null) Text(sub, color = t.tx2, fontSize = 16.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
        else -> Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = if (t.id == "color") 22.dp else 14.dp),
            verticalAlignment = if (t.id == "color") Alignment.Top else Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = t.tx, fontSize = if (t.id == "color") 44.sp else 32.sp, lineHeight = if (t.id == "color") 46.sp else 36.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = if (t.id == "color") (-.8).sp else (-.3).sp)
                if (sub != null) Text(sub, color = t.tx2, fontSize = if (t.id == "color") 17.sp else 15.sp, modifier = Modifier.padding(top = 8.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { actions.forEach { (ic, f) -> RoundBtn(ic, if (t.id == "color") 52.dp else 46.dp, f) } }
        }
    }
}

/** Заголовок раздела («Уроки», «Задания») с правой частью. */
@Composable
fun SecHead(title: String, right: (@Composable RowScope.() -> Unit)? = null) {
    val t = T
    val (size, color, top) = when (t.id) {
        "pixel" -> Triple(16.sp, t.acc, 28.dp)
        "color" -> Triple(26.sp, t.tx, 30.dp)
        "ios26", "ios18" -> Triple(24.sp, t.tx, 28.dp)
        "origin" -> Triple(21.sp, t.tx, 26.dp)
        else -> Triple(18.sp, t.tx, 26.dp)
    }
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = top, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = color, fontSize = size, fontWeight = if (t.pixel) FontWeight.SemiBold else FontWeight.Bold, modifier = Modifier.weight(1f))
        if (right != null) Row(verticalAlignment = Alignment.CenterVertically, content = right)
    }
}

/** Ссылка в заголовке раздела («Все 5»). */
@Composable
fun SecLink(text: String, onClick: () -> Unit) {
    Text(text, color = if (T.id == "origin") Color(0xFF4C8DFF) else T.acc, fontSize = if (T.pixel) 14.sp else 16.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(RoundedCornerShape(99.dp)).press(RoundedCornerShape(99.dp), onClick = onClick).padding(horizontal = 6.dp, vertical = 4.dp))
}

/** Пометка («замена», «отменён»). */
@Composable
fun Tag(text: String, color: Color) {
    Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(if (T.pixel) 8.dp else 99.dp)).background(color.copy(alpha = .2f)).padding(horizontal = 9.dp, vertical = 3.dp))
}

/* ===================== Переключатель в заголовке (Сегодня/Завтра, День/Неделя) ===================== */

@Composable
fun <K> HeadSeg(options: List<Pair<K, String>>, sel: K, modifier: Modifier = Modifier, wide: Boolean = false, onSel: (K) -> Unit) {
    val t = T
    if (t.pixel) {
        Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            options.forEachIndexed { i, (k, l) ->
                val on = k == sel
                val bg by animateColorAsState(if (on) t.secCont else t.s2, tween(220), label = "hs")
                val r by animateDpAsState(if (on) 99.dp else 6.dp, spring(.6f, 500f), label = "hsr")
                val shape = RoundedCornerShape(topStart = if (i == 0) 99.dp else r, bottomStart = if (i == 0) 99.dp else r,
                    topEnd = if (i == options.lastIndex) 99.dp else r, bottomEnd = if (i == options.lastIndex) 99.dp else r)
                Box(Modifier.then(if (wide) Modifier.weight(1f) else Modifier).clip(shape).background(bg).press(shape) { onSel(k) }
                    .padding(horizontal = 14.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text(l, color = if (on) t.onSecCont else t.tx2, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        return
    }
    val idx = options.indexOfFirst { it.first == sel }.coerceAtLeast(0)
    val outer = RoundedCornerShape(99.dp)
    val bgM = when (t.id) {
        "ios26" -> Modifier.liquid(outer)
        "origin" -> Modifier.vivoGlass(outer)
        "oneui" -> Modifier.frost(outer)
        else -> Modifier.clip(outer).background(Color(0xFF1C1C1E))
    }
    BoxWithConstraints(modifier.then(if (wide) Modifier.fillMaxWidth() else Modifier.width((options.size * 100).dp)).height(if (wide) 50.dp else 44.dp).then(bgM).padding(4.dp)) {
        val w = maxWidth / options.size
        val x by animateDpAsState(w * idx, spring(.78f, 420f), label = "hsx")
        val ind = RoundedCornerShape(99.dp)
        Box(Modifier.offset(x = x).width(w).fillMaxHeight().then(
            if (t.id == "color") Modifier.vol3d(ind) else Modifier.clip(ind).background(Color.White.copy(alpha = .18f)),
        ))
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { i, (k, l) ->
                val c by animateColorAsState(if (i == idx) t.tx else t.tx2, tween(200), label = "hsc")
                Box(Modifier.width(w).fillMaxHeight().clip(ind).press(ind, scaleTo = .94f) { onSel(k) }, contentAlignment = Alignment.Center) {
                    Text(l, color = c, fontSize = if (wide) 16.sp else 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
    }
}

/* ===================== Полоса дней недели ===================== */

@Composable
fun DayStrip(lang: String, days: List<DayInfo>, sel: LocalDate, today: LocalDate, onSel: (LocalDate) -> Unit) {
    val t = T
    val dot = if (t.id == "origin") Color(0xFFFFB067) else if (t.id == "oneui") Color(0xFFFFB86B) else t.acc
    when (t.id) {
        "ios26", "ios18" -> Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            days.forEach { d ->
                val on = d.date == sel
                val bg by animateColorAsState(if (on) t.acc else Color.Transparent, tween(220), label = "dsi")
                val sc by animateFloatAsState(if (on) 1f else .9f, spring(.5f, 400f), label = "dss")
                Column(Modifier.width(52.dp).press(CircleShape) { onSel(d.date) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(Tr.dayShort(lang, d.date.dayOfWeek.value - 1), color = if (on) t.tx else t.tx2, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Box(Modifier.padding(top = 6.dp).size(44.dp).graphicsScale(sc).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
                        Text("${d.date.dayOfMonth}", color = if (d.count == 0 && !on) t.tx2.copy(alpha = .5f) else t.tx, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Dots(d, if (d.changed) Color(0xFFFF9F0A) else Color.White.copy(alpha = .55f))
                }
            }
        }
        "pixel" -> Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            days.forEachIndexed { i, d ->
                val on = d.date == sel
                val bg by animateColorAsState(if (on) t.acc else t.s2, tween(220), label = "dsp")
                val r by animateDpAsState(if (on) 32.dp else 10.dp, spring(.6f, 450f), label = "dsr")
                val l = if (i == 0) 32.dp else r; val rr = if (i == days.lastIndex) 32.dp else r
                val shape = RoundedCornerShape(topStart = l, bottomStart = l, topEnd = rr, bottomEnd = rr)
                Column(Modifier.weight(1f).height(64.dp).clip(shape).background(bg).press(shape) { onSel(d.date) }.graphicsAlpha(if (d.count == 0 && !on) .45f else 1f),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    val fg = if (on) t.accTx else t.tx
                    Text(Tr.dayShort(lang, d.date.dayOfWeek.value - 1), color = if (on) fg else t.tx2, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("${d.date.dayOfMonth}", color = fg, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Dots(d, if (on) t.accTx else Color(0xFFFFB1A8), 4.dp)
                }
            }
        }
        "origin" -> Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            days.forEach { d ->
                val on = d.date == sel
                val shape = RoundedCornerShape(24.dp)
                Column(Modifier.weight(1f).height(76.dp).then(
                    if (on) Modifier.vivoGlass(shape, Brush.verticalGradient(listOf(Color(0x8CFFAA5A), Color(0x4DFF7828)))) else Modifier.vivoGlass(shape),
                ).press(shape) { onSel(d.date) }.graphicsAlpha(if (d.count == 0 && !on) .45f else 1f),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(Tr.dayShort(lang, d.date.dayOfWeek.value - 1), color = t.tx2, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("${d.date.dayOfMonth}", color = t.tx, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Dots(d, dot, 5.dp)
                }
            }
        }
        "oneui" -> Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).frost(RoundedCornerShape(40.dp)).padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            days.forEach { d ->
                val on = d.date == sel
                val bg by animateColorAsState(if (on) Color.White else Color.Transparent, tween(220), label = "dsu")
                Column(Modifier.width(52.dp).height(64.dp).clip(RoundedCornerShape(26.dp)).background(bg).press(RoundedCornerShape(26.dp)) { onSel(d.date) }
                    .graphicsAlpha(if (d.count == 0 && !on) .4f else 1f),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(Tr.dayShort(lang, d.date.dayOfWeek.value - 1), color = if (on) Color(0xFF5A5290) else t.tx2, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("${d.date.dayOfMonth}", color = if (on) Color(0xFF2A2356) else t.tx, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Dots(d, dot, 5.dp)
                }
            }
        }
        else -> Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(28.dp)).background(t.s1).padding(horizontal = 10.dp, vertical = 16.dp)) {
            // ColorOS: как почасовой прогноз — выбранный день объёмной плашкой
            days.forEach { d ->
                val on = d.date == sel
                val shape = RoundedCornerShape(22.dp)
                val a by animateFloatAsState(if (on) 1f else 0f, tween(220), label = "dsc")
                Column(Modifier.weight(1f).clip(shape).then(if (on) Modifier.vol3d(shape) else Modifier).graphicsAlpha(if (d.count == 0 && !on) .45f else 1f)
                    .press(shape) { onSel(d.date) }.padding(vertical = 9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(Tr.dayShort(lang, d.date.dayOfWeek.value - 1), color = lerp(t.tx2, t.tx, a), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("${d.date.dayOfMonth}", color = t.tx, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (d.count > 0) "${d.count} " + (when (lang) { "kk" -> "сб."; "en" -> "les."; else -> "ур." }) else "—",
                        color = Color(0x57EBEBF5), fontSize = 13.sp)
                    Dots(d, if (d.changed) Color(0xFFFF9F0A) else t.acc)
                }
            }
        }
    }
}

/** Точки под днём: задания (или изменения). */
@Composable
private fun Dots(d: DayInfo, color: Color, s: Dp = 6.dp) {
    Row(Modifier.padding(top = 3.dp).height(s), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        val n = if (d.changed && d.hw == 0) 1 else d.hw.coerceAtMost(3)
        repeat(n) { Box(Modifier.size(s).clip(CircleShape).background(color)) }
    }
}

fun Modifier.graphicsScale(s: Float): Modifier = this.graphicsLayer { scaleX = s; scaleY = s }

/* ===================== Лента уроков ===================== */

enum class RowState { Normal, Past, Cur }

/** Строка урока для ленты: всё уже посчитано заранее. */
data class LItem(
    val key: String,
    val start: Int, val end: Int,
    val name: String, val emoji: String, val color: Long,
    val sub: String,
    val state: RowState = RowState.Normal,
    val hw: Boolean = false,
    val was: String? = null,
    val cancelled: Boolean = false,
    val progress: Float = 0f,
)

/** Время слева или справа: настройка или как в стиле (ColorOS — слева, остальные — справа). */
@Composable
fun timeLeft(pref: String): Boolean = when (pref) { "left" -> true; "right" -> false; else -> T.id == "color" }

/**
 * Лента уроков дня в стиле темы. Для ColorOS и iOS — с вертикальной линией и точками,
 * для Pixel — отдельные плашки с формами, для OriginOS — стеклянные «уведомления», One UI — матовая карточка.
 */
@Composable
fun LessonFeed(
    items: List<LItem>, lang: String, timeLeftSide: Boolean,
    swapLabel: String, cancelLabel: String,
    onClick: (Int) -> Unit, onLong: (Int) -> Unit, onRestore: (Int) -> Unit = {},
) {
    val t = T
    val rail = t.id == "color" || t.id == "ios26" || t.id == "ios18"
    val sep = t.pixel || t.id == "origin"
    val outer = RoundedCornerShape(if (t.id == "color" || t.id == "ios26") 30.dp else 28.dp)
    val content: @Composable () -> Unit = {
        items.forEachIndexed { i, it ->
            val first = i == 0; val last = i == items.lastIndex
            FeedRow(it, i, first, last, rail, timeLeftSide, swapLabel, cancelLabel, { onClick(i) }, { onLong(i) }, { onRestore(i) })
        }
    }
    if (sep) Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(if (t.pixel) 3.dp else 8.dp)) { content() }
    else Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).styleCard(outer).padding(vertical = 6.dp, horizontal = if (t.id == "oneui") 8.dp else 0.dp)) { content() }
}

@Composable
private fun FeedRow(
    x: LItem, i: Int, first: Boolean, last: Boolean, rail: Boolean, tLeft: Boolean,
    swapLabel: String, cancelLabel: String, onClick: () -> Unit, onLong: () -> Unit, onRestore: () -> Unit,
) {
    val t = T
    val past = x.state == RowState.Past
    val cur = x.state == RowState.Cur
    val alpha = if (past) .42f else 1f
    val curC = when (t.id) { "ios26", "ios18" -> t.acc; "color" -> t.acc; "origin" -> Color(0xFFFF9A3C); else -> t.tx }

    // форма и фон строки
    val big = 28.dp; val small = 8.dp
    val rowShape: Shape = when {
        t.pixel && cur -> RoundedCornerShape(22.dp)
        t.pixel -> RoundedCornerShape(topStart = if (first) big else small, topEnd = if (first) big else small, bottomStart = if (last) big else small, bottomEnd = if (last) big else small)
        t.id == "origin" -> RoundedCornerShape(24.dp)
        else -> RoundedCornerShape(24.dp)
    }
    val rowBg: Modifier = when {
        t.pixel -> Modifier.clip(rowShape).background(if (cur) t.secCont else t.s1)
        t.id == "origin" && cur -> Modifier.vivoGlass(rowShape, Brush.verticalGradient(listOf(Color(0x5CFFA050), Color(0x24FF7828))))
        t.id == "origin" -> Modifier.vivoGlass(rowShape)
        else -> Modifier
    }

    val timeCol: @Composable () -> Unit = {
        Column(Modifier.width(58.dp).graphicsAlpha(alpha), horizontalAlignment = if (tLeft) Alignment.Start else Alignment.End) {
            Text(Tr.hm(x.start), color = t.tx, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(Tr.hm(x.end), color = t.tx2.copy(alpha = .6f), fontSize = 13.sp)
        }
    }
    val railCol: @Composable () -> Unit = {
        Canvas(Modifier.width(18.dp).fillMaxHeight()) {
            val cx = size.width / 2; val cy = size.height / 2
            val doneC = curC.copy(alpha = .55f)
            val idle = Color.White.copy(alpha = .14f)
            if (!first) drawLine(if (past || cur) doneC else idle, Offset(cx, 0f), Offset(cx, cy), 2.dp.toPx())
            if (!last) drawLine(if (past) doneC else idle, Offset(cx, cy), Offset(cx, size.height), 2.dp.toPx())
            if (cur) {
                drawCircle(curC.copy(alpha = .22f), 11.dp.toPx(), Offset(cx, cy))
                drawCircle(curC, 7.dp.toPx(), Offset(cx, cy))
            } else drawCircle(if (past) curC.copy(alpha = .75f) else Color.White.copy(alpha = .3f), 4.5.dp.toPx(), Offset(cx, cy))
        }
    }
    val body: @Composable RowScope.() -> Unit = {
        val shape = RoundedCornerShape(24.dp)
        val curBg = when {
            !cur -> Modifier
            t.id == "color" -> Modifier.vol3d(shape)
            t.id == "ios26" || t.id == "ios18" -> Modifier.liquid(shape, t.acc.copy(alpha = .28f))
            t.id == "oneui" -> Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = .16f))
            else -> Modifier
        }
        Row(Modifier.weight(1f).padding(horizontal = if (rail) 4.dp else 0.dp, vertical = if (rail || t.id == "oneui") 4.dp else 0.dp).then(curBg)
            .padding(horizontal = 12.dp, vertical = 10.dp).graphicsAlpha(alpha),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.graphicsAlpha(if (x.cancelled) .35f else 1f)) { Tile(x.emoji, x.color, if (t.pixel) 46.dp else 46.dp) }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(x.name, color = when { x.cancelled -> t.tx2.copy(alpha = .6f); cur && !t.pixel && t.id != "oneui" && t.id != "origin" -> curC; else -> t.tx },
                        fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        textDecoration = if (x.cancelled) TextDecoration.LineThrough else null, modifier = Modifier.weight(1f, fill = false))
                    if (x.hw && !x.cancelled) Icon(Ic.hw, null, tint = if (t.pixel) Color(0xFFFFB1A8) else if (t.id == "origin") Color(0xFFFF9A3C) else t.acc, modifier = Modifier.size(17.dp))
                }
                when {
                    x.cancelled -> Row(Modifier.padding(top = 4.dp)) { Tag(cancelLabel, Color(0xFFFF6961)) }
                    x.was != null -> {
                        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Tag(swapLabel, Color(0xFFFF9F0A))
                            Text(x.sub, color = t.tx2, fontSize = 15.sp, maxLines = 1)
                        }
                        Text(x.was, color = t.tx2.copy(alpha = .55f), fontSize = 13.sp, textDecoration = TextDecoration.LineThrough, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
                    }
                    else -> if (x.sub.isNotEmpty()) Text(x.sub, color = t.tx2, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                }
                if (cur && t.id == "origin") ProgressLine(x.progress, Color.White, Color.White.copy(alpha = .2f), 4.dp, Modifier.padding(top = 8.dp))
            }
            if (x.cancelled) Box(Modifier.size(36.dp).clip(CircleShape).press(CircleShape, onClick = onRestore), contentAlignment = Alignment.Center) {
                Icon(Ic.restore, null, tint = t.tx2, modifier = Modifier.size(20.dp))
            }
            if (!rail) timeCol()
        }
    }
    Column(Modifier.fillMaxWidth().appear(i).then(rowBg).press(rowShape, scaleTo = .975f, onLong = onLong, onClick = onClick)) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).heightIn(min = if (t.pixel) 72.dp else 74.dp)
            .padding(start = if (rail && tLeft) 18.dp else if (!rail && !t.pixel && t.id != "origin") 0.dp else 0.dp, end = if (rail && !tLeft) 18.dp else 0.dp)
            .padding(horizontal = if (t.pixel || t.id == "origin") 4.dp else 0.dp, vertical = if (t.pixel || t.id == "origin") 2.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically) {
            if (rail && tLeft) { timeCol(); railCol(); body() }
            else if (rail) { body(); railCol(); timeCol() }
            else if (tLeft) { Spacer(Modifier.width(12.dp)); timeCol(); body() }
            else { body(); Spacer(Modifier.width(12.dp)) }
        }
        if (cur && t.pixel) Box(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp)) { WavyProgress(x.progress, t.onSecCont, t.onSecCont.copy(alpha = .25f)) }
    }
}

/** Тонкая полоса прогресса. */
@Composable
fun ProgressLine(p: Float, fg: Color, track: Color, h: Dp = 6.dp, modifier: Modifier = Modifier, glow: Boolean = false) {
    val a by animateFloatAsState(p.coerceIn(0f, 1f), tween(600), label = "pl")
    Canvas(modifier.fillMaxWidth().height(h)) {
        val r = size.height / 2
        drawLine(track, Offset(r, r), Offset(size.width - r, r), size.height, StrokeCap.Round)
        if (a > 0f) {
            val x = r + (size.width - 2 * r) * a
            if (glow) drawLine(fg.copy(alpha = .35f), Offset(r, r), Offset(x, r), size.height * 2.2f, StrokeCap.Round)
            drawLine(fg, Offset(r, r), Offset(x, r), size.height, StrokeCap.Round)
        }
    }
}

/* ===================== Кнопка «Добавить» ===================== */

@Composable
fun AddButton(text: String, onClick: () -> Unit) {
    val t = T
    val shape = RoundedCornerShape(if (t.pixel) 20.dp else 99.dp)
    val m = when (t.id) {
        "color" -> Modifier.vol3d(shape)
        "ios26" -> Modifier.liquid(shape)
        "origin" -> Modifier.vivoGlass(shape)
        "oneui" -> Modifier.frost(shape)
        "pixel" -> Modifier.clip(shape).background(Color(0xFF4F378B))
        else -> Modifier.clip(shape).background(t.s1)
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(if (t.id == "color") 60.dp else 58.dp).then(m).press(shape, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        val fg = if (t.pixel) Color(0xFFEADDFF) else if (t.id == "ios26" || t.id == "ios18") t.acc else t.tx
        Icon(Ic.plus, null, tint = fg, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = fg, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Suppress("unused") private fun keep(b: Brush) = b


/* ===================== Галочки ===================== */

private val TICK = listOf(Offset(5f, 12.5f), Offset(9.5f, 17f), Offset(19f, 7.5f))

/** Галочка, которая «рисуется» штрихом (и стирается обратно). Размер — сторона квадрата, в который вписана галочка. */
@Composable
fun AnimTick(on: Boolean, color: Color, size: Dp, stroke: Dp = 2.2.dp, modifier: Modifier = Modifier) {
    val p by animateFloatAsState(
        if (on) 1f else 0f,
        androidx.compose.animation.core.tween(if (on) 300 else 140, delayMillis = if (on) 90 else 0, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "tick",
    )
    androidx.compose.foundation.Canvas(modifier.size(size)) {
        if (p <= 0.001f) return@Canvas
        val k = this.size.minDimension / 24f
        val a = TICK.map { Offset(it.x * k, it.y * k) }
        val l1 = (a[1] - a[0]).getDistance(); val l2 = (a[2] - a[1]).getDistance()
        val d = (l1 + l2) * p
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(a[0].x, a[0].y)
            if (d <= l1) { val f = d / l1; lineTo(a[0].x + (a[1].x - a[0].x) * f, a[0].y + (a[1].y - a[0].y) * f) }
            else { lineTo(a[1].x, a[1].y); val f = ((d - l1) / l2).coerceAtMost(1f); lineTo(a[1].x + (a[2].x - a[1].x) * f, a[1].y + (a[2].y - a[1].y) * f) }
        }
        drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}

/**
 * Галочка-переключатель для всех стилей: заливка вырастает из центра с пружиной, штрих галочки дорисовывается,
 * коробочка слегка «подпрыгивает», по краю расходится тонкая волна. При снятии всё играется в обратную сторону.
 */
@Composable
fun AnimCheck(on: Boolean, size: Dp, square: Boolean, ring: Color, onClick: () -> Unit) {
    val t = T
    val fill by animateFloatAsState(if (on) 1f else 0f, spring(.55f, 420f), label = "ckf")
    val burst = remember { androidx.compose.animation.core.Animatable(0f) }
    val pop = remember { androidx.compose.animation.core.Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(on) {
        if (first) { first = false; return@LaunchedEffect }
        if (on) {
            launch { burst.snapTo(0f); burst.animateTo(1f, androidx.compose.animation.core.tween(520, easing = androidx.compose.animation.core.CubicBezierEasing(.05f, .7f, .1f, 1f))) }
            pop.animateTo(1.18f, androidx.compose.animation.core.tween(90))
            pop.animateTo(1f, spring(.4f, 450f))
        } else {
            burst.snapTo(0f)
            pop.animateTo(.9f, androidx.compose.animation.core.tween(70))
            pop.animateTo(1f, spring(.5f, 500f))
        }
    }
    val shape = if (square) RoundedCornerShape(6.dp) else CircleShape
    Box(
        Modifier.size(size).graphicsLayer { scaleX = pop.value; scaleY = pop.value }
            .press(shape, scaleTo = .85f, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val s = this.size.minDimension; val half = s / 2f
            val fr = fill.coerceAtLeast(0f)
            val border = androidx.compose.ui.graphics.lerp(ring.copy(alpha = .6f), t.acc, fr.coerceIn(0f, 1f))
            val sw = 2.dp.toPx()
            if (square) {
                val cr = 6.dp.toPx()
                drawRoundRect(border, Offset(sw / 2, sw / 2), Size(s - sw, s - sw), androidx.compose.ui.geometry.CornerRadius(cr), style = androidx.compose.ui.graphics.drawscope.Stroke(sw))
                if (fr > 0f) { val inset = (1f - fr) * half; drawRoundRect(t.acc, Offset(inset, inset), Size((s - 2 * inset).coerceAtLeast(0f), (s - 2 * inset).coerceAtLeast(0f)), androidx.compose.ui.geometry.CornerRadius(cr * fr.coerceAtMost(1f)))}
            } else {
                drawCircle(border, half - sw / 2, style = androidx.compose.ui.graphics.drawscope.Stroke(sw))
                if (fr > 0f) drawCircle(t.acc, half * fr)
            }
            val b = burst.value
            if (b > 0f && b < 1f) drawCircle(t.acc.copy(alpha = (1f - b) * .55f), half * (1f + b * .75f), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx() * (1f - b) + .5f))
        }
        AnimTick(on, t.accTx, size * .62f, stroke = if (square) 2.2.dp else 2.4.dp)
    }
}

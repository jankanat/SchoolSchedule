package kz.alnazar.schedule.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.alnazar.schedule.data.AppState
import kz.alnazar.schedule.data.CustomTheme
import kz.alnazar.schedule.data.Store
import kz.alnazar.schedule.logic.Tr
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Своя тема: два цвета (акцент и основной), каждый настраивается
 * кругом радуги (тон + насыщенность) и тремя ползунками.
 * Пока тянешь — меняется только память (всё приложение перекрашивается вживую),
 * на отпускании — сохраняем и обновляем виджеты.
 */
@Composable
fun CustomThemeEditor(st: AppState) {
    val ctx = LocalContext.current
    val lang = st.appLang
    var target by rememberSaveable { mutableStateOf("acc") }
    val c = st.ct
    val isAcc = target == "acc"
    val h = if (isAcc) c.ah else c.bh
    val s = if (isAcc) c.asat else c.bs
    val l = if (isAcc) c.al else c.bl

    fun edit(h2: Float = h, s2: Float = s, l2: Float = l, a2: Float = c.alpha): CustomTheme =
        if (isAcc) c.copy(ah = h2, asat = s2, al = l2, alpha = a2) else c.copy(bh = h2, bs = s2, bl = l2, alpha = a2)
    fun live(n: CustomTheme) = Store.preview { it.copy(ct = n) }
    fun commit() = Store.update(ctx) { it }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Цель: акцент или основной (фон), с образцами текущих цветов
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Swatch(Tr.t(lang, "ct_acc"), hsl(c.ah, c.asat, c.al), isAcc, Modifier.weight(1f)) { target = "acc" }
            Swatch(Tr.t(lang, "ct_base"), hsl(c.bh, c.bs, c.bl), !isAcc, Modifier.weight(1f)) { target = "base" }
        }

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    ColorWheel(h, s, l, Modifier.fillMaxWidth(.78f),
                        onChange = { nh, ns -> live(edit(h2 = nh, s2 = ns)) }, onEnd = { commit() })
                }
                GradSlider(Tr.t(lang, "ct_sat"), s,
                    listOf(hsl(h, 0f, l.coerceIn(.3f, .7f)), hsl(h, 1f, l.coerceIn(.3f, .7f))), hsl(h, s, l),
                    onChange = { live(edit(s2 = it)) }, onEnd = { commit() })
                GradSlider(Tr.t(lang, "ct_light"), l,
                    listOf(Color.Black, hsl(h, s, .5f), Color.White), hsl(h, s, l),
                    onChange = { live(edit(l2 = it)) }, onEnd = { commit() })
                GradSlider(Tr.t(lang, "ct_alpha"), c.alpha,
                    listOf(hsl(c.bh, c.bs, c.bl + .1f, .15f), hsl(c.bh, c.bs, c.bl + .1f, 1f)), hsl(c.bh, c.bs, c.bl + .1f),
                    checker = true, min = .15f,
                    onChange = { live(edit(a2 = it)) }, onEnd = { commit() })
            }
        }

        Text(Tr.t(lang, "ct_presets"), color = T.tx2, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            PRESETS.forEach { p ->
                Box(
                    Modifier.size(42.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(hsl(p.bh, p.bs, p.bl + .12f), hsl(p.ah, p.asat, p.al))))
                        .border(1.dp, T.tx.copy(alpha = .2f), CircleShape)
                        .tap { Store.update(ctx) { it.copy(ct = p) } },
                )
            }
        }
    }
}

private val PRESETS = listOf(
    CustomTheme(),                                                               // фиолетовая ночь
    CustomTheme(ah = 160f, asat = .7f, al = .55f, bh = 200f, bs = .4f, bl = .08f, alpha = .7f),  // бирюза
    CustomTheme(ah = 25f, asat = .95f, al = .6f, bh = 15f, bs = .25f, bl = .08f, alpha = .72f),  // закат
    CustomTheme(ah = 340f, asat = .85f, al = .65f, bh = 300f, bs = .3f, bl = .1f, alpha = .7f),  // малина
    CustomTheme(ah = 215f, asat = .9f, al = .5f, bh = 210f, bs = .25f, bl = .94f, alpha = .8f),  // светлая
    CustomTheme(ah = 45f, asat = .0f, al = .9f, bh = 0f, bs = 0f, bl = .04f, alpha = .6f),      // ч/б
)

@Composable
private fun Swatch(label: String, color: Color, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val t = T
    Row(
        modifier.clip(RoundedCornerShape(16.dp))
            .background(if (on) t.s2 else t.s1)
            .border(if (on) 2.dp else 1.dp, if (on) t.acc else t.line, RoundedCornerShape(16.dp))
            .tap(onClick = onClick).padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(26.dp).clip(CircleShape).background(color).border(1.dp, t.tx.copy(alpha = .25f), CircleShape))
        Text(label, color = t.tx, fontSize = 15.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
    }
}

/** Круг радуги: угол — тон, расстояние от центра — насыщенность. */
@Composable
private fun ColorWheel(h: Float, s: Float, l: Float, modifier: Modifier, onChange: (Float, Float) -> Unit, onEnd: () -> Unit) {
    val change by rememberUpdatedState(onChange)
    val end by rememberUpdatedState(onEnd)
    val hues = remember { List(13) { Color.hsv(it * 30f % 360f, 1f, 1f) } }
    BoxWithConstraints(modifier.aspectRatio(1f)) {
        val d = LocalDensity.current
        val sizePx = with(d) { maxWidth.toPx() }
        val r = sizePx / 2f
        fun pick(p: Offset) {
            val dx = p.x - r; val dy = p.y - r
            val ang = (Math.toDegrees(atan2(dy, dx).toDouble()).toFloat() + 360f) % 360f
            change(ang, (hypot(dx, dy) / r).coerceIn(0f, 1f))
        }
        Canvas(
            Modifier.matchParentSize()
                .pointerInput(Unit) { detectTapGestures { pick(it); end() } }
                .pointerInput(Unit) { detectDragGestures(onDragStart = { pick(it) }, onDragEnd = { end() }, onDragCancel = { end() }) { ch, _ -> pick(ch.position) } },
        ) {
            drawCircle(Brush.sweepGradient(hues, center))
            drawCircle(Brush.radialGradient(listOf(Color.White, Color.White.copy(alpha = 0f)), center, size.minDimension / 2f))
            // затемнение/осветление — чтобы круг показывал выбранную яркость
            val dim = ((.5f - l) * 1.6f).coerceIn(-.85f, .85f)
            if (dim > 0f) drawCircle(Color.Black.copy(alpha = dim)) else if (dim < 0f) drawCircle(Color.White.copy(alpha = -dim))
        }
        val rad = Math.toRadians(h.toDouble())
        val px = r + cos(rad).toFloat() * s * r
        val py = r + sin(rad).toFloat() * s * r
        val k = with(d) { 30.dp.toPx() }
        Box(
            Modifier.offset { IntOffset((px - k / 2).roundToInt(), (py - k / 2).roundToInt()) }
                .size(30.dp).shadow(6.dp, CircleShape).clip(CircleShape)
                .background(Color.White).padding(3.dp).clip(CircleShape).background(hsl(h, s, l)),
        )
    }
}

/** Ползунок с градиентной дорожкой и круглым бегунком. */
@Composable
fun GradSlider(
    label: String, value: Float, colors: List<Color>, knobColor: Color, checker: Boolean = false, min: Float = 0f,
    fmt: (Float) -> String = { "${(it * 100).roundToInt()}%" },
    onChange: (Float) -> Unit, onEnd: () -> Unit,
) {
    val change by rememberUpdatedState(onChange)
    val end by rememberUpdatedState(onEnd)
    val t = T
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row {
            Text(label, color = t.tx, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text(fmt(value), color = t.tx2, fontSize = 14.sp)
        }
        BoxWithConstraints(Modifier.fillMaxWidth().height(30.dp)) {
            val d = LocalDensity.current
            val w = with(d) { maxWidth.toPx() }
            val knob = with(d) { 28.dp.toPx() }
            fun pick(x: Float) = change(((x - knob / 2) / (w - knob)).coerceIn(0f, 1f).coerceAtLeast(min))
            Box(
                Modifier.matchParentSize()
                    .pointerInput(Unit) { detectTapGestures { pick(it.x); end() } }
                    .pointerInput(Unit) { detectDragGestures(onDragStart = { pick(it.x) }, onDragEnd = { end() }, onDragCancel = { end() }) { ch, _ -> pick(ch.position.x) } },
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)).let { m ->
                    if (checker) m.background(Color(0xFF8A8A8A)).background(
                        Brush.linearGradient(listOf(Color(0xFFCCCCCC), Color(0xFF666666), Color(0xFFCCCCCC), Color(0xFF666666), Color(0xFFCCCCCC)))) else m
                }.background(Brush.horizontalGradient(colors)).border(1.dp, t.tx.copy(alpha = .15f), RoundedCornerShape(7.dp)))
                Box(
                    Modifier.offset { IntOffset((value * (w - knob)).roundToInt(), 0) }
                        .size(28.dp).shadow(5.dp, CircleShape).clip(CircleShape).background(Color.White)
                        .padding(4.dp).clip(CircleShape).background(knobColor),
                )
            }
        }
    }
}

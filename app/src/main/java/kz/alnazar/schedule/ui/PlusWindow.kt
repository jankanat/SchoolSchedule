package kz.alnazar.schedule.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.min
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Где на экране находится кнопка «+», из которой «вырастает» окно. */
object PlusAnchor {
    var coords: LayoutCoordinates? = null
    var rect: Rect? = null
    /** Пока окно открыто (и сворачивается), настоящая кнопка «+» спрятана — вместо неё само окно. */
    var open by mutableStateOf(false)
    /** Запоминает положение кнопки в момент нажатия. */
    fun capture() { rect = coords?.takeIf { it.isAttached }?.boundsInRoot() }
}

fun Modifier.plusAnchor(): Modifier = this.onGloballyPositioned { PlusAnchor.coords = it }

/**
 * Окно из «+»: кружок вырастает в компактное скруглённое окно по центру экрана (цвет кнопки перетекает в цвет окна, плюс
 * поворачивается и гаснет, содержимое проявляется). При закрытии всё сворачивается обратно в кнопку.
 */
@Composable
fun BoxScope.PlusWindow(visible: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val t = T
    // Как открытие приложения в HyperOS / ColorOS / iOS: движение к центру быстрое, а масштаб растёт медленнее (на половине пути ~ четверть размера)
    val ep = remember { Animatable(0f) }   // движение
    val e = remember { Animatable(0f) }    // размер, форма, содержимое
    var mounted by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            PlusAnchor.open = true; mounted = true
            launch { ep.animateTo(1f, tween(520, easing = CubicBezierEasing(.1f, .8f, .2f, 1f))) }
            e.animateTo(1f, tween(520, easing = CubicBezierEasing(.25f, .35f, .2f, 1f)))
        } else if (mounted) {
            launch { ep.animateTo(0f, tween(400, easing = CubicBezierEasing(.3f, 0f, .2f, 1f))) }
            e.animateTo(0f, tween(400, easing = CubicBezierEasing(.3f, 0f, .2f, 1f)))
            mounted = false; PlusAnchor.open = false
        }
    }
    if (!mounted) return
    val den = LocalDensity.current
    val anchor = remember { PlusAnchor.rect }
    var host by remember { mutableStateOf(Offset.Zero) }
    val startW = anchor?.width ?: with(den) { 64.dp.toPx() }
    val startH = anchor?.height ?: with(den) { 64.dp.toPx() }
    val ac = anchor?.center ?: Offset(host.x * 1.7f, host.y * 1.85f)
    val endCr = with(den) { (if (t.nav == "m3") 28.dp else if (t.id == "oneui") 36.dp else 32.dp).toPx() }
    val c0 = when {
        t.nav == "m3" -> Color(0xFFFF8A80)
        t.id == "color" -> Color(0xFF4DDB80)
        t.id == "ios26" -> Color(0x66302F3C)
        else -> Color(0x40FFFFFF)
    }
    val plusTint = when { t.nav == "m3" -> Color(0xFF4A0E0B); t.id == "color" -> Color(0xFF05230F); else -> t.tx }
    val marginPx = with(den) { 24.dp.toPx() }
    val maxWpx = with(den) { 360.dp.toPx() }

    Box(
        Modifier.fillMaxSize().graphicsLayer { alpha = e.value.coerceIn(0f, 1f) }.background(Color(0x99000000))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
    )
    Box(Modifier.fillMaxSize().imePadding().onGloballyPositioned { host = it.boundsInRoot().center }, contentAlignment = Alignment.Center) {
        Layout(
            content = {
                Column(
                    Modifier.fillMaxWidth().graphicsLayer { alpha = ((e.value - .28f) / .42f).coerceIn(0f, 1f) }
                        .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 22.dp),
                    content = content,
                )
                Icon(Ic.plus, null, tint = plusTint, modifier = Modifier.size(28.dp).graphicsLayer {
                    val v = e.value.coerceIn(0f, 1f)
                    alpha = (1f - v * 3.2f).coerceIn(0f, 1f); rotationZ = v * 135f
                })
            },
            modifier = Modifier
                .offset { IntOffset(((ac.x - host.x) * (1f - ep.value)).roundToInt(), ((ac.y - host.y) * (1f - ep.value)).roundToInt()) }
                .graphicsLayer {
                    val v = e.value
                    shadowElevation = 28.dp.toPx() * v.coerceIn(0f, 1f)
                    clip = true
                    shape = RoundedCornerShape(CornerSize(lerp(startW / 2f, endCr, v).coerceAtLeast(0f)))
                    alpha = if (host == Offset.Zero) 0f else 1f
                }
                .drawBehind { drawRect(androidx.compose.ui.graphics.lerp(c0, t.sheet, (e.value * 2.2f).coerceIn(0f, 1f))) }
                .pointerInput(Unit) { detectTapGestures { } },
        ) { ms, c ->
            val maxW = min(c.maxWidth - marginPx.roundToInt() * 2, maxWpx.roundToInt()).coerceAtLeast(1)
            val maxH = (c.maxHeight * .88f).roundToInt().coerceAtLeast(1)
            val body = ms[0].measure(Constraints(minWidth = maxW, maxWidth = maxW, maxHeight = maxH))
            val ic = ms[1].measure(Constraints())
            val v = e.value
            val w = lerp(startW, body.width.toFloat(), v).roundToInt().coerceAtLeast(1)
            val h = lerp(startH, body.height.toFloat(), v).roundToInt().coerceAtLeast(1)
            layout(w, h) {
                body.place((w - body.width) / 2, (h - body.height) / 2)
                ic.place((w - ic.width) / 2, (h - ic.height) / 2)
            }
        }
    }
}

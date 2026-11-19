package kz.alnazar.schedule.ui

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Кривая как на картинке пользователя: резкий старт, долгое плавное затухание. */
private val DECEL = CubicBezierEasing(.05f, .7f, .1f, 1f)
/** Та же кривая, но с лёгким перелётом в конце — кнопки чуть пружинят. */
private val BOUNCY = CubicBezierEasing(.05f, .66f, .10f, 1.22f)

/**
 * Нижнее меню ColorOS 17: слева круглая кнопка настроек, в центре стеклянная капсула с вкладками, справа зелёная «+».
 *  • Нажали капсулу — она мягко сжимается, под пальцем мягкое свечение, которое едет за пальцем;
 *    вкладка под пальцем сразу становится белой, но экран переключается только когда палец отпустили.
 *  • В этот момент боковые кнопки втекают в капсулу как капли жидкого стекла (масштаб — по кривой затухания,
 *    сдвиг — быстрая пружина), а при отпускании вытекают обратно с лёгкой пружиной.
 *  • Если тянуть палец — капсула тянется за ним как резина.
 */
@Composable
fun LiquidDock(tabs: List<NavTab>, pager: PagerState, backdrop: GraphicsLayer, modifier: Modifier, onPlus: () -> Unit, onSettings: () -> Unit) {
    val t = T
    val scope = rememberCoroutineScope()
    val hf = LocalHapticFeedback.current
    val den = LocalDensity.current
    val absorb = remember { Animatable(0f) }          // 0 — кнопки на месте, 1 — втекли в капсулу
    val move = remember { Animatable(0f) }
    val press = remember { Animatable(1f) }
    val glowA = remember { Animatable(0f) }
    val drag = remember { Animatable(0f) }
    val dragY = remember { Animatable(0f) }
    var touchX by remember { mutableStateOf(0f) }
    var hover by remember { mutableIntStateOf(-1) }
    // Какие боковые кнопки нужны на текущей вкладке: «+» — на Главной и Заданиях, настройки — на Главной и Виджетах.
    // Лишняя кнопка втекает в капсулу так же, как при нажатии на неё.
    val page = pager.targetPage
    val showL = remember { Animatable(if (page == 0) 1f else 0f) }
    val showR = remember { Animatable(if (page == 0 || page == 2) 1f else 0f) }
    LaunchedEffect(page) {
        launch { showL.animateTo(if (page == 0) 1f else 0f, tween(800, easing = BOUNCY)) }
        launch { showR.animateTo(if (page == 0 || page == 2) 1f else 0f, tween(800, easing = BOUNCY)) }
    }

    val circ = 62.dp; val gap = 8.dp; val pillH = 68.dp
    val tint = Color(0x66323236)
    val pillShape = RoundedCornerShape(99.dp)

    BoxWithConstraints(modifier.fillMaxWidth().navigationBarsPadding().padding(start = 4.dp, end = 4.dp, bottom = 18.dp).height(pillH)) {
        val pillW = maxWidth - (circ + gap) * 2
        val n = tabs.size
        val itemW = (pillW - 12.dp) / n
        val cur = if (hover >= 0) hover else pager.targetPage
        // сдвиг круга к точке внутри капсулы
        val travel = with(den) { (gap + circ / 2 + 20.dp).toPx() }
        // Значения анимаций читаем только внутри graphicsLayer/draw-лямбд — тогда каждый кадр перерисовывается слой, а не пересобирается всё меню (это и был лаг)
        val pillWpx = with(den) { pillW.toPx() }
        val rubberPx = with(den) { 8.dp.toPx() }
        fun dxL() = maxOf(move.value, 1f - showL.value) * travel
        fun dxR() = maxOf(move.value, 1f - showR.value) * travel
        fun sL() = ((1f - absorb.value) * showL.value).coerceIn(0f, 1f)
        fun sR() = if (PlusAnchor.open) 0f else ((1f - absorb.value) * showR.value).coerceIn(0f, 1f)
        val rubberYpx = with(den) { 10.dp.toPx() }; val pillHpx = with(den) { pillH.toPx() }
        // вертикальная тяга увеличивает капсулу целиком (равномерно), поэтому она остаётся капсулой, только крупнее
        fun vf() = 1f + abs(rubberX(dragY.value, rubberYpx)) / pillHpx * .5f
        fun pillSX() = press.value * (1f + abs(drag.value) / pillWpx * .06f) * vf()
        fun pillTx() = rubberX(drag.value, rubberPx)
        fun pillSY() = press.value * vf()
        fun pillTy() = rubberX(dragY.value, rubberYpx)
        val lOn by remember { androidx.compose.runtime.derivedStateOf { ((1f - absorb.value) * showL.value) > .6f } }
        val rOn by remember { androidx.compose.runtime.derivedStateOf { !PlusAnchor.open && ((1f - absorb.value) * showR.value) > .6f } }
        val acc = t.acc
        val plusInk = androidx.compose.ui.graphics.lerp(acc, Color.Black, .82f)

        // ---- «жидкий» слой: капсула и кружки сливаются (размытие + порог прозрачности) ----
        Canvas(Modifier.fillMaxSize().graphicsLayer {
            renderEffect = gooEffect(.20f, .20f, .22f, 9.dp.toPx())
            alpha = .62f
        }) { drawBlobs(this, circ, gap, pillW, pillH, dxL(), sL(), pillSX(), pillSY(), pillTx(), onlyAccent = false, pillTy = pillTy()) }
        Canvas(Modifier.fillMaxSize().graphicsLayer { renderEffect = gooEffect(acc.red, acc.green, acc.blue, 9.dp.toPx()) }) {
            drawBlobs(this, circ, gap, pillW, pillH, dxR(), sR(), pillSX(), pillSY(), pillTx(), onlyAccent = true, pillTy = pillTy())
        }

        // ---- левая кнопка: настройки (с бликами по краю, как у капсулы) ----
        val rim = Brush.verticalGradient(listOf(Color(0x59FFFFFF), Color.Transparent, Color(0x1AFFFFFF)))
        Box(Modifier.offset(x = 0.dp, y = (pillH - circ) / 2).size(circ)
            .graphicsLayer { val k = sL(); translationX = dxL(); scaleX = k; scaleY = k; alpha = (k * 1.4f).coerceIn(0f, 1f) }
            .clip(CircleShape).backdropBlur(backdrop, 20.dp, Color.Transparent, CircleShape)
            .border(1.dp, rim, CircleShape)
            .drawWithContent {
                drawContent()
                // мягкий блик сверху-слева
                drawCircle(Brush.radialGradient(listOf(Color(0x2EFFFFFF), Color.Transparent), Offset(size.width * .3f, size.height * .22f), size.width * .6f))
            }
            .press(CircleShape, scaleTo = .88f, enabled = lOn, onClick = onSettings), contentAlignment = Alignment.Center) {
            Icon(Ic.set, null, tint = t.tx, modifier = Modifier.size(26.dp))
        }
        // ---- правая кнопка: «+» ----
        Box(Modifier.offset(x = maxWidth - circ, y = (pillH - circ) / 2).size(circ).plusAnchor()
            .graphicsLayer { val k = sR(); translationX = -dxR(); scaleX = k; scaleY = k; alpha = (k * 1.4f).coerceIn(0f, 1f) }
            .clip(CircleShape)
            // слабая кромка: светлая сверху и снизу, каждая ~35% окружности
            .border(1.2.dp, Brush.verticalGradient(0f to Color(0x66FFFFFF), .35f to Color.Transparent, .65f to Color.Transparent, 1f to Color(0x47FFFFFF)), CircleShape)
            .press(CircleShape, scaleTo = .88f, enabled = rOn, onClick = onPlus), contentAlignment = Alignment.Center) {
            Icon(Ic.plus, null, tint = plusInk, modifier = Modifier.size(28.dp))
        }

        // ---- капсула ----
        Box(Modifier.offset(x = circ + gap).width(pillW).height(pillH)
            .graphicsLayer { scaleX = pillSX(); scaleY = pillSY(); translationX = pillTx(); translationY = pillTy() }
            .clip(pillShape).backdropBlur(backdrop, 26.dp, Color.Transparent, pillShape)
            .border(1.dp, Brush.verticalGradient(listOf(Color(0x38FFFFFF), Color.Transparent, Color(0x0DFFFFFF))), pillShape)
            .drawWithContent {
                drawContent()
                val a = glowA.value
                if (a > 0f) {
                    val r = 85.dp.toPx()
                    drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = .38f * a), Color.White.copy(alpha = .16f * a), Color.Transparent),
                        Offset(touchX, size.height / 2), r), r, Offset(touchX, size.height / 2))
                }
            }
            .pointerInput(n) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val padPx = 6.dp.toPx()
                    val itemPx = (size.width - padPx * 2) / n
                    fun idxAt(x: Float) = ((x - padPx) / itemPx).toInt().coerceIn(0, n - 1)
                    touchX = down.position.x
                    hover = idxAt(touchX)
                    scope.launch { press.animateTo(.94f, spring(.6f, 500f)) }
                    scope.launch { glowA.animateTo(1f, tween(140)) }
                    var cancelled = false
                    while (true) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull { it.id == down.id } ?: run { cancelled = true; null } ?: break
                        if (ch.changedToUpIgnoreConsumed()) { ch.consume(); break }
                        touchX = ch.position.x
                        val h2 = idxAt(touchX)
                        if (h2 != hover) hover = h2
                        scope.launch { drag.snapTo(ch.position.x - down.position.x); dragY.snapTo(ch.position.y - down.position.y) }
                        ch.consume()
                    }
                    val target = hover
                    if (!cancelled && target >= 0 && target != pager.currentPage) scope.launch { pager.animateScrollToPage(target) }
                    hover = -1
                    scope.launch { press.animateTo(1f, spring(.45f, 380f)) }
                    scope.launch { glowA.animateTo(0f, tween(380)) }
                    scope.launch { drag.animateTo(0f, spring(.55f, 380f)) }
                    scope.launch { dragY.animateTo(0f, spring(.5f, 380f)) }
                }
            },
        ) {
            Row(Modifier.fillMaxSize().padding(horizontal = 6.dp)) {
                tabs.forEachIndexed { i, tab ->
                    val on = i == cur
                    val c = if (on) Color.White else Color.White.copy(alpha = .5f)
                    Column(Modifier.width(itemW).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(tab.icon, null, tint = c, modifier = Modifier.size(25.dp))
                        Spacer(Modifier.height(4.dp))
                        NavLabel(tab.label, c, 11f, bold = true)
                    }
                }
            }
        }
    }
}

/** Резина: чем дальше тянешь, тем меньше сдвиг. */
private fun rubberX(x: Float, max: Float): Float {
    val s = if (x < 0) -1f else 1f
    val a = abs(x)
    return s * max * (1f - 1f / (a / max * .5f + 1f))
}

/** Капли: размытие и порог прозрачности дают сплошную заливку, а соприкасающиеся фигуры сливаются. */
private fun gooEffect(r: Float, g: Float, b: Float, blur: Float): androidx.compose.ui.graphics.RenderEffect {
    val m = ColorMatrix(floatArrayOf(
        0f, 0f, 0f, 0f, r * 255f,
        0f, 0f, 0f, 0f, g * 255f,
        0f, 0f, 0f, 0f, b * 255f,
        0f, 0f, 0f, 20f, -10f * 255f,
    ))
    val bl = RenderEffect.createBlurEffect(blur, blur, Shader.TileMode.DECAL)
    return RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(m), bl).asComposeRenderEffect()
}

private fun drawBlobs(
    ds: androidx.compose.ui.graphics.drawscope.DrawScope,
    circ: androidx.compose.ui.unit.Dp, gap: androidx.compose.ui.unit.Dp,
    pillW: androidx.compose.ui.unit.Dp, pillH: androidx.compose.ui.unit.Dp,
    dx: Float, s: Float, pillScaleX: Float, pillScaleY: Float, pillTx: Float, onlyAccent: Boolean, pillTy: Float = 0f,
) = with(ds) {
    val c = circ.toPx(); val g = gap.toPx(); val pw = pillW.toPx(); val ph = pillH.toPx()
    val cy = size.height / 2
    if (!onlyAccent) {
        // капсула
        val w = pw * pillScaleX; val h = ph * pillScaleY
        val x0 = c + g + (pw - w) / 2 + pillTx
        drawRoundRect(Color.Black, Offset(x0, (size.height - h) / 2 + pillTy), Size(w, h), CornerRadius(h / 2))
        // левый кружок
        if (s > .02f) drawCircle(Color.Black, c / 2 * s, Offset(c / 2 + dx, cy))
    } else {
        if (s > .02f) drawCircle(Color.Black, c / 2 * s, Offset(size.width - c / 2 - dx, cy))
    }
}

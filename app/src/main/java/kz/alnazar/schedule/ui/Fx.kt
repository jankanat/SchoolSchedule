package kz.alnazar.schedule.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kz.alnazar.schedule.R
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/* ===================== Общие эффекты ===================== */

/** Слой с содержимым экрана — для размытия под меню и шапкой. */
val LocalBackdrop = staticCompositionLocalOf<GraphicsLayer?> { null }

/** Узкий жирный шрифт для крупных цифр (Pixel OS, OriginOS): переменный Roboto, ширина 75%. */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
val CondensedFont: FontFamily by lazy {
    FontFamily(
        listOf(FontWeight.Normal, FontWeight.Bold, FontWeight.ExtraBold).map { w ->
            Font(R.font.f_flex, w, variationSettings = FontVariation.Settings(FontVariation.weight(w.weight), FontVariation.width(75f)))
        },
    )
}

/**
 * Нажатие с откликом по стилю:
 *  • ColorOS — плашка мягко сжимается, под пальцем мягкое свечение, которое едет за пальцем; тянется как резина;
 *  • iOS 26 — сжатие и резина;
 *  • остальные — лёгкое сжатие; Pixel — ещё и рябь Material.
 * Если родитель начал прокрутку — нажатие отменяется.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.press(
    shape: Shape? = null,
    enabled: Boolean = true,
    scaleTo: Float = .965f,
    onLong: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier {
    if (!enabled) return this
    val t = T
    val hf = LocalHapticFeedback.current
    val click by rememberUpdatedState(onClick)
    val long by rememberUpdatedState(onLong)
    if (t.pixel) {
        val src = remember { MutableInteractionSource() }
        val pressed by src.collectIsPressedAsState()
        val s = remember { Animatable(1f) }
        LaunchedEffect(pressed) { s.animateTo(if (pressed) .975f else 1f, spring(.6f, 500f)) }
        return this.graphicsLayer { scaleX = s.value; scaleY = s.value }
            .combinedClickable(src, LocalIndication.current,
                onLongClick = if (onLong != null) ({ hf.performHapticFeedback(HapticFeedbackType.LongPress); long?.invoke() }) else null,
                onClick = { click() })
    }
    val glow = t.id == "color"
    val rubber = t.id == "color" || t.id == "ios26"
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    val glowA = remember { Animatable(0f) }
    val drag = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var touch by remember { mutableStateOf(Offset.Zero) }
    return this
        .pointerInput(Unit) {
            awaitEachGesture {
                // Нажатие забираем себе: родитель с таким же press его не увидит (кнопка внутри карточки)
                val down = awaitFirstDown(requireUnconsumed = true)
                down.consume()
                touch = down.position
                scope.launch { scale.animateTo(scaleTo, spring(.62f, 520f)) }
                if (glow) scope.launch { glowA.animateTo(1f, tween(140)) }
                var longFired = false
                val longJob = if (long != null) scope.launch {
                    delay(viewConfiguration.longPressTimeoutMillis)
                    longFired = true
                    hf.performHapticFeedback(HapticFeedbackType.LongPress)
                    long?.invoke()
                } else null
                var cancelled = false
                var upPos: Offset? = null
                while (true) {
                    val ev = awaitPointerEvent(PointerEventPass.Final)
                    val ch = ev.changes.firstOrNull { it.id == down.id } ?: run { cancelled = true; null } ?: break
                    // changedToUp() возвращает false, если предок уже забрал событие (clickable шторки) — поэтому IgnoreConsumed
                    if (ch.changedToUpIgnoreConsumed()) { upPos = ch.position; if (!ch.isConsumed) ch.consume(); break }
                    // родитель прокручивает (само нажатие, которое мы же и забрали, не считается — у него previousPressed == false)
                    if (ch.isConsumed && ch.previousPressed) { cancelled = true; break }
                    touch = ch.position
                    val d = ch.position - down.position
                    if (d.getDistance() > viewConfiguration.touchSlop) longJob?.cancel()
                    if (rubber) scope.launch { drag.snapTo(d) }
                }
                longJob?.cancel()
                val inside = upPos != null && upPos.x in -24f..size.width + 24f && upPos.y in -24f..size.height + 24f
                if (!cancelled && !longFired && inside) click()
                scope.launch { scale.animateTo(1f, spring(.48f, 420f)) }
                if (glow) scope.launch { glowA.animateTo(0f, tween(360)) }
                if (rubber) scope.launch { drag.animateTo(Offset.Zero, spring(.55f, 380f)) }
            }
        }
        .graphicsLayer {
            val d = drag.value
            val w = size.width.coerceAtLeast(1f); val h = size.height.coerceAtLeast(1f)
            // резина: тянется к пальцу с затуханием, вдоль движения чуть вытягивается
            val sx = (abs(d.x) / w).coerceAtMost(1f) * .06f
            val sy = (abs(d.y) / h).coerceAtMost(1f) * .06f
            scaleX = scale.value * (1f + sx - sy * .4f)
            scaleY = scale.value * (1f + sy - sx * .4f)
            translationX = rub(d.x, 10.dp.toPx())
            translationY = rub(d.y, 10.dp.toPx())
        }
        .then(
            if (!glow) Modifier else Modifier.drawWithContent {
                drawContent()
                val a = glowA.value
                if (a > 0f) {
                    val r = 85.dp.toPx()
                    val br = Brush.radialGradient(
                        listOf(Color.White.copy(alpha = .30f * a), Color.White.copy(alpha = .12f * a), Color.Transparent),
                        center = touch, radius = r,
                    )
                    val sh = shape
                    if (sh != null) {
                        val p = Path().apply { addOutline(sh.createOutline(size, layoutDirection, this@drawWithContent)) }
                        clipPath(p) { drawCircle(br, r, touch) }
                    } else drawCircle(br, r, touch)
                }
            },
        )
}

/** Резиновое затухание: чем дальше тянешь, тем меньше сдвиг (не больше max). */
private fun rub(x: Float, max: Float): Float {
    val s = if (x < 0) -1f else 1f
    val a = abs(x)
    return s * max * (1f - 1f / (a / max * .55f + 1f))
}

/** Появление блока: снизу вверх с растворением, с небольшой задержкой по номеру. */
@Composable
fun Modifier.appear(i: Int = 0): Modifier {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay((i * 28L).coerceAtMost(220L)); a.animateTo(1f, spring(.82f, 260f)) }
    return this.graphicsLayer {
        val v = a.value
        alpha = v.coerceIn(0f, 1f)
        translationY = (1f - v) * 18.dp.toPx()
    }
}

/* ===================== Формы Material 3 Expressive ===================== */

/** Полярные формы: печенька, мягкие звёзды, клевер, цветок, скруглённый квадрат, эллипс. */
enum class M3Form { Cookie, Star4, Star5, Clover, Flower, Square, Oval, Sun }

fun formRadius(f: M3Form, th: Float): Float = when (f) {
    M3Form.Cookie -> 1f + .07f * cos(9 * th)
    M3Form.Star4 -> 1f + .2f * cos(4 * th)
    M3Form.Star5 -> 1f + .17f * cos(5 * th)
    M3Form.Clover -> 1f + .12f * cos(4 * th)
    M3Form.Flower -> 1f + .13f * cos(6 * th)
    M3Form.Sun -> 1f + .05f * cos(12 * th)
    M3Form.Square -> { val p = 4.5; (1.0 / Math.pow(Math.pow(abs(cos(th).toDouble()), p) + Math.pow(abs(sin(th).toDouble()), p), 1.0 / p)).toFloat() }
    M3Form.Oval -> { val a = 1f; val b = .64f; val t2 = th + .61f; (a * b) / sqrt((b * cos(t2)) * (b * cos(t2)) + (a * sin(t2)) * (a * sin(t2))) }
}

private val maxR = HashMap<M3Form, Float>()
private fun formMax(f: M3Form): Float = maxR.getOrPut(f) { (0 until 360).maxOf { formRadius(f, it * PI.toFloat() / 180f) } }

fun formPath(f: M3Form, size: Size, rot: Float = 0f, path: Path = Path()): Path {
    val c = Offset(size.width / 2, size.height / 2)
    val r0 = minOf(size.width, size.height) / 2 / formMax(f)
    path.reset()
    for (i in 0..180) {
        val th = i * 2f * PI.toFloat() / 180f
        val r = r0 * formRadius(f, th)
        val x = c.x + r * cos(th + rot); val y = c.y + r * sin(th + rot)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

class FormShape(private val f: M3Form) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(formPath(f, size))
}

/** Форма за предметом в Pixel OS — закреплена за эмодзи, чтобы предмет узнавался по форме. */
fun formFor(key: String): M3Form {
    val all = listOf(M3Form.Cookie, M3Form.Star4, M3Form.Square, M3Form.Clover, M3Form.Oval, M3Form.Flower, M3Form.Star5)
    return all[abs(key.hashCode()) % all.size]
}

/* ===================== Загрузка ===================== */

/**
 * Pixel OS: формы перетекают друг в друга (0,8 с на смену) с лёгкой пружиной,
 * каждая следующая — под случайным углом, поворот 75–105°.
 */
@Composable
fun MorphLoader(size: Dp = 56.dp, color: Color = T.acc, container: Color = T.secCont) {
    val forms = remember { listOf(M3Form.Cookie, M3Form.Star4, M3Form.Square, M3Form.Clover, M3Form.Oval, M3Form.Flower, M3Form.Star5) }
    var tSec by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (isActive) { withFrameNanos { tSec = (it - start) / 1e9f } }
    }
    val seed = remember { Random.nextInt(0, 1_000_000) }
    val path = remember { Path() }
    Canvas(Modifier.size(size)) {
        val step = .8f
        val k = (tSec / step).toInt()
        val local = (tSec - k * step).coerceAtMost(.7f) / .7f
        val rnd = Random(seed + k)
        val a = forms[(k + abs(seed)) % forms.size]; val b = forms[(k + 1 + abs(seed)) % forms.size]
        val phA = Random(seed + k).nextFloat() * 6.28f; val phB = Random(seed + k + 1).nextFloat() * 6.28f
        val e = springEase(local * .7f)
        var rot = 0f
        for (j in 0 until k) rot += (75f + Random(seed * 31 + j).nextFloat() * 30f) * PI.toFloat() / 180f
        rot += (75f + rnd.nextFloat() * 30f) * PI.toFloat() / 180f * e
        drawCircle(container)
        val c = Offset(this.size.width / 2, this.size.height / 2)
        val rr = this.size.minDimension * .3f
        path.reset()
        for (i in 0..180) {
            val th = i * 2f * PI.toFloat() / 180f
            val ra = formRadius(a, th - phA) / formMax(a); val rb = formRadius(b, th - phB) / formMax(b)
            val r = rr * (ra + (rb - ra) * e)
            val x = c.x + r * cos(th + rot); val y = c.y + r * sin(th + rot)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, color)
    }
}

/** Пружина с небольшим перелётом (ζ≈0.62): 0→1 примерно за 0.5 с. */
private fun springEase(t: Float): Float {
    if (t <= 0f) return 0f
    val z = .62f; val w = 15f
    val wd = w * sqrt(1 - z * z)
    return 1f - exp(-z * w * t) * (cos(wd * t) + z * w / wd * sin(wd * t))
}

/** Загрузка в стиле: Pixel — перетекающие формы, остальные — тонкое кольцо. */
@Composable
fun Loader(size: Dp = 48.dp) {
    if (T.pixel) { MorphLoader(size); return }
    var tSec by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { val s0 = withFrameNanos { it }; while (isActive) withFrameNanos { tSec = (it - s0) / 1e9f } }
    val c = T.acc
    Canvas(Modifier.size(size)) {
        rotate(tSec * 300f) {
            drawArc(c, 0f, 270f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
    }
}

@Suppress("unused") private val keepEasing = FastOutSlowInEasing

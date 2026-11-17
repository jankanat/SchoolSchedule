package kz.alnazar.schedule.ui

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.collectIsDraggedAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sqrt

/* =====================================================================
   Стеклянное нижнее меню iOS 26: эллипс-капля с физикой пружин Apple,
   преломлением (AGSL-шейдер, Android 13+), RGB-разделением только по краю
   и перекрашиванием кнопок внутри эллипса. Перенесено из HTML-прототипа.
   ===================================================================== */

private const val IND_PAD = 4f        // эллипс шире кнопки на 4dp с каждой стороны
private const val NAV_H = 70f
private const val NAV_PAD = 6f

/** Пружина Apple в терминах SwiftUI: duration (с) и bounce → жёсткость k и коэффициент затухания ζ. */
private fun appleSpring(d: Float, b: Float): Pair<Float, Float> {
    val k = (2 * PI / d).pow(2).toFloat()
    val c = (4 * PI * (1 - b) / d).toFloat()
    return k to c / (2 * sqrt(k))
}
private val SP_MOVE = appleSpring(.5f, .2f)
private val SP_DEF = appleSpring(.45f, .35f)

private fun spring(pos: Float, vel: Float, target: Float, k: Float, z: Float, dt: Float): Pair<Float, Float> {
    val c = 2 * sqrt(k) * z
    val v = vel + (k * (target - pos) - c * vel) * dt
    return (pos + v * dt) to v
}

/** Состояние капли. Все расстояния — в dp, как в прототипе. */
class Drop {
    var x by mutableFloatStateOf(Float.NaN)   // левый край эллипса в покое
    var g by mutableFloatStateOf(0f)          // увеличение 0..1
    var d by mutableFloatStateOf(0f)          // деформация от скорости
    var lens by mutableFloatStateOf(0f)       // сила преломления 0..1
    var vx = 0f; var tx = 0f; var gv = 0f; var gt = 0f; var dv = 0f
    var follow = false; var lastF = 0L; var moving = false; var dir = 0f
    var wake by mutableStateOf(0)

    fun followTo(nx: Float, nowMs: Long) {
        if (x.isNaN()) x = nx
        if (follow) { val dt = (nowMs - lastF).coerceAtLeast(8L) / 1000f; vx = vx * .5f + (nx - x) / dt * .5f } else vx = 0f
        follow = true; lastF = nowMs; x = nx; tx = nx; wake++
    }

    fun to(nx: Float, jump: Boolean = false) {
        if (x.isNaN() || jump) { x = nx; vx = 0f; d = 0f; dv = 0f }
        follow = false; tx = nx; dir = sign(nx - x); moving = abs(nx - x) > .5f; wake++
    }

    fun busy(active: Boolean, lensTarget: Float): Boolean =
        active || abs(tx - x) > .2f || abs(vx) > 3f || abs(g - gt) > .003f || abs(gv) > .02f ||
            abs(d) > .002f || abs(dv) > .02f || abs(lens - lensTarget) > .01f

    fun step(dtIn: Float, nowMs: Long, grabbing: Boolean, active: Boolean) {
        val dt = dtIn.coerceIn(.001f, .032f)
        if (follow) { if (nowMs - lastF > 50) vx *= .002f.pow(dt) }
        else { val (p, v) = spring(x, vx, tx, SP_MOVE.first, SP_MOVE.second, dt); x = p; vx = v }
        // доехал (или впервые пересёк цель на перелёте) — сразу начинаем уменьшаться
        if (moving && !follow && (tx - x) * dir <= 2f) moving = false
        gt = when { grabbing -> 1f; active || moving -> .8f; else -> 0f }
        val up = gt > g
        val (gp, gvv) = spring(g, gv, gt, if (up) 430f else 60f, if (up) .55f else 1f, dt); g = gp; gv = gvv
        val (dp, dvv) = spring(d, dv, (abs(vx) / 2000f).coerceIn(0f, .16f), SP_DEF.first, SP_DEF.second, dt); d = dp; dv = dvv
        val lt = if (active || moving) 1f else 0f
        val tau = if (lt > lens) .04f else .09f
        lens += (lt - lens) * (1f - kotlin.math.exp(-dt / tau))
        if (!busy(false, lt) && !follow) { x = tx; vx = 0f; d = 0f; dv = 0f; g = gt; gv = 0f; lens = lt }
    }
}

/* ===== AGSL: линза капли ===== */
private const val LENS_AGSL = """
uniform shader content;
uniform float2 size;
uniform float radius;
uniform float bezel;
uniform float shift;
uniform float mag;
uniform float ca;
uniform float amount;
half4 main(float2 p) {
  float2 c = size * 0.5;
  float2 d = p - c;
  float2 q = abs(d) - (c - float2(radius));
  float2 qq = max(q, float2(0.0));
  float dist = -(length(qq) + min(max(q.x, q.y), 0.0) - radius);
  float2 s = float2(d.x >= 0.0 ? 1.0 : -1.0, d.y >= 0.0 ? 1.0 : -1.0);
  float2 n = (qq.x > 0.0 || qq.y > 0.0) ? normalize(qq) * s : (q.x > q.y ? float2(s.x, 0.0) : float2(0.0, s.y));
  float t = clamp(1.0 - dist / bezel, 0.0, 1.0);
  float m = t * t * t;
  float2 disp = (-n * m * shift - d / c * mag * shift) * amount;
  float e = t * t * (3.0 - 2.0 * t) * ca * amount;
  half4 g = content.eval(p + disp);
  half r = content.eval(p + disp * (1.0 - e)).r;
  half b = content.eval(p + disp * (1.0 + e)).b;
  return half4(r, g.g, b, g.a);
}
"""

/** Эффект линзы для слоя размера w×h (px). Возвращает null ниже Android 13 или когда линза выключена. */
private class Lens {
    private val shader: Any? = if (Build.VERSION.SDK_INT >= 33) RuntimeShader(LENS_AGSL) else null
    fun effect(w: Float, h: Float, density: Float, amount: Float, shiftDp: Float, ca: Float): androidx.compose.ui.graphics.RenderEffect? {
        if (Build.VERSION.SDK_INT < 33 || amount < .01f || w < 2 || h < 2) return null
        val s = shader as RuntimeShader
        s.setFloatUniform("size", w, h)
        s.setFloatUniform("radius", h / 2f)
        s.setFloatUniform("bezel", h * .3f)
        s.setFloatUniform("shift", shiftDp * density)
        s.setFloatUniform("mag", .03f)
        s.setFloatUniform("ca", ca)
        s.setFloatUniform("amount", amount)
        return RenderEffect.createRuntimeShaderEffect(s, "content").asComposeRenderEffect()
    }
}

data class NavTab(val label: String, val icon: ImageVector)

@Composable
private fun NavButtons(tabs: List<NavTab>, btnW: Dp, color: Color) {
    Row(Modifier.fillMaxSize().padding(NAV_PAD.dp)) {
        tabs.forEach { t ->
            Column(
                Modifier.width(btnW).fillMaxHeight().padding(top = 6.dp, bottom = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.height(28.dp), contentAlignment = Alignment.Center) {
                    Icon(t.icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.height(3.dp))
                // размер подписи не растёт от системного «крупного шрифта», иначе она обрезается снизу
                val fs = LocalDensity.current.fontScale
                Text(
                    t.label, color = color, maxLines = 1, softWrap = false,
                    style = TextStyle(
                        fontSize = (10.5f / fs).sp, lineHeight = (13f / fs).sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1f / fs).sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
                    ),
                )
            }
        }
    }
}

/**
 * @param backdrop слой с содержимым экрана (для размытия и преломления под меню)
 */
@Composable
fun GlassNav(tabs: List<NavTab>, pager: PagerState, backdrop: GraphicsLayer, modifier: Modifier = Modifier) {
    val t = T
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val drop = remember { Drop() }
    var pressed by remember { mutableStateOf(false) }
    var grabbing by remember { mutableStateOf(false) }
    val pagerDragged by pager.interactionSource.collectIsDraggedAsState()
    var navPos by remember { mutableStateOf(Offset.Zero) }
    val navLayer = rememberGraphicsLayer()
    val bgLayer = rememberGraphicsLayer()
    val lensBg = remember { Lens() }
    val lensTint = remember { Lens() }
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val n = tabs.size
    val pill = RoundedCornerShape(999.dp)

    BoxWithConstraints(
        modifier.fillMaxWidth().navigationBarsPadding().padding(start = 14.dp, end = 14.dp, bottom = 14.dp).height(NAV_H.dp)
            .onGloballyPositioned { navPos = it.positionInRoot() },
    ) {
        val navW = maxWidth.value
        val btnW = (navW - 2 * NAV_PAD) / n
        val baseW = btnW + 2 * IND_PAD
        val baseH = NAV_H - 2 * NAV_PAD + 4f
        val top = NAV_PAD - 2f
        fun btnX(i: Int) = NAV_PAD + i * btnW - IND_PAD
        fun xAt(f: Float) = NAV_PAD + f.coerceIn(0f, (n - 1).toFloat()) * btnW - IND_PAD
        if (drop.x.isNaN()) drop.to(btnX(pager.currentPage), jump = true)

        fun select(i: Int) {
            val k = i.coerceIn(0, n - 1)
            drop.to(btnX(k))
            if (k != pager.currentPage || pager.isScrollInProgress) scope.launch { pager.animateScrollToPage(k) }
        }

        // ---- цикл кадров: работает только пока есть движение ----
        LaunchedEffect(navW) {
            if (drop.x.isNaN()) drop.to(btnX(pager.currentPage), jump = true)
            var last = 0L
            while (true) {
                val active = pressed || grabbing || pagerDragged
                val lt = if (active || drop.moving) 1f else 0f
                val wantTarget = btnX(pager.targetPage)
                if (!drop.follow && !grabbing && !pagerDragged && abs(drop.tx - wantTarget) > .5f) drop.to(wantTarget)
                if (!drop.busy(active, lt) && !pagerDragged && !(drop.follow && !grabbing)) {
                    last = 0L
                    // всё спокойно — ждём следующего изменения, не тратя кадры
                    val snap = listOf(pressed, grabbing, pagerDragged, pager.targetPage, drop.wake)
                    snapshotFlow { listOf(pressed, grabbing, pagerDragged, pager.targetPage, drop.wake) }.first { it != snap }
                }
                androidx.compose.runtime.withFrameNanos { ns ->
                    val ms = ns / 1_000_000
                    val dt = if (last == 0L) .016f else (ms - last) / 1000f
                    last = ms
                    if (pagerDragged) drop.followTo(xAt(pager.currentPage + pager.currentPageOffsetFraction), ms)
                    else if (drop.follow && !grabbing) drop.to(btnX(pager.targetPage))
                    drop.step(dt, ms, grabbing, pressed || grabbing || pagerDragged)
                }
            }
        }

        // ---- жесты: тап по кнопке, нажатие, перетаскивание капли ----
        Box(
            Modifier.fillMaxSize().pointerInput(navW) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    pressed = true; drop.wake++
                    val x0 = down.position.x
                    val ex = drop.x
                    val dpx = with(density) { 1.dp.toPx() }
                    val within = x0 / dpx in (ex - 2f)..(ex + baseW + 2f)
                    var dragging = false
                    while (true) {
                        val ev = awaitPointerEvent()
                        val c = ev.changes.firstOrNull { it.id == down.id } ?: break
                        if (!c.pressed) break
                        val dx = c.position.x - x0
                        if (within && !dragging && abs(dx) > touchSlop) { dragging = true; grabbing = true }
                        if (dragging) {
                            val nx = (ex + dx / dpx).coerceIn(btnX(0), btnX(n - 1))
                            drop.followTo(nx, System.nanoTime() / 1_000_000)
                            c.consume()
                        }
                    }
                    pressed = false; grabbing = false; drop.wake++
                    if (dragging) select(((drop.x - btnX(0)) / btnW).roundToInt())
                    else select(((x0 / dpx - NAV_PAD) / btnW).toInt())
                }
            },
        ) {
            // ---- основа меню: размытая подложка + серые кнопки (записывается в слой для линзы) ----
            Box(
                Modifier.fillMaxSize().drawWithContent {
                    navLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(navLayer)
                },
            ) {
                Box(
                    Modifier.fillMaxSize().clip(pill).drawBehind {
                        bgLayer.renderEffect = BlurEffect(4.dp.toPx(), 4.dp.toPx(), TileMode.Clamp)
                        bgLayer.record { translate(-navPos.x, -navPos.y) { drawLayer(backdrop) } }
                        drawLayer(bgLayer)
                        drawRect(t.navBg)
                        drawRect(Brush.verticalGradient(0f to Color(0x4DFFFFFF), .42f to Color.Transparent, .76f to Color.Transparent, 1f to Color(0x1FFFFFFF)))
                    }.border(1.dp, t.navBorder, pill),
                )
                NavButtons(tabs, btnW.dp, t.tx2)
            }

            // ---- капля ----
            Box(
                Modifier
                    .layout { m, _ ->
                        val w = (baseW + 2 * 10f * drop.g) * (1 + drop.d)
                        val h = (baseH + 2 * 7f * drop.g) * (1 - drop.d * .55f)
                        val pw = (w * density.density).roundToInt(); val ph = (h * density.density).roundToInt()
                        val p = m.measure(androidx.compose.ui.unit.Constraints.fixed(pw, ph))
                        layout(pw, ph) { p.place(0, 0) }
                    }
                    .graphicsLayer {
                        val w = (baseW + 2 * 10f * drop.g) * (1 + drop.d)
                        val h = (baseH + 2 * 7f * drop.g) * (1 - drop.d * .55f)
                        translationX = (drop.x - (w - baseW) / 2f) * density.density
                        translationY = (top - (h - baseH) / 2f) * density.density
                    }
                    .clip(pill),
            ) {
                // преломлённая копия того, что под каплей
                Box(
                    Modifier.fillMaxSize()
                        .graphicsLayer {
                            renderEffect = lensBg.effect(size.width, size.height, density.density, drop.lens, 7f, .35f)
                            alpha = if (drop.lens > .01f && Build.VERSION.SDK_INT >= 33) 1f else 0f
                        }
                        .drawBehind {
                            val l = translationXOf(drop, baseW, density.density)
                            val tp = translationYOf(drop, baseH, top, density.density)
                            translate(-l, -tp) { drawLayer(navLayer) }
                        },
                )
                // стекло
                Box(Modifier.fillMaxSize().drawBehind { drawRect(Color(0x14FFFFFF)) })
                // кнопки акцентным цветом — видны только внутри капли
                Box(
                    Modifier.fillMaxSize().graphicsLayer {
                        renderEffect = lensTint.effect(size.width, size.height, density.density, drop.lens, 6f, .25f)
                    },
                ) {
                    Box(
                        Modifier.wrapContentSize(Alignment.TopStart, unbounded = true)
                            .requiredSize(navW.dp, NAV_H.dp)
                            .graphicsLayer {
                                translationX = -translationXOf(drop, baseW, density.density)
                                translationY = -translationYOf(drop, baseH, top, density.density)
                            },
                    ) { NavButtons(tabs, btnW.dp, t.acc) }
                }
                // блики по диагонали 45°: сверху-слева и снизу-справа
                Box(
                    Modifier.fillMaxSize().border(
                        1.2.dp,
                        Brush.linearGradient(
                            0f to Color(0xD9FFFFFF), .14f to Color(0x59FFFFFF), .30f to Color.Transparent,
                            .70f to Color.Transparent, .86f to Color(0x59FFFFFF), 1f to Color(0xD9FFFFFF),
                        ),
                        pill,
                    ),
                )
            }
        }
    }
}

private fun translationXOf(d: Drop, baseW: Float, den: Float): Float {
    val w = (baseW + 2 * 10f * d.g) * (1 + d.d)
    return (d.x - (w - baseW) / 2f) * den
}

private fun translationYOf(d: Drop, baseH: Float, top: Float, den: Float): Float {
    val h = (baseH + 2 * 7f * d.g) * (1 - d.d * .55f)
    return (top - (h - baseH) / 2f) * den
}

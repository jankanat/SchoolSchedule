package kz.alnazar.schedule.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/** Нижнее меню нужного стиля. */
@Composable
fun StyleNav(tabs: List<NavTab>, pager: PagerState, backdrop: GraphicsLayer, modifier: Modifier) {
    when (T.nav) {
        "glass" -> GlassNav(tabs, pager, backdrop, modifier)
        "m3" -> PixelNav(tabs, pager, backdrop, modifier)
        "ios18" -> IosBar(tabs, pager, backdrop, modifier)
        else -> FloatNav(tabs, pager, backdrop, modifier)
    }
}

/** Подложка с размытием того, что под ней (по слою содержимого экрана). */
@Composable
fun Modifier.backdropBlur(backdrop: GraphicsLayer, blur: Dp, tint: Color, shape: Shape): Modifier {
    var pos by remember { mutableStateOf(Offset.Zero) }
    val layer = rememberGraphicsLayer()
    return this.onGloballyPositioned { pos = it.positionInRoot() }.clip(shape).drawBehind {
        if (blur > 0.dp) layer.renderEffect = BlurEffect(blur.toPx(), blur.toPx(), TileMode.Clamp)
        layer.record { translate(-pos.x, -pos.y) { drawLayer(backdrop) } }
        drawLayer(layer)
        drawRect(tint)
    }
}

/** Подпись вкладки, не зависящая от системного «крупного шрифта» (иначе обрезается). */
@Composable
fun NavLabel(text: String, color: Color, size: Float = 11f, bold: Boolean = false) {
    val fs = LocalDensity.current.fontScale
    androidx.compose.material3.Text(
        text, color = color, maxLines = 1, softWrap = false,
        style = TextStyle(
            fontSize = (size / fs).sp, lineHeight = ((size + 3) / fs).sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
        ),
    )
}

private fun Modifier.noRippleClick(onClick: () -> Unit) = this.clickable(MutableInteractionSource(), null, onClick = onClick)

/* ===================== Pixel OS: панель Material 3 Expressive (Android 16–17) ===================== */

/**
 * Современная выразительная панель: 64 dp, полупрозрачная с размытием (Android 17).
 * Активная вкладка — капсула secondaryContainer с иконкой и подписью в одну строку,
 * она «перетекает» по ширине с пружиной; неактивные — только иконки.
 */
@Composable
fun PixelNav(tabs: List<NavTab>, pager: PagerState, backdrop: GraphicsLayer, modifier: Modifier = Modifier) {
    val t = T
    val scope = rememberCoroutineScope()
    val cur = pager.targetPage
    Box(modifier.fillMaxWidth().backdropBlur(backdrop, 18.dp, t.navBg.copy(alpha = .88f), RoundedCornerShape(0.dp))) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().height(64.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        ) {
            tabs.forEachIndexed { i, tab ->
                val on = i == cur
                val bg by animateColorAsState(if (on) t.secCont else Color.Transparent, tween(220), label = "pbg")
                val ic by animateColorAsState(if (on) t.onSecCont else t.tx2, tween(220), label = "pic")
                Row(
                    Modifier.height(44.dp).clip(RoundedCornerShape(99.dp)).background(bg)
                        .noRippleClick { scope.launch { pager.animateScrollToPage(i) } }
                        .animateContentSize(spring(dampingRatio = .62f, stiffness = 380f))
                        .padding(horizontal = if (on) 16.dp else 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(tab.icon, contentDescription = tab.label, tint = ic, modifier = Modifier.size(24.dp))
                    if (on) {
                        Spacer(Modifier.width(8.dp))
                        NavLabel(tab.label, t.onSecCont, 14f, bold = true)
                    }
                }
            }
        }
    }
}

/* ===================== iOS 18: классическая панель вкладок ===================== */

@Composable
fun IosBar(tabs: List<NavTab>, pager: PagerState, backdrop: GraphicsLayer, modifier: Modifier = Modifier) {
    val t = T
    val scope = rememberCoroutineScope()
    val cur = pager.targetPage
    Box(modifier.fillMaxWidth().backdropBlur(backdrop, 22.dp, t.navBg, RoundedCornerShape(0.dp))) {
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(t.navBorder))
        Row(Modifier.fillMaxWidth().navigationBarsPadding().height(54.dp).padding(top = 6.dp)) {
            tabs.forEachIndexed { i, tab ->
                val c by animateColorAsState(if (i == cur) t.acc else t.tx2, tween(160), label = "ios18")
                Column(Modifier.weight(1f).fillMaxHeight().noRippleClick { scope.launch { pager.animateScrollToPage(i) } },
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(tab.icon, contentDescription = null, tint = c, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.height(3.dp))
                    NavLabel(tab.label, c, 10f)
                }
            }
        }
    }
}

/* ===================== ColorOS 17 / OriginOS 7 / One UI 9: плавающие панели ===================== */

/**
 * island (ColorOS 17): матовая капсула-остров, белое контурное свечение, светлая «линза» под активной вкладкой.
 * origin (OriginOS 7): скруглённый прямоугольник, капсула акцентного цвета за иконкой.
 * oneui (One UI 9): плавающая капсула с тонкой белой кромкой, тональная капсула за иконкой.
 * Выделение едет вслед за пальцем при свайпе страниц.
 */
@Composable
fun FloatNav(tabs: List<NavTab>, pager: PagerState, backdrop: GraphicsLayer, modifier: Modifier = Modifier) {
    val t = T
    val scope = rememberCoroutineScope()
    val kind = t.nav
    val radius = when (kind) { "origin" -> 26.dp; else -> 999.dp }
    val shape = RoundedCornerShape(radius)
    val sideMargin = when (kind) { "oneui" -> 22.dp; "origin" -> 12.dp; else -> 16.dp }
    val h = when (kind) { "oneui" -> 62.dp; else -> 64.dp }
    val blur = when (kind) { "island" -> 20.dp; "origin" -> 22.dp; else -> 16.dp }
    BoxWithConstraints(
        modifier.fillMaxWidth().navigationBarsPadding().padding(start = sideMargin, end = sideMargin, bottom = 12.dp).height(h),
    ) {
        val n = tabs.size
        val pad = 6.dp
        val itemW = (maxWidth - pad * 2) / n
        val frac = (pager.currentPage + pager.currentPageOffsetFraction).coerceIn(0f, (n - 1).toFloat())
        // подложка
        Box(
            Modifier.fillMaxSize().backdropBlur(backdrop, blur, t.navBg, shape)
                .border(
                    if (kind == "island") 1.2.dp else 1.dp,
                    if (kind == "island") Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color(0x14FFFFFF))) else Brush.verticalGradient(listOf(t.navBorder, t.navBorder)),
                    shape,
                ),
        )
        // выделение
        when (kind) {
            "island" -> {
                // ColorOS 17: без линзы — мягкий акцентный ореол за активной иконкой и точка под подписью
                val d = 40.dp
                val ix = pad + itemW * frac + (itemW - d) / 2
                Box(
                    Modifier.offset(x = ix, y = 4.dp).size(d).clip(RoundedCornerShape(99.dp))
                        .background(Brush.radialGradient(listOf(t.acc.copy(alpha = .38f), t.acc.copy(alpha = .10f), Color.Transparent)))
                        .border(1.dp, Brush.verticalGradient(listOf(Color(0x4DFFFFFF), Color(0x00FFFFFF))), RoundedCornerShape(99.dp)),
                )
                Box(Modifier.offset(x = pad + itemW * frac + (itemW - 5.dp) / 2, y = h - 9.dp).size(5.dp).clip(RoundedCornerShape(99.dp)).background(t.acc))
            }
            else -> {
                val pw = if (kind == "origin") 50.dp else 56.dp
                val ix = pad + itemW * frac + (itemW - pw) / 2
                Box(
                    Modifier.offset(x = ix, y = 8.dp).width(pw).height(30.dp)
                        .clip(RoundedCornerShape(if (kind == "origin") 14.dp else 99.dp)).background(t.accSoft),
                )
            }
        }
        // кнопки
        Row(Modifier.fillMaxSize().padding(horizontal = pad)) {
            tabs.forEachIndexed { i, tab ->
                val on = i == pager.targetPage
                val c by animateColorAsState(if (on) t.acc else t.tx2, tween(160), label = "fn")
                Column(
                    Modifier.weight(1f).fillMaxHeight().noRippleClick { scope.launch { pager.animateScrollToPage(i) } }
                        .padding(top = if (kind == "island") 12.dp else 11.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(tab.icon, contentDescription = null, tint = c, modifier = Modifier.size(if (kind == "island") 24.dp else 22.dp))
                    Spacer(Modifier.height(if (kind == "island") 5.dp else 6.dp))
                    // подпись ужимается до ширины вкладки, чтобы не вылезать за выделение
                    NavLabel(tab.label, if (on && kind != "island") t.tx else c, if (kind == "island") 10f else 10.5f, bold = on)
                }
            }
        }
    }
}

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

/**
 * Нижнее меню нужного стиля. onPlus — круглая кнопка «+» справа (новая домашка / урок),
 * onSettings — в ColorOS круглая кнопка настроек слева.
 */
@Composable
fun StyleNav(tabs: List<NavTab>, pager: PagerState, backdrop: GraphicsLayer, modifier: Modifier, onPlus: () -> Unit, onSettings: () -> Unit) {
    // «+» нужен только на Главной и Заданиях; на Расписании и Виджетах он уходит (там свои кнопки добавления). У Pixel и One UI он на всех экранах.
    val showPlus = T.nav == "m3" || T.id == "oneui" || pager.targetPage == 0 || pager.targetPage == 2
    when (T.nav) {
        "island" -> LiquidDock(tabs, pager, backdrop, modifier, onPlus, onSettings)
        "glass", "origin" -> {
            // Ширина меню не меняется (иначе каждый кадр пересчитывается стекло и всё лагает): оно только плавно съезжает к центру.
            val shift by animateDpAsState(if (showPlus) 0.dp else 40.dp, spring(.66f, 600f), label = "navShift")
            Box(modifier.fillMaxWidth()) {
                GlassNav(tabs, pager, backdrop, Modifier.padding(end = 80.dp).offset { androidx.compose.ui.unit.IntOffset(shift.roundToPx(), 0) }, accent = if (T.nav == "origin") Color(0xFF4C8DFF) else null)
                SidePop(showPlus, Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 14.dp, bottom = 14.dp)) {
                    SideGlassBtn(Ic.plus, backdrop, Modifier, 70.dp, onPlus)
                }
            }
        }
        "m3" -> PixelNav(tabs, pager, backdrop, modifier, onPlus, showPlus)
        "ios18" -> IosBar(tabs, pager, backdrop, modifier)
        else -> FloatNav(tabs, pager, backdrop, modifier, onPlus, showPlus)
    }
}

/** Боковая кнопка, которая появляется и уходит с масштабом и мягкой пружиной. */
@Composable
fun SidePop(show: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    androidx.compose.animation.AnimatedVisibility(
        show, modifier,
        enter = androidx.compose.animation.scaleIn(spring(.58f, 520f), initialScale = .2f) + androidx.compose.animation.fadeIn(tween(200)),
        exit = androidx.compose.animation.scaleOut(spring(.7f, 760f), targetScale = .2f) + androidx.compose.animation.fadeOut(tween(200)),
    ) { content() }
}

/** Круглая стеклянная кнопка рядом с меню (iOS 26, OriginOS, One UI). */
@Composable
fun SideGlassBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, backdrop: GraphicsLayer, modifier: Modifier, size: Dp, onClick: () -> Unit) {
    val t = T
    val shape = androidx.compose.foundation.shape.CircleShape
    val tint = when (t.id) { "oneui" -> Color(0x2EFFFFFF); "origin" -> Color(0x33FFFFFF); else -> Color(0x40302F3C) }
    Box(modifier.size(size).plusAnchor().graphicsLayer { alpha = if (PlusAnchor.open) 0f else 1f }.press(shape, scaleTo = .9f, onClick = onClick).backdropBlur(backdrop, 14.dp, tint, shape)
        .then(if (t.id == "oneui") Modifier else Modifier.border(1.dp, Brush.linearGradient(listOf(Color(0xB3FFFFFF), Color(0x14FFFFFF), Color(0x80FFFFFF))), shape)),
        contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = t.tx, modifier = Modifier.size(28.dp))
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

/* ===================== Pixel OS: плавающая панель Material 3 Expressive + FAB ===================== */

/**
 * Плавающая капсула: у активной вкладки — светлая пилюля с иконкой и подписью (перетекает по ширине с пружиной),
 * у остальных — только иконки. Справа — коралловая FAB скруглённым квадратом.
 */
@Composable
fun PixelNav(tabs: List<NavTab>, pager: PagerState, backdrop: GraphicsLayer, modifier: Modifier = Modifier, onPlus: () -> Unit, showPlus: Boolean = true) {
    val t = T
    val scope = rememberCoroutineScope()
    val cur = pager.targetPage
    Row(modifier.fillMaxWidth().navigationBarsPadding().padding(start = 12.dp, end = 12.dp, bottom = 18.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f, fill = false).height(68.dp).shadow(10.dp, RoundedCornerShape(34.dp)).clip(RoundedCornerShape(34.dp)).background(Color(0xFF36343B)).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { i, tab ->
                val on = i == cur
                val bg by animateColorAsState(if (on) t.acc else Color.Transparent, tween(220), label = "pbg")
                val ic by animateColorAsState(if (on) t.accTx else t.tx2, tween(220), label = "pic")
                Row(
                    Modifier.height(52.dp).clip(RoundedCornerShape(26.dp)).background(bg)
                        .press(RoundedCornerShape(26.dp)) { scope.launch { pager.animateScrollToPage(i) } }
                        .animateContentSize(spring(dampingRatio = .62f, stiffness = 380f))
                        .padding(horizontal = if (on) 16.dp else 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(tab.icon, contentDescription = tab.label, tint = ic, modifier = Modifier.size(24.dp))
                    if (on) {
                        Spacer(Modifier.width(6.dp))
                        NavLabel(tab.label, t.accTx, 13f, bold = true)
                    }
                }
            }
        }
        SidePop(showPlus) {
            Row {
                Spacer(Modifier.width(10.dp))
                Box(Modifier.size(64.dp).plusAnchor().graphicsLayer { alpha = if (PlusAnchor.open) 0f else 1f }.shadow(10.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp)).background(Color(0xFFFF8A80))
                    .press(RoundedCornerShape(22.dp), scaleTo = .9f, onClick = onPlus), contentAlignment = Alignment.Center) {
                    Icon(Ic.plus, null, tint = Color(0xFF4A0E0B), modifier = Modifier.size(28.dp))
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

/* ===================== OriginOS 7 / One UI 9.5: капсула с вкладками и круглая «+» ===================== */

/**
 * Капсула со стеклом; под активной вкладкой в покое — просто полупрозрачный белый эллипс,
 * он едет вслед за пальцем при свайпе страниц.
 */
@Composable
fun FloatNav(tabs: List<NavTab>, pager: PagerState, backdrop: GraphicsLayer, modifier: Modifier = Modifier, onPlus: () -> Unit, showPlus: Boolean = true) {
    val t = T
    val scope = rememberCoroutineScope()
    val shape = RoundedCornerShape(999.dp)
    val h = 66.dp
    val tint = if (t.id == "oneui") Color(0x2EFFFFFF) else Color(0x38FFFFFF)
    Row(modifier.fillMaxWidth().navigationBarsPadding().padding(start = 14.dp, end = 14.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        BoxWithConstraints(Modifier.weight(1f).height(h).backdropBlur(backdrop, 22.dp, tint, shape)
            .then(if (t.id == "origin") Modifier.border(1.2.dp, Brush.verticalGradient(listOf(Color(0x99FFFFFF), Color(0x1FFFFFFF))), shape) else Modifier)) {
            val n = tabs.size
            val pad = 6.dp
            val itemW = (maxWidth - pad * 2) / n
            val frac = (pager.currentPage + pager.currentPageOffsetFraction).coerceIn(0f, (n - 1).toFloat())
            Box(Modifier.offset(x = pad + itemW * frac - 3.dp, y = pad).width(itemW + 6.dp).height(h - pad * 2).clip(shape).background(Color.White.copy(alpha = .18f)))
            Row(Modifier.fillMaxSize().padding(horizontal = pad)) {
                tabs.forEachIndexed { i, tab ->
                    val on = i == pager.targetPage
                    val acc = if (t.id == "origin") Color(0xFF4C8DFF) else Color.White
                    val c by animateColorAsState(if (on) acc else t.tx.copy(alpha = .75f), tween(160), label = "fn")
                    Column(
                        Modifier.weight(1f).fillMaxHeight().press(shape, scaleTo = .92f) { scope.launch { pager.animateScrollToPage(i) } },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    ) {
                        Icon(tab.icon, contentDescription = null, tint = c, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.height(3.dp))
                        NavLabel(tab.label, c, 10f, bold = true)
                    }
                }
            }
        }
        SidePop(showPlus) {
            Row {
                Spacer(Modifier.width(10.dp))
                SideGlassBtn(Ic.plus, backdrop, Modifier, h, onPlus)
            }
        }
    }
}

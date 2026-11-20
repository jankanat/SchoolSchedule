package kz.alnazar.schedule.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ===== ColorOS 17: компоненты настроек (линейные иконки, строки с подсветкой, поиск) ===== */

/** Левый отступ разделителя: у списка с иконками — от текста, у остальных — от края. */
val LocalRowInset = staticCompositionLocalOf { 18.dp }

/** Строка с подсветкой всей полосы при нажатии (как в системных настройках ColorOS). */
@Composable
fun Modifier.coRow(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val bg by animateColorAsState(if (pressed) Color(0x1FFFFFFF) else Color.Transparent, tween(110), label = "corow")
    return this.background(bg).clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick)
}

object CoIc {
    private fun rr(x: Float, y: Float, w: Float, h: Float, r: Float) =
        "M${x + r} ${y}h${w - 2 * r}a$r $r 0 0 1 $r ${r}v${h - 2 * r}a$r $r 0 0 1 -$r ${r}h-${w - 2 * r}a$r $r 0 0 1 -$r -${r}v-${h - 2 * r}a$r $r 0 0 1 $r -${r}z"

    private fun circ(cx: Float, cy: Float, r: Float) = "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 -${2 * r} 0"

    private fun icon(name: String, vararg paths: String): ImageVector {
        val b = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        paths.forEach { d ->
            b.addPath(
                pathData = addPathNodes(d), fill = null, stroke = SolidColor(Color.White),
                strokeLineWidth = 1.7f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
            )
        }
        return b.build()
    }

    val palette = icon("palette",
        "M12 3.5a8.5 8.5 0 1 0 0 17c1.2 0 1.9-.8 1.9-1.7 0-.5-.2-.9-.5-1.3-.3-.4-.5-.8-.5-1.3 0-1 .8-1.7 1.8-1.7H17a3.5 3.5 0 0 0 3.5-3.5C20.5 6.9 16.7 3.5 12 3.5z",
        circ(7.6f, 11f, .9f), circ(10f, 7.6f, .9f), circ(14.4f, 7.6f, .9f))
    val globe = icon("globe", circ(12f, 12f, 8.5f), "M3.5 12h17", "M12 3.5c2.4 2.4 3.5 5.3 3.5 8.5s-1.1 6.1-3.5 8.5c-2.4-2.4-3.5-5.3-3.5-8.5s1.1-6.1 3.5-8.5z")
    val school = icon("school", "M2.5 9.5 12 5l9.5 4.5L12 14z", "M6.5 11.8V16c0 1.2 2.5 2.7 5.5 2.7s5.5-1.5 5.5-2.7v-4.2", "M21.5 9.5V15")
    val sun = icon("sun", circ(12f, 12f, 3.8f), "M12 3v2M12 19v2M3 12h2M19 12h2M5.6 5.6 7 7M17 17l1.4 1.4M18.4 5.6 17 7M7 17l-1.4 1.4")
    val book = icon("book", "M4.5 5.5a2 2 0 0 1 2-2h13V17h-13a2 2 0 0 0-2 2z", "M4.5 19a2 2 0 0 0 2 2h13v-4", "M9 8h6")
    val cloud = icon("cloud", "M7 18a4.5 4.5 0 0 1-.6-9A6 6 0 0 1 18 9.5a4 4 0 0 1-.5 8.5z")
    val chat = icon("chat", "M4.5 5.5h15v10h-7l-4 3.5v-3.5h-4z", "M8.5 9.5h7")
    val search = icon("search", circ(10.5f, 10.5f, 6.5f), "M15.5 15.5 20 20")
    val dot = icon("dot", circ(12f, 12f, 7f))
    val photo = icon("photo", rr(3.5f, 4.5f, 17f, 15f, 3.5f), circ(9f, 9.5f, 1.6f), "M4 17l5-4.5 4 3.5 3-2.5 4.5 4")
}

/** Линейная иконка строки настроек по эмодзи, на которое она заменяет цветную плитку. */
fun coIconFor(emoji: String): ImageVector = when {
    emoji.startsWith("🧩") -> Ic.wid
    emoji.startsWith("🎨") -> CoIc.palette
    emoji.startsWith("🌐") -> CoIc.globe
    emoji.startsWith("🏫") -> CoIc.school
    emoji.startsWith("🔔") -> Ic.bell
    emoji.startsWith("🏖") -> CoIc.sun
    emoji.startsWith("📚") -> CoIc.book
    emoji.startsWith("📥") -> CoIc.photo
    emoji.startsWith("💾") -> CoIc.cloud
    emoji.startsWith("📝") -> CoIc.chat
    emoji.startsWith("⏰") -> Ic.clock
    else -> CoIc.dot
}

/** Матовое стекло: размывает слой [backdrop] под собой. [origin] — положение слоя на экране. */
@Composable
fun Modifier.frostBlur(backdrop: GraphicsLayer, origin: () -> Offset, blur: Dp, tint: Color, shape: Shape): Modifier {
    var pos by remember { mutableStateOf(Offset.Zero) }
    val layer = rememberGraphicsLayer()
    return this.onGloballyPositioned { pos = it.positionInRoot() }.clip(shape).drawBehind {
        val rel = pos - origin()
        val hw = (size.width / 2f).toInt().coerceAtLeast(1); val hh = (size.height / 2f).toInt().coerceAtLeast(1)
        layer.renderEffect = BlurEffect(blur.toPx() / 2f, blur.toPx() / 2f, TileMode.Clamp)
        layer.record(size = IntSize(hw, hh)) { scale(.5f, Offset.Zero) { translate(-rel.x, -rel.y) { drawLayer(backdrop) } } }
        scale(2f, Offset.Zero) { drawLayer(layer) }
        drawRect(tint)
    }
}

/** Плавающая пилюля «Поиск»: матовое стекло с широкой мягкой кромкой; по нажатию плавно раскрывается в поле ввода. */
@Composable
fun CoSearchPill(
    label: String, open: Boolean, query: String, backdrop: GraphicsLayer, origin: () -> Offset,
    onOpen: () -> Unit, onQuery: (String) -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier,
) {
    val t = T
    val focus = remember { FocusRequester() }
    val p by animateFloatAsState(if (open) 1f else 0f, spring(dampingRatio = .78f, stiffness = 320f), label = "pill")
    val press = remember { MutableInteractionSource() }
    val pressed by press.collectIsPressedAsState()
    val sc by animateFloatAsState(if (pressed && !open) .95f else 1f, spring(.6f, 500f), label = "pillp")
    LaunchedEffect(open) { if (open) { kotlinx.coroutines.delay(120); runCatching { focus.requestFocus() } } }
    val shape = CircleShape
    BoxWithConstraints(modifier.fillMaxWidth().imePadding().padding(bottom = 22.dp), contentAlignment = Alignment.BottomCenter) {
        val closedW = 168.dp
        val w = closedW + (maxWidth - 36.dp - closedW) * p
        Box(
            Modifier.width(w).height(56.dp).graphicsLayer { scaleX = sc; scaleY = sc }
                .shadow(14.dp, shape, ambientColor = Color(0x66000000), spotColor = Color(0x66000000))
                .frostBlur(backdrop, origin, 22.dp, Color(0x6626262A), shape)
                .drawBehind {
                    // две широкие слабые кромки: сверху и снизу, к середине и бокам затухают
                    val o = shape.createOutline(size, layoutDirection, this)
                    drawOutline(o, Brush.verticalGradient(0f to Color(0x4DFFFFFF), .34f to Color(0x06FFFFFF), .66f to Color(0x06FFFFFF), 1f to Color(0x40FFFFFF)), style = Stroke(1.5.dp.toPx()))
                    drawOutline(o, Brush.verticalGradient(0f to Color(0x59FFFFFF), .4f to Color(0x0AFFFFFF), .6f to Color(0x0AFFFFFF), 1f to Color(0x4DFFFFFF)), style = Stroke(.5.dp.toPx()))
                }
                .clickable(interactionSource = press, indication = null, enabled = !open, onClick = onOpen),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(CoIc.search, null, tint = t.tx2, modifier = Modifier.size(22.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    Text(label, color = t.tx2, fontSize = 17.sp, fontWeight = FontWeight.Medium, maxLines = 1,
                        modifier = Modifier.graphicsLayer { alpha = if (query.isEmpty()) 1f else 0f })
                    if (p > .02f) BasicTextField(
                        query, onQuery, singleLine = true,
                        textStyle = TextStyle(color = t.tx, fontSize = 17.sp), cursorBrush = SolidColor(t.acc),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus).graphicsLayer { alpha = p },
                    )
                }
                if (p > .02f) Box(
                    Modifier.size(30.dp).graphicsLayer { alpha = p }
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) { Icon(Ic.close, null, tint = t.tx2, modifier = Modifier.size(18.dp)) }
            }
        }
    }
}

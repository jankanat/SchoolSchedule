package kz.alnazar.schedule.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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

/** Плавающая стеклянная пилюля «Поиск»: по нажатию превращается в поле ввода. */
@Composable
fun CoSearchPill(label: String, open: Boolean, query: String, onOpen: () -> Unit, onQuery: (String) -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val t = T
    val focus = remember { FocusRequester() }
    LaunchedEffect(open) { if (open) runCatching { focus.requestFocus() } }
    val shape = CircleShape
    val w: Dp = if (open) 0.dp else 168.dp
    Box(
        modifier.imePadding().padding(bottom = 22.dp).then(if (open) Modifier.fillMaxWidth().padding(horizontal = 18.dp) else Modifier.width(w))
            .height(54.dp).then(Modifier.vol3d(shape)).background(Color(0xCC1C1C1E), shape)
            .then(if (open) Modifier else Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onOpen)),
        contentAlignment = if (open) Alignment.CenterStart else Alignment.Center,
    ) {
        Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(CoIc.search, null, tint = t.tx2, modifier = Modifier.size(22.dp))
            if (open) {
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text(label, color = t.tx2, fontSize = 17.sp)
                    BasicTextField(
                        query, onQuery, singleLine = true,
                        textStyle = TextStyle(color = t.tx, fontSize = 17.sp), cursorBrush = SolidColor(t.acc),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                }
                Box(Modifier.size(28.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose), contentAlignment = Alignment.Center) {
                    Icon(Ic.close, null, tint = t.tx2, modifier = Modifier.size(18.dp))
                }
            } else Text(label, color = t.tx2, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        }
    }
}

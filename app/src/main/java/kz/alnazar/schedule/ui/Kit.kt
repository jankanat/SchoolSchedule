package kz.alnazar.schedule.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.Icon
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.painterResource
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ===== Базовые поверхности ===== */

/** Стеклянная поверхность iOS 26: полупрозрачная заливка + светлая кромка сверху. */
@Composable
fun Modifier.glass(shape: Shape, color: Color = T.s1): Modifier {
    val t = T
    return this.clip(shape).background(color, shape)
        .border(1.dp, Brush.verticalGradient(listOf(t.cardBorderTop, t.cardBorderBottom)), shape)
}

private val noRipple = MutableInteractionSource()

@Composable
fun Modifier.tap(enabled: Boolean = true, onClick: () -> Unit): Modifier =
    this.clickable(enabled = enabled, onClick = onClick)

/* ===== Заголовки ===== */

@Composable
fun Hdr(title: String, sub: String? = null, right: (@Composable RowScope.() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = T.tx, fontSize = T.titleSize.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp, lineHeight = (T.titleSize * 1.1f).sp)
            if (sub != null) Text(sub, color = T.tx2, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 4.dp))
        }
        if (right != null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = right)
    }
}

@Composable
fun HdrBack(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(T.s1).tap(onClick = onBack), contentAlignment = Alignment.Center) {
            Text("‹", color = T.tx, fontSize = 26.sp, fontWeight = FontWeight.Medium, modifier = Modifier.offset(y = (-2).dp))
        }
        Text(title, color = T.tx, fontSize = T.titleSize.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}

@Composable
fun Sec(text: String) {
    Text(text.uppercase(), color = T.tx2, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = .2.sp,
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 20.dp, bottom = 8.dp))
}

@Composable
fun Lab(text: String) {
    Text(text, color = T.tx2, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 6.dp))
}

@Composable
fun Empty(text: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 10.dp), contentAlignment = Alignment.Center) {
        Text(text, color = T.tx2, fontSize = 14.sp)
    }
}

/* ===== Карточки и строки ===== */

@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().glass(RoundedCornerShape(T.rc)).padding(14.dp), content = content)
}

@Composable
fun Group(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(T.rc)), content = content)
}

@Composable
fun RowDivider() {
    Box(Modifier.padding(start = 14.dp).fillMaxWidth().height(1.dp).background(T.line))
}

/** Строка настроек: цветная плитка, заголовок, подзаголовок, стрелка. */
@Composable
fun SetRow(emoji: String, color: Long, title: String, sub: String, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 54.dp).tap(enabled, onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Color(color).copy(alpha = .26f)), contentAlignment = Alignment.Center) {
            Text(emoji, fontSize = 18.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = if (enabled) T.tx else T.tx2, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold)
            if (sub.isNotEmpty()) Text(sub, color = T.tx2, fontSize = 12.5.sp, modifier = Modifier.padding(top = 1.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (enabled) Text("›", color = T.tx2, fontSize = 22.sp)
    }
}

/** Строка с переключателем. */
@Composable
fun TgRow(title: String, on: Boolean, sub: String? = null, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 54.dp).tap { onChange(!on) }.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = T.tx, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold)
            if (sub != null) Text(sub, color = T.tx2, fontSize = 12.5.sp, modifier = Modifier.padding(top = 1.dp))
        }
        IosSwitch(on)
    }
}

@Composable
fun IosSwitch(on: Boolean) {
    val bg by animateColorAsState(if (on) T.swOn else T.s2, tween(200), label = "sw")
    val x by animateDpAsState(if (on) 22.dp else 2.dp, tween(200), label = "swx")
    Box(Modifier.size(51.dp, 31.dp).clip(RoundedCornerShape(99.dp)).background(bg)) {
        Box(Modifier.offset(x = x, y = 2.dp).size(27.dp).shadow(2.dp, CircleShape).background(Color.White, CircleShape))
    }
}

/* ===== Сегменты и чипы: выделение плавно переезжает к нажатому варианту ===== */

private val SLIDE = spring<Dp>(dampingRatio = .82f, stiffness = 520f)

@Composable
fun <K> Seg(options: List<Pair<K, String>>, selected: K, onSelect: (K) -> Unit) {
    val idx = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    BoxWithConstraints(Modifier.fillMaxWidth().clip(RoundedCornerShape(T.rsg)).background(T.s2).padding(3.dp)) {
        val gap = 2.dp
        val w = (maxWidth - gap * (options.size - 1)) / options.size
        val x by animateDpAsState((w + gap) * idx, SLIDE, label = "seg")
        Box(Modifier.matchParentSize()) {
            Box(Modifier.offset(x = x).width(w).fillMaxHeight().shadow(1.dp, RoundedCornerShape(T.rsg - 3.dp)).clip(RoundedCornerShape(T.rsg - 3.dp)).background(T.segOn))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            options.forEachIndexed { i, (k, label) ->
                val c by animateColorAsState(if (i == idx) T.tx else T.tx2, tween(200), label = "segc")
                Box(
                    Modifier.width(w).clip(RoundedCornerShape(T.rsg - 3.dp)).tap { onSelect(k) }.padding(vertical = 8.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(label, color = c, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
    }
}

/** Ряд одинаковых кнопок (дни недели) с переезжающей подсветкой. */
@Composable
fun <K> EqualPills(options: List<Pair<K, String>>, selected: K, dots: Set<K> = emptySet(), onSelect: (K) -> Unit) {
    val idx = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val gap = 6.dp
        val w = (maxWidth - gap * (options.size - 1)) / options.size
        val x by animateDpAsState((w + gap) * idx, SLIDE, label = "pills")
        val shape = RoundedCornerShape(14.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) { options.forEach { _ -> Box(Modifier.width(w).height(38.dp).clip(shape).background(T.s1)) } }
        Box(Modifier.offset(x = x).width(w).height(38.dp).clip(shape).background(T.acc))
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            options.forEachIndexed { i, (k, label) ->
                val c by animateColorAsState(if (i == idx) T.accTx else T.tx2, tween(200), label = "pc")
                Box(Modifier.width(w).height(38.dp).clip(shape).tap { onSelect(k) }, contentAlignment = Alignment.Center) {
                    Text(label, color = c, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                    if (k in dots) Box(Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 6.dp).size(6.dp).background(Color(0xFFFF9F0A), CircleShape))
                }
            }
        }
    }
}

/** Чипы разной ширины: подсветка переезжает и меняет ширину под выбранный чип. */
@Composable
fun <K> Chips(options: List<Pair<K, String>>, selected: K?, onSelect: (K) -> Unit) {
    val idx = options.indexOfFirst { it.first == selected }
    val pos = remember(options.size) { mutableStateListOf(*Array(options.size) { 0f to 0f }) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val sel = pos.getOrNull(idx)
    var chipH by remember { mutableStateOf(0) }
    val x by animateDpAsState(with(density) { (sel?.first ?: 0f).toDp() }, SLIDE, label = "chx")
    val w by animateDpAsState(with(density) { (sel?.second ?: 0f).toDp() }, SLIDE, label = "chw")
    Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 2.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEachIndexed { i, (_, label) ->
                Box(
                    Modifier.onGloballyPositioned { c -> if (i < pos.size) pos[i] = c.positionInParent().x to c.size.width.toFloat(); chipH = c.size.height }
                        .clip(RoundedCornerShape(99.dp)).background(T.s2).padding(vertical = 8.dp, horizontal = 14.dp),
                ) { Text(label, color = Color.Transparent, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) }
            }
        }
        if (idx >= 0 && sel != null && sel.second > 0f) Box(Modifier.offset(x = x).width(w).height(with(density) { chipH.toDp() }).clip(RoundedCornerShape(99.dp)).background(T.acc))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEachIndexed { i, (k, label) ->
                val c by animateColorAsState(if (i == idx) T.accTx else T.tx, tween(200), label = "chc")
                Box(Modifier.clip(RoundedCornerShape(99.dp)).tap { onSelect(k) }.padding(vertical = 8.dp, horizontal = 14.dp)) {
                    Text(label, color = c, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun Chip(label: String, on: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (on) T.acc else T.s2, tween(200), label = "chip")
    Box(
        Modifier.clip(RoundedCornerShape(99.dp)).background(bg).tap(onClick = onClick).padding(vertical = 8.dp, horizontal = 14.dp),
    ) { Text(label, color = if (on) T.accTx else T.tx, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) }
}

/* ===== Кнопки ===== */

enum class BtnKind { Primary, Sec, Ghost, Danger }

@Composable
fun Btn(text: String, modifier: Modifier = Modifier, kind: BtnKind = BtnKind.Primary, small: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val (bg, fg) = when (kind) {
        BtnKind.Primary -> T.acc to T.accTx
        BtnKind.Sec -> T.accSoft to T.acc
        BtnKind.Ghost -> T.s2 to T.tx
        BtnKind.Danger -> Color(0x2EFF453A) to Color(0xFFFF453A)
    }
    Box(
        modifier.clip(RoundedCornerShape(99.dp)).background(if (enabled) bg else T.s2).tap(enabled, onClick)
            .padding(vertical = if (small) 8.dp else 12.dp, horizontal = if (small) 14.dp else 18.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = if (enabled) fg else T.tx2, fontSize = if (small) 13.5.sp else 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) }
}

/* ===== Плитка предмета и строка урока ===== */

/** Настройки иконок предметов (набор и цвет монохромных). */
data class IconPrefs(val set: String = "android", val tint: String = "subject")
val LocalIcons = staticCompositionLocalOf { IconPrefs() }

/** Плитка предмета: системный эмодзи, Fluent 3D, Twemoji или монохромная иконка — по настройке. */
@Composable
fun Tile(emoji: String, color: Long, size: Dp = 40.dp, onAccent: Boolean = false) {
    val p = LocalIcons.current
    val t = T
    val key = IconRes.key(emoji)
    val mono = p.set == "mono" && key != null
    val bg = when {
        onAccent -> Color.White.copy(alpha = .22f)
        mono && p.tint == "accent" -> t.accSoft
        mono && p.tint == "bw" -> t.s2
        else -> Color(color).copy(alpha = .24f)
    }
    Box(Modifier.size(size).clip(RoundedCornerShape(size * t.iconRadius)).background(bg), contentAlignment = Alignment.Center) {
        val res = when {
            key == null -> null
            p.set == "fluent" -> IconRes.fluent[key]
            p.set == "twemoji" -> IconRes.twemoji[key]
            p.set == "mono" -> IconRes.mono[key]
            else -> null
        }
        when {
            res != null && mono -> Icon(
                painterResource(res), contentDescription = null, modifier = Modifier.size(size * .6f),
                tint = when { onAccent -> Color.White; p.tint == "accent" -> t.acc; p.tint == "bw" -> t.tx; else -> Color(color) },
            )
            res != null -> Image(painterResource(res), contentDescription = null, modifier = Modifier.size(size * if (p.set == "fluent") .7f else .6f))
            else -> Text(emoji, fontSize = (size.value * .55f).sp)
        }
    }
}

enum class LsState { Normal, Cur, Past }

/** Строка урока (.ls из прототипа). */
@Composable
fun LessonRow(
    num: Int, emoji: String, color: Long, name: String, sub: String,
    start: String, end: String, state: LsState = LsState.Normal,
    tag: String? = null, was: String? = null, onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(T.rc * .75f)
    var m = Modifier.padding(bottom = 8.dp).fillMaxWidth()
    m = if (state == LsState.Cur) m.clip(shape).background(T.accSoft, shape).border(1.5.dp, T.acc, shape) else m.glass(shape)
    if (onClick != null) m = m.tap(onClick = onClick)
    val a = if (state == LsState.Past) .5f else 1f
    Row(m.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
        Text("$num", color = T.tx2.copy(alpha = a), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(16.dp))
        Box(Modifier.graphicsAlpha(a)) { Tile(emoji, color, 40.dp) }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(name, color = T.tx.copy(alpha = a), fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (tag != null) Text(tag, color = Color(0xFFFF9F0A), fontSize = 10.5.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(Color(0x3DFF9F0A)).padding(horizontal = 7.dp, vertical = 2.dp))
            }
            if (sub.isNotEmpty() || was != null) Row {
                if (was != null) Text("$was · ", color = T.tx2.copy(alpha = a * .7f), fontSize = 12.5.sp, textDecoration = TextDecoration.LineThrough, maxLines = 1)
                Text(sub, color = T.tx2.copy(alpha = a), fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(start, color = T.tx.copy(alpha = a), fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            Text(end, color = T.tx2.copy(alpha = a), fontSize = 12.5.sp)
        }
    }
}

fun Modifier.graphicsAlpha(a: Float): Modifier = this.alpha(a)

/* ===== Поле ввода ===== */

@Composable
fun Field(value: String, onChange: (String) -> Unit, placeholder: String = "", number: Boolean = false, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val t = T
    BasicTextField(
        value = value, onValueChange = onChange, singleLine = true,
        textStyle = TextStyle(color = t.tx, fontSize = 15.sp),
        cursorBrush = SolidColor(t.acc),
        keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
        modifier = modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
        decorationBox = { inner ->
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(t.s2)
                    .border(1.5.dp, if (focused) t.acc else Color.Transparent, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                if (value.isEmpty()) Text(placeholder, color = t.tx2, fontSize = 15.sp)
                inner()
            }
        },
    )
}

/* ===== Нижняя шторка ===== */

@Composable
fun BoxScope.SheetHost(visible: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    AnimatedVisibility(visible, enter = fadeIn(tween(180)), exit = fadeOut(tween(180)), modifier = Modifier.matchParentSize()) {
        Box(Modifier.fillMaxSize().background(Color(0x80000000)).clickable(interactionSource = noRipple, indication = null, onClick = onDismiss))
    }
    AnimatedVisibility(
        visible,
        enter = slideInVertically(tween(240)) { it / 3 } + fadeIn(tween(200)),
        exit = slideOutVertically(tween(200)) { it / 3 } + fadeOut(tween(160)),
        modifier = Modifier.align(Alignment.BottomCenter),
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 640.dp)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(T.sheet)
                .clickable(interactionSource = noRipple, indication = null) {}
                .navigationBarsPadding().imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 26.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).padding(bottom = 12.dp).size(38.dp, 5.dp).clip(RoundedCornerShape(9.dp)).background(Color(0x40FFFFFF)))
            content()
        }
    }
}

@Composable
fun SheetTitle(text: String) {
    Text(text, color = T.tx, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 8.dp))
}

@Composable
fun Btns(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

@Composable
fun VSpace(h: Dp) = Spacer(Modifier.size(width = 0.dp, height = h))

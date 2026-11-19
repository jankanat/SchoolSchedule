package kz.alnazar.schedule.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.Image
import kz.alnazar.schedule.ui.IconRes
import kz.alnazar.schedule.data.Files
import androidx.glance.layout.ContentScale
import kz.alnazar.schedule.ui.withCustom
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kz.alnazar.schedule.R
import kz.alnazar.schedule.data.AppState
import kz.alnazar.schedule.data.Store
import kz.alnazar.schedule.logic.Now
import kz.alnazar.schedule.logic.Phase
import kz.alnazar.schedule.logic.Sched
import kz.alnazar.schedule.logic.Slot
import kz.alnazar.schedule.logic.Tr
import kz.alnazar.schedule.ui.MainActivity

/* ===== Цвета стиля iOS 26 (тёмная тема). Остальные стили и темы — на следующем этапе. ===== */
/** Палитра виджета для текущего стиля приложения. */
internal data class WPal(
    val upper: Boolean, val tileR: Float, val tx: Color, val tx2: Color, val tx3: Color, val acc: Color, val soft: Color,
    val colBg: Color, val line: Color, val tileBw: Color, val bg: Int, val radius: Dp,
    val fill: Color? = null,   // своя тема: сплошная заливка вместо картинки-фона
    val ff: androidx.glance.text.FontFamily? = null,   // ближайший системный шрифт к выбранному в приложении
    val id: String = "",   // стиль: у каждого ОС своя раскладка виджетов
)

/** Своя тема для виджета: форма от стиля, цвета — из настроек пользователя. */
private fun customPal(base: WPal, s: kz.alnazar.schedule.data.AppState): WPal {
    val t = kz.alnazar.schedule.ui.tokFor(s.style).withCustom(s.ct)
    val c = s.ct
    val dark = c.bl < .55f
    val fill = kz.alnazar.schedule.ui.hsl(c.bh, c.bs, c.bl + (if (dark) .07f else -.03f), (c.alpha + .2f).coerceAtMost(1f))
    return base.copy(
        tx = t.tx, tx2 = t.tx.copy(alpha = .62f), tx3 = t.tx.copy(alpha = .36f), acc = t.acc,
        soft = t.acc.copy(alpha = .2f), colBg = t.tx.copy(alpha = .07f), line = t.tx.copy(alpha = .12f),
        tileBw = t.tx.copy(alpha = .16f), fill = fill,
    )
}
private fun pal(upper: Boolean, tileR: Float, acc: Color, soft: Color, bg: Int, r: Int, tx: Color = Color.White, tx2: Color = Color(0xFF9B9BA3)) =
    WPal(upper, tileR, tx, tx2, tx2.copy(alpha = .55f), acc, soft, Color(0x12FFFFFF), Color(0x1FFFFFFF), Color(0xFF2C2C2E), bg, r.dp)
private val IOS = WPal(true, .3f, Color(0xFFFFFFFF), Color(0x99FFFFFF), Color(0x59FFFFFF), Color(0xFF0A84FF), Color(0x2B0A84FF),
    Color(0x12FFFFFF), Color(0x1FFFFFFF), Color(0x42828296), R.drawable.glass_dark, 34.dp, id = "ios26")
private val PIXEL = WPal(false, .5f, Color(0xFFE6E0E9), Color(0xFFCAC4D0), Color(0x80CAC4D0), Color(0xFFD0BCFF), Color(0xFF4A4458),
    Color(0xFF36343B), Color(0x29CAC4D0), Color(0xFF36343B), R.drawable.pixel_dark, 28.dp, id = "pixel")
private val IOS18 = pal(true, .24f, Color(0xFF0A84FF), Color(0x2B0A84FF), R.drawable.ios18_dark, 24).copy(id = "ios18")
private val COLOR = pal(false, .5f, Color(0xFF35D07F), Color(0x2B35D07F), R.drawable.color_dark, 22).copy(id = "color")
private val ORIGIN = pal(false, .34f, Color(0xFFFFB36B), Color(0x2EFFB36B), R.drawable.origin_dark, 28).copy(id = "origin")
private val ONEUI = pal(false, .36f, Color(0xFF4DA3FF), Color(0x2B4DA3FF), R.drawable.oneui_dark, 30).copy(id = "oneui")
private fun palFor(style: String) = when (style) {
    "pixel" -> PIXEL; "ios18" -> IOS18; "color" -> COLOR; "origin" -> ORIGIN; "oneui" -> ONEUI; else -> IOS
}
internal val LocalWP = staticCompositionLocalOf { IOS }

private val TX: ColorProvider @Composable get() = ColorProvider(LocalWP.current.tx)
private val TX2: ColorProvider @Composable get() = ColorProvider(LocalWP.current.tx2)
private val TX3: ColorProvider @Composable get() = ColorProvider(LocalWP.current.tx3)
private val ACC: ColorProvider @Composable get() = ColorProvider(LocalWP.current.acc)
/** Цвет подписи-метки: у iOS она красная, как в виджетах Календаря. */
private val LBC: ColorProvider @Composable get() = ColorProvider(if (LocalWP.current.id.startsWith("ios")) Color(0xFFFF6961) else LocalWP.current.acc)
/** Тёмный текст на акцентной заливке. */
private val INK: ColorProvider @Composable get() = ColorProvider(androidx.compose.ui.graphics.lerp(LocalWP.current.acc, Color.Black, .8f))
private val sid: String @Composable get() = LocalWP.current.id
private fun subjColor(s: AppState, x: Slot, lang: String) = Color(Sched.subj(s, x.lesson.subj, lang).color)

@Composable
private fun st(size: TextUnit, color: ColorProvider = TX, bold: Boolean = false, center: Boolean = false, fam: androidx.glance.text.FontFamily? = null) =
    TextStyle(color = color, fontSize = size, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, fontFamily = fam ?: LocalWP.current.ff,
        textAlign = if (center) androidx.glance.text.TextAlign.Center else androidx.glance.text.TextAlign.Start)

/** Тонкие крупные цифры (OriginOS, число дня в iOS). */
private val LIGHT = androidx.glance.text.FontFamily("sans-serif-light")

/** Общая основа всех виджетов: стекло, клик открывает приложение, пересчёт «сейчас» при каждом тике. */
abstract class LessonWidget : GlanceAppWidget() {
    // точный размер нужен, чтобы полоса прогресса, кольцо и «облачко» были ровно по ширине виджета
    override val sizeMode = SizeMode.Exact

    @Composable
    abstract fun Body(s: AppState, n: Now, lang: String)
    open val padH: Dp = 14.dp
    open val padV: Dp = 14.dp
    /** Маленькие виджеты Pixel — капсулой. */
    open val pill: Boolean = false

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Store.ensure(context)
        provideContent {
            val s by Store.state.collectAsState()
            val tick by Store.tick.collectAsState()
            val n = remember(s, tick) { Sched.now(s) }
            val pal = palFor(s.style).let { if (s.theme == "custom") customPal(it, s) else it }.copy(
                ff = when (s.font) {
                    "serif" -> androidx.glance.text.FontFamily.Serif; "mono" -> androidx.glance.text.FontFamily.Monospace
                    "hand" -> androidx.glance.text.FontFamily.Cursive; else -> null
                },
            )
            val sz = androidx.glance.LocalSize.current
            val inner = androidx.compose.ui.unit.DpSize(sz.width - padH * 2, sz.height - padV * 2)
            val rad = if (pill && pal.id == "pixel") minOf(sz.width, sz.height) / 2 else pal.radius
            // своя тема даёт сплошную заливку — кромку и блик кладём сверху отдельным слоем (у ColorOS, iOS 26 и OriginOS)
            val rim = if (pal.fill != null) when (pal.id) { "color" -> R.drawable.rim_color; "ios26" -> R.drawable.rim_ios; "origin" -> R.drawable.rim_origin; else -> null } else null
            CompositionLocalProvider(LocalWP provides pal, LocalInner provides inner) {
                Box(
                    GlanceModifier.fillMaxSize()
                        .let { m -> pal.fill?.let { m.background(it) } ?: m.background(ImageProvider(pal.bg)) }
                        .cornerRadius(rad)
                        .clickable(actionStartActivity<MainActivity>()),
                ) {
                    Box(GlanceModifier.fillMaxSize().let { if (rim != null) it.background(ImageProvider(rim)) else it }.padding(horizontal = padH, vertical = padV)) {
                        Body(s, n, s.widgetLang)
                    }
                }
            }
        }
    }
}

/** Размер содержимого виджета без отступов. */
internal val LocalInner = staticCompositionLocalOf { androidx.compose.ui.unit.DpSize(150.dp, 150.dp) }

/* ===== Общие кусочки (разметка 1:1 с прототипом: .lb, .nm, .mt, .ic) ===== */

private val LINE: ColorProvider @Composable get() = ColorProvider(LocalWP.current.line)
private val SOFT: Color @Composable get() = LocalWP.current.soft
private val COLBG: Color @Composable get() = LocalWP.current.colBg

@Composable
private fun TileE(s: AppState, emoji: String, color: Long, size: Dp) {
    // своя картинка или скачанный эмодзи — битмапом
    val bmp = if (emoji.startsWith("img:")) Files.iconBitmap(emoji.removePrefix("img:"))
        else if (IconRes.key(emoji) == null) Files.emojiFile(s.iconSet, emoji)?.let { Files.bitmap(it) } else null
    if (bmp != null) {
        val full = emoji.startsWith("img:")
        Box(GlanceModifier.size(size).background(Color(color).copy(alpha = .24f)).cornerRadius(size * LocalWP.current.tileR), contentAlignment = Alignment.Center) {
            Image(ImageProvider(bmp), contentDescription = null, contentScale = if (full) ContentScale.Crop else ContentScale.Fit,
                modifier = if (full) GlanceModifier.size(size).cornerRadius(size * LocalWP.current.tileR) else GlanceModifier.size(size * if (s.iconSet == "fluent") .7f else .6f))
        }
        return
    }
    val key = IconRes.key(emoji)
    val mono = s.iconSet == "mono" && key != null
    val bg = when {
        mono && s.iconTint == "accent" -> SOFT
        mono && s.iconTint == "bw" -> LocalWP.current.tileBw
        else -> Color(color).copy(alpha = .24f)
    }
    val res = when {
        key == null -> null
        s.iconSet == "fluent" -> IconRes.fluent[key]
        s.iconSet == "twemoji" -> IconRes.twemoji[key]
        s.iconSet == "mono" -> IconRes.mono[key]
        else -> null
    }
    Box(GlanceModifier.size(size).background(bg).cornerRadius(size * LocalWP.current.tileR), contentAlignment = Alignment.Center) {
        when {
            res != null && mono -> Image(
                ImageProvider(res), contentDescription = null, modifier = GlanceModifier.size(size * .6f),
                colorFilter = ColorFilter.tint(ColorProvider(when (s.iconTint) { "accent" -> LocalWP.current.acc; "bw" -> LocalWP.current.tx; else -> Color(color) })),
            )
            res != null -> Image(ImageProvider(res), contentDescription = null, modifier = GlanceModifier.size(size * if (s.iconSet == "fluent") .7f else .6f))
            else -> Text(emoji, style = TextStyle(fontSize = (size.value * .55f).sp))
        }
    }
}

@Composable
private fun TileS(s: AppState, slot: Slot, lang: String, size: Dp) {
    val v = Sched.subj(s, slot.lesson.subj, lang); TileE(s, v.emoji, v.color, size)
}

private fun name(s: AppState, slot: Slot, lang: String) = Sched.subj(s, slot.lesson.subj, lang).name + Sched.hwMark(s, slot)

/** Подпись-метка: слева акцентом капсом, справа серым. */
@Composable
private fun Lb(a: String, b: String = "", size: TextUnit = 10.sp, center: Boolean = false) {
    Row(GlanceModifier.fillMaxWidth(), horizontalAlignment = if (center) Alignment.CenterHorizontally else Alignment.Start, verticalAlignment = Alignment.Bottom) {
        Text(if (LocalWP.current.upper) a.uppercase() else a, style = st(size, LBC, bold = true), maxLines = 2, modifier = if (b.isNotEmpty()) GlanceModifier.defaultWeight() else GlanceModifier)
        if (b.isNotEmpty()) Text(b, style = st(size, TX2), maxLines = 1)
    }
}

@Composable
private fun Mt(text: String, size: TextUnit = 10.5.sp, lines: Int = 1) {
    if (text.isNotEmpty()) Text(text, style = st(size, TX2), maxLines = lines)
}

@Composable
private fun Big(em: String, title: String) {
    Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
        Text(em, style = TextStyle(fontSize = 30.sp))
        Text(title, style = st(15.sp, bold = true), maxLines = 2)
    }
}

@Composable
private fun Hd(b: String, s: String) {
    Row(GlanceModifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.Bottom) {
        Text(b, style = st(15.sp, bold = true), maxLines = 1, modifier = GlanceModifier.defaultWeight())
        if (s.isNotEmpty()) Text(s, style = st(10.5.sp, TX2), maxLines = 1)
    }
}

/** до какого времени идёт урок / когда начнётся следующий (вместо обратного отсчёта — виджет обновляется по звонкам) */
private fun tillCur(n: Now, lang: String) = Tr.t(lang, "till", Tr.hm(n.today[n.cur].end))
private fun atNext(n: Now) = Tr.hm(n.today[n.next].start)

private fun live(n: Now) = n.phase == Phase.LESSON || n.phase == Phase.BREAK || n.phase == Phase.BEFORE

/* ===== Общее для раскладок ===== */

/** Раскладка виджета по стилю: у каждой ОС своя. */
private val kind: String @Composable get() = when (LocalWP.current.id) {
    "ios26", "ios18" -> "ios"; "pixel" -> "pixel"; "origin" -> "origin"; "oneui" -> "oneui"; else -> "color"
}
private fun mn(lang: String) = if (lang == "en") "min" else "мин"
private fun schedWord(lang: String) = when (lang) { "kk" -> "Кесте"; "en" -> "Schedule"; else -> "Расписание" }
private fun roomRaw(x: Slot) = x.lesson.room.trim()
private fun span(x: Slot) = Tr.hm(x.start) + "–" + Tr.hm(x.end)
private fun join(vararg p: String) = p.filter { it.isNotBlank() }.joinToString(" · ")
/** Сколько минут до начала (если урок ещё не начался) или до конца. */
private fun minsTo(x: Slot, n: Now) = if (n.minute < x.start) x.start - n.minute else (x.end - n.minute).coerceAtLeast(0)
private fun fracOf(x: Slot, n: Now) =
    if (n.minute < x.start) 0f else ((n.minute - x.start).toFloat() / (x.end - x.start).coerceAtLeast(1)).coerceIn(0f, 1f)

/** Главное в больших виджетах: идущий урок или ближайший. */
private class Fx(val pos: Int, val label: String, val left: String)
private fun focusOf(n: Now, lang: String) =
    if (n.phase == Phase.LESSON) Fx(n.cur, Tr.t(lang, "now"), Tr.t(lang, "left", n.minutesLeft))
    else Fx(n.next, Tr.t(lang, if (n.phase == Phase.BEFORE) "soon" else "break"), Tr.t(lang, "startsIn", n.minutesLeft))

private fun roomOf(s: AppState, L: List<Slot>, i: Int, lang: String) = if (s.widget.room) Sched.roomLabel(L, i, lang) else ""

/** Полоса прогресса: дорожка и заливка — два скруглённых блока точной ширины. */
@Composable
private fun Bar(frac: Float, color: Color, width: Dp, h: Dp = 6.dp) {
    Box(GlanceModifier.width(width).height(h).background(LocalWP.current.line).cornerRadius(h / 2)) {
        if (frac > 0f) Box(GlanceModifier.width(maxOf(h, width * frac.coerceIn(0f, 1f))).height(h).background(color).cornerRadius(h / 2)) {}
    }
}

@Composable
private fun density() = androidx.glance.LocalContext.current.resources.displayMetrics.density

/** Кольцо прогресса (iOS) — картинкой: Glance рисовать дуги не умеет. */
private fun ringBmp(px: Int, frac: Float, fg: Int, track: Int, sw: Float): android.graphics.Bitmap {
    val b = android.graphics.Bitmap.createBitmap(px, px, android.graphics.Bitmap.Config.ARGB_8888)
    val c = android.graphics.Canvas(b)
    val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.STROKE; strokeWidth = sw; strokeCap = android.graphics.Paint.Cap.ROUND
    }
    val r = android.graphics.RectF(sw / 2, sw / 2, px - sw / 2, px - sw / 2)
    p.color = track; c.drawArc(r, 0f, 360f, false, p)
    if (frac > 0f) { p.color = fg; c.drawArc(r, -90f, 360f * frac.coerceIn(0.02f, 1f), false, p) }
    return b
}

/** «Облачко» Material 3 Expressive (Pixel). */
private fun cookieBmp(px: Int, color: Int): android.graphics.Bitmap {
    val b = android.graphics.Bitmap.createBitmap(px, px, android.graphics.Bitmap.Config.ARGB_8888)
    val c = android.graphics.Canvas(b)
    val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    val path = android.graphics.Path()
    val cx = px / 2f; val R = px / 2f * .9f
    for (i in 0..360) {
        val th = Math.toRadians(i.toDouble())
        val rr = R * (1f - .07f * (1f - Math.cos(9 * th).toFloat()) / 2f * 2f)
        val x = cx + rr * Math.cos(th).toFloat(); val y = cx + rr * Math.sin(th).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close(); c.drawPath(path, p)
    return b
}

/* ===== 2×2: главное сейчас ===== */
class W22 : LessonWidget() {
    @Composable
    override fun Body(s: AppState, n: Now, lang: String) {
        val cfg = s.widget; val L = n.today
        Column(GlanceModifier.fillMaxSize()) {
            if (!live(n)) {
                val I = Sched.idle(n, lang); val nd = n.nextDayLessons
                Lb(if (cfg.idleNoLessons) I.title else I.day, if (cfg.idleNoLessons) I.day else "")
                if (cfg.idleNextDay && nd.isNotEmpty()) {
                    nd.take(4).forEach { x -> MiniRow(s, x, lang) }
                    if (nd.size > 4) Mt("+${nd.size - 4}")
                } else Big(I.em, I.title)
                return@Column
            }
            val f = focusOf(n, lang); val x = L[f.pos]
            when (kind) {
                "ios" -> Ios22(s, n, L, f, x, lang)
                "pixel" -> Pixel22(s, n, L, f, x, lang)
                "origin" -> Origin22(s, n, L, f, x, lang)
                "oneui" -> OneUi22(s, n, L, f, x, lang)
                else -> Color22(s, n, L, f, x, lang)
            }
        }
    }
}

@Composable
private fun ColumnScope.Color22(s: AppState, n: Now, L: List<Slot>, f: Fx, x: Slot, lang: String) {
    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        TileS(s, x, lang, 42.dp)
        Spacer(GlanceModifier.defaultWeight())
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${n.minutesLeft}", style = st(30.sp, ACC, bold = true), maxLines = 1)
            Text(" " + mn(lang), style = st(12.sp, ACC, bold = true), maxLines = 1, modifier = GlanceModifier.padding(bottom = 5.dp))
        }
    }
    Spacer(GlanceModifier.defaultWeight())
    Text(name(s, x, lang), style = st(17.sp, bold = true), maxLines = 2)
    Text(join(roomOf(s, L, f.pos, lang), if (n.phase == Phase.LESSON) Tr.t(lang, "till", Tr.hm(x.end)) else f.label), style = st(10.5.sp, TX2), maxLines = 1)
    Spacer(GlanceModifier.height(8.dp))
    Bar(fracOf(x, n), LocalWP.current.acc, LocalInner.current.width)
}

@Composable
private fun ColumnScope.Ios22(s: AppState, n: Now, L: List<Slot>, f: Fx, x: Slot, lang: String) {
    Text(join(f.label, if (s.widget.room) roomRaw(x) else "").uppercase(), style = st(10.5.sp, LBC, bold = true), maxLines = 1)
    Text(name(s, x, lang), style = st(17.sp, bold = true), maxLines = 2, modifier = GlanceModifier.padding(top = 2.dp))
    Spacer(GlanceModifier.defaultWeight())
    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Row(GlanceModifier.defaultWeight(), verticalAlignment = Alignment.Bottom) {
            Text("${n.minutesLeft}", style = st(30.sp, bold = true), maxLines = 1)
            Text(" " + mn(lang), style = st(11.sp, TX2), maxLines = 1, modifier = GlanceModifier.padding(bottom = 5.dp))
        }
        val d = density(); val px = (40 * d).toInt()
        Image(ImageProvider(ringBmp(px, fracOf(x, n), Color(0xFFFF6961).toArgb(), Color(0x33FFFFFF).toArgb(), 5 * d)), null, GlanceModifier.size(40.dp))
    }
    val nx = if (n.phase == Phase.LESSON) n.next else -1
    if (nx >= 0) Text(Tr.t(lang, "next") + ": " + name(s, L[nx], lang), style = st(10.5.sp, TX2), maxLines = 1, modifier = GlanceModifier.padding(top = 4.dp))
}

@Composable
private fun ColumnScope.Pixel22(s: AppState, n: Now, L: List<Slot>, f: Fx, x: Slot, lang: String) {
    val inner = LocalInner.current
    val cs = minOf(inner.width * .62f, inner.height * .56f)
    val d = density()
    Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
        Box(GlanceModifier.size(cs), contentAlignment = Alignment.Center) {
            Image(ImageProvider(cookieBmp((cs.value * d).toInt(), LocalWP.current.acc.toArgb())), null, GlanceModifier.size(cs))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${n.minutesLeft}", style = st(26.sp, INK, bold = true, center = true), maxLines = 1)
                Text(mn(lang), style = st(11.sp, INK, bold = true, center = true), maxLines = 1)
            }
        }
        Text(name(s, x, lang), style = st(14.sp, bold = true, center = true), maxLines = 2, modifier = GlanceModifier.padding(top = 8.dp))
        val rm = roomOf(s, L, f.pos, lang)
        if (rm.isNotEmpty()) Text(rm, style = st(10.5.sp, TX2, center = true), maxLines = 1)
    }
}

@Composable
private fun ColumnScope.Origin22(s: AppState, n: Now, L: List<Slot>, f: Fx, x: Slot, lang: String) {
    Text(f.label, style = st(11.5.sp, ACC, bold = true), maxLines = 1)
    Row(verticalAlignment = Alignment.Bottom) {
        Text("${n.minutesLeft}", style = st(52.sp, fam = LIGHT), maxLines = 1)
        Text(" " + mn(lang), style = st(11.sp, TX2), maxLines = 1, modifier = GlanceModifier.padding(bottom = 9.dp))
    }
    Spacer(GlanceModifier.defaultWeight())
    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TileS(s, x, lang, 32.dp)
        Spacer(GlanceModifier.width(8.dp))
        Column(GlanceModifier.defaultWeight()) {
            Text(name(s, x, lang), style = st(13.sp, bold = true), maxLines = 2)
            Mt(roomOf(s, L, f.pos, lang))
        }
    }
}

@Composable
private fun ColumnScope.OneUi22(s: AppState, n: Now, L: List<Slot>, f: Fx, x: Slot, lang: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(GlanceModifier.size(13.dp).background(LocalWP.current.acc).cornerRadius(4.dp)) {}
        Spacer(GlanceModifier.width(6.dp))
        Text(schedWord(lang), style = st(11.sp, TX2, bold = true), maxLines = 1)
    }
    Row(GlanceModifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        TileS(s, x, lang, 22.dp)
        Spacer(GlanceModifier.width(6.dp))
        Text(f.label, style = st(12.sp, ACC, bold = true), maxLines = 1)
    }
    Text(name(s, x, lang), style = st(16.sp, bold = true), maxLines = 2, modifier = GlanceModifier.padding(top = 3.dp))
    Spacer(GlanceModifier.defaultWeight())
    Row(GlanceModifier.fillMaxWidth()) {
        Text(Tr.hm(x.start), style = st(10.sp, TX2), maxLines = 1, modifier = GlanceModifier.defaultWeight())
        Text(Tr.hm(x.end), style = st(10.sp, TX2), maxLines = 1)
    }
    Spacer(GlanceModifier.height(4.dp))
    Bar(fracOf(x, n), LocalWP.current.acc, LocalInner.current.width)
}

@Composable
private fun MiniRow(s: AppState, x: Slot, lang: String) {
    Row(GlanceModifier.fillMaxWidth().padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("${x.idx + 1}", style = st(9.5.sp, TX2))
        Spacer(GlanceModifier.width(6.dp))
        TileS(s, x, lang, 18.dp)
        Spacer(GlanceModifier.width(6.dp))
        Text(name(s, x, lang), style = st(11.5.sp, bold = true), maxLines = 1, modifier = GlanceModifier.defaultWeight())
        Text(Tr.hm(x.start), style = st(9.5.sp, TX2), maxLines = 1)
    }
}

/* ===== 2×1 и 1×2: один урок, что именно — по настройке ===== */
internal data class Pick(val pos: Int, val label: String)

internal fun pickSmall(s: AppState, n: Now, lang: String): Pick? = when (n.phase) {
    Phase.LESSON -> if (s.widget.onLesson == "next" && n.next >= 0) Pick(n.next, Tr.t(lang, "next"))
                    else Pick(n.cur, Tr.t(lang, "now"))
    Phase.BREAK -> if (s.widget.onBreak == "prev" && n.prev >= 0) Pick(n.prev, Tr.t(lang, "break") + " · " + Tr.t(lang, "prev").lowercase())
                   else Pick(n.next, Tr.t(lang, "break") + " · " + Tr.t(lang, "next").lowercase())
    Phase.BEFORE -> Pick(n.next, Tr.t(lang, "soon") + " · " + Tr.t(lang, "next").lowercase())
    else -> null
}

internal fun mainPos(n: Now): Int = when (n.phase) {
    Phase.LESSON -> n.cur
    Phase.BREAK, Phase.BEFORE -> n.next
    else -> -1
}

/* ===== 2×1: один урок строкой ===== */
class W21 : LessonWidget() {
    override val padV: Dp = 10.dp
    override val pill = true
    @Composable
    override fun Body(s: AppState, n: Now, lang: String) {
        val cfg = s.widget
        Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            if (!live(n)) {
                val I = Sched.idle(n, lang); val nd = n.nextDayLessons
                if (cfg.idleNextDay && nd.isNotEmpty()) {
                    val f = nd.first()
                    TileS(s, f, lang, 32.dp)
                    Spacer(GlanceModifier.width(10.dp))
                    Column(GlanceModifier.defaultWeight()) {
                        Lb(if (cfg.idleNoLessons) I.title else I.day, if (cfg.idleNoLessons) I.day else "")
                        Text(name(s, f, lang), style = st(14.5.sp, bold = true), maxLines = 1)
                        Mt(Tr.t(lang, "lessonsCnt", nd.size) + " · " + Tr.t(lang, "from", Tr.hm(f.start)))
                    }
                } else {
                    TileE(s, I.em, 0xFF8E8E93, 36.dp)
                    Spacer(GlanceModifier.width(10.dp))
                    Column(GlanceModifier.defaultWeight()) { Lb(I.day); Text(I.title, style = st(14.5.sp, bold = true), maxLines = 1) }
                }
                return@Row
            }
            val p = pickSmall(s, n, lang) ?: return@Row
            val x = n.today[p.pos]
            val m = minsTo(x, n)
            when (kind) {
                "ios" -> {
                    Box(GlanceModifier.width(4.dp).height(40.dp).background(subjColor(s, x, lang)).cornerRadius(2.dp)) {}
                    Spacer(GlanceModifier.width(9.dp))
                    Column(GlanceModifier.defaultWeight()) {
                        Text(join(p.label, "$m ${mn(lang)}").uppercase(), style = st(10.sp, LBC, bold = true), maxLines = 1)
                        Text(name(s, x, lang), style = st(14.5.sp, bold = true), maxLines = 1)
                    }
                }
                "pixel" -> {
                    Box(GlanceModifier.size(48.dp).background(LocalWP.current.acc).cornerRadius(18.dp), contentAlignment = Alignment.Center) {
                        Text("$m", style = st(18.sp, INK, bold = true, center = true), maxLines = 1)
                    }
                    Spacer(GlanceModifier.width(10.dp))
                    Column(GlanceModifier.defaultWeight()) {
                        Text(p.label, style = st(11.sp, ACC, bold = true), maxLines = 1)
                        Text(name(s, x, lang), style = st(14.5.sp, bold = true), maxLines = 1)
                    }
                }
                "origin" -> {
                    TileS(s, x, lang, 36.dp)
                    Spacer(GlanceModifier.width(10.dp))
                    Column(GlanceModifier.defaultWeight()) {
                        Text(name(s, x, lang), style = st(14.5.sp, bold = true), maxLines = 1)
                        Mt(roomOf(s, n.today, p.pos, lang).ifEmpty { p.label })
                    }
                    Text("$m", style = st(32.sp, fam = LIGHT), maxLines = 1)
                }
                else -> {
                    TileS(s, x, lang, 36.dp)
                    Spacer(GlanceModifier.width(10.dp))
                    Column(GlanceModifier.defaultWeight()) {
                        Text(join(p.label, "$m ${mn(lang)}"), style = st(11.sp, ACC, bold = true), maxLines = 1)
                        Text(name(s, x, lang), style = st(14.5.sp, bold = true), maxLines = 1)
                    }
                }
            }
        }
    }
}

/* ===== 1×2: один урок столбиком ===== */
class W12 : LessonWidget() {
    override val padH: Dp = 8.dp
    override val padV: Dp = 12.dp
    override val pill = true
    @Composable
    override fun Body(s: AppState, n: Now, lang: String) {
        val cfg = s.widget
        if (!live(n)) {
            Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                val I = Sched.idle(n, lang); val nd = n.nextDayLessons
                if (cfg.idleNextDay && nd.isNotEmpty()) {
                    val f = nd.first()
                    Lb(if (cfg.idleNoLessons) I.title else I.day, size = 9.sp, center = true)
                    Spacer(GlanceModifier.defaultWeight())
                    TileS(s, f, lang, 34.dp)
                    Text(name(s, f, lang), style = st(11.sp, bold = true, center = true), maxLines = 3)
                    Spacer(GlanceModifier.defaultWeight())
                    Text(Tr.hm(f.start), style = st(10.sp, TX2, center = true), maxLines = 1)
                } else {
                    Lb(I.day, size = 9.sp, center = true)
                    Spacer(GlanceModifier.defaultWeight())
                    TileE(s, I.em, 0xFF8E8E93, 34.dp)
                    Text(I.title, style = st(11.sp, bold = true, center = true), maxLines = 3)
                    Spacer(GlanceModifier.defaultWeight())
                }
            }
            return
        }
        val p = pickSmall(s, n, lang) ?: return
        val x = n.today[p.pos]
        val lbl = p.label.replace(" · ", "\n")
        val rm = if (cfg.room) roomRaw(x) else ""
        if (kind == "ios") {
            Column(GlanceModifier.fillMaxSize()) {
                Text(lbl.uppercase(), style = st(9.5.sp, LBC, bold = true), maxLines = 2)
                Text(Tr.hm(x.start), style = st(20.sp, bold = true), maxLines = 1)
                Spacer(GlanceModifier.defaultWeight())
                Text(name(s, x, lang), style = st(11.5.sp, bold = true), maxLines = 3)
                if (rm.isNotEmpty()) Text(rm, style = st(9.5.sp, TX2), maxLines = 1)
            }
            return
        }
        Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            when (kind) {
                "origin" -> {
                    TileS(s, x, lang, 36.dp)
                    Text(lbl, style = st(10.sp, ACC, bold = true, center = true), maxLines = 2, modifier = GlanceModifier.padding(top = 6.dp))
                    Text(name(s, x, lang), style = st(11.sp, bold = true, center = true), maxLines = 3)
                    Spacer(GlanceModifier.defaultWeight())
                    Text(Tr.hm(x.start), style = st(20.sp, center = true, fam = LIGHT), maxLines = 1)
                }
                "oneui" -> {
                    Text(lbl, style = st(10.sp, ACC, bold = true, center = true), maxLines = 2)
                    Spacer(GlanceModifier.defaultWeight())
                    TileS(s, x, lang, 32.dp)
                    Text(name(s, x, lang), style = st(11.sp, bold = true, center = true), maxLines = 3, modifier = GlanceModifier.padding(top = 5.dp))
                    Spacer(GlanceModifier.defaultWeight())
                    Text(Tr.hm(x.start), style = st(14.sp, ACC, bold = true, center = true), maxLines = 1)
                }
                else -> {
                    Text(lbl, style = st(10.sp, ACC, bold = true, center = true), maxLines = 2)
                    Spacer(GlanceModifier.defaultWeight())
                    TileS(s, x, lang, 34.dp)
                    Text(name(s, x, lang), style = st(11.sp, bold = true, center = true), maxLines = 3, modifier = GlanceModifier.padding(top = 5.dp))
                    Spacer(GlanceModifier.defaultWeight())
                    Text(Tr.hm(x.start), style = st(10.sp, TX2, center = true), maxLines = 1)
                    if (rm.isNotEmpty()) Text(rm, style = st(9.5.sp, TX2, center = true), maxLines = 1)
                }
            }
        }
    }
}

/* ===== 2×4: уроки дня списком на всю высоту ===== */
class W24 : LessonWidget() {
    @Composable
    override fun Body(s: AppState, n: Now, lang: String) {
        val cfg = s.widget
        Column(GlanceModifier.fillMaxSize()) {
            if (!live(n)) {
                val I = Sched.idle(n, lang); val nd = n.nextDayLessons
                if (cfg.idleNextDay && nd.isNotEmpty()) Day24(s, nd, -1, -1, lang, if (cfg.idleNoLessons) I.title else I.day, null)
                else { Hd(I.day, ""); Big(I.em, I.title) }
                return@Column
            }
            val L = n.today
            val past = when (n.phase) { Phase.LESSON -> n.cur - 1; Phase.BREAK -> n.prev; else -> -1 }
            val title = when (kind) {
                "ios" -> join(Tr.t(lang, "today"), Tr.t(lang, "lessonsCnt", L.size))
                "origin", "oneui" -> Tr.dayLong(lang, n.date.dayOfWeek.value - 1)
                else -> Tr.t(lang, "today")
            }
            Day24(s, L, if (n.phase == Phase.LESSON) n.cur else -1, past, lang, title, n.date.dayOfMonth)
        }
    }
}

@Composable
private fun ColumnScope.Day24(s: AppState, L: List<Slot>, cur: Int, past: Int, lang: String, title: String, dayNum: Int?) {
    val k = kind
    when (k) {
        "ios" -> Text(title.uppercase(), style = st(10.5.sp, LBC, bold = true), maxLines = 1, modifier = GlanceModifier.padding(bottom = 4.dp))
        "origin" -> Row(GlanceModifier.padding(bottom = 2.dp), verticalAlignment = Alignment.Bottom) {
            if (dayNum != null) Text("$dayNum", style = st(34.sp, fam = LIGHT), maxLines = 1)
            Text(" " + title.lowercase(), style = st(12.sp, ACC, bold = true), maxLines = 1, modifier = GlanceModifier.padding(bottom = 6.dp))
        }
        "oneui" -> Row(GlanceModifier.padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(GlanceModifier.size(13.dp).background(LocalWP.current.acc).cornerRadius(4.dp)) {}
            Spacer(GlanceModifier.width(6.dp))
            Text(title, style = st(11.5.sp, TX2, bold = true), maxLines = 1)
        }
        else -> Text(title, style = st(12.sp, ACC, bold = true), maxLines = 1, modifier = GlanceModifier.padding(start = 4.dp, bottom = 4.dp))
    }
    L.take(8).forEachIndexed { i, _ -> Row24(s, L, i, lang, if (i == cur) 1 else if (i <= past) 2 else 0, k) }
}

@Composable
private fun ColumnScope.Row24(s: AppState, L: List<Slot>, i: Int, lang: String, state: Int, k: String) {
    val x = L[i]
    val tx = if (state == 2) TX3 else TX
    val tx2 = if (state == 2) TX3 else TX2
    val on = state == 1
    val rm = if (s.widget.room) roomRaw(x) else ""
    val bar = subjColor(s, x, lang).copy(alpha = if (state == 2) .35f else 1f)
    when (k) {
        "ios" -> Row(GlanceModifier.defaultWeight().fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(GlanceModifier.width(3.5.dp).fillMaxHeight().background(bar).cornerRadius(2.dp)) {}
            Spacer(GlanceModifier.width(8.dp))
            Column(GlanceModifier.defaultWeight()) {
                Text(name(s, x, lang), style = st(13.sp, tx, bold = true), maxLines = 1)
                Text(join(span(x), rm), style = st(10.sp, tx2), maxLines = 1)
            }
        }
        "pixel" -> Box(GlanceModifier.defaultWeight().fillMaxWidth().padding(bottom = 3.dp)) {
            val ink = if (on) INK else tx
            Row(GlanceModifier.fillMaxSize().background(if (on) LocalWP.current.acc else LocalWP.current.colBg).cornerRadius(if (on) 16.dp else 8.dp).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                TileS(s, x, lang, 26.dp)
                Spacer(GlanceModifier.width(9.dp))
                Column(GlanceModifier.defaultWeight()) {
                    Text(name(s, x, lang), style = st(13.sp, ink, bold = true), maxLines = 1)
                    Text(join(Tr.hm(x.start), rm), style = st(10.sp, if (on) INK else tx2), maxLines = 1)
                }
            }
        }
        "origin" -> Column(GlanceModifier.defaultWeight().fillMaxWidth()) {
            if (i > 0) Box(GlanceModifier.fillMaxWidth().height(1.dp).background(LocalWP.current.line)) {}
            Row(GlanceModifier.defaultWeight().fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${x.idx + 1}", style = st(11.sp, if (on) ACC else tx2, bold = true), maxLines = 1, modifier = GlanceModifier.width(16.dp))
                TileS(s, x, lang, 28.dp)
                Spacer(GlanceModifier.width(9.dp))
                Column(GlanceModifier.defaultWeight()) {
                    Text(name(s, x, lang), style = st(13.sp, tx, bold = true), maxLines = 1)
                    Text(Tr.hm(x.start), style = st(10.sp, if (on) ACC else tx2), maxLines = 1)
                }
            }
        }
        "oneui" -> Row(GlanceModifier.defaultWeight().fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(Tr.hm(x.start), style = st(11.sp, if (on) ACC else tx2, bold = true), maxLines = 1, modifier = GlanceModifier.width(42.dp))
            Box(GlanceModifier.width(3.dp).fillMaxHeight().padding(vertical = 5.dp)) {
                Box(GlanceModifier.fillMaxSize().background(bar).cornerRadius(2.dp)) {}
            }
            Spacer(GlanceModifier.width(8.dp))
            Column(GlanceModifier.defaultWeight()) {
                Text(name(s, x, lang), style = st(13.sp, tx, bold = true), maxLines = 1)
                val r2 = roomOf(s, L, i, lang)
                if (r2.isNotEmpty()) Text(r2, style = st(10.sp, tx2), maxLines = 1)
            }
        }
        else -> Row(GlanceModifier.defaultWeight().fillMaxWidth().background(if (on) LocalWP.current.soft else Color.Transparent).cornerRadius(14.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            TileS(s, x, lang, 28.dp)
            Spacer(GlanceModifier.width(9.dp))
            Column(GlanceModifier.defaultWeight()) {
                Text(name(s, x, lang), style = st(13.sp, tx, bold = true), maxLines = 1)
                Text(join(Tr.hm(x.start), rm), style = st(10.5.sp, tx2), maxLines = 1)
            }
        }
    }
}

/* ===== 4×2: день целиком ===== */
class W42 : LessonWidget() {
    @Composable
    override fun Body(s: AppState, n: Now, lang: String) {
        val cfg = s.widget
        Column(GlanceModifier.fillMaxSize()) {
            if (!live(n)) {
                val I = Sched.idle(n, lang); val nd = n.nextDayLessons
                Hd(if (cfg.idleNoLessons) I.title else I.day, if (cfg.idleNoLessons) I.day else "")
                if (cfg.idleNextDay && nd.isNotEmpty()) Cells42(s, nd, 0, -1, lang) else Big(I.em, I.title)
                return@Column
            }
            val L = n.today
            val f = focusOf(n, lang)
            val cur = if (n.phase == Phase.LESSON) n.cur else -1
            val day = Tr.dayLong(lang, n.date.dayOfWeek.value - 1)
            val cnt = Tr.t(lang, "lessonsCnt", L.size)
            when (kind) {
                "ios" -> Ios42(s, n, L, f, lang, day)
                "origin" -> Origin42(s, n, L, f, lang)
                "oneui" -> {
                    Row(GlanceModifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(GlanceModifier.size(13.dp).background(LocalWP.current.acc).cornerRadius(4.dp)) {}
                        Spacer(GlanceModifier.width(6.dp))
                        Text("$day, ${n.date.dayOfMonth}", style = st(11.5.sp, TX2, bold = true), maxLines = 1, modifier = GlanceModifier.defaultWeight())
                        Text(cnt, style = st(11.sp, TX2), maxLines = 1)
                    }
                    L.drop(f.pos).take(4).forEachIndexed { k, x ->
                        val on = f.pos + k == cur
                        Row(GlanceModifier.defaultWeight().fillMaxWidth().background(if (on) LocalWP.current.soft else Color.Transparent).cornerRadius(14.dp).padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(Tr.hm(x.start), style = st(11.5.sp, if (on) ACC else TX2, bold = true), maxLines = 1, modifier = GlanceModifier.width(44.dp))
                            Box(GlanceModifier.size(8.dp).background(subjColor(s, x, lang)).cornerRadius(4.dp)) {}
                            Spacer(GlanceModifier.width(8.dp))
                            Text(name(s, x, lang), style = st(13.sp, bold = true), maxLines = 1, modifier = GlanceModifier.defaultWeight())
                            if (cfg.room) Text(roomRaw(x), style = st(10.5.sp, TX2), maxLines = 1)
                        }
                    }
                }
                "pixel" -> {
                    Row(GlanceModifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Text("$day, ${n.date.dayOfMonth}", style = st(12.sp, ACC, bold = true), maxLines = 1, modifier = GlanceModifier.defaultWeight())
                        Text(cnt, style = st(11.sp, TX2), maxLines = 1)
                    }
                    Cells42(s, L, f.pos, cur, lang)
                }
                else -> {
                    Row(GlanceModifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Text("● " + join(day, cnt), style = st(11.5.sp, ACC, bold = true), maxLines = 1, modifier = GlanceModifier.defaultWeight())
                        Text("${f.pos + 1}/${L.size}", style = st(11.sp, TX2), maxLines = 1)
                    }
                    Cells42(s, L, f.pos, cur, lang)
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.Cells42(s: AppState, L: List<Slot>, start: Int, cur: Int, lang: String) {
    val px = kind == "pixel"
    Row(GlanceModifier.defaultWeight().fillMaxWidth()) {
        val items = L.drop(start).take(4)
        for (k in 0 until 4) {
            if (k > 0) Spacer(GlanceModifier.width(if (px) 4.dp else 6.dp))
            val x = items.getOrNull(k)
            if (x == null) { Spacer(GlanceModifier.defaultWeight()); continue }
            val on = start + k == cur
            val bg = if (on) (if (px) LocalWP.current.acc else LocalWP.current.soft) else LocalWP.current.colBg
            val fg = if (px && on) INK else TX
            val fg2 = if (px && on) INK else TX2
            Column(GlanceModifier.defaultWeight().fillMaxHeight().background(bg).cornerRadius(if (px) (if (on) 20.dp else 12.dp) else 16.dp).padding(horizontal = 8.dp, vertical = 8.dp)) {
                Text(Tr.hm(x.start), style = st(10.5.sp, if (on && !px) ACC else fg2, bold = true), maxLines = 1)
                Spacer(GlanceModifier.height(4.dp))
                TileS(s, x, lang, 26.dp)
                Spacer(GlanceModifier.height(4.dp))
                Text(name(s, x, lang), style = st(12.sp, fg, bold = true), maxLines = 2)
                Spacer(GlanceModifier.defaultWeight())
                if (s.widget.room) Text(roomRaw(x), style = st(10.sp, fg2), maxLines = 1)
            }
        }
    }
}

@Composable
private fun ColumnScope.Ios42(s: AppState, n: Now, L: List<Slot>, f: Fx, lang: String, day: String) {
    val x = L[f.pos]
    Row(GlanceModifier.fillMaxSize()) {
        Column(GlanceModifier.width(LocalInner.current.width * .36f).fillMaxHeight()) {
            Text(day.uppercase(), style = st(10.5.sp, LBC, bold = true), maxLines = 1)
            Text("${n.date.dayOfMonth}", style = st(38.sp, fam = LIGHT), maxLines = 1)
            Spacer(GlanceModifier.defaultWeight())
            Text(f.label, style = st(10.5.sp, TX2), maxLines = 1)
            Text(name(s, x, lang), style = st(13.sp, bold = true), maxLines = 2)
            Text(f.left, style = st(10.5.sp, TX2), maxLines = 1)
        }
        Spacer(GlanceModifier.width(12.dp))
        Column(GlanceModifier.defaultWeight().fillMaxHeight()) {
            L.drop(f.pos).take(4).forEach { y ->
                Row(GlanceModifier.defaultWeight().fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(GlanceModifier.width(3.5.dp).fillMaxHeight().background(subjColor(s, y, lang)).cornerRadius(2.dp)) {}
                    Spacer(GlanceModifier.width(7.dp))
                    Column(GlanceModifier.defaultWeight()) {
                        Text(name(s, y, lang), style = st(12.5.sp, bold = true), maxLines = 1)
                        Text(join(span(y), if (s.widget.room) roomRaw(y) else ""), style = st(10.sp, TX2), maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.Origin42(s: AppState, n: Now, L: List<Slot>, f: Fx, lang: String) {
    val x = L[f.pos]
    Row(GlanceModifier.fillMaxSize()) {
        Column(GlanceModifier.width(LocalInner.current.width * .44f).fillMaxHeight()) {
            TileS(s, x, lang, 38.dp)
            Text(join(f.label, if (s.widget.room) roomRaw(x) else ""), style = st(11.sp, ACC, bold = true), maxLines = 1, modifier = GlanceModifier.padding(top = 6.dp))
            Text(name(s, x, lang), style = st(15.sp, bold = true), maxLines = 2)
            Spacer(GlanceModifier.defaultWeight())
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${n.minutesLeft}", style = st(40.sp, fam = LIGHT), maxLines = 1)
                Text(" " + mn(lang), style = st(11.sp, TX2), maxLines = 1, modifier = GlanceModifier.padding(bottom = 7.dp))
            }
        }
        Spacer(GlanceModifier.width(10.dp))
        Box(GlanceModifier.width(1.dp).fillMaxHeight().background(LocalWP.current.line)) {}
        Spacer(GlanceModifier.width(12.dp))
        Column(GlanceModifier.defaultWeight().fillMaxHeight()) {
            val rest = L.drop(f.pos + 1).take(4)
            if (rest.isEmpty()) Mt(Tr.t(lang, "lastLesson"))
            rest.forEach { y ->
                Row(GlanceModifier.defaultWeight().fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TileS(s, y, lang, 24.dp)
                    Spacer(GlanceModifier.width(8.dp))
                    Text(name(s, y, lang), style = st(12.5.sp, bold = true), maxLines = 1, modifier = GlanceModifier.defaultWeight())
                    Text(Tr.hm(y.start), style = st(10.5.sp, TX2), maxLines = 1)
                }
            }
        }
    }
}

/* ===== Приёмники виджетов (по одному на размер) ===== */
class W22Receiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = W22() }
class W21Receiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = W21() }
class W12Receiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = W12() }
class W24Receiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = W24() }
class W42Receiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = W42() }

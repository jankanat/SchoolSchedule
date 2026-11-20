package kz.alnazar.schedule.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.material3.LocalTextStyle
import kz.alnazar.schedule.R
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Токены оформления — один в один из CSS прототипа (стиль iOS 26, тёмная тема).
 * Остальные стили и темы будут такими же наборами токенов.
 */
data class Tok(
    val bg: List<Color>,            // фон-градиент экрана (165°)
    val s1: Color,                  // карточки, строки
    val s2: Color,                  // вторичная поверхность (чипы, сегменты)
    val sheet: Color,               // нижняя шторка
    val tx: Color,
    val tx2: Color,
    val line: Color,
    val acc: Color,
    val accTx: Color,
    val cardBorderTop: Color,       // стеклянная кромка карточек сверху
    val cardBorderBottom: Color,
    val navBg: Color,
    val navBorder: Color,
    val segOn: Color,
    val swOn: Color,
    val rc: Dp = 26.dp,             // радиус карточек
    val rsg: Dp = 13.dp,            // радиус сегментов
    val iconRadius: Float = .30f,   // радиус плитки предмета, доля от размера
    val titleSize: Float = 34f,
    val id: String = "ios26",
    val glassy: Boolean = true,     // стеклянные поверхности с кромкой (iOS 26)
    val titleWeight: FontWeight = FontWeight.Bold,
    val hdrMin: Dp = 56.dp,
    val hero: Color? = null,        // заливка карточки «Сейчас» (null — градиент акцента)
    val heroTx: Color? = null,
    val secCont: Color = s2,        // M3 secondaryContainer
    val onSecCont: Color = tx,
    val outline: Color = line,
    val secUpper: Boolean = true,   // заголовки разделов капсом (iOS)
    val titleCenter: Boolean = false,
    val titleTrack: Float = -0.6f,
    val segKind: String = "slide",  // slide | soft | connected
    val swW: Dp = 51.dp, val swH: Dp = 31.dp, val swPad: Dp = 2.dp,
    val glow: Boolean = false,      // белое внутреннее свечение (ColorOS)
    val nav: String = "glass",      // glass | ios18 | m3 | island | origin | oneui
    val wfill: Color? = null,       // своя тема: заливка виджета
) {
    val pixel: Boolean get() = id == "pixel"
    val accSoft: Color get() = acc.copy(alpha = .17f)
    val heroBrush: Brush get() = Brush.linearGradient(listOf(acc, lerp(acc, Color.Black, .45f)))
}

val Ios26Dark = Tok(
    bg = listOf(Color(0xFF0B1020), Color(0xFF1B1040), Color(0xFF07222E)),
    s1 = Color(0x6B464656),         // rgba(70,70,86,.42)
    s2 = Color(0x42828296),         // rgba(130,130,150,.26)
    sheet = Color(0xFF1D1D26),
    tx = Color.White,
    tx2 = Color(0xFF9B9BA3),
    line = Color(0x1FFFFFFF),
    acc = Color(0xFF0A84FF),
    accTx = Color.White,
    cardBorderTop = Color(0x4DFFFFFF),
    cardBorderBottom = Color(0x14FFFFFF),
    navBg = Color(0x8C2E2E3C),      // rgba(46,46,60,.55)
    navBorder = Color(0x29FFFFFF),
    segOn = Color(0x2EFFFFFF),
    swOn = Color(0xFF34C759),
)

/** Pixel OS — Material 3 Expressive, тёмная (цвета Material You из прототипа). */
val PixelDark = Tok(
    bg = listOf(Color(0xFF141218), Color(0xFF141218), Color(0xFF141218)),
    s1 = Color(0xFF211F26),         // surfaceContainer
    s2 = Color(0xFF2B2930),         // surfaceContainerHigh
    sheet = Color(0xFF211F26),
    tx = Color(0xFFE6E0E9),
    tx2 = Color(0xFFCAC4D0),
    line = Color(0x29CAC4D0),
    acc = Color(0xFFD0BCFF),
    accTx = Color(0xFF381E72),
    cardBorderTop = Color.Transparent,
    cardBorderBottom = Color.Transparent,
    navBg = Color(0xFF211F26),
    navBorder = Color.Transparent,
    segOn = Color(0xFFD0BCFF),
    swOn = Color(0xFFD0BCFF),
    rc = 28.dp,
    rsg = 999.dp,
    iconRadius = .5f,
    titleSize = 32f,
    id = "pixel",
    glassy = false,
    titleWeight = FontWeight.Normal,
    hdrMin = 88.dp,
    hero = Color(0xFF4F378B),       // primaryContainer
    heroTx = Color(0xFFEADDFF),
    secCont = Color(0xFF4A4458),
    onSecCont = Color(0xFFE8DEF8),
    outline = Color(0xFF938F99),
    secUpper = false, titleTrack = 0f, segKind = "connected", nav = "m3",
)

/** iOS 18 — классический тёмный iOS: сплошные поверхности, сегменты #636366, зелёные переключатели. */
val Ios18Dark = Tok(
    bg = listOf(Color.Black, Color.Black, Color.Black),
    s1 = Color(0xFF1C1C1E), s2 = Color(0xFF2C2C2E), sheet = Color(0xFF1C1C1E),
    tx = Color.White, tx2 = Color(0xFF9B9BA3), line = Color(0x1FFFFFFF),
    acc = Color(0xFF0A84FF), accTx = Color.White,
    cardBorderTop = Color.Transparent, cardBorderBottom = Color.Transparent,
    navBg = Color(0xD91C1C1E), navBorder = Color(0x26FFFFFF),
    segOn = Color(0xFF636366), swOn = Color(0xFF34C759),
    rc = 14.dp, rsg = 9.dp, iconRadius = .24f, titleSize = 34f, id = "ios18", glassy = false, nav = "ios18",
)

/** ColorOS 17 — матовое «жидкое акриловое» стекло, белое контурное свечение, плавающий остров. */
val ColorDark = Tok(
    bg = listOf(Color.Black, Color.Black, Color.Black),
    s1 = Color(0xFF1C1C1E), s2 = Color(0xFF2C2C2E), sheet = Color(0xFF1C1C1E),
    tx = Color.White, tx2 = Color(0xFF9B9BA3), line = Color(0x1FFFFFFF),
    acc = Color(0xFF4ADE80), accTx = Color(0xFF05230F),
    cardBorderTop = Color.Transparent, cardBorderBottom = Color.Transparent,
    navBg = Color(0x9E262626), navBorder = Color(0x40FFFFFF),
    segOn = Color(0x38FFFFFF), swOn = Color(0xFF4ADE80),
    rc = 28.dp, rsg = 99.dp, iconRadius = .5f, titleSize = 44f, id = "color", glassy = false,
    titleWeight = FontWeight.Bold, hdrMin = 50.dp, secUpper = false, titleCenter = false, titleTrack = -0.8f,
    swW = 46.dp, swH = 26.dp, swPad = 3.dp, glow = false, nav = "island",
)

/** OriginOS 7 — тёплые световые дуги, толстое объёмное стекло vivo, оранжевый «сейчас», синие ссылки. */
val OriginDark = Tok(
    bg = listOf(Color(0xFF1B1009), Color(0xFF120B07), Color(0xFF0D0906)),
    s1 = Color(0x1FFFFFFF), s2 = Color(0x2EFFFFFF), sheet = Color(0xFF1E140E),
    tx = Color.White, tx2 = Color(0xA8FFFFFF), line = Color(0x1AFFFFFF),
    acc = Color(0xFFFF9A3C), accTx = Color(0xFF2A1203),
    cardBorderTop = Color(0x73FFFFFF), cardBorderBottom = Color(0x14FFFFFF),
    navBg = Color(0x40FFFFFF), navBorder = Color(0x40FFFFFF),
    segOn = Color(0x2EFFFFFF), swOn = Color(0xFFFF9A3C),
    rc = 24.dp, rsg = 16.dp, iconRadius = .3f, titleSize = 32f, id = "origin", glassy = true,
    titleWeight = FontWeight.Bold, hdrMin = 66.dp, secUpper = false, titleTrack = -0.3f,
    swW = 50.dp, swH = 30.dp, swPad = 3.dp, nav = "origin",
)

/** One UI 9.5 — сиреневые обои, матовые виджеты без бликов, высокая шапка, капсула меню. */
val OneUiDark = Tok(
    bg = listOf(Color(0xFF1D1838), Color(0xFF272056), Color(0xFF1A1735)),
    s1 = Color(0x1FFFFFFF), s2 = Color(0x2EFFFFFF), sheet = Color(0xFF221D44),
    tx = Color.White, tx2 = Color(0xADEBE8FF), line = Color(0x1FFFFFFF),
    acc = Color(0xFFA8B4FF), accTx = Color(0xFF1A1640),
    cardBorderTop = Color.Transparent, cardBorderBottom = Color.Transparent,
    navBg = Color(0x1FFFFFFF), navBorder = Color.Transparent,
    segOn = Color(0x2EFFFFFF), swOn = Color(0xFFA8B4FF),
    rc = 28.dp, rsg = 999.dp, iconRadius = .5f, titleSize = 34f, id = "oneui", glassy = false,
    titleWeight = FontWeight.Bold, hdrMin = 132.dp, secUpper = false, titleTrack = -0.4f, segKind = "soft",
    swW = 48.dp, swH = 26.dp, swPad = 3.dp, nav = "oneui",
)

fun tokFor(style: String): Tok = when (style) {
    "pixel" -> PixelDark; "ios18" -> Ios18Dark; "color" -> ColorDark; "origin" -> OriginDark; "oneui" -> OneUiDark
    else -> Ios26Dark
}

/** Цвет из тона (0..360), насыщенности и яркости (0..1). */
fun hsl(h: Float, s: Float, l: Float, a: Float = 1f): Color =
    Color.hsl(((h % 360f) + 360f) % 360f, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f), a.coerceIn(0f, 1f))

/**
 * Своя тема: берём форму и поведение выбранного стиля, а цвета — из круга и ползунков.
 * Светлый основной цвет даёт светлую тему (тёмный текст), тёмный — тёмную.
 */
fun Tok.withCustom(c: kz.alnazar.schedule.data.CustomTheme): Tok {
    val dark = c.bl < .55f
    val base = hsl(c.bh, c.bs, c.bl)
    val up = if (dark) 1f else -1f
    val tx = if (dark) Color.White else Color(0xFF111114)
    val acc = hsl(c.ah, c.asat, c.al)
    val accTx = if (acc.luminance() > .45f) Color(0xFF0B0B10) else Color.White
    val surf = hsl(c.bh, c.bs * .9f, c.bl + up * .09f)
    val surf2 = hsl(c.bh, c.bs * .85f, c.bl + up * .16f)
    return copy(
        bg = listOf(base, hsl(c.bh + 28f, c.bs, c.bl + up * .05f), hsl(c.bh - 24f, c.bs, c.bl + up * .02f)),
        s1 = surf.copy(alpha = c.alpha), s2 = surf2.copy(alpha = (c.alpha + .15f).coerceAtMost(1f)),
        sheet = hsl(c.bh, c.bs * .9f, c.bl + up * .07f),
        tx = tx, tx2 = tx.copy(alpha = .6f), line = tx.copy(alpha = .12f),
        acc = acc, accTx = accTx,
        cardBorderTop = if (glassy) tx.copy(alpha = if (dark) .28f else .6f) else Color.Transparent,
        cardBorderBottom = if (glassy) tx.copy(alpha = .06f) else Color.Transparent,
        navBg = surf.copy(alpha = (c.alpha * .8f).coerceAtLeast(.35f)), navBorder = tx.copy(alpha = .16f),
        segOn = if (segKind == "soft") acc else tx.copy(alpha = if (id == "ios18") .28f else .18f),
        swOn = if (id == "ios26" || id == "ios18") Color(0xFF34C759) else acc,
        hero = if (hero != null) lerp(base, acc, .45f) else null, heroTx = if (hero != null) tx else null,
        secCont = lerp(surf2, acc, .35f), onSecCont = tx, outline = tx.copy(alpha = .45f),
        wfill = hsl(c.bh, c.bs, c.bl + (if (dark) .07f else -.03f), (c.alpha + .2f).coerceAtMost(1f)),
    )
}

val Tok.isDark: Boolean get() = bg.first().luminance() < .5f

val LocalTok = staticCompositionLocalOf { Ios26Dark }

/** Короткий доступ к токенам из любого компонента. */
val T: Tok @Composable get() = LocalTok.current

/** Встроенные шрифты (Google Fonts, OFL): переменные, вес подстраивается сам. */
private fun vf(res: Int) = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { Font(res, it) },
)
private val FF_ROUNDED by lazy { vf(R.font.f_rounded) }
private val FF_SERIF by lazy { vf(R.font.f_serif) }
private val FF_MONO by lazy { vf(R.font.f_mono) }
private val FF_HAND by lazy { vf(R.font.f_hand) }

fun fontFamily(id: String): FontFamily? = when (id) {
    "rounded" -> FF_ROUNDED; "serif" -> FF_SERIF; "mono" -> FF_MONO; "hand" -> FF_HAND; else -> null
}

/** Ползунок «Скругление углов»: множитель радиусов стиля. */
/** Скругление не ниже 20%: при нуле в данных приложение падало при запуске. */
fun Tok.withRadius(k0: Float): Tok {
    val k = k0.coerceIn(.2f, 1.5f)
    return if (k == 1f) this else copy(rc = rc * k, rsg = rsg * k, iconRadius = (iconRadius * k).coerceAtMost(.5f))
}

/** Сила бликов/кромок интерфейса: 0 — выкл, 1 — как задумано, до 1.5. */
val LocalGlare = androidx.compose.runtime.compositionLocalOf { 1f }
fun Color.gl(k: Float): Color = copy(alpha = (alpha * k).coerceIn(0f, 1f))

@Composable
fun AppTheme(tok: Tok = Ios26Dark, font: String = "style", content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (tok.isDark) darkColorScheme(
            primary = tok.acc, onPrimary = tok.accTx, background = tok.bg.first(), surface = tok.sheet,
            onSurface = tok.tx, onBackground = tok.tx,
        ) else lightColorScheme(
            primary = tok.acc, onPrimary = tok.accTx, background = tok.bg.first(), surface = tok.sheet,
            onSurface = tok.tx, onBackground = tok.tx,
        ),
    ) {
        val ff = fontFamily(font)
        val ts = LocalTextStyle.current
        CompositionLocalProvider(LocalTok provides tok, LocalTextStyle provides if (ff != null) ts.copy(fontFamily = ff) else ts, content = content)
    }
}

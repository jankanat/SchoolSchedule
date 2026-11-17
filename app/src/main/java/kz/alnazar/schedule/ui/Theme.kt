package kz.alnazar.schedule.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
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
    bg = listOf(Color(0xFF0E0E0E), Color(0xFF101312), Color(0xFF0E0E0E)),
    s1 = Color(0xB82A2A2A), s2 = Color(0x993A3A3A), sheet = Color(0xFF1E1E1E),
    tx = Color.White, tx2 = Color(0xFF9B9BA3), line = Color(0x1FFFFFFF),
    acc = Color(0xFF35D07F), accTx = Color(0xFF04210F),
    cardBorderTop = Color(0x52FFFFFF), cardBorderBottom = Color(0x0FFFFFFF),
    navBg = Color(0x9E262626), navBorder = Color(0x40FFFFFF),
    segOn = Color(0x38FFFFFF), swOn = Color(0xFF35D07F),
    rc = 20.dp, rsg = 14.dp, iconRadius = .5f, titleSize = 22f, id = "color", glassy = true,
    titleWeight = FontWeight.SemiBold, hdrMin = 50.dp, secUpper = false, titleCenter = true, titleTrack = 0f,
    swW = 46.dp, swH = 26.dp, swPad = 3.dp, glow = true, nav = "island",
)

/** OriginOS 7 — глубина и мягкое стекло vivo, синевато-фиолетовый акцент. */
val OriginDark = Tok(
    bg = listOf(Color(0xFF0B0C10), Color(0xFF0E1018), Color(0xFF10131F)),
    s1 = Color(0xD91B1E28), s2 = Color(0xFF222531), sheet = Color(0xFF171921),
    tx = Color.White, tx2 = Color(0xFF9B9BA3), line = Color(0x1AFFFFFF),
    acc = Color(0xFF7C93FF), accTx = Color(0xFF0B1030),
    cardBorderTop = Color(0x1FFFFFFF), cardBorderBottom = Color(0x08FFFFFF),
    navBg = Color(0xC7171921), navBorder = Color(0x1FFFFFFF),
    segOn = Color(0xFF2F3345), swOn = Color(0xFF7C93FF),
    rc = 24.dp, rsg = 16.dp, iconRadius = .34f, titleSize = 30f, id = "origin", glassy = true,
    titleWeight = FontWeight.SemiBold, hdrMin = 66.dp, secUpper = false, titleTrack = -0.3f,
    swW = 50.dp, swH = 30.dp, swPad = 3.dp, nav = "origin",
)

/** One UI 9 — высокий заголовок, мягкие тональные сегменты, плавающая капсула с тонкой белой кромкой. */
val OneUiDark = Tok(
    bg = listOf(Color.Black, Color.Black, Color.Black),
    s1 = Color(0xFF1E1E20), s2 = Color(0xFF2B2B2E), sheet = Color(0xFF1E1E20),
    tx = Color.White, tx2 = Color(0xFF9B9BA3), line = Color(0x1FFFFFFF),
    acc = Color(0xFF4DA3FF), accTx = Color(0xFF001A33),
    cardBorderTop = Color.Transparent, cardBorderBottom = Color.Transparent,
    navBg = Color(0xB31E1E20), navBorder = Color(0x38FFFFFF),
    segOn = Color(0xFF4DA3FF), swOn = Color(0xFF4DA3FF),
    rc = 28.dp, rsg = 999.dp, iconRadius = .36f, titleSize = 34f, id = "oneui", glassy = false,
    titleWeight = FontWeight.SemiBold, hdrMin = 132.dp, secUpper = false, titleTrack = -0.4f, segKind = "soft",
    swW = 48.dp, swH = 26.dp, swPad = 3.dp, nav = "oneui",
)

fun tokFor(style: String): Tok = when (style) {
    "pixel" -> PixelDark; "ios18" -> Ios18Dark; "color" -> ColorDark; "origin" -> OriginDark; "oneui" -> OneUiDark
    else -> Ios26Dark
}

val LocalTok = staticCompositionLocalOf { Ios26Dark }

/** Короткий доступ к токенам из любого компонента. */
val T: Tok @Composable get() = LocalTok.current

@Composable
fun AppTheme(tok: Tok = Ios26Dark, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = tok.acc, onPrimary = tok.accTx, background = tok.bg.first(), surface = tok.sheet,
            onSurface = tok.tx, onBackground = tok.tx,
        ),
    ) { CompositionLocalProvider(LocalTok provides tok, content = content) }
}

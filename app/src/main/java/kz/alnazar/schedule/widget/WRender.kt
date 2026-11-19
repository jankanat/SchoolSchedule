package kz.alnazar.schedule.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.LruCache
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.graphics.PathParser
import kz.alnazar.schedule.data.AppState
import kz.alnazar.schedule.data.Files
import kz.alnazar.schedule.logic.Now
import kz.alnazar.schedule.logic.Phase
import kz.alnazar.schedule.logic.Sched
import kz.alnazar.schedule.logic.Slot
import kz.alnazar.schedule.logic.Tr
import kz.alnazar.schedule.ui.IconRes
import kz.alnazar.schedule.ui.tokFor
import kz.alnazar.schedule.ui.withCustom
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Рисует виджет целиком в одну картинку, один в один по макетам: пять стилей × пять размеров.
 * Картинка одинаково выглядит в любом лаунчере и в превью приложения — у лаунчера нет ни одного
 * вложенного элемента, который мог бы сломаться.
 */
object WRender {
    /** size: "2x2" | "2x1" | "1x2" | "2x4" | "4x2"; wDp/hDp — реальный размер виджета в dp. */
    fun render(ctx: Context, s: AppState, n: Now, size: String, wDp: Float, hDp: Float, maxBytes: Long = 6_000_000L): Bitmap {
        val dens = ctx.resources.displayMetrics.density
        var sc = dens
        val bytes = wDp * sc * hDp * sc * 4f
        if (bytes > maxBytes) sc *= Math.sqrt(maxBytes / bytes.toDouble()).toFloat()
        val bmp = Bitmap.createBitmap(max(1, (wDp * sc).roundToInt()), max(1, (hDp * sc).roundToInt()), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.scale(sc, sc)
        val lang = s.widgetLang
        R(ctx, s, n, lang, size, wDp, hDp, c, sc).draw()
        return bmp
    }

    /** Голосовое описание для TalkBack. */
    fun describe(s: AppState, n: Now): String {
        val lang = s.widgetLang
        val p = pickSmall(s, n, lang) ?: return Sched.idle(n, lang).title
        val x = n.today.getOrNull(p.pos) ?: return Sched.idle(n, lang).title
        return p.label + ": " + Sched.subj(s, x.lesson.subj, lang).name
    }
}

/* ===== Палитра стиля ===== */

private class Pal(
    val kind: String,          // color | ios | pixel | origin | oneui
    val tx: Int, val tx2: Int, val tx3: Int,
    val acc: Int,              // акцент (заливка, полоса, кольцо)
    val accT: Int,             // акцент как цвет текста
    val ink: Int,              // текст на акцентной заливке
    val line: Int, val card: Int, val cardSel: Int,
    val radius: Float,
    val fill: Int?,            // своя тема: сплошная заливка
)

private fun a(c: Int, al: Float) = (c and 0xFFFFFF) or ((al * 255).roundToInt().coerceIn(0, 255) shl 24)
private fun mix(c1: Int, c2: Int, t: Float): Int {
    fun ch(x: Int, y: Int) = (x + (y - x) * t).roundToInt().coerceIn(0, 255)
    return Color.argb(ch(Color.alpha(c1), Color.alpha(c2)), ch(Color.red(c1), Color.red(c2)), ch(Color.green(c1), Color.green(c2)), ch(Color.blue(c1), Color.blue(c2)))
}

private fun palOf(s: AppState): Pal {
    val kind = when (s.style) { "ios26", "ios18" -> "ios"; "pixel" -> "pixel"; "origin" -> "origin"; "oneui" -> "oneui"; else -> "color" }
    val W = Color.WHITE
    var p = when (kind) {
        "ios" -> Pal(kind, W, a(W, .6f), a(W, .38f), 0xFFFF6961.toInt(), 0xFFFF6961.toInt(), W, a(W, .2f), a(W, .06f), a(W, .12f), 24f, null)
        "pixel" -> Pal(kind, 0xFFE2E2E9.toInt(), 0xFFC4C6D0.toInt(), 0x99C4C6D0.toInt(), 0xFFA8C7FA.toInt(), 0xFFA8C7FA.toInt(), 0xFF062E6F.toInt(),
            a(W, .1f), 0xFF2A3341.toInt(), 0xFFA8C7FA.toInt(), 28f, null)
        "origin" -> Pal(kind, W, a(W, .6f), a(W, .4f), 0xFFFFB36B.toInt(), 0xFFFFB36B.toInt(), 0xFF2A1708.toInt(), a(W, .12f), a(W, .06f), a(W, .12f), 30f, null)
        "oneui" -> Pal(kind, W, a(W, .6f), a(W, .4f), 0xFF3E91FF.toInt(), 0xFF6EA8FF.toInt(), W, a(W, .15f), a(W, .06f), a(0xFF3E91FF.toInt(), .2f), 26f, null)
        else -> Pal(kind, W, a(W, .55f), a(W, .36f), 0xFF4ADE80.toInt(), 0xFF4ADE80.toInt(), 0xFF04210F.toInt(), a(W, .12f), a(W, .06f), a(0xFF4ADE80.toInt(), .16f), 26f, null)
    }
    if (s.theme == "custom") {
        val t = tokFor(s.style).withCustom(s.ct)
        val c = s.ct
        val dark = c.bl < .55f
        val fill = kz.alnazar.schedule.ui.hsl(c.bh, c.bs, c.bl + (if (dark) .07f else -.03f), (c.alpha + .2f).coerceAtMost(1f)).toArgb()
        val tx = t.tx.toArgb(); val acc = t.acc.toArgb()
        p = Pal(kind, tx, a(tx, .62f), a(tx, .36f), acc, acc, mix(acc, Color.BLACK, .82f), a(tx, .14f), a(tx, .07f),
            if (kind == "pixel") acc else a(acc, .2f), p.radius, fill)
    }
    return p
}

/* ===== Шрифты и значки ===== */

private val tfCache = HashMap<String, Typeface>()
private fun typeface(base: String, weight: Int): Typeface = tfCache.getOrPut("$base:$weight") {
    val b = when (base) {
        "serif" -> Typeface.SERIF
        "mono" -> Typeface.MONOSPACE
        "hand" -> Typeface.create("cursive", Typeface.NORMAL)
        "rounded" -> Typeface.create("sans-serif-medium", Typeface.NORMAL)
        else -> Typeface.DEFAULT
    }
    Typeface.create(b, weight, false)
}

private val iconCache = object : LruCache<String, Bitmap>(48) {}

private fun drawableBitmap(ctx: Context, res: Int, px: Int, tint: Int?): Bitmap? {
    val key = "$res:$px:${tint ?: 0}"
    iconCache.get(key)?.let { return it }
    val d = ContextCompat.getDrawable(ctx, res)?.mutate() ?: return null
    val b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val c = Canvas(b)
    val iw = d.intrinsicWidth.takeIf { it > 0 } ?: px
    val ih = d.intrinsicHeight.takeIf { it > 0 } ?: px
    val f = min(px / iw.toFloat(), px / ih.toFloat())
    val w = (iw * f).roundToInt(); val h = (ih * f).roundToInt()
    val l = (px - w) / 2; val t = (px - h) / 2
    d.setBounds(l, t, l + w, t + h)
    if (tint != null) d.setTint(tint)
    d.draw(c)
    iconCache.put(key, b)
    return b
}

/* ===== Рисовальщик ===== */

private class R(
    val ctx: Context, val s: AppState, val n: Now, val lang: String, val size: String,
    val W: Float, val H: Float, val c: Canvas, val sc: Float,
) {
    val P = palOf(s)
    val cfg = s.widget
    val L = n.today
    val live = n.phase == Phase.LESSON || n.phase == Phase.BREAK || n.phase == Phase.BEFORE
    val nd = n.nextDayLessons
    val showNextDay = !live && cfg.idleNextDay && nd.isNotEmpty()
    val showToday = live || (!showNextDay && L.isNotEmpty())
    val fontBase: String = when (s.font) { "serif", "mono", "hand", "rounded" -> s.font; else -> "" }
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    val fp = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    // масштаб оформления от размера виджета
    val k: Float = run {
        val (bw, bh) = when (size) { "2x2" -> 183f to 183f; "2x1" -> 183f to 90f; "1x2" -> 85f to 196f; "2x4" -> 183f to 408f; else -> 380f to 196f }
        min(W / bw, H / bh).coerceIn(.8f, 1.3f)
    }

    /* ---- данные ---- */
    val pick = pickSmall(s, n, lang)
    val fSlot: Slot? = pick?.let { L.getOrNull(it.pos) }
    val isCur = n.phase == Phase.LESSON && pick?.pos == n.cur
    val label: String get() = if (isCur) Tr.t(lang, "now") else if (pick != null && pick.pos == n.prev && n.phase == Phase.BREAK) Tr.t(lang, "prev") else Tr.t(lang, "next")
    fun minsOf(x: Slot): Int = if (isCur) n.minutesLeft else if (n.minute < x.start) x.start - n.minute else 0
    fun frac(x: Slot): Float = if (!isCur) 0f else ((n.minute - x.start).toFloat() / (x.end - x.start).coerceAtLeast(1)).coerceIn(0f, 1f)
    val mn: String get() = if (lang == "en") "min" else "мин"
    fun room(list: List<Slot>, i: Int) = if (cfg.room) Sched.roomLabel(list, i, lang) else ""
    fun roomShort(list: List<Slot>, i: Int): String {
        val r = list.getOrNull(i)?.lesson?.room?.trim().orEmpty()
        return if (cfg.room) r else ""
    }
    fun name(x: Slot) = Sched.subj(s, x.lesson.subj, lang).name + Sched.hwMark(s, x)
    fun sub(x: Slot) = Sched.subj(s, x.lesson.subj, lang)
    fun hm(m: Int) = Tr.hm(m)
    fun join(vararg x: String) = x.filter { it.isNotBlank() }.joinToString(" · ")
    fun up(t: String) = t.uppercase()
    fun dayWord() = Tr.dayLong(lang, n.date.dayOfWeek.value - 1)
    fun lessonsWord(list: List<Slot>) = Tr.plural(lang, "lessons", list.size)

    /** Узкий виджет 1x2 во время урока показывает следующий урок («Далее»). */
    fun slot12(): Slot? = if (n.phase == Phase.LESSON && n.next >= 0 && cfg.onLesson != "next") L.getOrNull(n.next) else fSlot
    fun lab12(x: Slot) = if (x === fSlot) label else Tr.t(lang, "next")

    fun f(v: Float) = v * k

    /* ---- примитивы ---- */
    fun rr(x: Float, y: Float, w: Float, h: Float, r: Float, color: Int) {
        p.reset(); p.isAntiAlias = true; p.color = color
        c.drawRoundRect(RectF(x, y, x + w, y + h), r, r, p)
    }
    fun rrx(x: Float, y: Float, w: Float, h: Float, tl: Float, tr: Float, br: Float, bl: Float, color: Int) {
        p.reset(); p.isAntiAlias = true; p.color = color
        val path = Path(); path.addRoundRect(RectF(x, y, x + w, y + h), floatArrayOf(tl, tl, tr, tr, br, br, bl, bl), Path.Direction.CW)
        c.drawPath(path, p)
    }
    fun circle(cx: Float, cy: Float, r: Float, color: Int) { p.reset(); p.isAntiAlias = true; p.color = color; c.drawCircle(cx, cy, r, p) }

    fun tp(size: Float, color: Int, weight: Int, ls: Float = 0f) = TextPaint(Paint.ANTI_ALIAS_FLAG).also {
        it.textSize = size; it.color = color; it.typeface = typeface(fontBase, weight); it.letterSpacing = ls
    }
    fun layout(t: String, w: Float, size0: Float, color: Int, weight: Int, lines: Int, align: Layout.Alignment, ls: Float = 0f): StaticLayout {
        var size = size0
        if (lines > 1) {   // самое длинное слово не должно ломаться посреди: чуть уменьшаем шрифт
            val q = tp(size, color, weight, ls)
            val longest = t.split(' ').maxOfOrNull { q.measureText(it) } ?: 0f
            if (longest > w && longest > 0f) size = max(size0 * .66f, size0 * w / longest)
        }
        return StaticLayout.Builder.obtain(t, 0, t.length, tp(size, color, weight, ls), max(1, w.roundToInt()))
            .setMaxLines(lines).setEllipsize(TextUtils.TruncateAt.END).setAlignment(align).setIncludePad(false).build()
    }
    /** Нарисовать текст, вернуть высоту. */
    fun text(t: String, x: Float, y: Float, w: Float, size: Float, color: Int, weight: Int = 400, lines: Int = 1,
             align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL, ls: Float = 0f): Float {
        if (t.isEmpty()) return 0f
        val l = layout(t, w, size, color, weight, lines, align, ls)
        c.save(); c.translate(x, y); l.draw(c); c.restore()
        return l.height.toFloat()
    }
    fun th(t: String, w: Float, size: Float, weight: Int = 400, lines: Int = 1): Float =
        if (t.isEmpty()) 0f else layout(t, w, size, 0, weight, lines, Layout.Alignment.ALIGN_NORMAL).height.toFloat()
    /** Текст по базовой линии; возвращает ширину. */
    fun base(t: String, x: Float, by: Float, size: Float, color: Int, weight: Int = 400, ls: Float = 0f, alignRight: Boolean = false): Float {
        val q = tp(size, color, weight, ls)
        val w = q.measureText(t)
        c.drawText(t, if (alignRight) x - w else x, by, q)
        return w
    }
    fun ascent(size: Float, weight: Int = 400) = -tp(size, 0, weight).fontMetrics.ascent
    /** базовая линия, чтобы цифры размера size стояли по центру cy */
    fun midBase(cy: Float, size: Float) = cy + size * .36f
    /** базовая линия, чтобы верх цифр был на высоте y */
    fun topBase(y: Float, size: Float) = y + size * .72f
    fun bigW(t: String, size: Float, weight: Int, ls: Float = 0f) = tp(size, 0, weight, ls).measureText(t)
    val CTR = Layout.Alignment.ALIGN_CENTER
    val OPP = Layout.Alignment.ALIGN_OPPOSITE

    /** Значок предмета. */
    fun icon(x: Slot, px: Float, py: Float, size: Float) {
        val v = sub(x); val emoji = v.emoji
        val dst = RectF(px, py, px + size, py + size)
        val target = max(8, (size * sc).roundToInt())
        if (emoji.startsWith("img:")) {
            val bmp = Files.iconBitmap(emoji.removePrefix("img:"))
            if (bmp != null) {
                val sh = BitmapShader2(bmp, dst)
                p.reset(); p.isAntiAlias = true; p.shader = sh
                c.drawRoundRect(dst, size * .28f, size * .28f, p); p.shader = null
                return
            }
        }
        val key = IconRes.key(emoji)
        if (key == null) {
            Files.emojiFile(s.iconSet, emoji)?.let { f -> Files.bitmap(f)?.let { c.drawBitmap(it, null, dst, fp); return } }
        } else {
            val res = when (s.iconSet) { "fluent" -> IconRes.fluent[key]; "twemoji" -> IconRes.twemoji[key]; "mono" -> IconRes.mono[key]; else -> null }
            if (res != null) {
                val tint = if (s.iconSet == "mono") when (s.iconTint) { "accent" -> P.acc; "bw" -> P.tx; else -> v.color.toInt() } else null
                drawableBitmap(ctx, res, target, tint)?.let { c.drawBitmap(it, null, dst, fp); return }
            }
        }
        // системный эмодзи
        val q = tp(size * .82f, Color.WHITE, 400)
        val w = q.measureText(emoji)
        c.drawText(emoji, px + (size - w) / 2, py + size * .8f, q)
    }
    private fun BitmapShader2(b: Bitmap, dst: RectF): android.graphics.BitmapShader {
        val sh = android.graphics.BitmapShader(b, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val m = Matrix(); val side = min(b.width, b.height).toFloat()
        val sc2 = dst.width() / side
        m.postTranslate(-(b.width - side) / 2, -(b.height - side) / 2); m.postScale(sc2, sc2); m.postTranslate(dst.left, dst.top)
        sh.setLocalMatrix(m); return sh
    }

    /* ---- фон виджета ---- */
    fun background(bgOverride: Int? = null, capsule: Boolean = false) {
        val r = if (capsule) min(W, H) / 2 else P.radius
        val rect = RectF(0f, 0f, W, H)
        p.reset(); p.isAntiAlias = true
        val fill = P.fill
        when {
            P.kind == "pixel" -> {
                p.color = if (fill != null) (if (bgOverride != null) mix(fill or 0xFF000000.toInt(), P.tx, .1f) else fill) else (bgOverride ?: 0xFF1E2530.toInt())
                c.drawRoundRect(rect, r, r, p)
            }
            fill != null -> { p.color = fill; c.drawRoundRect(rect, r, r, p) }
            P.kind == "color" -> {
                p.shader = LinearGradient(0f, 0f, W * .35f, H, 0xEB2E3432.toInt(), 0xEB141816.toInt(), Shader.TileMode.CLAMP)
                c.drawRoundRect(rect, r, r, p); p.shader = null
            }
            P.kind == "ios" -> { p.color = 0xC41E1E24.toInt(); c.drawRoundRect(rect, r, r, p) }
            P.kind == "origin" -> {
                p.shader = LinearGradient(0f, 0f, 0f, H, 0xE02E2926.toInt(), 0xE0201C1A.toInt(), Shader.TileMode.CLAMP)
                c.drawRoundRect(rect, r, r, p); p.shader = null
            }
            else -> { p.color = 0xE01C1A28.toInt(); c.drawRoundRect(rect, r, r, p) }
        }
        // кромка и очень слабый блик — только у ColorOS, iOS 26 и OriginOS
        if (P.kind == "color" || P.kind == "ios" || P.kind == "origin") {
            p.reset(); p.isAntiAlias = true
            p.shader = LinearGradient(0f, 0f, 0f, H * .34f, 0x14FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
            c.drawRoundRect(rect, r, r, p)
            p.reset(); p.isAntiAlias = true; p.style = Paint.Style.STROKE; p.strokeWidth = 1f; p.color = 0x42FFFFFF
            c.drawRoundRect(RectF(.5f, .5f, W - .5f, H - .5f), r - .5f, r - .5f, p)
            p.reset(); p.isAntiAlias = true; p.style = Paint.Style.STROKE; p.strokeWidth = 1.6f
            p.shader = LinearGradient(0f, 0f, 0f, min(H * .3f, 34f), 0x5CFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
            c.drawRoundRect(RectF(.8f, .8f, W - .8f, H - .8f), r - .8f, r - .8f, p)
        }
    }

    fun bar(x: Float, y: Float, w: Float, h: Float, fr: Float, fill: Int, track: Int) {
        rr(x, y, w, h, h / 2, track)
        if (fr > 0f) rr(x, y, max(h, w * fr), h, h / 2, fill)
    }

    fun ring(cx: Float, cy: Float, r: Float, sw: Float, fr: Float, fg: Int, track: Int) {
        p.reset(); p.isAntiAlias = true; p.style = Paint.Style.STROKE; p.strokeWidth = sw; p.strokeCap = Paint.Cap.ROUND
        val rect = RectF(cx - r, cy - r, cx + r, cy + r)
        p.color = track; c.drawArc(rect, 0f, 360f, false, p)
        if (fr > 0f) { p.color = fg; c.drawArc(rect, -90f, 360f * fr.coerceIn(.03f, 1f), false, p) }
    }

    fun cookie(x: Float, y: Float, w: Float, h: Float, color: Int) {
        // «облачко» Material 3 Expressive: путь из макета 96×100
        val path = PathParser.createPathFromPathData(
            "M48 2C56 2 58 9 65 11C72 13 78 9 84 15C90 21 85 27 87 34C89 41 96 44 96 52C96 60 89 62 87 69C85 76 90 82 84 88C78 94 72 89 65 91C58 93 56 98 48 98C40 98 38 93 31 91C24 89 18 94 12 88C6 82 11 76 9 69C7 62 0 60 0 52C0 44 7 41 9 34C11 27 6 21 12 15C18 9 24 13 31 11C38 9 40 2 48 2Z")
        val m = Matrix(); m.setScale(w / 96f, h / 100f); m.postTranslate(x, y); path.transform(m)
        p.reset(); p.isAntiAlias = true; p.color = color; c.drawPath(path, p)
    }

    /* ---- пустое состояние (нет уроков сегодня) ---- */
    fun idleView() {
        val I = Sched.idle(n, lang)
        if (size == "2x2" && showNextDay) { idleList(I); return }
        val title = if (cfg.idleNoLessons) I.title else I.day
        val subT = if (cfg.idleNoLessons) I.day else ""
        if (size == "2x1" || size == "4x2") {
            val ex = f(16f); val w2 = W - ex * 2
            val h1 = th(title, w2, f(15f), 700); val h2 = th(subT, w2, f(12f))
            var y = (H - h1 - h2) / 2
            y += text(title, ex, y, w2, f(15f), P.tx, 700)
            text(subT, ex, y, w2, f(12f), P.tx2)
            return
        }
        val w2 = W - f(20f); val cx = W / 2
        val fs = if (size == "1x2") f(12f) else f(15f)
        val h1 = th(title, w2, fs, 700, 2); val h2 = th(subT, w2, f(11f), 400, 2)
        var y = (H - h1 - h2 - f(2f)) / 2
        y += text(title, cx - w2 / 2, y, w2, fs, P.tx, 700, 2, CTR)
        text(subT, cx - w2 / 2, y + f(2f), w2, f(11f), P.tx2, 400, 2, CTR)
    }

    /** 2×2 без уроков сегодня: заголовок и все уроки следующего учебного дня. */
    fun idleList(I: Sched.IdleInfo) {
        val pd = f(14f); val w2 = W - pd * 2
        val head = if (cfg.idleNoLessons) join(I.title, I.day) else I.day
        val hh = text(if (P.kind == "ios") up(head) else head, pd, pd, w2, f(12f), P.accT, 700, 2,
            Layout.Alignment.ALIGN_NORMAL, if (P.kind == "ios") .02f else 0f)
        val top = pd + hh + f(8f)
        val rowH = f(24f)
        val avail = ((H - top - pd) / rowH).toInt().coerceAtLeast(1)
        val items = if (nd.size <= avail) nd else nd.take(avail - 1)
        val more = nd.size - items.size
        items.forEachIndexed { i, sl ->
            val cy = top + rowH * i + rowH / 2
            circle(pd + f(3f), cy, f(3f), sub(sl).color.toInt())
            val tx = pd + f(12f)
            base(hm(sl.start), tx, midBase(cy, f(12f)), f(12f), P.tx2, 600)
            val nx = tx + f(40f)
            val r = roomShort(nd, nd.indexOf(sl))
            val rw = if (r.isNotEmpty()) bigW(r, f(11f), 400) + f(6f) else 0f
            val nw = W - pd - rw - nx
            val h = th(name(sl), nw, f(13f), 700)
            text(name(sl), nx, cy - h / 2, nw, f(13f), P.tx, 700)
            if (r.isNotEmpty()) base(r, W - pd, midBase(cy, f(11f)), f(11f), P.tx2, 400, 0f, true)
        }
        if (more > 0) {
            val cy = top + rowH * items.size + rowH / 2
            base("+$more", pd + f(12f), midBase(cy, f(11f)), f(11f), P.tx2, 400)
        }
    }

    /* ---- список уроков для больших виджетов ---- */
    class Lst(val items: List<Slot>, val first: Int, val hl: Int, val today: Boolean)
    fun listFor(cap: Int): Lst {
        val src = if (showToday) L else nd
        val hl = if (showToday) when (n.phase) { Phase.LESSON -> n.cur; Phase.BREAK, Phase.BEFORE -> n.next; else -> -1 } else -1
        val first = if (src.size <= cap) 0 else if (hl >= 0) hl.coerceIn(0, src.size - cap) else 0
        return Lst(src.subList(first, min(src.size, first + cap)), first, hl, showToday)
    }
    fun listHeader(): String = if (showToday) Tr.t(lang, "today") else n.nextDay?.let { Sched.dayLabel(lang, n.date, it) } ?: ""

    /* ================================ точка входа ================================ */
    fun draw() {
        val listy = size == "2x4" || size == "4x2"
        val empty = if (listy) !showToday && !showNextDay else pick == null
        val capsule = P.kind == "pixel" && (size == "2x1" || size == "1x2")
        val bgOv: Int? = if (P.kind == "pixel") when (size) {
            "2x2", "2x1" -> P.card
            "1x2" -> if (P.fill != null) P.card else 0xFF334A6B.toInt()
            else -> null
        } else null
        background(bgOv, capsule)
        if (empty) { idleView(); return }
        when (P.kind) {
            "color" -> when (size) { "2x2" -> color22(); "2x1" -> color21(); "1x2" -> color12(); "2x4" -> color24(); else -> color42() }
            "ios" -> when (size) { "2x2" -> ios22(); "2x1" -> ios21(); "1x2" -> ios12(); "2x4" -> ios24(); else -> ios42() }
            "pixel" -> when (size) { "2x2" -> pixel22(); "2x1" -> pixel21(); "1x2" -> pixel12(); "2x4" -> pixel24(); else -> pixel42() }
            "origin" -> when (size) { "2x2" -> origin22(); "2x1" -> origin21(); "1x2" -> origin12(); "2x4" -> origin24(); else -> origin42() }
            else -> when (size) { "2x2" -> oneui22(); "2x1" -> oneui21(); "1x2" -> oneui12(); "2x4" -> oneui24(); else -> oneui42() }
        }
    }

    /** строка «каб. 215 · до 08:45» */
    fun metaOf(x: Slot): String {
        val i = L.indexOf(x)
        return join(room(L, i), if (isCur) Tr.t(lang, "till", hm(x.end)) else Tr.t(lang, "from", hm(x.start)))
    }

    /* ================================ ColorOS 17 ================================ */
    fun color22() {
        val x = fSlot ?: return
        val pv = f(16f); val ph = f(14f)
        icon(x, pv, ph, f(44f))
        val mins = minsOf(x)
        if (mins > 0 || isCur) {
            val u = " $mn"
            val uw = bigW(u, f(13f), 700); val bw = bigW("$mins", f(30f), 800)
            val nby = topBase(ph, f(30f))
            base("$mins", W - pv - uw - bw, nby, f(30f), P.acc, 800)
            base(u, W - pv - uw, nby, f(13f), P.acc, 700)
        }
        val bx = pv; val bwid = W - pv * 2; val bh = f(8f)
        val by = H - ph - bh
        if (isCur) bar(bx, by, bwid, bh, frac(x), P.acc, a(Color.WHITE, .12f))
        else rr(bx, by, bwid, bh, bh / 2, a(Color.WHITE, .12f))
        val meta = metaOf(x)
        val mh = th(meta, bwid, f(12f), 600)
        val my = by - f(10f) - mh
        text(meta, bx, my, bwid, f(12f), P.tx2, 600)
        val titleH = th(name(x), bwid, f(19f), 800, 2)
        text(name(x), bx, my - f(4f) - titleH, bwid, f(19f), P.tx, 800, 2)
    }

    fun color42() {
        val pv = f(16f); val ph = f(14f)
        val lst = listFor(4)
        val head = (if (lst.today) "● " + dayWord() + " · " + lessonsWord(L) else "● " + listHeader() + " · " + lessonsWord(nd))
        val sideT = if (lst.hl >= 0 && lst.today) "${lst.hl + 1} / ${L.size}" else ""
        val hh = th(head, W, f(12f), 700)
        text(head, pv, ph, W - pv * 2 - f(60f), f(12f), P.acc, 700)
        if (sideT.isNotEmpty()) text(sideT, W - pv - f(60f), ph, f(60f), f(12f), P.tx2, 600, 1, OPP)
        val top = ph + hh + f(10f); val bot = H - ph
        val cnt = lst.items.size; val gap = f(8f)
        val avail = W - pv * 2 - gap * (cnt - 1)
        val wts = lst.items.mapIndexed { i, _ -> if (lst.first + i == lst.hl) 1.18f else 1f }
        val tw = wts.sum()
        var x = pv
        lst.items.forEachIndexed { i, sl ->
            val w = avail * wts[i] / tw
            val sel = lst.first + i == lst.hl
            rr(x, top, w, bot - top, f(18f), if (sel) P.cardSel else P.card)
            val ip = f(8f)
            var y = top + f(10f)
            y += text(hm(sl.start), x + ip, y, w - ip * 2, f(11f), if (sel) P.acc else P.tx2, 700) + f(4f)
            icon(sl, x + ip, y, f(30f)); y += f(34f)
            text(name(sl), x + ip, y, w - ip * 2, f(13f), P.tx, 800, 2)
            val r = room(lst.items, i)
            val rh = th(r, w - ip * 2, f(11f), 400, 2)
            text(r, x + ip, bot - f(10f) - rh, w - ip * 2, f(11f), P.tx2, 400, 2)
            x += w + gap
        }
    }

    fun color24() {
        val pv = f(12f)
        val head = listHeader()
        val hh = text(head, pv + f(4f), pv + f(2f), W, f(12f), P.acc, 700)
        val top = pv + f(2f) + hh + f(6f)
        val cap = max(1, ((H - top - pv) / f(46f)).toInt())
        val lst = listFor(cap)
        val rowH = (H - top - pv) / lst.items.size
        lst.items.forEachIndexed { i, sl ->
            val y = top + rowH * i
            val sel = lst.first + i == lst.hl
            if (sel) rr(pv, y, W - pv * 2, rowH, f(16f), P.cardSel)
            val isz = min(f(32f), rowH - f(8f))
            icon(sl, pv + f(8f), y + (rowH - isz) / 2, isz)
            val tx = pv + f(8f) + isz + f(10f); val tw = W - tx - pv - f(6f)
            val h1 = th(name(sl), tw, f(14f), 700); val subt = join(if (cfg.time) hm(sl.start) else "", roomShort(lst.items, i))
            val h2 = th(subt, tw, f(11.5f))
            var yy = y + (rowH - h1 - h2) / 2
            yy += text(name(sl), tx, yy, tw, f(14f), P.tx, 700)
            text(subt, tx, yy, tw, f(11.5f), P.tx2)
        }
    }

    fun color12() {
        val x = slot12() ?: return
        val pv = f(8f); val w = W - pv * 2
        var y = f(12f)
        y += text(lab12(x), pv, y, w, f(12f), P.acc, 700, 1, CTR) + f(8f)
        val isz = min(f(40f), W - f(20f)); icon(x, (W - isz) / 2, y, isz); y += isz + f(8f)
        text(name(x), pv, y, w, f(13f), P.tx, 800, 2, CTR)
        val r = room(L, L.indexOf(x))
        val t1 = if (cfg.time) hm(x.start) else ""
        val h2 = th(r, w, f(11.5f), 600); val h1 = th(t1, w, f(11.5f), 600)
        text(r, pv, H - f(12f) - h2, w, f(11.5f), P.tx2, 600, 1, CTR)
        text(t1, pv, H - f(12f) - h2 - h1, w, f(11.5f), P.tx2, 600, 1, CTR)
    }

    fun color21() {
        val x = fSlot ?: return
        val pv = f(14f)
        val isz = min(f(40f), H - f(24f)); icon(x, pv, (H - isz) / 2, isz)
        val tx = pv + isz + f(12f); val tw = W - tx - pv
        val k1 = if (isCur) join(Tr.t(lang, "now"), "${n.minutesLeft} $mn") else join(label, if (minsOf(x) > 0) "${minsOf(x)} $mn" else "")
        val h1 = th(k1, tw, f(12f), 700); val h2 = th(name(x), tw, f(15f), 800)
        var y = (H - h1 - h2) / 2
        y += text(k1, tx, y, tw, f(12f), P.acc, 700)
        text(name(x), tx, y, tw, f(15f), P.tx, 800)
    }

    /* ================================ iOS 26 ================================ */
    fun iosBars(items: List<Slot>, first: Int, x: Float, top: Float, bot: Float, w: Float, fs: Float, gap: Float, hl: Int) {
        val rowH = (bot - top - gap * (items.size - 1)) / items.size
        items.forEachIndexed { i, sl ->
            val y = top + (rowH + gap) * i
            val col = sub(sl).color.toInt()
            val bh = min(rowH - f(6f), f(46f))
            rr(x, y + (rowH - bh) / 2, f(4f), bh, f(2f), col)
            val tx = x + f(11f); val tw = w - f(11f)
            val line2 = join(if (cfg.time) hm(sl.start) + "–" + hm(sl.end) else "", roomShort(items, i))
            val h1 = th(name(sl), tw, fs, 700); val h2 = th(line2, tw, f(11f), 500)
            var yy = y + (rowH - h1 - h2) / 2
            yy += text(name(sl), tx, yy, tw, fs, P.tx, 700)
            text(line2, tx, yy, tw, f(11f), P.tx2, 500)
        }
    }

    fun ios22() {
        val x = fSlot ?: return
        val pd = f(14f); val w = W - pd * 2
        val i = L.indexOf(x)
        val head = up(join(label, roomShort(L, i)))
        var y = pd
        y += text(head, pd, y, w, f(11f), P.accT, 700, 1, Layout.Alignment.ALIGN_NORMAL, .02f) + f(4f)
        text(name(x), pd, y, w, f(20f), P.tx, 700, 2)
        val nxt = L.getOrNull(i + 1)
        val nextT = if (nxt != null) Tr.t(lang, "next") + ": " + name(nxt) else ""
        var by = H - pd
        if (nextT.isNotEmpty()) { val h = th(nextT, w, f(12f), 500); text(nextT, pd, by - h, w, f(12f), P.tx2, 500); by -= h + f(8f) }
        val mins = minsOf(x)
        val ringR = f(18f)
        val sz = f(34f)
        if (mins > 0 || isCur) {
            val bw = base("$mins", pd, by - f(2f), sz, P.tx, 600)
            base(" $mn", pd + bw, by - f(2f), f(12f), P.tx2, 500)
        }
        if (isCur) ring(W - pd - ringR - f(2.5f), by - ringR - f(2.5f), ringR, f(5f), frac(x), P.acc, a(Color.WHITE, .2f))
    }

    fun ios42() {
        val pd = f(14f)
        val lst = listFor(4)
        val lw = f(110f)
        var y = pd
        val shownDate = if (showToday) n.date else (n.nextDay ?: n.date)
        y += text(up(Tr.dayLong(lang, shownDate.dayOfWeek.value - 1)), pd, y, lw, f(11f), P.accT, 700, 1, Layout.Alignment.ALIGN_NORMAL, .02f)
        base("${shownDate.dayOfMonth}", pd, topBase(y + f(2f), f(44f)), f(44f), P.tx, 300)
        val x = fSlot
        if (x != null) {
            val h3 = if (isCur) th(Tr.t(lang, "left", n.minutesLeft), lw, f(12f), 500) else if (minsOf(x) > 0) th(Tr.t(lang, "startsIn", minsOf(x)), lw, f(12f), 500) else 0f
            var by = H - pd
            if (isCur) text(Tr.t(lang, "left", n.minutesLeft), pd, by - h3, lw, f(12f), P.tx2, 500)
            else if (minsOf(x) > 0) text(Tr.t(lang, "startsIn", minsOf(x)), pd, by - h3, lw, f(12f), P.tx2, 500)
            by -= h3
            val h2 = th(name(x), lw, f(15f), 700, 2)
            text(name(x), pd, by - h2 - f(2f), lw, f(15f), P.tx, 700, 2)
            by -= h2 + f(2f)
            val h1 = th(label, lw, f(12f), 500)
            text(label, pd, by - h1, lw, f(12f), P.tx2, 500)
        }
        val lx = pd + lw + f(14f)
        iosBars(lst.items, lst.first, lx, pd, H - pd, W - lx - pd, f(13f), 0f, lst.hl)
    }

    fun ios24() {
        val pd = f(14f); val w = W - pd * 2
        val head = if (showToday) up(join(Tr.t(lang, "today"), lessonsWord(L))) else up(listHeader())
        var y = pd
        y += text(head, pd, y, w, f(11f), P.accT, 700, 1, Layout.Alignment.ALIGN_NORMAL, .02f) + f(8f)
        val cap = max(1, ((H - y - pd) / f(54f)).toInt())
        val lst = listFor(cap)
        iosBars(lst.items, lst.first, pd, y, H - pd, w, f(14f), f(6f), lst.hl)
    }

    fun ios12() {
        val x = slot12() ?: return
        val pv = f(12f); val ph = f(10f); val w = W - ph * 2
        var y = pv
        y += text(up(lab12(x)), ph, y, w, f(11f), P.accT, 700, 1, Layout.Alignment.ALIGN_NORMAL, .02f) + f(4f)
        text(hm(x.start), ph, y, w, f(24f), P.tx, 600)
        val r = room(L, L.indexOf(x))
        val h2 = th(r, w, f(12f), 500); val h1 = th(name(x), w, f(13f), 700, 2)
        text(r, ph, H - pv - h2, w, f(12f), P.tx2, 500)
        text(name(x), ph, H - pv - h2 - h1 - f(1f), w, f(13f), P.tx, 700, 2)
    }

    fun ios21() {
        val x = fSlot ?: return
        val pd = f(14f)
        val bh = min(f(52f), H - f(24f))
        rr(pd, (H - bh) / 2, f(4f), bh, f(2f), sub(x).color.toInt())
        val tx = pd + f(14f); val tw = W - tx - pd
        val k1 = up(if (isCur) join(Tr.t(lang, "now"), Tr.t(lang, "left", n.minutesLeft)) else join(label, if (minsOf(x) > 0) Tr.t(lang, "startsIn", minsOf(x)) else ""))
        val h1 = th(k1, tw, f(11f), 700); val h2 = th(name(x), tw, f(15f), 700)
        var y = (H - h1 - h2) / 2
        y += text(k1, tx, y, tw, f(11f), P.accT, 700, 1, Layout.Alignment.ALIGN_NORMAL, .02f)
        text(name(x), tx, y, tw, f(15f), P.tx, 700)
    }

    /* ================================ Pixel ================================ */
    fun pixel22() {
        val x = fSlot ?: return
        val pd = f(14f); val w = W - pd * 2
        val ckW = min(f(96f), W * .55f); val ckH = ckW * 100f / 96f
        val ckX = (W - ckW) / 2
        cookie(ckX, pd, ckW, ckH, P.accT)
        val mins = minsOf(x)
        val big = 34f * ckW / 96f
        val numT = if (mins > 0 || isCur) "$mins" else hm(x.start)
        val ns = if (mins > 0 || isCur) big else big * .7f
        val unitT = if (mins > 0 || isCur) mn else ""
        val nh = ns * .72f; val uh = if (unitT.isNotEmpty()) f(12f) * 1.15f else 0f
        val cy = pd + ckH / 2
        val top = cy - (nh + uh) / 2 + f(1f)
        base(numT, W / 2 - bigW(numT, ns, 700) / 2, top + nh, ns, P.ink, 700)
        if (unitT.isNotEmpty()) base(unitT, W / 2 - bigW(unitT, f(12f), 600) / 2, top + nh + uh, f(12f), P.ink, 600)
        var y = pd + ckH + f(10f)
        y += text(name(x), pd, y, w, f(15f), P.tx, 600, 2, CTR) + f(2f)
        text(room(L, L.indexOf(x)), pd, y, w, f(12f), P.tx2, 400, 1, CTR)
    }

    fun pixel42() {
        val pd = f(14f)
        val lst = listFor(4)
        val head = if (lst.today) Tr.dayShort(lang, n.date.dayOfWeek.value - 1).let { it.replaceFirstChar { c0 -> c0.uppercase() } } + ", " + Tr.dateShort(lang, n.date) else listHeader()
        val rightT = lessonsWord(if (lst.today) L else nd)
        text(head, pd, pd, W / 2, f(12f), P.accT, 600, 1, Layout.Alignment.ALIGN_NORMAL, .02f)
        text(rightT, W / 2, pd, W / 2 - pd, f(12f), P.tx2, 400, 1, OPP)
        val top = pd + f(12f) * 1.2f + f(10f); val bot = H - pd
        val cnt = lst.items.size; val gap = f(4f)
        val cw = (W - pd * 2 - gap * (cnt - 1)) / cnt
        lst.items.forEachIndexed { i, sl ->
            val sel = lst.first + i == lst.hl
            val x = pd + (cw + gap) * i
            rr(x, top, cw, bot - top, if (sel) f(20f) else f(12f), if (sel) P.cardSel else P.card)
            val ip = f(8f); val tc = if (sel) P.ink else P.tx
            var y = top + f(10f)
            y += text(hm(sl.start), x + ip, y, cw - ip * 2, f(12f), a(tc, .8f), 700) + f(4f)
            icon(sl, x + ip, y, f(28f)); y += f(32f)
            text(name(sl), x + ip, y, cw - ip * 2, f(13f), tc, 600, 2)
            val r = roomShort(lst.items, i)
            val rh = th(r, cw - ip * 2, f(11f))
            text(r, x + ip, bot - f(10f) - rh, cw - ip * 2, f(11f), a(tc, .75f), 400)
        }
    }

    fun pixel24() {
        val pd = f(12f)
        val hh = text(listHeader(), pd + f(4f), pd + f(2f), W, f(12f), P.accT, 600, 1, Layout.Alignment.ALIGN_NORMAL, .02f)
        val top = pd + f(2f) + hh + f(6f)
        val gap = f(3f)
        val cap = max(1, ((H - top - pd) / f(44f)).toInt())
        val lst = listFor(cap)
        val cnt = lst.items.size
        val rowH = (H - top - pd - gap * (cnt - 1)) / cnt
        lst.items.forEachIndexed { i, sl ->
            val y = top + (rowH + gap) * i
            val sel = lst.first + i == lst.hl
            val big = f(18f); val sm = f(6f)
            val tl = if (i == 0) big else sm; val bl = if (i == cnt - 1) big else sm
            rrx(pd, y, W - pd * 2, rowH, tl, tl, bl, bl, if (sel) P.cardSel else P.card)
            val tc = if (sel) P.ink else P.tx
            val isz = min(f(28f), rowH - f(8f))
            val ix = pd + f(12f)
            icon(sl, ix, y + (rowH - isz) / 2, isz)
            val tx = ix + isz + f(10f); val tw = W - tx - pd - f(10f)
            val subt = join(if (cfg.time) hm(sl.start) else "", roomShort(lst.items, i))
            val h1 = th(name(sl), tw, f(13.5f), 600); val h2 = th(subt, tw, f(11f))
            var yy = y + (rowH - h1 - h2) / 2
            yy += text(name(sl), tx, yy, tw, f(13.5f), tc, 600)
            text(subt, tx, yy, tw, f(11f), a(tc, .75f))
        }
    }

    fun pixel12() {
        val x = slot12() ?: return
        val ph = f(8f); val w = W - ph * 2
        var y = f(18f)
        y += text(lab12(x), ph, y, w, f(12f), P.accT, 600, 1, CTR, .02f) + f(8f)
        val isz = min(f(36f), W - f(24f)); icon(x, (W - isz) / 2, y, isz); y += isz + f(8f)
        text(name(x), ph, y, w, f(12.5f), P.tx, 600, 2, CTR)
        val t = hm(x.start); val h = th(t, w, f(12f))
        text(t, ph, H - f(18f) - h, w, f(12f), P.tx2, 400, 1, CTR)
    }

    fun pixel21() {
        val x = fSlot ?: return
        val bs = min(f(60f), H - f(16f)); val pl = f(12f)
        rr(pl, (H - bs) / 2, bs, bs, bs * .36f, P.cardSel)
        val mins = minsOf(x)
        val t = if (mins > 0 || isCur) "$mins" else hm(x.start)
        val fs = if (mins > 0 || isCur) f(20f) else f(15f)
        base(t, pl + bs / 2 - bigW(t, fs, 700) / 2, midBase(H / 2, fs), fs, P.ink, 700)
        val tx = pl + bs + f(12f); val tw = W - tx - f(18f)
        val h1 = th(label, tw, f(12f), 600); val h2 = th(name(x), tw, f(15f), 600)
        var y = (H - h1 - h2) / 2
        y += text(label, tx, y, tw, f(12f), P.accT, 600, 1, Layout.Alignment.ALIGN_NORMAL, .02f)
        text(name(x), tx, y, tw, f(15f), P.tx, 600)
    }

    /* ================================ OriginOS 7 ================================ */
    fun origin22() {
        val x = fSlot ?: return
        val pd = f(16f); val w = W - pd * 2
        var y = pd
        y += text(label, pd, y, w, f(12f), P.accT, 600) + f(2f)
        val mins = minsOf(x)
        val ns = f(64f)
        if (mins > 0 || isCur) {
            val bw = base("$mins", pd, topBase(y + f(4f), ns), ns, P.tx, 200, -.03f)
            base(" $mn", pd + bw + f(2f), topBase(y + f(4f), ns), f(12f), P.tx2, 400)
        } else base(hm(x.start), pd, topBase(y + f(4f), ns * .7f), ns * .7f, P.tx, 200, -.03f)
        val isz = f(38f)
        val tx = pd + isz + f(10f); val tw = w - isz - f(10f)
        val r = room(L, L.indexOf(x))
        val h1 = th(name(x), tw, f(14f), 700, 2); val h2 = th(r, tw, f(12f))
        val blk = max(isz, h1 + h2)
        icon(x, pd, H - pd - blk + (blk - isz) / 2, isz)
        var yy = H - pd - blk + (blk - h1 - h2) / 2
        yy += text(name(x), tx, yy, tw, f(14f), P.tx, 700, 2)
        text(r, tx, yy, tw, f(12f), P.tx2)
    }

    fun origin42() {
        val x = fSlot
        val pd = f(16f)
        val lw = f(150f).coerceAtMost(W * .42f)
        var y = pd
        val rest: List<Slot>
        if (x != null) {
            val i = L.indexOf(x)
            icon(x, pd, y, f(52f)); y += f(52f) + f(8f)
            y += text(join(label, roomShort(L, i).let { if (it.isNotBlank()) Tr.t(lang, "room", it) else "" }), pd, y, lw, f(12f), P.accT, 600) + f(1f)
            text(name(x), pd, y, lw, f(18f), P.tx, 700, 2)
            val mins = minsOf(x)
            if (mins > 0 || isCur) {
                val bw = base("$mins", pd, H - pd - f(2f), f(46f), P.tx, 200, -.03f)
                base(" $mn", pd + bw + f(2f), H - pd - f(2f), f(12f), P.tx2, 400)
            }
            rest = L.drop(i + 1).take(4)
        } else {
            val dn = n.nextDay?.dayOfMonth?.toString() ?: ""
            base(dn, pd, topBase(pd, f(52f)), f(52f), P.tx, 200, -.03f)
            y += f(52f) + f(10f)
            y += text(listHeader(), pd, y, lw, f(14f), P.accT, 700, 2) + f(2f)
            text(lessonsWord(nd), pd, y, lw, f(12f), P.tx2)
            rest = nd.take(4)
        }
        val dx = pd + lw + f(16f)
        p.reset(); p.color = a(Color.WHITE, .12f); c.drawRect(dx, pd + f(6f), dx + 1f, H - pd - f(6f), p)
        val rx = dx + f(14f); val rw = W - rx - pd
        if (rest.isNotEmpty()) {
            val rowH = (H - pd * 2) / rest.size
            rest.forEachIndexed { j, sl ->
                val yy = pd + rowH * j
                val isz = min(f(26f), rowH - f(6f))
                icon(sl, rx, yy + (rowH - isz) / 2, isz)
                val t = hm(sl.start); val tw = bigW(t, f(12f), 400)
                if (cfg.time) base(t, W - pd, midBase(yy + rowH / 2, f(12f)), f(12f), P.tx2, 400, 0f, true)
                val nx = rx + isz + f(8f); val nw = rw - isz - f(8f) - (if (cfg.time) tw + f(8f) else 0f)
                val h = th(name(sl), nw, f(13f), 700)
                text(name(sl), nx, yy + (rowH - h) / 2, nw, f(13f), P.tx, 700)
            }
        }
    }

    fun origin24() {
        val pd = f(14f)
        val ds = f(40f)
        val dn = if (showToday) "${n.date.dayOfMonth}" else n.nextDay?.let { "${it.dayOfMonth}" } ?: ""
        val dayT = if (showToday) dayWord().lowercase() else listHeader().lowercase()
        val by = topBase(pd, ds)
        val dw = base(dn, pd, by, ds, P.tx, 200, -.03f)
        base(dayT, pd + dw + f(6f), by, f(12f), P.accT, 600)
        val top = pd + ds * 1.05f + f(4f)
        val cap = max(1, ((H - top - pd) / f(48f)).toInt())
        val lst = listFor(cap)
        val rowH = (H - top - pd) / lst.items.size
        lst.items.forEachIndexed { i, sl ->
            val y = top + rowH * i
            if (i > 0) { p.reset(); p.color = a(Color.WHITE, .08f); c.drawRect(pd, y, W - pd, y + 1f, p) }
            val sel = lst.first + i == lst.hl
            base("${lst.first + i + 1}", pd, midBase(y + rowH / 2, f(12f)), f(12f), if (sel) P.accT else P.tx3, 700)
            val isz = min(f(30f), rowH - f(8f))
            icon(sl, pd + f(24f), y + (rowH - isz) / 2, isz)
            val tx = pd + f(24f) + isz + f(10f); val tw = W - tx - pd
            val h1 = th(name(sl), tw, f(13.5f), 700); val t = if (cfg.time) hm(sl.start) else ""
            val h2 = th(t, tw, f(12f))
            var yy = y + (rowH - h1 - h2) / 2
            yy += text(name(sl), tx, yy, tw, f(13.5f), P.tx, 700)
            text(t, tx, yy, tw, f(12f), P.tx2)
        }
    }

    fun origin12() {
        val x = slot12() ?: return
        val pd = f(8f); val w = W - pd * 2
        var y = f(14f)
        val isz = min(f(44f), W - f(24f)); icon(x, (W - isz) / 2, y, isz); y += isz + f(8f)
        y += text(lab12(x), pd, y, w, f(12f), P.accT, 600, 1, CTR) + f(1f)
        text(name(x), pd, y, w, f(13f), P.tx, 700, 2, CTR)
        val t = hm(x.start); val ts = f(26f).coerceAtMost(W * .31f)
        base(t, W / 2 - bigW(t, ts, 200, -.03f) / 2, H - f(14f), ts, P.tx, 200, -.03f)
    }

    fun origin21() {
        val x = fSlot ?: return
        val pd = f(16f)
        val isz = min(f(42f), H - f(24f)); icon(x, pd, (H - isz) / 2, isz)
        val mins = minsOf(x)
        val ns = f(36f)
        val numT = if (mins > 0 || isCur) "$mins" else ""
        val nw = if (numT.isNotEmpty()) bigW(numT, ns, 200, -.03f) else 0f
        if (numT.isNotEmpty()) base(numT, W - pd - nw, midBase(H / 2, ns), ns, P.tx, 200, -.03f)
        val tx = pd + isz + f(12f); val tw = W - tx - pd - nw - f(8f)
        val r = room(L, L.indexOf(x))
        val h1 = th(name(x), tw, f(15f), 700); val h2 = th(r, tw, f(12f))
        var y = (H - h1 - h2) / 2
        y += text(name(x), tx, y, tw, f(15f), P.tx, 700)
        text(r, tx, y, tw, f(12f), P.tx2)
    }

    /* ================================ One UI ================================ */
    fun oneHeader(t: String, x: Float, y: Float, w: Float, right: String = ""): Float {
        val q = f(16f)
        rr(x, y, q, q, f(5f), P.acc)
        text(right, x + w - f(70f), y + f(1f), f(70f), f(12f), P.tx2, 600, 1, OPP)
        return max(q, text(t, x + q + f(6f), y + f(1f), w - q - f(6f) - (if (right.isNotEmpty()) f(72f) else 0f), f(12f), a(P.tx, .7f), 600))
    }

    fun oneui22() {
        val x = fSlot ?: return
        val pd = f(16f); val w = W - pd * 2
        var y = pd
        y += oneHeader(schedWord(), pd, y, w) + f(12f)
        val isz = f(30f); icon(x, pd, y, isz)
        text(label, pd + isz + f(8f), y + (isz - f(16f)) / 2, w - isz, f(13f), P.accT, 700)
        y += isz + f(4f)
        text(name(x), pd, y, w, f(18f), P.tx, 700, 2)
        val bh = f(6f); val by = H - pd - bh
        val t1 = hm(x.start); val t2 = hm(x.end); val th1 = th(t1, w, f(12f))
        if (isCur) bar(pd, by, w, bh, frac(x), P.acc, a(Color.WHITE, .15f)) else rr(pd, by, w, bh, bh / 2, a(Color.WHITE, .15f))
        text(t1, pd, by - f(4f) - th1, w, f(12f), P.tx2)
        text(t2, pd, by - f(4f) - th1, w, f(12f), P.tx2, 400, 1, OPP)
    }
    fun schedWord() = when (lang) { "kk" -> "Кесте"; "en" -> "Schedule"; else -> "Расписание" }

    fun oneui42() {
        val pd = f(10f)
        val lst = listFor(4)
        val head = if (lst.today) Tr.t(lang, "today") + ", " + Tr.dateShort(lang, n.date) else listHeader()
        var y = f(14f)
        y += oneHeader(head, pd + f(8f), y, W - pd * 2 - f(16f), lessonsWord(if (lst.today) L else nd)) + f(6f)
        val rowH = (H - y - f(14f)) / lst.items.size
        lst.items.forEachIndexed { i, sl ->
            val yy = y + rowH * i
            val sel = lst.first + i == lst.hl
            if (sel) rr(pd, yy + f(1f), W - pd * 2, rowH - f(2f), f(14f), P.cardSel)
            val cy = yy + rowH / 2
            val ix = pd + f(10f)
            val t = hm(sl.start)
            base(t, ix, midBase(cy, f(12.5f)), f(12.5f), if (sel) P.accT else P.tx2, 600)
            circle(ix + f(46f) + f(14f), cy, f(4f), sub(sl).color.toInt())
            val r = roomShort(lst.items, i); val rw = if (r.isNotEmpty()) bigW(r, f(12f), 400) + f(8f) else 0f
            val nx = ix + f(46f) + f(28f); val nw = W - pd - f(10f) - nx - rw
            val h = th(name(sl), nw, f(14f), 700)
            text(name(sl), nx, cy - h / 2, nw, f(14f), P.tx, 700)
            if (r.isNotEmpty()) base(r, W - pd - f(10f), midBase(cy, f(12f)), f(12f), P.tx2, 400, 0f, true)
        }
    }

    fun oneui24() {
        val pd = f(16f)
        var y = pd
        val head = if (showToday) dayWord() else listHeader()
        y += oneHeader(head, pd, y, W - pd * 2) + f(6f)
        val cap = max(1, ((H - y - pd) / f(46f)).toInt())
        val lst = listFor(cap)
        val rowH = (H - y - pd) / lst.items.size
        lst.items.forEachIndexed { i, sl ->
            val yy = y + rowH * i
            val t = hm(sl.start)
            text(if (cfg.time) t else "", pd, yy + (rowH - th(t, f(40f), f(12f), 600)) / 2, f(44f), f(12f), P.tx2, 600)
            val bx = pd + f(50f)
            val bh = min(rowH - f(8f), f(46f))
            rr(bx, yy + (rowH - bh) / 2, f(3f), bh, f(1.5f), sub(sl).color.toInt())
            val tx = bx + f(11f); val tw = W - tx - pd
            val r = room(lst.items, i)
            val h1 = th(name(sl), tw, f(13.5f), 700); val h2 = th(r, tw, f(12f))
            var y2 = yy + (rowH - h1 - h2) / 2
            y2 += text(name(sl), tx, y2, tw, f(13.5f), P.tx, 700)
            text(r, tx, y2, tw, f(12f), P.tx2)
        }
    }

    fun oneui12() {
        val x = slot12() ?: return
        val ph = f(8f); val w = W - ph * 2
        var y = f(14f)
        y += text(lab12(x), ph, y, w, f(12f), P.accT, 700, 1, CTR) + f(12f)
        val isz = min(f(40f), W - f(24f)); icon(x, (W - isz) / 2, y, isz); y += isz + f(8f)
        text(name(x), ph, y, w, f(13f), P.tx, 700, 2, CTR)
        val t = hm(x.start); val h = th(t, w, f(16f), 700)
        text(t, ph, H - f(14f) - h, w, f(16f), P.accT, 700, 1, CTR)
    }

    fun oneui21() {
        val x = fSlot ?: return
        val pd = f(16f)
        val isz = min(f(40f), H - f(24f)); icon(x, pd, (H - isz) / 2, isz)
        val tx = pd + isz + f(12f); val tw = W - tx - pd
        val k1 = if (isCur) join(Tr.t(lang, "now"), "${n.minutesLeft} $mn") else join(label, if (minsOf(x) > 0) "${minsOf(x)} $mn" else "")
        val h1 = th(k1, tw, f(12f), 700); val h2 = th(name(x), tw, f(15f), 700)
        var y = (H - h1 - h2) / 2
        y += text(k1, tx, y, tw, f(12f), P.accT, 700)
        text(name(x), tx, y, tw, f(15f), P.tx, 700)
    }
}

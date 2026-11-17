package kz.alnazar.schedule.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.LruCache
import androidx.compose.runtime.mutableIntStateOf
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kz.alnazar.schedule.ui.IconRes
import kz.alnazar.schedule.widget.WidgetUpdater
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Картинки, которых нет внутри APK:
 *  • icons/  — иконки своих предметов: картинки из галереи и скачанные Fluent 3D / Twemoji для любых эмодзи;
 *  • photos/ — фото к домашке.
 */
object Files {
    private lateinit var root: File
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cache = LruCache<String, Bitmap>(64)
    private var fluentIndex: Map<String, String>? = null

    /** Меняется после скачивания — экраны перерисовывают плитки. */
    val ver = mutableIntStateOf(0)

    fun init(ctx: Context) { root = ctx.applicationContext.filesDir }

    fun icons(): File = File(root, "icons").apply { mkdirs() }
    fun photos(): File = File(root, "photos").apply { mkdirs() }

    /* ===== Эмодзи вне встроенного набора ===== */

    /** «1f9ea», «1f3f3-200d-1f308» — коды без вариационного селектора FE0F. */
    fun hex(emoji: String): String =
        emoji.codePoints().toArray().filter { it != 0xFE0F }.joinToString("-") { Integer.toHexString(it) }

    fun emojiFile(set: String, emoji: String): File? {
        if (set != "fluent" && set != "twemoji") return null
        val f = File(icons(), (if (set == "fluent") "fl_" else "tw_") + hex(emoji) + ".png")
        return if (f.exists()) f else null
    }

    /** Скачать Fluent 3D и Twemoji для эмодзи, если его нет во встроенном наборе. */
    fun fetchEmoji(ctx: Context, emoji: String) {
        if (emoji.isBlank() || emoji.startsWith("img:") || IconRes.key(emoji) != null) return
        val app = ctx.applicationContext
        scope.launch {
            val h = hex(emoji)
            var got = false
            val fl = File(icons(), "fl_$h.png")
            if (!fl.exists()) {
                val path = index(app)[h]
                if (path != null) {
                    val url = "https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/" +
                        path.split("/").joinToString("/") { URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
                    got = download(url, fl, 128) || got
                }
            }
            val tw = File(icons(), "tw_$h.png")
            if (!tw.exists()) {
                // в Twemoji у последовательностей с ZWJ селектор FE0F иногда остаётся в имени файла
                val full = emoji.codePoints().toArray().joinToString("-") { Integer.toHexString(it) }
                for (name in listOf(h, full).distinct()) {
                    if (download("https://cdn.jsdelivr.net/gh/jdecked/twemoji@latest/assets/72x72/$name.png", tw, 72)) { got = true; break }
                }
            }
            if (got) {
                ver.intValue++
                WidgetUpdater.refresh(app)
            }
        }
    }

    /** Докачать иконки для всех своих предметов (например, после импорта или смены набора). */
    fun fetchAll(ctx: Context, st: AppState) = st.custom.forEach { if (it.img == null) fetchEmoji(ctx, it.emoji) }

    private fun index(ctx: Context): Map<String, String> {
        fluentIndex?.let { return it }
        val m = runCatching {
            ctx.assets.open("fluent_index.tsv").bufferedReader().readLines()
                .mapNotNull { l -> l.split('\t').takeIf { it.size == 2 }?.let { it[0] to it[1] } }.toMap()
        }.getOrDefault(emptyMap())
        fluentIndex = m
        return m
    }

    private fun download(url: String, to: File, size: Int): Boolean = runCatching {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 8000; c.readTimeout = 15000
        try {
            if (c.responseCode != 200) return@runCatching false
            val bmp = c.inputStream.use { BitmapFactory.decodeStream(it) } ?: return@runCatching false
            val out = if (bmp.width > size) Bitmap.createScaledBitmap(bmp, size, size * bmp.height / bmp.width, true) else bmp
            to.outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
            true
        } finally { c.disconnect() }
    }.getOrDefault(false)

    /* ===== Битмапы для экранов и виджетов ===== */

    fun bitmap(f: File): Bitmap? {
        val k = f.path + ":" + f.lastModified()
        cache.get(k)?.let { return it }
        val b = runCatching { BitmapFactory.decodeFile(f.path) }.getOrNull() ?: return null
        cache.put(k, b)
        return b
    }

    /** Уменьшенная копия для превью фото (≈ 320 px). */
    fun thumb(f: File): Bitmap? {
        val k = "t:" + f.path + ":" + f.lastModified()
        cache.get(k)?.let { return it }
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, o)
        if (o.outWidth <= 0) return null
        var sz = 1
        while (maxOf(o.outWidth, o.outHeight) / (sz * 2) >= 320) sz *= 2
        val b = BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sz }) ?: return null
        cache.put(k, b)
        return b
    }

    fun iconBitmap(name: String): Bitmap? = File(icons(), name).takeIf { it.exists() }?.let { bitmap(it) }

    /* ===== Картинки пользователя ===== */

    /** Картинка предмета из галереи: квадрат по центру, 256 px. Возвращает имя файла. */
    fun saveSubjectImage(ctx: Context, uri: Uri): String? = runCatching {
        val src = decodeScaled(ctx, uri, 512) ?: return null
        val side = minOf(src.width, src.height)
        val sq = Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
        val out = Bitmap.createScaledBitmap(sq, 256, 256, true)
        val name = "s_${System.currentTimeMillis()}.png"
        File(icons(), name).outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
        name
    }.getOrNull()

    /** Фото к домашке: длинная сторона до 1600 px, JPEG. Возвращает имя файла. */
    fun savePhoto(ctx: Context, uri: Uri): String? = runCatching {
        val b = decodeScaled(ctx, uri, 1600) ?: return null
        val name = "p_${System.currentTimeMillis()}.jpg"
        File(photos(), name).outputStream().use { b.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        name
    }.getOrNull()

    fun photo(name: String): File = File(photos(), name)

    fun deletePhotos(names: List<String>) = names.forEach { File(photos(), it).delete() }

    private fun decodeScaled(ctx: Context, uri: Uri, max: Int): Bitmap? {
        val cr = ctx.contentResolver
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
        if (o.outWidth <= 0) return null
        var s = 1
        while (maxOf(o.outWidth, o.outHeight) / (s * 2) >= max) s *= 2
        val b = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = s }) } ?: return null
        val rot = runCatching {
            cr.openInputStream(uri)?.use {
                when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        }.getOrDefault(0f)
        val r = if (rot != 0f) Bitmap.createBitmap(b, 0, 0, b.width, b.height, Matrix().apply { postRotate(rot) }, true) else b
        val k = max.toFloat() / maxOf(r.width, r.height)
        return if (k < 1f) Bitmap.createScaledBitmap(r, (r.width * k).toInt(), (r.height * k).toInt(), true) else r
    }
}

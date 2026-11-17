package kz.alnazar.schedule.data

import android.content.Context
import android.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Резервная копия: всё состояние + картинки своих предметов (base64), одним JSON.
 * Фото к домашке не входят — они могут весить десятки мегабайт.
 */
@Serializable
data class BackupFile(val app: String = "SchoolSchedule", val v: Int = 1, val state: AppState, val images: Map<String, String> = emptyMap())

object Backup {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun export(): String {
        val st = Store.state.value
        val imgs = st.custom.mapNotNull { it.img }.distinct().mapNotNull { n ->
            val f = File(Files.icons(), n)
            if (f.exists()) n to Base64.encodeToString(f.readBytes(), Base64.NO_WRAP) else null
        }.toMap()
        return json.encodeToString(BackupFile.serializer(), BackupFile(state = st, images = imgs))
    }

    /** true — данные загружены. Понимает и полный файл бэкапа, и «голое» состояние. */
    fun import(ctx: Context, text: String): Boolean {
        val t = text.trim()
        if (t.isEmpty()) return false
        val b = runCatching { json.decodeFromString(BackupFile.serializer(), t) }.getOrNull()
            ?: runCatching { BackupFile(state = json.decodeFromString(AppState.serializer(), t)) }.getOrNull()
            ?: return false
        b.images.forEach { (n, data) ->
            if (n.matches(Regex("[A-Za-z0-9_.-]+"))) runCatching { File(Files.icons(), n).writeBytes(Base64.decode(data, Base64.NO_WRAP)) }
        }
        // фото, которых нет на этом телефоне, убираем из записей
        val st = b.state.copy(hw = b.state.hw.map { h -> h.copy(photos = h.photos.filter { Files.photo(it).exists() }) })
        Store.update(ctx) { st }
        Files.fetchAll(ctx, st)
        return true
    }
}

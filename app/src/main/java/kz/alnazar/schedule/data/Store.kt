package kz.alnazar.schedule.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import kz.alnazar.schedule.widget.WidgetUpdater
import java.io.File

/**
 * Всё состояние приложения хранится одним JSON-файлом (как в прототипе).
 * Так проще экспорт/импорт, а для расписания одного ученика этого более чем достаточно.
 */
object Store {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = false }
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state
    /** Увеличивается при каждом обновлении виджетов — чтобы они пересчитали «сейчас». */
    val tick = MutableStateFlow(0L)
    @Volatile private var loaded = false

    private fun file(ctx: Context) = File(ctx.filesDir, "state.json")

    @Synchronized
    fun ensure(ctx: Context): AppState {
        if (!loaded) {
            val f = file(ctx.applicationContext)
            if (f.exists()) {
                runCatching { json.decodeFromString(AppState.serializer(), f.readText()) }
                    .onSuccess { _state.value = it }
            }
            loaded = true
        }
        return _state.value
    }

    fun get(ctx: Context): AppState = ensure(ctx)

    fun update(ctx: Context, f: (AppState) -> AppState) {
        ensure(ctx)
        val next = f(_state.value)
        _state.value = next
        val file = file(ctx.applicationContext)
        val tmp = File(file.parentFile, "state.json.tmp")
        tmp.writeText(json.encodeToString(AppState.serializer(), next))
        tmp.renameTo(file)
        WidgetUpdater.refresh(ctx.applicationContext)
    }

    fun exportJson(): String = json.encodeToString(AppState.serializer(), _state.value)
}

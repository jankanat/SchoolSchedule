package kz.alnazar.schedule.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kz.alnazar.schedule.data.*
import kz.alnazar.schedule.imp.*
import kz.alnazar.schedule.logic.Tr
import java.io.File

/** Импорт расписания по фото: выбрать фото → распознать → проверить и поправить → заменить или добавить. */
@Composable
fun ImportPage(st: AppState) {
    val ctx = LocalContext.current
    val ui = LocalUi.current
    val scope = rememberCoroutineScope()
    val lang = st.appLang
    val prefs = remember { ctx.getSharedPreferences("imp", android.content.Context.MODE_PRIVATE) }
    var key by remember { mutableStateOf(prefs.getString("gk", "") ?: "") }
    var engine by remember { mutableStateOf(if (key.isNotBlank()) "gem" else "tess") }
    var bmp by remember { mutableStateOf<Bitmap?>(null) }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    var days by remember { mutableStateOf<List<List<ImpCell>>?>(null) }
    var bells by remember { mutableStateOf<List<Bell>>(emptyList()) }
    var useBells by remember { mutableStateOf(true) }
    var mode by remember { mutableStateOf("replace") }

    fun load(uri: Uri) {
        scope.launch {
            val b = withContext(Dispatchers.IO) { runCatching { Files.decodeScaled(ctx, uri, 2000) }.getOrNull() }
            bmp = b; days = null; err = null
        }
    }
    val camFile = remember { File(ctx.cacheDir, "cam").apply { mkdirs() }.let { File(it, "imp.jpg") } }
    val camUri = remember { FileProvider.getUriForFile(ctx, ctx.packageName + ".files", camFile) }
    val cam = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) load(camUri) }
    val gal = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { u -> if (u != null) load(u) }

    fun run() {
        val b = bmp ?: return
        if (engine == "gem" && key.isBlank()) { err = Tr.t(lang, "i5_nokey"); return }
        prefs.edit().putString("gk", key.trim()).apply()
        busy = true; err = null
        scope.launch {
            val r = withContext(Dispatchers.Default) {
                runCatching { if (engine == "gem") Gemini.recognize(b, key) else Tess.recognize(ctx, b) }
            }
            busy = false
            r.onSuccess { res ->
                if (res.days.none { it.any { c -> c.name.isNotBlank() } }) err = Tr.t(lang, "i5_empty")
                else { days = res.days.map { d -> d.filter { c -> c.name.isNotBlank() } }; bells = res.bells }
            }.onFailure { e -> err = Tr.t(lang, "i5_err") + (e.message ?: e.javaClass.simpleName).take(160) }
        }
    }

    fun save() {
        val d = days ?: return
        Store.update(ctx) { s ->
            var custom = s.custom
            var n = 0
            fun lesson(c: ImpCell): Lesson? {
                if (c.name.isBlank()) return null
                val id = ImpMatch.find(c.name, custom) ?: run {
                    val nid = "c_${System.currentTimeMillis()}_${n++}"
                    custom = custom + CustomSubject(nid, c.name.trim().replaceFirstChar { it.uppercase() }, "📘", 0xFF0A84FF, null, c.room.trim())
                    nid
                }
                return Lesson(id, c.room.trim())
            }
            val parsed = d.map { day -> day.mapNotNull { lesson(it) } }
            val old = s.week
            val week = List(6) { i ->
                val add = parsed.getOrNull(i) ?: emptyList()
                if (mode == "replace") add else (old.getOrNull(i) ?: emptyList()) + add
            }
            val b = if (useBells && bells.isNotEmpty()) bells else null
            s.copy(
                week = week, custom = custom,
                overrides = if (mode == "replace") emptyList() else s.overrides,
                sixDays = s.sixDays || (parsed.getOrNull(5)?.isNotEmpty() == true),
                bells1 = if (b != null && s.shift != 2) b else s.bells1,
                bells2 = if (b != null && s.shift == 2) b else s.bells2,
            )
        }
        ui.back()
    }

    Column(Modifier.padding(horizontal = 16.dp)) {
        val d = days
        if (d == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Btn(Tr.t(lang, "i5_gal"), Modifier.weight(1f), BtnKind.Sec) { gal.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                Btn(Tr.t(lang, "i5_cam"), Modifier.weight(1f), BtnKind.Sec) { cam.launch(camUri) }
            }
            bmp?.let { b ->
                VSpace(12.dp)
                Image(b.asImageBitmap(), null, Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp)).background(T.s2), contentScale = ContentScale.Fit)
                Sec(Tr.t(lang, "i5_engine"))
                Chips(listOf("gem" to Tr.t(lang, "i5_gem"), "tess" to Tr.t(lang, "i5_tess")), engine) { engine = it }
                if (engine == "gem") {
                    VSpace(10.dp)
                    Field(key, { key = it.trim() }, Tr.t(lang, "i5_key"))
                    Text(Tr.t(lang, "i5_key_h"), color = T.tx2, fontSize = 12.5.sp, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
                }
                VSpace(14.dp)
                Btn(Tr.t(lang, if (busy) "i5_busy" else "i5_run"), Modifier.fillMaxWidth(), enabled = !busy) { run() }
            }
            err?.let { Text(it, color = androidx.compose.ui.graphics.Color(0xFFFF453A), fontSize = 13.5.sp, modifier = Modifier.padding(top = 12.dp)) }
        } else {
            Sec(Tr.t(lang, "i5_prev"))
            d.forEachIndexed { di, day ->
                Text(Tr.dayLong(lang, di.coerceAtMost(5)), color = T.tx, fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                day.forEachIndexed { ci, c ->
                    Row(Modifier.padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${ci + 1}", color = T.tx2, fontSize = 14.sp, modifier = Modifier.width(18.dp))
                        Box(Modifier.weight(1f)) { Field(c.name, { v -> days = d.mapIndexed { i, dd -> if (i == di) dd.mapIndexed { j, x -> if (j == ci) x.copy(name = v) else x } else dd } }, Tr.t(lang, "i5_subj")) }
                        Box(Modifier.width(84.dp)) { Field(c.room, { v -> days = d.mapIndexed { i, dd -> if (i == di) dd.mapIndexed { j, x -> if (j == ci) x.copy(room = v) else x } else dd } }, Tr.t(lang, "i5_room")) }
                        Text("✕", color = T.tx2, fontSize = 16.sp, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { days = d.mapIndexed { i, dd -> if (i == di) dd.filterIndexed { j, _ -> j != ci } else dd } }.padding(6.dp))
                    }
                }
                Btn(Tr.t(lang, "i5_addl"), Modifier, BtnKind.Ghost, small = true) { days = d.mapIndexed { i, dd -> if (i == di) dd + ImpCell("") else dd } }
            }
            if (bells.isNotEmpty()) {
                Sec(Tr.t(lang, "i5_bells"))
                Chips(listOf("on" to Tr.t(lang, "i5_bells") + " · ${bells.size}", "off" to Tr.t(lang, "off")), if (useBells) "on" else "off") { useBells = it == "on" }
            }
            Sec(Tr.t(lang, "i5_replace").substringBefore(' ').let { Tr.t(lang, "i5_save") })
            Chips(listOf("replace" to Tr.t(lang, "i5_replace"), "append" to Tr.t(lang, "i5_append")), mode) { mode = it }
            VSpace(14.dp)
            Btn(Tr.t(lang, "i5_save"), Modifier.fillMaxWidth()) { save() }
            VSpace(8.dp)
            Btn(Tr.t(lang, "i5_again"), Modifier.fillMaxWidth(), BtnKind.Ghost) { days = null; bmp = null }
        }
        VSpace(24.dp)
    }
}

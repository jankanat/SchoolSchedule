package kz.alnazar.schedule.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import kz.alnazar.schedule.data.AppState
import kz.alnazar.schedule.data.Catalog
import kz.alnazar.schedule.data.Files
import kz.alnazar.schedule.data.Hw
import kz.alnazar.schedule.data.Override
import kz.alnazar.schedule.data.Store
import kz.alnazar.schedule.logic.Sched
import kz.alnazar.schedule.logic.Slot
import kz.alnazar.schedule.logic.Tr
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/* ===================== ЗАДАНИЯ ===================== */

@Composable
fun HwScreen(st: AppState) {
    val lang = st.appLang
    val ui = LocalUi.current
    val ctx = LocalContext.current
    var tab by rememberSaveable { mutableStateOf("hw") }
    val today = LocalDate.now()
    val tk = today.toString()

    Page {
        item {
            Hdr(Tr.t(lang, "hw_title")) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(T.acc).tap { ui.open { HwSheet(null, tab) } },
                    contentAlignment = Alignment.Center,
                ) { Icon(Ic.plus, null, tint = T.accTx, modifier = Modifier.size(22.dp)) }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 6.dp)) {
                Seg(listOf("hw" to Tr.t(lang, "hw_tab_hw"), "note" to Tr.t(lang, "hw_tab_notes")), tab) { tab = it }
            }
        }
        if (tab == "hw") {
            val open = st.hw.filter { it.kind == "hw" && !it.done }
            val done = st.hw.filter { it.kind == "hw" && it.done }.sortedByDescending { it.due ?: "" }
            if (open.isEmpty() && done.isEmpty()) item { Empty(Tr.t(lang, "hw_empty")) }
            val over = open.filter { it.due != null && it.due < tk }.sortedBy { it.due }
            if (over.isNotEmpty()) {
                item { Box(Modifier.padding(horizontal = 16.dp)) { Sec(Tr.t(lang, "hw_overdue")) } }
                items(over, key = { it.id }) { h -> Box(Modifier.padding(horizontal = 16.dp)) { HwItem(st, h, today) } }
            }
            // по дням сдачи: «Сегодня», «Завтра», «пт, 21 ноя»…
            open.filter { it.due != null && it.due >= tk }.groupBy { it.due!! }.toSortedMap().forEach { (d, list) ->
                val date = LocalDate.parse(d)
                item(key = "h$d") { Box(Modifier.padding(horizontal = 16.dp)) { Sec(Sched.dueLabel(lang, today, date)) } }
                items(list, key = { it.id }) { h -> Box(Modifier.padding(horizontal = 16.dp)) { HwItem(st, h, today, showDue = false) } }
            }
            val nodue = open.filter { it.due == null }
            if (nodue.isNotEmpty()) {
                item { Box(Modifier.padding(horizontal = 16.dp)) { Sec(Tr.t(lang, "hw_nodue")) } }
                items(nodue, key = { it.id }) { h -> Box(Modifier.padding(horizontal = 16.dp)) { HwItem(st, h, today) } }
            }
            if (done.isNotEmpty()) {
                item {
                    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { Sec(Tr.t(lang, "hw_done_sec") + " · " + done.size) }
                        Text(Tr.t(lang, "hw_clear_done"), color = T.acc, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 14.dp).tap {
                                Files.deletePhotos(done.flatMap { it.photos })
                                Store.update(ctx) { s -> s.copy(hw = s.hw.filterNot { it.kind == "hw" && it.done }) }
                            })
                    }
                }
                items(done.take(30), key = { it.id }) { h -> Box(Modifier.padding(horizontal = 16.dp)) { HwItem(st, h, today) } }
            }
            if (open.isNotEmpty() || done.isNotEmpty()) item { Text(Tr.t(lang, "hw_swipe"), color = T.tx2, fontSize = 12.5.sp, modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp)) }
        } else {
            val notes = st.hw.filter { it.kind == "note" }.sortedByDescending { it.created }
            if (notes.isEmpty()) item { Empty(Tr.t(lang, "hw_empty")) }
            items(notes, key = { it.id }) { h -> Box(Modifier.padding(horizontal = 16.dp)) { HwItem(st, h, today) } }
        }
    }
}

/** Карточка записи: кружок-галочка, предмет, срок, текст, фото. Смахивание влево — удалить. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HwItem(st: AppState, h: Hw, today: LocalDate, showDue: Boolean = true) {
    val ctx = LocalContext.current
    val ui = LocalUi.current
    val lang = st.appLang
    val t = T
    val state = rememberSwipeToDismissBoxState(confirmValueChange = { v ->
        if (v == SwipeToDismissBoxValue.EndToStart) {
            Files.deletePhotos(h.photos)
            Store.update(ctx) { s -> s.copy(hw = s.hw.filterNot { it.id == h.id }) }
            true
        } else false
    })
    val shape = RoundedCornerShape(t.rc * .75f)
    SwipeToDismissBox(
        state = state, modifier = Modifier.padding(bottom = 8.dp), enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(Modifier.fillMaxSize().clip(shape).background(Color(0xFFFF453A)).padding(end = 22.dp), contentAlignment = Alignment.CenterEnd) {
                Icon(Ic.trash, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        },
    ) {
        Row(
            Modifier.fillMaxWidth().clip(shape).background(t.sheet).glass(shape).tap { ui.open { HwSheet(h.id, h.kind) } }
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            if (h.kind == "hw") {
                val c by animateColorAsState(if (h.done) t.acc else Color.Transparent, label = "chk")
                Box(
                    Modifier.padding(top = 8.dp).size(24.dp).clip(CircleShape).background(c)
                        .border(2.dp, if (h.done) t.acc else t.tx2, CircleShape)
                        .tap { Store.update(ctx) { s -> s.copy(hw = s.hw.map { if (it.id == h.id) it.copy(done = !it.done) else it }) } },
                    contentAlignment = Alignment.Center,
                ) { if (h.done) Text("✓", color = t.accTx, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
            }
            val v = h.subj?.let { Sched.subj(st, it, lang) }
            if (v != null) Tile(v.emoji, v.color, 40.dp)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(v?.name ?: Tr.t(lang, "type_note"), color = t.tx, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (showDue && h.due != null) {
                        val d = LocalDate.parse(h.due)
                        val late = h.kind == "hw" && !h.done && d < today
                        Text(" · " + Sched.dueLabel(lang, today, d), color = if (late) Color(0xFFFF453A) else t.tx2, fontSize = 13.sp, maxLines = 1)
                    }
                }
                if (h.text.isNotBlank()) Text(
                    h.text, color = if (h.done) t.tx2 else t.tx.copy(alpha = .9f), fontSize = 14.sp, maxLines = 4, overflow = TextOverflow.Ellipsis,
                    textDecoration = if (h.done) TextDecoration.LineThrough else null, modifier = Modifier.padding(top = 2.dp),
                )
                if (h.photos.isNotEmpty()) Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    h.photos.take(4).forEach { Thumb(it, 48) }
                    if (h.photos.size > 4) Text("+${h.photos.size - 4}", color = t.tx2, fontSize = 13.sp, modifier = Modifier.align(Alignment.CenterVertically))
                }
            }
        }
    }
}

@Composable
private fun Thumb(name: String, size: Int, onClick: (() -> Unit)? = null) {
    val bmp = remember(name) { Files.thumb(Files.photo(name))?.asImageBitmap() }
    var open by remember { mutableStateOf(false) }
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape(10.dp)).background(T.s2)
            .tap { if (onClick != null) onClick() else open = true },
    ) { if (bmp != null) Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    if (open) PhotoViewer(name) { open = false }
}

/** Фото на весь экран, нажатие — закрыть. */
@Composable
fun PhotoViewer(name: String, onClose: () -> Unit) {
    Dialog(onClose, DialogProperties(usePlatformDefaultWidth = false)) {
        val bmp = remember(name) { runCatching { BitmapFactory.decodeFile(Files.photo(name).path) }.getOrNull()?.asImageBitmap() }
        Box(Modifier.fillMaxSize().background(Color.Black).tap(onClick = onClose), contentAlignment = Alignment.Center) {
            if (bmp != null) Image(bmp, null, Modifier.fillMaxWidth(), contentScale = ContentScale.Fit)
        }
    }
}

/**
 * Шторка записи. id = null — новая.
 * Для новой домашки можно передать предмет и срок (из меню урока).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnScope.HwSheet(id: Long?, kind0: String, subj0: String? = null, due0: LocalDate? = null) {
    val ctx = LocalContext.current
    val ui = LocalUi.current
    val st = remember { Store.state.value }
    val lang = st.appLang
    val old = id?.let { i -> st.hw.firstOrNull { it.id == i } }
    var kind by remember { mutableStateOf(old?.kind ?: kind0) }
    var subj by remember { mutableStateOf(old?.subj ?: subj0) }
    var text by remember { mutableStateOf(old?.text ?: "") }
    val today = LocalDate.now()
    var due by remember { mutableStateOf(old?.due?.let { LocalDate.parse(it) } ?: due0 ?: subj0?.let { Sched.nextLessonDate(st, it, today) }) }
    var dueTouched by remember { mutableStateOf(old != null || due0 != null) }
    val photos = remember { mutableStateListOf<String>().apply { addAll(old?.photos ?: emptyList()) } }
    var picker by remember { mutableStateOf(false) }

    // камера: снимок во временный файл, потом уменьшенная копия в photos/
    val camFile = remember { File(ctx.cacheDir, "cam").apply { mkdirs() }.let { File(it, "shot.jpg") } }
    val camUri = remember { FileProvider.getUriForFile(ctx, ctx.packageName + ".files", camFile) }
    val cam = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) Files.savePhoto(ctx, camUri)?.let { photos += it }
        camFile.delete()
    }
    val gal = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(6)) { uris: List<Uri> ->
        uris.forEach { u -> Files.savePhoto(ctx, u)?.let { photos += it } }
    }

    SheetTitle(Tr.t(lang, if (old == null) "hw_new" else "hw_edit"))
    if (old == null) Seg(listOf("hw" to Tr.t(lang, "type_hw"), "note" to Tr.t(lang, "type_note")), kind) { kind = it }

    // предмет: сначала предметы программы, потом свои
    Lab(Tr.t(lang, "subject"))
    val ids = remember { Catalog.programSubjects(st.program, st.grade) + st.custom.map { it.id } }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (kind == "note") SubjChip(null, Tr.t(lang, "hw_no_subj"), "🗒️", 0xFF8E8E93, subj == null) { subj = null }
        (listOfNotNull(subj?.takeIf { it !in ids }) + ids).forEach { sid ->
            val v = Sched.subj(st, sid, lang)
            SubjChip(sid, v.name, v.emoji, v.color, subj == sid) {
                subj = sid
                if (!dueTouched) due = Sched.nextLessonDate(st, sid, today)
            }
        }
    }

    Lab(Tr.t(lang, if (kind == "hw") "hw_text" else "hw_note_text"))
    Field(text, { text = it.take(2000) }, "", lines = 3)

    if (kind == "hw") {
        Lab(Tr.t(lang, "hw_due"))
        val next = subj?.let { Sched.nextLessonDate(st, it, today) }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (next != null) Chip(Tr.t(lang, "hw_next_lesson") + " · " + Sched.dueLabel(lang, today, next), due == next) { due = next; dueTouched = true }
            val tm = today.plusDays(1)
            if (next != tm) Chip(Tr.t(lang, "tomorrow"), due == tm) { due = tm; dueTouched = true }
            val other = due != null && due != next && due != tm
            Chip(if (other) Sched.dueLabel(lang, today, due!!) else Tr.t(lang, "hw_pick_date") + "…", other) { picker = true }
        }
    }

    Lab(Tr.t(lang, "hw_photo"))
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        photos.toList().forEach { p ->
            Box {
                Thumb(p, 64)
                Box(
                    Modifier.align(Alignment.TopEnd).padding(3.dp).size(20.dp).clip(CircleShape).background(Color(0xCC000000))
                        .tap { photos.remove(p); if (old?.photos?.contains(p) != true) Files.deletePhotos(listOf(p)) },
                    contentAlignment = Alignment.Center,
                ) { Text("✕", color = Color.White, fontSize = 11.sp) }
            }
        }
        Btn("📷 " + Tr.t(lang, "hw_camera"), kind = BtnKind.Sec, small = true) { runCatching { cam.launch(camUri) } }
        Btn("🖼️ " + Tr.t(lang, "hw_gallery"), kind = BtnKind.Sec, small = true) {
            gal.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    Btns {
        val ok = if (kind == "hw") subj != null else (text.isNotBlank() || photos.isNotEmpty())
        Btn(Tr.t(lang, "save"), Modifier.weight(1f), enabled = ok) {
            val h = Hw(
                id = old?.id ?: System.currentTimeMillis(), kind = kind, subj = subj, text = text.trim(),
                due = if (kind == "hw") due?.toString() else null, done = old?.done ?: false,
                photos = photos.toList(), created = old?.created ?: System.currentTimeMillis(),
            )
            // удалённые из записи фото — стереть с диска
            old?.photos?.filter { it !in photos }?.let { Files.deletePhotos(it) }
            Store.update(ctx) { s -> s.copy(hw = if (old == null) s.hw + h else s.hw.map { if (it.id == h.id) h else it }) }
            ui.close()
        }
        if (old != null) Btn("🗑", kind = BtnKind.Danger) {
            Files.deletePhotos(old.photos)
            Store.update(ctx) { s -> s.copy(hw = s.hw.filterNot { it.id == old.id }) }
            ui.close()
        }
    }

    if (picker) {
        val ps = rememberDatePickerState(
            initialSelectedDateMillis = (due ?: today.plusDays(1)).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { picker = false },
            confirmButton = {
                TextButton({
                    ps.selectedDateMillis?.let { due = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate(); dueTouched = true }
                    picker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton({ picker = false }) { Text(Tr.t(lang, "cancel")) } },
        ) { DatePicker(ps) }
    }
}

@Composable
private fun SubjChip(id: String?, name: String, emoji: String, color: Long, on: Boolean, onClick: () -> Unit) {
    val t = T
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier.size(width = 78.dp, height = 72.dp).clip(shape).background(if (on) t.accSoft else t.s2)
            .border(2.dp, if (on) t.acc else Color.Transparent, shape).tap(onClick = onClick).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Tile(emoji, color, 30.dp)
        Text(name, color = t.tx, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
    }
}

/* ===================== МЕНЮ УРОКА (долгое нажатие) ===================== */

@Composable
fun ColumnScope.LessonMenu(date: LocalDate, slot: Slot) {
    val ctx = LocalContext.current
    val ui = LocalUi.current
    val st = remember { Store.state.value }
    val lang = st.appLang
    val v = Sched.subj(st, slot.lesson.subj, lang)
    val wk = Sched.weekKey(date)
    val d = slot.dayIdx; val i = slot.idx
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 10.dp)) {
        Tile(v.emoji, v.color, 46.dp)
        Column {
            Text(v.name, color = T.tx, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text(Tr.dayLong(lang, d) + " · " + Tr.hm(slot.start) + "–" + Tr.hm(slot.end), color = T.tx2, fontSize = 13.5.sp)
        }
    }
    val hasOv = st.overrides.any { it.week == wk && it.day == d && it.idx == i }
    Group {
        MenuRow("📝", Tr.t(lang, "lm_hw")) { ui.open { HwSheet(null, "hw", slot.lesson.subj, date) } }
        RowDivider()
        MenuRow("🔁", Tr.t(lang, "lm_swap")) { ui.open { LessonSheet(d, i, wk, "week") } }
        RowDivider()
        MenuRow("✖️", Tr.t(lang, "lm_cancel")) {
            Store.update(ctx) { s -> s.copy(overrides = s.overrides.filterNot { it.week == wk && it.day == d && it.idx == i } + Override(wk, d, i, null)) }
            ui.close()
        }
        if (hasOv) {
            RowDivider()
            MenuRow("↩️", Tr.t(lang, "lm_restore")) {
                Store.update(ctx) { s -> s.copy(overrides = s.overrides.filterNot { it.week == wk && it.day == d && it.idx == i }) }
                ui.close()
            }
        }
        RowDivider()
        MenuRow("✏️", Tr.t(lang, "lm_edit")) { ui.open { LessonSheet(d, i, wk, "perm") } }
    }
}

@Composable
private fun MenuRow(em: String, text: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().tap(onClick = onClick).padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(em, fontSize = 18.sp)
        Text(text, color = T.tx, fontSize = 15.5.sp, fontWeight = FontWeight.Medium)
    }
}


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
import androidx.compose.foundation.lazy.itemsIndexed
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
        item { ScreenHead(Tr.t(lang, "hw_title"), Tr.t(lang, "more_week", st.hw.count { it.kind == "hw" && !it.done }).replaceFirstChar { it.uppercase() }) }
        item {
            Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 6.dp).appear(1)) {
                HeadSeg(listOf("hw" to Tr.t(lang, "hw_tab_hw"), "note" to Tr.t(lang, "hw_tab_notes")), tab, wide = true) { tab = it }
            }
        }
        if (tab == "hw") {
            val open = st.hw.filter { it.kind == "hw" && !it.done }
            val done = st.hw.filter { it.kind == "hw" && it.done }.sortedByDescending { it.due ?: "" }
            if (open.isEmpty() && done.isEmpty()) item { Empty(Tr.t(lang, "hw_empty")) }
            val over = open.filter { it.due != null && it.due < tk }.sortedBy { it.due }
            if (over.isNotEmpty()) {
                item { Box(Modifier.padding(horizontal = 16.dp)) { Sec(Tr.t(lang, "hw_overdue")) } }
                hwList(over, st, today)
            }
            // по дням сдачи: «Сегодня», «Завтра», «пт, 21 ноя»…
            open.filter { it.due != null && it.due >= tk }.groupBy { it.due!! }.toSortedMap().forEach { (d, list) ->
                val date = LocalDate.parse(d)
                item(key = "h$d") { Box(Modifier.padding(horizontal = 16.dp)) { Sec(Sched.dueLabel(lang, today, date)) } }
                hwList(list, st, today, showDue = false)
            }
            val nodue = open.filter { it.due == null }
            if (nodue.isNotEmpty()) {
                item { Box(Modifier.padding(horizontal = 16.dp)) { Sec(Tr.t(lang, "hw_nodue")) } }
                hwList(nodue, st, today)
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
                hwList(done.take(30), st, today)
            }
            if (open.isNotEmpty() || done.isNotEmpty()) item { Text(Tr.t(lang, "hw_swipe"), color = T.tx2, fontSize = 12.5.sp, modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp)) }
        } else {
            val notes = st.hw.filter { it.kind == "note" }.sortedByDescending { it.created }
            if (notes.isEmpty()) item { Empty(Tr.t(lang, "hw_empty")) }
            hwList(notes, st, today)
        }
    }
}

/** Список записей: у ColorOS соседние записи одного дня слиты в одну карточку с общими скруглениями. */
private fun androidx.compose.foundation.lazy.LazyListScope.hwList(list: List<Hw>, st: AppState, today: LocalDate, showDue: Boolean = true) {
    itemsIndexed(list, key = { _, h -> h.id }) { i, h ->
        Box(Modifier.padding(horizontal = 16.dp)) { HwItem(st, h, today, showDue, first = i == 0, last = i == list.lastIndex) }
    }
}

/** Карточка записи: кружок-галочка, предмет, срок, текст, фото. Смахивание влево — удалить. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HwItem(st: AppState, h: Hw, today: LocalDate, showDue: Boolean = true, first: Boolean = true, last: Boolean = true) {
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
    val co = t.id == "color"
    val shape = if (co) RoundedCornerShape(topStart = if (first) t.rc else 0.dp, topEnd = if (first) t.rc else 0.dp, bottomStart = if (last) t.rc else 0.dp, bottomEnd = if (last) t.rc else 0.dp)
        else RoundedCornerShape(t.rc * .75f)
    SwipeToDismissBox(
        state = state, modifier = Modifier.padding(bottom = if (co) 0.dp else 8.dp), enableDismissFromStartToEnd = false,
        backgroundContent = {
            // красный фон с корзиной виден только пока тянешь карточку (карточка полупрозрачная — иначе он просвечивает)
            if (state.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                Box(Modifier.fillMaxSize().clip(shape).background(Color(0xFFFF453A)).padding(end = 22.dp), contentAlignment = Alignment.CenterEnd) {
                    Icon(Ic.trash, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
        },
    ) {
        androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth().then(if (co) Modifier.clip(shape).background(t.s1) else Modifier)) {
        Row(
            (if (co) Modifier.fillMaxWidth().coRow { ui.open { HwSheet(h.id, h.kind) } }.padding(horizontal = 18.dp, vertical = 14.dp)
            else Modifier.fillMaxWidth().styleCard(shape).press(shape) { ui.open { HwSheet(h.id, h.kind) } }
                .padding(horizontal = 12.dp, vertical = 11.dp)),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (co) 14.dp else 11.dp),
        ) {
            if (h.kind == "hw") {
                AnimCheck(h.done, 24.dp, false, t.tx2) {
                    Store.update(ctx) { s -> s.copy(hw = s.hw.map { if (it.id == h.id) it.copy(done = !it.done) else it }) }
                }
            }
            val v = h.subj?.let { Sched.subj(st, it, lang) }
            if (v != null) Tile(v.emoji, v.color, 40.dp)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(v?.name ?: Tr.t(lang, "type_note"), color = t.tx, fontSize = if (co) 17.sp else 15.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (showDue && h.due != null) {
                        val d = LocalDate.parse(h.due)
                        val late = h.kind == "hw" && !h.done && d < today
                        Text(" · " + Sched.dueLabel(lang, today, d), color = if (late) Color(0xFFFF453A) else t.tx2, fontSize = 13.sp, maxLines = 1)
                    }
                }
                if (h.text.isNotBlank()) Text(
                    h.text, color = if (h.done) t.tx2 else t.tx.copy(alpha = .9f), fontSize = if (co) 15.sp else 14.sp, maxLines = 4, overflow = TextOverflow.Ellipsis,
                    textDecoration = if (h.done) TextDecoration.LineThrough else null, modifier = Modifier.padding(top = 2.dp),
                )
                if (h.photos.isNotEmpty()) Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    h.photos.take(4).forEach { Thumb(it, 48) }
                    if (h.photos.size > 4) Text("+${h.photos.size - 4}", color = t.tx2, fontSize = 13.sp, modifier = Modifier.align(Alignment.CenterVertically))
                }
            }
        }
        if (co && !last) androidx.compose.runtime.CompositionLocalProvider(LocalRowInset provides 18.dp) { RowDivider() }
        }
    }
}

@Composable
internal fun Thumb(name: String, size: Int, onClick: (() -> Unit)? = null) {
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

    val ids = remember { Catalog.programSubjects(st.program, st.grade) + st.custom.map { it.id } }
    val subjects = (if (kind == "note") listOf(Subj4(null, Tr.t(lang, "hw_no_subj"), "🗒️", 0xFF8E8E93)) else emptyList()) +
        (listOfNotNull(subj?.takeIf { it !in ids }) + ids).map { sid -> Sched.subj(st, sid, lang).let { v -> Subj4(sid, v.name, v.emoji, v.color) } }
    val next = subj?.let { Sched.nextLessonDate(st, it, today) }
    val tm = today.plusDays(1)
    val other = due != null && due != next && due != tm
    val dueOpts = listOfNotNull(
        next?.let { n -> DueOpt(Tr.t(lang, "hw_next_lesson") + " · " + Sched.dueLabel(lang, today, n), due == n) { due = n; dueTouched = true } },
        if (next != tm) DueOpt(Tr.t(lang, "tomorrow"), due == tm) { due = tm; dueTouched = true } else null,
        DueOpt(if (other) Sched.dueLabel(lang, today, due!!) else Tr.t(lang, "hw_pick_date") + "…", other) { picker = true },
    )
    val ok = if (kind == "hw") subj != null else (text.isNotBlank() || photos.isNotEmpty())
    HwForm(HwUi(
        isNew = old == null, lang = lang,
        kind = kind, setKind = { kind = it },
        subj = subj, setSubj = { sid -> subj = sid; if (sid != null && !dueTouched) due = Sched.nextLessonDate(st, sid, today) }, subjects = subjects,
        text = text, setText = { text = it },
        dueOptions = dueOpts,
        photos = photos.toList(),
        removePhoto = { p -> photos.remove(p); if (old?.photos?.contains(p) != true) Files.deletePhotos(listOf(p)) },
        camera = { runCatching { cam.launch(camUri) } },
        gallery = { gal.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        canSave = ok,
        save = {
            val h = Hw(
                id = old?.id ?: System.currentTimeMillis(), kind = kind, subj = subj, text = text.trim(),
                due = if (kind == "hw") due?.toString() else null, done = old?.done ?: false,
                photos = photos.toList(), created = old?.created ?: System.currentTimeMillis(),
            )
            // удалённые из записи фото — стереть с диска
            old?.photos?.filter { it !in photos }?.let { Files.deletePhotos(it) }
            Store.update(ctx) { s -> s.copy(hw = if (old == null) s.hw + h else s.hw.map { if (it.id == h.id) h else it }) }
            ui.close()
        },
        delete = old?.let { o -> { Files.deletePhotos(o.photos); Store.update(ctx) { s -> s.copy(hw = s.hw.filterNot { it.id == o.id }) }; ui.close() } },
        close = { ui.close() },
    ))

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
        MenuRow(Ic.hw, Tr.t(lang, "lm_hw")) { ui.open { HwSheet(null, "hw", slot.lesson.subj, date) } }
        RowDivider()
        MenuRow(Ic.restore, Tr.t(lang, "lm_swap")) { ui.open { LessonSheet(d, i, wk, "week") } }
        RowDivider()
        MenuRow(Ic.close, Tr.t(lang, "lm_cancel")) {
            Store.update(ctx) { s -> s.copy(overrides = s.overrides.filterNot { it.week == wk && it.day == d && it.idx == i } + Override(wk, d, i, null)) }
            ui.close()
        }
        if (hasOv) {
            RowDivider()
            MenuRow(Ic.restore, Tr.t(lang, "lm_restore")) {
                Store.update(ctx) { s -> s.copy(overrides = s.overrides.filterNot { it.week == wk && it.day == d && it.idx == i }) }
                ui.close()
            }
        }
        RowDivider()
        MenuRow(Ic.edit, Tr.t(lang, "lm_edit")) { ui.open { LessonSheet(d, i, wk, "perm") } }
    }
}

@Composable
private fun MenuRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().press(RoundedCornerShape(16.dp), onClick = onClick).padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, null, tint = T.acc, modifier = Modifier.size(22.dp))
        Text(text, color = T.tx, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}


package kz.alnazar.schedule.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.alnazar.schedule.data.AppState
import kz.alnazar.schedule.data.Backup
import kz.alnazar.schedule.data.Catalog
import kz.alnazar.schedule.data.CustomSubject
import kz.alnazar.schedule.data.Files
import kz.alnazar.schedule.data.Holiday
import kz.alnazar.schedule.data.Store
import kz.alnazar.schedule.logic.Sched
import kz.alnazar.schedule.logic.Tr
import kz.alnazar.schedule.widget.Notif
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.roundToInt

private fun toast(ctx: Context, s: String) = Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show()

/** Шторка подтверждения. */
@Composable
fun ColumnScope.ConfirmSheet(text: String, yes: String, danger: Boolean = false, onYes: () -> Unit) {
    val ui = LocalUi.current
    val lang = Store.state.value.appLang
    SheetTitle(text)
    Btns {
        Btn(yes, Modifier.weight(1f), if (danger) BtnKind.Danger else BtnKind.Primary) { onYes(); ui.close() }
        Btn(Tr.t(lang, "cancel"), Modifier.weight(1f), BtnKind.Sec) { ui.close() }
    }
}

/** Кнопка-дата с календарём Material 3. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateBtn(label: String, date: LocalDate, modifier: Modifier = Modifier, onPick: (LocalDate) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val lang = Store.state.value.appLang
    Column(modifier) {
        Lab(label)
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(T.s2).tap { open = true }.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text("${date.dayOfMonth}.${"%02d".format(date.monthValue)}.${date.year}", color = T.tx, fontSize = 15.sp)
        }
    }
    if (open) {
        val ps = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton({ ps.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }; open = false }) { Text("OK") }
            },
            dismissButton = { TextButton({ open = false }) { Text(Tr.t(lang, "cancel")) } },
        ) { DatePicker(ps) }
    }
}

/* ===================== КАНИКУЛЫ ===================== */

@Composable
fun HolidaysPage(st: AppState) {
    val ui = LocalUi.current
    val ctx = LocalContext.current
    val lang = st.appLang
    val today = LocalDate.now().toString()
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text(Tr.t(lang, "hol_note"), color = T.tx2, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("kz" to "hol_tpl_kz", "ru" to "hol_tpl_ru").forEach { (p, k) ->
                Btn(Tr.t(lang, k), Modifier.weight(1f), BtnKind.Sec, small = true) {
                    ui.open { ConfirmSheet(Tr.t(lang, "hol_tpl_ask"), Tr.t(lang, "yes")) { Store.update(ctx) { it.copy(holidays = Catalog.holidayTemplate(p)) } } }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        if (st.holidays.isEmpty()) Empty(Tr.t(lang, "hw_empty"))
        else Group {
            st.holidays.sortedBy { it.from }.forEachIndexed { i, h ->
                if (i > 0) RowDivider()
                val a = LocalDate.parse(h.from); val b = LocalDate.parse(h.to)
                val days = (b.toEpochDay() - a.toEpochDay() + 1).toInt()
                val now = today >= h.from && today <= h.to
                Row(
                    Modifier.fillMaxWidth().tap { ui.open { HolidaySheet(h) } }.padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(h.name + if (now) " · " + Tr.t(lang, "hol_now") else "", color = if (now) T.acc else T.tx, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold)
                        Text(Tr.dateShort(lang, a) + " – " + Tr.dateShort(lang, b) + " " + b.year, color = T.tx2, fontSize = 13.sp)
                    }
                    Text(Tr.t(lang, "hol_days", days), color = T.tx2, fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Btn("+ " + Tr.t(lang, "hol_add"), Modifier.fillMaxWidth(), BtnKind.Sec) { ui.open { HolidaySheet(null) } }
    }
}

@Composable
private fun ColumnScope.HolidaySheet(h: Holiday?) {
    val ctx = LocalContext.current
    val ui = LocalUi.current
    val lang = Store.state.value.appLang
    var name by remember { mutableStateOf(h?.name ?: "") }
    var from by remember { mutableStateOf(h?.from?.let { LocalDate.parse(it) } ?: LocalDate.now()) }
    var to by remember { mutableStateOf(h?.to?.let { LocalDate.parse(it) } ?: LocalDate.now().plusDays(6)) }
    SheetTitle(Tr.t(lang, if (h == null) "hol_new" else "holidays"))
    Lab(Tr.t(lang, "hol_name"))
    Field(name, { name = it.take(40) }, Tr.t(lang, "holiday"))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        DateBtn(Tr.t(lang, "hol_from"), from, Modifier.weight(1f)) { from = it; if (to < it) to = it }
        DateBtn(Tr.t(lang, "hol_to"), to, Modifier.weight(1f)) { to = it }
    }
    val bad = to < from
    if (bad) Text(Tr.t(lang, "hol_bad"), color = Color(0xFFFF453A), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
    Btns {
        Btn(Tr.t(lang, "save"), Modifier.weight(1f), enabled = !bad) {
            val n = Holiday(h?.id ?: System.currentTimeMillis(), name.trim().ifEmpty { Tr.t(lang, "holiday") }, from.toString(), to.toString())
            Store.update(ctx) { s -> s.copy(holidays = if (h == null) s.holidays + n else s.holidays.map { if (it.id == n.id) n else it }) }
            ui.close()
        }
        if (h != null) Btn("", kind = BtnKind.Danger, icon = Ic.trash) {
            Store.update(ctx) { s -> s.copy(holidays = s.holidays.filterNot { it.id == h.id }) }
            ui.close()
        }
    }
}

/* ===================== СВОИ ПРЕДМЕТЫ ===================== */

private val PALETTE = listOf(
    0xFFFF453A, 0xFFFF9F0A, 0xFFFFD60A, 0xFF30D158, 0xFF63E6E2, 0xFF40C8E0,
    0xFF0A84FF, 0xFF5E5CE6, 0xFFBF5AF2, 0xFFFF375F, 0xFFAC8E68, 0xFF8E8E93,
)
private val EMOJI = listOf(
    "📐", "📏", "🧮", "🧪", "🔬", "🧬", "⚗️", "🌍", "🗺️", "🧭", "🎨", "🎵", "🎹", "🎸", "🎭", "⚽",
    "🏀", "🏐", "🏊", "🥋", "♟️", "💻", "🤖", "📊", "💡", "🧠", "📖", "📚", "✍️", "🗣️", "🇰🇿", "🇷🇺",
    "🇬🇧", "🇩🇪", "🇫🇷", "🇨🇳", "🇹🇷", "🕌", "🧘", "🍳", "🧵", "🔧", "🛠️", "🚗", "🛡️", "🎯", "🌱", "🐾",
    "🌌", "⚖️", "💰", "🏛️", "📰", "🎬", "📷", "🩺", "🚀", "⚡", "🧩", "🎤",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubjectsPage(st: AppState) {
    val ui = LocalUi.current
    val lang = st.appLang
    Column(Modifier.padding(horizontal = 16.dp)) {
        if (st.custom.isEmpty()) Text(Tr.t(lang, "subj_mine_empty"), color = T.tx2, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
        else Group {
            st.custom.forEachIndexed { i, c ->
                if (i > 0) RowDivider()
                val v = Sched.subj(st, c.id, lang)
                Row(
                    Modifier.fillMaxWidth().tap { ui.open { SubjectSheet(c) } }.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Tile(v.emoji, v.color, 40.dp)
                    Column(Modifier.weight(1f)) {
                        Text(c.name, color = T.tx, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold)
                        if (c.room.isNotBlank()) Text(Tr.t(lang, "room", c.room), color = T.tx2, fontSize = 13.sp)
                    }
                    Text("›", color = T.tx2, fontSize = 20.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Btn("+ " + Tr.t(lang, "subj_add"), Modifier.fillMaxWidth(), BtnKind.Sec) { ui.open { SubjectSheet(null) } }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.SubjectSheet(c: CustomSubject?) {
    val ctx = LocalContext.current
    val ui = LocalUi.current
    val st = remember { Store.state.value }
    val lang = st.appLang
    var name by remember { mutableStateOf(c?.name ?: "") }
    var color by remember { mutableStateOf(c?.color ?: PALETTE[6]) }
    var emoji by remember { mutableStateOf(c?.emoji ?: "📘") }
    var img by remember { mutableStateOf(c?.img) }
    var src by remember { mutableStateOf(if (c?.img != null) "img" else "emoji") }
    var room by remember { mutableStateOf(c?.room ?: "") }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { u: Uri? ->
        if (u != null) Files.saveSubjectImage(ctx, u)?.let { img = it; src = "img" }
    }

    SheetTitle(Tr.t(lang, if (c == null) "subj_new" else "subj_edit"))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Tile(if (src == "img" && img != null) "img:$img" else emoji, color, 56.dp)
        Column(Modifier.weight(1f)) { Field(name, { name = it.take(30) }, Tr.t(lang, "subj_name")) }
    }
    Lab(Tr.t(lang, "subj_color"))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        PALETTE.forEach { p ->
            Box(
                Modifier.size(24.dp).clip(CircleShape).background(Color(p))
                    .border(if (p == color) 3.dp else 0.dp, if (p == color) T.tx else Color.Transparent, CircleShape).tap { color = p },
            )
        }
    }
    Lab(Tr.t(lang, "subj_icon"))
    Seg(listOf("emoji" to Tr.t(lang, "subj_emoji"), "img" to Tr.t(lang, "subj_img")), src) { src = it }
    Spacer(Modifier.height(10.dp))
    if (src == "emoji") {
        Text(Tr.t(lang, "subj_emoji_hint"), color = T.tx2, fontSize = 12.5.sp, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        Field(emoji, { v -> if (v.isNotBlank()) emoji = v.trim().takeLast(16) }, "🙂")
        Spacer(Modifier.height(8.dp))
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            EMOJI.forEach { e ->
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(if (e == emoji) T.accSoft else Color.Transparent).tap { emoji = e },
                    contentAlignment = Alignment.Center,
                ) { Text(e, fontSize = 22.sp) }
            }
        }
        Text(Tr.t(lang, "subj_dl"), color = T.tx2, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
    } else {
        Btn(Tr.t(lang, "subj_pick_img"), Modifier.fillMaxWidth(), BtnKind.Sec, icon = Ic.image) {
            pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }
    Lab(Tr.t(lang, "subj_room"))
    Field(room, { room = it.take(20) }, Tr.t(lang, "room_ph"))
    Btns {
        Btn(Tr.t(lang, "save"), Modifier.weight(1f), enabled = name.isNotBlank()) {
            val n = CustomSubject(
                id = c?.id ?: "c_${System.currentTimeMillis()}", name = name.trim(), emoji = emoji, color = color,
                img = if (src == "img") img else null, room = room.trim(),
            )
            Store.update(ctx) { s -> s.copy(custom = if (c == null) s.custom + n else s.custom.map { if (it.id == n.id) n else it }) }
            if (n.img == null) Files.fetchEmoji(ctx, n.emoji)
            ui.close()
        }
        if (c != null) Btn("", kind = BtnKind.Danger, icon = Ic.trash) {
            val used = st.week.any { d -> d.any { it.subj == c.id } }
            val del = {
                Store.update(ctx) { s ->
                    s.copy(
                        custom = s.custom.filterNot { it.id == c.id },
                        week = s.week.map { d -> d.filterNot { it.subj == c.id } },
                        overrides = s.overrides.filterNot { it.lesson?.subj == c.id },
                        hw = s.hw.map { if (it.subj == c.id) it.copy(subj = null, kind = "note") else it },
                    )
                }
            }
            if (used) ui.open { ConfirmSheet(Tr.t(lang, "subj_in_use"), Tr.t(lang, "delete"), danger = true) { del() } }
            else { del(); ui.close() }
        }
    }
}

/* ===================== ЭКСПОРТ И ИМПОРТ ===================== */

@Composable
fun BackupPage(st: AppState) {
    val ctx = LocalContext.current
    val ui = LocalUi.current
    val lang = st.appLang
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { u: Uri? ->
        if (u != null) runCatching {
            ctx.contentResolver.openOutputStream(u)?.use { it.write(Backup.export().toByteArray()) }
            toast(ctx, Tr.t(lang, "saved"))
        }.onFailure { toast(ctx, it.message ?: "error") }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u: Uri? ->
        if (u != null) {
            val text = runCatching { ctx.contentResolver.openInputStream(u)?.use { it.readBytes().decodeToString() } }.getOrNull() ?: ""
            ui.open {
                ConfirmSheet(Tr.t(lang, "imp_ask"), Tr.t(lang, "imp_json"), danger = true) {
                    toast(ctx, Tr.t(lang, if (Backup.import(ctx, text)) "imp_ok" else "imp_bad"))
                }
            }
        }
    }
    Column(Modifier.padding(horizontal = 16.dp)) {
        Sec(Tr.t(lang, "exp"))
        Card {
            Column(Modifier.padding(16.dp)) {
                Text(Tr.t(lang, "exp_d"), color = T.tx2, fontSize = 13.5.sp)
                Btns {
                    Btn(Tr.t(lang, "exp_file"), Modifier.weight(1f), icon = Ic.save) { save.launch("schoolschedule-${LocalDate.now()}.json") }
                    Btn(Tr.t(lang, "copy"), kind = BtnKind.Sec) {
                        val cm = ctx.getSystemService(ClipboardManager::class.java)
                        cm.setPrimaryClip(ClipData.newPlainText("SchoolSchedule", Backup.export()))
                        if (Build.VERSION.SDK_INT < 33) toast(ctx, Tr.t(lang, "copied"))
                    }
                }
            }
        }
        Sec(Tr.t(lang, "imp_json"))
        Card {
            Column(Modifier.padding(16.dp)) {
                Text(Tr.t(lang, "imp_d"), color = T.tx2, fontSize = 13.5.sp)
                Btns {
                    Btn(Tr.t(lang, "imp_file"), Modifier.weight(1f), BtnKind.Sec, icon = Ic.folder) { open.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
                    Btn("", kind = BtnKind.Sec, icon = Ic.clip) {
                        val cm = ctx.getSystemService(ClipboardManager::class.java)
                        val text = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(ctx)?.toString() ?: ""
                        ui.open {
                            ConfirmSheet(Tr.t(lang, "imp_ask"), Tr.t(lang, "imp_json"), danger = true) {
                                toast(ctx, Tr.t(lang, if (Backup.import(ctx, text)) "imp_ok" else "imp_bad"))
                            }
                        }
                    }
                }
                Text(Tr.t(lang, "imp_paste") + ": 📋", color = T.tx2, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
        Sec(Tr.t(lang, "autobak"))
        Card {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("☁️", fontSize = 26.sp)
                Text(Tr.t(lang, "autobak_d"), color = T.tx2, fontSize = 13.5.sp)
            }
        }
    }
}

/* ===================== НАПОМИНАНИЯ ===================== */

@Composable
fun NotifPage(st: AppState) {
    val ctx = LocalContext.current
    val lang = st.appLang
    var allowed by remember { mutableStateOf(Notif.allowed(ctx)) }
    var pending by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        allowed = ok
        pending?.invoke(ok); pending = null
    }
    fun enable(f: (Boolean) -> Unit) {
        if (allowed || Build.VERSION.SDK_INT < 33) f(true)
        else { pending = f; perm.launch(Manifest.permission.POST_NOTIFICATIONS) }
    }
    val n = st.notif
    Column(Modifier.padding(horizontal = 16.dp)) {
        if (!allowed && (n.evening || n.before)) Text(Tr.t(lang, "n_perm"), color = Color(0xFFFF9F0A), fontSize = 13.5.sp, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        Group {
            TgRow(Tr.t(lang, "n_evening"), n.evening, Tr.t(lang, "n_evening_sub")) { v ->
                if (v) enable { ok -> if (ok) Store.update(ctx) { it.copy(notif = it.notif.copy(evening = true)) } }
                else Store.update(ctx) { it.copy(notif = it.notif.copy(evening = false)) }
            }
        }
        if (n.evening) {
            Lab(Tr.t(lang, "n_at"))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (16..22).map { it * 60 }.forEach { m -> Chip(Tr.hm(m), n.eveningAt == m) { Store.update(ctx) { it.copy(notif = it.notif.copy(eveningAt = m)) } } }
            }
        }
        Spacer(Modifier.height(14.dp))
        Group {
            TgRow(Tr.t(lang, "n_before"), n.before, Tr.t(lang, "n_before_sub")) { v ->
                if (v) enable { ok -> if (ok) Store.update(ctx) { it.copy(notif = it.notif.copy(before = true)) } }
                else Store.update(ctx) { it.copy(notif = it.notif.copy(before = false)) }
            }
        }
        if (n.before) {
            Lab(Tr.t(lang, "n_at"))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(10, 15, 30, 60, 120, 180).forEach { m ->
                    Chip(if (m < 60) Tr.t(lang, "n_min", m) else Tr.t(lang, "n_h", m / 60), n.beforeMin == m) {
                        Store.update(ctx) { it.copy(notif = it.notif.copy(beforeMin = m)) }
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Btn(Tr.t(lang, "n_test"), Modifier.fillMaxWidth(), BtnKind.Sec, icon = Ic.bell) {
            enable { ok ->
                if (ok) {
                    val ev = Notif.events(st).firstOrNull()
                        ?: Notif.Ev(java.time.LocalDateTime.now(), 999, Tr.t(lang, "n_t_evening", Tr.t(lang, "tomorrow").lowercase()), Tr.t(lang, "no_hw"))
                    Notif.post(ctx, ev.copy(id = 999))
                }
            }
        }
        if (!WidgetUpdaterExact(ctx)) Text(Tr.t(lang, "exactAlarmHint"), color = T.tx2, fontSize = 12.5.sp, modifier = Modifier.padding(start = 4.dp, top = 10.dp))
    }
}

private fun WidgetUpdaterExact(ctx: Context) = kz.alnazar.schedule.widget.WidgetUpdater.canExact(ctx)

/* ===================== ШРИФТ И СКРУГЛЕНИЕ ===================== */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FontAndCorners(st: AppState) {
    val ctx = LocalContext.current
    val lang = st.appLang
    Sec(Tr.t(lang, "corners"))
    Card {
        Column(Modifier.padding(16.dp)) {
            GradSlider(
                "", st.rs / 1.5f, listOf(T.s2, T.acc), T.acc,
                fmt = { "${(it * 150).roundToInt()}%" },
                onChange = { v -> Store.preview { it.copy(rs = (v * 1.5f * 20).roundToInt() / 20f) } },
                onEnd = { Store.update(ctx) { it } },
            )
        }
    }
    Sec(Tr.t(lang, "font"))
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("style" to "f_style", "rounded" to "f_rounded", "serif" to "f_serif", "mono" to "f_mono", "hand" to "f_hand").forEach { (id, k) ->
            val on = st.font == id
            val ff = fontFamily(id)
            Box(
                Modifier.clip(RoundedCornerShape(99.dp)).background(if (on) T.acc else T.s2)
                    .tap { Store.update(ctx) { it.copy(font = id) } }.padding(horizontal = 16.dp, vertical = 9.dp),
            ) {
                Text(Tr.t(lang, k), style = TextStyle(fontFamily = ff ?: androidx.compose.ui.text.font.FontFamily.Default),
                    color = if (on) T.accTx else T.tx, fontSize = if (id == "hand") 17.sp else 14.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    Text(Tr.t(lang, "f_widget_note"), color = T.tx2, fontSize = 12.5.sp, modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 6.dp))
}


/* ===================== Размер интерфейса и время в ленте ===================== */

@Composable
fun UiScaleAndTime(st: AppState) {
    val ctx = LocalContext.current
    val lang = st.appLang
    Sec(Tr.t(lang, "ui_scale"))
    Card {
        Column(Modifier.padding(16.dp)) {
            // 80–130 % с шагом 5. Применяем при отпускании: если менять масштаб во время
            // перетаскивания, слайдер меняет размер под пальцем и значение начинает прыгать.
            var v by androidx.compose.runtime.remember(st.uiScale) { androidx.compose.runtime.mutableFloatStateOf((st.uiScale - .8f) / .5f) }
            GradSlider(
                "", v, listOf(T.s2, T.acc), T.acc,
                fmt = { "${(80 + it * 50).roundToInt() / 5 * 5}%" },
                onChange = { v = it },
                onEnd = {
                    val k = (((80 + v * 50).roundToInt() / 5 * 5) / 100f).coerceIn(.8f, 1.3f)
                    Store.update(ctx) { it.copy(uiScale = k) }
                },
            )
        }
    }
    Sec(Tr.t(lang, "time_side"))
    Chips(listOf("auto" to Tr.t(lang, "ts_auto"), "left" to Tr.t(lang, "ts_left"), "right" to Tr.t(lang, "ts_right")), st.timeSide) { v ->
        Store.update(ctx) { it.copy(timeSide = v) }
    }
}

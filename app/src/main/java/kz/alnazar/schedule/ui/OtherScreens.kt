package kz.alnazar.schedule.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.alnazar.schedule.data.AppState
import kz.alnazar.schedule.data.Bell
import kz.alnazar.schedule.data.Catalog
import kz.alnazar.schedule.data.Store
import kz.alnazar.schedule.data.WidgetCfg
import kz.alnazar.schedule.logic.Sched
import kz.alnazar.schedule.logic.Tr
import kz.alnazar.schedule.widget.WidgetUpdater

/* ===================== ВИДЖЕТЫ ===================== */

/** «Обои» рабочего стола из прототипа (.wall): три радиальных пятна на синем. */
fun Modifier.wall(): Modifier = this.drawBehind {
    drawRect(Color(0xFF1B2A55))
    drawRect(Brush.radialGradient(listOf(Color(0xFFFF8A5C), Color.Transparent), Offset(size.width * .18f, size.height * .12f), size.maxDimension * .42f))
    drawRect(Brush.radialGradient(listOf(Color(0xFF7A5CFF), Color.Transparent), Offset(size.width * .88f, size.height * .28f), size.maxDimension * .48f))
    drawRect(Brush.radialGradient(listOf(Color(0xFF19C7A4), Color.Transparent), Offset(size.width * .35f, size.height * .92f), size.maxDimension * .52f))
}

@Composable
internal fun FakeApp(emoji: String) {
    Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = .22f)), contentAlignment = Alignment.Center) {
        Text(emoji, fontSize = 26.sp)
    }
}

/* ===================== НАСТРОЙКИ ===================== */

private class SRow(val emoji: String, val color: Long, val title: String, val sub: String, val enabled: Boolean = true, val onClick: () -> Unit)

@Composable
fun SettingsScreen(st: AppState) {
    val ui = LocalUi.current
    val ctx = LocalContext.current
    val lang = st.appLang
    val co = T.id == "color"
    var query by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }

    val b = st.bells
    val hn = Sched.holiday(st, java.time.LocalDate.now())
    val nOn = listOfNotNull(
        if (st.notif.evening) Tr.hm(st.notif.eveningAt) else null,
        if (st.notif.before) Tr.t(lang, "n_min", st.notif.beforeMin) else null,
    )
    val exactOk = WidgetUpdater.canExact(ctx)
    val sections = listOf(
        Tr.t(lang, "set_look") to listOf(
            SRow("🧩", 0xFF30D158, Tr.t(lang, "tab_widgets"), Tr.t(lang, "w_sub")) { ui.go("widgets") },
            SRow("🎨", 0xFFBF5AF2, Tr.t(lang, "appearance"), (mapOf("ios26" to "iOS 26", "pixel" to "Pixel OS", "ios18" to "iOS 18", "color" to "ColorOS 17", "origin" to "OriginOS 7", "oneui" to "One UI 9.5")[st.style] ?: "iOS 26") + " · " + Tr.t(lang, if (st.theme == "custom") "th_custom" else "th_dark")) { ui.go("appearance") },
            SRow("🌐", 0xFF0A84FF, Tr.t(lang, "language"), "${st.appLang.uppercase()} · ${Tr.t(lang, "tab_widgets")}: ${st.widgetLang.uppercase()}") { ui.go("language") },
        ),
        Tr.t(lang, "set_school") to listOf(
            SRow("🏫", 0xFFFF9F0A, Tr.t(lang, "class_prog"), Tr.t(lang, if (st.program == "kz") "prog_kz" else "prog_ru") + " · ${st.grade} · " + Tr.t(lang, if (st.shift == 2) "shift2" else "shift1")) { ui.go("class") },
            SRow("🔔", 0xFFFF453A, Tr.t(lang, "bells"), if (b.isEmpty()) "" else Tr.hm(b.first().start) + "–" + Tr.hm(b[minOf(6, b.size - 1)].end)) { ui.go("bells") },
            SRow("🏖️", 0xFF30D158, Tr.t(lang, "holidays"), if (hn != null) hn.name + " · " + Tr.t(lang, "hol_now") else "${st.holidays.size}") { ui.go("holidays") },
            SRow("📚", 0xFF5E5CE6, Tr.t(lang, "subjects"), Tr.t(lang, "subj_mine") + ": ${st.custom.size}") { ui.go("subjects") },
        ),
        Tr.t(lang, "set_data") to listOf(
            SRow("📥", 0xFF32ADE6, Tr.t(lang, "import_title"), Tr.t(lang, "i5_sub")) { ui.go("import") },
            SRow("💾", 0xFF8E8E93, Tr.t(lang, "backup"), Tr.t(lang, "exp") + " · " + Tr.t(lang, "imp_json") + " · Google") { ui.go("backup") },
        ),
        Tr.t(lang, "set_system") to listOf(
            SRow("📝", 0xFFFF375F, Tr.t(lang, "notif"), if (nOn.isEmpty()) "—" else nOn.joinToString(" · ")) { ui.go("notif") },
            SRow("⏰", 0xFFFF9F0A, Tr.t(lang, "exactAlarm"), Tr.t(lang, if (exactOk) "exactAlarmOn" else "exactAlarmHint"), enabled = !exactOk) {
                ctx.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + ctx.packageName))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            },
        ),
    )
    val q = query.trim()

    @Composable
    fun rows(list: List<SRow>) {
        Group {
            list.forEachIndexed { i, r ->
                if (i > 0) RowDivider()
                SetRow(r.emoji, r.color, r.title, r.sub, r.enabled, r.onClick)
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Page(bottomNav = false) {
            item { HdrBack(Tr.t(lang, "set_title")) { ui.back() } }
            item {
                CompositionLocalProvider(LocalRowInset provides (if (co) 62.dp else 14.dp)) {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        if (co && q.isNotEmpty()) {
                            val hits = sections.flatMap { it.second }.filter { it.title.contains(q, true) || it.sub.contains(q, true) }
                            if (hits.isEmpty()) Empty("—") else { Spacer(Modifier.height(8.dp)); rows(hits) }
                        } else sections.forEach { (name, list) ->
                            Sec(name)
                            rows(list)
                        }
                        if (co) Spacer(Modifier.height(72.dp))
                    }
                }
            }
        }
        if (co) CoSearchPill(
            Tr.t(lang, "search"), searching, query,
            onOpen = { searching = true }, onQuery = { query = it }, onClose = { searching = false; query = "" },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/* ----- вложенные страницы настроек ----- */

@Composable
fun SubPage(id: String, st: AppState) {
    if (id == "settings") { SettingsScreen(st); return }
    if (id == "widgets") { WidgetsScreen(st, remember { System.currentTimeMillis() }, true); return }
    val ui = LocalUi.current
    val ctx = LocalContext.current
    val lang = st.appLang
    val langs = listOf("ru" to "Русский", "kk" to "Қазақша", "en" to "English")
    Page(bottomNav = false) {
        when (id) {
            "language" -> {
                item { HdrBack(Tr.t(lang, "language")) { ui.back() } }
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Sec(Tr.t(lang, "appLang"))
                        Chips(langs, st.appLang) { l -> Store.update(ctx) { it.copy(appLang = l) } }
                        Sec(Tr.t(lang, "widgetLang"))
                        Chips(langs, st.widgetLang) { l -> Store.update(ctx) { it.copy(widgetLang = l) } }
                    }
                }
            }
            "class" -> {
                item { HdrBack(Tr.t(lang, "class_prog")) { ui.back() } }
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Sec(Tr.t(lang, "program"))
                        Seg(listOf("kz" to Tr.t(lang, "prog_kz"), "ru" to Tr.t(lang, "prog_ru")), st.program) { p -> Store.update(ctx) { it.copy(program = p) } }
                        Sec(Tr.t(lang, "grade"))
                        Chips((5..11).map { it to "$it" }, st.grade) { g -> Store.update(ctx) { it.copy(grade = g) } }
                        Sec(Tr.t(lang, "shift"))
                        Seg(listOf(1 to Tr.t(lang, "shift1"), 2 to Tr.t(lang, "shift2")), st.shift) { s -> Store.update(ctx) { it.copy(shift = s) } }
                        Spacer(Modifier.height(12.dp))
                        Group { TgRow(Tr.t(lang, "sixDays"), st.sixDays) { v -> Store.update(ctx) { it.copy(sixDays = v) } } }
                        Spacer(Modifier.height(14.dp))
                        Btn(Tr.t(lang, "regen"), Modifier.fillMaxWidth(), BtnKind.Sec) { ui.open { RegenSheet() } }
                    }
                }
            }
            "bells" -> {
                item { HdrBack(Tr.t(lang, "bells")) { ui.back() } }
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Seg(listOf(1 to Tr.t(lang, "shift1"), 2 to Tr.t(lang, "shift2")), st.shift) { s -> Store.update(ctx) { it.copy(shift = s) } }
                        Spacer(Modifier.height(12.dp))
                        Group {
                            st.bells.forEachIndexed { i, b ->
                                if (i > 0) RowDivider()
                                Row(
                                    Modifier.fillMaxWidth().tap { ui.open { BellSheet(i) } }.padding(horizontal = if (T.id == "color") 18.dp else 14.dp, vertical = 15.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(Tr.t(lang, "lessonN", i + 1), color = T.tx, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    Text(Tr.hm(b.start) + " – " + Tr.hm(b.end), color = T.tx2, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
            }
            "holidays" -> {
                item { HdrBack(Tr.t(lang, "holidays")) { ui.back() } }
                item { HolidaysPage(st) }
            }
            "subjects" -> {
                item { HdrBack(Tr.t(lang, "subjects")) { ui.back() } }
                item { SubjectsPage(st) }
            }
            "backup" -> {
                item { HdrBack(Tr.t(lang, "backup")) { ui.back() } }
                item { BackupPage(st) }
            }
            "import" -> {
                item { HdrBack(Tr.t(lang, "import_title")) { ui.back() } }
                item { ImportPage(st) }
            }
            "notif" -> {
                item { HdrBack(Tr.t(lang, "notif")) { ui.back() } }
                item { NotifPage(st) }
            }
            "settings" -> { /* рисуется отдельно, см. SubPage */ }
            else -> {
                item { HdrBack(Tr.t(lang, "appearance")) { ui.back() } }
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        val n = Sched.now(st)
                        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).wall().padding(14.dp), contentAlignment = Alignment.Center) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                WidgetPreview("2x2", st, n, st.widgetLang)
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    WidgetPreview("2x1", st, n, st.widgetLang)
                                    Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) { FakeApp("📷"); FakeApp("💬") }
                                }
                            }
                        }
                        Sec(Tr.t(lang, "style"))
                        StylePicker(st)
                        Sec(Tr.t(lang, "theme"))
                        Chips(listOf("dark" to Tr.t(lang, "th_dark"), "custom" to Tr.t(lang, "th_custom")), st.theme) { th ->
                            Store.update(ctx) { it.copy(theme = th) }
                        }
                        if (st.theme == "custom") { VSpace(12.dp); CustomThemeEditor(st) }
                        FontAndCorners(st)
                        UiScaleAndTime(st)
                        IconSetPicker(st)
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.RegenSheet() {
    val ctx = LocalContext.current
    val ui = LocalUi.current
    val st = Store.state.value
    val lang = st.appLang
    SheetTitle(Tr.t(lang, "regenAsk", st.grade))
    Btns {
        Btn(Tr.t(lang, "yes"), Modifier.weight(1f)) {
            Store.update(ctx) { it.copy(week = Catalog.genTimetable(it.program, it.grade, it.sixDays), overrides = emptyList()) }
            ui.close()
        }
        Btn(Tr.t(lang, "cancel"), Modifier.weight(1f), BtnKind.Ghost) { ui.close() }
    }
}

private fun parseHm(s: String): Int? {
    val m = Regex("""^\s*(\d{1,2})[:.](\d{2})\s*$""").find(s) ?: return null
    val h = m.groupValues[1].toInt(); val mi = m.groupValues[2].toInt()
    return if (h in 0..23 && mi in 0..59) h * 60 + mi else null
}

@Composable
private fun ColumnScope.BellSheet(i: Int) {
    val ctx = LocalContext.current
    val ui = LocalUi.current
    val st = remember { Store.state.value }
    val lang = st.appLang
    val b = st.bells.getOrNull(i) ?: return
    var s by remember { mutableStateOf(Tr.hm(b.start)) }
    var e by remember { mutableStateOf(Tr.hm(b.end)) }
    val ps = parseHm(s); val pe = parseHm(e)
    val valid = ps != null && pe != null && pe > ps
    SheetTitle(Tr.t(lang, "lessonN", i + 1))
    Lab(Tr.t(lang, "start")); Field(s, { s = it.take(5) }, "08:30")
    Lab(Tr.t(lang, "end")); Field(e, { e = it.take(5) }, "09:15")
    Btns {
        Btn(Tr.t(lang, "save"), Modifier.weight(1f), enabled = valid) {
            Store.update(ctx) { a ->
                fun upd(list: List<Bell>) = list.mapIndexed { k, x -> if (k == i) Bell(ps!!, pe!!) else x }
                if (a.shift == 2) a.copy(bells2 = upd(a.bells2)) else a.copy(bells1 = upd(a.bells1))
            }
            ui.close()
        }
        Btn(Tr.t(lang, "cancel"), Modifier.weight(1f), BtnKind.Ghost) { ui.close() }
    }
}

/** Выбор набора иконок предметов + цвет монохромных. */
@Composable
fun IconSetPicker(st: AppState) {
    val ctx = LocalContext.current
    val lang = st.appLang
    val sets = listOf("android" to "ic_android", "fluent" to "ic_fluent", "twemoji" to "ic_tw", "mono" to "ic_mono")
    val sample = listOf("algebra", "phys", "pe", "inf").mapNotNull { Catalog.find(it) }
    Sec(Tr.t(lang, "icons"))
    sets.chunked(2).forEach { pair ->
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pair.forEach { (id, key) ->
                val on = st.iconSet == id
                val shape = RoundedCornerShape(T.rc * .7f)
                Column(
                    Modifier.weight(1f).glass(shape).border(2.dp, if (on) T.acc else Color.Transparent, shape)
                        .tap { Store.update(ctx) { it.copy(iconSet = id) } }.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    androidx.compose.runtime.CompositionLocalProvider(LocalIcons provides IconPrefs(id, st.iconTint)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { sample.forEach { Tile(it.emoji, it.color, 30.dp) } }
                    }
                    Text(Tr.t(lang, key), color = T.tx, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    if (st.iconSet == "mono") {
        Lab(Tr.t(lang, "ic_tint"))
        Seg(listOf("subject" to Tr.t(lang, "tint_subject"), "accent" to Tr.t(lang, "tint_accent"), "bw" to Tr.t(lang, "tint_bw")), st.iconTint) { v ->
            Store.update(ctx) { it.copy(iconTint = v) }
        }
    }
    Text(Tr.t(lang, "credits"), color = T.tx2, fontSize = 11.sp, modifier = Modifier.padding(top = 12.dp, start = 4.dp, end = 4.dp))
}

/** Карточки стилей с превью виджета 2×1 в этом стиле. Готовы iOS 26 и Pixel OS, остальные — позже. */
@Composable
fun StylePicker(st: AppState) {
    val ctx = LocalContext.current
    val styles = listOf("ios26" to ("iOS 26" to "Liquid Glass"), "pixel" to ("Pixel OS" to "Material 3 Expressive"),
        "ios18" to ("iOS 18" to "Classic"), "color" to ("ColorOS 17" to "Luminous · стекло"), "origin" to ("OriginOS 7" to "Origin Design"), "oneui" to ("One UI 9.5" to "Samsung"))
    val n = Sched.now(st)
    styles.chunked(2).forEach { pair ->
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            pair.forEach { (id, names) ->
                val ready = true
                val on = st.style == id
                val shape = RoundedCornerShape(T.rc * .8f)
                Column(
                    Modifier.weight(1f).glass(shape).border(2.dp, if (on) T.acc else Color.Transparent, shape)
                        .tap(enabled = ready) { Store.update(ctx) { it.copy(style = id) } }.padding(10.dp).graphicsAlpha(if (ready) 1f else .45f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).wall().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        if (ready) AppTheme(tokFor(id)) { Box(Modifier.graphicsLayerScale(.8f)) { WidgetPreview("2x1", st.copy(style = id), n, st.widgetLang) } }
                        else Text("скоро", color = Color.White.copy(alpha = .8f), fontSize = 13.sp, modifier = Modifier.padding(vertical = 22.dp))
                    }
                    Text(names.first, color = T.tx, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                    if (names.second.isNotEmpty()) Text(names.second, color = T.tx2, fontSize = 11.5.sp)
                }
            }
        }
    }
}

fun Modifier.graphicsLayerScale(k: Float): Modifier = this.then(
    Modifier.layout { m, c ->
        val p = m.measure(c.copy(minWidth = 0, minHeight = 0, maxWidth = androidx.compose.ui.unit.Constraints.Infinity, maxHeight = androidx.compose.ui.unit.Constraints.Infinity))
        val w = (p.width * k).toInt(); val h = (p.height * k).toInt()
        layout(w, h) { p.placeWithLayer((w - p.width) / 2, (h - p.height) / 2) { scaleX = k; scaleY = k } }
    },
)

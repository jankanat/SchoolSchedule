package kz.alnazar.schedule.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.alnazar.schedule.data.AppState
import kz.alnazar.schedule.data.Bell
import kz.alnazar.schedule.data.Catalog
import kz.alnazar.schedule.data.Store
import kz.alnazar.schedule.logic.Tr
import kz.alnazar.schedule.widget.WidgetUpdater

@Composable
fun SettingsScreen(st: AppState) {
    val ctx = LocalContext.current
    val lang = st.appLang
    var askRegen by remember { mutableStateOf(false) }
    var bellEdit by remember { mutableStateOf(-1) }
    val langs = listOf("ru" to "Русский", "kk" to "Қазақша", "en" to "English")

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Text(Tr.t(lang, "tab_settings"), color = Pal.tx, fontSize = 30.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp))
        }

        item { SectionTitle(Tr.t(lang, "grade")) }
        item { ChipRow((5..11).map { it to "$it" }, st.grade) { g -> Store.update(ctx) { it.copy(grade = g) } } }
        item { SectionTitle(Tr.t(lang, "program")) }
        item {
            ChipRow(listOf("kz" to Tr.t(lang, "prog_kz"), "ru" to Tr.t(lang, "prog_ru")), st.program) { p ->
                Store.update(ctx) { it.copy(program = p) }
            }
        }
        item {
            Group {
                Text(
                    Tr.t(lang, "regen"), color = Pal.acc, fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth().clickable { askRegen = true }.padding(16.dp),
                )
            }
        }

        item { SectionTitle(Tr.t(lang, "shift")) }
        item {
            ChipRow(listOf(1 to Tr.t(lang, "shift1"), 2 to Tr.t(lang, "shift2")), st.shift) { sh ->
                Store.update(ctx) { it.copy(shift = sh) }
            }
        }
        item {
            Group {
                ToggleRow(Tr.t(lang, "sixDays"), st.sixDays) { v -> Store.update(ctx) { it.copy(sixDays = v) } }
            }
        }

        item { SectionTitle(Tr.t(lang, "bells") + " · " + Tr.t(lang, if (st.shift == 2) "shift2" else "shift1")) }
        item {
            Group {
                st.bells.forEachIndexed { i, b ->
                    if (i > 0) HorizontalDivider(color = Pal.line, modifier = Modifier.padding(start = 16.dp))
                    Row(
                        Modifier.fillMaxWidth().clickable { bellEdit = i }.padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(Tr.t(lang, "lessonN", i + 1), color = Pal.tx, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        Text(Tr.hm(b.start) + " – " + Tr.hm(b.end), color = Pal.tx2, fontSize = 16.sp)
                    }
                }
            }
        }

        item { SectionTitle(Tr.t(lang, "appLang")) }
        item { ChipRow(langs, st.appLang) { l -> Store.update(ctx) { it.copy(appLang = l) } } }
        item { SectionTitle(Tr.t(lang, "widgetLang")) }
        item { ChipRow(langs, st.widgetLang) { l -> Store.update(ctx) { it.copy(widgetLang = l) } } }

        item { SectionTitle(Tr.t(lang, "onLesson")) }
        item {
            ChipRow(listOf("current" to Tr.t(lang, "opt_current"), "next" to Tr.t(lang, "opt_next")), st.widget.onLesson) { v ->
                Store.update(ctx) { it.copy(widget = it.widget.copy(onLesson = v)) }
            }
        }
        item { SectionTitle(Tr.t(lang, "onBreak")) }
        item {
            ChipRow(listOf("next" to Tr.t(lang, "opt_next"), "prev" to Tr.t(lang, "opt_prev")), st.widget.onBreak) { v ->
                Store.update(ctx) { it.copy(widget = it.widget.copy(onBreak = v)) }
            }
        }
        item { SectionTitle(Tr.t(lang, "idle")) }
        item {
            Group {
                ToggleRow(Tr.t(lang, "idleNo"), st.widget.idleNoLessons) { v ->
                    Store.update(ctx) { it.copy(widget = it.widget.copy(idleNoLessons = v)) }
                }
                HorizontalDivider(color = Pal.line, modifier = Modifier.padding(start = 16.dp))
                ToggleRow(Tr.t(lang, "idleNext"), st.widget.idleNextDay) { v ->
                    Store.update(ctx) { it.copy(widget = it.widget.copy(idleNextDay = v)) }
                }
            }
        }

        item { SectionTitle(Tr.t(lang, "exactAlarm")) }
        item {
            val ok = WidgetUpdater.canExact(ctx)
            Group {
                Row(
                    Modifier.fillMaxWidth().clickable(enabled = !ok) {
                        ctx.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + ctx.packageName))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(Tr.t(lang, "exactAlarm"), color = Pal.tx, fontSize = 16.sp)
                        Text(Tr.t(lang, "exactAlarmHint"), color = Pal.tx2, fontSize = 13.sp)
                    }
                    Text(Tr.t(lang, if (ok) "exactAlarmOn" else "exactAlarmOff"), color = if (ok) Pal.tx2 else Pal.acc, fontSize = 15.sp)
                }
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }

    if (askRegen) AlertDialog(
        onDismissRequest = { askRegen = false },
        containerColor = Pal.card,
        text = { Text(Tr.t(lang, "regenAsk", st.grade), color = Pal.tx) },
        confirmButton = {
            TextButton(onClick = {
                Store.update(ctx) { it.copy(week = Catalog.genTimetable(it.program, it.grade, it.sixDays), overrides = emptyList()) }
                askRegen = false
            }) { Text(Tr.t(lang, "yes"), color = Pal.red) }
        },
        dismissButton = { TextButton(onClick = { askRegen = false }) { Text(Tr.t(lang, "cancel"), color = Pal.tx2) } },
    )

    if (bellEdit >= 0) BellDialog(st, bellEdit) { bellEdit = -1 }
}

@Composable
private fun ToggleRow(title: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!value) }.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = Pal.tx, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = value, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Pal.acc, checkedThumbColor = Pal.tx),
        )
    }
}

private fun parseHm(s: String): Int? {
    val m = Regex("""^\s*(\d{1,2})[:.](\d{2})\s*$""").find(s) ?: return null
    val h = m.groupValues[1].toInt(); val mi = m.groupValues[2].toInt()
    return if (h in 0..23 && mi in 0..59) h * 60 + mi else null
}

@Composable
private fun BellDialog(st: AppState, i: Int, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val lang = st.appLang
    val b = st.bells[i]
    var s by remember { mutableStateOf(Tr.hm(b.start)) }
    var e by remember { mutableStateOf(Tr.hm(b.end)) }
    val ps = parseHm(s); val pe = parseHm(e)
    val valid = ps != null && pe != null && pe > ps
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = Pal.card,
        title = { Text(Tr.t(lang, "lessonN", i + 1), color = Pal.tx) },
        text = {
            Column {
                OutlinedTextField(s, { s = it.take(5) }, label = { Text(Tr.t(lang, "start")) }, singleLine = true, isError = ps == null)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(e, { e = it.take(5) }, label = { Text(Tr.t(lang, "end")) }, singleLine = true, isError = pe == null || (ps != null && pe <= ps))
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                Store.update(ctx) { a ->
                    fun upd(list: List<Bell>) = list.mapIndexed { k, x -> if (k == i) Bell(ps!!, pe!!) else x }
                    if (a.shift == 2) a.copy(bells2 = upd(a.bells2)) else a.copy(bells1 = upd(a.bells1))
                }
                onClose()
            }) { Text(Tr.t(lang, "save"), color = if (valid) Pal.acc else Pal.tx3) }
        },
        dismissButton = { TextButton(onClick = onClose) { Text(Tr.t(lang, "cancel"), color = Pal.tx2) } },
    )
}

package kz.alnazar.schedule.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import java.time.LocalDate
import kz.alnazar.schedule.logic.Sched
import kz.alnazar.schedule.logic.Tr

/** Всё, что нужно форме записи: значения и действия. Раскладка у каждого стиля своя. */
class HwUi(
    val isNew: Boolean, val lang: String,
    val kind: String, val setKind: (String) -> Unit,
    val subj: String?, val setSubj: (String?) -> Unit, val subjects: List<Subj4>,
    val text: String, val setText: (String) -> Unit,
    val dueOptions: List<DueOpt>,
    val photos: List<String>, val removePhoto: (String) -> Unit, val camera: () -> Unit, val gallery: () -> Unit,
    val canSave: Boolean, val save: () -> Unit, val delete: (() -> Unit)?, val close: () -> Unit,
)
class Subj4(val id: String?, val name: String, val emoji: String, val color: Long)
class DueOpt(val label: String, val on: Boolean, val onClick: () -> Unit)

private fun HwUi.title() = Tr.t(lang, if (isNew) "hw_new" else "hw_edit")
private fun HwUi.textLabel() = Tr.t(lang, if (kind == "hw") "hw_text" else "hw_note_text")

@Composable
fun ColumnScope.HwForm(u: HwUi) {
    when {
        T.pixel -> FormPixel(u)
        T.id == "ios26" || T.id == "ios18" -> FormIos(u)
        T.id == "origin" -> FormOrigin(u)
        T.id == "oneui" -> FormOneUi(u)
        else -> FormColor(u)
    }
}

/* ---------- общие кусочки ---------- */

@Composable
private fun PhotoRow(u: HwUi, small: Boolean = true) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        u.photos.toList().forEach { p ->
            Box {
                Thumb(p, 64)
                Box(Modifier.align(Alignment.TopEnd).padding(3.dp).size(20.dp).clip(CircleShape).background(Color(0xCC000000)).tap { u.removePhoto(p) }, contentAlignment = Alignment.Center) {
                    Text("✕", color = Color.White, fontSize = 11.sp)
                }
            }
        }
        Btn(Tr.t(u.lang, "hw_camera"), kind = BtnKind.Sec, small = small, icon = Ic.cam) { u.camera() }
        Btn(Tr.t(u.lang, "hw_gallery"), kind = BtnKind.Sec, small = small, icon = Ic.image) { u.gallery() }
    }
}

/** Многострочное поле без собственной рамки — рамку и фон задаёт стиль. */
@Composable
private fun BareField(value: String, onChange: (String) -> Unit, hint: String, lines: Int = 3, modifier: Modifier = Modifier) {
    val t = T
    BasicTextField(
        value = value, onValueChange = { onChange(it.take(2000)) }, minLines = lines, maxLines = 12,
        textStyle = TextStyle(color = t.tx, fontSize = 16.sp), cursorBrush = SolidColor(t.acc), modifier = modifier.fillMaxWidth(),
        decorationBox = { inner -> Box { if (value.isEmpty()) Text(hint, color = t.tx2, fontSize = 16.sp); inner() } },
    )
}

@Composable
private fun SubjTile(s: Subj4, on: Boolean, round: Boolean, w: Int, onClick: () -> Unit) {
    val t = T
    val shape = if (round) CircleShape else RoundedCornerShape(20.dp)
    val bg by animateColorAsState(if (on) t.accSoft else t.s2, label = "stb")
    Column(Modifier.width(w.dp).clip(RoundedCornerShape(20.dp)).tap(onClick = onClick).padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(if (round) 58.dp else 52.dp).clip(shape).background(bg).border(2.dp, if (on) t.acc else Color.Transparent, shape), contentAlignment = Alignment.Center) {
            Tile(s.emoji, s.color, if (round) 38.dp else 34.dp)
        }
        Text(s.name, color = if (on) t.tx else t.tx2, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
    }
}

/* ---------- ColorOS 17: крупные объёмные карточки, предметы сеткой в два ряда ---------- */
@Composable
private fun ColumnScope.FormColor(u: HwUi) {
    val t = T
    Text(u.title(), color = t.tx, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
    if (u.isNew) Seg(listOf("hw" to Tr.t(u.lang, "type_hw"), "note" to Tr.t(u.lang, "type_note")), u.kind) { u.setKind(it) }
    Lab(Tr.t(u.lang, "subject"))
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        u.subjects.chunked(2).forEach { col ->
            Column { col.forEach { s -> SubjTile(s, u.subj == s.id, false, 82) { u.setSubj(s.id) } } }
        }
    }
    Lab(u.textLabel())
    Box(Modifier.fillMaxWidth().vol3d(RoundedCornerShape(22.dp)).padding(16.dp)) { BareField(u.text, u.setText, "", 3) }
    if (u.kind == "hw") {
        Lab(Tr.t(u.lang, "hw_due"))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) { u.dueOptions.forEach { Chip(it.label, it.on, it.onClick) } }
    }
    Lab(Tr.t(u.lang, "hw_photo")); PhotoRow(u)
    Btns {
        Btn(Tr.t(u.lang, "save"), Modifier.weight(1f), enabled = u.canSave) { u.save() }
        u.delete?.let { d -> Btn("", kind = BtnKind.Danger, icon = Ic.trash) { d() } }
    }
}

/* ---------- iOS 26: шапка «Отмена · заголовок · Добавить», круглые иконки, группы-стекло, строки с галочкой ---------- */
@Composable
private fun ColumnScope.FormIos(u: HwUi) {
    val t = T
    val blue = Color(0xFF0A84FF)
    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(Tr.t(u.lang, "cancel"), color = blue, fontSize = 17.sp, modifier = Modifier.clip(RoundedCornerShape(99.dp)).tap { u.close() }.padding(8.dp))
        Text(u.title(), color = t.tx, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        Text(Tr.t(u.lang, "save"), color = if (u.canSave) blue else t.tx2, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clip(RoundedCornerShape(99.dp)).tap(enabled = u.canSave) { u.save() }.padding(8.dp))
    }
    if (u.isNew) Seg(listOf("hw" to Tr.t(u.lang, "type_hw"), "note" to Tr.t(u.lang, "type_note")), u.kind) { u.setKind(it) }
    Spacer(Modifier.height(14.dp))
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        u.subjects.forEach { s -> SubjTile(s, u.subj == s.id, true, 74) { u.setSubj(s.id) } }
    }
    Spacer(Modifier.height(14.dp))
    Box(Modifier.fillMaxWidth().liquid(RoundedCornerShape(22.dp)).padding(16.dp)) { BareField(u.text, u.setText, u.textLabel(), 3) }
    if (u.kind == "hw") {
        Spacer(Modifier.height(14.dp))
        Column(Modifier.fillMaxWidth().liquid(RoundedCornerShape(22.dp))) {
            u.dueOptions.forEachIndexed { i, o ->
                if (i > 0) Box(Modifier.padding(start = 16.dp).fillMaxWidth().height(.5.dp).background(t.line))
                Row(Modifier.fillMaxWidth().tap(onClick = o.onClick).padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(o.label, color = t.tx, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    AnimTick(o.on, blue, 20.dp, 2.2.dp)
                }
            }
        }
    }
    Spacer(Modifier.height(14.dp)); PhotoRow(u)
    u.delete?.let { d -> Spacer(Modifier.height(12.dp)); Text(Tr.t(u.lang, "delete"), color = Color(0xFFFF453A), fontSize = 17.sp, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().liquid(RoundedCornerShape(22.dp)).tap { d() }.padding(14.dp)) }
}

/* ---------- Pixel: Material 3 — закрытие, фильтр-чипы, контурное поле с меткой, расширенная кнопка ---------- */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.FormPixel(u: HwUi) {
    val t = T
    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).tap { u.close() }, contentAlignment = Alignment.Center) { Icon(Ic.close, null, tint = t.tx2, modifier = Modifier.size(22.dp)) }
        Text(u.title(), color = t.tx, fontSize = 22.sp, fontWeight = FontWeight.Normal, modifier = Modifier.weight(1f).padding(start = 8.dp))
    }
    if (u.isNew) HeadSeg(listOf("hw" to Tr.t(u.lang, "type_hw"), "note" to Tr.t(u.lang, "type_note")), u.kind, wide = true) { u.setKind(it) }
    Text(Tr.t(u.lang, "subject"), color = t.acc, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        u.subjects.forEach { s -> Chip((if (s.id == null) "" else s.emoji + "  ") + s.name, u.subj == s.id) { u.setSubj(s.id) } }
    }
    // контурное поле: метка «сидит» на рамке
    Box(Modifier.padding(top = 20.dp).fillMaxWidth()) {
        Box(Modifier.padding(top = 8.dp).fillMaxWidth().border(1.dp, t.outline, RoundedCornerShape(12.dp)).padding(16.dp)) { BareField(u.text, u.setText, "", 3) }
        Text(u.textLabel(), color = t.acc, fontSize = 12.sp, modifier = Modifier.padding(start = 12.dp).background(t.sheet).padding(horizontal = 4.dp))
    }
    if (u.kind == "hw") {
        Text(Tr.t(u.lang, "hw_due"), color = t.acc, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { u.dueOptions.forEach { Chip(it.label, it.on, it.onClick) } }
    }
    Spacer(Modifier.height(16.dp)); PhotoRow(u)
    Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        u.delete?.let { d -> Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(Color(0x2EFF453A)).tap { d() }, contentAlignment = Alignment.Center) { Icon(Ic.trash, null, tint = Color(0xFFFF453A), modifier = Modifier.size(22.dp)) } }
        val bg by animateColorAsState(if (u.canSave) t.acc else t.s2, label = "sv")
        Box(Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(16.dp)).background(bg).tap(enabled = u.canSave) { u.save() }, contentAlignment = Alignment.Center) {
            Text(Tr.t(u.lang, "save"), color = if (u.canSave) t.accTx else t.tx2, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/* ---------- OriginOS 7: стеклянные плитки предметов сеткой, капсула сроков, синяя кнопка со свечением ---------- */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.FormOrigin(u: HwUi) {
    val t = T
    Text(u.title(), color = t.tx, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
    if (u.isNew) Seg(listOf("hw" to Tr.t(u.lang, "type_hw"), "note" to Tr.t(u.lang, "type_note")), u.kind) { u.setKind(it) }
    Spacer(Modifier.height(12.dp))
    val rows = u.subjects.chunked(4)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        rows.take(3).forEach { r ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                r.forEach { s ->
                    val on = u.subj == s.id
                    val shape = RoundedCornerShape(20.dp)
                    Column(Modifier.weight(1f).clip(shape).vivoGlass(shape).border(1.5.dp, if (on) t.acc else Color.Transparent, shape).tap { u.setSubj(s.id) }.padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Tile(s.emoji, s.color, 36.dp)
                        Text(s.name, color = if (on) t.tx else t.tx2, fontSize = 10.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp, start = 3.dp, end = 3.dp))
                    }
                }
                repeat(4 - r.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    if (u.subjects.size > 12) Text("+${u.subjects.size - 12}", color = t.tx2, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp, start = 6.dp))
    Spacer(Modifier.height(12.dp))
    val tf = RoundedCornerShape(24.dp)
    Box(Modifier.fillMaxWidth().vivoGlass(tf).padding(16.dp)) { BareField(u.text, u.setText, u.textLabel(), 3) }
    if (u.kind == "hw") {
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().vivoGlass(RoundedCornerShape(99.dp)).padding(4.dp)) {
            u.dueOptions.forEach { o ->
                val bg by animateColorAsState(if (o.on) Color.White.copy(alpha = .9f) else Color.Transparent, label = "do")
                Box(Modifier.weight(1f).clip(RoundedCornerShape(99.dp)).background(bg).tap(onClick = o.onClick).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                    Text(o.label, color = if (o.on) Color(0xFF1D4FB0) else t.tx2, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 6.dp))
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp)); PhotoRow(u)
    Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        u.delete?.let { d -> Btn("", kind = BtnKind.Danger, icon = Ic.trash) { d() } }
        Btn(Tr.t(u.lang, "save"), Modifier.weight(1f), enabled = u.canSave) { u.save() }
    }
}

/* ---------- One UI 9.5: крупный центральный заголовок, карточки, выпадающий выбор предмета, две кнопки внизу ---------- */
@Composable
private fun ColumnScope.FormOneUi(u: HwUi) {
    val t = T
    Text(u.title(), color = t.tx, fontSize = 26.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp))
    if (u.isNew) Seg(listOf("hw" to Tr.t(u.lang, "type_hw"), "note" to Tr.t(u.lang, "type_note")), u.kind) { u.setKind(it) }
    Spacer(Modifier.height(12.dp))
    var open by remember { mutableStateOf(u.subj == null) }
    val cur = u.subjects.firstOrNull { it.id == u.subj }
    val card = RoundedCornerShape(26.dp)
    Column(Modifier.fillMaxWidth().animateContentSize(spring(.8f, 500f)).clip(card).frost(card)) {
        Row(Modifier.fillMaxWidth().tap { open = !open }.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (cur != null) { Tile(cur.emoji, cur.color, 34.dp); Spacer(Modifier.width(12.dp)) }
            Column(Modifier.weight(1f)) {
                Text(Tr.t(u.lang, "subject"), color = t.acc, fontSize = 13.sp)
                Text(cur?.name ?: "—", color = t.tx, fontSize = 17.sp)
            }
            Icon(if (open) Ic.chevL else Ic.chevR, null, tint = t.tx2, modifier = Modifier.size(20.dp))
        }
        if (open) {
            Box(Modifier.padding(start = 18.dp).fillMaxWidth().height(.5.dp).background(t.line))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 8.dp)) {
                u.subjects.forEach { s -> SubjTile(s, u.subj == s.id, false, 80) { u.setSubj(s.id); open = false } }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    Box(Modifier.fillMaxWidth().clip(card).frost(card).padding(18.dp)) { BareField(u.text, u.setText, u.textLabel(), 3) }
    if (u.kind == "hw") {
        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxWidth().clip(card).frost(card).padding(vertical = 4.dp)) {
            u.dueOptions.forEachIndexed { i, o ->
                if (i > 0) Box(Modifier.padding(start = 18.dp).fillMaxWidth().height(.5.dp).background(t.line))
                Row(Modifier.fillMaxWidth().tap(onClick = o.onClick).padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(o.label, color = if (o.on) t.acc else t.tx, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    AnimTick(o.on, t.acc, 20.dp, 2.2.dp)
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp)); PhotoRow(u)
    Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(99.dp)).background(t.s2).tap { u.close() }, contentAlignment = Alignment.Center) { Text(Tr.t(u.lang, "cancel"), color = t.tx, fontSize = 16.sp) }
        val bg by animateColorAsState(if (u.canSave) t.acc else t.s2, label = "sv")
        Box(Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(99.dp)).background(bg).tap(enabled = u.canSave) { u.save() }, contentAlignment = Alignment.Center) {
            Text(Tr.t(u.lang, "save"), color = if (u.canSave) t.accTx else t.tx2, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
    u.delete?.let { d -> Text(Tr.t(u.lang, "delete"), color = Color(0xFFFF453A), fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).tap { d() }.padding(10.dp)) }
}

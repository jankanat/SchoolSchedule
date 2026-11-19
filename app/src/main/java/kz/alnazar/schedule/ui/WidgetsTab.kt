package kz.alnazar.schedule.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.alnazar.schedule.data.AppState
import kz.alnazar.schedule.data.Store
import kz.alnazar.schedule.data.WidgetCfg
import kz.alnazar.schedule.logic.Sched
import kz.alnazar.schedule.logic.Tr
import kz.alnazar.schedule.widget.W12Receiver
import kz.alnazar.schedule.widget.W21Receiver
import kz.alnazar.schedule.widget.W22Receiver
import kz.alnazar.schedule.widget.W24Receiver
import kz.alnazar.schedule.widget.W42Receiver

/* ===================== ВИДЖЕТЫ: у каждого стиля своя вкладка ===================== */

private val SIZES = listOf("2x2", "4x2", "2x4", "2x1", "1x2")

private fun pin(ctx: android.content.Context, size: String, hint: String) {
    val cls = when (size) {
        "2x2" -> W22Receiver::class.java; "2x1" -> W21Receiver::class.java; "1x2" -> W12Receiver::class.java
        "2x4" -> W24Receiver::class.java; else -> W42Receiver::class.java
    }
    val m = AppWidgetManager.getInstance(ctx)
    if (m.isRequestPinAppWidgetSupported) m.requestPinAppWidget(ComponentName(ctx, cls), null, null)
    else Toast.makeText(ctx, hint, Toast.LENGTH_LONG).show()
}

private fun addLabel(lang: String) = when (lang) { "kk" -> "Жұмыс үстеліне қосу"; "en" -> "Add to home screen"; else -> "Добавить на рабочий стол" }
private fun addShort(lang: String) = when (lang) { "kk" -> "Қосу"; "en" -> "Add"; else -> "Добавить" }
private fun addWidget(lang: String) = when (lang) { "kk" -> "Виджетті қосу"; "en" -> "Add Widget"; else -> "Добавить виджет" }

/** Обои под превью: у каждого ОС свой характер. */
private fun Modifier.wallFor(id: String): Modifier = when (id) {
    "color" -> this.drawBehind {
        drawRect(Color(0xFF0F2A22))
        drawRect(Brush.radialGradient(listOf(Color(0xFF2BD17E), Color.Transparent), Offset(size.width * .2f, size.height * .1f), size.maxDimension * .6f))
        drawRect(Brush.radialGradient(listOf(Color(0xFF0E7C66), Color.Transparent), Offset(size.width * .9f, size.height * .9f), size.maxDimension * .6f))
    }
    "pixel" -> this.drawBehind {
        drawRect(Color(0xFF1B1B22))
        drawRect(Brush.radialGradient(listOf(Color(0x558AB4F8), Color.Transparent), Offset(size.width * .8f, size.height * .15f), size.maxDimension * .55f))
    }
    "origin" -> this.drawBehind {
        drawRect(Color(0xFF241A18))
        drawRect(Brush.radialGradient(listOf(Color(0xFFFF8A3D), Color.Transparent), Offset(size.width * .15f, size.height * .1f), size.maxDimension * .5f))
        drawRect(Brush.radialGradient(listOf(Color(0xFF6B4BFF), Color.Transparent), Offset(size.width * .95f, size.height * .6f), size.maxDimension * .5f))
    }
    "oneui" -> this.drawBehind {
        drawRect(Color(0xFF0B1A2E))
        drawRect(Brush.radialGradient(listOf(Color(0xFF2F6BFF), Color.Transparent), Offset(size.width * .5f, size.height * .05f), size.maxDimension * .6f))
    }
    else -> this.wall()
}

@Composable
fun WidgetsScreen(st: AppState, clock: Long) {
    val ctx = LocalContext.current
    val lang = st.appLang
    val n = remember(st, clock) { Sched.now(st) }
    val wl = st.widgetLang
    var sel by rememberSaveable { mutableStateOf("2x2") }
    fun upd(f: (WidgetCfg) -> WidgetCfg) = Store.update(ctx) { it.copy(widget = f(it.widget)) }
    val id = T.id
    val hint = Tr.t(lang, "w_add_hint")

    Page {
        // ----- заголовок -----
        item {
            when {
                id == "oneui" -> Column(Modifier.fillMaxWidth().padding(top = 30.dp, bottom = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(Tr.t(lang, "tab_widgets"), color = T.tx, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text(Tr.t(lang, "w_sub"), color = T.tx2, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                }
                else -> ScreenHead(Tr.t(lang, "tab_widgets"), Tr.t(lang, "w_sub"))
            }
        }

        // ----- превью + выбор размера + кнопка добавления -----
        item {
            when (id) {
                "origin" -> OriginDesk(st, n, wl, sel, { sel = it }) { pin(ctx, sel, hint) }
                "ios26", "ios18" -> IosGallery(st, n, wl, lang, sel, { sel = it }) { pin(ctx, sel, hint) }
                "pixel" -> PixelPicker(st, n, wl, lang, sel, { sel = it }) { pin(ctx, sel, hint) }
                "oneui" -> OneUiPicker(st, n, wl, lang, sel, { sel = it }) { pin(ctx, sel, hint) }
                else -> ColorPicker(st, n, wl, lang, sel, { sel = it }) { pin(ctx, sel, hint) }
            }
        }

        // ----- настройки -----
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Sec(Tr.t(lang, "w_settings"))
                Group {
                    Column(Modifier.padding(14.dp)) {
                        Text(Tr.t(lang, "onLesson"), color = T.tx, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
                        Seg(listOf("current" to Tr.t(lang, "opt_current"), "next" to Tr.t(lang, "opt_next")), st.widget.onLesson) { v -> upd { it.copy(onLesson = v) } }
                    }
                    RowDivider()
                    Column(Modifier.padding(14.dp)) {
                        Text(Tr.t(lang, "onBreak"), color = T.tx, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
                        Seg(listOf("next" to Tr.t(lang, "opt_next"), "prev" to Tr.t(lang, "opt_prev")), st.widget.onBreak) { v -> upd { it.copy(onBreak = v) } }
                    }
                    RowDivider(); TgRow(Tr.t(lang, "w_room"), st.widget.room) { v -> upd { it.copy(room = v) } }
                    RowDivider(); TgRow(Tr.t(lang, "w_time"), st.widget.time) { v -> upd { it.copy(time = v) } }
                    RowDivider(); TgRow(Tr.t(lang, "w_hw"), st.widget.hw) { v -> upd { it.copy(hw = v) } }
                    RowDivider(); TgRow(Tr.t(lang, "idleNo"), st.widget.idleNoLessons) { v -> upd { it.copy(idleNoLessons = v) } }
                    RowDivider(); TgRow(Tr.t(lang, "idleNext"), st.widget.idleNextDay) { v -> upd { it.copy(idleNextDay = v) } }
                }
                Sec(Tr.t(lang, "w_add"))
                Card { Text(hint, color = T.tx2, fontSize = 13.5.sp) }
            }
        }
    }
}

/* ---------- общее ---------- */

@Composable
private fun PreviewSwitch(sel: String, st: AppState, n: kz.alnazar.schedule.logic.Now, wl: String) {
    Crossfade(sel, animationSpec = tween(260), label = "wsel") { s ->
        WidgetPreview(s, st, n, wl)
    }
}

private fun sizeBox(sel: String): Pair<Int, Int> = when (sel) {
    "2x2" -> 160 to 160; "4x2" -> 328 to 160; "2x4" -> 160 to 328; "2x1" -> 160 to 76; else -> 76 to 160
}

/** Подпись размера «2×2». */
private fun sz(s: String) = s.replace("x", "×")

@Composable
private fun SizePill(text: String, on: Boolean, activeBg: Color, activeFg: Color, shape: RoundedCornerShape, onClick: () -> Unit) {
    val bg by animateColorAsState(if (on) activeBg else T.tx.copy(alpha = .08f), tween(220), label = "pill")
    val fg by animateColorAsState(if (on) activeFg else T.tx2, tween(220), label = "pillfg")
    val sc by animateFloatAsState(if (on) 1.06f else 1f, spring(.55f, 380f), label = "pills")
    Box(Modifier.graphicsLayer { scaleX = sc; scaleY = sc }.clip(shape).background(bg).tap(onClick = onClick).padding(horizontal = 16.dp, vertical = 9.dp), contentAlignment = Alignment.Center) {
        Text(text, color = fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StageBox(sel: String, h: Int, content: @Composable () -> Unit) {
    // «сцена» растёт/сжимается под размер выбранного виджета, без прыжков
    val (_, wh) = sizeBox(sel)
    val hh by animateFloatAsState(maxOf(wh, h).toFloat(), spring(.8f, 260f), label = "stage")
    Box(Modifier.fillMaxWidth().height(hh.dp), contentAlignment = Alignment.Center) { content() }
}

/* ---------- ColorOS 17: карточка на зелёных обоях, карусель размеров, зелёная кнопка ---------- */
@Composable
private fun ColorPicker(st: AppState, n: kz.alnazar.schedule.logic.Now, wl: String, lang: String, sel: String, onSel: (String) -> Unit, onAdd: () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).wallFor("color").padding(vertical = 22.dp)) {
            StageBox(sel, 170) { PreviewSwitch(sel, st, n, wl) }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SIZES.forEach { SizePill(sz(it), it == sel, T.acc, T.accTx, RoundedCornerShape(99.dp)) { onSel(it) } }
        }
        Btn(addLabel(lang), Modifier.fillMaxWidth(), onClick = onAdd)
    }
}

/* ---------- iOS 26: галерея виджетов, точки страниц и синяя капсула ---------- */
@Composable
private fun IosGallery(st: AppState, n: kz.alnazar.schedule.logic.Now, wl: String, lang: String, sel: String, onSel: (String) -> Unit, onAdd: () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(11.dp)).background(Brush.linearGradient(listOf(Color(0xFF5E5CE6), Color(0xFF0A84FF)))), contentAlignment = Alignment.Center) {
                Text("📅", fontSize = 24.sp)
            }
            Column { Text("SchoolSchedule", color = T.tx, fontSize = 16.sp, fontWeight = FontWeight.SemiBold); Text(Tr.t(lang, "w_sub"), color = T.tx2, fontSize = 12.sp) }
        }
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).wallFor("ios26").padding(vertical = 20.dp)) {
            StageBox(sel, 170) { PreviewSwitch(sel, st, n, wl) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            SIZES.forEach { s ->
                val on = s == sel
                val w by animateFloatAsState(if (on) 18f else 7f, spring(.7f, 400f), label = "dot")
                Box(Modifier.size(w.dp, 7.dp).clip(RoundedCornerShape(99.dp)).background(if (on) T.tx else T.tx.copy(alpha = .3f)).tap { onSel(s) })
            }
        }
        Text(sz(sel), color = T.tx2, fontSize = 12.sp)
        Btn(addWidget(lang), Modifier.fillMaxWidth(.7f), onClick = onAdd)
    }
}

/* ---------- Pixel: чипы размеров и «расширенная FAB» ---------- */
@Composable
private fun PixelPicker(st: AppState, n: kz.alnazar.schedule.logic.Now, wl: String, lang: String, sel: String, onSel: (String) -> Unit, onAdd: () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SIZES.forEachIndexed { i, s ->
                val r = when { i == 0 -> RoundedCornerShape(topStart = 99.dp, bottomStart = 99.dp, topEnd = 8.dp, bottomEnd = 8.dp)
                    i == SIZES.lastIndex -> RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 99.dp, bottomEnd = 99.dp)
                    else -> RoundedCornerShape(8.dp) }
                SizePill((if (s == sel) "✓ " else "") + sz(s), s == sel, T.acc, T.accTx, r) { onSel(s) }
            }
        }
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).wallFor("pixel").padding(vertical = 22.dp)) {
            StageBox(sel, 170) { PreviewSwitch(sel, st, n, wl) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Row(Modifier.clip(RoundedCornerShape(18.dp)).background(T.acc).tap(onClick = onAdd).padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Ic.plus, null, tint = T.accTx, modifier = Modifier.size(22.dp))
                Text(addShort(lang), color = T.accTx, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/* ---------- OriginOS: мини-рабочий стол со всеми виджетами, выбор нажатием ---------- */
@Composable
private fun OriginDesk(st: AppState, n: kz.alnazar.schedule.logic.Now, wl: String, sel: String, onSel: (String) -> Unit, onAdd: () -> Unit) {
    val u = 76; val gp = 8
    fun at(c: Int, r: Int) = Modifier.offset((c * (u + gp)).dp, (r * (u + gp)).dp)
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).wallFor("origin").padding(vertical = 16.dp), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.size((4 * u + 3 * gp).dp, (7 * u + 6 * gp).dp)) {
                @Composable fun Pick(s: String, m: Modifier) {
                    val on = s == sel
                    val sc by animateFloatAsState(if (on) 1f else .97f, spring(.6f, 400f), label = "osc")
                    Box(m.graphicsLayer { scaleX = sc; scaleY = sc }.then(if (on) Modifier.border(2.dp, T.acc, RoundedCornerShape(30.dp)) else Modifier).tap { onSel(s) }) { WidgetPreview(s, st, n, wl) }
                }
                Pick("4x2", at(0, 0)); Pick("2x2", at(0, 2)); Pick("2x4", at(2, 2)); Pick("2x1", at(0, 4)); Pick("1x2", at(0, 5))
                Box(at(1, 5).padding(12.dp)) { FakeApp("📷") }
                Box(at(1, 6).padding(12.dp)) { FakeApp("💬") }
            }
        }
        Btn(addLabel(if (st.appLang == "kk") "kk" else st.appLang) + " · " + sz(sel), Modifier.fillMaxWidth(), onClick = onAdd)
    }
}

/* ---------- One UI: отдельная карточка, размеры списком, круглая «+» ---------- */
@Composable
private fun OneUiPicker(st: AppState, n: kz.alnazar.schedule.logic.Now, wl: String, lang: String, sel: String, onSel: (String) -> Unit, onAdd: () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).wallFor("oneui").padding(vertical = 22.dp)) {
            StageBox(sel, 170) { PreviewSwitch(sel, st, n, wl) }
        }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(T.s2).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SIZES.forEach { s ->
                val on = s == sel
                val bg by animateColorAsState(if (on) T.acc else Color.Transparent, tween(220), label = "obg")
                val fg by animateColorAsState(if (on) T.accTx else T.tx2, tween(220), label = "ofg")
                Box(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(bg).tap { onSel(s) }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                    Text(sz(s), color = fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(99.dp)).background(T.acc).tap(onClick = onAdd), contentAlignment = Alignment.Center) {
                Icon(Ic.plus, null, tint = T.accTx, modifier = Modifier.size(26.dp))
            }
        }
    }
}

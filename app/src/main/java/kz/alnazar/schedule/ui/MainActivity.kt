package kz.alnazar.schedule.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kz.alnazar.schedule.data.Store
import kz.alnazar.schedule.logic.Tr
import kz.alnazar.schedule.widget.WidgetUpdater

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        Store.ensure(this)
        tabReq.value = intent?.getIntExtra("tab", -1)?.takeIf { it >= 0 }
        setContent { Root() }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        tabReq.value = intent.getIntExtra("tab", -1).takeIf { it >= 0 }
    }

    override fun onResume() {
        super.onResume()
        // вернулись из системных настроек (например, выдали точные будильники) — обновить виджеты
        WidgetUpdater.refresh(this)
    }
}

/** Открыть вкладку по запросу извне (уведомление о домашке → «Задания»). */
val tabReq = androidx.compose.runtime.mutableStateOf<Int?>(null)

/** Фон экрана: градиент стиля; у OriginOS и One UI — нарисованные «обои». */
@Composable
fun Modifier.screenBg(): Modifier {
    val t = T
    val c = t.bg
    return this.drawBehind {
        if (t.id == "origin" || t.id == "oneui") drawStyleWall(t.id, size.width, size.height)
        else drawRect(Brush.linearGradient(0f to c[0], .55f to c[1], 1f to c[2],
            start = Offset(size.width * .37f, 0f), end = Offset(size.width * .63f, size.height)))
    }
}

@Composable
private fun Root() {
    val st by Store.state.collectAsStateWithLifecycle()
    val ui = remember { UiCtl() }
    val pager = rememberPagerState { 3 }
    val backdrop = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    val req = tabReq.value
    LaunchedEffect(req) { if (req != null) { ui.home(); ui.close(); pager.scrollToPage(req.coerceIn(0, 2)); tabReq.value = null } }
    // «сейчас» пересчитывается раз в 20 секунд — главный экран сам переключается по звонкам
    var clock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(20_000); clock = System.currentTimeMillis() } }
    val lang = st.appLang

    BackHandler(enabled = ui.sheet != null || ui.page != null) { if (ui.sheet != null) ui.close() else ui.back() }

    val tok = remember(st.style, st.theme, st.ct, st.rs) { tokFor(st.style).let { if (st.theme == "custom") it.withCustom(st.ct) else it }.withRadius(st.rs) }
    val view = LocalView.current
    SideEffect {
        val win = (view.context as? android.app.Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(win, view).apply {
            isAppearanceLightStatusBars = !tok.isDark
            isAppearanceLightNavigationBars = !tok.isDark
        }
    }
    // размер интерфейса: масштабируем плотность — вырастают и текст, и отступы, и иконки; системный шрифт сохраняется
    val d0 = LocalDensity.current
    val scaled = remember(d0, st.uiScale) { Density(d0.density * st.uiScale.coerceIn(.8f, 1.3f), d0.fontScale) }
    CompositionLocalProvider(LocalDensity provides scaled) {
    AppTheme(tok, st.font) {
    val goTab: (Int) -> Unit = { i -> ui.home(); ui.close(); scope.launch { pager.animateScrollToPage(i.coerceIn(0, 2)) } }
    CompositionLocalProvider(LocalUi provides ui, LocalIcons provides IconPrefs(st.iconSet, st.iconTint), LocalBackdrop provides backdrop, LocalGoTab provides goTab) {
        Box(Modifier.fillMaxSize()) {
            // содержимое записывается в слой — по нему меню и шапка делают размытие
            Box(
                Modifier.fillMaxSize().drawWithContent {
                    backdrop.record { this@drawWithContent.drawContent() }
                    drawLayer(backdrop)
                },
            ) {
                Box(Modifier.fillMaxSize().screenBg())
                HorizontalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1) { p ->
                    CompositionLocalProvider(LocalPageIdx provides p, LocalPageOn provides (pager.currentPage == p)) {
                        when (p) {
                            0 -> HomeScreen(st, clock)
                            1 -> ScheduleScreen(st)
                            else -> HwScreen(st)
                        }
                    }
                }
            }

            // ColorOS: при прокрутке заголовок с кнопками собирается в одну строку под лёгким матовым стеклом
            if (tok.id == "color" && ui.page == null && HeaderBus.owner == pager.currentPage) CompactHeader(backdrop)

            if (ui.page == null) {
                val tabs = listOf(
                    NavTab(Tr.t(lang, "tab_home"), Ic.home),
                    NavTab(Tr.t(lang, "tab_schedule"), Ic.cal),
                    NavTab(Tr.t(lang, "tab_hw"), Ic.hw),
                )
                val onPlus: () -> Unit = {
                    PlusAnchor.capture()
                    when (pager.currentPage) {
                        1 -> {
                            val today = java.time.LocalDate.now()
                            val dow = (today.dayOfWeek.value - 1).coerceAtMost(st.schoolDays - 1)
                            val n = st.week.getOrNull(dow)?.size ?: 0
                            ui.open(morph = true) { LessonSheet(dow, n, kz.alnazar.schedule.logic.Sched.weekKey(today)) }
                        }
                        else -> ui.open(morph = true) { HwSheet(null, "hw") }
                    }
                }
                StyleNav(tabs, pager, backdrop, Modifier.align(Alignment.BottomCenter), onPlus) { ui.go("settings") }
            }

            // вложенные страницы: настройки и их разделы — въезжают справа
            AnimatedContent(
                ui.page, modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    val forward = (targetState != null && initialState == null) || (ui.stack.size > 0 && targetState != null && initialState != null && ui.stack.contains(initialState))
                    if (targetState == null) (fadeIn(tween(1)) togetherWith slideOutHorizontally(tween(260)) { it } + fadeOut(tween(260)))
                    else if (forward) (slideInHorizontally(spring(.9f, 420f)) { it } togetherWith slideOutHorizontally(tween(260)) { -it / 4 } + fadeOut(tween(200)))
                    else (slideInHorizontally(spring(.9f, 420f)) { -it / 4 } + fadeIn(tween(200)) togetherWith slideOutHorizontally(tween(240)) { it })
                },
                label = "pages",
            ) { pg ->
                if (pg != null) Box(Modifier.fillMaxSize().screenBg()) { SubPage(pg, st) }
                else Box(Modifier.fillMaxSize())
            }

            SheetHost(ui.sheet != null && !ui.sheetMorph, { ui.close() }) {
                key(ui.sheetId) { ui.lastSheet?.invoke(this) }
            }
            PlusWindow(ui.sheet != null && ui.sheetMorph, { ui.close() }) {
                key(ui.sheetId) { ui.lastSheet?.invoke(this) }
            }
        }
    }
}
}
}

/** Свёрнутая шапка ColorOS: лёгкое матовое стекло, растворяющееся книзу, заголовок с мягкой тенью и круглые кнопки. */
@Composable
private fun BoxScope.CompactHeader(backdrop: androidx.compose.ui.graphics.layer.GraphicsLayer) {
    val f = HeaderBus.frac
    if (f <= .01f) return
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(top + 92.dp).graphicsLayer { alpha = f }) {
        Box(Modifier.fillMaxSize()
            .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
            .backdropBlur(backdrop, 5.dp, Color(0x29282830), RoundedCornerShape(0.dp))
            .drawWithContent {
                drawContent()
                drawRect(Brush.verticalGradient(0f to Color.Black, .62f to Color.Black, 1f to Color.Transparent), blendMode = androidx.compose.ui.graphics.BlendMode.DstIn)
            })
        Row(Modifier.padding(top = top).fillMaxWidth().height(64.dp).padding(start = 20.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(HeaderBus.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).graphicsLayer { translationY = (1f - f) * 12.dp.toPx() },
                style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = .45f), Offset(0f, 2f), 24f)))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { HeaderBus.actions.forEach { (ic, fn) -> RoundBtn(ic, 46.dp, fn) } }
        }
    }
}

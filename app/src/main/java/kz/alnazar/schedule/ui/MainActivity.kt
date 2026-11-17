package kz.alnazar.schedule.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
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
        setContent { Root() }
    }

    override fun onResume() {
        super.onResume()
        // вернулись из системных настроек (например, выдали точные будильники) — обновить виджеты
        WidgetUpdater.refresh(this)
    }
}

/** Фон экрана — градиент 165° из прототипа. */
@Composable
fun Modifier.screenBg(): Modifier {
    val c = T.bg
    return this.drawBehind {
        drawRect(Brush.linearGradient(0f to c[0], .55f to c[1], 1f to c[2],
            start = Offset(size.width * .37f, 0f), end = Offset(size.width * .63f, size.height)))
    }
}

@Composable
private fun Root() {
    val st by Store.state.collectAsStateWithLifecycle()
    val ui = remember { UiCtl() }
    val pager = rememberPagerState { 5 }
    val backdrop = rememberGraphicsLayer()
    // «сейчас» пересчитывается раз в 20 секунд — главный экран сам переключается по звонкам
    var clock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(20_000); clock = System.currentTimeMillis() } }
    val lang = st.appLang

    BackHandler(enabled = ui.sheet != null || ui.page != null) { if (ui.sheet != null) ui.close() else ui.back() }

    AppTheme(tokFor(st.style)) {
    CompositionLocalProvider(LocalUi provides ui, LocalIcons provides IconPrefs(st.iconSet, st.iconTint)) {
        Box(Modifier.fillMaxSize()) {
            // содержимое записывается в слой — по нему меню делает размытие и преломление
            Box(
                Modifier.fillMaxSize().drawWithContent {
                    backdrop.record { this@drawWithContent.drawContent() }
                    drawLayer(backdrop)
                },
            ) {
                Box(Modifier.fillMaxSize().screenBg())
                HorizontalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1) { p ->
                    when (p) {
                        0 -> HomeScreen(st, clock)
                        1 -> ScheduleScreen(st)
                        2 -> WidgetsScreen(st, clock)
                        3 -> HwScreen(st)
                        else -> SettingsScreen(st)
                    }
                }
            }

            if (ui.page == null) {
                val tabs = listOf(
                    NavTab(Tr.t(lang, "tab_home"), Ic.home),
                    NavTab(Tr.t(lang, "tab_schedule"), Ic.cal),
                    NavTab(Tr.t(lang, "tab_widgets"), Ic.wid),
                    NavTab(Tr.t(lang, "tab_hw"), Ic.hw),
                    NavTab(Tr.t(lang, "tab_settings"), Ic.set),
                )
                if (st.style == "pixel") PixelNav(tabs, pager, Modifier.align(Alignment.BottomCenter))
                else GlassNav(tabs, pager, backdrop, Modifier.align(Alignment.BottomCenter))
            }

            AnimatedVisibility(
                ui.page != null,
                enter = slideInHorizontally(tween(260)) { it },
                exit = slideOutHorizontally(tween(220)) { it },
            ) {
                Box(Modifier.fillMaxSize().screenBg()) { ui.lastPage?.let { SubPage(it, st) } }
            }

            SheetHost(ui.sheet != null, { ui.close() }) {
                key(ui.sheetId) { ui.lastSheet?.invoke(this) }
            }
        }
    }
}
}

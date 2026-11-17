package kz.alnazar.schedule.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kz.alnazar.schedule.data.Store
import kz.alnazar.schedule.logic.Tr
import kz.alnazar.schedule.widget.WidgetUpdater

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Store.ensure(this)
        setContent { AppTheme { Root() } }
    }

    override fun onResume() {
        super.onResume()
        // вернулись из настроек (например, выдали точные будильники) — переставить и обновить виджеты
        WidgetUpdater.refresh(this)
    }
}

@Composable
private fun Root() {
    val st by Store.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(0) }
    // «сейчас» обновляется раз в 20 секунд, чтобы главный экран сам переключался по звонкам
    var clock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(20_000); clock = System.currentTimeMillis() } }
    val lang = st.appLang

    Scaffold(
        containerColor = Pal.bg,
        bottomBar = {
            NavigationBar(containerColor = Pal.card) {
                val items = listOf(
                    Triple("tab_home", Icons.Filled.Home, 0),
                    Triple("tab_schedule", Icons.Filled.DateRange, 1),
                    Triple("tab_settings", Icons.Filled.Settings, 2),
                )
                items.forEach { (key, icon, i) ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(icon, contentDescription = null) },
                        label = { Text(Tr.t(lang, key)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Pal.acc, selectedTextColor = Pal.acc,
                            indicatorColor = Pal.card2, unselectedIconColor = Pal.tx2, unselectedTextColor = Pal.tx2,
                        ),
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when (tab) {
                0 -> HomeScreen(st, clock)
                1 -> ScheduleScreen(st)
                else -> SettingsScreen(st)
            }
        }
    }
}

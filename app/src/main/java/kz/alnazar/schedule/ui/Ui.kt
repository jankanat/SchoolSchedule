package kz.alnazar.schedule.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Управление шторками и вложенными страницами (как S.sheet и S.page в прототипе). */
class UiCtl {
    var sheet by mutableStateOf<(@Composable ColumnScope.() -> Unit)?>(null)
        private set
    var lastSheet: (@Composable ColumnScope.() -> Unit)? = null
        private set
    var page by mutableStateOf<String?>(null)
    var lastPage: String? = null
    var sheetId by mutableStateOf(0)
        private set

    fun open(content: @Composable ColumnScope.() -> Unit) { lastSheet = content; sheetId++; sheet = content }
    fun close() { sheet = null }
    fun go(p: String) { lastPage = p; page = p }
    fun back() { page = null }
}

val LocalUi = staticCompositionLocalOf { UiCtl() }

/** Страница-вкладка: прокрутка, отступ сверху под статус-бар и снизу под плавающее меню. */
@Composable
fun Page(bottomNav: Boolean = true, content: LazyListScope.() -> Unit) {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = top, bottom = bottom + if (bottomNav) 110.dp else 28.dp),
        content = content,
    )
}

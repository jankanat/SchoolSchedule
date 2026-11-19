package kz.alnazar.schedule.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kz.alnazar.schedule.data.AppState
import kz.alnazar.schedule.logic.Now
import kz.alnazar.schedule.widget.WRender

/**
 * Превью виджета в приложении — та же картинка, что рисуется на рабочем столе (WRender),
 * поэтому превью и настоящий виджет не могут разойтись.
 */
@Composable
fun WidgetPreview(size: String, st: AppState, n: Now, lang: String) {
    val ctx = LocalContext.current
    val (w, h) = when (size) {
        "2x2" -> 160 to 160
        "2x1" -> 160 to 76
        "1x2" -> 76 to 160
        "2x4" -> 160 to 328
        else -> 328 to 160
    }
    val bmp = remember(st, n, size, lang) { WRender.render(ctx, st, n, size, w.toFloat(), h.toFloat()).asImageBitmap() }
    Image(bmp, contentDescription = null, modifier = Modifier.size(w.dp, h.dp))
}

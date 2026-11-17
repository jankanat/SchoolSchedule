package kz.alnazar.schedule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Палитра стиля iOS 26, тёмная тема. Остальные стили и темы — следующий этап. */
object Pal {
    val bg = Color(0xFF000000)
    val card = Color(0xFF1C1C1E)
    val card2 = Color(0xFF2C2C2E)
    val line = Color(0x26FFFFFF)
    val tx = Color(0xFFFFFFFF)
    val tx2 = Color(0x99EBEBF5)
    val tx3 = Color(0x4DEBEBF5)
    val acc = Color(0xFF0A84FF)
    val warn = Color(0xFFFF9F0A)
    val red = Color(0xFFFF453A)
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Pal.acc, onPrimary = Color.White, background = Pal.bg, surface = Pal.card,
            onSurface = Pal.tx, onBackground = Pal.tx, surfaceVariant = Pal.card2, onSurfaceVariant = Pal.tx2,
            secondaryContainer = Pal.card2, onSecondaryContainer = Pal.tx, outline = Pal.line,
        ),
        content = content,
    )
}

/** Цветная плитка с эмодзи предмета. */
@Composable
fun EmojiTile(emoji: String, color: Long, size: Dp = 40.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size / 3.6f)).background(Color(color).copy(alpha = .28f)),
        contentAlignment = Alignment.Center,
    ) { Text(emoji, fontSize = (size.value * .5f).sp) }
}

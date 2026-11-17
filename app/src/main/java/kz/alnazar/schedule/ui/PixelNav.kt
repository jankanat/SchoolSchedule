package kz.alnazar.schedule.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Нижняя навигация Pixel OS (Material 3 Expressive): плоская панель, у выбранной вкладки
 * капсула-индикатор за иконкой раскрывается от центра с пружиной.
 */
@Composable
fun PixelNav(tabs: List<NavTab>, pager: PagerState, modifier: Modifier = Modifier) {
    val t = T
    val scope = rememberCoroutineScope()
    val fs = LocalDensity.current.fontScale
    val cur = pager.targetPage
    Row(
        modifier.fillMaxWidth().background(t.navBg).navigationBarsPadding().height(80.dp).padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEachIndexed { i, tab ->
            val on = i == cur
            val w by animateDpAsState(if (on) 56.dp else 0.dp, spring(dampingRatio = .55f, stiffness = 420f), label = "ind")
            val ic by animateColorAsState(if (on) t.onSecCont else t.tx2, tween(180), label = "ic")
            val lc by animateColorAsState(if (on) t.tx else t.tx2, tween(180), label = "lc")
            Column(
                Modifier.weight(1f).clickable(remember { MutableInteractionSource() }, null) {
                    scope.launch { pager.animateScrollToPage(i) }
                },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(64.dp, 32.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.width(w).height(32.dp).clip(RoundedCornerShape(99.dp)).background(t.secCont))
                    Icon(tab.icon, contentDescription = null, tint = ic, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    tab.label, color = lc, maxLines = 1, softWrap = false,
                    style = TextStyle(
                        fontSize = (12f / fs).sp, lineHeight = (16f / fs).sp,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                    ),
                )
            }
        }
    }
}

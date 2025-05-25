package io.github.couchtracker.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.couchtracker.ui.ImagePreloadOptions
import kotlin.math.roundToInt

object PortraitComposableDefaults {
    const val POSTER_ASPECT_RATIO = 2f / 3
    val SUGGESTED_WIDTH = 120.dp

    @Composable
    fun preloadOptions(maxW: Dp = SUGGESTED_WIDTH): ImagePreloadOptions.Preload {
        val wPx = with(LocalDensity.current) { maxW.toPx() }
        val hPx = wPx * LocalDensity.current.density / POSTER_ASPECT_RATIO
        return ImagePreloadOptions.Preload(LocalContext.current, wPx.roundToInt(), hPx.roundToInt())
    }
}

@Composable
fun PortraitComposable(
    modifier: Modifier,
    image: @Composable (w: Int, h: Int) -> Unit,
    label: @Composable () -> Unit,
) {
    Column(modifier, horizontalAlignment = Alignment.Companion.CenterHorizontally) {
        Surface(
            shape = MaterialTheme.shapes.small,
            shadowElevation = 8.dp,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(PortraitComposableDefaults.POSTER_ASPECT_RATIO),
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                image(this.constraints.maxWidth, this.constraints.minHeight)
            }
        }
        Spacer(Modifier.height(8.dp))
        CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.titleSmall) {
            Column(Modifier.animateContentSize(), horizontalAlignment = Alignment.Companion.CenterHorizontally) {
                label()
            }
        }
    }
}

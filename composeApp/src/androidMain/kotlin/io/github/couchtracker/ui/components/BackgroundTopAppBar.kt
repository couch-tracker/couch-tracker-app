@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.couchtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.pow

private const val GRADIENT_STEPS = 16
private const val BACKGROUND_START_ALPHA = 0.4f
private val APP_BAR_MAX_ELEVATION = 16.dp
private val BACKGROUND_MAX_BLUR = 16.dp

@Composable
fun BackgroundTopAppBar(
    scrollBehavior: TopAppBarScrollBehavior,
    image: @Composable (Modifier) -> Unit,
    appBar: @Composable (TopAppBarColors) -> Unit,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
) {
    Box(
        Modifier.graphicsLayer {
            val scrollAmount = -scrollBehavior.state.contentOffset
            shadowElevation = scrollAmount.coerceAtMost(APP_BAR_MAX_ELEVATION.toPx())
            ambientShadowColor = backgroundColor
        },
    ) {
        image(
            Modifier.matchParentSize().graphicsLayer {
                val radius = scrollBehavior.state.collapsedFraction * BACKGROUND_MAX_BLUR.toPx()
                if (radius > 0) {
                    this.renderEffect = BlurEffect(radius, radius, TileMode.Clamp)
                }
                this.clip = true
            },
        )
        Box(Modifier.background(createGradientBrush(backgroundColor))) {
            appBar(
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
            )
        }
    }
}

private fun createGradientBrush(color: Color): Brush {
    return Brush.verticalGradient(
        List(GRADIENT_STEPS + 1) { step ->
            val progress = step.toFloat() / GRADIENT_STEPS
            // The derivative of the alpha is 0 when progress is 1.
            val progressAdjusted = 2 * progress - progress.pow(2)
            color.copy(alpha = BACKGROUND_START_ALPHA + progressAdjusted * (1f - BACKGROUND_START_ALPHA))
        },
    )
}
